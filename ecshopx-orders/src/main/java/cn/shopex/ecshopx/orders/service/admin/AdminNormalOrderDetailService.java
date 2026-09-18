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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.companys.service.setting.TradeCancelSettingRedisService;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.orders.port.AdminOrderDetailDistributionSupportPort;
import cn.shopex.ecshopx.common.orders.port.AdminOrderDetailItemsCatalogPort;
import cn.shopex.ecshopx.common.orders.port.AdminOrderDetailItemsSkuSpecPort;
import cn.shopex.ecshopx.common.port.order.OrderDetailEmployeePurchaseEnrichmentPort;
import cn.shopex.ecshopx.orders.domain.CancelOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.NormalOrdersRelDada;
import cn.shopex.ecshopx.orders.domain.NormalOrdersRelZiti;
import cn.shopex.ecshopx.orders.domain.OrdersDelivery;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.CancelOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersRelDadaMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersRelZitiMapper;
import cn.shopex.ecshopx.orders.mapper.OrdersDeliveryMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.normal.NormalOrdersServiceOrderDataAssembler;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Admin normal-order detail bundle ({@code orderInfo} + trades + cancel + aftersale slot).
 *
 * <p>Deliberately does not filter by {@code order_class}; a matching order is any row with the given
 * {@code order_id} and {@code company_id}, consistent with the legacy API behavior.
 */
@Service
public class AdminNormalOrderDetailService {

	private static final String NO_APPLY_CANCEL = "NO_APPLY_CANCEL";

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final TradeMapper tradeMapper;
	private final CancelOrdersMapper cancelOrdersMapper;
	private final NormalOrdersRelDadaMapper normalOrdersRelDadaMapper;
	private final NormalOrdersRelZitiMapper normalOrdersRelZitiMapper;
	private final OrdersDeliveryMapper ordersDeliveryMapper;
	private final NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler;
	private final AdminOrderDetailStatusAppApplier adminOrderDetailStatusAppApplier;
	private final AftersalesMapper aftersalesMapper;
	private final AftersalesDetailMapper aftersalesDetailMapper;
	private final AdminOrderDetailDistributionSupportPort adminOrderDetailDistributionSupportPort;
	private final AdminOrderDetailItemsSkuSpecPort adminOrderDetailItemsSkuSpecPort;
	private final AdminOrderDetailItemsCatalogPort adminOrderDetailItemsCatalogPort;
	private final AdminOrderDetailAftersalesApplier adminOrderDetailAftersalesApplier;
	private final ObjectProvider<OrderDetailEmployeePurchaseEnrichmentPort> employeePurchaseEnrichmentPort;
	private final TradeCancelSettingRedisService tradeCancelSettingRedisService;

