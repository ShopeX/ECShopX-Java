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

package cn.shopex.ecshopx.goods.service;

import cn.shopex.ecshopx.goods.domain.ItemsAttributeValues;
import cn.shopex.ecshopx.goods.domain.ItemsAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 属性 / 属性值行 Map 构建，与分类详情接口字段一致，避免列表与详情漂移。 */
public final class ItemsAttributesRowMaps {

	private ItemsAttributesRowMaps() {
	}

	public static Map<String, Object> toAttributeRowMap(ItemsAttributes a) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("attribute_id", a.getAttributeId());
		m.put("company_id", a.getCompanyId());
		m.put("shop_id", a.getShopId() != null ? a.getShopId() : 0L);
		m.put("attribute_type", a.getAttributeType());
		m.put("attribute_name", a.getAttributeName());
		m.put("attribute_memo", a.getAttributeMemo());
		m.put("attribute_sort", a.getAttributeSort() != null ? a.getAttributeSort() : "");
		m.put("distributor_id", a.getDistributorId() != null ? a.getDistributorId() : 0L);
		m.put("is_show", a.getIsShow() != null ? a.getIsShow() : "");
		m.put("is_image", a.getIsImage() != null ? a.getIsImage() : "");
		m.put("image_url", a.getImageUrl());
		m.put("created", a.getCreated() != null ? a.getCreated() : 0);
		m.put("updated", a.getUpdated() != null ? a.getUpdated() : 0);
		m.put("attribute_code", a.getAttributeCode());
		m.put("attribute_show", a.getAttributeShow() != null ? a.getAttributeShow() : "select");
		return m;
	}

	public static Map<String, Object> toAttributeValueRowMap(ItemsAttributeValues v) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("attribute_value_id", v.getAttributeValueId());
		m.put("attribute_id", v.getAttributeId());
		m.put("company_id", v.getCompanyId());
		m.put("shop_id", v.getShopId() != null ? v.getShopId() : 0L);
		m.put("attribute_value", v.getAttributeValue());
		m.put("sort", v.getSort() != null ? v.getSort() : "");
		m.put("image_url", v.getImageUrl());
		m.put("created", v.getCreated() != null ? v.getCreated() : 0);
		m.put("updated", v.getUpdated() != null ? v.getUpdated() : 0);
		m.put("oms_value_id", v.getOmsValueId());
		return m;
	}

	/**
	 * 与 {@link ItemsCategoryInfoService} 中嵌套 {@code attribute_values} 形状一致：{@code total_count} + {@code list}。
	 */
	public static Map<String, Object> buildAttributeValuesNested(List<Map<String, Object>> valueRows) {
		List<Map<String, Object>> list = valueRows != null ? valueRows : List.of();
		Map<String, Object> nested = new LinkedHashMap<>();
		nested.put("total_count", list.size());
		nested.put("list", list);
		return nested;
	}

	public static Map<String, Object> buildAttributeValuesNestedFromEntities(List<ItemsAttributeValues> vals) {
		if (vals == null || vals.isEmpty()) {
			return buildAttributeValuesNested(List.of());
		}
		List<Map<String, Object>> list = new ArrayList<>();
		for (ItemsAttributeValues v : vals) {
			list.add(toAttributeValueRowMap(v));
		}
		return buildAttributeValuesNested(list);
	}
}
