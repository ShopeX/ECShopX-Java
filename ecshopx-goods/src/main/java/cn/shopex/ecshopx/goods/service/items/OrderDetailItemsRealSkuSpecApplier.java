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

import cn.shopex.ecshopx.goods.domain.ItemRelAttributes;
import cn.shopex.ecshopx.goods.repository.ItemRelAttributesRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class OrderDetailItemsRealSkuSpecApplier {

	private final ItemRelAttributesRepository itemRelAttributesRepository;
	private final ItemsRelAttrValuesQueryService itemsRelAttrValuesQueryService;

	public OrderDetailItemsRealSkuSpecApplier(
			ItemRelAttributesRepository itemRelAttributesRepository,
			ItemsRelAttrValuesQueryService itemsRelAttrValuesQueryService) {
		this.itemRelAttributesRepository = itemRelAttributesRepository;
		this.itemsRelAttrValuesQueryService = itemsRelAttrValuesQueryService;
	}

	public void apply(long companyId, List<Map<String, Object>> orderItems) {
		if (orderItems == null || orderItems.isEmpty()) {
			return;
		}
		List<Long> itemIds =
				orderItems.stream().map(r -> toLong(r.get("item_id"))).filter(id -> id > 0).distinct().toList();
		if (itemIds.isEmpty()) {
			return;
		}
		List<ItemRelAttributes> rels =
				itemRelAttributesRepository.listByCompanyItemIdsAndAttributeType(companyId, itemIds, "item_spec");
		if (rels.isEmpty()) {
			return;
		}
		Map<Long, List<ItemRelAttributes>> byItem =
				rels.stream().collect(Collectors.groupingBy(ItemRelAttributes::getItemId, LinkedHashMap::new, Collectors.toList()));
		for (Map<String, Object> row : orderItems) {
			long itemId = toLong(row.get("item_id"));
			List<ItemRelAttributes> relRows = byItem.get(itemId);
			if (relRows == null || relRows.isEmpty()) {
				continue;
			}
			ItemsRelAttrValuesQueryService.ItemDetailAttrData ad =
					itemsRelAttrValuesQueryService.assemble(companyId, relRows);
			Map<Long, Map<String, Object>> nested = ad.itemSpecNested.get(itemId);
			if (nested == null || nested.isEmpty()) {
				continue;
			}
			List<Map<String, Object>> specList = new ArrayList<>(nested.values());
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

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o == null) {
			return 0L;
		}
		String t = o.toString().trim();
		if (t.isEmpty()) {
			return 0L;
		}
		return Long.parseLong(t);
	}
}
