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
import cn.shopex.ecshopx.orders.domain.NormalOrdersRelSupplier;
import cn.shopex.ecshopx.orders.domain.NormalOrdersRelZiti;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.domain.dto.NormalOrderExportItemRow;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersRelSupplierMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersRelZitiMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderDetailStatusAppApplier;
import cn.shopex.ecshopx.orders.service.invoice.export.InvoiceExportItemCategoryPathService;
import cn.shopex.ecshopx.orders.service.orderexport.support.NormalOrderExportDistributorLookupService;
import cn.shopex.ecshopx.orders.service.orderexport.support.NormalOrderExportRefundLookupService;
import cn.shopex.ecshopx.supplier.domain.Supplier;
import cn.shopex.ecshopx.supplier.mapper.SupplierMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NormalOrderExportCsvService {

	private static final int PAGE_SIZE = 2000;
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
					Map.entry("offline_pay", "线下支付"));

	private static final Map<String, String> RECEIPT_TYPE_LABELS =
			Map.of(
					"merchant", "商家自配",
					"logistics", "快递配送",
					"ziti", "上门自提",
					"dada", "同城配");

	private static final Map<String, String> PURCHASE_TYPE_LABELS =
			Map.of("employee", "员工", "relative", "亲友");

	private static final Map<String, String> AFTERSALES_STATUS_LABELS =
			Map.ofEntries(
					Map.entry("WAIT_SELLER_AGREE", "等待商家处理"),
					Map.entry("WAIT_BUYER_RETURN_GOODS", "商家接受申请，等待消费者回寄"),
					Map.entry("WAIT_SELLER_CONFIRM_GOODS", "消费者回寄，等待商家收货确认"),
					Map.entry("SELLER_REFUSE_BUYER", "售后驳回"),
					Map.entry("SELLER_SEND_GOODS", "卖家重新发货 换货完成"),
					Map.entry("REFUND_SUCCESS", "退款成功"),
					Map.entry("REFUND_CLOSED", "退款关闭"),
					Map.entry("CLOSED", "售后关闭"));

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersRelZitiMapper normalOrdersRelZitiMapper;
	private final NormalOrdersRelSupplierMapper normalOrdersRelSupplierMapper;
	private final TradeMapper tradeMapper;
	private final MembersInfoMapper membersInfoMapper;
	private final SupplierMapper supplierMapper;
	private final ExportCsvFileService exportCsvFileService;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final ObjectMapper objectMapper;
	private final AdminOrderDetailStatusAppApplier adminOrderDetailStatusAppApplier;
	private final ObjectProvider<OrderExportEmployeePurchaseInfoLookupPort> employeePurchaseInfoLookupPort;
	private final InvoiceExportItemCategoryPathService invoiceExportItemCategoryPathService;
	private final NormalOrderExportRefundLookupService normalOrderExportRefundLookupService;
	private final NormalOrderExportDistributorLookupService normalOrderExportDistributorLookupService;

	public NormalOrderExportCsvService(
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersRelZitiMapper normalOrdersRelZitiMapper,
			NormalOrdersRelSupplierMapper normalOrdersRelSupplierMapper,
			TradeMapper tradeMapper,
			MembersInfoMapper membersInfoMapper,
			SupplierMapper supplierMapper,
			ExportCsvFileService exportCsvFileService,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			ObjectMapper objectMapper,
			AdminOrderDetailStatusAppApplier adminOrderDetailStatusAppApplier,
			ObjectProvider<OrderExportEmployeePurchaseInfoLookupPort> employeePurchaseInfoLookupPort,
			InvoiceExportItemCategoryPathService invoiceExportItemCategoryPathService,
			NormalOrderExportRefundLookupService normalOrderExportRefundLookupService,
			NormalOrderExportDistributorLookupService normalOrderExportDistributorLookupService) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersRelZitiMapper = normalOrdersRelZitiMapper;
		this.normalOrdersRelSupplierMapper = normalOrdersRelSupplierMapper;
		this.tradeMapper = tradeMapper;
		this.membersInfoMapper = membersInfoMapper;
		this.supplierMapper = supplierMapper;
		this.exportCsvFileService = exportCsvFileService;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.objectMapper = objectMapper;
		this.adminOrderDetailStatusAppApplier = adminOrderDetailStatusAppApplier;
		this.employeePurchaseInfoLookupPort = employeePurchaseInfoLookupPort;
		this.invoiceExportItemCategoryPathService = invoiceExportItemCategoryPathService;
		this.normalOrderExportRefundLookupService = normalOrderExportRefundLookupService;
		this.normalOrderExportDistributorLookupService = normalOrderExportDistributorLookupService;
	}

	public Optional<Map<String, String>> export(long companyId, LinkedHashMap<String, Object> filter) {
		LinkedHashMap<String, Object> work = new LinkedHashMap<>(filter);
		work.put("company_id", companyId);
		boolean datapassBlock = truthyDatapass(work.remove("datapass_block"));

		LinkedHashMap<String, String> title = buildTitle();
		List<Map<String, String>> rows = new ArrayList<>();
		Set<String> supplierHas = new HashSet<>();
		Set<Long> itemHas = new HashSet<>();
		long offset = 0L;
		while (true) {
			List<NormalOrderExportItemRow> page =
					normalOrdersMapper.selectExportNormalOrderItems(work, offset, PAGE_SIZE);
			if (page == null || page.isEmpty()) {
				break;
			}
			List<Long> orderIds = page.stream().map(NormalOrderExportItemRow::getOrderId).distinct().toList();
			List<Long> userIds =
					page.stream()
							.map(NormalOrderExportItemRow::getUserId)
							.filter(uid -> uid != null && uid > 0L)
							.distinct()
							.toList();
			List<Long> distributorIds =
					page.stream()
							.map(NormalOrderExportItemRow::getDistributorId)
							.filter(id -> id != null && id > 0L)
							.distinct()
							.toList();
			List<Integer> supplierIds =
					page.stream()
							.map(NormalOrderExportItemRow::getItemSupplierId)
							.filter(id -> id != null && id > 0)
							.distinct()
							.toList();
			List<Long> itemIds =
					page.stream()
							.map(NormalOrderExportItemRow::getItemId)
							.filter(id -> id != null && id > 0L)
							.distinct()
							.toList();

			Map<Long, MemberNames> memberNames = loadMemberNames(companyId, userIds);
			Map<Long, NormalOrderExportDistributorLookupService.StoreInfo> stores =
					normalOrderExportDistributorLookupService.loadStores(companyId, distributorIds);
			Map<Integer, String> supplierNames = loadSupplierNames(companyId, supplierIds);
			Map<Long, Trade> successTradeByOrder = loadSuccessTrades(companyId, orderIds);
			Map<Long, NormalOrdersRelZiti> zitiByOrder = loadZitiByOrder(companyId, orderIds);
			Map<Long, OrderExportEmployeePurchaseInfoLookupPort.Info> purchaseByOrder =
					loadPurchaseInfo(companyId, orderIds);
			Map<Long, String> categoryPaths = invoiceExportItemCategoryPathService.loadCategoryPaths(companyId, itemIds);
			Map<String, NormalOrderExportRefundLookupService.RefundInfo> refundByOrderItem =
					normalOrderExportRefundLookupService.loadItemRefunds(companyId, orderIds);
			Map<Long, Integer> forwardRefundTime =
					normalOrderExportRefundLookupService.loadForwardRefundSuccessTime(companyId, orderIds);
			FreightContext freightCtx = loadFreightContext(companyId, orderIds);

			for (NormalOrderExportItemRow row : page) {
				rows.add(
						buildRow(
								row,
								memberNames.getOrDefault(row.getUserId(), MemberNames.EMPTY),
								stores.getOrDefault(row.getDistributorId(), NormalOrderExportDistributorLookupService.StoreInfo.EMPTY),
								supplierNames.getOrDefault(row.getItemSupplierId(), ""),
								successTradeByOrder.get(row.getOrderId()),
								zitiByOrder.get(row.getOrderId()),
								purchaseByOrder.get(row.getOrderId()),
								categoryPaths.getOrDefault(row.getItemId(), ""),
								refundByOrderItem.get(
										NormalOrderExportRefundLookupService.key(
												row.getOrderId(), row.getItemId())),
								forwardRefundTime.get(row.getOrderId()),
								freightCtx,
								supplierHas,
								itemHas,
								datapassBlock));
			}
			if (page.size() < PAGE_SIZE) {
				break;
			}
			offset += PAGE_SIZE;
		}
		if (rows.isEmpty()) {
			return Optional.empty();
		}
		String fileBaseName = FILE_TS.format(Instant.now()) + companyId + resolveNormalOrderFileSuffix(work);
		Map<String, String> upload = exportCsvFileService.exportCsv(fileBaseName, title, rows);
		if (upload == null || upload.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(upload);
	}

	static LinkedHashMap<String, String> buildTitle() {
		LinkedHashMap<String, String> t = new LinkedHashMap<>();
		t.put("order_id", "订单号");
		t.put("name", "用户名");
		t.put("id", "子订单号");
		t.put("supplier_name", "所属供应商");
		t.put("item_name", "商品名称");
		t.put("category_tree", "管理分类");
		t.put("price", "商品销售单价");
		t.put("cost_price", "成本单价");
		t.put("num", "购买数量");
		t.put("item_fee", "销售总金额");
		t.put("commission_fee", "商品佣金");
		t.put("cost_fee", "结算总价（¥）");
		t.put("freight_fee", "运费(总)");
		t.put("total_fee_total", "实付金额(总)");
		t.put("point_fee", "积分抵扣（¥）");
		t.put("total_fee", "现金实付（¥）");
		t.put("discount_fee", "优惠总金额");
		t.put("discount_info", "优惠详情");
		t.put("refund_num", "退货数量");
		t.put("refund_cost_price", "退货成本");
		t.put("refunded_point_fee", "退款积分");
		t.put("refunded_fee", "退款金额");
		t.put("store_name", "所属店铺");
		t.put("store_code", "店铺号");
		t.put("mobile", "会员手机号");
		t.put("user_name", "会员昵称");
		t.put("create_time", "下单时间");
		t.put("pay_time", "支付时间");
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
		t.put("delivery_status", "收货状态");
		t.put("delivery_time", "发货时间");
		t.put("delivery_code", "快递单号");
		t.put("delivery_corp", "快递公司");
		t.put("end_time", "订单完成时间");
		t.put("pay_type", "支付方式");
		t.put("item_bn", "商品货号");
		t.put("aftersales_status", "售后状态");
		t.put("refund_time", "退款时间");
		t.put("item_spec_desc", "规格描述");
		t.put("remark", "订单备注");
		t.put("pickup_address", "自提地址");
		t.put("pickup_datetime", "提货时间");
		t.put("purchase_type", "角色");
		t.put("employee_name", "员工姓名");
		t.put("enterprise_name", "所属企业");
		t.put("purchase_mode_desc", "企业购购买方式");
		t.put("employee_purchase_activity_name", "企业购名称");
		t.put("employee_purchase_activity_id", "企业购活动ID");
		return t;
	}

	private Map<String, String> buildRow(
			NormalOrderExportItemRow src,
			MemberNames member,
			NormalOrderExportDistributorLookupService.StoreInfo store,
			String supplierName,
			Trade trade,
			NormalOrdersRelZiti ziti,
			OrderExportEmployeePurchaseInfoLookupPort.Info purchaseInfo,
			String categoryTree,
			NormalOrderExportRefundLookupService.RefundInfo refundInfo,
			Integer forwardRefundSuccessTime,
			FreightContext freightCtx,
			Set<String> supplierHas,
			Set<Long> itemHas,
			boolean datapassBlock) {
		String deliveryStatus = nz(src.getDeliveryStatus());
		Integer deliveryTime = src.getItemDeliveryTime();
		String deliveryCode = nz(src.getItemDeliveryCode());
		String deliveryCorp = nz(src.getItemDeliveryCorp());
		if ("DONE".equals(nz(src.getZitiStatus())) && "DONE".equals(nz(src.getOrderDeliveryStatus()))) {
			deliveryStatus = nz(src.getOrderDeliveryStatus());
			deliveryTime = src.getOrderDeliveryTime();
			deliveryCode = nz(src.getOrderDeliveryCorp());
			deliveryCorp = nz(src.getOrderDeliveryCode());
		}

		int discountCents = resolveItemDiscountCents(src);
		String mobile = decryptMaybe(nz(src.getMobile()));
		String username = member.username();
		String receiverName = decryptMaybe(nz(src.getReceiverName()));
		String receiverMobile = decryptMaybe(nz(src.getReceiverMobile()));
		String receiverAddress = decryptMaybe(nz(src.getReceiverAddress()));
		if (datapassBlock) {
			mobile = nz(DataMasking.maskMobile(mobile));
			username = nz(DataMasking.maskTruename(username));
			receiverName = nz(DataMasking.maskTruename(receiverName));
			receiverMobile = nz(DataMasking.maskMobile(receiverMobile));
			receiverAddress = nz(DataMasking.maskAddress(receiverAddress));
		}

		int refundNum = refundInfo != null && refundInfo.num() != null ? refundInfo.num() : 0;
		int refundedFee = refundInfo != null && refundInfo.refundedFee() != null ? refundInfo.refundedFee() : 0;
		int refundedPoint = refundInfo != null && refundInfo.refundedPoint() != null ? refundInfo.refundedPoint() : 0;
		Integer refundSuccessTime = refundInfo != null ? refundInfo.refundSuccessTime() : null;
		int costPrice = src.getCostPrice() == null ? 0 : src.getCostPrice();
		int refundCostPrice = costPrice * refundNum;

		if ("CANCEL".equals(nz(src.getOrderStatus())) && "PAYED".equals(nz(src.getPayStatus()))) {
			refundNum = src.getNum() == null ? 0 : src.getNum();
			refundedPoint = src.getItemPointFee() == null ? 0 : src.getItemPointFee();
			refundedFee = src.getItemTotalFee() == null ? 0 : src.getItemTotalFee();
			refundCostPrice = costPrice * refundNum;
			refundSuccessTime = forwardRefundSuccessTime;
		}

		String freightFee = "0";
		int supplierId = src.getItemSupplierId() == null ? 0 : src.getItemSupplierId();
		String supplierKey = src.getOrderId() + "_" + supplierId;
		if (supplierId > 0
				&& freightCtx.supplierFreight().containsKey(supplierKey)
				&& !supplierHas.contains(supplierKey)) {
			freightFee = centsToYuan(freightCtx.supplierFreight().get(supplierKey));
			supplierHas.add(supplierKey);
		} else if (supplierId == 0 && !itemHas.contains(src.getOrderId())) {
			int orderFreight = src.getOrderFreightFee() == null ? 0 : src.getOrderFreightFee();
			freightFee = centsToYuan(Math.max(0, orderFreight - freightCtx.pageSupplierTotalFreight()));
			itemHas.add(src.getOrderId());
		}

		String purchaseType = purchaseInfo == null ? "" : nz(purchaseInfo.type());
		boolean deliveryDone = "DONE".equals(deliveryStatus);
		int itemPointFee = src.getItemPointFee() == null ? 0 : src.getItemPointFee();
		int itemTotalFee = src.getItemTotalFee() == null ? 0 : src.getItemTotalFee();

		LinkedHashMap<String, String> row = new LinkedHashMap<>();
		row.put("order_id", textCell(String.valueOf(src.getOrderId())));
		row.put("name", member.name());
		row.put("id", src.getItemRowId() == null ? "" : String.valueOf(src.getItemRowId()));
		row.put("supplier_name", supplierName);
		row.put("item_name", nz(src.getItemName()).replace("#", ""));
		row.put("category_tree", categoryTree);
		row.put("price", centsToYuan(src.getPrice()));
		row.put("cost_price", centsToYuan(src.getCostPrice()));
		row.put("num", src.getNum() == null ? "" : String.valueOf(src.getNum()));
		row.put("item_fee", centsToYuan(src.getItemFee()));
		row.put("commission_fee", centsToYuan(src.getCommissionFee()));
		row.put("cost_fee", centsToYuan(src.getCostFee()));
		row.put("freight_fee", freightFee);
		row.put("total_fee_total", centsToYuan(itemTotalFee + itemPointFee));
		row.put("point_fee", centsToYuan(src.getItemPointFee()));
		row.put("total_fee", centsToYuan(src.getItemTotalFee()));
		row.put("discount_fee", centsToYuan(discountCents));
		row.put("discount_info", buildDiscountInfoDesc(src.getDiscountInfo()));
		row.put("refund_num", String.valueOf(refundNum));
		row.put("refund_cost_price", centsToYuan(refundCostPrice));
		row.put("refunded_point_fee", centsToYuan(refundedPoint));
		row.put("refunded_fee", centsToYuan(refundedFee));
		row.put("store_name", store.name());
		row.put("store_code", store.shopCode());
		row.put("mobile", mobile);
		row.put("user_name", username);
		row.put("create_time", formatEpoch(src.getOrderCreateTime()));
		row.put("pay_time", formatPayTime(trade));
		row.put("order_class", ORDER_CLASS_LABELS.getOrDefault(nz(src.getOrderClass()), nz(src.getOrderClass())));
		row.put("order_status", resolveOrderStatusMessage(src));
		row.put("receipt_type", RECEIPT_TYPE_LABELS.getOrDefault(nz(src.getReceiptType()), nz(src.getReceiptType())));
		row.put("ziti_status", "DONE".equals(nz(src.getZitiStatus())) ? "已自提" : "");
		row.put("receiver_name", clearSpecialChars(receiverName));
		row.put("receiver_mobile", receiverMobile);
		row.put("receiver_zip", nz(src.getReceiverZip()));
		row.put("receiver_state", nz(src.getReceiverState()));
		row.put("receiver_city", nz(src.getReceiverCity()));
		row.put("receiver_district", nz(src.getReceiverDistrict()));
		row.put("receiver_address", clearSpecialChars(receiverAddress));
		row.put("delivery_status", deliveryDone ? "已发货" : "未发货");
		row.put("delivery_time", deliveryDone ? formatEpoch(deliveryTime) : "0");
		row.put("delivery_code", deliveryDone ? textCell(deliveryCode) : "");
		row.put("delivery_corp", deliveryDone ? deliveryCorp : "");
		row.put("end_time", formatEpoch(src.getEndTime()));
		row.put("pay_type", PAY_TYPE_LABELS.getOrDefault(nz(src.getPayType()), nz(src.getPayType())));
		row.put("item_bn", textCell(nz(src.getItemBn())));
		row.put(
				"aftersales_status",
				AFTERSALES_STATUS_LABELS.getOrDefault(nz(src.getAftersalesStatus()), ""));
		row.put("refund_time", formatEpoch(refundSuccessTime));
		row.put("item_spec_desc", formatItemSpecDesc(src.getItemSpecDesc()));
		row.put("remark", nz(src.getRemark()));
		row.put("pickup_address", formatPickupAddress(ziti));
		row.put("pickup_datetime", formatPickupDatetime(ziti));
		row.put("purchase_type", PURCHASE_TYPE_LABELS.getOrDefault(purchaseType, "会员"));
		row.put("employee_name", purchaseInfo == null ? "" : nz(purchaseInfo.employeeName()));
		row.put("enterprise_name", purchaseInfo == null ? "" : nz(purchaseInfo.enterpriseName()));
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

	private FreightContext loadFreightContext(long companyId, List<Long> orderIds) {
		if (orderIds.isEmpty()) {
			return new FreightContext(Map.of(), 0);
		}
		List<NormalOrdersRelSupplier> rels =
				normalOrdersRelSupplierMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersRelSupplier>()
								.eq(NormalOrdersRelSupplier::getCompanyId, companyId)
								.in(NormalOrdersRelSupplier::getOrderId, orderIds));
		Map<String, Integer> supplierFreight = new HashMap<>();
		int pageTotal = 0;
		for (NormalOrdersRelSupplier rel : rels) {
			int fee = rel.getFreightFee() == null ? 0 : rel.getFreightFee();
			String key = rel.getOrderId() + "_" + rel.getSupplierId();
			supplierFreight.put(key, fee);
			pageTotal += fee;
		}
		return new FreightContext(supplierFreight, pageTotal);
	}

	private Map<Long, MemberNames> loadMemberNames(long companyId, List<Long> userIds) {
		if (userIds.isEmpty()) {
			return Map.of();
		}
		List<MembersInfo> infos =
				membersInfoMapper.selectList(
						new LambdaQueryWrapper<MembersInfo>()
								.eq(MembersInfo::getCompanyId, companyId)
								.in(MembersInfo::getUserId, userIds));
		Map<Long, MemberNames> out = new HashMap<>();
		for (MembersInfo mi : infos) {
			String name = decryptMaybe(firstNonBlank(mi.getName(), ""));
			String username = decryptMaybe(firstNonBlank(mi.getUsername(), ""));
			out.put(mi.getUserId(), new MemberNames(name, username));
		}
		return out;
	}

	private Map<Integer, String> loadSupplierNames(long companyId, List<Integer> supplierIds) {
		if (supplierIds.isEmpty()) {
			return Map.of();
		}
		List<Long> operatorIds = supplierIds.stream().map(Integer::longValue).distinct().toList();
		List<Supplier> suppliers =
				supplierMapper.selectList(
						new LambdaQueryWrapper<Supplier>()
								.eq(Supplier::getCompanyId, companyId)
								.in(Supplier::getOperatorId, operatorIds));
		Map<Integer, String> out = new HashMap<>();
		for (Supplier s : suppliers) {
			if (s.getOperatorId() != null) {
				out.put(s.getOperatorId().intValue(), nz(s.getSupplierName()));
			}
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

	private String resolveOrderStatusMessage(NormalOrderExportItemRow row) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("order_status", row.getOrderStatus());
		m.put("receipt_type", row.getReceiptType());
		m.put("cancel_status", row.getCancelStatus());
		m.put("ziti_status", row.getZitiStatus());
		m.put("delivery_status", row.getDeliveryStatus());
		m.put("prescription_status", row.getPrescriptionStatus());
		m.put("diagnosis_data", null);
		adminOrderDetailStatusAppApplier.apply(m, null, "0");
		Object msg = m.get("order_status_msg");
		return msg == null ? "" : String.valueOf(msg);
	}

	private static int resolveItemDiscountCents(NormalOrderExportItemRow row) {
		int md = row.getMemberDiscount() == null ? 0 : row.getMemberDiscount();
		int cd = row.getCouponDiscount() == null ? 0 : row.getCouponDiscount();
		if (md > 0 && cd > 0) {
			return md + cd;
		}
		return row.getItemDiscountFee() == null ? 0 : row.getItemDiscountFee();
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
			case "full_discount" -> "满折：" + feeYuan + ";";
			case "full_minus" -> "满减：" + feeYuan + ";";
			case "coupon_discount" -> "折扣优惠券：" + feeYuan + ";";
			case "cash_discount" -> "代金优惠券：" + feeYuan + ";";
			case "limited_time_sale" -> "限时特惠：" + feeYuan + ";";
			case "seckill" -> "秒杀：" + feeYuan + ";";
			case "groups" -> "拼团：" + feeYuan + ";";
			case "member_price" -> "会员价：" + feeYuan + ";";
			case "member_tag_targeted_promotio：" -> "定向促销：" + feeYuan + ";";
			case "member_tag_targeted_promotion" -> "定向促销：" + feeYuan + ";";
			default -> "";
		};
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

	private static String textCell(String value) {
		if (!StringUtils.hasText(value)) {
			return "";
		}
		return value + "\t";
	}

	private static String formatEpoch(Integer epoch) {
		if (epoch == null || epoch <= 0) {
			return "";
		}
		return CSV_TIME.format(Instant.ofEpochSecond(epoch.longValue()).atZone(SHANGHAI));
	}

	private static String centsToYuan(Integer cents) {
		if (cents == null) {
			return "";
		}
		return centsToYuan(cents.longValue());
	}

	private static String centsToYuan(long cents) {
		if (cents == 0L) {
			return "0";
		}
		return BigDecimal.valueOf(cents)
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.toPlainString();
	}

	private static String formatItemSpecDesc(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "";
		}
		return raw.replace(",", "，");
	}

	private static String clearSpecialChars(String str) {
		if (!StringUtils.hasText(str)) {
			return str == null ? "" : str;
		}
		return str.replace("\r", " ").replace("\n", " ").replace(",", " ");
	}

	private static String resolveNormalOrderFileSuffix(LinkedHashMap<String, Object> filter) {
		Object orderClass = filter.get("order_class");
		if (orderClass != null && "pointsmall".equals(String.valueOf(orderClass).trim())) {
			return "order积分商城";
		}
		Object supplierId = filter.get("supplier_id");
		if (supplierId != null) {
			long sid;
			if (supplierId instanceof Number n) {
				sid = n.longValue();
			} else {
				try {
					sid = Long.parseLong(String.valueOf(supplierId).trim());
				} catch (NumberFormatException e) {
					sid = 0L;
				}
			}
			if (sid > 0L) {
				return "supplier_order";
			}
		}
		return "order";
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

	private record MemberNames(String name, String username) {
		static final MemberNames EMPTY = new MemberNames("", "");
	}

	private record FreightContext(Map<String, Integer> supplierFreight, int pageSupplierTotalFreight) {}
}
