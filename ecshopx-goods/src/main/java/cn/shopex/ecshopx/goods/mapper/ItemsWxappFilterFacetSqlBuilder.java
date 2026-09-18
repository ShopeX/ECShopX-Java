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

package cn.shopex.ecshopx.goods.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 将筛选 Map 转为 facet 查询动态条件；仅允许 {@code items} 表真实列名，禁止拼接未校验列名。
 */
public final class ItemsWxappFilterFacetSqlBuilder {

	private static final Set<String> ITEMS_COLUMNS = Set.of(
			"item_id", "item_type", "item_category", "consume_type", "item_name", "item_bn", "barcode", "brief",
			"company_id", "price", "cost_price", "item_unit", "special_type", "item_address_province", "item_address_city",
			"regions_id", "regions", "store", "sales", "rebate_conf", "rebate", "rebate_type", "approve_status",
			"audit_status", "audit_reason", "market_price", "goods_function", "goods_series", "goods_color", "goods_brand",
			"is_default", "default_item_id", "goods_id", "nospec", "weight", "sort", "is_epidemic", "templates_id", "pics",
			"pics_create_qrcode", "video_type", "videos", "video_pic_url", "intro", "purchase_agreement", "is_show_specimg",
			"enable_agreement", "date_type", "begin_date", "end_date", "fixed_term", "brand_logo", "is_point", "point",
			"distributor_id", "volume", "item_source", "brand_id", "tax_rate", "crossborder_tax_rate", "profit_type",
			"origincountry_id", "taxstrategy_id", "taxation_num", "profit_fee", "type", "is_profit", "is_medicine",
			"is_prescription", "created", "updated", "is_gift", "is_package", "tdk_content", "supplier_id",
			"supplier_item_id", "is_market", "goods_bn", "supplier_goods_bn", "audit_date", "start_num", "delivery_time",
			"is_taobao");

	private ItemsWxappFilterFacetSqlBuilder() {}

	public static ItemsWxappFacetSqlParams build(Map<String, Object> filter) {
		LinkedHashMap<String, Object> f = new LinkedHashMap<>(filter);
		ItemsWxappFacetSqlParams out = new ItemsWxappFacetSqlParams();
		Object orRaw = f.remove("or");
		if (orRaw instanceof Map<?, ?> orMap && !orMap.isEmpty()) {
			for (Map.Entry<?, ?> en : orMap.entrySet()) {
				String key = String.valueOf(en.getKey());
				ItemsWxappFacetCondition c = parseFieldCondition(key, en.getValue());
				if (c != null) {
					out.getOrConds().add(c);
				}
			}
		}
		for (Map.Entry<String, Object> en : f.entrySet()) {
			String field = en.getKey();
			if ("or".equals(field)) {
				continue;
			}
			ItemsWxappFacetCondition c = parseFieldCondition(field, en.getValue());
			if (c != null) {
				out.getAnds().add(c);
			}
		}
		return out;
	}

	private static ItemsWxappFacetCondition parseFieldCondition(String fieldKey, Object rawValue) {
		String[] parts = fieldKey.split("\\|", 2);
		String col = parts[0];
		if (!ITEMS_COLUMNS.contains(col)) {
			return null;
		}
		if (parts.length == 1) {
			return buildScalarOrIn(col, rawValue);
		}
		String op = parts[1];
		if ("contains".equals(op)) {
			op = "like";
		}
		if ("direct".equals(op)) {
			return buildScalarOrIn(col, rawValue);
		}
		if ("like".equals(op)) {
			if (rawValue == null) {
				return null;
			}
			return ItemsWxappFacetCondition.likeContains(col, String.valueOf(rawValue));
		}
		Object norm = normalizeScalar(rawValue);
		return switch (op) {
			case "gt" -> ItemsWxappFacetCondition.gt(col, norm);
			case "lt" -> ItemsWxappFacetCondition.lt(col, norm);
			case "gte" -> ItemsWxappFacetCondition.gte(col, norm);
			case "lte" -> ItemsWxappFacetCondition.lte(col, norm);
			case "neq" -> ItemsWxappFacetCondition.neq(col, norm);
			default -> null;
		};
	}

	private static ItemsWxappFacetCondition buildScalarOrIn(String col, Object rawValue) {
		if (rawValue instanceof Collection<?> coll) {
			if (coll.isEmpty()) {
				return null;
			}
			List<Object> vals = new ArrayList<>();
			for (Object o : coll) {
				vals.add(normalizeScalar(o));
			}
			return ItemsWxappFacetCondition.inList(col, vals);
		}
		return ItemsWxappFacetCondition.eq(col, normalizeScalar(rawValue));
	}

	private static Object normalizeScalar(Object v) {
		if (v instanceof Boolean b) {
			return b ? 1 : 0;
		}
		return v;
	}
}
