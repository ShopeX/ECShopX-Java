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

import cn.shopex.ecshopx.orders.domain.Cart;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

/** Builds column-name maps for cart rows in API responses. */
public final class OrdersCartColumnNamesMapSupport {

	private OrdersCartColumnNamesMapSupport() {}

	public static Map<String, Object> toColumnNamesMap(Cart c) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("cart_id", c.getCartId());
		m.put("company_id", c.getCompanyId());
		m.put("user_id", c.getUserId());
		m.put("promoter_user_id", c.getPromoterUserId());
		m.put("source_type", c.getSourceType());
		m.put("user_ident", c.getUserIdent());
		m.put("shop_type", c.getShopType());
		m.put("shop_id", c.getShopId());
		m.put("activity_type", c.getActivityType());
		m.put("activity_id", c.getActivityId());
		m.put("marketing_type", c.getMarketingType());
		m.put("marketing_id", c.getMarketingId());
		m.put("item_type", c.getItemType());
		m.put("item_id", c.getItemId());
		m.put("items_id", splitItemsId(c.getItemsId()));
		m.put("item_name", c.getItemName());
		m.put("pics", c.getPics());
		m.put("price", c.getPrice());
		m.put("num", c.getNum());
		m.put("point", c.getPoint());
		m.put("wxa_appid", c.getWxaAppid());
		m.put("is_checked", c.getIsChecked());
		m.put("is_plus_buy", c.getIsPlusBuy());
		m.put("created", c.getCreated());
		m.put("updated", c.getUpdated());
		return m;
	}

	/** Like {@link #toColumnNamesMap} but omits {@code point} for wxapp column-name responses. */
	public static Map<String, Object> toColumnNamesMapWxappNoPoint(Cart c) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("cart_id", c.getCartId());
		m.put("company_id", c.getCompanyId());
		m.put("user_id", c.getUserId());
		m.put("promoter_user_id", c.getPromoterUserId());
		m.put("source_type", c.getSourceType());
		m.put("user_ident", c.getUserIdent());
		m.put("shop_type", c.getShopType());
		m.put("shop_id", c.getShopId());
		m.put("activity_type", c.getActivityType());
		m.put("activity_id", c.getActivityId());
		m.put("marketing_type", c.getMarketingType());
		m.put("marketing_id", c.getMarketingId());
		m.put("item_type", c.getItemType());
		m.put("item_id", c.getItemId());
		m.put("items_id", splitItemsId(c.getItemsId()));
		m.put("item_name", c.getItemName());
		m.put("pics", c.getPics());
		m.put("price", c.getPrice());
		m.put("num", c.getNum());
		m.put("wxa_appid", c.getWxaAppid());
		m.put("is_checked", c.getIsChecked());
		m.put("is_plus_buy", c.getIsPlusBuy());
		m.put("created", c.getCreated());
		m.put("updated", c.getUpdated());
		return m;
	}

	public static String joinItemsId(Object raw) {
		List<String> ids = normalizeItemsIdList(raw);
		return String.join(",", ids);
	}

	public static List<String> normalizeItemsIdList(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof List<?> list) {
			List<String> out = new ArrayList<>();
			for (Object o : list) {
				if (o == null) {
					continue;
				}
				String t = o.toString().trim();
				if (!t.isEmpty()) {
					out.add(t);
				}
			}
			return out;
		}
		if (raw instanceof String s) {
			return splitItemsId(s);
		}
		return splitItemsId(raw.toString());
	}

	public static List<String> splitItemsId(String raw) {
		if (!StringUtils.hasText(raw)) {
			return List.of();
		}
		String[] parts = raw.trim().split(",");
		List<String> out = new ArrayList<>();
		for (String p : parts) {
			if (p == null) {
				continue;
			}
			String id = p.trim();
			if (!id.isEmpty()) {
				out.add(id);
			}
		}
		return out;
	}
}
