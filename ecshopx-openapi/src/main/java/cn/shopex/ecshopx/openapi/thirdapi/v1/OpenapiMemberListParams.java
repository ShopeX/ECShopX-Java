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

package cn.shopex.ecshopx.openapi.thirdapi.v1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OpenapiMemberListParams {

	private OpenapiMemberListParams() {}

	public static String resolveScope(String scopeParam, Map<String, Object> body) {
		String merged = OpenapiRequestParams.mergeString(scopeParam, body, "scope");
		return merged != null ? merged : "fp";
	}

	public static List<Long> resolveTagIds(List<String> tagIdQueryList, Map<String, Object> body) {
		if (body != null && body.containsKey("tag_id")) {
			return parseTagIdList(body.get("tag_id"));
		}
		if (tagIdQueryList != null && !tagIdQueryList.isEmpty()) {
			List<Long> ids = new ArrayList<>();
			for (String raw : tagIdQueryList) {
				Long id = parseLongOrNull(raw);
				if (id != null) {
					ids.add(id);
				}
			}
			return ids.isEmpty() ? null : ids;
		}
		return null;
	}

	public static boolean isPhpEmpty(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			return t.isEmpty() || "0".equals(t);
		}
		if (raw instanceof Number n) {
			return n.longValue() == 0L;
		}
		if (raw instanceof Boolean b) {
			return !b;
		}
		return false;
	}

	public static Map<String, Object> buildMergedParams(
			Map<String, Object> body,
			List<String> tagIdQueryList,
			String scopeParam,
			String pageParam,
			String pageSizeParam,
			String birthdayStartParam,
			String birthdayEndParam,
			String typeParam,
			String salespersonCodeParam,
			String storeBnParam,
			String pointStartParam,
			String pointEndParam,
			String gradeIdParam,
			String buyStartParam,
			String buyEndParam,
			String keywordParam) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		putIfPresent(merged, "scope", scopeParam, body, "scope");
		putIfPresent(merged, "page", pageParam, body, "page");
		putIfPresent(merged, "page_size", pageSizeParam, body, "page_size");
		putIfPresent(merged, "birthday_start", birthdayStartParam, body, "birthday_start");
		putIfPresent(merged, "birthday_end", birthdayEndParam, body, "birthday_end");
		putIfPresent(merged, "type", typeParam, body, "type");
		putIfPresent(merged, "salesperson_code", salespersonCodeParam, body, "salesperson_code");
		putIfPresent(merged, "store_bn", storeBnParam, body, "store_bn");
		putIfPresent(merged, "point_start", pointStartParam, body, "point_start");
		putIfPresent(merged, "point_end", pointEndParam, body, "point_end");
		putIfPresent(merged, "grade_id", gradeIdParam, body, "grade_id");
		putIfPresent(merged, "buy_start", buyStartParam, body, "buy_start");
		putIfPresent(merged, "buy_end", buyEndParam, body, "buy_end");
		putIfPresent(merged, "keyword", keywordParam, body, "keyword");
		List<Long> tagIds = resolveTagIds(tagIdQueryList, body);
		if (tagIds != null) {
			merged.put("tag_id", tagIds);
		}
		return merged;
	}

	private static void putIfPresent(
			Map<String, Object> merged,
			String key,
			String queryParam,
			Map<String, Object> body,
			String bodyKey) {
		if (body != null && body.containsKey(bodyKey)) {
			merged.put(key, body.get(bodyKey));
		} else if (queryParam != null) {
			merged.put(key, queryParam);
		}
	}

	private static List<Long> parseTagIdList(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Collection<?> coll) {
			List<Long> ids = new ArrayList<>();
			for (Object item : coll) {
				Long id = parseLongOrNull(item);
				if (id != null) {
					ids.add(id);
				}
			}
			return ids;
		}
		Long single = parseLongOrNull(raw);
		if (single != null) {
			return List.of(single);
		}
		return null;
	}

	private static Long parseLongOrNull(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
