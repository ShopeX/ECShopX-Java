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

package cn.shopex.ecshopx.openapi.thirdapi.v2.member;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class OpenapiMemberV2BatchCreateParams {

	private static final ObjectMapper JSON = new ObjectMapper();

	private OpenapiMemberV2BatchCreateParams() {}

	public static boolean hasDataParameter(
			HttpServletRequest request, Map<String, Object> body, String dataQueryParam) {
		if (body != null && body.containsKey("data")) {
			return true;
		}
		return request.getParameterMap().containsKey("data");
	}

	public static List<Map<String, Object>> parseDataArray(String dataRaw) {
		if (!StringUtils.hasText(dataRaw)) {
			return List.of();
		}
		try {
			List<?> parsed = JSON.readValue(dataRaw, List.class);
			if (parsed == null) {
				return List.of();
			}
			List<Map<String, Object>> result = new ArrayList<>();
			for (Object item : parsed) {
				if (item instanceof Map<?, ?> map) {
					Map<String, Object> copy = new LinkedHashMap<>();
					for (Map.Entry<?, ?> entry : map.entrySet()) {
						if (entry.getKey() instanceof String key) {
							copy.put(key, entry.getValue());
						}
					}
					result.add(copy);
				}
			}
			return result;
		} catch (Exception e) {
			return List.of();
		}
	}
}
