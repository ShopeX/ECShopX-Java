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

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class OpenapiPaginationParams {

	private OpenapiPaginationParams() {}

	public static int resolvePage(String pageParam, Map<String, Object> body) {
		Object raw = body != null ? body.get("page") : null;
		if (raw == null && pageParam != null && !pageParam.isBlank()) {
			raw = pageParam.trim();
		}
		if (raw == null) {
			throw new ResourceException("page参数必填");
		}
		return parsePositiveInt(raw, "page参数必填");
	}

	public static int resolvePageSize(String pageSizeParam, Map<String, Object> body) {
		Object raw = body != null ? body.get("page_size") : null;
		if (raw == null && pageSizeParam != null && !pageSizeParam.isBlank()) {
			raw = pageSizeParam.trim();
		}
		if (raw == null) {
			throw new ResourceException("page_size参数必填");
		}
		return parseInt(raw, "page_size参数必填");
	}

	public static String mergeCountryCode(String countryCodeParam, Map<String, Object> body) {
		if (body != null && body.containsKey("country_code")) {
			Object raw = body.get("country_code");
			if (raw != null) {
				String t = String.valueOf(raw).trim();
				if (StringUtils.hasText(t)) {
					return t;
				}
			}
			return null;
		}
		if (countryCodeParam != null) {
			String t = countryCodeParam.trim();
			if (StringUtils.hasText(t)) {
				return t;
			}
		}
		return null;
	}

	private static int parsePositiveInt(Object raw, String message) {
		int value = parseInt(raw, message);
		if (value < 1) {
			throw new ResourceException(message);
		}
		return value;
	}

	private static int parseInt(Object raw, String message) {
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			throw new ResourceException(message);
		}
	}
}
