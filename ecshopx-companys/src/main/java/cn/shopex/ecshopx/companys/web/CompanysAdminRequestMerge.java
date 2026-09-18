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

package cn.shopex.ecshopx.companys.web;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * Merges servlet query parameters with JSON body for admin APIs (query + JSON combined resolution).
 */
public final class CompanysAdminRequestMerge {

	private CompanysAdminRequestMerge() {}

	public static Map<String, Object> mergeInputLikeFlexibleResolver(
			HttpServletRequest request, Map<String, Object> body) {
		LinkedHashMap<String, Object> input = new LinkedHashMap<>(parameterMapToMapLikeResolver(request));
		if (body != null) {
			input.putAll(body);
		}
		return input;
	}

	private static Map<String, Object> parameterMapToMapLikeResolver(HttpServletRequest request) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		request.getParameterMap()
				.forEach(
						(k, v) -> {
							if (v != null && v.length > 0 && StringUtils.hasText(v[0])) {
								m.put(k, v[0]);
							}
						});
		return m;
	}

	/**
	 * Parses the raw JSON body for the WeChat shops settings update API. A root JSON object becomes a key-value map
	 * (insertion order preserved). Any non-object root (including arrays and primitives) or blank input yields an
	 * empty map, so only query parameters contribute body-like keys when the payload is not an object.
	 */
	public static Map<String, Object> parseWxShopsSettingJsonBodyToBodyMap(ObjectMapper objectMapper, String rawJsonBody) {
		if (rawJsonBody == null || rawJsonBody.isBlank()) {
			return new LinkedHashMap<>();
		}
		final JsonNode root;
		try {
			root = objectMapper.readTree(rawJsonBody);
		} catch (JsonProcessingException ex) {
			throw new BadRequestException("请求体格式错误");
		}
		if (root != null && root.isObject()) {
			return objectMapper.convertValue(
					root, new TypeReference<LinkedHashMap<String, Object>>() {});
		}
		return new LinkedHashMap<>();
	}

	/**
	 * Normalizes merged request input (query plus body) into the outbound map for WeChat shops settings: always
	 * includes {@code country_code}, {@code logo}, {@code intro}, {@code brand_name}, and {@code background}.
	 * If {@code logo}, {@code intro}, or {@code brand_name} are absent from {@code merged}, their values are
	 * {@code null}; if {@code background} is absent, it defaults to an empty string.
	 */
	public static Map<String, Object> buildWxShopsSettingNormalizedMerged(Map<String, Object> merged) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("country_code", merged.get("country_code"));
		out.put("logo", merged.containsKey("logo") ? merged.get("logo") : null);
		out.put("intro", merged.containsKey("intro") ? merged.get("intro") : null);
		out.put("brand_name", merged.containsKey("brand_name") ? merged.get("brand_name") : null);
		out.put("background", merged.containsKey("background") ? merged.get("background") : "");
		return out;
	}
}