	public AdminNormalOrderDetailService(
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			TradeMapper tradeMapper,
			CancelOrdersMapper cancelOrdersMapper,
			NormalOrdersRelDadaMapper normalOrdersRelDadaMapper,
			NormalOrdersRelZitiMapper normalOrdersRelZitiMapper,
			OrdersDeliveryMapper ordersDeliveryMapper,
			NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler,
			AdminOrderDetailStatusAppApplier adminOrderDetailStatusAppApplier,
			AftersalesMapper aftersalesMapper,
			AftersalesDetailMapper aftersalesDetailMapper,
			AdminOrderDetailDistributionSupportPort adminOrderDetailDistributionSupportPort,
			AdminOrderDetailItemsSkuSpecPort adminOrderDetailItemsSkuSpecPort,
			AdminOrderDetailItemsCatalogPort adminOrderDetailItemsCatalogPort,
			AdminOrderDetailAftersalesApplier adminOrderDetailAftersalesApplier,
			ObjectProvider<OrderDetailEmployeePurchaseEnrichmentPort> employeePurchaseEnrichmentPort,
			TradeCancelSettingRedisService tradeCancelSettingRedisService) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.tradeMapper = tradeMapper;
		this.cancelOrdersMapper = cancelOrdersMapper;
		this.normalOrdersRelDadaMapper = normalOrdersRelDadaMapper;
		this.normalOrdersRelZitiMapper = normalOrdersRelZitiMapper;
		this.ordersDeliveryMapper = ordersDeliveryMapper;
		this.normalOrdersServiceOrderDataAssembler = normalOrdersServiceOrderDataAssembler;
		this.adminOrderDetailStatusAppApplier = adminOrderDetailStatusAppApplier;
		this.aftersalesMapper = aftersalesMapper;
		this.aftersalesDetailMapper = aftersalesDetailMapper;
		this.adminOrderDetailDistributionSupportPort = adminOrderDetailDistributionSupportPort;
		this.adminOrderDetailItemsSkuSpecPort = adminOrderDetailItemsSkuSpecPort;
		this.adminOrderDetailItemsCatalogPort = adminOrderDetailItemsCatalogPort;
		this.adminOrderDetailAftersalesApplier = adminOrderDetailAftersalesApplier;
		this.employeePurchaseEnrichmentPort = employeePurchaseEnrichmentPort;
		this.tradeCancelSettingRedisService = tradeCancelSettingRedisService;
	}

	public Map<String, Object> buildOrderBundle(long companyId, String orderIdRaw, boolean checkaftersales) {
		return buildOrderBundle(companyId, orderIdRaw, checkaftersales, "api");
	}

	public Map<String, Object> buildOrderBundle(
			long companyId, String orderIdRaw, boolean checkaftersales, String detailFrom) {
		String fromCtx = detailFrom == null || detailFrom.isBlank() ? "api" : detailFrom.trim();
		if (!StringUtils.hasText(orderIdRaw)) {
			throw new BadRequestException("参数错误");
		}
		String orderIdStr = orderIdRaw.trim();
		long orderIdNum;
		try {
			orderIdNum = Long.parseLong(orderIdStr);
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数错误");
		}

		NormalOrders order =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getOrderId, orderIdNum)
								.eq(NormalOrders::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (order == null) {
			throw new BadRequestException("订单号为" + orderIdStr + "的订单不存在", 500);
		}

		Map<String, Object> orderInfo =
				new LinkedHashMap<>(normalOrdersServiceOrderDataAssembler.toServiceOrderData(order));
		orderInfo.put("order_id", orderIdStr);
		orderInfo.put("company_id", companyId);

		List<NormalOrdersItems> itemRows =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getOrderId, orderIdNum)
								.orderByAsc(NormalOrdersItems::getId));

		List<Map<String, Object>> itemMaps = new ArrayList<>();
		for (NormalOrdersItems it : itemRows) {
			Map<String, Object> im = AdminOrderDetailPayloadMaps.itemToMap(it);
			im.put("item_holder", "self");
			im.put("supplier_name", "");
			int totalFee = intVal(im.get("total_fee"));
			int pointFee = intVal(im.get("point_fee"));
			int refunded = intVal(im.get("refunded_fee"));
			im.put("after_sales_fee", 0);
			im.put("remain_fee", totalFee);
			im.put("remain_point", pointFee);
			im.put("refundable_amount", totalFee - refunded);
			String ds = str(im.get("delivery_status"));
			int num = intVal(im.get("num"));
			Integer din = it.getDeliveryItemNum();
			if ("DONE".equals(ds) && (din == null || din == 0)) {
				im.put("delivery_item_num", num);
			}
			int effectiveDin = intVal(im.get("delivery_item_num"));
			im.put("delivery_item_num", Math.min(effectiveDin > 0 ? effectiveDin : 0, num));
			if (StringUtils.hasText(str(im.get("delivery_corp")))) {
				String corp = str(im.get("delivery_corp"));
				im.put("delivery_corp_name", corp);
			}
			itemMaps.add(im);
		}
		adminOrderDetailItemsSkuSpecPort.applyRealSkuSpec(companyId, itemMaps);
		adminOrderDetailItemsCatalogPort.applyCatalogSalePrice(companyId, itemMaps);
		orderInfo.put("items", itemMaps);

		Map<String, Object> cancelData = new LinkedHashMap<>();
		String cancelStatus = str(orderInfo.get("cancel_status"));
		CancelOrders cancelRow = null;
		if (!NO_APPLY_CANCEL.equals(cancelStatus)) {
			cancelRow =
					cancelOrdersMapper.selectOne(
							new LambdaQueryWrapper<CancelOrders>()
									.eq(CancelOrders::getOrderId, orderIdNum)
									.eq(CancelOrders::getCompanyId, companyId)
									.last("LIMIT 1"));
			if (cancelRow != null) {
				cancelData = cancelToMap(cancelRow);
			}
		}
		boolean repeatCancel =
				Boolean.TRUE.equals(tradeCancelSettingRedisService.getCancelSetting(companyId).get("repeat_cancel"));
		AdminOrderCanApplyCancelResolver.apply(orderInfo, cancelRow, repeatCancel);

		if (checkaftersales) {
			adminOrderDetailAftersalesApplier.apply(orderInfo, itemMaps);
		} else {
			orderInfo.put("can_apply_aftersales", 0);
		}
		orderInfo.put("refund_freight", 0);
		int freight = intVal(orderInfo.get("freight_fee"));
		orderInfo.put("refund_freight_amount", freight);

		Object afterSaleSlot;
		if (checkaftersales) {
			int refundFreightSum = 0;
			List<Aftersales> mains =
					aftersalesMapper.selectList(
							new LambdaQueryWrapper<Aftersales>()
									.eq(Aftersales::getCompanyId, companyId)
									.eq(Aftersales::getOrderId, orderIdNum)
									.orderByAsc(Aftersales::getCreateTime));
			for (Aftersales m : mains) {
				int st = m.getAftersalesStatus() == null ? -1 : m.getAftersalesStatus();
				if (st == 0 || st == 1 || st == 2) {
					refundFreightSum += m.getFreight() == null ? 0 : m.getFreight();
				}
			}
			orderInfo.put("refund_freight", refundFreightSum);
			orderInfo.put("refund_freight_amount", freight - refundFreightSum);
			afterSaleSlot = buildAftersalesListEnvelope(companyId, mains);
		} else {
			afterSaleSlot = new ArrayList<Map<String, Object>>();
		}

		List<Trade> trades =
				tradeMapper.selectList(
						new LambdaQueryWrapper<Trade>()
								.eq(Trade::getCompanyId, String.valueOf(companyId))
								.eq(Trade::getOrderId, orderIdStr)
								.orderByDesc(Trade::getTimeExpire)
								.orderByDesc(Trade::getTimeStart));

		Map<String, Object> tradeInfo = new LinkedHashMap<>();
		List<Map<String, Object>> tradeListMaps = new ArrayList<>();
		for (Trade t : trades) {
			tradeListMaps.add(AdminOrderDetailPayloadMaps.tradeToMap(t));
		}
		if (!tradeListMaps.isEmpty()) {
			tradeInfo = new LinkedHashMap<>(tradeListMaps.get(0));
		}

		List<Map<String, Object>> paySuccess = new ArrayList<>();
		for (Map<String, Object> tm : tradeListMaps) {
			if ("SUCCESS".equals(String.valueOf(tm.get(AdminOrderDetailPayloadMaps.KEY_TRADE_STATE)))) {
				paySuccess.add(tm);
			}
		}

		applySyntheticNotpayCancel(orderInfo);

		Map<String, Object> dadaMap = new LinkedHashMap<>();
		if ("dada".equals(str(orderInfo.get("receipt_type")))) {
			NormalOrdersRelDada dadaRow =
					normalOrdersRelDadaMapper.selectOne(
							new LambdaQueryWrapper<NormalOrdersRelDada>()
									.eq(NormalOrdersRelDada::getCompanyId, companyId)
									.eq(NormalOrdersRelDada::getOrderId, orderIdNum)
									.last("LIMIT 1"));
			dadaMap = AdminOrderDetailPayloadMaps.dadaToMap(dadaRow);
			orderInfo.put("dada", dadaMap);
		} else {
			orderInfo.put("dada", dadaMap);
		}

		String cancelFrom = "";
		if (!cancelData.isEmpty()) {
			cancelFrom = str(cancelData.get("cancel_from"));
		}

		adminOrderDetailStatusAppApplier.apply(orderInfo, dadaMap, cancelFrom, fromCtx);

		if (StringUtils.hasText(str(orderInfo.get("delivery_corp")))) {
			String corp = str(orderInfo.get("delivery_corp"));
			orderInfo.put("delivery_corp_name", corp);
		}

		orderInfo.put("latest_aftersale_time", 0);

		int getPts = intVal(orderInfo.get("get_points"));
		int bonus = intVal(orderInfo.get("bonus_points"));
		orderInfo.put("estimate_get_points", getPts + bonus);

		orderInfo.put("delivery_type", "new");
		if (StringUtils.hasText(str(orderInfo.get("delivery_code")))) {
			orderInfo.put("delivery_type", "old");
		} else {
			for (Map<String, Object> im : itemMaps) {
				if (StringUtils.hasText(str(im.get("delivery_code")))) {
					orderInfo.put("delivery_type", "old");
					break;
				}
			}
		}

		if ("new".equals(str(orderInfo.get("delivery_type")))) {
			OrdersDelivery od =
					ordersDeliveryMapper.selectOne(
							new LambdaQueryWrapper<OrdersDelivery>()
									.eq(OrdersDelivery::getOrderId, orderIdNum)
									.last("LIMIT 1"));
			if (od != null) {
				orderInfo.put("orders_delivery_id", od.getOrdersDeliveryId() != null ? od.getOrdersDeliveryId() : "");
				putStr(orderInfo, "delivery_corp", od.getDeliveryCorp());
				putStr(orderInfo, "delivery_corp_name", od.getDeliveryCorpName());
				putStr(orderInfo, "delivery_code", od.getDeliveryCode());
				orderInfo.put(
						"is_all_delivery",
						od.getPackageType() != null && "batch".equals(od.getPackageType()));
			}
		}

		if ("ziti".equals(str(orderInfo.get("receipt_type")))) {
			NormalOrdersRelZiti ziti =
					normalOrdersRelZitiMapper.selectOne(
							new LambdaQueryWrapper<NormalOrdersRelZiti>()
									.eq(NormalOrdersRelZiti::getCompanyId, companyId)
									.eq(NormalOrdersRelZiti::getOrderId, orderIdNum)
									.last("LIMIT 1"));
			if (ziti != null) {
				orderInfo.put("ziti_info", zitiToMap(ziti));
			}
		}

		orderInfo.putIfAbsent("self_delivery_operator_mobile", "");
		orderInfo.putIfAbsent("self_delivery_operator_name", "");

		if ("offline_pay".equals(str(orderInfo.get("pay_type")))) {
			int ops = intVal(orderInfo.get("offline_payment_status"));
			orderInfo.put("offline_pay_check_status", ops == -1 ? null : ops);
		}

		OrderDetailEmployeePurchaseEnrichmentPort purchasePort = employeePurchaseEnrichmentPort.getIfAvailable();
		if (purchasePort != null) {
			purchasePort.enrich(companyId, orderIdNum, str(orderInfo.get("order_class")), orderInfo);
		} else {
			orderInfo.put("orders_purchase_info", null);
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("orderInfo", orderInfo);
		out.put("tradeInfo", tradeInfo.isEmpty() ? new LinkedHashMap<>() : tradeInfo);
		out.put("tradeList", paySuccess);
		out.put("cancelData", cancelData);
		out.put("afterSaleInfo", afterSaleSlot);
		return out;
	}

	private Map<String, Object> buildAftersalesListEnvelope(long companyId, List<Aftersales> mains) {
		List<Map<String, Object>> list = new ArrayList<>();
		for (Aftersales main : mains) {
			if (main.getAftersalesBn() == null) {
				continue;
			}
			list.add(aftersalesMainToApiRow(companyId, main));
		}
		Map<String, Object> envelope = new LinkedHashMap<>();
		envelope.put("list", list);
		envelope.put("total_count", list.size());
		return envelope;
	}

	private Map<String, Object> aftersalesMainToApiRow(long companyId, Aftersales main) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("aftersales_bn", main.getAftersalesBn());
		row.put("order_id", main.getOrderId());
		row.put("company_id", main.getCompanyId());
		row.put("user_id", main.getUserId());
		row.put("salesman_id", main.getSalesmanId());
		row.put("item_bn", main.getItemBn());
		row.put("shop_id", main.getShopId());
		row.put("distributor_id", main.getDistributorId());
		row.put("supplier_id", main.getSupplierId());
		row.put("aftersales_type", main.getAftersalesType());
		row.put("aftersales_status", main.getAftersalesStatus());
		row.put("progress", main.getProgress());
		row.put("refund_fee", main.getRefundFee());
		row.put("refund_point", main.getRefundPoint());
		row.put("reason", main.getReason());
		row.put("description", main.getDescription());
		row.put("evidence_pic", main.getEvidencePic());
		row.put("refuse_reason", main.getRefuseReason());
		row.put("memo", main.getMemo());
		row.put("sendback_data", main.getSendbackData());
		row.put("sendconfirm_data", main.getSendconfirmData());
		row.put("third_data", main.getThirdData());
		row.put("aftersales_address", main.getAftersalesAddress());
		row.put("distributor_remark", main.getDistributorRemark());
		row.put("create_time", main.getCreateTime());
		row.put("update_time", main.getUpdateTime());
		row.put("contact", main.getContact());
		row.put("mobile", main.getMobile());
		row.put("merchant_id", main.getMerchantId());
		row.put("is_partial_cancel", main.getIsPartialCancel());
		row.put("return_type", main.getReturnType());
		row.put("return_distributor_id", main.getReturnDistributorId());
		row.put("self_delivery_operator_id", main.getSelfDeliveryOperatorId());
		row.put("self_delivery_operator_mobile", "");
		row.put("self_delivery_operator_name", "");
		row.put("freight", main.getFreight());
		row.put("freight_type", main.getFreightType());
		row.put("user_delete", Boolean.FALSE);

		long distributorId = main.getDistributorId() == null ? 0L : main.getDistributorId();
		Map<String, Object> distributorInfo = new LinkedHashMap<>();
		if (distributorId > 0L) {
			distributorInfo.putAll(
					adminOrderDetailDistributionSupportPort.getDistributorInfoSimple(
							companyId, String.valueOf(distributorId)));
		}
		if (distributorInfo.isEmpty()) {
			distributorInfo.put("name", "平台自营");
		}
		row.put("distributor_info", distributorInfo);

		List<AftersalesDetail> detailRows =
				aftersalesDetailMapper.selectList(
						new LambdaQueryWrapper<AftersalesDetail>()
								.eq(AftersalesDetail::getAftersalesBn, main.getAftersalesBn())
								.eq(AftersalesDetail::getCompanyId, companyId)
								.eq(AftersalesDetail::getUserId, main.getUserId()));
		List<Map<String, Object>> detailMaps = new ArrayList<>();
		for (AftersalesDetail d : detailRows) {
			detailMaps.add(aftersalesDetailToApiRow(d));
		}
		row.put("detail", detailMaps);
		return row;
	}

	private static Map<String, Object> aftersalesDetailToApiRow(AftersalesDetail d) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("detail_id", d.getDetailId());
		m.put("company_id", d.getCompanyId());
		m.put("distributor_id", d.getDistributorId());
		m.put("user_id", d.getUserId());
		m.put("aftersales_bn", d.getAftersalesBn());
		m.put("order_id", d.getOrderId());
		m.put("sub_order_id", d.getSubOrderId());
		m.put("goods_id", d.getGoodsId());
		m.put("item_id", d.getItemId());
		m.put("item_bn", d.getItemBn());
		m.put("item_name", d.getItemName());
		m.put("order_item_type", d.getOrderItemType());
		m.put("item_pic", d.getItemPic());
		m.put("num", d.getNum());
		m.put("refund_fee", d.getRefundFee());
		m.put("refund_point", d.getRefundPoint());
		m.put("aftersales_type", d.getAftersalesType());
		m.put("progress", d.getProgress());
		m.put("aftersales_status", d.getAftersalesStatus());
		m.put("create_time", d.getCreateTime());
		m.put("update_time", d.getUpdateTime());
		m.put("auto_refuse_time", d.getAutoRefuseTime());
		m.put("refunded_num", d.getRefundedNum());
		return m;
	}

	private static void applySyntheticNotpayCancel(Map<String, Object> orderInfo) {
		if (!"NOTPAY".equals(str(orderInfo.get("order_status")))) {
			return;
		}
		if ("drug".equals(str(orderInfo.get("order_class")))) {
			return;
		}
		long now = System.currentTimeMillis() / 1000L;
		int autoCancel = intVal(orderInfo.get("auto_cancel_time"));
		if (autoCancel <= 0 || autoCancel - now > 0) {
			return;
		}
		String payType = str(orderInfo.get("pay_type"));
		String offlineSt = str(orderInfo.get("offline_payment_status"));
		boolean branch1 = !"offline_pay".equals(payType) || "-1".equals(offlineSt);
		if (branch1) {
			orderInfo.put("order_status", "CANCEL");
			return;
		}
		if ("offline_pay".equals(payType) && !"0".equals(offlineSt)) {
			orderInfo.put("order_status", "CANCEL");
		}
	}

	private static Map<String, Object> cancelToMap(CancelOrders c) {
		Map<String, Object> m = new LinkedHashMap<>();
		putStr(m, "cancel_id", c.getCancelId());
		putStr(m, "order_id", c.getOrderId());
		putStr(m, "company_id", c.getCompanyId());
		putStr(m, "cancel_from", c.getCancelFrom());
		m.put("cancel_reason", c.getCancelReason());
		m.put("refund_status", c.getRefundStatus());
		m.put("progress", c.getProgress());
		m.put("create_time", c.getCreateTime());
		m.put("update_time", c.getUpdateTime());
		return m;
	}

	private static Map<String, Object> zitiToMap(NormalOrdersRelZiti z) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("name", z.getName());
		m.put("lng", z.getLng());
		m.put("lat", z.getLat());
		m.put("province", z.getProvince());
		m.put("city", z.getCity());
		m.put("area", z.getArea());
		m.put("address", z.getAddress());
		m.put("contract_phone", z.getContractPhone());
		m.put("pickup_date", z.getPickupDate());
		m.put("pickup_time", decodeZitiPickupTime(z.getPickupTime()));
		m.put("create_time", z.getCreateTime());
		m.put("update_time", z.getUpdateTime());
		return m;
	}

	private static Object decodeZitiPickupTime(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return new ObjectMapper().readValue(raw, new TypeReference<List<Object>>() {});
		} catch (Exception e) {
			return null;
		}
	}

	private static void putStr(Map<String, Object> m, String k, Object v) {
		m.put(k, v == null ? "" : String.valueOf(v));
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private static int intVal(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
