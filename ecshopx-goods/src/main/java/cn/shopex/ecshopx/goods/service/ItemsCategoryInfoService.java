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

import cn.shopex.ecshopx.goods.domain.ItemRelAttributes;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.ItemsAttributeValues;
import cn.shopex.ecshopx.goods.domain.ItemsAttributes;
import cn.shopex.ecshopx.goods.domain.ItemsCategory;
import cn.shopex.ecshopx.goods.repository.ItemRelAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsAttributeValuesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsQueryRepository;
import cn.shopex.ecshopx.pointsmall.domain.PointsmallItemRelAttributes;
import cn.shopex.ecshopx.pointsmall.domain.PointsmallItems;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemRelAttributesMapper;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ItemsCategoryInfoService {

	private final ItemsCategoryRepository itemsCategoryRepository;
	private final ItemsAttributesRepository itemsAttributesRepository;
	private final ItemsAttributeValuesRepository itemsAttributeValuesRepository;
	private final ItemsQueryRepository itemsQueryRepository;
	private final ItemRelAttributesRepository itemRelAttributesRepository;
	private final PointsmallItemsMapper pointsmallItemsMapper;
	private final PointsmallItemRelAttributesMapper pointsmallItemRelAttributesMapper;
	private final ItemsCategoryMultiLangApplier itemsCategoryMultiLangApplier;
	private final ObjectMapper objectMapper;

	public ItemsCategoryInfoService(ItemsCategoryRepository itemsCategoryRepository,
			ItemsAttributesRepository itemsAttributesRepository,
			ItemsAttributeValuesRepository itemsAttributeValuesRepository, ItemsQueryRepository itemsQueryRepository,
			ItemRelAttributesRepository itemRelAttributesRepository, PointsmallItemsMapper pointsmallItemsMapper,
			PointsmallItemRelAttributesMapper pointsmallItemRelAttributesMapper,
			ItemsCategoryMultiLangApplier itemsCategoryMultiLangApplier, ObjectMapper objectMapper) {
		this.itemsCategoryRepository = itemsCategoryRepository;
		this.itemsAttributesRepository = itemsAttributesRepository;
		this.itemsAttributeValuesRepository = itemsAttributeValuesRepository;
		this.itemsQueryRepository = itemsQueryRepository;
		this.itemRelAttributesRepository = itemRelAttributesRepository;
		this.pointsmallItemsMapper = pointsmallItemsMapper;
		this.pointsmallItemRelAttributesMapper = pointsmallItemRelAttributesMapper;
		this.itemsCategoryMultiLangApplier = itemsCategoryMultiLangApplier;
		this.objectMapper = objectMapper;
	}

	/**
	 * 分类详情：找到为单层 Map；未找到为空列表（与现网一致）。
	 */
	public Object getCategoryInfo(long companyId, long categoryId, Optional<Long> itemIdOpt, int isPoint, String countryCode) {
		Optional<ItemsCategory> opt = itemsCategoryRepository.findEntityByCompanyAndCategoryId(companyId, categoryId);
		if (opt.isEmpty()) {
			return Collections.emptyList();
		}
		ItemsCategory entity = opt.get();
		Map<String, Object> data = ItemsCategoryRowMaps.fromEntity(entity);
		String catIdStr = String.valueOf(entity.getCategoryId() != null ? entity.getCategoryId() : 0L);
		data.put("id", catIdStr);
		data.put("label", data.get("category_name"));

		decodeTaobaoCategoryInfo(data, entity.getTaobaoCategoryInfo());

		List<Object> gpDecoded = parseGoodsJsonArray(entity.getGoodsParams());
		List<Object> gsDecoded = parseGoodsJsonArray(entity.getGoodsSpec());
		List<Long> mergedIds = mergeAttributeIds(gpDecoded, gsDecoded);
		if (mergedIds.isEmpty()) {
			data.put("goods_params", toSerializableList(gpDecoded));
			data.put("goods_spec", toSerializableList(gsDecoded));
			finishLabels(data);
			itemsCategoryMultiLangApplier.apply(companyId, countryCode, Collections.singletonList(data));
			syncLabelWithCategoryName(data);
			return data;
		}

		List<ItemsAttributes> attrEntities = itemsAttributesRepository.listByCompanyAndAttributeIdsIn(companyId, mergedIds);
		Map<String, String> customValues = Collections.emptyMap();
		if (itemIdOpt.isPresent() && itemIdOpt.get() >= 1L) {
			customValues = loadCustomAttributeValues(companyId, itemIdOpt.get(), isPoint, mergedIds);
		}

		List<Map<String, Object>> goodsParams = new ArrayList<>();
		List<Map<String, Object>> goodsSpec = new ArrayList<>();
		for (ItemsAttributes row : attrEntities) {
			Map<String, Object> attrRow = ItemsAttributesRowMaps.toAttributeRowMap(row);
			List<ItemsAttributeValues> vals = itemsAttributeValuesRepository.listByAttributeId(companyId, row.getAttributeId());
			Map<String, Object> nested = ItemsAttributesRowMaps.buildAttributeValuesNestedFromEntities(vals);
			attrRow.put("attribute_values", nested);
			String type = row.getAttributeType();
			if ("item_params".equals(type)) {
				applyCustomToParamsRow(attrRow, customValues);
				goodsParams.add(attrRow);
			} else {
				applyCustomToSpecRow(attrRow, customValues);
				goodsSpec.add(attrRow);
			}
		}

		goodsSpec.sort(Comparator.comparingLong(a -> -asLong(a.get("attribute_id"))));

		data.put("goods_params", goodsParams);
		data.put("goods_spec", goodsSpec);
		finishLabels(data);
		itemsCategoryMultiLangApplier.apply(companyId, countryCode, Collections.singletonList(data));
		syncLabelWithCategoryName(data);
		return data;
	}

	private static void syncLabelWithCategoryName(Map<String, Object> data) {
		Object cn = data.get("category_name");
		data.put("label", cn != null ? cn.toString() : "");
	}

	private void finishLabels(Map<String, Object> data) {
		data.put("id", data.get("category_id"));
	}

	private void decodeTaobaoCategoryInfo(Map<String, Object> data, String raw) {
		if (!StringUtils.hasText(raw)) {
			data.put("taobao_category_info", Collections.emptyList());
			return;
		}
		try {
			JsonNode n = objectMapper.readTree(raw);
			if (n.isArray() || n.isObject()) {
				data.put("taobao_category_info", objectMapper.readValue(raw, Object.class));
			} else {
				data.put("taobao_category_info", Collections.emptyList());
			}
		} catch (Exception e) {
			data.put("taobao_category_info", Collections.emptyList());
		}
	}

	private List<Object> parseGoodsJsonArray(String json) {
		if (!StringUtils.hasText(json)) {
			return new ArrayList<>();
		}
		try {
			return objectMapper.readValue(json, new TypeReference<List<Object>>() {});
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	private static List<Object> toSerializableList(List<Object> src) {
		if (src == null) {
			return Collections.emptyList();
		}
		return new ArrayList<>(src);
	}

	private static List<Long> mergeAttributeIds(List<Object> gp, List<Object> gs) {
		Set<Long> seen = new LinkedHashSet<>();
		addIdsFrom(gp, seen);
		addIdsFrom(gs, seen);
		return new ArrayList<>(seen);
	}

	private static void addIdsFrom(List<Object> arr, Set<Long> seen) {
		if (arr == null) {
			return;
		}
		for (Object el : arr) {
			Long id = extractAttributeId(el);
			if (id != null && id != 0L) {
				seen.add(id);
			}
		}
	}

	private static Long extractAttributeId(Object el) {
		if (el instanceof Number n) {
			return n.longValue();
		}
		if (el instanceof CharSequence cs) {
			String s = cs.toString().trim();
			if (s.isEmpty()) {
				return null;
			}
			try {
				return Long.parseLong(s);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		if (el instanceof Map<?, ?> m) {
			Object aid = m.get("attribute_id");
			if (aid instanceof Number n) {
				return n.longValue();
			}
			if (aid != null) {
				try {
					return Long.parseLong(aid.toString());
				} catch (NumberFormatException e) {
					return null;
				}
			}
		}
		return null;
	}

	private Map<String, String> loadCustomAttributeValues(long companyId, long defaultItemId, int isPoint, List<Long> attributeIds) {
		if (isPoint == 1) {
			LambdaQueryWrapper<PointsmallItems> w = new LambdaQueryWrapper<>();
			w.eq(PointsmallItems::getCompanyId, companyId).eq(PointsmallItems::getDefaultItemId, defaultItemId).last("LIMIT 100");
			List<PointsmallItems> items = pointsmallItemsMapper.selectList(w);
			if (items.isEmpty()) {
				return Collections.emptyMap();
			}
			List<Long> itemIds = new ArrayList<>();
			for (PointsmallItems it : items) {
				if (it.getItemId() != null) {
					itemIds.add(it.getItemId());
				}
			}
			if (itemIds.isEmpty()) {
				return Collections.emptyMap();
			}
			LambdaQueryWrapper<PointsmallItemRelAttributes> rw = new LambdaQueryWrapper<>();
			rw.eq(PointsmallItemRelAttributes::getCompanyId, companyId).in(PointsmallItemRelAttributes::getItemId, itemIds)
					.in(PointsmallItemRelAttributes::getAttributeId, attributeIds).last("LIMIT 100");
			List<PointsmallItemRelAttributes> rels = pointsmallItemRelAttributesMapper.selectList(rw);
			return buildCustomMapFromPointsmall(rels);
		}
		List<Items> items = itemsQueryRepository.listByDefaultItemIdAndCompanyId(defaultItemId, companyId);
		if (items.isEmpty()) {
			return Collections.emptyMap();
		}
		List<Long> itemIds = new ArrayList<>();
		for (Items it : items) {
			if (it.getItemId() != null) {
				itemIds.add(it.getItemId());
			}
		}
		if (itemIds.isEmpty()) {
			return Collections.emptyMap();
		}
		List<ItemRelAttributes> rels = itemRelAttributesRepository.listByItemIdsAndAttributeIds(companyId, itemIds, attributeIds);
		return buildCustomMapFromNormal(rels);
	}

	private static Map<String, String> buildCustomMapFromNormal(List<ItemRelAttributes> rels) {
		Map<String, String> customValues = new HashMap<>();
		for (ItemRelAttributes r : rels) {
			String v = r.getCustomAttributeValue();
			if (!StringUtils.hasText(v)) {
				continue;
			}
			String type = r.getAttributeType();
			if ("item_params".equals(type)) {
				if (r.getAttributeValueId() != null && r.getAttributeValueId() != 0L) {
					customValues.put(r.getAttributeId() + "_" + r.getAttributeValueId(), v);
				} else {
					customValues.put(String.valueOf(r.getAttributeId()), v);
				}
			} else if ("item_spec".equals(type) && r.getAttributeValueId() != null && r.getAttributeValueId() != 0L) {
				customValues.put(r.getAttributeId() + "_" + r.getAttributeValueId(), v);
			}
		}
		return customValues;
	}

	private static Map<String, String> buildCustomMapFromPointsmall(List<PointsmallItemRelAttributes> rels) {
		Map<String, String> customValues = new HashMap<>();
		for (PointsmallItemRelAttributes r : rels) {
			String v = r.getCustomAttributeValue();
			if (!StringUtils.hasText(v)) {
				continue;
			}
			String type = r.getAttributeType();
			if ("item_params".equals(type)) {
				if (r.getAttributeValueId() != null && r.getAttributeValueId() != 0L) {
					customValues.put(r.getAttributeId() + "_" + r.getAttributeValueId(), v);
				} else {
					customValues.put(String.valueOf(r.getAttributeId()), v);
				}
			} else if ("item_spec".equals(type) && r.getAttributeValueId() != null && r.getAttributeValueId() != 0L) {
				customValues.put(r.getAttributeId() + "_" + r.getAttributeValueId(), v);
			}
		}
		return customValues;
	}

	@SuppressWarnings("unchecked")
	private static void applyCustomToParamsRow(Map<String, Object> attrRow, Map<String, String> customValues) {
		Object nestedObj = attrRow.get("attribute_values");
		if (nestedObj instanceof Map<?, ?> nested) {
			Object listObj = nested.get("list");
			if (listObj instanceof List<?> rawList) {
				for (Object o : rawList) {
					if (o instanceof Map) {
						Map<String, Object> attrValue = (Map<String, Object>) o;
						Object vid = attrValue.get("attribute_value_id");
						Object aid = attrRow.get("attribute_id");
						if (vid != null && aid != null) {
							String key = aid + "_" + vid;
							String val = customValues.get(key);
							if (val != null) {
								attrValue.put("custom_attribute_value", val);
								attrValue.put("attribute_value", val);
							}
						}
					}
				}
			}
		}
		Object aidObj = attrRow.get("attribute_id");
		if (aidObj != null) {
			String idKey = String.valueOf(aidObj);
			String val = customValues.get(idKey);
			if (val != null) {
				attrRow.put("custom_attribute_value", val);
				attrRow.put("attribute_value", val);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static void applyCustomToSpecRow(Map<String, Object> attrRow, Map<String, String> customValues) {
		Object nestedObj = attrRow.get("attribute_values");
		if (nestedObj instanceof Map<?, ?> nested) {
			Object listObj = nested.get("list");
			if (listObj instanceof List<?> rawList) {
				for (Object o : rawList) {
					if (o instanceof Map) {
						Map<String, Object> attrValue = (Map<String, Object>) o;
						Object vid = attrValue.get("attribute_value_id");
						Object aid = attrRow.get("attribute_id");
						if (vid != null && aid != null) {
							String key = aid + "_" + vid;
							String val = customValues.get(key);
							if (val != null) {
								attrValue.put("custom_attribute_value", val);
								attrValue.put("attribute_value", val);
							}
						}
					}
				}
			}
		}
	}

	private static long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v != null) {
			try {
				return Long.parseLong(v.toString());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		return 0L;
	}
}
