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

package cn.shopex.ecshopx.goods.service.items;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 组装列表接口 filter 回显：蛇形字段名、解析后的查询条件，而非仓库内部 KEY。
 */
public final class GoodsItemsListResponseFilterBuilder {

	private static final Set<String> QUERY_SNAP_SKIP = Set.of(
			"operate_source",
			"tag_id_list",
			"main_cat_id_list",
			"category_list",
			"keywords",
			"item_bn",
			"barcode",
			"price_gt",
			"price_lt",
			"store_gt",
			"store_lt",
			"store_status",
			"is_sku");

	private static final Set<String> INTERNAL_SKIP = Set.of(
			"item_source_supplier_mode",
			"item_source_non_supplier_mode",
			"distributor_id_gt0_only",
			"is_default_true",
			"item_id_or_default_ids",
			"item_category_in",
			"distributor_id_eq",
			"distributor_id_in",
			"supplier_id_eq",
			"supplier_id_in",
			"price_gt_cents",
			"price_lt_cents",
			"store_lte_warning",
			"store_gt",
			"store_lt",
			"created_time_start",
			"created_time_end");

	private GoodsItemsListResponseFilterBuilder() {
	}

	public static LinkedHashMap<String, Object> build(LinkedHashMap<String, Object> querySnap, LinkedHashMap<String, Object> internal,
			boolean skuMode) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();

		if (internal.get("company_id") != null) {
			out.put("company_id", internal.get("company_id"));
		}
		out.put("item_type", internal.getOrDefault("item_type", "services"));

		if (internal.containsKey("type")) {
			out.put("type", internal.get("type"));
		}

		if (Boolean.TRUE.equals(internal.get("item_source_supplier_mode"))) {
			out.put("supplier_id|gte", 1);
			out.put("audit_status", "approved");
		} else if (Boolean.TRUE.equals(internal.get("item_source_non_supplier_mode"))) {
			out.put("supplier_id", 0);
		}

		if (internal.containsKey("supplier_id_eq") && !out.containsKey("supplier_id|gte")) {
			out.put("supplier_id", internal.get("supplier_id_eq"));
		}
		if (internal.containsKey("supplier_id_in")) {
			out.put("supplier_id", internal.get("supplier_id_in"));
		}

		if (internal.containsKey("distributor_id_eq")) {
			out.put("distributor_id", internal.get("distributor_id_eq"));
		} else if (internal.containsKey("distributor_id_in")) {
			out.put("distributor_id", internal.get("distributor_id_in"));
		}

		if (Boolean.TRUE.equals(internal.get("distributor_id_gt0_only"))) {
			out.put("distributor_id|gt", 0);
			out.remove("distributor_id");
		}

		if (!skuMode) {
			out.put("is_default", Boolean.TRUE);
		}

		if (internal.containsKey("item_id_or_default_ids")) {
			out.put("item_id", internal.get("item_id_or_default_ids"));
		}
		if (internal.containsKey("item_category_in")) {
			out.put("item_category", internal.get("item_category_in"));
		}

		if (internal.containsKey("price_gt_cents")) {
			out.put("price|gt", internal.get("price_gt_cents"));
		}
		if (internal.containsKey("price_lt_cents")) {
			out.put("price|lt", internal.get("price_lt_cents"));
		}
		if (internal.containsKey("store_gt")) {
			out.put("store|gt", internal.get("store_gt"));
		}
		if (internal.containsKey("store_lt")) {
			out.put("store|lt", internal.get("store_lt"));
		}
		if (internal.containsKey("store_lte_warning")) {
			out.put("store|lte", internal.get("store_lte_warning"));
		}
		if (internal.containsKey("created_time_start")) {
			out.put("created|gte", internal.get("created_time_start"));
		}
		if (internal.containsKey("created_time_end")) {
			out.put("created|lte", internal.get("created_time_end"));
		}

		for (Map.Entry<String, Object> e : internal.entrySet()) {
			String k = e.getKey();
			if (INTERNAL_SKIP.contains(k) || out.containsKey(k)) {
				continue;
			}
			out.put(k, e.getValue());
		}

		if (querySnap != null) {
			for (Map.Entry<String, Object> e : querySnap.entrySet()) {
				String k = e.getKey();
				if (QUERY_SNAP_SKIP.contains(k)) {
					continue;
				}
				if (!out.containsKey(k)) {
					out.put(k, e.getValue());
				}
			}
		}

		// operate_source 不应出现在 filter 回显中；若 querySnap 合并带入则移除
		out.remove("operate_source");

		return out;
	}

	public static LinkedHashMap<String, Object> buildForPath(boolean supplierOperateSource, LinkedHashMap<String, Object> querySnap,
			LinkedHashMap<String, Object> internal, boolean skuMode) {
		if (supplierOperateSource) {
			return buildSupplier(internal, skuMode);
		}
		return build(querySnap, internal, skuMode);
	}

	/** 供应商 {@code operate_source=supplier} 列表 filter：仅回显 PHP {@code $params} 中实际参与查询的键。 */
	public static LinkedHashMap<String, Object> buildSupplier(LinkedHashMap<String, Object> internal, boolean skuMode) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		Object companyId = internal.get("company_id");
		if (companyId != null) {
			out.put("company_id", companyId.toString());
		}
		out.put("item_type", internal.getOrDefault("item_type", "services"));
		if (internal.containsKey("type")) {
			Object type = internal.get("type");
			out.put("type", type != null ? type.toString() : type);
		}
		if (internal.containsKey("supplier_id_eq")) {
			out.put("supplier_id", internal.get("supplier_id_eq"));
		} else if (internal.containsKey("supplier_id_in")) {
			out.put("supplier_id", internal.get("supplier_id_in"));
		}
		if (internal.containsKey("regions_id")) {
			out.put("regions_id", internal.get("regions_id"));
		}
		if (!skuMode) {
			out.put("is_default", Boolean.TRUE);
		}
		return out;
	}
}
