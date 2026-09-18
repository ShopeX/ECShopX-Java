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

package cn.shopex.ecshopx.goods.service.wxapp;

import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.goods.mapper.ItemsWxappFacetSqlParams;
import cn.shopex.ecshopx.goods.mapper.ItemsWxappFilterFacetMapper;
import cn.shopex.ecshopx.goods.mapper.ItemsWxappFilterFacetSqlBuilder;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class WxappGoodsItemsFilterServiceImpl implements WxappGoodsItemsFilterService {

	private final ItemsWxappFilterFacetMapper itemsWxappFilterFacetMapper;
	private final ItemsCategoryRepository itemsCategoryRepository;
	private final DistributorListQueryService distributorListQueryService;

	public WxappGoodsItemsFilterServiceImpl(ItemsWxappFilterFacetMapper itemsWxappFilterFacetMapper,
			ItemsCategoryRepository itemsCategoryRepository,
			DistributorListQueryService distributorListQueryService) {
		this.itemsWxappFilterFacetMapper = itemsWxappFilterFacetMapper;
		this.itemsCategoryRepository = itemsCategoryRepository;
		this.distributorListQueryService = distributorListQueryService;
	}

	@Override
	public Map<String, Object> buildFilterResult(long companyId, Map<String, Object> clientFilter) {
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>(clientFilter);
		filter.remove("is_default");
		filter.put("company_id", companyId);

		ItemsWxappFacetSqlParams sqlParams = ItemsWxappFilterFacetSqlBuilder.build(filter);
		List<Map<String, Object>> rows = new ArrayList<>(itemsWxappFilterFacetMapper.selectFacetRows(sqlParams));

		Set<Long> mainCategoryIds = new LinkedHashSet<>();
		Set<Long> distributorIds = new LinkedHashSet<>();
		LinkedHashMap<Long, AttributeAgg> itemParams = new LinkedHashMap<>();
		LinkedHashMap<Long, AttributeAgg> itemSpec = new LinkedHashMap<>();

		for (Map<String, Object> row : rows) {
			long itemCat = longish(row.get("item_category"));
			if (itemCat > 0L) {
				mainCategoryIds.add(itemCat);
			}
			long dist = longish(row.get("distributor_id"));
			if (dist > 0L) {
				distributorIds.add(dist);
			}

			String attrType = stringOf(row.get("attribute_type"));
			if ("item_params".equals(attrType)) {
				mergeAttrRow(itemParams, row);
				reshapeAllValues(itemParams);
			}
			if ("item_spec".equals(attrType)) {
				mergeAttrRow(itemSpec, row);
				reshapeAllValues(itemSpec);
			}
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("main_category_ids", new ArrayList<>(mainCategoryIds));
		result.put("distributor_ids", new ArrayList<>(distributorIds));
		result.put("item_params", flattenAttrMap(itemParams));
		result.put("item_spec", flattenAttrMap(itemSpec));

		if (!mainCategoryIds.isEmpty()) {
			result.put("main_category_list", itemsCategoryRepository.getTopByChildrenId(new ArrayList<>(mainCategoryIds)));
		}
		if (!distributorIds.isEmpty()) {
			result.put("distributor_list", distributorListQueryService.listDistributionNameRowsByIdsPaged(companyId,
					new ArrayList<>(distributorIds), 1, 10));
		}
		return result;
	}

	private static void mergeAttrRow(LinkedHashMap<Long, AttributeAgg> byAttrId, Map<String, Object> row) {
		long aid = longish(row.get("attribute_id"));
		long vid = longish(row.get("attribute_value_id"));
		AttributeAgg agg = byAttrId.computeIfAbsent(aid, k -> new AttributeAgg(aid, stringOf(row.get("attribute_name"))));
		LinkedHashMap<String, Object> valRow = new LinkedHashMap<>();
		valRow.put("attribute_value_id", vid);
		valRow.put("attribute_value_name", stringOf(row.get("attribute_value_name")));
		agg.valuesByValueId.put(vid, valRow);
	}

	private static void reshapeAllValues(LinkedHashMap<Long, AttributeAgg> map) {
		for (AttributeAgg agg : map.values()) {
			agg.valuesAsList = new ArrayList<>(agg.valuesByValueId.values());
		}
	}

	private static List<Map<String, Object>> flattenAttrMap(LinkedHashMap<Long, AttributeAgg> src) {
		List<Map<String, Object>> out = new ArrayList<>();
		for (AttributeAgg ag : src.values()) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("attribute_id", ag.attributeId);
			m.put("attribute_name", ag.attributeName);
			m.put("values", ag.valuesAsList != null ? new ArrayList<>(ag.valuesAsList) : new ArrayList<>(ag.valuesByValueId.values()));
			out.add(m);
		}
		return out;
	}

	private static long longish(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String stringOf(Object o) {
		return o == null ? "" : o.toString();
	}

	private static final class AttributeAgg {
		private final long attributeId;
		private final String attributeName;
		private final LinkedHashMap<Long, Map<String, Object>> valuesByValueId = new LinkedHashMap<>();
		private List<Map<String, Object>> valuesAsList;

		private AttributeAgg(long attributeId, String attributeName) {
			this.attributeId = attributeId;
			this.attributeName = attributeName;
		}
	}
}
