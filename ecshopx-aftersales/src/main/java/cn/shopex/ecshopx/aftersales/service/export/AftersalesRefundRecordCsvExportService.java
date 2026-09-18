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

package cn.shopex.ecshopx.aftersales.service.export;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundListService;
import cn.shopex.ecshopx.common.port.order.OrderExportEmployeePurchaseInfoLookupPort;
import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AftersalesRefundRecordCsvExportService {

	private static final int PAGE_SIZE = 500;
	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter CSV_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(SHANGHAI);
	private static final Pattern NUMERIC_ONLY = Pattern.compile("^[0-9]+$");

	private final AftersalesRefundMapper aftersalesRefundMapper;
	private final AftersalesMapper aftersalesMapper;
	private final ExportCsvFileService exportCsvFileService;
	private final AftersalesRefundListService aftersalesRefundListService;
	private final ObjectProvider<OrderExportEmployeePurchaseInfoLookupPort> employeePurchaseInfoLookupPort;

	public AftersalesRefundRecordCsvExportService(
			AftersalesRefundMapper aftersalesRefundMapper,
			AftersalesMapper aftersalesMapper,
			ExportCsvFileService exportCsvFileService,
			AftersalesRefundListService aftersalesRefundListService,
			ObjectProvider<OrderExportEmployeePurchaseInfoLookupPort> employeePurchaseInfoLookupPort) {
		this.aftersalesRefundMapper = aftersalesRefundMapper;
		this.aftersalesMapper = aftersalesMapper;
		this.exportCsvFileService = exportCsvFileService;
		this.aftersalesRefundListService = aftersalesRefundListService;
		this.employeePurchaseInfoLookupPort = employeePurchaseInfoLookupPort;
	}

	public Optional<Map<String, String>> runExport(
			LinkedHashMap<String, Object> filter, long companyId, @SuppressWarnings("unused") long operatorId) {
		LinkedHashMap<String, String> title = new LinkedHashMap<>();
		title.put("distributor_name", "店铺名称");
		title.put("shop_code", "店铺号");
		title.put("refund_bn", "退款单号");
		title.put("aftersales_bn", "售后单号");
		title.put("order_id", "订单号");
		title.put("trade_no", "订单序号");
		title.put("refund_type", "退款类型");
		title.put("refund_channel", "退款方式");
		title.put("refund_status", "退款状态");
		title.put("refund_fee", "应退商品金额");
		title.put("refunded_fee", "实退商品金额");
		title.put("refund_point", "退款商品积分");
		title.put("refund_freight_fee", "退款运费金额（¥）");
		title.put("refund_freight_point", "退款运费（积分）");
		title.put("create_time", "创建时间");
		title.put("refund_success_time", "退款成功时间");
		title.put("purchase_mode_desc", "购买方式");
		title.put("employee_purchase_activity_name", "企业购活动名称");
		title.put("employee_purchase_activity_id", "企业购活动ID");

		LambdaQueryWrapper<AftersalesRefund> w = aftersalesRefundListService.buildRefundLogExportWrapper(filter, companyId);
		w.orderByDesc(AftersalesRefund::getCreateTime).orderByDesc(AftersalesRefund::getRefundBn);

		List<Map<String, String>> rows = new ArrayList<>();
		int pageNo = 1;
		while (true) {
			Page<AftersalesRefund> page = new Page<>(pageNo, PAGE_SIZE);
			List<AftersalesRefund> records = aftersalesRefundMapper.selectPage(page, w).getRecords();
			if (records == null || records.isEmpty()) {
				break;
			}
			List<Map<String, Object>> batch = new ArrayList<>();
			for (AftersalesRefund r : records) {
				batch.add(new LinkedHashMap<>(aftersalesRefundListService.refundEntityToMap(r)));
			}
			aftersalesRefundListService.enrichDistributorInfo(companyId, batch);
			for (Map<String, Object> raw : batch) {
				flattenDistributorColumns(raw);
			}
			List<Long> orderIds = new ArrayList<>();
			for (Map<String, Object> raw : batch) {
				orderIds.add(longVal(raw.get("order_id")));
			}
			Map<Long, String> tradeIndex = buildTradeIndex(String.valueOf(companyId), orderIds);
			Map<Long, OrderExportEmployeePurchaseInfoLookupPort.Info> purchaseByOrder =
					loadEmployeePurchaseInfo(companyId, orderIds);
			for (Map<String, Object> raw : batch) {
				rows.add(toCsvRow(title, raw, tradeIndex, purchaseByOrder));
			}
			if (records.size() < PAGE_SIZE) {
				break;
			}
			pageNo++;
		}

		if (rows.isEmpty()) {
			return Optional.empty();
		}

		String fileBaseName = FILE_TS.format(Instant.now()) + "_退款单列表";
		Map<String, String> upload = exportCsvFileService.exportCsv(fileBaseName, title, rows);
		if (upload == null || upload.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(upload);
	}

	private Map<Long, String> buildTradeIndex(String companyId, List<Long> orderIds) {
		List<Long> distinct = orderIds.stream().filter(oid -> oid > 0L).distinct().toList();
		if (distinct.isEmpty()) {
			return Map.of();
		}
		List<Map<String, Object>> dbRows =
				aftersalesMapper.selectTradeIndexRowsForRefundExport(companyId, distinct);
		Map<Long, String> out = new LinkedHashMap<>();
		if (dbRows != null) {
			for (Map<String, Object> row : dbRows) {
				long oid = longVal(firstOf(row, "order_id", "orderId", "ORDER_ID"));
				String tn = stringOf(firstOf(row, "trade_no", "tradeNo", "TRADE_NO"));
				String display = (StringUtils.hasText(tn) && !"0".equals(tn)) ? tn : "-";
				out.put(oid, display);
			}
		}
		return out;
	}

	private static Object firstOf(Map<String, Object> row, String... keys) {
		for (String k : keys) {
			if (row.containsKey(k)) {
				return row.get(k);
			}
		}
		for (String k : keys) {
			for (String ek : row.keySet()) {
				if (ek != null && ek.equalsIgnoreCase(k)) {
					return row.get(ek);
				}
			}
		}
		return null;
	}

	private void flattenDistributorColumns(Map<String, Object> raw) {
		Object di = raw.get("distributor_info");
		if (di instanceof Map<?, ?> m) {
			Object name = m.get("name");
			raw.put("distributor_name", name == null ? "" : String.valueOf(name).trim());
			Object sc = m.get("shop_code");
			raw.put("shop_code", sc == null ? "" : String.valueOf(sc).trim());
		} else {
			raw.put("distributor_name", "平台自营");
			raw.put("shop_code", "");
		}
	}

	private Map<Long, OrderExportEmployeePurchaseInfoLookupPort.Info> loadEmployeePurchaseInfo(
			long companyId, List<Long> orderIds) {
		OrderExportEmployeePurchaseInfoLookupPort port = employeePurchaseInfoLookupPort.getIfAvailable();
		if (port == null || orderIds == null || orderIds.isEmpty()) {
			return Map.of();
		}
		return port.lookupByOrderIds(companyId, orderIds);
	}

	private Map<String, String> toCsvRow(
			LinkedHashMap<String, String> title,
			Map<String, Object> raw,
			Map<Long, String> tradeIdx,
			Map<Long, OrderExportEmployeePurchaseInfoLookupPort.Info> purchaseByOrder) {
		LinkedHashMap<String, String> row = new LinkedHashMap<>();
		long orderId = longVal(raw.get("order_id"));
		String tradeNo = tradeIdx.getOrDefault(orderId, "-");
		OrderExportEmployeePurchaseInfoLookupPort.Info purchaseInfo = purchaseByOrder.get(orderId);
		for (String k : title.keySet()) {
			row.put(k, cellForColumn(k, raw, tradeNo, purchaseInfo));
		}
		return row;
	}

	private String cellForColumn(
			String key,
			Map<String, Object> raw,
			String tradeNo,
			OrderExportEmployeePurchaseInfoLookupPort.Info purchaseInfo) {
		return switch (key) {
			case "distributor_name" -> stringOf(raw.get("distributor_name"));
			case "shop_code" -> stringOf(raw.get("shop_code"));
			case "refund_bn" -> textFieldCell(stringOf(raw.get("refund_bn")));
			case "aftersales_bn" -> textFieldCell(stringOf(raw.get("aftersales_bn")));
			case "order_id" -> textFieldCell(stringOf(raw.get("order_id")));
			case "trade_no" -> tradeNo != null ? tradeNo : "-";
			case "refund_type" -> refundTypeLabel(stringOf(raw.get("refund_type")));
			case "refund_channel" -> refundChannelLabel(stringOf(raw.get("refund_channel")));
			case "refund_status" -> refundStatusLabel(stringOf(raw.get("refund_status")));
			case "refund_fee" -> centsToYuanPlain(raw.get("refund_fee"));
			case "refunded_fee" -> centsToYuanPlain(raw.get("refunded_fee"));
			case "refund_point" -> stringOf(raw.get("refund_point"));
			case "refund_freight_fee" -> refundFreightFeeYuan(raw);
			case "refund_freight_point" -> refundFreightPoint(raw);
			case "create_time" -> formatEpochSeconds(raw.get("create_time"));
			case "refund_success_time" -> formatEpochSeconds(raw.get("refund_success_time"));
			case "purchase_mode_desc" -> purchaseModeDesc(purchaseInfo);
			case "employee_purchase_activity_name" ->
					purchaseInfo == null || purchaseInfo.activityName() == null
							? ""
							: purchaseInfo.activityName();
			case "employee_purchase_activity_id" ->
					purchaseInfo == null || purchaseInfo.activityId() == null
							? ""
							: String.valueOf(purchaseInfo.activityId());
			default -> "";
		};
	}

	private static String purchaseModeDesc(OrderExportEmployeePurchaseInfoLookupPort.Info purchaseInfo) {
		if (purchaseInfo == null) {
			return "";
		}
		String mode = purchaseInfo.purchaseMode() == null ? "" : purchaseInfo.purchaseMode();
		return switch (mode) {
			case "prepaid_point" -> "预充点数";
			case "cash" -> "现金";
			default -> "";
		};
	}

	private static String refundTypeLabel(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "--";
		}
		return switch (raw.trim()) {
			case "0" -> "售后";
			case "1" -> "售前";
			case "2" -> "拒单";
			default -> "--";
		};
	}

	private static String refundChannelLabel(Object v) {
		String raw = stringOf(v);
		if (!StringUtils.hasText(raw)) {
			return "--";
		}
		return switch (raw) {
			case "offline" -> "线下退回";
			case "original" -> "原路退回";
			default -> "--";
		};
	}

	private static String refundStatusLabel(Object v) {
		String s = stringOf(v);
		if (!StringUtils.hasText(s)) {
			return "--";
		}
		return switch (s) {
			case "AUDIT_SUCCESS" -> "审核成功待退款";
			case "SUCCESS" -> "退款成功";
			case "REFUSE" -> "退款驳回";
			case "CANCEL" -> "撤销退款";
			case "REFUNDCLOSE" -> "退款关闭";
			case "PROCESSING" -> "已发起退款等待到账";
			case "CHANGE" -> "退款异常";
			default -> "--";
		};
	}

	private static String refundFreightFeeYuan(Map<String, Object> raw) {
		String ft = stringOf(raw.get("freight_type"));
		if ("cash".equals(ft) && raw.get("freight") != null) {
			return centsToYuanPlain(raw.get("freight"));
		}
		return "0";
	}

	private static String refundFreightPoint(Map<String, Object> raw) {
		String ft = stringOf(raw.get("freight_type"));
		if ("point".equals(ft) && raw.get("freight") != null) {
			return stringOf(raw.get("freight"));
		}
		return "0";
	}

	private static String centsToYuanPlain(Object v) {
		if (v == null) {
			return "";
		}
		BigDecimal cents;
		if (v instanceof BigDecimal bd) {
			cents = bd;
		} else if (v instanceof Number n) {
			cents = BigDecimal.valueOf(n.longValue());
		} else {
			try {
				cents = new BigDecimal(String.valueOf(v).trim());
			} catch (NumberFormatException e) {
				return "";
			}
		}
		if (cents.compareTo(BigDecimal.ZERO) == 0) {
			return "0";
		}
		return cents.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).toPlainString();
	}

	private static String formatEpochSeconds(Object v) {
		if (v == null) {
			return "";
		}
		long sec;
		if (v instanceof Number n) {
			sec = n.longValue();
		} else {
			try {
				sec = Long.parseLong(String.valueOf(v).trim());
			} catch (NumberFormatException e) {
				return "";
			}
		}
		if (sec == 0L) {
			return "";
		}
		return ZonedDateTime.ofInstant(Instant.ofEpochSecond(sec), SHANGHAI).format(CSV_TIME);
	}

	private static String textFieldCell(String value) {
		if (value.isEmpty()) {
			return "";
		}
		if (NUMERIC_ONLY.matcher(value).matches()) {
			return "\t" + value;
		}
		return value;
	}

	private static String stringOf(Object v) {
		if (v == null) {
			return "";
		}
		return String.valueOf(v).trim();
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
