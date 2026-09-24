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

package cn.shopex.ecshopx.orders.service.normal.create;

import cn.shopex.ecshopx.common.order.OrderTypeSlugMapper;
import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.common.order.normal.OrderCreatePersistencePort;
import cn.shopex.ecshopx.common.order.normal.OrderProfitByOrderResultPort;
import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.NormalOrdersRelSupplier;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.domain.OrderPromotions;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersRelSupplierMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.OrderPromotionsMapper;
import cn.shopex.ecshopx.orders.service.excard.NormalOrderNumericIdService;
import cn.shopex.ecshopx.orders.service.normal.OrderDiscountInfoSupport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderCreatePersistencePortImpl implements OrderCreatePersistencePort {

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final OrderPromotionsMapper orderPromotionsMapper;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final NormalOrdersRelSupplierMapper normalOrdersRelSupplierMapper;
	private final NormalOrderNumericIdService normalOrderNumericIdService;
	private final OrderProfitByOrderResultPort orderProfitByOrderResultPort;
	private final CompanyDefaultCurrencyService companyDefaultCurrencyService;
	private final ObjectMapper objectMapper;

	public OrderCreatePersistencePortImpl(
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			OrderPromotionsMapper orderPromotionsMapper,
			OrderAssociationsMapper orderAssociationsMapper,
			NormalOrdersRelSupplierMapper normalOrdersRelSupplierMapper,
			NormalOrderNumericIdService normalOrderNumericIdService,
			OrderProfitByOrderResultPort orderProfitByOrderResultPort,
			CompanyDefaultCurrencyService companyDefaultCurrencyService,
			ObjectMapper objectMapper) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.orderPromotionsMapper = orderPromotionsMapper;
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.normalOrdersRelSupplierMapper = normalOrdersRelSupplierMapper;
		this.normalOrderNumericIdService = normalOrderNumericIdService;
		this.orderProfitByOrderResultPort = orderProfitByOrderResultPort;
		this.companyDefaultCurrencyService = companyDefaultCurrencyService;
		this.objectMapper = objectMapper;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void persistOrder(NormalOrderCreateParams p) {
		Map<String, Object> od = p.getOrderData();
		ensureCurrencyFields(od);
		long companyId = longVal(od.get("company_id"), 0L);
		long userId = longVal(od.get("user_id"), 0L);
		long orderId = normalOrderNumericIdService.generate(userId);
		od.put("order_id", orderId);
		int now = (int) (System.currentTimeMillis() / 1000L);
		long distributorId = longVal(od.get("distributor_id"), 0L);
		long totalFee = longVal(od.get("total_fee"), 0L);
		int itemFeeInt = (int) Math.min(longVal(od.get("item_fee"), 0L), Integer.MAX_VALUE);
		String orderType = stringVal(od.get("order_type"));
		String orderClass = stringVal(od.get("order_class"));
		if (orderType.isBlank() || orderClass.isBlank()) {
			// FormatDataPort 之外的调用方未填字段时按 slug 兜底；slug 也缺失则按后台代客默认
			String slug = stringVal(od.get("order_type_slug"));
			OrderTypeSlugMapper.Resolved typed =
					OrderTypeSlugMapper.resolve(slug.isBlank() ? "normal_shopadmin" : slug);
			if (orderType.isBlank()) {
				orderType = typed.orderType();
			}
			if (orderClass.isBlank()) {
				orderClass = typed.orderClass();
			}
		}

		NormalOrders order = new NormalOrders();
		order.setOrderId(orderId);
		order.setCompanyId(companyId);
		order.setUserId(userId);
		order.setMobile(stringVal(od.get("mobile")));
		order.setOrderClass(orderClass);
		order.setOrderType(orderType);
		order.setReceiptType(stringVal(od.get("receipt_type")));
		order.setDistributorId(distributorId);
		order.setIsDistribution(distributorId > 0L);
		// PHP: shop_id from orderData (shopInfo; 0 when isCheckShopValid=false) — not distributor_id
		long shopId = longVal(od.get("shop_id"), 0L);
		order.setShopId(shopId);
		String orderHolder = stringVal(od.get("order_holder"));
		order.setOrderHolder(orderHolder.isBlank() ? "self" : orderHolder);
		order.setOrderSource(stringVal(od.get("order_source")));
		order.setOrderStatus("NOTPAY");
		order.setPayStatus("NOTPAY");
		String zitiStatus = stringVal(od.get("ziti_status"));
		order.setZitiStatus(zitiStatus.isBlank() ? "NOTZITI" : zitiStatus);
		order.setZitiCode(longVal(od.get("ziti_code"), 0L));
		order.setDeliveryStatus("PENDING");
		order.setCancelStatus("NO_APPLY_CANCEL");
		order.setSelfDeliveryStatus("NOTMERCHANT");
		order.setTitle(stringVal(od.get("title")));
		order.setTotalFee(String.valueOf(totalFee));
		order.setItemFee(String.valueOf(itemFeeInt));
		order.setMarketFee(String.valueOf(longVal(od.get("market_fee"), totalFee)));
		order.setDiscountFee(intVal(od.get("discount_fee"), 0));
		order.setMemberDiscount(intVal(od.get("member_discount"), 0));
		order.setPayType(stringVal(od.get("pay_type")));
		order.setRemark(stringVal(od.get("remark")));
		order.setFreightFee(intVal(od.get("freight_fee"), 0));
		order.setFreightPoint(intVal(od.get("freight_point"), 0));
		order.setFreightPointFee(intVal(od.get("freight_point_fee"), 0));
		String freightType = stringVal(od.get("freight_type"));
		order.setFreightType(freightType.isBlank() ? "cash" : freightType);
		order.setItemPoint(intVal(od.get("item_point"), 0));
		order.setPoint(intVal(od.get("point"), 0));
		order.setPointUse(intVal(od.get("point_use"), 0));
		order.setPointFee(intVal(od.get("point_fee"), 0));
		order.setCostFee(intVal(od.get("cost_fee"), 0));
		order.setCouponDiscount(intVal(od.get("coupon_discount"), 0));
		order.setGetPointType(intVal(od.get("get_point_type"), 0));
		order.setGetPoints(intValPoints(od.get("get_points")));
		order.setExtraPoints(intVal(od.get("extra_points"), 0));
		order.setPayChannel(stringVal(od.get("pay_channel")));
		String sourceFrom = stringVal(od.get("source_from"));
		if (!sourceFrom.isEmpty()) {
			order.setSourceFrom(sourceFrom);
		}
		order.setCreateTime(now);
		order.setUpdateTime(now);
		long actIdVal = longVal(od.get("act_id"), 0L);
		if (actIdVal > 0L) {
			order.setActId(actIdVal);
		}
		Object autoCancelRaw = od.get("auto_cancel_time");
		if (autoCancelRaw != null && !stringVal(autoCancelRaw).isEmpty()) {
			order.setAutoCancelTime(stringVal(autoCancelRaw));
		} else {
			// Last-resort fallback; create path should set from order_cancel_time (default 15 min).
			order.setAutoCancelTime(String.valueOf(now + 15 * 60));
		}
		order.setIsOnlineOrder(Boolean.FALSE.equals(od.get("is_online_order")) ? Boolean.FALSE : Boolean.TRUE);
		order.setSalesmanId(longVal(od.get("salesman_id"), 0L));
		long opId = longVal(od.get("operator_id"), 0L);
		order.setOperatorId(opId > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) opId);
		applyReceiverFieldsFromOrderData(order, od);
		order.setMerchantId(longVal(od.get("merchant_id"), 0L));
		order.setDiscountInfo(writeDiscountInfoJson(od.get("discount_info")));
		order.setCouponDiscountDesc(writeCouponDiscountDescJson(od));
		applyCurrencyFields(order, od);
		normalOrdersMapper.insert(order);

		Object itemsRaw = od.get("items");
		Map<Long, NormalOrdersItems> itemOrder = new LinkedHashMap<>();
		if (itemsRaw instanceof List<?> itemList) {
			for (Object o : itemList) {
				if (!(o instanceof Map<?, ?> row)) {
					continue;
				}
				NormalOrdersItems line = new NormalOrdersItems();
				line.setOrderId(orderId);
				line.setCompanyId(companyId);
				line.setUserId(userId);
				line.setItemId(longVal(row.get("item_id"), 0L));
				line.setGoodsId(longVal(row.get("goods_id"), line.getItemId()));
				line.setItemBn(stringVal(row.get("item_bn")));
				line.setGoodsBn(stringVal(row.get("goods_bn")));
				line.setItemName(stringVal(row.get("item_name")));
				line.setItemUnit(stringVal(row.get("item_unit")));
				line.setPic(stringVal(row.get("pic")));
				line.setNum(intVal(row.get("num"), 1));
				line.setPrice(intVal(row.get("price"), 0));
				line.setCostPrice(intVal(row.get("cost_price"), 0));
				line.setCostFee(intVal(row.get("cost_fee"), 0));
				line.setCouponDiscount(intVal(row.get("coupon_discount"), 0));
				line.setGetPoints(intValPoints(row.get("get_points")));
				line.setMarketPrice(intVal(row.get("market_price"), line.getPrice()));
				line.setTotalFee(intVal(row.get("total_fee"), 0));
				line.setItemFee(intVal(row.get("item_fee"), line.getTotalFee()));
				line.setDiscountFee(intVal(row.get("discount_fee"), 0));
				line.setPointFee(intVal(row.get("point_fee"), 0));
				line.setSharePoints(intVal(row.get("share_points"), 0));
				line.setItemPoint(intVal(row.get("item_point"), 0));
				line.setPoint(intVal(row.get("point"), line.getSharePoints()));
				line.setDistributorId(longVal(row.get("distributor_id"), distributorId));
				line.setShopId(longVal(row.get("shop_id"), shopId));
				line.setIsTotalStore(!Boolean.FALSE.equals(row.get("is_total_store")));
				line.setOrderItemType(stringVal(row.get("order_item_type")));
				if (line.getOrderItemType() == null || line.getOrderItemType().isBlank()) {
					line.setOrderItemType("normal");
				}
				line.setCreateTime(now);
				line.setUpdateTime(now);
				line.setItemSpecDesc(itemSpecDescText(row.get("item_spec_desc")));
				line.setSupplierId(intVal(row.get("supplier_id"), 0));
				Object actId = row.get("activity_id");
				if (actId instanceof Number n) {
					line.setActId(n.longValue());
				} else {
					line.setActId(0L);
				}
				line.setDiscountInfo(writeDiscountInfoJson(row.get("discount_info")));
				line.setCouponDiscountDesc(writeCouponDiscountDescJson((Map<String, Object>) row));
				applyCurrencyFields(line, (Map<String, Object>) row, od);
				normalOrdersItemsMapper.insert(line);
				if (line.getItemId() != null && line.getItemId() > 0L) {
					itemOrder.put(line.getItemId(), line);
				}
				long act = line.getActId() != null ? line.getActId() : 0L;
				if (act > 0L) {
					OrderPromotions pr = new OrderPromotions();
					pr.setMoid(orderId);
					pr.setCoid(line.getId());
					pr.setUserId(userId);
					pr.setOrderType(orderType);
					pr.setItemId(line.getItemId());
					pr.setItemName(line.getItemName());
					pr.setItemType("normal");
					pr.setActivityId(act);
					pr.setActivityName(stringVal(row.get("activity_name")));
					pr.setActivityType(stringVal(row.get("activity_type")));
					if (pr.getActivityType() == null || pr.getActivityType().isBlank()) {
						pr.setActivityType("normal");
					}
					pr.setShopId(distributorId);
					pr.setCompanyId(companyId);
					pr.setCreated(now);
					pr.setUpdated(now);
					orderPromotionsMapper.insert(pr);
				}
			}
		}
		persistItemsPromotion(od, itemOrder, orderId, companyId, userId, orderType, now);

		OrderAssociations assoc = new OrderAssociations();
		assoc.setOrderId(orderId);
		assoc.setCompanyId(companyId);
		assoc.setUserId(userId);
		assoc.setMobile(stringVal(od.get("mobile")));
		assoc.setTitle(stringVal(od.get("title")));
		assoc.setTotalFee(totalFee);
		assoc.setOrderClass(orderClass);
		assoc.setOrderType(orderType);
		assoc.setOrderStatus("NOTPAY");
		assoc.setShopId(shopId);
		assoc.setIsDistribution(distributorId > 0L);
		assoc.setDeliveryStatus("PENDING");
		assoc.setCancelStatus("NO_APPLY_CANCEL");
		assoc.setCouponDiscount(intVal(od.get("coupon_discount"), 0));
		assoc.setCouponDiscountDesc(writeCouponDiscountDescJson(od));
		assoc.setCreateTime(now);
		assoc.setUpdateTime(now);
		applyCurrencyFields(assoc, od);
		orderAssociationsMapper.insert(assoc);

		Object supObj = od.get("supplier_freight_fee");
		if (supObj instanceof Map<?, ?> supMap) {
			for (Map.Entry<?, ?> entry : supMap.entrySet()) {
				int supplierId = intVal(entry.getKey(), 0);
				int freight = intVal(entry.getValue(), 0);
				if (supplierId > 0 && freight >= 0) {
					NormalOrdersRelSupplier rel = new NormalOrdersRelSupplier();
					rel.setCompanyId(companyId);
					rel.setOrderId(orderId);
					rel.setSupplierId(supplierId);
					rel.setFreightFee(freight);
					rel.setCreateTime(now);
					normalOrdersRelSupplierMapper.insert(rel);
				}
			}
		}

		orderProfitByOrderResultPort.profitByOrderResult(p);

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("order_id", orderId);
		res.put("company_id", companyId);
		res.put("user_id", userId);
		res.put("total_fee", totalFee);
		res.put("order_status", "NOTPAY");
		res.put("pay_type", stringVal(od.get("pay_type")));
		res.put("receipt_type", stringVal(od.get("receipt_type")));
		res.put("order_class", orderClass);
		res.put("order_type", orderType);
		res.put("discount_fee", intVal(od.get("discount_fee"), 0));
		res.put("discount_info", od.get("discount_info"));
		res.put("point_fee", intVal(od.get("point_fee"), 0));
		res.put("point_use", intVal(od.get("point_use"), 0));
		res.put("mobile", stringVal(od.get("mobile")));
		res.put("title", stringVal(od.get("title")));
		res.put("operator_id", longVal(od.get("operator_id"), 0L));
		List<Map<String, Object>> outItems = new ArrayList<>();
		if (itemsRaw instanceof List<?> itemList2) {
			for (Object o : itemList2) {
				if (o instanceof Map<?, ?> m) {
					outItems.add(new LinkedHashMap<>((Map<String, Object>) m));
				}
			}
		}
		res.put("items", outItems);
		res.put("auto_cancel_time", order.getAutoCancelTime());
		res.put("item_fee", od.get("item_fee"));
		res.put("market_fee", od.get("market_fee"));
		res.put("create_time", now);
		res.put("prescription_status", intVal(od.get("prescription_status"), 0));
		if (od.containsKey("team_id")) {
			res.put("team_id", od.get("team_id"));
		}
		res.put("point", intVal(od.get("point_use"), 0));
		res.put("fee_type", stringVal(od.get("fee_type")));
		res.put("fee_rate", od.get("fee_rate"));
		res.put("fee_symbol", stringVal(od.get("fee_symbol")));
		p.setOrdersInsertResult(res);
	}

	private void ensureCurrencyFields(Map<String, Object> od) {
		if (od == null || od.isEmpty()) {
			return;
		}
		if (StringUtils.hasText(stringVal(od.get("fee_type")))) {
			return;
		}
		long companyId = longVal(od.get("company_id"), 0L);
		if (companyId <= 0L) {
			return;
		}
		CurrencyExchangeRate cur = companyDefaultCurrencyService.getCur(companyId);
		if (cur == null) {
			return;
		}
		od.put("fee_type", cur.getCurrency() != null ? cur.getCurrency() : "CNY");
		od.put("fee_rate", cur.getRate() != null ? cur.getRate() : 1.0);
		od.put("fee_symbol", cur.getSymbol() != null ? cur.getSymbol() : "￥");
	}

	private static void applyCurrencyFields(NormalOrders order, Map<String, Object> od) {
		String feeType = stringVal(od.get("fee_type"));
		if (StringUtils.hasText(feeType)) {
			order.setFeeType(feeType);
		}
		Float feeRate = floatVal(od.get("fee_rate"), null);
		if (feeRate != null) {
			order.setFeeRate(feeRate);
		}
		String feeSymbol = stringVal(od.get("fee_symbol"));
		if (StringUtils.hasText(feeSymbol)) {
			order.setFeeSymbol(feeSymbol);
		}
	}

	private static void applyCurrencyFields(OrderAssociations assoc, Map<String, Object> od) {
		String feeType = stringVal(od.get("fee_type"));
		if (StringUtils.hasText(feeType)) {
			assoc.setFeeType(feeType);
		}
		Float feeRate = floatVal(od.get("fee_rate"), null);
		if (feeRate != null) {
			assoc.setFeeRate(feeRate);
		}
		String feeSymbol = stringVal(od.get("fee_symbol"));
		if (StringUtils.hasText(feeSymbol)) {
			assoc.setFeeSymbol(feeSymbol);
		}
	}

	private static void applyCurrencyFields(
			NormalOrdersItems line, Map<String, Object> row, Map<String, Object> orderData) {
		String feeType = stringVal(row.get("fee_type"));
		if (!StringUtils.hasText(feeType)) {
			feeType = stringVal(orderData.get("fee_type"));
		}
		if (StringUtils.hasText(feeType)) {
			line.setFeeType(feeType);
		}
		Object feeRateRaw = row.get("fee_rate");
		if (feeRateRaw == null) {
			feeRateRaw = orderData.get("fee_rate");
		}
		Float feeRate = floatVal(feeRateRaw, null);
		if (feeRate != null) {
			line.setFeeRate(feeRate);
		}
		String feeSymbol = stringVal(row.get("fee_symbol"));
		if (!StringUtils.hasText(feeSymbol)) {
			feeSymbol = stringVal(orderData.get("fee_symbol"));
		}
		if (StringUtils.hasText(feeSymbol)) {
			line.setFeeSymbol(feeSymbol);
		}
	}

	@SuppressWarnings("unchecked")
	private void persistItemsPromotion(
			Map<String, Object> od,
			Map<Long, NormalOrdersItems> itemOrder,
			long orderId,
			long companyId,
			long userId,
			String orderType,
			int now) {
		if (itemOrder == null || itemOrder.isEmpty()) {
			return;
		}
		Object promoRaw = od.get("items_promotion");
		if (!(promoRaw instanceof List<?> promos) || promos.isEmpty()) {
			return;
		}
		for (Object o : promos) {
			if (!(o instanceof Map<?, ?> promoRawMap)) {
				continue;
			}
			Map<String, Object> promo = (Map<String, Object>) promoRawMap;
			long itemId = longVal(promo.get("item_id"), 0L);
			NormalOrdersItems item = itemOrder.get(itemId);
			if (item == null || item.getId() == null) {
				continue;
			}
			OrderPromotions pr = new OrderPromotions();
			pr.setMoid(item.getOrderId() != null ? item.getOrderId() : orderId);
			pr.setCoid(item.getId());
			pr.setUserId(longVal(promo.get("user_id"), userId));
			String promoOrderType = stringVal(promo.get("order_type"));
			pr.setOrderType(promoOrderType.isBlank() ? orderType : promoOrderType);
			pr.setItemId(itemId);
			pr.setItemName(firstNonBlank(stringVal(promo.get("item_name")), item.getItemName()));
			String itemType = stringVal(promo.get("item_type"));
			pr.setItemType(itemType.isBlank() ? "normal" : itemType);
			pr.setActivityId(longVal(promo.get("activity_id"), 0L));
			pr.setActivityName(stringVal(promo.get("activity_name")));
			String activityType = stringVal(promo.get("activity_type"));
			if (activityType.isBlank()) {
				activityType = stringVal(promo.get("marketing_type"));
			}
			pr.setActivityType(activityType);
			pr.setActivityTag(stringVal(promo.get("activity_tag")));
			pr.setActivityDesc(writeJsonValue(promo.get("activity_desc")));
			pr.setShopId(longVal(promo.get("shop_id"), item.getDistributorId() != null ? item.getDistributorId() : 0L));
			String shopType = stringVal(promo.get("shop_type"));
			pr.setShopType(shopType.isBlank() ? "shop" : shopType);
			pr.setStatus("valid");
			pr.setCompanyId(longVal(promo.get("company_id"), companyId));
			pr.setCreated(now);
			pr.setUpdated(now);
			orderPromotionsMapper.insert(pr);
		}
	}

	private static void applyReceiverFieldsFromOrderData(NormalOrders order, Map<String, Object> od) {
		if (od.containsKey("receiver_name")) {
			order.setReceiverName(truncateToMaxChars(stringVal(od.get("receiver_name")), 50));
		}
		if (od.containsKey("receiver_mobile")) {
			order.setReceiverMobile(stringVal(od.get("receiver_mobile")));
		}
		if (od.containsKey("receiver_zip")) {
			String zip = stringVal(od.get("receiver_zip"));
			if (!zip.isEmpty()) {
				order.setReceiverZip(zip);
			}
		}
		if (od.containsKey("receiver_state")) {
			String state = stringVal(od.get("receiver_state"));
			if (!state.isEmpty()) {
				order.setReceiverState(state);
			}
		}
		if (od.containsKey("receiver_city")) {
			String city = stringVal(od.get("receiver_city"));
			if (!city.isEmpty()) {
				order.setReceiverCity(city);
			}
		}
		if (od.containsKey("receiver_district")) {
			String district = stringVal(od.get("receiver_district"));
			if (!district.isEmpty()) {
				order.setReceiverDistrict(district);
			}
		}
		if (od.containsKey("receiver_address")) {
			order.setReceiverAddress(stringVal(od.get("receiver_address")));
		}
	}

	private static String truncateToMaxChars(String value, int maxChars) {
		if (value == null || value.isEmpty()) {
			return value;
		}
		int len = value.codePointCount(0, value.length());
		if (len <= maxChars) {
			return value;
		}
		return value.substring(0, value.offsetByCodePoints(0, maxChars));
	}

	private static long longVal(Object v, long def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static int intVal(Object v, int def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static Float floatVal(Object v, Float def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.floatValue();
		}
		try {
			return Float.parseFloat(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString();
	}

	private static String itemSpecDescText(Object raw) {
		if (!(raw instanceof String text)) {
			return "";
		}
		String spec = text.trim();
		if (spec.isEmpty() || "单规格".equals(spec) || "[]".equals(spec) || "{}".equals(spec)) {
			return "";
		}
		return spec;
	}

	private static String firstNonBlank(String primary, String fallback) {
		if (primary != null && !primary.isBlank()) {
			return primary;
		}
		return fallback == null ? "" : fallback;
	}

	private String writeJsonValue(Object raw) {
		if (raw == null) {
			return "";
		}
		if (raw instanceof String s) {
			return s;
		}
		try {
			return objectMapper.writeValueAsString(raw);
		} catch (JsonProcessingException e) {
			return "";
		}
	}

	private String writeDiscountInfoJson(Object raw) {
		try {
			List<Map<String, Object>> list =
					OrderDiscountInfoSupport.parseDiscountInfoRaw(raw, objectMapper);
			return objectMapper.writeValueAsString(list);
		} catch (JsonProcessingException e) {
			return "[]";
		}
	}

	private String writeCouponDiscountDescJson(Map<String, Object> source) {
		if (!source.containsKey("coupon_discount_desc")) {
			return "";
		}
		Object raw = source.get("coupon_discount_desc");
		Map<String, Object> desc = OrderDiscountInfoSupport.copyCouponDiscountDesc(raw);
		if (desc == null) {
			return "";
		}
		try {
			return objectMapper.writeValueAsString(desc);
		} catch (JsonProcessingException e) {
			return "";
		}
	}

	private static int intValPoints(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof BigDecimal b) {
			return b.setScale(0, RoundingMode.DOWN).intValue();
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return new BigDecimal(v.toString().trim()).setScale(0, RoundingMode.DOWN).intValue();
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
