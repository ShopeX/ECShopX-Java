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

import cn.shopex.ecshopx.goods.domain.ItemsRelType;
import cn.shopex.ecshopx.goods.repository.ItemsRelTypeRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Fills {@code type_labels} on SKU list rows from {@code items_rel_type}.
 */
@Service
public class ItemsSkuListAssembler {

	private final ItemsRelTypeRepository itemsRelTypeRepository;

	public ItemsSkuListAssembler(ItemsRelTypeRepository itemsRelTypeRepository) {
		this.itemsRelTypeRepository = itemsRelTypeRepository;
	}

	/**
	 * Loads relational type rows for each row's {@code item_id} and sets {@code type_labels}
	 * using camelCase keys for nested label objects.
	 */
	public void applyTypeLabels(List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		List<Long> itemIds = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			Object id = row.get("item_id");
			if (id instanceof Number n && n.longValue() > 0) {
				itemIds.add(n.longValue());
			}
		}
		if (itemIds.isEmpty()) {
			return;
		}
		List<ItemsRelType> rels = itemsRelTypeRepository.listByItemIds(itemIds);
		Map<Long, List<ItemsRelType>> byItem = rels.stream().collect(Collectors.groupingBy(ItemsRelType::getItemId, LinkedHashMap::new, Collectors.toList()));
		for (List<ItemsRelType> list : byItem.values()) {
			list.sort(Comparator.comparing(ItemsRelType::getCreated, Comparator.nullsLast(Comparator.reverseOrder())));
		}
		for (Map<String, Object> row : rows) {
			Object id = row.get("item_id");
			long itemId = id instanceof Number n ? n.longValue() : 0L;
			List<ItemsRelType> forItem = byItem.getOrDefault(itemId, List.of());
			List<Map<String, Object>> labels = new ArrayList<>(forItem.size());
			for (ItemsRelType r : forItem) {
				labels.add(relTypeToLabelMap(r));
			}
			row.put("type_labels", labels);
		}
	}

	private static Map<String, Object> relTypeToLabelMap(ItemsRelType r) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("itemId", r.getItemId() != null ? Long.toString(r.getItemId()) : null);
		m.put("labelId", r.getLabelId() != null ? Long.toString(r.getLabelId()) : null);
		m.put("labelName", r.getLabelName());
		m.put("labelPrice", r.getLabelPrice());
		m.put("numType", r.getNumType());
		m.put("num", r.getNum() != null ? Long.toString(r.getNum()) : null);
		m.put("isNotLimitNum", r.getIsNotLimitNum());
		m.put("limitTime", r.getLimitTime() != null ? Long.toString(r.getLimitTime()) : null);
		m.put("companyId", r.getCompanyId() != null ? Long.toString(r.getCompanyId()) : null);
		m.put("created", r.getCreated());
		m.put("updated", r.getUpdated());
		return m;
	}
}
