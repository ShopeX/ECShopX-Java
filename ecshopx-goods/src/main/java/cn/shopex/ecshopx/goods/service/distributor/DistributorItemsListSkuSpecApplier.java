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

package cn.shopex.ecshopx.goods.service.distributor;

import cn.shopex.ecshopx.goods.domain.ItemRelAttributes;
import cn.shopex.ecshopx.goods.repository.ItemRelAttributesRepository;
import cn.shopex.ecshopx.goods.service.items.ItemsRelAttrValuesQueryService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorItemsListSkuSpecApplier {

	private final ItemRelAttributesRepository itemRelAttributesRepository;
	private final ItemsRelAttrValuesQueryService itemsRelAttrValuesQueryService;

	public DistributorItemsListSkuSpecApplier(
			ItemRelAttributesRepository itemRelAttributesRepository,
			ItemsRelAttrValuesQueryService itemsRelAttrValuesQueryService) {
		this.itemRelAttributesRepository = itemRelAttributesRepository;
		this.itemsRelAttrValuesQueryService = itemsRelAttrValuesQueryService;
	}

	public void apply(long companyId, List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		List<Long> itemIds = rows.stream().map(r -> toLong(r.get("item_id"))).filter(id -> id > 0).distinct().toList();
		if (itemIds.isEmpty()) {
			return;
		}
		List<ItemRelAttributes> rels =
				itemRelAttributesRepository.listByCompanyItemIdsAndAttributeType(companyId, itemIds, "item_spec");
		Map<Long, List<ItemRelAttributes>> byItem = rels.stream().collect(Collectors.groupingBy(ItemRelAttributes::getItemId));
		for (Map<String, Object> row : rows) {
			long itemId = toLong(row.get("item_id"));
			normalizeNospec(row);
			Object itype = row.get("item_type");
			if (itype == null || !StringUtils.hasText(itype.toString())) {
				row.put("item_type", "services");
			}
			long def = toLong(row.get("default_item_id"));
			if (def <= 0) {
				row.put("default_item_id", itemId);
			}
			List<ItemRelAttributes> relRows = byItem.getOrDefault(itemId, List.of());
			if (relRows.isEmpty()) {
				row.put("item_spec", new ArrayList<>());
				row.put("item_spec_desc", "");
				continue;
			}
			ItemsRelAttrValuesQueryService.ItemDetailAttrData ad = itemsRelAttrValuesQueryService.assemble(companyId, relRows);
			List<Map<String, Object>> specList = new ArrayList<>(ad.itemSpecNested.getOrDefault(itemId, Map.of()).values());
			row.put("item_spec", specList);
			StringBuilder desc = new StringBuilder();
			for (Map<String, Object> s : specList) {
				Object n = s.get("spec_name");
				Object v = s.get("spec_value_name");
				if (desc.length() > 0) {
					desc.append(',');
				}
				desc.append(n != null ? n : "").append(':').append(v != null ? v : "");
			}
			row.put("item_spec_desc", desc.toString());
		}
	}

	private static void normalizeNospec(Map<String, Object> row) {
		Object ns = row.get("nospec");
		boolean b = ns instanceof Boolean z ? z
				: "true".equalsIgnoreCase(String.valueOf(ns)) || "1".equals(String.valueOf(ns));
		row.put("nospec", b);
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return o == null ? 0L : Long.parseLong(o.toString().trim());
	}

}
