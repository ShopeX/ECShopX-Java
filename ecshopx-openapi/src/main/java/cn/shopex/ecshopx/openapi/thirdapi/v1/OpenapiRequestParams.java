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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import org.springframework.util.StringUtils;

public final class OpenapiRequestParams {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private OpenapiRequestParams() {}

	public static String mergeRequiredString(
			String queryParam, Map<String, Object> body, String key, String requiredMessage) {
		String result = mergeString(queryParam, body, key);
		if (!StringUtils.hasText(result)) {
			throw new ResourceException(requiredMessage);
		}
		return result;
	}

	/**
	 * 请求参数原始字符串值（不 trim）。Body 含键时用 body 值；否则用 queryParam。
	 */
	public static String originalString(String queryParam, Map<String, Object> body, String key) {
		if (body != null && body.containsKey(key)) {
			Object raw = body.get(key);
			if (raw == null) {
				return null;
			}
			if (raw instanceof String s) {
				return s;
			}
			return String.valueOf(raw);
		}
		return queryParam;
	}

	/**
	 * 可选参数「键是否存在」语义：Body 含 key → present；否则 Query 非 null → present；否则 empty。
	 */
	public static Optional<String> presentOptionalString(
			String queryParam, Map<String, Object> body, String key) {
		if (body != null && body.containsKey(key)) {
			return Optional.of(originalString(queryParam, body, key));
		}
		if (queryParam != null) {
			return Optional.of(queryParam);
		}
		return Optional.empty();
	}

	public static String mergeString(String queryParam, Map<String, Object> body, String key) {
		Object raw = null;
		if (body != null && body.containsKey(key)) {
			raw = body.get(key);
		} else if (queryParam != null && !queryParam.isBlank()) {
			raw = queryParam.trim();
		}
		return convertToString(raw);
	}

	private static String convertToString(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			return t.isEmpty() ? null : t;
		}
		if (raw instanceof Number || raw instanceof Boolean) {
			return String.valueOf(raw);
		}
		if (raw instanceof Map<?, ?> || raw instanceof Collection<?>) {
			try {
				return OBJECT_MAPPER.writeValueAsString(raw);
			} catch (JsonProcessingException e) {
				return null;
			}
		}
		String t = String.valueOf(raw).trim();
		return t.isEmpty() ? null : t;
	}
}
