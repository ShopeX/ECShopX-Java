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

package cn.shopex.ecshopx.goods.integration;

import cn.shopex.ecshopx.common.goods.port.WxappCategoryTreeForPageParamsPort;
import cn.shopex.ecshopx.goods.service.ItemsCategoryQueryService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxappCategoryTreeForPageParamsPortImpl implements WxappCategoryTreeForPageParamsPort {

	private final ItemsCategoryQueryService itemsCategoryQueryService;

	public WxappCategoryTreeForPageParamsPortImpl(ItemsCategoryQueryService itemsCategoryQueryService) {
		this.itemsCategoryQueryService = itemsCategoryQueryService;
	}

	@Override
	public List<Map<String, Object>> loadTreeForWxappPageParams(
			long companyId,
			long distributorId,
			String countryCode,
			long jwtDistributorId) {
		List<Map<String, Object>> tree = itemsCategoryQueryService.getWxappCategoryList(
				companyId, null, Long.valueOf(distributorId), null, countryCode, jwtDistributorId);
		if (tree == null || tree.isEmpty()) {
			return List.of();
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Map<String, Object> node : tree) {
			out.add(processingParamsNode(node));
		}
		return out;
	}

	private static Map<String, Object> processingParamsNode(Map<String, Object> value) {
		LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e : value.entrySet()) {
			if ("children".equals(e.getKey())) {
				continue;
			}
			copy.put(e.getKey(), e.getValue());
		}
		if (value.containsKey("category_name")) {
			copy.put("name", value.get("category_name"));
		}
		if (value.containsKey("image_url")) {
			copy.put("img", value.get("image_url"));
		}
		Object ch = value.get("children");
		if (ch instanceof List<?> list && !list.isEmpty()) {
			List<Map<String, Object>> childMaps = new ArrayList<>();
			for (Object el : list) {
				if (el instanceof Map<?, ?> cm) {
					LinkedHashMap<String, Object> child = new LinkedHashMap<>();
					for (Map.Entry<?, ?> e : cm.entrySet()) {
						child.put(String.valueOf(e.getKey()), e.getValue());
					}
					childMaps.add(processingParamsNode(child));
				}
			}
			copy.put("children", childMaps);
		}
		return copy;
	}
}
