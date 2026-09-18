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

package cn.shopex.ecshopx.goods.service.pointsmall.export;

import cn.shopex.ecshopx.common.dispatch.PointsmallItemsExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.goods.service.items.ItemsCategoryItemIdResolver;
import cn.shopex.ecshopx.pointsmall.service.PointsmallExportItemsDataService;
import cn.shopex.ecshopx.pointsmall.service.dto.PointsmallExportItemsDataResult;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PointsmallExportItemsDataServiceImpl implements PointsmallExportItemsDataService {

	private final ItemsCategoryItemIdResolver itemsCategoryItemIdResolver;
	private final PointsmallItemsListForExportQueryService pointsmallItemsListForExportQueryService;
	private final PointsmallItemsExportFileJobDispatchPublisher pointsmallItemsExportFileJobDispatchPublisher;

	public PointsmallExportItemsDataServiceImpl(ItemsCategoryItemIdResolver itemsCategoryItemIdResolver,
			PointsmallItemsListForExportQueryService pointsmallItemsListForExportQueryService,
			PointsmallItemsExportFileJobDispatchPublisher pointsmallItemsExportFileJobDispatchPublisher) {
		this.itemsCategoryItemIdResolver = itemsCategoryItemIdResolver;
		this.pointsmallItemsListForExportQueryService = pointsmallItemsListForExportQueryService;
		this.pointsmallItemsExportFileJobDispatchPublisher = pointsmallItemsExportFileJobDispatchPublisher;
	}

	@Override
	public PointsmallExportItemsDataResult submit(long companyId, long operatorId, Map<String, Object> input) {
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", companyId);

		if (ValuePresence.hasEffectiveValue(input.get("templates_id"))) {
			params.put("templates_id", toInt(input.get("templates_id")));
		}
		Object regionsRaw = input.get("regions_id");
		if (ValuePresence.hasEffectiveValue(regionsRaw)) {
			String joined = normalizeRegionsId(regionsRaw);
			if (joined != null) {
				params.put("regions_id", joined);
			}
		}
		if (ValuePresence.hasEffectiveValue(input.get("keywords"))) {
			params.put("item_name|contains", input.get("keywords").toString().trim());
		}
		if (input.containsKey("nospec")) {
			params.put("nospec", input.get("nospec"));
		}
		Object approveStatus = input.get("approve_status");
		if (ValuePresence.hasEffectiveValue(approveStatus)) {
			String s = approveStatus.toString();
			if ("processing".equals(s) || "rejected".equals(s)) {
				params.put("audit_status", s);
			} else {
				params.put("approve_status", s);
			}
		}
		if (ValuePresence.hasEffectiveValue(input.get("item_id"))) {
			params.put("item_id", normalizeLongList(input.get("item_id")));
		}
		if (ValuePresence.hasEffectiveValue(input.get("main_cat_id"))) {
			long mainCat = toLong(input.get("main_cat_id"));
			List<Long> cats = itemsCategoryItemIdResolver.expandMainCategoryIdsForPointsmallExport(companyId, mainCat);
			List<String> asStr = cats.stream().map(String::valueOf).toList();
			params.put("item_category", asStr);
		}
		if (ValuePresence.hasEffectiveValue(input.get("category"))) {
			long categoryId = toLong(input.get("category"));
			List<Long> ids = itemsCategoryItemIdResolver.getItemIdsByCategoryTree(companyId, categoryId);
			if (ids.isEmpty()) {
				return new PointsmallExportItemsDataResult.EmptyPreview();
			}
			@SuppressWarnings("unchecked")
			List<Long> existing = (List<Long>) params.get("item_id");
			if (existing != null && !existing.isEmpty()) {
				LinkedHashSet<Long> a = new LinkedHashSet<>(existing);
				a.retainAll(new LinkedHashSet<>(ids));
				params.put("item_id", new ArrayList<>(a));
			} else {
				params.put("item_id", new ArrayList<>(ids));
			}
		}

		String itemType = input.get("item_type") != null ? input.get("item_type").toString() : "services";
		params.put("item_type", itemType);

		int storeGt = intOrZero(input.get("store_gt"));
		if (storeGt != 0) {
			params.put("store|gt", storeGt);
		}
		int storeLt = intOrZero(input.get("store_lt"));
		if (storeLt != 0) {
			params.put("store|lt", storeLt);
		}
		if (numericNonZero(input.get("price_gt"))) {
			params.put("point|gt", toPointFilterValue(input.get("price_gt")));
		}
		if (numericNonZero(input.get("price_lt"))) {
			params.put("point|lt", toPointFilterValue(input.get("price_lt")));
		}

		Object st = input.get("special_type");
		if (st != null) {
			String ss = st.toString();
			if ("normal".equals(ss) || "drug".equals(ss)) {
				params.put("special_type", ss);
			}
		}
		int brandId = intOrZero(input.get("brand_id"));
		if (brandId != 0) {
			params.put("brand_id", brandId);
		}

		if (ValuePresence.hasEffectiveValue(input.get("item_bn"))) {
			LinkedHashMap<String, Object> q = new LinkedHashMap<>(params);
			q.put("item_bn", input.get("item_bn").toString());
			List<Long> found = pointsmallItemsListForExportQueryService.listDefaultItemIdsForExport(q);
			if (found.isEmpty()) {
				return new PointsmallExportItemsDataResult.EmptyPreview();
			}
			params.remove("item_bn");
			params.put("item_id", found);
		}

		boolean isGetSkuList = isSkuListFlag(input.get("is_sku"));
		params.put("isGetSkuList", isGetSkuList);

		pointsmallItemsExportFileJobDispatchPublisher.publish(companyId, operatorId, new LinkedHashMap<>(params));
		return new PointsmallExportItemsDataResult.Enqueued();
	}

	private static boolean isSkuListFlag(Object v) {
		if (v instanceof Boolean b) {
			return b;
		}
		return "true".equals(String.valueOf(v));
	}

	private static String normalizeRegionsId(Object v) {
		if (v instanceof String s) {
			return s;
		}
		if (v instanceof List<?> list) {
			return list.stream().filter(Objects::nonNull).map(Object::toString).filter(StringUtils::hasText)
					.reduce((a, b) -> a + "," + b).orElse(null);
		}
		if (v != null && v.getClass().isArray()) {
			Object[] arr = (Object[]) v;
			List<String> parts = new ArrayList<>();
			for (Object o : arr) {
				if (o != null && StringUtils.hasText(o.toString())) {
					parts.add(o.toString());
				}
			}
			return parts.isEmpty() ? null : String.join(",", parts);
		}
		return v != null ? v.toString() : null;
	}

	private static List<Long> normalizeLongList(Object v) {
		if (v instanceof Map<?, ?> map) {
			List<Long> out = new ArrayList<>();
			for (Object o : map.values()) {
				if (o == null) {
					continue;
				}
				out.add(toLong(o));
			}
			return out;
		}
		if (v instanceof List<?> list) {
			List<Long> out = new ArrayList<>();
			for (Object o : list) {
				if (o == null) {
					continue;
				}
				out.add(toLong(o));
			}
			return out;
		}
		return List.of(toLong(v));
	}

	private static long toLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(v.toString().trim());
	}

	private static int toInt(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		return (int) Double.parseDouble(v.toString().trim());
	}

	private static int intOrZero(Object v) {
		if (v == null) {
			return 0;
		}
		try {
			return toInt(v);
		} catch (Exception e) {
			return 0;
		}
	}

	private static boolean numericNonZero(Object v) {
		if (v == null) {
			return false;
		}
		try {
			if (v instanceof Number n) {
				return n.doubleValue() != 0d;
			}
			return new BigDecimal(v.toString().trim()).compareTo(BigDecimal.ZERO) != 0;
		} catch (Exception e) {
			return false;
		}
	}

	private static Object toPointFilterValue(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return new BigDecimal(v.toString().trim()).intValue();
		} catch (Exception e) {
			return v;
		}
	}
}
