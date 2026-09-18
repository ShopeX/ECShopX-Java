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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.common.distribution.DistributorGetInfoSimpleByDistributorIdPort;
import cn.shopex.ecshopx.common.port.pointsmall.PointsmallFreightMoneyConvertPort;
import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.orders.service.excard.NormalOrderNumericIdService;
import cn.shopex.ecshopx.orders.service.freight.NormalOrderShippingFreightCountService;
import cn.shopex.ecshopx.common.order.normal.OrderCreateFormatDataPort;
import cn.shopex.ecshopx.orders.service.normal.create.NormalOrderCreateState;
import cn.shopex.ecshopx.orders.service.normal.create.NormalOrderCheckoutPointDeductService;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import cn.shopex.ecshopx.point.service.PointMemberBalanceReadService;
import cn.shopex.ecshopx.point.service.PointMemberRuleReadService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Fills {@code orderData} for wxapp online normal physical checkout preview so the API payload
 * matches PHP {@code OrderService::_formatOrderData} / {@code __formatNormalOrder} / {@code getOrderTempInfo}
 * for the fields the legacy client consumes.
 */
@Service
public class WxappNormalOrderTempInfoEnrichmentService {

	private static final Set<String> PROMOTION_DISCOUNT_TYPES =
			Set.of("full_minus", "full_discount", "member_tag_targeted_promotion");

	private final NormalOrderNumericIdService normalOrderNumericIdService;

	private final OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;

	private final NormalOrderShippingFreightCountService normalOrderShippingFreightCountService;

	private final PointMemberRuleReadService pointMemberRuleReadService;

	private final PointMemberBalanceReadService pointMemberBalanceReadService;

	private final CompanyDefaultCurrencyService companyDefaultCurrencyService;

	private final DistributorGetInfoSimpleByDistributorIdPort distributorGetInfoSimpleByDistributorIdPort;

	private final NormalOrderCheckoutPointDeductService normalOrderCheckoutPointDeductService;

	private final OrderCreateFormatDataPort orderCreateFormatDataPort;

	private final PointsmallFreightMoneyConvertPort pointsmallFreightMoneyConvertPort;

	public WxappNormalOrderTempInfoEnrichmentService(
			NormalOrderNumericIdService normalOrderNumericIdService,
			OrderValiditySettingRedisReadService orderValiditySettingRedisReadService,
			NormalOrderShippingFreightCountService normalOrderShippingFreightCountService,
			PointMemberRuleReadService pointMemberRuleReadService,
			PointMemberBalanceReadService pointMemberBalanceReadService,
			CompanyDefaultCurrencyService companyDefaultCurrencyService,
			DistributorGetInfoSimpleByDistributorIdPort distributorGetInfoSimpleByDistributorIdPort,
			NormalOrderCheckoutPointDeductService normalOrderCheckoutPointDeductService,
			OrderCreateFormatDataPort orderCreateFormatDataPort,
			PointsmallFreightMoneyConvertPort pointsmallFreightMoneyConvertPort) {
		this.normalOrderNumericIdService = normalOrderNumericIdService;
		this.orderValiditySettingRedisReadService = orderValiditySettingRedisReadService;
		this.normalOrderShippingFreightCountService = normalOrderShippingFreightCountService;
		this.pointMemberRuleReadService = pointMemberRuleReadService;
		this.pointMemberBalanceReadService = pointMemberBalanceReadService;
		this.companyDefaultCurrencyService = companyDefaultCurrencyService;
		this.distributorGetInfoSimpleByDistributorIdPort = distributorGetInfoSimpleByDistributorIdPort;
		this.normalOrderCheckoutPointDeductService = normalOrderCheckoutPointDeductService;
		this.orderCreateFormatDataPort = orderCreateFormatDataPort;
		this.pointsmallFreightMoneyConvertPort = pointsmallFreightMoneyConvertPort;
	}

	@SuppressWarnings("unchecked")
	public void applyForOnlineWxappPointsmallPhysical(NormalOrderCreateState state) {
		applyForOnlineWxappPhysicalPreview(state, true);
	}

