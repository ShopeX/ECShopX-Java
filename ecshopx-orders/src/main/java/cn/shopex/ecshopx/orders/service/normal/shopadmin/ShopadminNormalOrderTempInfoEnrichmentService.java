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

package cn.shopex.ecshopx.orders.service.normal.shopadmin;

import cn.shopex.ecshopx.common.distribution.DistributorGetInfoSimpleByDistributorIdPort;
import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.orders.service.excard.NormalOrderNumericIdService;
import cn.shopex.ecshopx.orders.service.normal.create.NormalOrderCheckoutPointDeductService;
import cn.shopex.ecshopx.orders.service.normal.create.NormalOrderCreateState;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import cn.shopex.ecshopx.point.service.PointMemberBalanceReadService;
import cn.shopex.ecshopx.point.service.PointMemberRuleReadService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ShopadminNormalOrderTempInfoEnrichmentService {

	private static final Set<String> PROMOTION_DISCOUNT_TYPES =
			Set.of("full_minus", "full_discount", "member_tag_targeted_promotion");

	private final NormalOrderNumericIdService normalOrderNumericIdService;
	private final OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;
	private final PointMemberRuleReadService pointMemberRuleReadService;
	private final PointMemberBalanceReadService pointMemberBalanceReadService;
	private final CompanyDefaultCurrencyService companyDefaultCurrencyService;
	private final DistributorGetInfoSimpleByDistributorIdPort distributorGetInfoSimpleByDistributorIdPort;
	private final NormalOrderCheckoutPointDeductService normalOrderCheckoutPointDeductService;
	private final MarketingActivityCatalogAccess marketingActivityCatalogAccess;

	public ShopadminNormalOrderTempInfoEnrichmentService(
			NormalOrderNumericIdService normalOrderNumericIdService,
			OrderValiditySettingRedisReadService orderValiditySettingRedisReadService,
			PointMemberRuleReadService pointMemberRuleReadService,
			PointMemberBalanceReadService pointMemberBalanceReadService,
			CompanyDefaultCurrencyService companyDefaultCurrencyService,
			DistributorGetInfoSimpleByDistributorIdPort distributorGetInfoSimpleByDistributorIdPort,
			NormalOrderCheckoutPointDeductService normalOrderCheckoutPointDeductService,
			MarketingActivityCatalogAccess marketingActivityCatalogAccess) {
		this.normalOrderNumericIdService = normalOrderNumericIdService;
		this.orderValiditySettingRedisReadService = orderValiditySettingRedisReadService;
		this.pointMemberRuleReadService = pointMemberRuleReadService;
		this.pointMemberBalanceReadService = pointMemberBalanceReadService;
		this.companyDefaultCurrencyService = companyDefaultCurrencyService;
		this.distributorGetInfoSimpleByDistributorIdPort = distributorGetInfoSimpleByDistributorIdPort;
		this.normalOrderCheckoutPointDeductService = normalOrderCheckoutPointDeductService;
		this.marketingActivityCatalogAccess = marketingActivityCatalogAccess;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> apply(NormalOrderCreateState state) {
		Map<String, Object> pr = state.getParams();
		Map<String, Object> od = state.getOrderData();
		if (od == null || od.isEmpty()) {
			return new LinkedHashMap<>();
		}
		long companyId = longVal(od.get("company_id"), 0L);
		long userId = longVal(od.get("user_id"), 0L);
		long distributorId = longVal(od.get("distributor_id"), 0L);

		long orderId = normalOrderNumericIdService.generate(userId);
		od.put("order_id", orderId);
		od.put("order_status", "NOTPAY");
		int cancelMinutes = resolveCancelMinutes(companyId, pr);
		od.put("auto_cancel_time", Instant.now().getEpochSecond() + cancelMinutes * 60L);

		od.put("shop_id", 0);
		od.put("store_name", "");
		od.put("is_distribution", distributorId > 0L);
		od.put("order_holder", "self");
		od.putIfAbsent("remark", stringVal(pr.get("remark")));
		od.put("operator_desc", "");
		od.put("promoter_user_id", 0);
		od.put("promoter_shop_id", 0);
		od.put("sale_salesman_distributor_id", distributorId > 0L ? String.valueOf(distributorId) : 0);
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
		od.put("salespersonInfo", salespersonInfoEmpty());
		od.put("invoice_status", "");
		od.put("is_logistics", false);
		od.put("item_point", 0);
		od.put("point", 0);
		od.put("commission_fee", 0);
		od.put("total_rebate", 0);
		od.put("supplier_freight_fee", new ArrayList<>());
		od.put("is_shopscreen", intVal(pr.get("isShopScreen"), 0));
		od.put("source_id", 0);
		od.put("monitor_id", 0);
		od.put("receiver_name", stringVal(pr.get("receiver_name")));
		od.put("receiver_mobile", stringVal(pr.get("receiver_mobile")));

		String payChannel = stringVal(pr.get("pay_channel"));
		od.put("pay_channel", StringUtils.hasText(payChannel) ? payChannel : null);
		od.put("app_pay_type", "07");

		long merchantId = 0L;
		if (distributorId > 0L) {
			Map<String, Object> di =
					distributorGetInfoSimpleByDistributorIdPort.getInfoSimpleByDistributorId(companyId, distributorId);
			if (di != null && di.get("merchant_id") != null) {
				merchantId = longVal(di.get("merchant_id"), 0L);
			}
		}
		od.put("merchant_id", String.valueOf(merchantId));

		CurrencyExchangeRate cur = companyDefaultCurrencyService.getCur(companyId);
		od.put("fee_type", cur.getCurrency() != null ? cur.getCurrency() : "CNY");
		od.put("fee_rate", cur.getRate() != null ? cur.getRate() : 1.0);
		od.put("fee_symbol", cur.getSymbol() != null ? cur.getSymbol() : "￥");

		Map<String, Object> pointRule =
				new LinkedHashMap<>(pointMemberRuleReadService.getPointRule(companyId));
		od.put("point_rule", normalizePointRuleForCheckout(pointRule));

		long memberPoint = userId > 0L ? pointMemberBalanceReadService.getPointBalance(companyId, userId) : 0L;
		normalOrderCheckoutPointDeductService.applyCheckoutPointDeduct(
				od, pr, companyId, userId, pointRule, memberPoint);
		od.put("user_point", intVal(od.get("user_point"), 0));

		if (userId <= 0L) {
			od.put("bind_auth_code", String.valueOf(100000 + ThreadLocalRandom.current().nextInt(900000)));
		}

		mergeSkuOntoItems(state, companyId);
		applyPromotionDiscountFromDiscountInfo(od);
		enrichItemLines(od, orderId);

		int totalItemNum = 0;
		Object itemsRaw = od.get("items");
		if (itemsRaw instanceof List<?> itemList) {
			for (Object o : itemList) {
				if (o instanceof Map<?, ?> row) {
					totalItemNum += intVal(row.get("num"), 0);
				}
			}
		}
		od.put("totalItemNum", totalItemNum);
		od.put("cost_fee", sumItemsCostFeeCents(itemsRaw));

		long totalFeeAfterPoints = longVal(od.get("total_fee"), 0L);
		int freightFenAfterPoints = intVal(od.get("freight_fee"), 0);
		long promotionDiscount = longVal(od.get("promotion_discount"), 0L);
		long pointFeeItem = longVal(od.get("point_fee_item"), 0L);
		long couponDiscount = longVal(od.get("coupon_discount"), 0L);
		long memberDisc = longVal(od.get("member_discount"), 0L);
		long itemFeeNew =
				totalFeeAfterPoints
						- freightFenAfterPoints
						+ pointFeeItem
						+ couponDiscount
						+ promotionDiscount
						+ memberDisc;
		od.put("item_fee_new", itemFeeNew);

		if (pr.containsKey("source_from")) {
			od.put("source_from", pr.get("source_from"));
		}

		od.remove("order_type_slug");
		od.remove("promotion");
		od.remove("items_promotion");
		od.remove("max_point_ziti");

		return applyPhpResponseTypes(new LinkedHashMap<>(od));
	}

	@SuppressWarnings("unchecked")
	private void mergeSkuOntoItems(NormalOrderCreateState state, long companyId) {
		Map<String, Object> od = state.getOrderData();
		Object itemsRaw = od.get("items");
		if (!(itemsRaw instanceof List<?> itemList) || itemList.isEmpty()) {
			return;
		}
		LinkedHashSet<Long> ids = new LinkedHashSet<>();
		for (Object o : itemList) {
			if (o instanceof Map<?, ?> row) {
				long iid = longVal(row.get("item_id"), 0L);
				if (iid > 0L) {
					ids.add(iid);
				}
			}
		}
		if (ids.isEmpty()) {
			return;
		}
		Map<String, Object> pack =
				marketingActivityCatalogAccess.loadSkuItemsListForMarketingGift(companyId, new ArrayList<>(ids));
		Map<Long, Map<String, Object>> byId = new LinkedHashMap<>();
		Object listObj = pack.get("list");
		if (listObj instanceof List<?> skuLines) {
			for (Object o : skuLines) {
				if (o instanceof Map<?, ?> sku) {
					long iid = longVal(sku.get("item_id"), 0L);
					if (iid > 0L) {
						byId.put(iid, (Map<String, Object>) sku);
					}
				}
			}
		}
		List<Map<String, Object>> merged = new ArrayList<>();
		for (Object o : itemList) {
			if (!(o instanceof Map<?, ?> lineRaw)) {
				continue;
			}
			Map<String, Object> line = new LinkedHashMap<>((Map<String, Object>) lineRaw);
			long itemId = longVal(line.get("item_id"), 0L);
			Map<String, Object> sku = byId.get(itemId);
			if (sku != null) {
				mergeSkuFieldsOntoLine(line, sku, itemId);
			}
			line.put("shop_id", 0);
			merged.add(line);
		}
		od.put("items", merged);
	}

	private static void mergeSkuFieldsOntoLine(Map<String, Object> line, Map<String, Object> sku, long itemId) {
		if (!StringUtils.hasText(stringVal(line.get("item_bn")))) {
			line.put("item_bn", stringVal(sku.get("item_bn")));
		}
		if (!StringUtils.hasText(stringVal(line.get("goods_bn")))) {
			line.put("goods_bn", stringVal(sku.get("goods_bn")));
		}
		if (!StringUtils.hasText(stringVal(line.get("item_unit")))) {
			line.put("item_unit", stringVal(sku.get("item_unit")));
		}
		if (sku.get("templates_id") != null) {
			line.put("templates_id", sku.get("templates_id"));
		} else if (sku.get("template_id") != null) {
			line.put("templates_id", sku.get("template_id"));
		}
		if (line.get("item_spec_desc") == null || !StringUtils.hasText(stringVal(line.get("item_spec_desc")))) {
			Object spec = sku.get("item_spec_desc");
			if (spec != null && StringUtils.hasText(String.valueOf(spec))) {
				line.put("item_spec_desc", spec);
			}
		}
		if (sku.get("weight") != null) {
			line.put("weight", sku.get("weight"));
		}
		if (sku.get("volume") != null) {
			line.put("volume", sku.get("volume"));
		}
		line.putIfAbsent("volume", 0);
		line.putIfAbsent("weight", 0);
		if (sku.containsKey("market_price")) {
			line.put("market_price", sku.get("market_price"));
		}
		if (sku.containsKey("cost_price")) {
			line.put("cost_price", intVal(sku.get("cost_price"), intVal(line.get("cost_price"), 0)));
		}
		if (sku.get("item_category") != null) {
			line.put("item_category", String.valueOf(sku.get("item_category")));
		}
		long goodsIdFromSku = longVal(sku.get("goods_id"), 0L);
		if (goodsIdFromSku > 0L) {
			line.put("goods_id", goodsIdFromSku);
		}
		Object defaultItemId = sku.get("default_item_id");
		line.put("default_item_id", defaultItemId != null ? defaultItemId : itemId);
		line.putIfAbsent("supplier_id", sku.get("supplier_id") != null ? sku.get("supplier_id") : "0");
		line.putIfAbsent("is_epidemic", sku.get("is_epidemic") != null ? sku.get("is_epidemic") : "0");
		line.putIfAbsent("is_medicine", sku.get("is_medicine") != null ? sku.get("is_medicine") : "0");
		line.putIfAbsent("is_prescription", sku.get("is_prescription") != null ? sku.get("is_prescription") : "0");
		line.putIfAbsent("tax_rate", sku.get("tax_rate") != null ? sku.get("tax_rate") : "0");
		line.putIfAbsent("aftersales_end_date", intVal(sku.get("aftersales_end_date"), 0));
		line.putIfAbsent("is_profit", "0");
		if (line.get("is_profit") instanceof Boolean b) {
			line.put("is_profit", b ? "1" : "0");
		} else if ("false".equals(String.valueOf(line.get("is_profit")))) {
			line.put("is_profit", "0");
		} else if (line.get("is_profit") != null) {
			line.put("is_profit", String.valueOf(line.get("is_profit")));
		}
		line.putIfAbsent("is_epidemic", "0");
		line.put("is_epidemic", String.valueOf(line.get("is_epidemic")));
		line.putIfAbsent("supplier_id", "0");
		line.put("supplier_id", String.valueOf(line.get("supplier_id")));
		line.putIfAbsent("tax_rate", "0");
		line.put("tax_rate", String.valueOf(line.get("tax_rate")));
		if (line.get("weight") != null) {
			line.put("weight", intVal(line.get("weight"), 0));
		} else {
			line.put("weight", 0);
		}
		if (line.get("volume") != null) {
			line.put("volume", intVal(line.get("volume"), 0));
		} else {
			line.put("volume", 0);
		}
		line.putIfAbsent("crossborder_tax_rate", stringVal(sku.get("crossborder_tax_rate")));
		line.putIfAbsent("taxstrategy_id", sku.get("taxstrategy_id") != null ? String.valueOf(sku.get("taxstrategy_id")) : "0");
		line.putIfAbsent("taxation_num", sku.get("taxation_num") != null ? String.valueOf(sku.get("taxation_num")) : "0");
		line.putIfAbsent("origincountry_id", sku.get("origincountry_id") != null ? String.valueOf(sku.get("origincountry_id")) : "0");
		line.putIfAbsent("type", sku.get("type") != null ? String.valueOf(sku.get("type")) : "0");
	}

	@SuppressWarnings("unchecked")
	private static void enrichItemLines(Map<String, Object> od, long orderId) {
		Object itemsRaw = od.get("items");
		if (!(itemsRaw instanceof List<?> itemList)) {
			return;
		}
		String orderIdStr = String.valueOf(orderId);
		Object feeRate = od.get("fee_rate");
		Object feeType = od.get("fee_type");
		Object feeSymbol = od.get("fee_symbol");
		String lineMobile = stringVal(od.get("mobile"));
		for (Object o : itemList) {
			if (!(o instanceof Map<?, ?> rowRaw)) {
				continue;
			}
			Map<String, Object> line = (Map<String, Object>) rowRaw;
			line.put("order_id", orderIdStr);
			line.put("fee_rate", feeRate);
			line.put("fee_type", feeType);
			line.put("fee_symbol", feeSymbol);
			line.put("mobile", lineMobile);
			line.putIfAbsent("sale_price", String.valueOf(longVal(line.get("price"), 0L)));
			line.putIfAbsent("commission_fee", "0");
			line.putIfAbsent("rebate", 0);
			line.putIfAbsent("total_rebate", 0);
			int costPrice = intVal(line.get("cost_price"), 0);
			int num = intVal(line.get("num"), 0);
			line.putIfAbsent("cost_fee", costPrice * num);
			line.putIfAbsent("item_point", 0);
			line.putIfAbsent("point", 0);
			line.putIfAbsent("is_logistics", false);
			line.putIfAbsent("order_item_type", "normal");
			line.putIfAbsent("item_spec_desc", stringVal(line.get("item_spec_desc")));
			line.putIfAbsent("default_item_id", line.get("item_id"));
			line.putIfAbsent("type", "0");
			line.putIfAbsent("crossborder_tax_rate", "");
			line.putIfAbsent("taxstrategy_id", "0");
			line.putIfAbsent("taxation_num", "0");
			line.putIfAbsent("origincountry_id", "0");
			line.putIfAbsent("is_profit", "0");
			if (line.get("is_profit") instanceof Boolean b) {
				line.put("is_profit", b ? "1" : "0");
			} else if ("false".equals(String.valueOf(line.get("is_profit")))) {
				line.put("is_profit", "0");
			}
			line.put("is_epidemic", String.valueOf(line.getOrDefault("is_epidemic", "0")));
			line.put("supplier_id", String.valueOf(line.getOrDefault("supplier_id", "0")));
			line.putIfAbsent("aftersales_end_date", 0);
			line.put("tax_rate", String.valueOf(line.getOrDefault("tax_rate", "0")));
			line.putIfAbsent("is_medicine", "0");
			line.putIfAbsent("is_prescription", "0");
			line.put("volume", intVal(line.get("volume"), 0));
			line.put("weight", intVal(line.get("weight"), 0));
			line.put("shop_id", 0);
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> applyPhpResponseTypes(Map<String, Object> od) {
		od.put("order_id", String.valueOf(od.get("order_id")));
		od.put("company_id", String.valueOf(od.get("company_id")));
		if (od.get("operator_id") != null) {
			od.put("operator_id", String.valueOf(od.get("operator_id")));
		}
		od.put("total_fee", String.valueOf(longVal(od.get("total_fee"), 0L)));
		od.put("item_fee", intVal(od.get("item_fee"), 0));
		od.put("market_fee", intVal(od.get("market_fee"), 0));
		od.put("cost_fee", intVal(od.get("cost_fee"), 0));
		if (od.get("ziti_code") != null) {
			od.put("ziti_code", String.valueOf(od.get("ziti_code")));
		}
		Object itemsRaw = od.get("items");
		if (itemsRaw instanceof List<?> itemList) {
			List<Map<String, Object>> typedItems = new ArrayList<>();
			for (Object o : itemList) {
				if (!(o instanceof Map<?, ?> rowRaw)) {
					continue;
				}
				Map<String, Object> line = new LinkedHashMap<>((Map<String, Object>) rowRaw);
				line.put("goods_id", String.valueOf(line.get("goods_id")));
				line.put("item_id", String.valueOf(line.get("item_id")));
				line.put("company_id", String.valueOf(line.get("company_id")));
				line.put("num", String.valueOf(intVal(line.get("num"), 0)));
				line.put("sale_price", String.valueOf(longVal(line.get("sale_price"), longVal(line.get("price"), 0L))));
				line.put("commission_fee", String.valueOf(line.get("commission_fee")));
				if (line.get("templates_id") != null) {
					line.put("templates_id", String.valueOf(line.get("templates_id")));
				}
				line.put("default_item_id", String.valueOf(line.get("default_item_id")));
				line.put("market_price", String.valueOf(line.get("market_price")));
				line.put("cost_price", String.valueOf(line.get("cost_price")));
				line.put("is_medicine", String.valueOf(line.get("is_medicine")));
				line.put("is_prescription", String.valueOf(line.get("is_prescription")));
				if (line.get("item_category") != null) {
					line.put("item_category", String.valueOf(line.get("item_category")));
				}
				typedItems.add(line);
			}
			od.put("items", typedItems);
		}
		return od;
	}

	private int resolveCancelMinutes(long companyId, Map<String, Object> pr) {
		String payType = stringVal(pr.get("pay_type"));
		if ("offline".equals(payType) || "offline_pay".equals(payType)) {
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

	private static void applyPromotionDiscountFromDiscountInfo(Map<String, Object> od) {
		int sum = 0;
		Object di = od.get("discount_info");
		if (di instanceof List<?> list) {
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
					s += (long) intVal(m.get("cost_price"), 0) * intVal(m.get("num"), 0);
				}
			}
		}
		return s;
	}

	private static Map<String, Object> normalizePointRuleForCheckout(Map<String, Object> raw) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("name", stringVal(raw.get("name")));
		out.put("isOpenMemberPoint", phpStringBool(raw.get("isOpenMemberPoint")));
		out.put("gain_point", stringVal(raw.get("gain_point")));
		out.put("gain_limit", stringVal(raw.get("gain_limit")));
		out.put("gain_time", stringVal(raw.get("gain_time")));
		out.put("isOpenDeductPoint", phpStringBool(raw.get("isOpenDeductPoint")));
		out.put("deduct_proportion_limit", stringVal(raw.get("deduct_proportion_limit")));
		out.put("deduct_point", stringVal(raw.get("deduct_point")));
		out.put("access", stringVal(raw.get("access")));
		out.put("rule_desc", stringVal(raw.get("rule_desc")));
		out.put("point_pay_first", stringVal(raw.get("point_pay_first")));
		out.put("can_deduct_freight", stringVal(raw.get("can_deduct_freight")));
		out.put(
				"include_freight",
				raw.containsKey("include_freight")
						? phpStringBool(raw.get("include_freight"))
						: "true");
		out.put(
				"popularize_commission_type",
				raw.containsKey("popularize_commission_type")
						? stringVal(raw.get("popularize_commission_type"))
						: "money");
		return out;
	}

	private static String phpStringBool(Object v) {
		if (v == null) {
			return "false";
		}
		if (v instanceof Boolean b) {
			return b ? "true" : "false";
		}
		if (v instanceof Number n) {
			return n.intValue() != 0 ? "true" : "false";
		}
		String s = v.toString().trim();
		if ("true".equalsIgnoreCase(s) || "1".equals(s)) {
			return "true";
		}
		return "false";
	}

	private static Map<String, Object> salespersonInfoEmpty() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("user_id", "");
		m.put("name", "");
		m.put("mobile", "");
		return m;
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
