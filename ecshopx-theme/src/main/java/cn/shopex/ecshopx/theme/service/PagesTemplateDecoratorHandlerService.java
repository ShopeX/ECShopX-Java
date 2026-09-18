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

package cn.shopex.ecshopx.theme.service;

import cn.shopex.ecshopx.distribution.service.DistributorTagsListByIdsFieldOrderService;
import cn.shopex.ecshopx.goods.service.tags.ItemsTagsListByIdsFieldOrderService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PagesTemplateDecoratorHandlerService {

	private final DistributorTagsListByIdsFieldOrderService distributorTagsListByIdsFieldOrderService;
	private final ItemsTagsListByIdsFieldOrderService itemsTagsListByIdsFieldOrderService;

	public PagesTemplateDecoratorHandlerService(
			DistributorTagsListByIdsFieldOrderService distributorTagsListByIdsFieldOrderService,
			ItemsTagsListByIdsFieldOrderService itemsTagsListByIdsFieldOrderService) {
		this.distributorTagsListByIdsFieldOrderService = distributorTagsListByIdsFieldOrderService;
		this.itemsTagsListByIdsFieldOrderService = itemsTagsListByIdsFieldOrderService;
	}

	public void applyTemplateHandler(long companyId, List<Map<String, Object>> list) {
		if (list == null) {
			return;
		}
		for (Map<String, Object> row : list) {
			String name = String.valueOf(row.getOrDefault("name", ""));
			Map<String, Object> p = getParamsMap(row);
			if (p == null) {
				continue;
			}
			if ("nearbyShop".equals(name)) {
				Object st = p.get("seletedTags");
				if (!(st instanceof List<?> rawList) || rawList.isEmpty()) {
					continue;
				}
				List<Long> tagIds = new ArrayList<>();
				for (Object o : rawList) {
					if (o instanceof Map<?, ?> tm) {
						Object tid = tm.get("tag_id");
						long id = longOrZero(tid);
						if (id > 0L) {
							tagIds.add(id);
						}
					}
				}
				if (tagIds.isEmpty()) {
					continue;
				}
				List<Map<String, Object>> tags = distributorTagsListByIdsFieldOrderService.listOrdered(companyId, tagIds, 1);
				p.put("seletedTags", tags);
			} else if ("store".equals(name)) {
				Object st = p.get("seletedTags");
				if (!(st instanceof List<?> rawList) || rawList.isEmpty()) {
					continue;
				}
				List<Long> tagIds = new ArrayList<>();
				for (Object o : rawList) {
					if (o instanceof Map<?, ?> tm) {
						Object tid = tm.get("tag_id");
						long id = longOrZero(tid);
						if (id > 0L) {
							tagIds.add(id);
						}
					}
				}
				if (tagIds.isEmpty()) {
					continue;
				}
				List<Map<String, Object>> tags = itemsTagsListByIdsFieldOrderService.listOrdered(companyId, tagIds, 1);
				p.put("seletedTags", tags);
			}
		}
	}

	private static Map<String, Object> getParamsMap(Map<String, Object> row) {
		Object p = row.get("params");
		if (!(p instanceof Map<?, ?> m)) {
			return null;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> out = (Map<String, Object>) (Map<?, ?>) m;
		return out;
	}

	private static long longOrZero(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v == null) {
			return 0L;
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