	@SuppressWarnings("unchecked")
	public void applyForOnlineWxappNormalPhysical(NormalOrderCreateState state) {
		applyForOnlineWxappPhysicalPreview(state, false);
	}

	@SuppressWarnings("unchecked")
	private void applyForOnlineWxappPhysicalPreview(NormalOrderCreateState state, boolean pointsmall) {
		Map<String, Object> pr = state.getParams();
		if (!Boolean.TRUE.equals(pr.get("is_online_order"))) {
			return;
		}
		String orderType = trim(pr.get("order_type"));
		if (pointsmall) {
			if (!"normal_pointsmall".equals(orderType)) {
				return;
			}
		} else if (!isOnlineWxappPhysicalPreviewOrderType(orderType)) {
			return;
		}
		Map<String, Object> od = state.getOrderData();
		if (od == null || od.isEmpty()) {
			return;
		}
		long companyId = longVal(od.get("company_id"), 0L);
		long userId = longVal(od.get("user_id"), 0L);
		long distributorId = longVal(od.get("distributor_id"), 0L);
		if (companyId <= 0L || userId <= 0L) {
			return;
		}

		long orderId = normalOrderNumericIdService.generate(userId);
		od.put("order_id", orderId);
		od.put("order_status", "NOTPAY");
		if (!od.containsKey("auto_cancel_time")) {
			int cancelMinutes = resolveCancelMinutesMinutes(companyId, pr);
			od.put("auto_cancel_time", Instant.now().getEpochSecond() + cancelMinutes * 60L);
		}

		od.put("store_name", "");
		od.put("is_distribution", distributorId > 0L);
		od.put("order_holder", "self");
		od.putIfAbsent("remark", stringVal(pr.get("remark")));
		od.put("operator_desc", "");
		od.put("promoter_user_id", 0);
		od.put("promoter_shop_id", 0);
		od.put("sale_salesman_distributor_id", distributorId);
		od.put("bind_salesman_id", 0);
		od.put("bind_salesman_distributor_id", 0);
		od.put("chat_id", intVal(pr.get("chat_id"), 0));
		od.put("third_params", new ArrayList<>());
		od.put("get_point_type", 1);
		od.put("is_profitsharing", 1);
		od.put("profitsharing_rate", "");
		od.put("pack", "");
		od.put("freight_type", "cash");
		od.put("self_delivery_time", intVal(pr.get("self_delivery_time"), 0));
		od.put("source_from", pr.get("source_from"));
		od.put("salespersonInfo", salespersonInfoEmpty());
		od.put("is_logistics", false);
		od.put("item_point", 0);
		od.put("point", 0);
		od.put("commission_fee", 0);
		od.put("total_rebate", 0);
		od.put("supplier_freight_fee", new ArrayList<>());
		od.put("bonus_points", 0);
		od.put("is_shopscreen", intVal(pr.get("isShopScreen"), 0));
		od.put("is_require_subdistrict", false);
		od.put("is_require_building", false);
		od.put("source_id", 0);
		od.put("monitor_id", 0);

		String payChannel = stringVal(pr.get("pay_channel"));
		if (!StringUtils.hasText(payChannel)) {
			payChannel = stringVal(pr.get("pay_type"));
		}
		od.put("pay_channel", payChannel);
		od.put("app_pay_type", "07");

		long merchantId = 0L;
		if (distributorId > 0L) {
			Map<String, Object> di =
					distributorGetInfoSimpleByDistributorIdPort.getInfoSimpleByDistributorId(companyId, distributorId);
			if (di != null && di.get("merchant_id") != null) {
				merchantId = longVal(di.get("merchant_id"), 0L);
			}
		}
		od.put("merchant_id", merchantId);

		applyDefaultCurrency(state);

		applyLogisticsReceiverAndFreightFromParams(state, pointsmall);
		if (!pointsmall) {
			orderCreateFormatDataPort.applyCheckoutCouponAfterFreight(state);

			String countryCode = stringVal(pr.get("country_code"));
			if (!StringUtils.hasText(countryCode)) {
				countryCode = "zh-CN";
			}
			Map<String, Object> pointRule =
					new LinkedHashMap<>(pointMemberRuleReadService.getPointRule(companyId, countryCode));
			od.put("point_rule", pointRule);

			boolean employeePurchase =
					"normal_employee_purchase".equals(orderType)
							|| "employee_purchase".equals(stringVal(od.get("order_class")));
			if (employeePurchase) {
				od.put("max_point", 0);
				od.put("limit_point", 0);
				od.put("is_open_deduct_point", false);
			} else {
				long memberPoint = pointMemberBalanceReadService.getPointBalance(companyId, userId);
				normalOrderCheckoutPointDeductService.applyCheckoutPointDeduct(
						od, pr, companyId, userId, pointRule, memberPoint);
			}
		} else {
			String countryCode = stringVal(pr.get("country_code"));
			if (!StringUtils.hasText(countryCode)) {
				countryCode = "zh-CN";
			}
			od.put("point_rule", new LinkedHashMap<>(pointMemberRuleReadService.getPointRule(companyId, countryCode)));
			od.put("max_point", 0);
			od.put("limit_point", 0);
			od.put("is_open_deduct_point", false);
		}

		applyPromotionDiscountFromDiscountInfo(od);

		long totalFeeAfterPoints = longVal(od.get("total_fee"), 0L);
		int freightFenAfterPoints = intVal(od.get("freight_fee"), 0);
		long promotionDiscount = longVal(od.get("promotion_discount"), 0L);
		long pointFeeItem = longVal(od.get("point_fee_item"), 0L);
		long couponDiscount = longVal(od.get("coupon_discount"), 0L);
		long itemFeeNew =
				totalFeeAfterPoints
						- freightFenAfterPoints
						+ pointFeeItem
						+ couponDiscount
						+ promotionDiscount;
		od.put("item_fee_new", itemFeeNew);

		Object itemsForTotals = od.get("items");
		if (itemsForTotals instanceof List<?> itemList) {
			int totalNum = 0;
			for (Object o : itemList) {
				if (!(o instanceof Map<?, ?> row)) {
					continue;
				}
				Map<String, Object> line = (Map<String, Object>) row;
				line.put("order_id", String.valueOf(orderId));
				line.put("fee_rate", od.get("fee_rate"));
				line.put("fee_type", od.get("fee_type"));
				line.put("fee_symbol", od.get("fee_symbol"));
				String lineMobile = stringVal(od.get("mobile"));
				if (!StringUtils.hasText(lineMobile)) {
					lineMobile = stringVal(pr.get("receiver_mobile"));
				}
				line.put("mobile", lineMobile);
				if (line.get("items_id") == null) {
					line.remove("items_id");
				}
				if (line.get("sale_price") == null) {
					line.put("sale_price", String.valueOf(longVal(line.get("price"), 0L)));
				}
				line.putIfAbsent("commission_fee", "0");
				line.putIfAbsent("rebate", 0);
				line.putIfAbsent("total_rebate", 0);
				line.putIfAbsent("cost_fee", intVal(line.get("cost_price"), 0) * intVal(line.get("num"), 0));
				line.putIfAbsent("item_point", 0);
				line.putIfAbsent("point", 0);
				line.putIfAbsent("is_logistics", false);
				if ("package".equals(trim(line.get("activity_type")))) {
					Object actId = line.get("activity_id");
					if (actId != null) {
						line.putIfAbsent("act_id", actId);
					}
				}
				line.putIfAbsent("order_item_type", "normal");
				line.putIfAbsent("type", "0");
				line.putIfAbsent("crossborder_tax_rate", "");
				line.putIfAbsent("taxstrategy_id", "0");
				line.putIfAbsent("taxation_num", "0");
				line.putIfAbsent("origincountry_id", "0");
				line.putIfAbsent("is_profit", "0");
				line.putIfAbsent("market_price", line.get("market_price") != null ? line.get("market_price") : "0");
				line.putIfAbsent("cost_price", line.get("cost_price") != null ? line.get("cost_price") : "0");
				line.putIfAbsent("is_epidemic", "0");
				line.putIfAbsent("supplier_id", "0");
				line.putIfAbsent("aftersales_end_date", 0);
				line.putIfAbsent("tax_rate", "0");
				line.putIfAbsent("is_medicine", "0");
				line.putIfAbsent("is_prescription", 0);
				totalNum += intVal(line.get("num"), 0);
			}
			od.put("totalItemNum", totalNum);
		}
		od.put("cost_fee", sumItemsCostFeeCents(od.get("items")));
	}

