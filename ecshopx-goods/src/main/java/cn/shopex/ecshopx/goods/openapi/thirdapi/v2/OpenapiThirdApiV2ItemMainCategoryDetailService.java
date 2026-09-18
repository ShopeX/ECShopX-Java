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

package cn.shopex.ecshopx.goods.openapi.thirdapi.v2;

import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiItemsV2FailException;
import cn.shopex.ecshopx.goods.domain.ItemsAttributeValues;
import cn.shopex.ecshopx.goods.domain.ItemsAttributes;
import cn.shopex.ecshopx.goods.domain.ItemsCategory;
import cn.shopex.ecshopx.goods.repository.ItemsAttributeValuesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import cn.shopex.ecshopx.goods.service.ItemsAttributesRowMaps;
import cn.shopex.ecshopx.goods.service.ItemsCategoryRowMaps;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2ItemMainCategoryDetailService {

	private static final DateTimeFormatter DATETIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final ItemsCategoryRepository itemsCategoryRepository;
	private final ItemsAttributesRepository itemsAttributesRepository;
	private final ItemsAttributeValuesRepository itemsAttributeValuesRepository;
	private final ObjectMapper objectMapper;

	public OpenapiThirdApiV2ItemMainCategoryDetailService(
			ItemsCategoryRepository itemsCategoryRepository,
			ItemsAttributesRepository itemsAttributesRepository,
			ItemsAttributeValuesRepository itemsAttributeValuesRepository,
			ObjectMapper objectMapper) {
		this.itemsCategoryRepository = itemsCategoryRepository;
		this.itemsAttributesRepository = itemsAttributesRepository;
		this.itemsAttributeValuesRepository = itemsAttributeValuesRepository;
		this.objectMapper = objectMapper;
	}

	public Object executeOpenapiGetItemMainCategoryDetail(long companyId, String categoryRaw) {
		try {
			validateCategoryRequired(categoryRaw);
			String[] pathParts = parseCategoryPath(categoryRaw);
			long categoryId = resolveMainCategoryByPath(companyId, pathParts);
			if (categoryId < 0L) {
				return List.of();
			}
			Map<String, Object> rawDetail = loadCategoryDetailWithAttributes(companyId, categoryId);
			if (rawDetail == null || rawDetail.isEmpty()) {
				return List.of();
			}
			return formatMainCategoryDetail(rawDetail);
		} catch (OpenapiItemsV2FailException ex) {
			throw ex;
		} catch (DataAccessException ex) {
			throw new OpenapiItemsV2FailException("E5000", ex.getMostSpecificCause().getMessage());
		}
	}

	private static void validateCategoryRequired(String categoryRaw) {
		if (categoryRaw == null || categoryRaw.isEmpty()) {
			throw new OpenapiItemsV2FailException(
					OpenapiErrorCode.SERVICE_MISSING_PARAMS, "主类目名称必填");
		}
	}

	private static String[] parseCategoryPath(String categoryRaw) {
		String[] parts = categoryRaw.split("->", -1);
		if (parts.length != 3) {
			throw new OpenapiItemsV2FailException(
					OpenapiErrorCode.GOODS_MAINCATEGORY_ERROR, "主类目名称格式错误");
		}
		return parts;
	}

	private long resolveMainCategoryByPath(long companyId, String[] pathParts) {
		LinkedHashMap<String, Object> f1 = buildPathFilter(companyId);
		f1.put("category_name", pathParts[0]);
		List<Map<String, Object>> level1 = itemsCategoryRepository.lists(f1, companyId, 0L, 1, -1);
		if (level1 == null || level1.isEmpty()) {
			throw notFound("一级类目名称未查询到相关数据");
		}
		List<Long> parentIds1 = extractCategoryIds(level1);

		LinkedHashMap<String, Object> f2 = buildPathFilter(companyId);
		f2.put("category_name", pathParts[1]);
		f2.put("parent_id_in", parentIds1);
		List<Map<String, Object>> level2 = itemsCategoryRepository.lists(f2, companyId, 0L, 1, -1);
		if (level2 == null || level2.isEmpty()) {
			throw notFound("二级类目名称未查询到相关数据");
		}
		List<Long> parentIds2 = extractCategoryIds(level2);

		LinkedHashMap<String, Object> f3 = buildPathFilter(companyId);
		f3.put("category_name", pathParts[2]);
		f3.put("parent_id_in", parentIds2);
		Map<String, Object> level3Wrap = itemsCategoryRepository.listsWxappLevelCategoryWithTotalCount(
				f3, companyId, 0L, 1, 1);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> level3 = (List<Map<String, Object>>) level3Wrap.get("list");
		if (level3 == null || level3.isEmpty()) {
			throw notFound("三级类目名称未查询到相关数据");
		}
		List<Long> level3Ids = extractCategoryIds(level3);

		LinkedHashMap<String, Object> fCheck = buildPathFilter(companyId);
		fCheck.put("category_id_in", level3Ids);
		Map<String, Object> checkWrap = itemsCategoryRepository.listsWxappLevelCategoryWithTotalCount(
				fCheck, companyId, 0L, 1, 1);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> checkList = (List<Map<String, Object>>) checkWrap.get("list");
		if (checkList == null || checkList.isEmpty()) {
			return -1L;
		}
		return toLong(checkList.get(0).get("category_id"));
	}

	private LinkedHashMap<String, Object> buildPathFilter(long companyId) {
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("is_main_category", 1);
		filter.put("distributor_id", 0L);
		return filter;
	}

	private Map<String, Object> loadCategoryDetailWithAttributes(long companyId, long categoryId) {
		Optional<ItemsCategory> opt = itemsCategoryRepository.findEntityByCompanyAndCategoryId(companyId, categoryId);
		if (opt.isEmpty()) {
			return Collections.emptyMap();
		}
		ItemsCategory entity = opt.get();
		Map<String, Object> data = ItemsCategoryRowMaps.fromEntity(entity);

		List<Object> gpDecoded = parseGoodsJsonArray(entity.getGoodsParams());
		List<Object> gsDecoded = parseGoodsJsonArray(entity.getGoodsSpec());
		List<Long> mergedIds = mergeAttributeIds(gpDecoded, gsDecoded);

		if (mergedIds.isEmpty()) {
			return data;
		}

		List<ItemsAttributes> attrEntities =
				itemsAttributesRepository.listByCompanyAndAttributeIdsIn(companyId, mergedIds);

		List<Map<String, Object>> goodsParams = new ArrayList<>();
		List<Map<String, Object>> goodsSpec = new ArrayList<>();
		for (ItemsAttributes row : attrEntities) {
			Map<String, Object> attrRow = ItemsAttributesRowMaps.toAttributeRowMap(row);
			List<ItemsAttributeValues> vals =
					itemsAttributeValuesRepository.listByAttributeId(companyId, row.getAttributeId());
			attrRow.put("attribute_values",
					ItemsAttributesRowMaps.buildAttributeValuesNestedFromEntities(vals));
			if ("item_params".equals(row.getAttributeType())) {
				goodsParams.add(attrRow);
			} else {
				goodsSpec.add(attrRow);
			}
		}
		goodsSpec.sort(Comparator.comparingLong(a -> -asLong(a.get("attribute_id"))));

		data.put("goods_params", goodsParams);
		data.put("goods_spec", goodsSpec);
		return data;
	}

	private Map<String, Object> formatMainCategoryDetail(Map<String, Object> raw) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("category_id", stringifyScalar(raw.get("category_id")));
		out.put("category_name", nullToEmpty(raw.get("category_name")));
		out.put("sort", stringifyScalar(raw.get("sort")));
		out.put("category_level", stringifyScalar(raw.get("category_level")));
		out.put("path", nullToEmpty(raw.get("path")));
		out.put("image_url", nullToEmpty(raw.get("image_url")));
		out.put("created", formatEpochSeconds(raw.get("created")));
		out.put("updated", formatEpochSeconds(raw.get("updated")));

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> gp = (List<Map<String, Object>>) raw.get("goods_params");
		if (gp != null && !gp.isEmpty()) {
			out.put("goods_params", formatAttributeList(gp));
		}
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> gs = (List<Map<String, Object>>) raw.get("goods_spec");
		if (gs != null && !gs.isEmpty()) {
			out.put("goods_spec", formatAttributeList(gs));
		}
		return out;
	}

	private List<Map<String, Object>> formatAttributeList(List<Map<String, Object>> attrs) {
		List<Map<String, Object>> result = new ArrayList<>();
		for (Map<String, Object> attr : attrs) {
			result.add(formatAttribute(attr));
		}
		return result;
	}

	private Map<String, Object> formatAttribute(Map<String, Object> attr) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("attribute_id", stringifyScalar(attr.get("attribute_id")));
		out.put("attribute_name", nullToEmpty(attr.get("attribute_name")));
		out.put("attribute_memo", nullToEmpty(attr.get("attribute_memo")));
		out.put("attribute_sort", stringifyScalar(attr.get("attribute_sort")));
		out.put("is_show", attr.get("is_show"));
		out.put("is_image", attr.get("is_image"));
		out.put("created", formatEpochSeconds(attr.get("created")));
		out.put("updated", formatEpochSeconds(attr.get("updated")));
		Object nested = attr.get("attribute_values");
		if (nested instanceof Map<?, ?> nestedMap) {
			out.put("attribute_values", formatAttributeValuesNested(nestedMap));
		}
		return out;
	}

	private Map<String, Object> formatAttributeValuesNested(Map<?, ?> nested) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", stringifyScalar(nested.get("total_count")));
		Object listObj = nested.get("list");
		List<Map<String, Object>> formattedList = new ArrayList<>();
		if (listObj instanceof List<?> rawList) {
			for (Object o : rawList) {
				if (o instanceof Map<?, ?> vm) {
					@SuppressWarnings("unchecked")
					Map<String, Object> valMap = (Map<String, Object>) vm;
					formattedList.add(formatAttributeValue(valMap));
				}
			}
		}
		out.put("list", formattedList);
		return out;
	}

	private Map<String, Object> formatAttributeValue(Map<String, Object> val) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("attribute_value_id", stringifyScalar(val.get("attribute_value_id")));
		out.put("attribute_id", stringifyScalar(val.get("attribute_id")));
		out.put("attribute_value", nullToEmpty(val.get("attribute_value")));
		out.put("sort", stringifyScalar(val.get("sort")));
		out.put("image_url", val.get("image_url"));
		out.put("created", formatEpochSeconds(val.get("created")));
		out.put("updated", formatEpochSeconds(val.get("updated")));
		return out;
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

	private static List<Long> extractCategoryIds(List<Map<String, Object>> rows) {
		List<Long> ids = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			long id = toLong(row.get("category_id"));
			if (id > 0L) {
				ids.add(id);
			}
		}
		return ids;
	}

	private static OpenapiItemsV2FailException notFound(String message) {
		return new OpenapiItemsV2FailException(OpenapiErrorCode.GOODS_MAINCATEGORY_NOT_FOUND, message);
	}

	private static long toLong(Object v) {
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

	private static long asLong(Object v) {
		return toLong(v);
	}

	private static String stringifyScalar(Object v) {
		if (v == null) {
			return "";
		}
		return v.toString();
	}

	private static String nullToEmpty(Object v) {
		return v == null ? "" : v.toString();
	}

	private static String formatEpochSeconds(Object epochObj) {
		if (epochObj == null) {
			return null;
		}
		long epoch;
		if (epochObj instanceof Number n) {
			epoch = n.longValue();
		} else {
			try {
				epoch = Long.parseLong(epochObj.toString());
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return Instant.ofEpochSecond(epoch).atZone(ZoneId.systemDefault()).format(DATETIME_FMT);
	}
}
