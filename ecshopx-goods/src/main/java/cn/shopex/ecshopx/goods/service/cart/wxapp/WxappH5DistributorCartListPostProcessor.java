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

package cn.shopex.ecshopx.goods.service.cart.wxapp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class WxappH5DistributorCartListPostProcessor {

	public void apply(Map<String, Object> result) {
		if (result == null) {
			return;
		}
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> validCart = (List<Map<String, Object>>) result.get("valid_cart");
		if (validCart == null) {
			return;
		}
		for (Map<String, Object> shopCart : validCart) {
			if (shopCart == null) {
				continue;
			}
			applyCrossBorderTaxBeforeSplit(shopCart);
			ensureShopPhpShapeAndTotals(shopCart);
			applyPlusBuyAdjustmentsFen(shopCart);
			applyActivityGroupingAdjustmentsFen(shopCart);
		}
	}

	private static void ensureShopPhpShapeAndTotals(Map<String, Object> shopCart) {
		if (!shopCart.containsKey("is_ziti")) {
			shopCart.put("is_ziti", Boolean.FALSE);
		}
		if (!shopCart.containsKey("is_delivery")) {
			shopCart.put("is_delivery", Boolean.TRUE);
		}
		if (!shopCart.containsKey("used_activity")) {
			shopCart.put("used_activity", List.of());
		}
		if (!shopCart.containsKey("used_activity_ids")) {
			shopCart.put("used_activity_ids", List.of());
		}
		if (!shopCart.containsKey("activity_grouping")) {
			shopCart.put("activity_grouping", List.of());
		}
		if (!shopCart.containsKey("gift_activity")) {
			shopCart.put("gift_activity", List.of());
		}
		if (!shopCart.containsKey("plus_buy_activity")) {
			shopCart.put("plus_buy_activity", List.of());
		}
		Object vg = shopCart.get("vipgrade_guide_title");
		if (!(vg instanceof Map<?, ?>)) {
			shopCart.put("vipgrade_guide_title", new LinkedHashMap<>(Map.of("guide_title_desc", "")));
		} else {
			@SuppressWarnings("unchecked")
			Map<String, Object> m = (Map<String, Object>) vg;
			if (!m.containsKey("guide_title_desc")) {
				m.put("guide_title_desc", "");
			}
		}

		long itemFeeFen = 0L;
		long discountFen = 0L;
		long memberDiscountFen = 0L;
		int cartTotalNum = 0;
		int cartTotalCount = 0;
		long sumLinePayFen = 0L;
		boolean anyLineTotal = false;
		Object listObj = shopCart.get("list");
		if (listObj instanceof List<?> lines) {
			for (Object o : lines) {
				if (!(o instanceof Map<?, ?> raw)) {
					continue;
				}
				@SuppressWarnings("unchecked")
				Map<String, Object> m = (Map<String, Object>) raw;
				if (!truthyChecked(m.get("is_checked"))) {
					continue;
				}
				int price = intAmount(m.get("price"));
				int num = intAmount(m.get("num"));
				itemFeeFen += (long) price * num;
				cartTotalNum += num;
				cartTotalCount += 1;
				discountFen += longFen(m.get("discount_fee"));
				memberDiscountFen += longFen(m.get("member_discount"));
				Object tf = m.get("total_fee");
				if (tf != null) {
					sumLinePayFen += longFen(tf);
					anyLineTotal = true;
				}
			}
		}
		long totalPayFen = anyLineTotal ? sumLinePayFen : itemFeeFen - discountFen;
		if (totalPayFen < 0L) {
			totalPayFen = 0L;
		}
		shopCart.put("cart_total_price", itemFeeFen);
		shopCart.put("item_fee", itemFeeFen);
		shopCart.put("discount_fee", discountFen);
		shopCart.put("member_discount", memberDiscountFen);
		shopCart.put("cart_total_num", cartTotalNum);
		shopCart.put("cart_total_count", cartTotalCount);
		shopCart.put("total_fee", Long.toString(totalPayFen));
	}

	private static void applyCrossBorderTaxBeforeSplit(Map<String, Object> shopCart) {
		Object listObj = shopCart.get("list");
		if (!(listObj instanceof List<?> lines)) {
			return;
		}
		for (Object o : lines) {
			if (!(o instanceof Map<?, ?> rawLine)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> line = (Map<String, Object>) rawLine;
			Object type = line.get("type");
			int t = type instanceof Number n ? n.intValue() : parseIntLoose(type, 0);
			if (t != 1) {
				continue;
			}
			long taxFen = longFen(line.get("cross_border_taxation"));
			long baseFen = longFen(line.get("total_fee"));
			if (baseFen == 0L) {
				baseFen = (long) intAmount(line.get("price")) * intAmount(line.get("num"));
			}
			long newTotalFen = baseFen + taxFen;
			line.put("total_fee", Long.toString(newTotalFen));
		}
	}

	private static int parseIntLoose(Object raw, int dflt) {
		if (raw == null) {
			return dflt;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(raw.toString().trim());
		} catch (NumberFormatException e) {
			return dflt;
		}
	}

	private static long longFen(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static void applyPlusBuyAdjustmentsFen(Map<String, Object> shopCart) {
		Object raw = shopCart.get("plus_buy_activity");
		if (!(raw instanceof List<?> acts) || acts.isEmpty()) {
			return;
		}
		long deltaTotalFen = 0L;
		long deltaItemFen = 0L;
		long deltaDiscountFen = 0L;
		for (Object actObj : acts) {
			if (!(actObj instanceof Map<?, ?> act)) {
				continue;
			}
			for (Map<String, Object> pm : plusItemsFromActivity(act)) {
				long plusFen = longFen(pm.get("plus_price"));
				long priceFen = longFen(pm.get("price"));
				deltaTotalFen += plusFen;
				deltaItemFen += priceFen;
				deltaDiscountFen += priceFen - plusFen;
			}
		}
		if (deltaTotalFen == 0L) {
			return;
		}
		long totalPay = longFen(shopCart.get("total_fee")) + deltaTotalFen;
		long itemFee = longFen(shopCart.get("item_fee")) + deltaItemFen;
		long disc = longFen(shopCart.get("discount_fee")) + deltaDiscountFen;
		shopCart.put("total_fee", Long.toString(totalPay));
		shopCart.put("item_fee", Long.toString(itemFee));
		shopCart.put("discount_fee", Long.toString(disc));
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> plusItemsFromActivity(Map<?, ?> act) {
		Object items = act.get("plus_item");
		if (items instanceof Map<?, ?> single) {
			return List.of((Map<String, Object>) single);
		}
		if (!(items instanceof List<?> plist)) {
			return List.of();
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object p : plist) {
			if (p instanceof Map<?, ?> pm) {
				out.add((Map<String, Object>) pm);
			}
		}
		return out;
	}

	private static void applyActivityGroupingAdjustmentsFen(Map<String, Object> shopCart) {
		Object raw = shopCart.get("activity_grouping");
		if (!(raw instanceof List<?> groups) || groups.isEmpty()) {
			return;
		}
		long addFen = 0L;
		for (Object gObj : groups) {
			if (!(gObj instanceof Map<?, ?> grp)) {
				continue;
			}
			String at = grp.get("activity_type") == null ? "" : grp.get("activity_type").toString();
			if (!"full_minus".equals(at) && !"full_discount".equals(at)) {
				continue;
			}
			long df = longFen(grp.get("discount_fee"));
			if (df == 0L) {
				Object info = grp.get("activityInfo");
				if (info instanceof Map<?, ?> im) {
					df = longFen(im.get("discount_fee"));
				}
			}
			addFen += df;
		}
		if (addFen == 0L) {
			return;
		}
		long totalPay = longFen(shopCart.get("total_fee")) + addFen;
		shopCart.put("total_fee", Long.toString(totalPay));
	}

	private static int intAmount(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static boolean truthyChecked(Object v) {
		if (v == null) {
			return false;
		}
		if (Boolean.FALSE.equals(v)) {
			return false;
		}
		if (v instanceof Number n) {
			return n.longValue() != 0L;
		}
		String s = v.toString().trim();
		return !s.isEmpty() && !"0".equals(s) && !"false".equalsIgnoreCase(s);
	}
}
