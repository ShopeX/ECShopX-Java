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

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.port.order.OrderExportEmployeePurchaseInfoLookupPort;
import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersRelZiti;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersRelZitiMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderDetailStatusAppApplier;
import cn.shopex.ecshopx.orders.service.orderexport.support.OrderExportNormalOrderQuerySupport;
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
import java.util.regex.Pattern;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NormalMasterOrderExportCsvService {

	private static final int PAGE_SIZE = 2000;
	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter FILE_TS =
			DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(SHANGHAI);
	private static final DateTimeFormatter CSV_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final Pattern ORDER_ID_NUMERIC = Pattern.compile("^[0-9]+$");

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
					Map.entry("shopadmin", "门店订单"),
					Map.entry("employee_purchase", "内购订单"));

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
					Map.entry("prepaid_point", "预充点数"),
					Map.entry("deposit", "余额支付"),
					Map.entry("pos", "现金支付"),
					Map.entry("gat", "关爱通支付"),
					Map.entry("offline", "线下转账"),
					Map.entry("offline_pay", "线下支付"));

	private static final Map<String, String> RECEIPT_TYPE_LABELS =
			Map.of(
					"merchant", "商家自配",
					"logistics", "快递配送",
					"ziti", "上门自提",
					"dada", "同城配");

	private static final Map<String, String> ORDER_HOLDER_LABELS =
			Map.of(
					"self", "自营订单",
					"distributor", "商家订单",
					"supplier", "供应商订单",
					"self_supplier", "自营和供应商订单",
					"distributor_supplier", "商家和供应商订单");

	private static final Map<String, String> PURCHASE_TYPE_LABELS =
			Map.of("employee", "员工", "relative", "亲友");

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersRelZitiMapper normalOrdersRelZitiMapper;
	private final TradeMapper tradeMapper;
	private final MembersInfoMapper membersInfoMapper;
	private final ExportCsvFileService exportCsvFileService;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final ObjectMapper objectMapper;
	private final AdminOrderDetailStatusAppApplier adminOrderDetailStatusAppApplier;
	private final ObjectProvider<OrderExportEmployeePurchaseInfoLookupPort> employeePurchaseInfoLookupPort;

	public NormalMasterOrderExportCsvService(
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersRelZitiMapper normalOrdersRelZitiMapper,
			TradeMapper tradeMapper,
			MembersInfoMapper membersInfoMapper,
			ExportCsvFileService exportCsvFileService,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			ObjectMapper objectMapper,
			AdminOrderDetailStatusAppApplier adminOrderDetailStatusAppApplier,
			ObjectProvider<OrderExportEmployeePurchaseInfoLookupPort> employeePurchaseInfoLookupPort) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersRelZitiMapper = normalOrdersRelZitiMapper;
		this.tradeMapper = tradeMapper;
		this.membersInfoMapper = membersInfoMapper;
		this.exportCsvFileService = exportCsvFileService;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.objectMapper = objectMapper;
		this.adminOrderDetailStatusAppApplier = adminOrderDetailStatusAppApplier;
		this.employeePurchaseInfoLookupPort = employeePurchaseInfoLookupPort;
	}

	public Optional<Map<String, String>> export(long companyId, LinkedHashMap<String, Object> filter) {
		LinkedHashMap<String, Object> work = new LinkedHashMap<>(filter);
		boolean datapassBlock = truthyDatapass(work.remove("datapass_block"));

		LinkedHashMap<String, String> title = buildTitle();
		List<Map<String, String>> rows = new ArrayList<>();
		int pageNum = 1;
		while (true) {
			Page<NormalOrders> page = new Page<>(pageNum, PAGE_SIZE);
			IPage<NormalOrders> result =
					normalOrdersMapper.selectPage(
							page, OrderExportNormalOrderQuerySupport.toListWrapper(companyId, work));
			List<NormalOrders> orders = result.getRecords();
			if (orders == null || orders.isEmpty()) {
				break;
			}
			List<Long> orderIds = orders.stream().map(NormalOrders::getOrderId).toList();
			Map<Long, String> memberNames = loadMemberNames(companyId, orders);
			Map<Long, NormalOrdersRelZiti> zitiByOrder = loadZitiByOrder(companyId, orderIds);
			Map<Long, Trade> successTradeByOrder = loadSuccessTrades(companyId, orderIds);
			Map<Long, OrderExportEmployeePurchaseInfoLookupPort.Info> purchaseByOrder =
					loadPurchaseInfo(companyId, orderIds);

			for (NormalOrders o : orders) {
				rows.add(
						buildRow(
								o,
								memberNames.getOrDefault(o.getUserId(), ""),
								zitiByOrder.get(o.getOrderId()),
								successTradeByOrder.get(o.getOrderId()),
								purchaseByOrder.get(o.getOrderId()),
								datapassBlock));
			}
			if (orders.size() < PAGE_SIZE) {
				break;
			}
			pageNum++;
		}
		if (rows.isEmpty()) {
			return Optional.empty();
		}
		String fileBaseName = FILE_TS.format(Instant.now()) + companyId + "master";
		Map<String, String> upload = exportCsvFileService.exportCsv(fileBaseName, title, rows);
		if (upload == null || upload.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(upload);
	}

	private static LinkedHashMap<String, String> buildTitle() {
		LinkedHashMap<String, String> t = new LinkedHashMap<>();
		t.put("order_id", "订单号");
		t.put("name", "用户名");
		t.put("create_time", "下单时间");
		t.put("order_holder", "订单分类");
		t.put("total_fee_total", "订单总金额(¥)");
		t.put("total_fee", "现金实付(¥)");
		t.put("point_fee", "积分抵扣");
		t.put("cost_fee", "成本价(¥)");
		t.put("freight_fee", "运费(总)");
		t.put("commission_fee", "佣金(总)");
		t.put("discount_fee", "优惠金额");
		t.put("discount_info", "优惠详情");
		t.put("order_class", "订单类型");
		t.put("order_status", "订单状态");
		t.put("end_time", "订单完成时间");
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
		t.put("delivery_code", "快递单号");
		t.put("delivery_corp", "快递公司");
		t.put("pay_type", "支付方式");
		t.put("pay_time", "支付时间");
		t.put("invoice", "发票内容");
		t.put("remark", "订单备注");
		t.put("pickup_address", "自提地址");
		t.put("pickup_datetime", "提货时间");
		t.put("source_from", "订单来源");
		t.put("purchase_type", "角色");
		t.put("employee_name", "员工姓名");
		t.put("enterprise_name", "所属企业");
		t.put("purchase_mode_desc", "企业购购买方式");
		t.put("employee_purchase_activity_name", "企业购名称");
		t.put("employee_purchase_activity_id", "企业购活动ID");
		return t;
	}

	private Map<String, String> buildRow(
			NormalOrders o,
			String memberName,
			NormalOrdersRelZiti ziti,
			Trade trade,
			OrderExportEmployeePurchaseInfoLookupPort.Info purchaseInfo,
			boolean datapassBlock) {
		String payType = nz(o.getPayType());
		int discountCents = resolveDiscountCents(o);
		String receiverName = nz(o.getReceiverName());
		String receiverMobile = nz(o.getReceiverMobile());
		String receiverAddress = nz(o.getReceiverAddress());
		if (datapassBlock) {
			receiverName = nz(DataMasking.maskTruename(receiverName));
			receiverMobile = nz(DataMasking.maskMobile(receiverMobile));
			receiverAddress = nz(DataMasking.maskAddress(receiverAddress));
		}

		boolean deliveryDone = "DONE".equals(nz(o.getDeliveryStatus()));
		String purchaseType = purchaseInfo == null ? "" : nz(purchaseInfo.type());
		String employeeName = purchaseInfo == null ? "" : nz(purchaseInfo.employeeName());
		String enterpriseName = purchaseInfo == null ? "" : nz(purchaseInfo.enterpriseName());

		LinkedHashMap<String, String> row = new LinkedHashMap<>();
		row.put("order_id", formatOrderIdCell(String.valueOf(o.getOrderId())));
		row.put("name", memberName);
		row.put("create_time", formatEpoch(o.getCreateTime()));
		row.put("order_holder", ORDER_HOLDER_LABELS.getOrDefault(nz(o.getOrderHolder()), "未知订单类型"));
		long itemFee = parseAmountCents(o.getItemFee());
		int freightFee = o.getFreightFee() == null ? 0 : o.getFreightFee();
		row.put("total_fee_total", centsToYuan((int) (itemFee + freightFee)));
		row.put("total_fee", formatTotalFee(o, payType));
		row.put("point_fee", centsToYuan(o.getPointFee()));
		row.put("cost_fee", centsToYuan(o.getCostFee()));
		row.put("freight_fee", centsToYuan(o.getFreightFee()));
		row.put("commission_fee", centsToYuan(o.getCommissionFee()));
		row.put("discount_fee", centsToYuan(discountCents));
		row.put("discount_info", buildDiscountInfoDesc(o.getDiscountInfo()));
		row.put("order_class", ORDER_CLASS_LABELS.getOrDefault(nz(o.getOrderClass()), nz(o.getOrderClass())));
		row.put("order_status", resolveOrderStatusMessage(o));
		row.put("end_time", formatEpochNumber(o.getEndTime()));
		row.put("receipt_type", RECEIPT_TYPE_LABELS.getOrDefault(nz(o.getReceiptType()), nz(o.getReceiptType())));
		row.put("ziti_status", "DONE".equals(nz(o.getZitiStatus())) ? "已自提" : "");
		row.put("receiver_name", clearSpecialChars(receiverName));
		row.put("receiver_mobile", receiverMobile);
		row.put("receiver_zip", nz(o.getReceiverZip()));
		row.put("receiver_state", nz(o.getReceiverState()));
		row.put("receiver_city", nz(o.getReceiverCity()));
		row.put("receiver_district", nz(o.getReceiverDistrict()));
		row.put("receiver_address", clearSpecialChars(receiverAddress));
		row.put("delivery_status", deliveryDone ? "已发货" : "未发货");
		row.put("delivery_time", deliveryDone ? formatEpoch(o.getDeliveryTime()) : "0");
		row.put("delivery_code", deliveryDone ? nz(o.getDeliveryCode()) + "\t" : "");
		row.put("delivery_corp", deliveryDone ? nz(o.getDeliveryCorp()) : "");
		row.put("pay_type", PAY_TYPE_LABELS.getOrDefault(payType, payType));
		row.put("pay_time", formatPayTime(trade));
		row.put("invoice", formatInvoiceCell(o.getInvoice()));
		row.put("remark", nz(o.getRemark()));
		row.put("pickup_address", formatPickupAddress(ziti));
		row.put("pickup_datetime", formatPickupDatetime(ziti));
		row.put("source_from", nz(o.getSourceFrom()));
		row.put("purchase_type", PURCHASE_TYPE_LABELS.getOrDefault(purchaseType, "会员"));
		row.put("employee_name", employeeName);
		row.put("enterprise_name", enterpriseName);
		String purchaseMode = purchaseInfo == null ? "" : nz(purchaseInfo.purchaseMode());
		row.put(
				"purchase_mode_desc",
				"prepaid_point".equals(purchaseMode)
						? "预充点数"
						: ("cash".equals(purchaseMode) ? "现金" : ""));
		row.put(
				"employee_purchase_activity_name",
				purchaseInfo == null ? "" : nz(purchaseInfo.activityName()));
		row.put(
				"employee_purchase_activity_id",
				purchaseInfo == null || purchaseInfo.activityId() == null
						? ""
						: String.valueOf(purchaseInfo.activityId()));
		return row;
	}

	private Map<Long, String> loadMemberNames(long companyId, List<NormalOrders> orders) {
		List<Long> userIds =
				orders.stream()
						.map(NormalOrders::getUserId)
						.filter(uid -> uid != null && uid > 0L)
						.distinct()
						.toList();
		if (userIds.isEmpty()) {
			return Map.of();
		}
		List<MembersInfo> infos =
				membersInfoMapper.selectList(
						new LambdaQueryWrapper<MembersInfo>()
								.eq(MembersInfo::getCompanyId, companyId)
								.in(MembersInfo::getUserId, userIds));
		Map<Long, String> out = new HashMap<>();
		for (MembersInfo mi : infos) {
			String raw = firstNonBlank(mi.getName(), mi.getUsername());
			out.put(mi.getUserId(), decryptMaybe(raw));
		}
		return out;
	}

	private Map<Long, NormalOrdersRelZiti> loadZitiByOrder(long companyId, List<Long> orderIds) {
		if (orderIds.isEmpty()) {
			return Map.of();
		}
		List<NormalOrdersRelZiti> all =
				normalOrdersRelZitiMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersRelZiti>()
								.eq(NormalOrdersRelZiti::getCompanyId, companyId)
								.in(NormalOrdersRelZiti::getOrderId, orderIds));
		Map<Long, NormalOrdersRelZiti> out = new HashMap<>();
		for (NormalOrdersRelZiti z : all) {
			out.putIfAbsent(z.getOrderId(), z);
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

	private Map<Long, OrderExportEmployeePurchaseInfoLookupPort.Info> loadPurchaseInfo(
			long companyId, List<Long> orderIds) {
		OrderExportEmployeePurchaseInfoLookupPort port = employeePurchaseInfoLookupPort.getIfAvailable();
		if (port == null) {
			return Map.of();
		}
		return port.lookupByOrderIds(companyId, orderIds);
	}

	private String resolveOrderStatusMessage(NormalOrders ord) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("order_status", ord.getOrderStatus());
		m.put("receipt_type", ord.getReceiptType());
		m.put("cancel_status", ord.getCancelStatus());
		m.put("ziti_status", ord.getZitiStatus());
		m.put("delivery_status", ord.getDeliveryStatus());
		m.put("prescription_status", ord.getPrescriptionStatus());
		m.put("diagnosis_data", null);
		adminOrderDetailStatusAppApplier.apply(m, null, "0");
		Object msg = m.get("order_status_msg");
		return msg == null ? "" : String.valueOf(msg);
	}

	private String formatTotalFee(NormalOrders o, String payType) {
		if ("dhpoint".equals(payType)) {
			return o.getTotalFee() == null ? "" : String.valueOf(o.getTotalFee());
		}
		return centsToYuan(parseAmountCents(o.getTotalFee()));
	}

	private static long parseAmountCents(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int resolveDiscountCents(NormalOrders o) {
		int md = o.getMemberDiscount() == null ? 0 : o.getMemberDiscount();
		int cd = o.getCouponDiscount() == null ? 0 : o.getCouponDiscount();
		if (md > 0 && cd > 0) {
			return md + cd;
		}
		return o.getDiscountFee() == null ? 0 : o.getDiscountFee();
	}

	private String buildDiscountInfoDesc(String discountInfoJson) {
		if (!StringUtils.hasText(discountInfoJson)) {
			return "";
		}
		try {
			JsonNode root = objectMapper.readTree(discountInfoJson.trim());
			if (!root.isArray()) {
				return "";
			}
			StringBuilder sb = new StringBuilder();
			for (JsonNode el : root) {
				if (!el.isObject()) {
					continue;
				}
				String part = discountDescForOne(el);
				if (StringUtils.hasText(part)) {
					sb.append(part);
				}
			}
			return sb.toString();
		} catch (Exception e) {
			return "";
		}
	}

	private static String discountDescForOne(JsonNode value) {
		JsonNode typeNode = value.get("type");
		if (typeNode == null || typeNode.isNull()) {
			return "";
		}
		String type = typeNode.asText("");
		JsonNode feeNode = value.get("discount_fee");
		int feeCents = feeNode == null || !feeNode.isNumber() ? 0 : feeNode.asInt();
		if (feeCents <= 0) {
			return "";
		}
		String feeYuan =
				BigDecimal.valueOf(feeCents)
								.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
								.toPlainString()
						+ "元";
		return switch (type) {
			case "full_discount" -> "满折：" + feeYuan + "; ";
			case "full_minus" -> "满减：" + feeYuan + "; ";
			case "coupon_discount" -> "折扣优惠券：" + feeYuan + "; ";
			case "cash_discount" -> "代金优惠券：" + feeYuan + "; ";
			case "limited_time_sale" -> "限时特惠：" + feeYuan + "; ";
			case "seckill" -> "秒杀：" + feeYuan + "; ";
			case "groups" -> "拼团：" + feeYuan + "; ";
			case "member_price" -> "会员价：" + feeYuan + "; ";
			case "member_tag_targeted_promotio：" -> "定向促销：" + feeYuan + "; ";
			case "member_tag_targeted_promotion" -> "定向促销：" + feeYuan + "; ";
			default -> "";
		};
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
		String val = n.get(key).asText("").trim();
		if (!StringUtils.hasText(val)) {
			return;
		}
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

	private String decryptMaybe(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "";
		}
		try {
			return sensitiveFieldEncryptor.decrypt(raw);
		} catch (Exception e) {
			return raw;
		}
	}

	private static String firstNonBlank(String a, String b) {
		if (StringUtils.hasText(a)) {
			return a.trim();
		}
		if (StringUtils.hasText(b)) {
			return b.trim();
		}
		return "";
	}

	private static String formatOrderIdCell(String orderIdStr) {
		if (ORDER_ID_NUMERIC.matcher(orderIdStr).matches()) {
			return "\t" + orderIdStr;
		}
		return orderIdStr;
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

	private static String centsToYuan(long cents) {
		if (cents == 0L) {
			return "0";
		}
		return BigDecimal.valueOf(cents)
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.toPlainString();
	}

	private static String centsToYuan(Integer cents) {
		if (cents == null) {
			return "";
		}
		return centsToYuan(cents.longValue());
	}

	private static String clearSpecialChars(String str) {
		if (!StringUtils.hasText(str)) {
			return str == null ? "" : str;
		}
		return str.replace("\r", " ").replace("\n", " ").replace(",", " ");
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

	private static String nz(String s) {
		return s == null ? "" : s;
	}
}
