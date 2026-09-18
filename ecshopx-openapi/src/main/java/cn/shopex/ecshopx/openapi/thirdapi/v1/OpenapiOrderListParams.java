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

public final class OpenapiOrderListParams {

	private OpenapiOrderListParams() {}

	public record PageSpec(int page, Integer pageSize, boolean pageOverridden) {}

	public static PageSpec resolve(String pageParam, String pageSizeParam, Map<String, Object> body) {
		int page = 1;
		Integer pageSize = 10;
		boolean pageOverridden = false;

		String mergedPage = OpenapiRequestParams.mergeString(pageParam, body, "page");
		if (OpenapiMemberQueryParams.isPhpTruthy(mergedPage)) {
			pageOverridden = true;
			page = parseInt(mergedPage, 1);

			if (!OpenapiMemberQueryParams.isParamPresent(pageSizeParam, body, "page_size")
					|| OpenapiMemberQueryParams.isPhpEmpty(pageSizeParam, body, "page_size")) {
				pageSize = null;
			} else {
				String mergedPageSize = OpenapiRequestParams.mergeString(pageSizeParam, body, "page_size");
				pageSize = parseIntNullable(mergedPageSize);
			}
		}
		return new PageSpec(page, pageSize, pageOverridden);
	}

	private static int parseInt(String raw, int defaultValue) {
		if (raw == null || raw.isBlank()) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private static Integer parseIntNullable(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
