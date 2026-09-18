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

package cn.shopex.ecshopx.aftersales.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AftersalesAdminDetailPhpParityApplier {

	private static final String DEFAULT_APP_PAY_TYPE_DESC = "微信小程序";

	private static final Set<String> MAIN_STRING_ID_KEYS =
			Set.of(
					"aftersales_bn",
					"order_id",
					"company_id",
					"user_id",
					"salesman_id",
					"shop_id",
					"distributor_id",
					"merchant_id",
					"return_distributor_id",
					"self_delivery_operator_id");

	private static final Set<String> DETAIL_STRING_ID_KEYS =
			Set.of(
					"detail_id",
					"company_id",
					"user_id",
					"distributor_id",
					"aftersales_bn",
					"order_id",
					"sub_order_id",
					"goods_id",
					"item_id");

	private static final Set<String> REFUND_STRING_ID_KEYS =
			Set.of(
					"refund_bn",
					"aftersales_bn",
					"supplier_id",
					"order_id",
					"trade_id",
					"company_id",
					"user_id",
					"shop_id",
					"distributor_id",
					"merchant_id");

	private static final Set<String> ORDER_INFO_NULL_WHEN_ZERO_KEYS = Set.of("act_id");

	private static final Set<String> ORDER_INFO_STRING_ID_KEYS =
			Set.of(
					"order_id",
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

	private static final Set<String> DISTRIBUTOR_STRING_ID_KEYS =
			Set.of(
					"distributor_id",
					"shop_id",
					"company_id",
					"merchant_id",
					"regionauth_id",
					"wdt_shop_id",
					"jst_shop_id",
					"kuaizhen_store_id",
					"open_divided");

	private static final Set<String> DISTRIBUTOR_REMOVE_KEYS = Set.of("merchant_name", "source_from");

	@SuppressWarnings("unchecked")
	public void apply(Map<String, Object> out) {
		normalizeMainHead(out);
		normalizeRefundInfo((Map<String, Object>) out.get("refund_info"));
		normalizeDetailList((List<Map<String, Object>>) out.get("detail"));
		normalizeOrderInfo((Map<String, Object>) out.get("order_info"));
		Object appInfoObj = out.get("app_info");
		if (appInfoObj instanceof Map<?, ?> appInfoMap) {
			normalizeOrderInfo((Map<String, Object>) appInfoMap.get("order_info"));
		}
		normalizeDistributorInfo((Map<String, Object>) out.get("distributor_info"));
	}

	private static void normalizeMainHead(Map<String, Object> out) {
		if (out == null) {
			return;
		}
		for (String key : MAIN_STRING_ID_KEYS) {
			if (out.containsKey(key)) {
				out.put(key, stringifyId(out.get(key)));
			}
		}
	}

	private static void normalizeRefundInfo(Map<String, Object> refundInfo) {
		if (refundInfo == null || refundInfo.isEmpty()) {
			return;
		}
		for (String key : REFUND_STRING_ID_KEYS) {
			if (refundInfo.containsKey(key)) {
				refundInfo.put(key, stringifyId(refundInfo.get(key)));
			}
		}
		if (refundInfo.get("refund_type") != null) {
			refundInfo.put("refund_type", stringifyId(refundInfo.get("refund_type")));
		}
		Object curPayFee = refundInfo.get("cur_pay_fee");
		if (curPayFee != null) {
			refundInfo.put("cur_pay_fee", stringifyNumber(curPayFee));
		}
		Object feeRate = refundInfo.get("cur_fee_rate");
		if (feeRate instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue())) {
			refundInfo.put("cur_fee_rate", n.intValue());
		}
	}

	private static void normalizeDetailList(List<Map<String, Object>> detailList) {
		if (detailList == null) {
			return;
		}
		for (Map<String, Object> detail : detailList) {
			for (String key : DETAIL_STRING_ID_KEYS) {
				if (detail.containsKey(key)) {
					detail.put(key, stringifyId(detail.get(key)));
				}
			}
			Object orderItemObj = detail.get("orderItem");
			if (orderItemObj instanceof Map<?, ?> orderItemMap) {
				@SuppressWarnings("unchecked")
				Map<String, Object> orderItem = (Map<String, Object>) orderItemMap;
				normalizeOrderItem(orderItem);
			}
		}
	}

	private static void normalizeOrderItem(Map<String, Object> item) {
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
		Object weight = item.get("weight");
		if (weight instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue())) {
			item.put("weight", n.intValue());
		}
		Object supplierName = item.get("supplier_name");
		if (supplierName instanceof Map<?, ?> m && m.isEmpty()) {
			item.put("supplier_name", "");
		}
	}

	private static void normalizeOrderInfo(Map<String, Object> orderInfo) {
		if (orderInfo == null || orderInfo.isEmpty()) {
			return;
		}
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
		if (orderInfo.get("third_params") == null) {
			orderInfo.put("third_params", new ArrayList<Object>());
		}
		Object feeRate = orderInfo.get("fee_rate");
		if (feeRate instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue())) {
			orderInfo.put("fee_rate", n.intValue());
		}
		applyAppPayTypeDesc(orderInfo);
	}

	private static void applyAppPayTypeDesc(Map<String, Object> orderInfo) {
		String current = str(orderInfo.get("app_pay_type_desc"));
		if (!org.springframework.util.StringUtils.hasText(current) || "{}".equals(current.trim())) {
			orderInfo.put("app_pay_type_desc", DEFAULT_APP_PAY_TYPE_DESC);
		}
	}

	private static void normalizeDistributorInfo(Map<String, Object> distributorInfo) {
		if (distributorInfo == null || distributorInfo.isEmpty()) {
			return;
		}
		for (String key : DISTRIBUTOR_REMOVE_KEYS) {
			distributorInfo.remove(key);
		}
		for (String key : DISTRIBUTOR_STRING_ID_KEYS) {
			if (distributorInfo.containsKey(key)) {
				distributorInfo.put(key, stringifyId(distributorInfo.get(key)));
			}
		}
		Object offline = distributorInfo.get("offline_aftersales_distributor_id");
		if (offline instanceof String s && s.isEmpty()) {
			distributorInfo.put("offline_aftersales_distributor_id", null);
		}
		Object reviewStatus = distributorInfo.get("review_status");
		if (reviewStatus instanceof Number n) {
			distributorInfo.put("review_status", n.intValue() != 0);
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
}
