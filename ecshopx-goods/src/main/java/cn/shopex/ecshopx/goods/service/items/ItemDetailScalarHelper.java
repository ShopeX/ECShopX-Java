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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ItemDetailScalarHelper {

	public void normalizeDetailScalars(Map<String, Object> detail) {
		detail.put("nospec", normalizeNospec(detail.get("nospec")));
		splitCsvToList(detail, "regions_id");
		splitCsvToList(detail, "regions");
		String it = str(detail.get("item_type"));
		if (!StringUtils.hasText(it)) {
			detail.put("item_type", "services");
		}
	}

	public boolean isMultiSpec(Object nospecNormalized) {
		if (nospecNormalized instanceof Boolean b) {
			return !b;
		}
		return false;
	}

	public void applyFallbackItemParams(Map<String, Object> detail) {
		List<Map<String, Object>> itemParams = new java.util.ArrayList<>();
		if (StringUtils.hasText(str(detail.get("goods_brand"))) && !detail.containsKey("brand_id")) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("attribute_name", "品牌");
			m.put("attribute_value_name", detail.get("goods_brand"));
			itemParams.add(m);
		}
		addSimpleParam(itemParams, detail, "goods_color", "颜色");
		addSimpleParam(itemParams, detail, "goods_function", "功能");
		addSimpleParam(itemParams, detail, "goods_series", "系列");
		detail.put("item_params", itemParams);
	}

	private static void addSimpleParam(List<Map<String, Object>> itemParams, Map<String, Object> detail, String key, String label) {
		if (StringUtils.hasText(str(detail.get(key)))) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("attribute_name", label);
			m.put("attribute_value_name", detail.get(key));
			itemParams.add(m);
		}
	}

	private static boolean normalizeNospec(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		String s = raw.toString().trim();
		return "true".equalsIgnoreCase(s) || "1".equals(s);
	}

	private static void splitCsvToList(Map<String, Object> detail, String key) {
		Object v = detail.get(key);
		if (v == null) {
			return;
		}
		if (v instanceof List<?> list) {
			detail.put(key, normalizeRegionList(list));
			return;
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s)) {
			return;
		}
		detail.put(key, splitCsvString(s));
	}

	private static List<String> splitCsvString(String raw) {
		String s = raw.trim();
		if (s.startsWith("[") && s.endsWith("]")) {
			s = s.substring(1, s.length() - 1);
		}
		return Arrays.stream(s.split(","))
				.map(String::trim)
				.map(ItemDetailScalarHelper::stripBrackets)
				.filter(StringUtils::hasText)
				.collect(Collectors.toList());
	}

	private static List<String> normalizeRegionList(List<?> list) {
		if (list.isEmpty()) {
			return List.of();
		}
		if (list.size() == 1) {
			Object only = list.get(0);
			if (only != null) {
				String s = only.toString().trim();
				if (s.startsWith("[") && s.contains(",")) {
					return splitCsvString(s);
				}
			}
		}
		boolean needsFix = list.stream().anyMatch(o -> {
			if (o == null) {
				return false;
			}
			String s = o.toString().trim();
			return s.startsWith("[") || s.endsWith("]");
		});
		if (!needsFix) {
			return list.stream()
					.map(o -> o == null ? "" : o.toString().trim())
					.filter(StringUtils::hasText)
					.collect(Collectors.toList());
		}
		return list.stream()
				.map(o -> o == null ? "" : stripBrackets(o.toString().trim()))
				.filter(StringUtils::hasText)
				.collect(Collectors.toList());
	}

	private static String stripBrackets(String s) {
		if (s.startsWith("[")) {
			s = s.substring(1);
		}
		if (s.endsWith("]")) {
			s = s.substring(0, s.length() - 1);
		}
		return s.trim();
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}
}
