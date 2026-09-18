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

import java.util.Map;

public final class OpenapiMemberBrowseHistoryParams {

	private OpenapiMemberBrowseHistoryParams() {}

	public record BrowseHistoryPagination(int page, Integer pageSize) {}

	public static BrowseHistoryPagination resolveBrowseHistoryPagination(
			String pageParam, String pageSizeParam, Map<String, Object> body) {
		String pageRaw = OpenapiRequestParams.mergeString(pageParam, body, "page");
		if (!OpenapiMemberQueryParams.isPhpTruthy(pageRaw)) {
			return new BrowseHistoryPagination(1, 10);
		}
		int page = resolvePositiveInt(pageRaw, 1);
		Integer pageSize = null;
		if (OpenapiMemberQueryParams.isParamPresent(pageSizeParam, body, "page_size")) {
			pageSize = parseIntegerOrNull(pageSizeParam, body, "page_size");
		}
		return new BrowseHistoryPagination(page, pageSize);
	}

	private static int resolvePositiveInt(String raw, int defaultValue) {
		Integer v = parseIntegerOrNull(raw);
		return v != null && v >= 1 ? v : defaultValue;
	}

	private static Integer parseIntegerOrNull(String queryParam, Map<String, Object> body, String key) {
		Object raw = null;
		if (body != null && body.containsKey(key)) {
			raw = body.get(key);
		} else if (queryParam != null) {
			raw = queryParam;
		}
		return parseIntegerOrNull(raw);
	}

	private static Integer parseIntegerOrNull(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
