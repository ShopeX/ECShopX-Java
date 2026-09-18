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

package cn.shopex.ecshopx.goods.service.pointsmall;

import cn.shopex.ecshopx.goods.domain.ItemsAttributeValues;
import cn.shopex.ecshopx.goods.domain.ItemsAttributes;
import cn.shopex.ecshopx.goods.repository.ItemsAttributeValuesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsAttributesRepository;
import cn.shopex.ecshopx.goods.service.ItemsAttributesMultiLangApplier;
import cn.shopex.ecshopx.goods.service.ItemsAttributesRowMaps;
import cn.shopex.ecshopx.pointsmall.service.PointsmallItemsAdminDetailAttributeEnricher;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PointsmallItemsAdminDetailActionEnricher implements PointsmallItemsAdminDetailAttributeEnricher {

	private static final int ATTR_PAGE_LIMIT = 100;
	private static final String DEFAULT_LANG = "zh-CN";

	private final ItemsAttributesRepository itemsAttributesRepository;
	private final ItemsAttributeValuesRepository itemsAttributeValuesRepository;
	private final ItemsAttributesMultiLangApplier itemsAttributesMultiLangApplier;

	public PointsmallItemsAdminDetailActionEnricher(ItemsAttributesRepository itemsAttributesRepository,
			ItemsAttributeValuesRepository itemsAttributeValuesRepository,
			ItemsAttributesMultiLangApplier itemsAttributesMultiLangApplier) {
		this.itemsAttributesRepository = itemsAttributesRepository;
		this.itemsAttributeValuesRepository = itemsAttributeValuesRepository;
		this.itemsAttributesMultiLangApplier = itemsAttributesMultiLangApplier;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void enrichAttributeLists(long companyId, Map<String, Object> result) {
		if (result == null) {
			return;
		}
		result.put("item_params_list", new ArrayList<>());
		Object rawIds = result.get("attribute_ids");
		if (!(rawIds instanceof List<?> idList) || idList.isEmpty()) {
			result.remove("item_spec_list");
			result.put("attribute_ids", new ArrayList<>());
			result.put("attr_values_custom", new ArrayList<>());
			return;
		}
		List<Long> attributeIds = new ArrayList<>();
		for (Object o : idList) {
			if (o instanceof Number n) {
				attributeIds.add(n.longValue());
			} else if (o != null && StringUtils.hasText(o.toString())) {
				try {
					attributeIds.add(Long.parseLong(o.toString().trim()));
				} catch (NumberFormatException ignored) {
				}
			}
		}
		if (attributeIds.isEmpty()) {
			result.remove("item_spec_list");
			result.remove("attribute_ids");
			result.remove("attr_values_custom");
			return;
		}

		List<ItemsAttributes> entities = itemsAttributesRepository.listByCompanyAndAttributeIdsIn(companyId, attributeIds);
		if (entities.size() > ATTR_PAGE_LIMIT) {
			entities = entities.subList(0, ATTR_PAGE_LIMIT);
		}

		List<Map<String, Object>> rowMaps = new ArrayList<>();
		for (ItemsAttributes rec : entities) {
			rowMaps.add(ItemsAttributesRowMaps.toAttributeRowMap(rec));
		}
		itemsAttributesMultiLangApplier.applyListLangForAttributes(companyId, DEFAULT_LANG, rowMaps);

		for (Map<String, Object> row : rowMaps) {
			Object aidObj = row.get("attribute_id");
			if (aidObj == null) {
				continue;
			}
			long attributeId;
			if (aidObj instanceof Number n) {
				attributeId = n.longValue();
			} else {
				String s = aidObj.toString().trim();
				if (!StringUtils.hasText(s)) {
					continue;
				}
				try {
					attributeId = Long.parseLong(s);
				} catch (NumberFormatException ignored) {
					continue;
				}
			}
			List<ItemsAttributeValues> vals = itemsAttributeValuesRepository.listByAttributeId(companyId, attributeId);
			List<Map<String, Object>> valMaps = new ArrayList<>();
			for (ItemsAttributeValues v : vals) {
				valMaps.add(ItemsAttributesRowMaps.toAttributeValueRowMap(v));
			}
			itemsAttributesMultiLangApplier.applyListLangForAttributeValues(companyId, DEFAULT_LANG, valMaps);
			row.put("attribute_values", ItemsAttributesRowMaps.buildAttributeValuesNested(valMaps));
		}

		List<Map<String, Object>> itemParamsList = new ArrayList<>();
		List<Map<String, Object>> itemSpecList = new ArrayList<>();
		for (Map<String, Object> row : rowMaps) {
			String type = row.get("attribute_type") != null ? row.get("attribute_type").toString() : "";
			if ("item_params".equals(type)) {
				itemParamsList.add(row);
			} else {
				Map<String, Object> copy = new LinkedHashMap<>(row);
				fillCustomOnNestedList(copy, result);
				itemSpecList.add(copy);
			}
		}
		result.put("item_params_list", itemParamsList);
		if (!itemSpecList.isEmpty()) {
			result.put("item_spec_list", itemSpecList);
		} else {
			result.remove("item_spec_list");
		}
		result.remove("attribute_ids");
		result.remove("attr_values_custom");
	}

	@SuppressWarnings("unchecked")
	private static void fillCustomOnNestedList(Map<String, Object> attrRow, Map<String, Object> result) {
		Object avObj = attrRow.get("attribute_values");
		if (!(avObj instanceof Map<?, ?> avMap)) {
			return;
		}
		Object inner = avMap.get("list");
		if (!(inner instanceof List<?> avList)) {
			return;
		}
		Object customMap = result.get("attr_values_custom");
		if (!(customMap instanceof Map<?, ?> cm)) {
			return;
		}
		for (Object x : avList) {
			if (!(x instanceof Map<?, ?> vm)) {
				continue;
			}
			Map<String, Object> valRow = (Map<String, Object>) vm;
			Object vid = valRow.get("attribute_value_id");
			if (vid == null) {
				continue;
			}
			long key = vid instanceof Number n ? n.longValue() : Long.parseLong(vid.toString().trim());
			Object c = cm.get(key);
			if (c == null) {
				c = cm.get((int) key);
			}
			valRow.put("custom_attribute_value", c != null ? c.toString() : null);
		}
	}
}
