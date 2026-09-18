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

import cn.shopex.ecshopx.espier.domain.Subdistrict;
import cn.shopex.ecshopx.espier.mapper.SubdistrictMapper;
import cn.shopex.ecshopx.orders.config.OrderAppPayTypeDescHolder;
import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdminOrderDetailPhpParityApplier {

	private static final String DEFAULT_APP_PAY_TYPE_DESC = "微信小程序";
	private static final String OFFLINE_PAY_LANG = "zh-CN";

	private static final Set<String> ORDER_INFO_NULL_WHEN_ZERO_KEYS = Set.of("act_id");

	private static final Set<String> ORDER_INFO_STRING_ID_KEYS =
			Set.of(
					"company_id",
					"user_id",
					"distributor_id",
					"ziti_code",
					"shop_id",
					"salesman_id",
					"sale_salesman_distributor_id",
					"bind_salesman_id",
					"bind_salesman_distributor_id",
					"merchant_id",
					"subdistrict_parent_id",
					"subdistrict_id",
					"self_delivery_operator_id");

	private static final Set<String> ORDER_INFO_MONEY_STRING_KEYS =
			Set.of("item_fee", "total_fee", "market_fee");

	private static final Set<String> ORDER_INFO_REMOVE_KEYS =
			Set.of("is_online_order", "coupon_discount_desc", "member_discount_desc", "original_order_id");

	private static final Set<String> ITEM_REMOVE_KEYS =
			Set.of(
					"add_service_info",
					"coupon_discount_desc",
					"member_discount_desc",
					"type",
					"up_share_points");

	private static final Set<String> WXAPP_DISTRIBUTOR_REMOVE_KEYS =
			Set.of(
					"source_from",
					"show_mobile",
					"show_salesperson",
					"distributor_category_id",
					"merchant_name");

	private static final Set<String> ITEM_STRING_ID_KEYS =
			Set.of(
					"id",
					"order_id",
					"company_id",
					"user_id",
					"act_id",
					"goods_id",
					"item_id",
					"shop_id",
					"distributor_id");

	private static final Set<String> PROFIT_STRING_KEYS =
			Set.of(
					"id",
					"order_id",
					"order_profit_status",
					"company_id",
					"total_fee",
					"pay_fee",
					"user_id",
					"dealer_id",
					"distributor_id",
					"order_distributor_id",
					"distributor_nid",
					"seller_id",
					"popularize_distributor_id",
					"popularize_seller_id",
					"proprietary",
					"popularize_proprietary",
					"dealers",
					"distributor",
					"seller",
					"popularize_distributor",
					"popularize_seller",
					"commission",
					"plan_close_time");

	private final SubdistrictMapper subdistrictMapper;
	private final StringRedisTemplate companysRedisTemplate;
	private final OrderAppPayTypeDescHolder orderAppPayTypeDescHolder;
	private final ObjectMapper objectMapper;

	public AdminOrderDetailPhpParityApplier(
			SubdistrictMapper subdistrictMapper,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			OrderAppPayTypeDescHolder orderAppPayTypeDescHolder,
			ObjectMapper objectMapper) {
		this.subdistrictMapper = subdistrictMapper;
		this.companysRedisTemplate = companysRedisTemplate;
		this.orderAppPayTypeDescHolder = orderAppPayTypeDescHolder;
		this.objectMapper = objectMapper;
	}

	@SuppressWarnings("unchecked")
	public void apply(long companyId, Map<String, Object> result) {
		Map<String, Object> orderInfo = (Map<String, Object>) result.get("orderInfo");
		if (orderInfo == null) {
			return;
		}

		normalizeEmptyCollections(result, orderInfo);
		applySubdistrictLabels(orderInfo);
		applyOfflinePayName(companyId, orderInfo);
		applyAppPayTypeDesc(orderInfo);
		applyNewDeliveryDefaults(orderInfo);
		normalizeOrderScalars(orderInfo);
		normalizeItems(orderInfo);
		normalizeDistributorMaps(orderInfo, (Map<String, Object>) result.get("distributor"));
		rewriteTradesToCamelCase(result);
		result.put("profit", formatProfitForResponse(result.get("profit")));
	}

	@SuppressWarnings("unchecked")
	public void applyWxappDetail(long companyId, Map<String, Object> result) {
		Map<String, Object> orderInfo = (Map<String, Object>) result.get("orderInfo");
		if (orderInfo == null) {
			return;
		}

		normalizeEmptyCollections(result, orderInfo);
		normalizeWxappEmptyCollectionJson(orderInfo);
		applySubdistrictLabels(orderInfo);
		applyOfflinePayName(companyId, orderInfo);
		applyAppPayTypeDesc(orderInfo);
		applyNewDeliveryDefaults(orderInfo);
		normalizeOrderScalarsWxapp(orderInfo, objectMapper);
		normalizeItemsWxapp(orderInfo);
		normalizeDistributorMapsWxapp(orderInfo, (Map<String, Object>) result.get("distributor"));
		rewriteTradesToCamelCase(result);
	}

	private static void normalizeEmptyCollections(Map<String, Object> result, Map<String, Object> orderInfo) {
		Object cd = result.get("cancelData");
		if (cd instanceof Map<?, ?> m && m.isEmpty()) {
			result.put("cancelData", new ArrayList<Object>());
		}
		Object dada = orderInfo.get("dada");
		if (dada instanceof Map<?, ?> dm && dm.isEmpty()) {
			orderInfo.put("dada", new ArrayList<Object>());
		}
	}

	private static void normalizeWxappEmptyCollectionJson(Map<String, Object> orderInfo) {
		Object inv = orderInfo.get("invoice_info");
		if (inv instanceof Map<?, ?> im && im.isEmpty()) {
			orderInfo.put("invoice_info", new ArrayList<Object>());
		}
	}

	private static void normalizeOrderScalarsWxapp(Map<String, Object> orderInfo, ObjectMapper objectMapper) {
		for (String key : ORDER_INFO_REMOVE_KEYS) {
			orderInfo.remove(key);
		}
		putEmptyStringIfNull(orderInfo, "delivery_corp");
		putEmptyStringIfNull(orderInfo, "delivery_code");
		decodeThirdParams(orderInfo, objectMapper);
		Object feeRate = orderInfo.get("fee_rate");
		if (feeRate instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue())) {
			orderInfo.put("fee_rate", n.intValue());
		}
		Object autoClose = orderInfo.get("order_auto_close_aftersales_time");
		if (autoClose instanceof Number n && n.longValue() == 0L) {
			orderInfo.put("order_auto_close_aftersales_time", null);
		}
	}

	private static void decodeThirdParams(Map<String, Object> orderInfo, ObjectMapper objectMapper) {
		Object raw = orderInfo.get("third_params");
		if (raw instanceof String str && StringUtils.hasText(str.trim())) {
			try {
				Object parsed = objectMapper.readValue(str.trim(), new TypeReference<Object>() {});
				orderInfo.put("third_params", parsed);
				return;
			} catch (Exception ignored) {
			}
		}
		if (raw == null) {
			orderInfo.put("third_params", new ArrayList<Object>());
		}
	}

	@SuppressWarnings("unchecked")
	private static void normalizeItemsWxapp(Map<String, Object> orderInfo) {
		Object itemsObj = orderInfo.get("items");
		if (!(itemsObj instanceof List<?> list)) {
			return;
		}
		for (Object el : list) {
			if (!(el instanceof Map<?, ?>)) {
				continue;
			}
			Map<String, Object> item = (Map<String, Object>) el;
			for (String key : ITEM_REMOVE_KEYS) {
				item.remove(key);
			}
			Object feeRate = item.get("fee_rate");
			if (feeRate instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue())) {
				item.put("fee_rate", n.intValue());
			}
			if (!item.containsKey("medicine_symptom")) {
				item.put("medicine_symptom", new ArrayList<Object>());
			}
			Object weight = item.get("weight");
			if (weight instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue())) {
				item.put("weight", n.intValue());
			}
			Object din = item.get("delivery_item_num");
			if (din instanceof Number n && n.intValue() == 0 && !"DONE".equals(str(item.get("delivery_status")))) {
				item.put("delivery_item_num", null);
			}
		}
	}

	private static void normalizeDistributorMapsWxapp(
			Map<String, Object> orderInfo, Map<String, Object> distributorRoot) {
		Object distInfo = orderInfo.get("distributor_info");
		if (distInfo instanceof Map<?, ?> m) {
			@SuppressWarnings("unchecked")
			Map<String, Object> dm = (Map<String, Object>) m;
			normalizeDistributorReviewStatusWxapp(dm);
			orderInfo.put("distributor_info", dm);
		}
		if (distributorRoot != null) {
			normalizeDistributorReviewStatusWxapp(distributorRoot);
		}
	}

	private static void normalizeDistributorReviewStatusWxapp(Map<String, Object> dm) {
		for (String key : WXAPP_DISTRIBUTOR_REMOVE_KEYS) {
			dm.remove(key);
		}
		putNullIfAbsent(dm, "house_number");
		putNullIfAbsent(dm, "split_ledger_info");
		putNullIfAbsent(dm, "bspay_split_ledger_info");
		putNullIfAbsent(dm, "shansong_store_id");
		Object rs = dm.get("review_status");
		if (rs instanceof Number n) {
			dm.put("review_status", n.intValue() != 0);
		}
	}

	private static void putNullIfAbsent(Map<String, Object> dm, String key) {
		if (!dm.containsKey(key)) {
			dm.put(key, null);
		}
	}

	private void applySubdistrictLabels(Map<String, Object> orderInfo) {
		LinkedHashSet<Long> ids = new LinkedHashSet<>();
		Long parentId = longOrNull(orderInfo.get("subdistrict_parent_id"));
		Long sid = longOrNull(orderInfo.get("subdistrict_id"));
		if (parentId != null && parentId > 0L) {
			ids.add(parentId);
		}
		if (sid != null && sid > 0L) {
			ids.add(sid);
		}
		Map<Long, String> labelById = new LinkedHashMap<>();
		if (!ids.isEmpty()) {
			List<Subdistrict> list =
					subdistrictMapper.selectList(new LambdaQueryWrapper<Subdistrict>().in(Subdistrict::getId, ids));
			for (Subdistrict s : list) {
				labelById.put(s.getId(), s.getLabel() != null ? s.getLabel() : "");
			}
		}
		orderInfo.put("subdistrict_parent", parentId != null ? labelById.getOrDefault(parentId, "") : "");
		orderInfo.put("subdistrict", sid != null ? labelById.getOrDefault(sid, "") : "");
	}

	private void applyOfflinePayName(long companyId, Map<String, Object> orderInfo) {
		if (!"offline_pay".equals(str(orderInfo.get("pay_type")))) {
			return;
		}
		String raw =
				companysRedisTemplate
						.opsForValue()
						.get(PaymentSettingRedisKeys.offlinePaySettingKey(companyId, OFFLINE_PAY_LANG));
		Map<String, Object> setting = PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw);
		String nameOverride =
				companysRedisTemplate
						.opsForValue()
						.get(PaymentSettingRedisKeys.offlinePayNameLangKey(companyId, OFFLINE_PAY_LANG));
		if (StringUtils.hasText(nameOverride)) {
			setting.put("pay_name", nameOverride);
		}
		Object payName = setting.get("pay_name");
		orderInfo.put("offline_pay_name", payName != null ? String.valueOf(payName) : null);
	}

	private void applyAppPayTypeDesc(Map<String, Object> orderInfo) {
		String appPayType = str(orderInfo.get("app_pay_type"));
		String configured = orderAppPayTypeDescHolder.descForAppPayTypeOrNull(appPayType);
		if (StringUtils.hasText(configured)) {
			orderInfo.put("app_pay_type_desc", configured);
			return;
		}
		String current = str(orderInfo.get("app_pay_type_desc"));
		if (!StringUtils.hasText(current) || "{}".equals(current.trim())) {
			orderInfo.put("app_pay_type_desc", DEFAULT_APP_PAY_TYPE_DESC);
		}
	}

	private static void applyNewDeliveryDefaults(Map<String, Object> orderInfo) {
		if (!"new".equals(str(orderInfo.get("delivery_type")))) {
			return;
		}
		orderInfo.putIfAbsent("orders_delivery_id", "");
		orderInfo.putIfAbsent("delivery_corp_name", "");
		orderInfo.putIfAbsent("is_all_delivery", false);
	}

	private static void normalizeOrderScalars(Map<String, Object> orderInfo) {
		for (String key : ORDER_INFO_REMOVE_KEYS) {
			orderInfo.remove(key);
		}
		for (String key : ORDER_INFO_NULL_WHEN_ZERO_KEYS) {
			if (!orderInfo.containsKey(key)) {
				continue;
			}
			Object v = orderInfo.get(key);
			if (v == null || "0".equals(String.valueOf(v).trim()) || (v instanceof Number n && n.longValue() == 0L)) {
				orderInfo.put(key, null);
			}
		}
		for (String key : ORDER_INFO_STRING_ID_KEYS) {
			if (orderInfo.containsKey(key)) {
				orderInfo.put(key, stringifyId(orderInfo.get(key)));
			}
		}
		for (String key : ORDER_INFO_MONEY_STRING_KEYS) {
			if (orderInfo.containsKey(key)) {
				orderInfo.put(key, stringifyNumber(orderInfo.get(key)));
			}
		}
		putEmptyStringIfNull(orderInfo, "delivery_corp");
		putEmptyStringIfNull(orderInfo, "delivery_code");
		if (orderInfo.get("third_params") == null) {
			orderInfo.put("third_params", new ArrayList<Object>());
		}
		Object feeRate = orderInfo.get("fee_rate");
		if (feeRate instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue())) {
			orderInfo.put("fee_rate", n.intValue());
		}
		Object estimate = orderInfo.get("estimate_get_points");
		if (estimate != null) {
			orderInfo.put("estimate_get_points", stringifyNumber(estimate));
		}
		Object itemTotal = orderInfo.get("item_total_fee");
		if (itemTotal != null) {
			orderInfo.put("item_total_fee", stringifyNumber(itemTotal));
		}
	}

	@SuppressWarnings("unchecked")
	private static void normalizeItems(Map<String, Object> orderInfo) {
		Object itemsObj = orderInfo.get("items");
		if (!(itemsObj instanceof List<?> list)) {
			return;
		}
		for (Object el : list) {
			if (!(el instanceof Map<?, ?>)) {
				continue;
			}
			Map<String, Object> item = (Map<String, Object>) el;
			for (String key : ITEM_REMOVE_KEYS) {
				item.remove(key);
			}
			for (String key : ITEM_STRING_ID_KEYS) {
				if (item.containsKey(key)) {
					item.put(key, stringifyId(item.get(key)));
				}
			}
			Object feeRate = item.get("fee_rate");
			if (feeRate instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue())) {
				item.put("fee_rate", n.intValue());
			}
			if (!item.containsKey("medicine_symptom")) {
				item.put("medicine_symptom", new ArrayList<Object>());
			}
			Object weight = item.get("weight");
			if (weight instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue())) {
				item.put("weight", n.intValue());
			}
			Object din = item.get("delivery_item_num");
			if (din instanceof Number n && n.intValue() == 0 && !"DONE".equals(str(item.get("delivery_status")))) {
				item.put("delivery_item_num", null);
			}
		}
	}

	private static void normalizeDistributorMaps(
			Map<String, Object> orderInfo, Map<String, Object> distributorRoot) {
		Object distInfo = orderInfo.get("distributor_info");
		if (distInfo instanceof Map<?, ?> m) {
			@SuppressWarnings("unchecked")
			Map<String, Object> dm = (Map<String, Object>) m;
			normalizeDistributorEntry(dm);
			orderInfo.put("distributor_info", dm);
		}
		Object saleDist = orderInfo.get("sale_salesman_distributor_info");
		if (saleDist instanceof Map<?, ?> m) {
			@SuppressWarnings("unchecked")
			Map<String, Object> dm = (Map<String, Object>) m;
			normalizeSaleSalesmanDistributorInfo(dm);
			orderInfo.put("sale_salesman_distributor_info", dm);
		}
		if (distributorRoot != null) {
			normalizeDistributorEntry(distributorRoot);
		}
	}

	private static void normalizeDistributorEntry(Map<String, Object> dm) {
		if (dm.containsKey("company_id")) {
			dm.put("company_id", stringifyId(dm.get("company_id")));
		}
		Object rs = dm.get("review_status");
		if (rs instanceof Boolean b) {
			dm.put("review_status", b ? 1 : 0);
		}
	}

	private static void normalizeSaleSalesmanDistributorInfo(Map<String, Object> dm) {
		for (String key :
				Set.of(
						"company_id",
						"shop_id",
						"merchant_id",
						"regionauth_id",
						"wdt_shop_id",
						"jst_shop_id",
						"kuaizhen_store_id",
						"open_divided")) {
			if (dm.containsKey(key)) {
				dm.put(key, stringifyId(dm.get(key)));
			}
		}
		Object rs = dm.get("review_status");
		if (rs instanceof Boolean b) {
			dm.put("review_status", b);
		} else if (rs instanceof Number n) {
			dm.put("review_status", n.intValue() != 0);
		}
	}

	@SuppressWarnings("unchecked")
	private void rewriteTradesToCamelCase(Map<String, Object> result) {
		Object ti = result.get("tradeInfo");
		if (ti instanceof Map<?, ?> tm) {
			Map<String, Object> src = new LinkedHashMap<>((Map<String, Object>) tm);
			if (!src.isEmpty()) {
				result.put("tradeInfo", OrderTradeInfoPhpParityMaps.tradeSnakeToPhpCamel(src, objectMapper));
			}
		}
		Object tl = result.get("tradeList");
		if (!(tl instanceof List<?> raw)) {
			return;
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object el : raw) {
			if (el instanceof Map<?, ?> m) {
				Map<String, Object> src = new LinkedHashMap<>((Map<String, Object>) m);
				out.add(OrderTradeInfoPhpParityMaps.tradeSnakeToPhpCamel(src, objectMapper));
			}
		}
		result.put("tradeList", out);
	}

	@SuppressWarnings("unchecked")
	public Object formatProfitForResponse(Object profitObj) {
		Map<String, Object> row = null;
		if (profitObj instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> m) {
			row = new LinkedHashMap<>((Map<String, Object>) m);
		} else if (profitObj instanceof Map<?, ?> m) {
			row = new LinkedHashMap<>((Map<String, Object>) m);
		}
		if (row == null || row.isEmpty()) {
			return new ArrayList<Object>();
		}
		for (String key : PROFIT_STRING_KEYS) {
			if (row.containsKey(key)) {
				row.put(key, stringifyNumber(row.get(key)));
			}
		}
		row.put("rule", decodeRule(row.get("rule")));
		row.put("distributor_info", emptyInfoToList(row.get("distributor_info")));
		row.put("seller_info", emptyInfoToList(row.get("seller_info")));
		row.put("popularize_seller_info", emptyInfoToList(row.get("popularize_seller_info")));
		return row;
	}

	private static Object emptyInfoToList(Object info) {
		if (info instanceof Map<?, ?> m && m.isEmpty()) {
			return new ArrayList<Object>();
		}
		if (info == null) {
			return new ArrayList<Object>();
		}
		return info;
	}

	private Object decodeRule(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Map<?, ?>) {
			return raw;
		}
		if (!(raw instanceof String str) || !StringUtils.hasText(str.trim())) {
			return raw;
		}
		try {
			return objectMapper.readValue(str.trim(), new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			return raw;
		}
	}

	private static void putEmptyStringIfNull(Map<String, Object> m, String key) {
		if (m.get(key) == null) {
			m.put(key, "");
		}
	}

	private static String stringifyId(Object o) {
		if (o == null) {
			return "0";
		}
		if (o instanceof String s) {
			return s;
		}
		return String.valueOf(o);
	}

	private static String stringifyNumber(Object o) {
		if (o == null) {
			return "0";
		}
		if (o instanceof String s) {
			return s;
		}
		if (o instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue())) {
			return String.valueOf(n.longValue());
		}
		return String.valueOf(o);
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private static Long longOrNull(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
