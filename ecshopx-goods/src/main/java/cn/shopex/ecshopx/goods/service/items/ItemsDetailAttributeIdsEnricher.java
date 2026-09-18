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

import cn.shopex.ecshopx.goods.service.ItemsAttributesQueryService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ItemsDetailAttributeIdsEnricher {

	private final ItemsAttributesQueryService itemsAttributesQueryService;

	public ItemsDetailAttributeIdsEnricher(ItemsAttributesQueryService itemsAttributesQueryService) {
		this.itemsAttributesQueryService = itemsAttributesQueryService;
	}

	@SuppressWarnings("unchecked")
	public void enrich(long companyId, long jwtDistributorId, Map<String, Object> detail, List<Long> attributeIds, String distributorIdQuery,
			String countryCode) {
		if (detail == null || attributeIds == null || attributeIds.isEmpty()) {
			return;
		}
		Map<String, Object> attrResp = itemsAttributesQueryService.getAttrList(companyId, jwtDistributorId, null, null, attributeIds,
				distributorIdQuery, 1, 1000, countryCode);
		Object listObj = attrResp.get("list");
		if (!(listObj instanceof List<?> list)) {
			return;
		}
		List<Map<String, Object>> itemParamsList = new ArrayList<>();
		List<Map<String, Object>> itemSpecList = new ArrayList<>();
		for (Object o : list) {
			if (!(o instanceof Map<?, ?> row)) {
				continue;
			}
			String type = row.get("attribute_type") != null ? row.get("attribute_type").toString() : "";
			if ("item_params".equals(type)) {
				itemParamsList.add((Map<String, Object>) row);
			} else if ("item_spec".equals(type)) {
				Map<String, Object> copy = new LinkedHashMap<>((Map<String, Object>) row);
				fillCustomAttributeValues(copy, detail);
				itemSpecList.add(copy);
			}
		}
		detail.put("item_params_list", itemParamsList);
		if (!itemSpecList.isEmpty()) {
			detail.put("item_spec_list", itemSpecList);
		}
		detail.remove("attribute_ids");
		detail.remove("attr_values_custom");
	}

	@SuppressWarnings("unchecked")
	private void fillCustomAttributeValues(Map<String, Object> attrRow, Map<String, Object> detail) {
		Object avObj = attrRow.get("attribute_values");
		if (!(avObj instanceof List<?> avList)) {
			return;
		}
		Object customMap = detail.get("attr_values_custom");
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
			Object c = cm.get(vid instanceof Number ? ((Number) vid).longValue() : Long.parseLong(vid.toString()));
			if (StringUtils.hasText(c != null ? c.toString() : null)) {
				valRow.put("custom_attribute_value", c.toString());
			}
		}
	}
}
