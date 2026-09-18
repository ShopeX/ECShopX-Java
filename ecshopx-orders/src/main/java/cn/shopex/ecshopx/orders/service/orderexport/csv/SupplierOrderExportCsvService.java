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

package cn.shopex.ecshopx.orders.service.orderexport.csv;

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.NormalOrdersRelSupplier;
import cn.shopex.ecshopx.orders.domain.NormalOrdersRelZiti;
import cn.shopex.ecshopx.orders.domain.OrdersDelivery;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersRelSupplierMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersRelZitiMapper;
import cn.shopex.ecshopx.orders.mapper.OrdersDeliveryMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.orderexport.support.NormalOrderExportDistributorLookupService;
import cn.shopex.ecshopx.orders.service.orderexport.support.OrderExportSupplierOrderQuerySupport;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SupplierOrderExportCsvService {

	private static final int PAGE_SIZE = 500;
	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter FILE_TS =
			DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(SHANGHAI);
	private static final DateTimeFormatter CSV_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static final Map<String, String> ORDER_CLASS_LABELS =
			Map.ofEntries(
					Map.entry("community", "社区活动订单"),
					Map.entry("groups", "拼团活动订单"),
					Map.entry("seckill", "秒杀活动订单"),
					Map.entry("normal", "普通订单"),
					Map.entry("drug", "药品需求订单"),
					Map.entry("shopguide", "代客下单订单"),
					Map.entry("pointsmall", "积分商城订单"),
					Map.entry("bargain", "砍价订单"),
					Map.entry("excard", "兑换券订单"),
					Map.entry("shopadmin", "门店订单"));

	private static final Map<String, String> PAY_TYPE_LABELS =
			Map.ofEntries(
					Map.entry("wxpay", "微信支付"),
					Map.entry("wxpaypc", "微信支付"),
					Map.entry("wxpayh5", "微信支付"),
					Map.entry("wxpayjs", "微信支付"),
					Map.entry("wxpayapp", "微信支付"),
					Map.entry("wxpaypos", "微信支付"),
					Map.entry("hfpay", "微信支付"),
					Map.entry("adapay", "微信支付"),
					Map.entry("alipay", "支付宝"),
					Map.entry("alipayh5", "支付宝"),
					Map.entry("alipayapp", "支付宝"),
					Map.entry("alipaypos", "支付宝"),
					Map.entry("point", "积分支付"),
					Map.entry("deposit", "余额支付"),
					Map.entry("pos", "现金支付"),
					Map.entry("gat", "关爱通支付"),
					Map.entry("offline", "线下转账"));

	private final SupplierOrderMapper supplierOrderMapper;
	private final OrdersDeliveryMapper ordersDeliveryMapper;
	private final TradeMapper tradeMapper;
	private final NormalOrdersRelZitiMapper normalOrdersRelZitiMapper;
	private final NormalOrdersRelSupplierMapper normalOrdersRelSupplierMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final NormalOrderExportDistributorLookupService distributorLookupService;
	private final ExportCsvFileService exportCsvFileService;
	private final ObjectMapper objectMapper;

	public SupplierOrderExportCsvService(
			SupplierOrderMapper supplierOrderMapper,
			OrdersDeliveryMapper ordersDeliveryMapper,
			TradeMapper tradeMapper,
			NormalOrdersRelZitiMapper normalOrdersRelZitiMapper,
			NormalOrdersRelSupplierMapper normalOrdersRelSupplierMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			NormalOrderExportDistributorLookupService distributorLookupService,
			ExportCsvFileService exportCsvFileService,
			ObjectMapper objectMapper) {
		this.supplierOrderMapper = supplierOrderMapper;
		this.ordersDeliveryMapper = ordersDeliveryMapper;
		this.tradeMapper = tradeMapper;
		this.normalOrdersRelZitiMapper = normalOrdersRelZitiMapper;
		this.normalOrdersRelSupplierMapper = normalOrdersRelSupplierMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.distributorLookupService = distributorLookupService;
		this.exportCsvFileService = exportCsvFileService;
		this.objectMapper = objectMapper;
	}

	public Optional<Map<String, String>> export(long companyId, LinkedHashMap<String, Object> filter) {
		LinkedHashMap<String, Object> work = new LinkedHashMap<>(filter);
		boolean datapassBlock = truthyDatapass(work.remove("datapass_block"));
		long supplierId = longVal(work.get("supplier_id"));

		LinkedHashMap<String, String> title = buildTitle();
		List<Map<String, String>> rows = new ArrayList<>();
		int pageNum = 1;
		while (true) {
			Page<SupplierOrder> page = new Page<>(pageNum, PAGE_SIZE);
			IPage<SupplierOrder> result =
					supplierOrderMapper.selectPage(
							page, OrderExportSupplierOrderQuerySupport.toListWrapper(companyId, work));
			List<SupplierOrder> list = result.getRecords();
			if (list == null || list.isEmpty()) {
				break;
			}
			List<Long> orderIds = list.stream().map(SupplierOrder::getOrderId).toList();
			Map<Long, OrdersDelivery> deliveryByOrder = loadDeliveryByOrder(supplierId, orderIds);
			Map<Long, Trade> successTradeByOrder = loadSuccessTrades(companyId, orderIds);
			List<Long> distributorIds =
					list.stream()
							.map(SupplierOrder::getDistributorId)
							.filter(id -> id != null && id > 0L)
							.distinct()
							.toList();
			Map<Long, NormalOrderExportDistributorLookupService.StoreInfo> stores =
					distributorLookupService.loadStores(companyId, distributorIds);
			Map<Long, NormalOrdersRelZiti> zitiByOrder = loadZitiByOrder(orderIds);
			Map<Long, Long> numByOrder = loadOrderSupplierNum(companyId, orderIds, supplierId);
			Map<Long, Long> freightByOrder = loadSupplierFreight(companyId, orderIds, supplierId);

			for (SupplierOrder o : list) {
				rows.add(
						buildRow(
								o,
								stores.getOrDefault(
										o.getDistributorId(), NormalOrderExportDistributorLookupService.StoreInfo.EMPTY),
								deliveryByOrder.get(o.getOrderId()),
								successTradeByOrder.get(o.getOrderId()),
								zitiByOrder.get(o.getOrderId()),
								numByOrder.getOrDefault(o.getOrderId(), 0L),
								freightByOrder.get(o.getOrderId()),
								datapassBlock));
			}
			if (list.size() < PAGE_SIZE) {
				break;
			}
			pageNum++;
		}
		if (rows.isEmpty()) {
			return Optional.empty();
		}
		String fileBaseName = FILE_TS.format(Instant.now()) + companyId + "supplier_order";
		Map<String, String> upload = exportCsvFileService.exportCsv(fileBaseName, title, rows);
		if (upload == null || upload.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(upload);
	}

	static LinkedHashMap<String, String> buildTitle() {
		LinkedHashMap<String, String> t = new LinkedHashMap<>();
		t.put("order_id", "订单号");
		t.put("store_name", "来源店铺");
		t.put("mobile", "会员手机号");
		t.put("create_time", "下单时间");
		t.put("num", "购买数量");
		t.put("cost_fee", "结算成本总价（¥）");
		t.put("freight_fee", "运费(总)");
		t.put("order_class", "订单类型");
		t.put("order_status", "订单状态");
		t.put("receipt_type", "收货方式");
		t.put("ziti_status", "自提状态");
		t.put("receiver_name", "收货人姓名");
		t.put("receiver_mobile", "收货人手机");
		t.put("receiver_zip", "收货人邮编");
		t.put("receiver_state", "收货人所在省份");
		t.put("receiver_city", "收货人所在城市");
		t.put("receiver_district", "收货人所在地区、县");
		t.put("receiver_address", "收货地址");
		t.put("delivery_status", "发货状态");
		t.put("delivery_time", "发货时间");
		t.put("end_time", "订单完成时间");
		t.put("delivery_code", "快递单号");
		t.put("delivery_corp", "快递公司");
		t.put("pay_type", "支付方式");
		t.put("pay_time", "支付时间");
		t.put("invoice", "发票内容");
		t.put("remark", "订单备注");
		t.put("pickup_address", "自提地址");
		t.put("pickup_datetime", "提货时间");
		return t;
	}

	private Map<String, String> buildRow(
			SupplierOrder o,
			NormalOrderExportDistributorLookupService.StoreInfo store,
			OrdersDelivery delivery,
			Trade trade,
			NormalOrdersRelZiti ziti,
			long num,
			Long freightCents,
			boolean datapassBlock) {
		String mobile = nz(o.getMobile());
		if (datapassBlock) {
			mobile = nz(DataMasking.maskMobile(mobile));
		}

		String deliveryCorp = nz(o.getDeliveryCorp());
		String deliveryCode = nz(o.getDeliveryCode());
		String deliveryTime = formatEpoch(o.getDeliveryTime());
		if (delivery != null) {
			deliveryCorp = nz(delivery.getDeliveryCorp());
			deliveryCode = nz(delivery.getDeliveryCode());
			deliveryTime = formatEpoch(delivery.getDeliveryTime());
		}

		String payType = nz(o.getPayType());
		LinkedHashMap<String, String> row = new LinkedHashMap<>();
		row.put("order_id", "'" + String.valueOf(o.getOrderId()));
		row.put("store_name", store == null ? "" : nz(store.name()));
		row.put("mobile", "'" + mobile);
		row.put("create_time", formatEpoch(o.getCreateTime()));
		row.put("num", String.valueOf(num));
		row.put("cost_fee", centsToYuan(o.getCostFee()));
		row.put("freight_fee", freightCents == null ? "0" : centsToYuan(freightCents));
		row.put(
				"order_class",
				ORDER_CLASS_LABELS.getOrDefault(nz(o.getOrderClass()), nz(o.getOrderClass())));
		row.put("order_status", resolveOrderStatusMsg(o));
		row.put("receipt_type", nz(o.getReceiptType()));
		row.put("ziti_status", nz(o.getZitiStatus()));
		row.put("receiver_name", nz(o.getReceiverName()));
		row.put("receiver_mobile", nz(o.getReceiverMobile()));
		row.put("receiver_zip", nz(o.getReceiverZip()));
		row.put("receiver_state", nz(o.getReceiverState()));
		row.put("receiver_city", nz(o.getReceiverCity()));
		row.put("receiver_district", nz(o.getReceiverDistrict()));
		row.put("receiver_address", nz(o.getReceiverAddress()));
		row.put("delivery_status", mapDeliveryStatus(o.getDeliveryStatus()));
		row.put("delivery_time", deliveryTime);
		row.put("end_time", formatEpochNumber(o.getEndTime()));
		row.put("delivery_code", deliveryCode);
		row.put("delivery_corp", deliveryCorp);
		row.put("pay_type", PAY_TYPE_LABELS.getOrDefault(payType, payType));
		row.put("pay_time", formatPayTime(trade));
		row.put("invoice", formatInvoiceCell(o.getInvoice()));
		row.put("remark", nz(o.getRemark()));
		row.put("pickup_address", formatPickupAddress(ziti));
		row.put("pickup_datetime", formatPickupDatetime(ziti));
		return row;
	}

	private Map<Long, OrdersDelivery> loadDeliveryByOrder(long supplierId, List<Long> orderIds) {
		if (orderIds.isEmpty()) {
			return Map.of();
		}
		LambdaQueryWrapper<OrdersDelivery> w =
				new LambdaQueryWrapper<OrdersDelivery>().in(OrdersDelivery::getOrderId, orderIds);
		if (supplierId > 0L) {
			w.eq(OrdersDelivery::getSupplierId, (int) supplierId);
		}
		List<OrdersDelivery> all = ordersDeliveryMapper.selectList(w);
		Map<Long, OrdersDelivery> out = new HashMap<>();
		for (OrdersDelivery d : all) {
			out.putIfAbsent(d.getOrderId(), d);
		}
		return out;
	}

	private Map<Long, Trade> loadSuccessTrades(long companyId, List<Long> orderIds) {
		if (orderIds.isEmpty()) {
			return Map.of();
		}
		List<String> idStrs = orderIds.stream().map(String::valueOf).toList();
		List<Trade> trades =
				tradeMapper.selectList(
						new LambdaQueryWrapper<Trade>()
								.eq(Trade::getCompanyId, String.valueOf(companyId))
								.in(Trade::getOrderId, idStrs)
								.eq(Trade::getTradeState, "SUCCESS"));
		Map<Long, Trade> out = new HashMap<>();
		for (Trade t : trades) {
			try {
				long oid = Long.parseLong(String.valueOf(t.getOrderId()).trim());
				out.putIfAbsent(oid, t);
			} catch (NumberFormatException ignored) {
			}
		}
		return out;
	}

	private Map<Long, NormalOrdersRelZiti> loadZitiByOrder(List<Long> orderIds) {
		if (orderIds.isEmpty()) {
			return Map.of();
		}
		List<NormalOrdersRelZiti> all =
				normalOrdersRelZitiMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersRelZiti>().in(NormalOrdersRelZiti::getOrderId, orderIds));
		Map<Long, NormalOrdersRelZiti> out = new HashMap<>();
		for (NormalOrdersRelZiti z : all) {
			out.putIfAbsent(z.getOrderId(), z);
		}
		return out;
	}

	private Map<Long, Long> loadOrderSupplierNum(long companyId, List<Long> orderIds, long supplierId) {
		if (orderIds.isEmpty()) {
			return Map.of();
		}
		LambdaQueryWrapper<NormalOrdersItems> w =
				new LambdaQueryWrapper<NormalOrdersItems>()
						.eq(NormalOrdersItems::getCompanyId, companyId)
						.in(NormalOrdersItems::getOrderId, orderIds);
		if (supplierId > 0L) {
			w.eq(NormalOrdersItems::getSupplierId, (int) supplierId);
		}
		List<NormalOrdersItems> items = normalOrdersItemsMapper.selectList(w);
		Map<Long, Long> out = new HashMap<>();
		for (NormalOrdersItems item : items) {
			long n = item.getNum() == null ? 0L : item.getNum().longValue();
			out.merge(item.getOrderId(), n, Long::sum);
		}
		return out;
	}

	private Map<Long, Long> loadSupplierFreight(long companyId, List<Long> orderIds, long supplierId) {
		if (orderIds.isEmpty()) {
			return Map.of();
		}
		LambdaQueryWrapper<NormalOrdersRelSupplier> w =
				new LambdaQueryWrapper<NormalOrdersRelSupplier>()
						.eq(NormalOrdersRelSupplier::getCompanyId, companyId)
						.in(NormalOrdersRelSupplier::getOrderId, orderIds);
		if (supplierId > 0L) {
			w.eq(NormalOrdersRelSupplier::getSupplierId, (int) supplierId);
		}
		List<NormalOrdersRelSupplier> rels = normalOrdersRelSupplierMapper.selectList(w);
		Map<Long, Long> out = new HashMap<>();
		for (NormalOrdersRelSupplier rel : rels) {
			long fee = rel.getFreightFee() == null ? 0L : rel.getFreightFee().longValue();
			out.merge(rel.getOrderId(), fee, Long::sum);
		}
		return out;
	}

	static String resolveOrderStatusMsg(SupplierOrder order) {
		String orderStatus = nz(order.getOrderStatus());
		String deliveryStatus = nz(order.getDeliveryStatus());
		return switch (orderStatus) {
			case "WAIT_GROUPS_SUCCESS" -> "等待成团";
			case "NOTPAY" -> "待支付";
			case "WAIT_PAID_CONFIRM" -> "支付待确认";
			case "PAYED" -> {
				if ("PARTAIL".equals(deliveryStatus)) {
					yield "部分发货";
				} else if ("DONE".equals(deliveryStatus)) {
					yield "待收货";
				} else {
					yield "待发货";
				}
			}
			case "REVIEW_PASS" -> {
				if ("PARTAIL".equals(deliveryStatus)) {
					yield "部分出库";
				} else {
					yield "审核完成,待出库";
				}
			}
			case "CANCEL" -> {
				if ("DONE".equals(deliveryStatus)) {
					yield "已关闭";
				} else {
					yield "已取消";
				}
			}
			case "WAIT_BUYER_CONFIRM" -> "待收货";
			case "DONE" -> "已完成";
			case "REFUND_PROCESS" -> "退款处理中";
			case "REFUND_SUCCESS" -> "已退款";
			case "PART_PAYMENT" -> "部分付款";
			default -> "订单异常:" + orderStatus;
		};
	}

	private static String mapDeliveryStatus(String deliveryStatus) {
		if ("DONE".equals(deliveryStatus)) {
			return "已发货";
		}
		if ("PARTAIL".equals(deliveryStatus)) {
			return "部分发货";
		}
		return "未发货";
	}

	private String formatInvoiceCell(String rawInvoice) {
		if (!StringUtils.hasText(rawInvoice)) {
			return "---无---";
		}
		try {
			JsonNode n = objectMapper.readTree(rawInvoice.trim());
			if (!n.isObject()) {
				return "---无---";
			}
			List<String> parts = new ArrayList<>();
			appendInvoicePart(parts, n, "title", "title");
			appendInvoicePart(parts, n, "registration_number", "税号");
			appendInvoicePart(parts, n, "content", "发票抬头");
			appendInvoicePart(parts, n, "company_address", "单位地址");
			appendInvoicePart(parts, n, "bankname", "开户银行");
			appendInvoicePart(parts, n, "bankaccount", "银行账户");
			appendInvoicePart(parts, n, "company_phone", "电话号码");
			if (parts.isEmpty()) {
				return "---无---";
			}
			return "\"" + String.join(";", parts) + "\"";
		} catch (Exception e) {
			return "---无---";
		}
	}

	private static void appendInvoicePart(List<String> parts, JsonNode n, String key, String label) {
		if (!n.has(key) || n.get(key).isNull()) {
			return;
		}
		String val = n.get(key).asText("");
		if ("title".equals(key)) {
			parts.add("title:" + val);
		} else {
			parts.add(label + ":" + val);
		}
	}

	private static String formatPickupAddress(NormalOrdersRelZiti ziti) {
		if (ziti == null) {
			return "";
		}
		return nz(ziti.getProvince()) + nz(ziti.getCity()) + nz(ziti.getArea()) + nz(ziti.getAddress());
	}

	private String formatPickupDatetime(NormalOrdersRelZiti ziti) {
		if (ziti == null) {
			return "";
		}
		String date = nz(ziti.getPickupDate());
		String[] times = parsePickupTimeRange(ziti.getPickupTime());
		if (!StringUtils.hasText(date) || times.length < 2) {
			return "";
		}
		return date + " " + times[0] + "~" + times[1];
	}

	private String[] parsePickupTimeRange(String raw) {
		if (!StringUtils.hasText(raw)) {
			return new String[0];
		}
		String trimmed = raw.trim();
		try {
			JsonNode n = objectMapper.readTree(trimmed);
			if (n.isArray() && n.size() >= 2) {
				return new String[] {n.get(0).asText(""), n.get(1).asText("")};
			}
		} catch (Exception ignored) {
		}
		if (trimmed.contains("~")) {
			String[] parts = trimmed.split("~", 2);
			return new String[] {parts[0].trim(), parts[1].trim()};
		}
		return new String[0];
	}

	private static String formatPayTime(Trade trade) {
		if (trade == null || !StringUtils.hasText(trade.getTimeExpire())) {
			return "";
		}
		return formatEpoch(parseEpoch(trade.getTimeExpire()));
	}

	private static Integer parseEpoch(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		String s = raw.trim();
		try {
			long n = Long.parseLong(s);
			if (s.length() >= 13) {
				n = n / 1000L;
			}
			return (int) n;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String formatEpoch(Integer epoch) {
		return formatEpochNumber(epoch);
	}

	private static String formatEpochNumber(Number epoch) {
		if (epoch == null) {
			return "";
		}
		long sec = epoch.longValue();
		if (sec <= 0L) {
			return "";
		}
		return CSV_TIME.format(Instant.ofEpochSecond(sec).atZone(SHANGHAI));
	}

	private static String centsToYuan(Integer cents) {
		if (cents == null) {
			return "0.00";
		}
		return centsToYuan(cents.longValue());
	}

	private static String centsToYuan(long cents) {
		return BigDecimal.valueOf(cents)
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.toPlainString();
	}

	private static boolean truthyDatapass(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		String s = String.valueOf(raw).trim();
		return StringUtils.hasText(s) && !"0".equals(s) && !"false".equalsIgnoreCase(s);
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

	private static String nz(String s) {
		return s == null ? "" : s;
	}
}