	/** 下单/结算预览：orderData 未带币种时写入公司默认货币（对齐 PHP OrderService::getCur）。 */
	public void applyDefaultCurrency(NormalOrderCreateState state) {
		Map<String, Object> od = state.getOrderData();
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
		od.put("fee_type", cur.getCurrency() != null ? cur.getCurrency() : "CNY");
		od.put("fee_rate", cur.getRate() != null ? cur.getRate() : 1.0);
		od.put("fee_symbol", cur.getSymbol() != null ? cur.getSymbol() : "￥");
	}

	@SuppressWarnings("unchecked")
	public void applyLogisticsReceiverAndFreightFromParams(NormalOrderCreateState state) {
		String ot = trim(state.getParams().get("order_type"));
		boolean pointsmall = "normal_pointsmall".equals(ot);
		if (!"normal".equals(ot)
				&& !pointsmall
				&& !"normal_groups".equals(ot)
				&& !"normal_employee_purchase".equals(ot)) {
			return;
		}
		applyLogisticsReceiverAndFreightFromParams(state, pointsmall);
	}

	@SuppressWarnings("unchecked")
	private void applyLogisticsReceiverAndFreightFromParams(NormalOrderCreateState state, boolean pointsmall) {
		Map<String, Object> pr = state.getParams();
		if (!Boolean.TRUE.equals(pr.get("is_online_order"))) {
			return;
		}
		Map<String, Object> od = state.getOrderData();
		if (od == null || od.isEmpty()) {
			return;
		}
		long companyId = longVal(od.get("company_id"), 0L);
		if (companyId <= 0L) {
			return;
		}
		long distributorId = longVal(od.get("distributor_id"), 0L);
		long merchantId = 0L;
		if (distributorId > 0L) {
			Map<String, Object> di =
					distributorGetInfoSimpleByDistributorIdPort.getInfoSimpleByDistributorId(companyId, distributorId);
			if (di != null && di.get("merchant_id") != null) {
				merchantId = longVal(di.get("merchant_id"), 0L);
			}
		}
		od.put("merchant_id", merchantId);
		String receipt = trim(pr.get("receipt_type"));
		if (StringUtils.hasText(receipt)) {
			od.put("receipt_type", receipt);
		}
		if ("logistics".equals(receipt)) {
			od.put("receiver_name", stringVal(pr.get("receiver_name")));
			od.put("receiver_mobile", stringVal(pr.get("receiver_mobile")));
			od.put("receiver_zip", stringVal(pr.get("receiver_zip")));
			od.put("receiver_state", stringVal(pr.get("receiver_state")));
			od.put("receiver_city", stringVal(pr.get("receiver_city")));
			od.put("receiver_district", stringVal(pr.get("receiver_district")));
			od.put("receiver_address", stringVal(pr.get("receiver_address")));
			Object itemsRaw = od.get("items");
			List<Map<String, Object>> freightRows = new ArrayList<>();
			if (itemsRaw instanceof List<?> lst) {
				for (Object o : lst) {
					if (o instanceof Map<?, ?> m) {
						freightRows.add((Map<String, Object>) m);
					}
				}
			}
			Map<Integer, Integer> supplierFreight = new LinkedHashMap<>();
			long freightFen =
					normalOrderShippingFreightCountService.countFreightFee(
							freightRows,
							companyId,
							stringVal(od.get("receiver_state")),
							stringVal(od.get("receiver_city")),
							stringVal(od.get("receiver_district")),
							false,
							supplierFreight);
			od.put("supplier_freight_fee", supplierFreight);
			int freightInt = (int) Math.min(freightFen, Integer.MAX_VALUE);
			if (Boolean.TRUE.equals(pr.get("_groups_free_post"))) {
				freightInt = 0;
			}
			if (pointsmall) {
				applyPointsmallFreightTotals(od, companyId, freightInt);
			} else {
				od.put("freight_fee", freightInt);
				long goodsSubtotal = sumItemsTotalFeeCents(od.get("items"));
				od.put("total_fee", goodsSubtotal > 0L ? goodsSubtotal + freightInt : 0L);
			}
		} else if ("ziti".equals(receipt)) {
			od.put("receiver_name", stringVal(pr.get("receiver_name")));
			od.put("receiver_mobile", stringVal(pr.get("receiver_mobile")));
			od.put("freight_fee", 0);
			Object itemsRaw = od.get("items");
			List<Map<String, Object>> allItems = new ArrayList<>();
			List<Map<String, Object>> logisticsItems = new ArrayList<>();
			if (itemsRaw instanceof List<?> lst) {
				for (Object o : lst) {
					if (!(o instanceof Map<?, ?> m)) {
						continue;
					}
					Map<String, Object> row = (Map<String, Object>) m;
					allItems.add(row);
					if (normalizeIsLogistics(od.get("is_logistics"))
							&& normalizeIsLogistics(row.get("is_logistics"))) {
						logisticsItems.add(row);
					}
				}
			}
			int freightInt = 0;
			if (!logisticsItems.isEmpty()) {
				od.put("receiver_zip", stringVal(pr.get("receiver_zip")));
				od.put("receiver_state", stringVal(pr.get("receiver_state")));
				od.put("receiver_city", stringVal(pr.get("receiver_city")));
				od.put("receiver_district", stringVal(pr.get("receiver_district")));
				od.put("receiver_address", stringVal(pr.get("receiver_address")));
				Map<Integer, Integer> supplierFreight = new LinkedHashMap<>();
				long freightFen =
						normalOrderShippingFreightCountService.countFreightFee(
								logisticsItems,
								companyId,
								stringVal(od.get("receiver_state")),
								stringVal(od.get("receiver_city")),
								stringVal(od.get("receiver_district")),
								false,
								supplierFreight);
				od.put("supplier_freight_fee", supplierFreight);
				freightInt = (int) Math.min(freightFen, Integer.MAX_VALUE);
				od.put("freight_fee", freightInt);
			}
			if (!allItems.isEmpty() && logisticsItems.size() == allItems.size()) {
				od.put("receipt_type", "logistics");
			} else {
				od.put("receipt_type", "ziti");
				od.putIfAbsent("ziti_status", "PENDING");
			}
			long goodsSubtotal = sumItemsTotalFeeCents(od.get("items"));
			if (pointsmall) {
				applyPointsmallFreightTotals(od, companyId, freightInt);
			} else {
				od.put("total_fee", goodsSubtotal > 0L ? goodsSubtotal + freightInt : 0L);
			}
		} else {
			od.putIfAbsent("freight_fee", 0);
			if (pointsmall) {
				applyPointsmallFreightTotals(od, companyId, 0);
			}
		}
	}

