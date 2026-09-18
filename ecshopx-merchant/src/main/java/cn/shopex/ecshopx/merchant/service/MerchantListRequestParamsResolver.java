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

package cn.shopex.ecshopx.merchant.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MerchantListRequestParamsResolver {

	private final ObjectMapper objectMapper;

	public MerchantListRequestParamsResolver(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * Operator 列表接口参数解析：若存在非空 {@code params} 查询参数，则仅将该 JSON 对象解析为参数 Map（并移除内层
	 * {@code params} 键）；否则从扁平查询参数收集分页（{@code page}、{@code pageSize}、{@code page_size}）与筛选项，
	 * 不与 JSON {@code params} 内层字段合并。
	 */
	public Map<String, Object> resolveToMapForOperator(HttpServletRequest request) throws IOException {
		String rawParams = request.getParameter("params");
		if (rawParams != null && !rawParams.isBlank()) {
			Map<String, Object> fromJson = parseParamsJsonObjectToMap(rawParams);
			fromJson.remove("params");
			return fromJson;
		}
		return resolveOperatorFlatParameterMap(request);
	}

	public Map<String, Object> resolveToMap(HttpServletRequest request) throws IOException {
		String rawParams = request.getParameter("params");
		if (rawParams != null && !rawParams.isBlank()) {
			if (onlySingleParamsQueryKey(request)) {
				Map<String, Object> out = new LinkedHashMap<>();
				out.put("params", rawParams);
				return out;
			}
			Map<String, Object> fromJson = parseParamsJsonObjectToMap(rawParams);
			Map<String, Object> flat = resolveFromFlatParameterMap(request);
			LinkedHashMap<String, Object> merged = new LinkedHashMap<>(fromJson);
			for (Map.Entry<String, Object> e : flat.entrySet()) {
				merged.put(e.getKey(), e.getValue());
			}
			merged.remove("params");
			normalizeTimeStartInMap(merged);
			return merged;
		}
		return resolveFromFlatParameterMap(request);
	}

	/**
	 * {@code true} when the servlet parameter map contains no keys other than {@code params}
	 * (no {@code page}, {@code pageSize}, filters, etc.). In that case the resolver returns a
	 * map with only {@code params} set to the raw query string so downstream validation does not
	 * see inner JSON fields promoted to the top level.
	 */
	private static boolean onlySingleParamsQueryKey(HttpServletRequest request) {
		Map<String, String[]> pm = request.getParameterMap();
		return pm.size() == 1 && pm.containsKey("params");
	}

	private Map<String, Object> parseParamsJsonObjectToMap(String raw) throws IOException {
		try {
			JsonNode root = objectMapper.readTree(raw);
			if (root.isTextual()) {
				root = objectMapper.readTree(root.asText());
			}
			if (!root.isObject()) {
				throw new ResourceException("请求体须为JSON对象");
			}
			ObjectNode obj = (ObjectNode) root;
			Map<String, Object> map =
					objectMapper.convertValue(obj, new TypeReference<LinkedHashMap<String, Object>>() {});
			normalizeTimeStartInMap(map);
			return map;
		} catch (ResourceException e) {
			throw e;
		} catch (JsonProcessingException e) {
			throw new ResourceException("请求体JSON格式错误");
		}
	}

	private void normalizeTimeStartInMap(Map<String, Object> map) {
		Object ts = map.get("time_start");
		if (ts == null) {
			return;
		}
		if (ts instanceof List<?>) {
			return;
		}
		if (ts instanceof String s) {
			if (s.isBlank()) {
				map.remove("time_start");
				return;
			}
			map.put("time_start", new ArrayList<>(List.of(s.trim())));
			return;
		}
		map.put("time_start", new ArrayList<>(List.of(String.valueOf(ts))));
	}

	private Map<String, Object> resolveOperatorFlatParameterMap(HttpServletRequest request) {
		Map<String, Object> out = new LinkedHashMap<>();
		putSingleOrList(out, "page", request.getParameterValues("page"));
		putSingleOrList(out, "pageSize", request.getParameterValues("pageSize"));
		putSingleOrList(out, "page_size", request.getParameterValues("page_size"));
		putSingleOrList(out, "merchant_name", request.getParameterValues("merchant_name"));
		putSingleOrList(out, "mobile", request.getParameterValues("mobile"));
		putSingleOrList(out, "country_code", request.getParameterValues("country_code"));
		return out;
	}

	private Map<String, Object> resolveFromFlatParameterMap(HttpServletRequest request) {
		Map<String, Object> out = new LinkedHashMap<>();
		putSingleOrList(out, "page", request.getParameterValues("page"));
		putSingleOrList(out, "pageSize", request.getParameterValues("pageSize"));
		putSingleOrList(out, "merchant_name", request.getParameterValues("merchant_name"));
		putSingleOrList(out, "legal_name", request.getParameterValues("legal_name"));
		putSingleOrList(out, "legal_mobile", request.getParameterValues("legal_mobile"));
		putSingleOrList(out, "country_code", request.getParameterValues("country_code"));
		String[] ts = request.getParameterValues("time_start");
		if (ts == null || ts.length == 0) {
			ts = request.getParameterValues("time_start[]");
		}
		if (ts != null && ts.length > 0) {
			out.put("time_start", new ArrayList<>(Arrays.asList(ts)));
		}
		return out;
	}

	private static void putSingleOrList(Map<String, Object> out, String key, String[] vals) {
		if (vals == null || vals.length == 0) {
			return;
		}
		if (vals.length == 1) {
			out.put(key, vals[0]);
		} else {
			out.put(key, new ArrayList<>(Arrays.asList(vals)));
		}
	}
}
