/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.orders.service.espier;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.service.espier.NormalOrdersUploadDeliveryPort.UploadSepItem;
import cn.shopex.ecshopx.orders.service.espier.NormalOrdersUploadDeliveryPort.UploadShipCommand;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.util.StringUtils;

@Service
public class NormalOrdersUploadSegmentService {

	private static final Pattern POSITIVE_INT = Pattern.compile("^[1-9]\\d*$");

	private final NormalOrdersUploadDeliveryPort deliveryPort;
	private final NormalOrdersUploadSplitValidator splitValidator;
	private final SegmentTx segmentTx;

	NormalOrdersUploadSegmentService(
			NormalOrdersUploadDeliveryPort deliveryPort,
			NormalOrdersUploadSplitValidator splitValidator,
			SegmentTx segmentTx) {
		this.deliveryPort = deliveryPort;
		this.splitValidator = splitValidator;
		this.segmentTx = segmentTx;
	}

	@Autowired
	public NormalOrdersUploadSegmentService(
			NormalOrdersUploadDeliveryPort deliveryPort,
			NormalOrdersUploadSplitValidator splitValidator,
			PlatformTransactionManager transactionManager) {
		this.deliveryPort = deliveryPort;
		this.splitValidator = splitValidator;
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		this.segmentTx = work -> transactionTemplate.executeWithoutResult(status -> work.run());
	}

	public NormalOrdersUploadImportResult importRows(
			long companyId,
			long operatorId,
			@SuppressWarnings("unused") long distributorId,
			long supplierId,
			@SuppressWarnings("unused") long merchantId,
			List<Map<String, Object>> rows,
			String operatorType) {
		ImportState state = new ImportState(companyId, operatorId, operatorType, supplierId);

		for (Map<String, Object> row : rows) {
			ParsedRow parsed = parseRow(row);
			if (parsed.errorMessage != null) {
				flushSegment(state);
				state.failures.add(new RowFailure(row, parsed.errorMessage));
				continue;
			}
			if (parsed.splitShipment) {
				if (state.wholeShipped.contains(parsed.orderId)) {
					flushSegment(state);
					state.failures.add(new RowFailure(row, "该订单已按整单发货，不能再拆单"));
					continue;
				}
				if (!state.segment.isEmpty()
						&& (state.segment.get(0).orderId != parsed.orderId
								|| !state.segment.get(0).deliveryCode.equals(parsed.deliveryCode))) {
					flushSegment(state);
				}
				state.segment.add(
						new SplitSegmentRow(
								row,
								parsed.orderId,
								parsed.deliveryCode,
								parsed.deliveryCorpName,
								parsed.orderItemId,
								parsed.deliveryNum));
			} else {
				flushSegment(state);
				UploadShipCommand command =
						new UploadShipCommand(
								companyId,
								operatorId,
								operatorType,
								supplierId,
								parsed.orderId,
								parsed.deliveryCorpName,
								parsed.deliveryCode);
				try {
					segmentTx.run(() -> deliveryPort.deliverWhole(command));
					state.wholeShipped.add(parsed.orderId);
					state.successLine++;
				} catch (RuntimeException ex) {
					state.failures.add(new RowFailure(row, extractMessage(ex)));
				}
			}
		}
		flushSegment(state);
		return new NormalOrdersUploadImportResult(state.successLine, List.copyOf(state.failures));
	}

	private void flushSegment(ImportState state) {
		if (state.segment.isEmpty()) {
			return;
		}
		String refCorp = state.segment.get(0).deliveryCorpName;
		boolean corpMismatch = false;
		for (SplitSegmentRow entry : state.segment) {
			if (!entry.deliveryCorpName.equals(refCorp)) {
				corpMismatch = true;
				break;
			}
		}
		if (corpMismatch) {
			for (SplitSegmentRow entry : state.segment) {
				String message =
						entry.deliveryCorpName.equals(refCorp)
								? "同一包裹存在错误行，本次未发货"
								: "同一包裹快递公司不一致";
				state.failures.add(new RowFailure(entry.row, message));
			}
			state.segment.clear();
			return;
		}

		List<Map<String, Object>> segmentMaps =
				state.segment.stream().map(SplitSegmentRow::row).toList();
		List<String> validation;
		try {
			validation =
					splitValidator.validate(state.companyId, state.supplierId, segmentMaps);
		} catch (RuntimeException ex) {
			String message = extractMessage(ex);
			for (SplitSegmentRow entry : state.segment) {
				state.failures.add(new RowFailure(entry.row, message));
			}
			state.segment.clear();
			return;
		}
		boolean hasValidatorError = validation.stream().anyMatch(StringUtils::hasText);
		if (hasValidatorError) {
			for (int i = 0; i < state.segment.size(); i++) {
				String message = validation.get(i);
				if (!StringUtils.hasText(message)) {
					message = "同一包裹存在错误行，本次未发货";
				}
				state.failures.add(new RowFailure(state.segment.get(i).row, message));
			}
			state.segment.clear();
			return;
		}

		SplitSegmentRow first = state.segment.get(0);
		UploadShipCommand command =
				new UploadShipCommand(
						state.companyId,
						state.operatorId,
						state.operatorType,
						state.supplierId,
						first.orderId,
						first.deliveryCorpName,
						first.deliveryCode);
		List<UploadSepItem> items =
				state.segment.stream()
						.map(r -> new UploadSepItem(r.orderItemId, r.deliveryNum))
						.toList();
		try {
			segmentTx.run(() -> deliveryPort.deliverSep(command, items));
			state.successLine += state.segment.size();
		} catch (RuntimeException ex) {
			String message = extractMessage(ex);
			for (SplitSegmentRow entry : state.segment) {
				state.failures.add(new RowFailure(entry.row, message));
			}
		}
		state.segment.clear();
	}

