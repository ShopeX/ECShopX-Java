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

import cn.shopex.ecshopx.common.openapi.OpenapiItemsV2FailException;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import cn.shopex.ecshopx.goods.service.ItemsCategoryMultiLangApplier;
import cn.shopex.ecshopx.goods.service.ItemsCategoryTreeService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2ItemCategoryListService {

	private static final DateTimeFormatter DATETIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final ItemsCategoryRepository itemsCategoryRepository;
	private final ItemsCategoryMultiLangApplier itemsCategoryMultiLangApplier;
	private final ItemsCategoryTreeService itemsCategoryTreeService;

	public OpenapiThirdApiV2ItemCategoryListService(
			ItemsCategoryRepository itemsCategoryRepository,
			ItemsCategoryMultiLangApplier itemsCategoryMultiLangApplier,
			ItemsCategoryTreeService itemsCategoryTreeService) {
		this.itemsCategoryRepository = itemsCategoryRepository;
		this.itemsCategoryMultiLangApplier = itemsCategoryMultiLangApplier;
		this.itemsCategoryTreeService = itemsCategoryTreeService;
	}

	public List<Map<String, Object>> executeOpenapiGetItemCategoryList(long companyId) {
		return executeOpenapiGetCategoryList(companyId, false);
	}

	public List<Map<String, Object>> executeOpenapiGetItemMainCategoryList(long companyId) {
		return executeOpenapiGetCategoryList(companyId, true);
	}

	private List<Map<String, Object>> executeOpenapiGetCategoryList(long companyId, boolean isMainCategory) {
		LinkedHashMap<String, Object> filter = buildOpenapiFilter(companyId, isMainCategory);

		List<Map<String, Object>> flat;
		try {
			flat = itemsCategoryRepository.lists(filter, companyId, 0L, 1, -1);
		} catch (DataAccessException ex) {
			throw new OpenapiItemsV2FailException("E5000", ex.getMostSpecificCause().getMessage());
		}

		if (flat == null || flat.isEmpty()) {
			return List.of();
		}

		itemsCategoryMultiLangApplier.apply(companyId, "zh-CN", flat);
		List<Map<String, Object>> tree = itemsCategoryTreeService.getTree(flat, 0L, 0, true);
		return formatCategoryList(tree);
	}

	private LinkedHashMap<String, Object> buildOpenapiFilter(long companyId, boolean isMainCategory) {
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("is_main_category", isMainCategory ? 1 : 0);
		filter.put("distributor_id", 0L);
		return filter;
	}

	private List<Map<String, Object>> formatCategoryList(List<Map<String, Object>> dataList) {
		if (dataList == null || dataList.isEmpty()) {
			return List.of();
		}
		List<Map<String, Object>> result = new ArrayList<>();
		for (Map<String, Object> node : dataList) {
			result.add(formatCategory(node));
		}
		return result;
	}

	private Map<String, Object> formatCategory(Map<String, Object> category) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("category_id", stringifyScalar(category.get("category_id")));
		out.put("category_name", nullToEmpty(category.get("category_name")));
		out.put("category_level", stringifyScalar(category.get("category_level")));
		out.put("parent_id", stringifyScalar(category.get("parent_id")));
		out.put("path", nullToEmpty(category.get("path")));
		out.put("sort", stringifyScalar(category.get("sort")));
		out.put("image_url", nullToEmpty(category.get("image_url")));
		out.put("created", formatEpochSeconds(category.get("created")));
		out.put("updated", formatEpochSeconds(category.get("updated")));

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> children = (List<Map<String, Object>>) category.get("children");
		if (children != null && !children.isEmpty()) {
			List<Map<String, Object>> formattedChildren = new ArrayList<>();
			for (Map<String, Object> child : children) {
				formattedChildren.add(formatCategory(child));
			}
			out.put("children", formattedChildren);
		}
		return out;
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