	private void applyPointsmallFreightTotals(Map<String, Object> od, long companyId, int freightFen) {
		long itemPoint = longVal(od.get("point"), 0L);
		if (itemPoint <= 0L) {
			itemPoint = sumItemsPointCents(od.get("items"));
			od.put("point", itemPoint);
			od.put("item_point", itemPoint);
		}
		long cashGoodsTotal = longVal(od.get("total_fee"), 0L);
		Map<String, Object> converted = pointsmallFreightMoneyConvertPort.moneyToPoint(companyId, freightFen);
		String freightType = stringVal(converted.get("freight_type"));
		long freightConverted = longVal(converted.get("money"), freightFen);
		od.put("freight_type", freightType.isEmpty() ? "cash" : freightType);
		od.put("freight_fee", (int) Math.min(freightConverted, Integer.MAX_VALUE));
		if ("point".equals(od.get("freight_type"))) {
			od.put("point", itemPoint + freightConverted);
			od.put("total_fee", cashGoodsTotal);
		} else {
			od.put("total_fee", cashGoodsTotal + freightConverted);
		}
	}

	private static long sumItemsPointCents(Object itemsRaw) {
		if (!(itemsRaw instanceof List<?> lst)) {
			return 0L;
		}
		long s = 0L;
		for (Object o : lst) {
			if (o instanceof Map<?, ?> m) {
				int itemPoint = intVal(m.get("item_point"), 0);
				int num = intVal(m.get("num"), 1);
				s += (long) itemPoint * num;
			}
		}
		return s;
	}

