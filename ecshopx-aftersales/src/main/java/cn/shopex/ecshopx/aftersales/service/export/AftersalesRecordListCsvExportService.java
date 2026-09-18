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
import cn.shopex.ecshopx.common.distribution.DistributorGetInfoSimpleByDistributorIdPort;
import cn.shopex.ecshopx.common.port.order.OrderExportEmployeePurchaseInfoLookupPort;
import cn.shopex.ecshopx.common.port.supplier.SupplierOperatorInfoReadPort;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AftersalesRecordListCsvExportService {

	private static final int PAGE_SIZE = 500;
	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter CSV_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(SHANGHAI);
	private static final Pattern NUMERIC_ONLY = Pattern.compile("^[0-9]+$");

	private static final Map<String, String> AFTERSALES_TYPE = Map.of(
			"ONLY_REFUND", "仅退款",
			"REFUND_GOODS", "退货退款",
			"EXCHANGING_GOODS", "换货");

	private static final Map<Integer, String> AFTERSALES_STATUS = Map.of(
			0, "待处理",
			1, "处理中",
			2, "已处理",
			3, "已驳回",
			4, "已关闭");

	private static final Map<Integer, String> PROGRESS = Map.ofEntries(
			Map.entry(0, "等待商家处理"),
			Map.entry(1, "商家接受申请，等待消费者回寄"),
			Map.entry(2, "消费者回寄，等待商家收货确认"),
			Map.entry(3, "已驳回"),
			Map.entry(4, "已处理"),
			Map.entry(5, "退款驳回"),
			Map.entry(6, "退款完成"),
			Map.entry(7, "售后关闭"),
			Map.entry(8, "商家确认收货,等待审核退款"),
			Map.entry(9, "退款处理中"));

	private static final Map<String, String> ORDER_HOLDER = Map.of(
			"self", "自营订单",
			"distributor", "商家订单",
			"supplier", "供应商订单",
			"self_supplier", "自营+供应商订单",
			"distributor_supplier", "商家+供应商订单");

	private final AftersalesMapper aftersalesMapper;
	private final AftersalesRefundMapper aftersalesRefundMapper;
	private final ExportCsvFileService exportCsvFileService;
	private final ShopMenuService shopMenuService;
	private final SupplierOperatorInfoReadPort supplierOperatorInfoReadPort;
	private final DistributorGetInfoSimpleByDistributorIdPort distributorGetInfoSimpleByDistributorIdPort;
	private final OperatorsMapper operatorsMapper;
	private final ObjectProvider<OrderExportEmployeePurchaseInfoLookupPort> employeePurchaseInfoLookupPort;

	public AftersalesRecordListCsvExportService(
			AftersalesMapper aftersalesMapper,
			AftersalesRefundMapper aftersalesRefundMapper,
			ExportCsvFileService exportCsvFileService,
			ShopMenuService shopMenuService,
			SupplierOperatorInfoReadPort supplierOperatorInfoReadPort,
			DistributorGetInfoSimpleByDistributorIdPort distributorGetInfoSimpleByDistributorIdPort,
			OperatorsMapper operatorsMapper,
			ObjectProvider<OrderExportEmployeePurchaseInfoLookupPort> employeePurchaseInfoLookupPort) {
		this.aftersalesMapper = aftersalesMapper;
		this.aftersalesRefundMapper = aftersalesRefundMapper;
		this.exportCsvFileService = exportCsvFileService;
		this.shopMenuService = shopMenuService;
		this.supplierOperatorInfoReadPort = supplierOperatorInfoReadPort;
		this.distributorGetInfoSimpleByDistributorIdPort = distributorGetInfoSimpleByDistributorIdPort;
		this.operatorsMapper = operatorsMapper;
		this.employeePurchaseInfoLookupPort = employeePurchaseInfoLookupPort;
	}

	public Optional<Map<String, String>> runExport(LinkedHashMap<String, Object> filter, long companyId, long operatorId) {
		String productModel = shopMenuService.resolveProductModelKeyForCompany(companyId);
		boolean showSalesmanColumn = "standard".equalsIgnoreCase(productModel);

		LinkedHashMap<String, String> title = new LinkedHashMap<>();
		title.put("distributor_name", "店铺名称");
		title.put("shop_code", "店铺号");
		title.put("aftersales_bn", "售后单号");
		title.put("order_id", "订单号");
		title.put("trade_no", "订单序号");
		title.put("item_bn", "商品编号");
		title.put("item_name", "商品名称");
		title.put("num", "数量");
		title.put("aftersales_type", "售后类型");
		title.put("aftersales_status", "售后状态");
		title.put("create_time", "创建时间");
		title.put("refund_fee", "退款商品金额");
		title.put("refund_point", "退款抵扣积分");
		title.put("refund_freight_fee", "退款运费金额（¥）");
		title.put("refund_freight_point", "退款运费（积分）");
		title.put("refunded_fee", "实退金额");
		title.put("refunded_point", "实退积分");
		title.put("progress", "处理进度");
		title.put("description", "申请描述");
		title.put("reason", "申请售后原因");
		title.put("refuse_reason", "拒绝原因");
		title.put("memo", "售后备注");
		if (showSalesmanColumn) {
			title.put("salesman_name", "导购");
		}
		title.put("order_holder", "订单分类");
		title.put("purchase_mode_desc", "购买方式");
		title.put("employee_purchase_activity_name", "企业购活动名称");
		title.put("employee_purchase_activity_id", "企业购活动ID");
		title.put("supplier_name", "来源供应商");
		title.put("self_delivery_operator_name", "配送员");

		List<Map<String, String>> rows = new ArrayList<>();
		int pageNo = 1;
		while (true) {
			long offset = (pageNo - 1L) * PAGE_SIZE;
			List<Map<String, Object>> page = aftersalesMapper.selectRecordListExportPage(filter, offset, PAGE_SIZE);
			if (page == null || page.isEmpty()) {
				break;
			}
			enrichAndAppendRows(companyId, page, rows, showSalesmanColumn, title);
			if (page.size() < PAGE_SIZE) {
				break;
			}
			pageNo++;
		}

		if (rows.isEmpty()) {
			return Optional.empty();
		}

		String fileBaseName = FILE_TS.format(Instant.now()) + "_售后列表";
		Map<String, String> upload = exportCsvFileService.exportCsv(fileBaseName, title, rows);
		if (upload == null || upload.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(upload);
	}

	private void enrichAndAppendRows(
			long companyId,
			List<Map<String, Object>> page,
			List<Map<String, String>> out,
			boolean showSalesmanColumn,
			LinkedHashMap<String, String> title) {
		List<Long> orderIds = new ArrayList<>();
		List<Long> aftersalesBns = new ArrayList<>();
		Set<Long> distributorIds = new LinkedHashSet<>();
		Set<Long> supplierIds = new LinkedHashSet<>();
		Set<Long> operatorIds = new LinkedHashSet<>();
		for (Map<String, Object> row : page) {
			long oid = longVal(row.get("order_id"));
			if (oid > 0L) {
				orderIds.add(oid);
			}
			long bn = longVal(row.get("aftersales_bn"));
			if (bn > 0L) {
				aftersalesBns.add(bn);
			}
			long did = longVal(row.get("distributor_id"));
			if (did > 0L) {
				distributorIds.add(did);
			}
			long sid = longVal(row.get("supplier_id"));
			if (sid > 0L) {
				supplierIds.add(sid);
			}
			long opId = longVal(row.get("self_delivery_operator_id"));
			if (opId > 0L) {
				operatorIds.add(opId);
			}
		}
		List<Long> distinctOrderIds = orderIds.stream().distinct().toList();
		List<Long> distinctBns = aftersalesBns.stream().distinct().toList();

		Map<Long, Map<String, Object>> orderById = new HashMap<>();
		if (!distinctOrderIds.isEmpty()) {
			List<Map<String, Object>> ords = aftersalesMapper.selectNormalOrderHeadersByOrderIds(companyId, distinctOrderIds);
			if (ords != null) {
				for (Map<String, Object> o : ords) {
					orderById.put(longVal(o.get("order_id")), o);
				}
			}
		}

		Map<Long, String> tradeIndex = new HashMap<>();
		if (!distinctOrderIds.isEmpty()) {
			List<Map<String, Object>> trades =
					aftersalesMapper.selectTradeIndexRowsForRefundExport(String.valueOf(companyId), distinctOrderIds);
			if (trades != null) {
				for (Map<String, Object> t : trades) {
					long oid = longVal(firstOf(t, "order_id", "orderId", "ORDER_ID"));
					String tn = stringOf(firstOf(t, "trade_no", "tradeNo", "TRADE_NO"));
					tradeIndex.put(oid, StringUtils.hasText(tn) && !"0".equals(tn) ? tn : "-");
				}
			}
		}

		Map<Long, Map<String, Object>> distById = new HashMap<>();
		for (Long did : distributorIds) {
			distById.put(did, distributorGetInfoSimpleByDistributorIdPort.getInfoSimpleByDistributorId(companyId, did));
		}

		Map<Long, String> supplierName = new HashMap<>();
		for (Long sid : supplierIds) {
			Map<String, Object> inf = supplierOperatorInfoReadPort.getInfo(companyId, sid);
			Object sn = inf == null ? null : inf.get("supplier_name");
			supplierName.put(sid, sn == null ? "" : String.valueOf(sn).trim());
		}

		Map<Long, AftersalesRefund> refundByBn = new HashMap<>();
		if (!distinctBns.isEmpty()) {
			List<AftersalesRefund> refunds =
					aftersalesRefundMapper.selectList(
							new LambdaQueryWrapper<AftersalesRefund>()
									.eq(AftersalesRefund::getCompanyId, companyId)
									.in(AftersalesRefund::getAftersalesBn, distinctBns));
			if (refunds != null) {
				for (AftersalesRefund r : refunds) {
					if (r.getAftersalesBn() != null) {
						refundByBn.putIfAbsent(r.getAftersalesBn(), r);
					}
				}
			}
		}

		Set<Long> salesmanIds = new LinkedHashSet<>();
		if (showSalesmanColumn) {
			for (Map<String, Object> ord : orderById.values()) {
				long sm = longVal(ord.get("salesman_id"));
				if (sm > 0L) {
					salesmanIds.add(sm);
				}
			}
		}
		Map<Long, String> workUserBySalesId = new HashMap<>();
		if (showSalesmanColumn && !salesmanIds.isEmpty()) {
			List<Map<String, Object>> sprows =
					aftersalesMapper.selectShoppingGuideWorkUseridBySalespersonIds(
							companyId, new ArrayList<>(salesmanIds));
			if (sprows != null) {
				for (Map<String, Object> sp : sprows) {
					workUserBySalesId.put(
							longVal(sp.get("salesperson_id")),
							sp.get("work_userid") == null ? "" : String.valueOf(sp.get("work_userid")));
				}
			}
		}

		Map<Long, String> operatorNameById = new HashMap<>();
		if (!operatorIds.isEmpty()) {
			List<Operators> ops =
					operatorsMapper.selectList(
							new LambdaQueryWrapper<Operators>().in(Operators::getOperatorId, operatorIds));
			if (ops != null) {
				for (Operators op : ops) {
					if (op.getOperatorId() != null) {
						operatorNameById.put(
								op.getOperatorId(), op.getUsername() == null ? "" : op.getUsername().trim());
					}
				}
			}
		}

		Map<Long, OrderExportEmployeePurchaseInfoLookupPort.Info> purchaseByOrder = loadEmployeePurchaseInfo(
				companyId, distinctOrderIds);

		for (Map<String, Object> value : page) {
			long oid = longVal(value.get("order_id"));
			long bn = longVal(value.get("aftersales_bn"));
			long did = longVal(value.get("distributor_id"));
			long sid = longVal(value.get("supplier_id"));
			long opId = longVal(value.get("self_delivery_operator_id"));
			Map<String, Object> orderInfo = orderById.getOrDefault(oid, Map.of());
			AftersalesRefund refundData = refundByBn.get(bn);

			String distributorName;
			String shopCode;
			if (did > 0L) {
				Map<String, Object> di = distById.get(did);
				if (di != null && !di.isEmpty()) {
					distributorName = stringOf(di.get("name"));
					shopCode = stringOf(di.get("shop_code"));
				} else {
					distributorName = "平台自营";
					shopCode = "-";
				}
			} else {
				distributorName = "平台自营";
				shopCode = "-";
			}

			LinkedHashMap<String, String> row = new LinkedHashMap<>();
			for (String k : title.keySet()) {
				row.put(k, cellForColumn(k, value, distributorName, shopCode, tradeIndex.getOrDefault(oid, "-"),
						orderInfo, refundData, workUserBySalesId, supplierName.getOrDefault(sid, ""),
						operatorNameById.getOrDefault(opId, ""), purchaseByOrder.get(oid)));
			}
			out.add(row);
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

	private static String cellForColumn(
			String key,
			Map<String, Object> value,
			String distributorName,
			String shopCode,
			String tradeNo,
			Map<String, Object> orderInfo,
			AftersalesRefund refundData,
			Map<Long, String> workUserBySalesId,
			String supplierName,
			String operatorName,
			OrderExportEmployeePurchaseInfoLookupPort.Info purchaseInfo) {
		return switch (key) {
			case "distributor_name" -> distributorName;
			case "shop_code" -> shopCode;
			case "aftersales_bn" -> textFieldCell(stringOf(value.get("aftersales_bn")));
			case "order_id" -> textFieldCell(stringOf(value.get("order_id")));
			case "trade_no" -> tradeNo != null ? tradeNo : "-";
			case "item_bn" -> textFieldCell(stringOf(value.get("item_bn")));
			case "item_name" -> stringOf(value.get("item_name"));
			case "num" -> stringOf(value.get("num"));
			case "aftersales_type" -> {
				String raw = stringOf(value.get("aftersales_type"));
				yield AFTERSALES_TYPE.getOrDefault(raw, "--");
			}
			case "aftersales_status" -> {
				Integer st = intOrNull(value.get("aftersales_status"));
				yield st == null ? "--" : AFTERSALES_STATUS.getOrDefault(st, "--");
			}
			case "create_time" -> formatEpochSeconds(value.get("create_time"));
			case "refund_fee" -> centsToYuanDiv(value.get("refund_fee"));
			case "refund_point" -> {
				if (refundData != null) {
					yield String.valueOf(refundData.getRefundPoint() == null ? 0 : refundData.getRefundPoint());
				}
				yield "--";
			}
			case "refund_freight_fee" -> {
				if (refundData != null
						&& "cash".equals(stringOf(refundData.getFreightType()))
						&& refundData.getFreight() != null) {
					yield centsToYuanDiv(refundData.getFreight());
				}
				yield "0";
			}
			case "refund_freight_point" -> {
				if (refundData != null
						&& "point".equals(stringOf(refundData.getFreightType()))
						&& refundData.getFreight() != null) {
					yield String.valueOf(refundData.getFreight());
				}
				yield "0";
			}
			case "refunded_fee" -> {
				if (refundData != null) {
					yield centsToYuanDiv(refundData.getRefundedFee() == null ? 0 : refundData.getRefundedFee());
				}
				yield "--";
			}
			case "refunded_point" -> {
				if (refundData != null) {
					yield String.valueOf(refundData.getRefundedPoint() == null ? 0 : refundData.getRefundedPoint());
				}
				yield "--";
			}
			case "progress" -> {
				Integer p = intOrNull(value.get("progress"));
				yield p == null ? "--" : PROGRESS.getOrDefault(p, "--");
			}
			case "description" -> stringOf(value.get("description"));
			case "reason" -> stringOf(value.get("reason"));
			case "refuse_reason" -> stringOf(value.get("refuse_reason"));
			case "memo" -> stringOf(value.get("memo"));
			case "salesman_name" -> {
				long sm = longVal(orderInfo.get("salesman_id"));
				yield workUserBySalesId.getOrDefault(sm, "");
			}
			case "order_holder" -> {
				String oh = stringOf(orderInfo.get("order_holder"));
				yield ORDER_HOLDER.getOrDefault(oh, oh);
			}
			case "purchase_mode_desc" -> purchaseModeDesc(purchaseInfo);
			case "employee_purchase_activity_name" ->
					purchaseInfo == null || purchaseInfo.activityName() == null
							? ""
							: purchaseInfo.activityName();
			case "employee_purchase_activity_id" ->
					purchaseInfo == null || purchaseInfo.activityId() == null
							? ""
							: String.valueOf(purchaseInfo.activityId());
			case "supplier_name" -> supplierName;
			case "self_delivery_operator_name" -> operatorName;
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

	private static String centsToYuanDiv(Object cents) {
		if (cents == null) {
			return "";
		}
		long v;
		if (cents instanceof Number n) {
			v = n.longValue();
		} else {
			try {
				v = Long.parseLong(String.valueOf(cents).trim());
			} catch (NumberFormatException e) {
				return "";
			}
		}
		if (v == 0L) {
			return "0";
		}
		return BigDecimal.valueOf(v).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).stripTrailingZeros()
				.toPlainString();
	}

	private static Integer intOrNull(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String stringOf(Object v) {
		if (v == null) {
			return "";
		}
		return String.valueOf(v).trim();
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