	private static ParsedRow parseRow(Map<String, Object> row) {
		String orderIdRaw = NormalOrdersUploadImportOrderIdSanitizer.sanitize(trim(row.get("order_id")));
		if (!StringUtils.hasText(orderIdRaw)) {
			return ParsedRow.error("订单号错误");
		}
		long orderId;
		try {
			orderId = Long.parseLong(orderIdRaw.trim());
		} catch (NumberFormatException e) {
			return ParsedRow.error("订单号格式错误");
		}
		String deliveryCode = trim(row.get("delivery_code"));
		String deliveryCorpName = trim(row.get("delivery_corp_name"));
		if (!StringUtils.hasText(deliveryCode) || !StringUtils.hasText(deliveryCorpName)) {
			return ParsedRow.error("缺少快递信息");
		}

		boolean hasOrderItemId = hasCellText(row.get("order_item_id"));
		boolean hasItemBn = hasCellText(row.get("item_bn"));
		boolean hasDeliveryNum = hasCellText(row.get("delivery_num"));
		if (!hasOrderItemId && !hasItemBn && !hasDeliveryNum) {
			return ParsedRow.whole(orderId, deliveryCode, deliveryCorpName);
		}
		if (!hasOrderItemId || !hasItemBn || !hasDeliveryNum) {
			return ParsedRow.error("拆单信息必须同时填写子订单号、商品编码和发货数量");
		}
		String deliveryNumRaw = trim(row.get("delivery_num"));
		if (!POSITIVE_INT.matcher(deliveryNumRaw).matches()) {
			return ParsedRow.error("发货数量必须是正整数");
		}
		String orderItemIdRaw = trim(row.get("order_item_id"));
		if (!POSITIVE_INT.matcher(orderItemIdRaw).matches()) {
			return ParsedRow.error("子订单号格式错误");
		}
		int deliveryNum;
		try {
			deliveryNum = Integer.parseInt(deliveryNumRaw);
		} catch (NumberFormatException e) {
			return ParsedRow.error("发货数量必须是正整数");
		}
		long orderItemId;
		try {
			orderItemId = Long.parseLong(orderItemIdRaw);
		} catch (NumberFormatException e) {
			return ParsedRow.error("子订单号格式错误");
		}
		return ParsedRow.split(
				orderId,
				deliveryCode,
				deliveryCorpName,
				orderItemId,
				deliveryNum);
	}

	private static String extractMessage(RuntimeException ex) {
		Throwable current = ex;
		while (current != null) {
			if (current instanceof ResourceException || current instanceof BadRequestException) {
				String message = current.getMessage();
				if (StringUtils.hasText(message)) {
					return message;
				}
			}
			current = current.getCause();
		}
		String message = ex.getMessage();
		return StringUtils.hasText(message) ? message : "发货失败";
	}

	private static boolean hasCellText(Object value) {
		return StringUtils.hasText(trim(value));
	}

	private static String trim(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}

	@FunctionalInterface
	interface SegmentTx {
		void run(Runnable work);
	}

	private static final class ImportState {
		final long companyId;
		final long operatorId;
		final String operatorType;
		final long supplierId;
		int successLine;
		final List<RowFailure> failures = new ArrayList<>();
		final Set<Long> wholeShipped = new HashSet<>();
		final List<SplitSegmentRow> segment = new ArrayList<>();

		ImportState(long companyId, long operatorId, String operatorType, long supplierId) {
			this.companyId = companyId;
			this.operatorId = operatorId;
			this.operatorType = operatorType;
			this.supplierId = supplierId;
		}
	}

	private record SplitSegmentRow(
			Map<String, Object> row,
			long orderId,
			String deliveryCode,
			String deliveryCorpName,
			long orderItemId,
			int deliveryNum) {}

	private static final class ParsedRow {
		final String errorMessage;
		final boolean splitShipment;
		final long orderId;
		final String deliveryCode;
		final String deliveryCorpName;
		final long orderItemId;
		final int deliveryNum;

		private ParsedRow(
				String errorMessage,
				boolean splitShipment,
				long orderId,
				String deliveryCode,
				String deliveryCorpName,
				long orderItemId,
				int deliveryNum) {
			this.errorMessage = errorMessage;
			this.splitShipment = splitShipment;
			this.orderId = orderId;
			this.deliveryCode = deliveryCode;
			this.deliveryCorpName = deliveryCorpName;
			this.orderItemId = orderItemId;
			this.deliveryNum = deliveryNum;
		}

		static ParsedRow error(String message) {
			return new ParsedRow(message, false, 0L, "", "", 0L, 0);
		}

		static ParsedRow whole(long orderId, String deliveryCode, String deliveryCorpName) {
			return new ParsedRow(null, false, orderId, deliveryCode, deliveryCorpName, 0L, 0);
		}

		static ParsedRow split(
				long orderId,
				String deliveryCode,
				String deliveryCorpName,
				long orderItemId,
				int deliveryNum) {
			return new ParsedRow(
					null, true, orderId, deliveryCode, deliveryCorpName, orderItemId, deliveryNum);
		}
	}

	public record NormalOrdersUploadImportResult(int successLine, List<RowFailure> failures) {}

	public record RowFailure(Map<String, Object> row, String message) {}
}