	private static Map<String, Object> salespersonInfoEmpty() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("user_id", "");
		m.put("name", "");
		m.put("mobile", "");
		return m;
	}

	private int resolveCancelMinutesMinutes(long companyId, Map<String, Object> pr) {
		String payType = stringVal(pr.get("pay_type"));
		if ("offline".equals(payType)) {
			return 24 * 60 * 365;
		}
		if ("offline_pay".equals(payType)) {
			return 24 * 60 * 365;
		}
		Map<String, Object> setting = orderValiditySettingRedisReadService.readPlatformSetting(companyId);
		Object raw = setting.get("order_cancel_time");
		if (raw instanceof Number n) {
			return Math.max(1, n.intValue());
		}
		try {
			return Math.max(1, Integer.parseInt(String.valueOf(raw).trim()));
		} catch (Exception e) {
			return 15;
		}
	}

	private static long sumItemsCostFeeCents(Object itemsRaw) {
		if (!(itemsRaw instanceof List<?> lst)) {
			return 0L;
		}
		long s = 0L;
		for (Object o : lst) {
			if (o instanceof Map<?, ?> m) {
				if (m.containsKey("cost_fee")) {
					s += longVal(m.get("cost_fee"), 0L);
				} else {
					s += longVal(m.get("cost_price"), 0L);
				}
			}
		}
		return s;
	}

	private static long sumItemsTotalFeeCents(Object itemsRaw) {
		if (!(itemsRaw instanceof List<?> lst)) {
			return 0L;
		}
		long s = 0L;
		for (Object o : lst) {
			if (o instanceof Map<?, ?> m) {
				s += longVal(m.get("total_fee"), 0L);
			}
		}
		return s;
	}

	private static void applyPromotionDiscountFromDiscountInfo(Map<String, Object> od) {
		int sum = 0;
		Object di = od.get("discount_info");
		if (di instanceof Map<?, ?> map) {
			for (Object value : map.values()) {
				if (!(value instanceof Map<?, ?> desc)) {
					continue;
				}
				Object type = desc.get("type");
				if (!PROMOTION_DISCOUNT_TYPES.contains(String.valueOf(type))) {
					continue;
				}
				sum += intVal(desc.get("discount_fee"), 0);
			}
		} else if (di instanceof List<?> list) {
			for (Object el : list) {
				if (!(el instanceof Map<?, ?> desc)) {
					continue;
				}
				Object type = desc.get("type");
				if (!PROMOTION_DISCOUNT_TYPES.contains(String.valueOf(type))) {
					continue;
				}
				sum += intVal(desc.get("discount_fee"), 0);
			}
		}
		od.put("promotion_discount", sum);
	}

	private static boolean isOnlineWxappPhysicalPreviewOrderType(String orderType) {
		return "normal".equals(orderType)
				|| "normal_groups".equals(orderType)
				|| "normal_employee_purchase".equals(orderType);
	}

	private static boolean normalizeIsLogistics(Object v) {
		if (v == null) {
			return false;
		}
		if (Boolean.TRUE.equals(v)) {
			return true;
		}
		if (v instanceof String s && "true".equalsIgnoreCase(s.trim())) {
			return true;
		}
		return false;
	}

	private static String trim(Object v) {
		return v == null ? "" : v.toString().trim();
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
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
}
