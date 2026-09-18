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

package cn.shopex.ecshopx.goods.service.recommend;

import cn.shopex.ecshopx.goods.domain.recommend.GoodsRecommendDisplaySetting;
import java.util.LinkedHashMap;
import java.util.Map;

/** 四页展示设置缺省值（SSOT §2.2） */
public final class GoodsRecommendDisplayDefaults {

	public static final int DEFAULT_LIMIT = 6;

	public static final int MAX_DISPLAY_LIMIT = 50;

	public static final String DEFAULT_SORT = GoodsRecommendDisplaySort.SALES_DESC;

	public static int clampLimit(int limit) {
		if (limit < 1) {
			return DEFAULT_LIMIT;
		}
		return Math.min(MAX_DISPLAY_LIMIT, limit);
	}

	private GoodsRecommendDisplayDefaults() {}

	public static Map<String, Object> defaultResponseMap(long companyId) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("company_id", companyId);
		out.put("detail_enabled", 0);
		out.put("cart_enabled", 0);
		out.put("checkout_enabled", 0);
		out.put("order_detail_enabled", 0);
		out.put("detail_limit", DEFAULT_LIMIT);
		out.put("cart_limit", DEFAULT_LIMIT);
		out.put("checkout_limit", DEFAULT_LIMIT);
		out.put("order_detail_limit", DEFAULT_LIMIT);
		out.put("detail_sort", DEFAULT_SORT);
		out.put("cart_sort", DEFAULT_SORT);
		out.put("checkout_sort", DEFAULT_SORT);
		out.put("order_detail_sort", DEFAULT_SORT);
		out.put("persisted", false);
		return out;
	}

	public static GoodsRecommendDisplaySetting newEntityDefaults(long companyId, int now) {
		GoodsRecommendDisplaySetting row = new GoodsRecommendDisplaySetting();
		row.setCompanyId(companyId);
		row.setDetailEnabled(0);
		row.setCartEnabled(0);
		row.setCheckoutEnabled(0);
		row.setOrderDetailEnabled(0);
		row.setDetailLimit(DEFAULT_LIMIT);
		row.setCartLimit(DEFAULT_LIMIT);
		row.setCheckoutLimit(DEFAULT_LIMIT);
		row.setOrderDetailLimit(DEFAULT_LIMIT);
		row.setDetailSort(DEFAULT_SORT);
		row.setCartSort(DEFAULT_SORT);
		row.setCheckoutSort(DEFAULT_SORT);
		row.setOrderDetailSort(DEFAULT_SORT);
		row.setCreated(now);
		row.setUpdated(now);
		return row;
	}

	public static Map<String, Object> toResponseMap(GoodsRecommendDisplaySetting row, boolean persisted) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("company_id", row.getCompanyId());
		out.put("detail_enabled", row.getDetailEnabled());
		out.put("cart_enabled", row.getCartEnabled());
		out.put("checkout_enabled", row.getCheckoutEnabled());
		out.put("order_detail_enabled", row.getOrderDetailEnabled());
		out.put("detail_limit", row.getDetailLimit());
		out.put("cart_limit", row.getCartLimit());
		out.put("checkout_limit", row.getCheckoutLimit());
		out.put("order_detail_limit", row.getOrderDetailLimit());
		out.put("detail_sort", row.getDetailSort());
		out.put("cart_sort", row.getCartSort());
		out.put("checkout_sort", row.getCheckoutSort());
		out.put("order_detail_sort", row.getOrderDetailSort());
		out.put("updated", row.getUpdated());
		out.put("persisted", persisted);
		return out;
	}
}
