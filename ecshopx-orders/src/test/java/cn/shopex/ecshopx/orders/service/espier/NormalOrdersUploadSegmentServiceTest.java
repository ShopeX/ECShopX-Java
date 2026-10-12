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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.service.espier.NormalOrdersUploadDeliveryPort.UploadSepItem;
import cn.shopex.ecshopx.orders.service.espier.NormalOrdersUploadDeliveryPort.UploadShipCommand;
import cn.shopex.ecshopx.orders.service.espier.NormalOrdersUploadSegmentService.NormalOrdersUploadImportResult;
import cn.shopex.ecshopx.orders.service.espier.NormalOrdersUploadSegmentService.RowFailure;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NormalOrdersUploadSegmentServiceTest {

	private static final long COMPANY_ID = 1L;
	private static final long OPERATOR_ID = 2L;
	private static final String OPERATOR_TYPE = "admin";

	private RecordingPort port;
	private NormalOrdersUploadSplitValidator validator;
	private NormalOrdersUploadSegmentService service;

	@BeforeEach
	void setUp() {
		port = new RecordingPort();
		validator = mock(NormalOrdersUploadSplitValidator.class);
		when(validator.validate(anyLong(), anyLong(), anyList()))
				.thenAnswer(
						inv -> {
							@SuppressWarnings("unchecked")
							List<Map<String, Object>> segment = inv.getArgument(2);
							return segment.stream().map(r -> (String) null).collect(Collectors.toList());
						});
		service =
				new NormalOrdersUploadSegmentService(
						port, validator, (NormalOrdersUploadSegmentService.SegmentTx) Runnable::run);
	}

	@Test
	void twoWholeRowsDifferentOrders_deliverWholeTwice() {
		List<Map<String, Object>> rows =
				List.of(
						wholeRow("9001", "YD1", "顺丰"),
						wholeRow("9002", "YD2", "圆通"));

		NormalOrdersUploadImportResult result = importRows(rows);

		assertThat(result.successLine()).isEqualTo(2);
		assertThat(result.failures()).isEmpty();
		assertThat(port.wholeCalls).hasSize(2);
		assertThat(port.sepCalls).isEmpty();
		assertThat(port.wholeCalls.get(0).orderId()).isEqualTo(9001L);
		assertThat(port.wholeCalls.get(1).orderId()).isEqualTo(9002L);
	}

	@Test
	void orderIdQuotes_stripToSameNumericId() {
		List<Map<String, Object>> rows =
				List.of(
						wholeRow("'9003'", "YD1", "顺丰"),
						wholeRow("\"9003\"", "YD2", "顺丰"));

		importRows(rows);

		assertThat(port.wholeCalls).hasSize(2);
		assertThat(port.wholeCalls.get(0).orderId()).isEqualTo(9003L);
		assertThat(port.wholeCalls.get(1).orderId()).isEqualTo(9003L);
	}

	@Test
	void consecutiveSplitRows_sameOrderAndTracking_oneDeliverSep() {
		List<Map<String, Object>> rows =
				List.of(
						splitRow("9003", "YD1", "顺丰", "11", "BN-1", "1"),
						splitRow("9003", "YD1", "顺丰", "12", "BN-2", "2"));

		NormalOrdersUploadImportResult result = importRows(rows);

		assertThat(result.successLine()).isEqualTo(2);
		assertThat(port.sepCalls).hasSize(1);
		assertThat(port.sepCalls.get(0).items())
				.containsExactly(new UploadSepItem(11L, 1), new UploadSepItem(12L, 2));
	}

	@Test
	void splitRows_interruptedByAnotherOrder_twoDeliverSep() {
		List<Map<String, Object>> rows =
				List.of(
						splitRow("9003", "YD1", "顺丰", "11", "BN-1", "1"),
						wholeRow("9004", "YD9", "顺丰"),
						splitRow("9003", "YD1", "顺丰", "12", "BN-2", "2"));

		importRows(rows);

		assertThat(port.sepCalls).hasSize(2);
		assertThat(port.wholeCalls).hasSize(1);
	}

	@Test
	void wholeThenSplitOnSameOrder_splitRejectedWithoutDeliverSep() {
		List<Map<String, Object>> rows =
				List.of(
						wholeRow("9003", "YD1", "顺丰"),
						splitRow("9003", "YD1", "顺丰", "11", "BN-1", "1"));

		NormalOrdersUploadImportResult result = importRows(rows);

		assertThat(port.wholeCalls).hasSize(1);
		assertThat(port.sepCalls).isEmpty();
		assertThat(result.successLine()).isEqualTo(1);
		assertThat(result.failures()).hasSize(1);
		assertThat(result.failures().get(0).message()).isEqualTo("该订单已按整单发货，不能再拆单");
	}

	@Test
	void wholeFails_doesNotBlockLaterSplit() {
		port.wholeAction =
				cmd -> {
					if (cmd.orderId() == 9003L) {
						throw new ResourceException("订单已取消，不能发货");
					}
				};

		List<Map<String, Object>> rows =
				List.of(
						wholeRow("9003", "YD1", "顺丰"),
						splitRow("9003", "YD2", "顺丰", "11", "BN-1", "1"));

		NormalOrdersUploadImportResult result = importRows(rows);

		assertThat(port.wholeCalls).hasSize(1);
		assertThat(port.sepCalls).hasSize(1);
		assertThat(result.successLine()).isEqualTo(1);
		assertThat(result.failures()).hasSize(1);
		assertThat(result.failures().get(0).message()).isEqualTo("订单已取消，不能发货");
	}

	@Test
	void splitThenWhole_deliverSepBeforeDeliverWhole() {
		List<Map<String, Object>> rows =
				List.of(
						splitRow("9003", "YD1", "顺丰", "11", "BN-1", "1"),
						wholeRow("9003", "YD2", "顺丰"));

		importRows(rows);

		assertThat(port.sepCalls).hasSize(1);
		assertThat(port.wholeCalls).hasSize(1);
	}

	@Test
	void twoWholeRowsSameOrder_deliverWholeTwice() {
		List<Map<String, Object>> rows =
				List.of(wholeRow("9003", "YD1", "顺丰"), wholeRow("9003", "YD2", "顺丰"));

		NormalOrdersUploadImportResult result = importRows(rows);

		assertThat(result.successLine()).isEqualTo(2);
		assertThat(port.wholeCalls).hasSize(2);
	}

	@Test
	void partialSplitColumnsAfterWhole_wholeShipsRowFailsValidation() {
		List<Map<String, Object>> rows =
				List.of(
						wholeRow("9003", "YD1", "顺丰"),
						row("9003", "YD1", "顺丰", "11", null, null));

		NormalOrdersUploadImportResult result = importRows(rows);

		assertThat(port.wholeCalls).hasSize(1);
		assertThat(result.failures()).hasSize(1);
		assertThat(result.failures().get(0).message())
				.isEqualTo("拆单信息必须同时填写子订单号、商品编码和发货数量");
	}

	@Test
	void splitFieldFormatErrors() {
		assertSingleFailure(
				splitRow("9003", "YD1", "顺丰", "11", "BN-1", "1.0"),
				"发货数量必须是正整数");
		assertSingleFailure(
				splitRow("9003", "YD1", "顺丰", "1.0", "BN-1", "1"),
				"子订单号格式错误");
	}

	@Test
	void differentDeliveryCorpInSegment_noDeliverSep() {
		Map<String, Object> row1 = splitRow("9003", "YD1", "顺丰", "11", "BN-1", "1");
		Map<String, Object> row2 = splitRow("9003", "YD1", "圆通", "12", "BN-2", "1");

		NormalOrdersUploadImportResult result = importRows(List.of(row1, row2));

		assertThat(port.sepCalls).isEmpty();
		assertThat(result.failures()).hasSize(2);
		assertThat(failureMessage(result, row1)).isEqualTo("同一包裹存在错误行，本次未发货");
		assertThat(failureMessage(result, row2)).isEqualTo("同一包裹快递公司不一致");
	}

	@Test
	void validatorReturnsMixedErrors_noDeliverSep() {
		Map<String, Object> row1 = splitRow("9003", "YD1", "顺丰", "11", "BN-1", "1");
		Map<String, Object> row2 = splitRow("9003", "YD1", "顺丰", "12", "BN-2", "1");
		when(validator.validate(anyLong(), anyLong(), anyList()))
				.thenReturn(Arrays.asList("子订单号不属于该订单", null));

		NormalOrdersUploadImportResult result = importRows(List.of(row1, row2));

		assertThat(port.sepCalls).isEmpty();
		assertThat(failureMessage(result, row1)).isEqualTo("子订单号不属于该订单");
		assertThat(failureMessage(result, row2)).isEqualTo("同一包裹存在错误行，本次未发货");
	}

	@Test
	void rowSupplierIdIgnored_commandUsesMethodSupplierId() {
		Map<String, Object> row = wholeRow("9003", "YD1", "顺丰");
		row.put("supplier_id", 99);

		importRows(0L, row);

		assertThat(port.wholeCalls).hasSize(1);
		assertThat(port.wholeCalls.get(0).supplierId()).isEqualTo(0L);
	}

	@Test
	void orderAndDeliveryValidationErrors() {
		assertSingleFailure(row("", "YD1", "顺丰", null, null, null), "订单号错误");
		assertSingleFailure(row("abc", "YD1", "顺丰", null, null, null), "订单号格式错误");
		assertSingleFailure(row("9003", "", "顺丰", null, null, null), "缺少快递信息");
	}

	@Test
	void splitDeliveryQtyAboveIntMax_failsRowLaterWholeStillDelivered() {
		String overIntMax = String.valueOf((long) Integer.MAX_VALUE + 1L);
		Map<String, Object> badRow = splitRow("9003", "YD1", "顺丰", "11", "BN-1", overIntMax);
		Map<String, Object> goodRow = wholeRow("9004", "YD2", "顺丰");

		NormalOrdersUploadImportResult result = importRows(List.of(badRow, goodRow));

		assertThat(failureMessage(result, badRow)).isEqualTo("发货数量必须是正整数");
		assertThat(port.wholeCalls).hasSize(1);
		assertThat(port.wholeCalls.get(0).orderId()).isEqualTo(9004L);
		assertThat(result.successLine()).isEqualTo(1);
	}

	@Test
	void splitOrderItemIdAboveLongMax_failsRowLaterSplitStillDelivered() {
		Map<String, Object> badRow =
				splitRow("9003", "YD1", "顺丰", "9223372036854775808", "BN-1", "1");
		Map<String, Object> goodRow = splitRow("9004", "YD2", "顺丰", "21", "BN-3", "1");

		NormalOrdersUploadImportResult result = importRows(List.of(badRow, goodRow));

		assertThat(failureMessage(result, badRow)).isEqualTo("子订单号格式错误");
		assertThat(port.sepCalls).hasSize(1);
		assertThat(port.sepCalls.get(0).command().orderId()).isEqualTo(9004L);
		assertThat(result.successLine()).isEqualTo(1);
	}

	@Test
	void validateThrows_openSegmentRowsFailLaterSegmentStillDelivered() {
		Map<String, Object> seg1Row = splitRow("9003", "YD1", "顺丰", "11", "BN-1", "1");
		Map<String, Object> seg2Row = splitRow("9004", "YD2", "顺丰", "21", "BN-3", "1");

		when(validator.validate(anyLong(), anyLong(), anyList()))
				.thenThrow(new ResourceException("校验服务不可用"))
				.thenAnswer(
						inv -> {
							@SuppressWarnings("unchecked")
							List<Map<String, Object>> segment = inv.getArgument(2);
							return segment.stream().map(r -> (String) null).collect(Collectors.toList());
						});

		NormalOrdersUploadImportResult result = importRows(List.of(seg1Row, seg2Row));

		assertThat(port.sepCalls).hasSize(1);
		assertThat(port.sepCalls.get(0).command().orderId()).isEqualTo(9004L);
		assertThat(failureMessage(result, seg1Row)).isEqualTo("校验服务不可用");
		assertThat(result.successLine()).isEqualTo(1);
	}

	@Test
	void secondSegmentValidatorFails_deliverSepOnlyOnce() {
		Map<String, Object> seg1Row1 = splitRow("9003", "YD1", "顺丰", "11", "BN-1", "1");
		Map<String, Object> seg1Row2 = splitRow("9003", "YD1", "顺丰", "12", "BN-2", "1");
		Map<String, Object> seg2Row1 = splitRow("9004", "YD2", "顺丰", "21", "BN-3", "1");

		when(validator.validate(anyLong(), anyLong(), anyList()))
				.thenReturn(Arrays.asList(null, null))
				.thenReturn(List.of("订单不存在"));

		NormalOrdersUploadImportResult result = importRows(List.of(seg1Row1, seg1Row2, seg2Row1));

		assertThat(port.sepCalls).hasSize(1);
		assertThat(result.successLine()).isEqualTo(2);
		assertThat(result.failures()).hasSize(1);
		assertThat(result.failures().get(0).message()).isEqualTo("订单不存在");
	}

	private NormalOrdersUploadImportResult importRows(List<Map<String, Object>> rows) {
		return importRows(0L, rows);
	}

	private NormalOrdersUploadImportResult importRows(long supplierId, Map<String, Object> row) {
		return importRows(supplierId, List.of(row));
	}

	private NormalOrdersUploadImportResult importRows(long supplierId, List<Map<String, Object>> rows) {
		return service.importRows(
				COMPANY_ID, OPERATOR_ID, 0L, supplierId, 0L, rows, OPERATOR_TYPE);
	}

	private void assertSingleFailure(Map<String, Object> row, String message) {
		NormalOrdersUploadImportResult result = importRows(List.of(row));
		assertThat(result.successLine()).isZero();
		assertThat(result.failures()).hasSize(1);
		assertThat(result.failures().get(0).message()).isEqualTo(message);
	}

	private static String failureMessage(NormalOrdersUploadImportResult result, Map<String, Object> row) {
		return result.failures().stream()
				.filter(f -> f.row() == row)
				.map(RowFailure::message)
				.findFirst()
				.orElseThrow();
	}

	private static Map<String, Object> wholeRow(String orderId, String deliveryCode, String corp) {
		return row(orderId, deliveryCode, corp, null, null, null);
	}

	private static Map<String, Object> splitRow(
			String orderId,
			String deliveryCode,
			String corp,
			String orderItemId,
			String itemBn,
			String deliveryNum) {
		return row(orderId, deliveryCode, corp, orderItemId, itemBn, deliveryNum);
	}

	private static Map<String, Object> row(
			String orderId,
			String deliveryCode,
			String corp,
			String orderItemId,
			String itemBn,
			String deliveryNum) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("order_id", orderId);
		m.put("delivery_code", deliveryCode);
		m.put("delivery_corp_name", corp);
		if (orderItemId != null) {
			m.put("order_item_id", orderItemId);
		}
		if (itemBn != null) {
			m.put("item_bn", itemBn);
		}
		if (deliveryNum != null) {
			m.put("delivery_num", deliveryNum);
		}
		return m;
	}

	static final class RecordingPort implements NormalOrdersUploadDeliveryPort {
		final List<UploadShipCommand> wholeCalls = new ArrayList<>();
		final List<DeliverSepCall> sepCalls = new ArrayList<>();
		Consumer<UploadShipCommand> wholeAction = cmd -> {};

		record DeliverSepCall(UploadShipCommand command, List<UploadSepItem> items) {}

		@Override
		public void deliverWhole(UploadShipCommand command) {
			wholeCalls.add(command);
			wholeAction.accept(command);
		}

		@Override
		public void deliverSep(UploadShipCommand command, List<UploadSepItem> items) {
			sepCalls.add(new DeliverSepCall(command, List.copyOf(items)));
		}
	}
}
