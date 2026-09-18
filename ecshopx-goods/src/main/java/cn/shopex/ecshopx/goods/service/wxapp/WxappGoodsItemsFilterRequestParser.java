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

package cn.shopex.ecshopx.goods.service.wxapp;

import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WxappGoodsItemsFilterRequestParser {

	private static final Pattern FILTER_BRACKET_KEY = Pattern.compile("^filter\\[([^\\]]+)\\](?:\\[\\])?$");

	private final ObjectMapper objectMapper;

	public WxappGoodsItemsFilterRequestParser(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> parseFilter(HttpServletRequest request) {
		String single = request.getParameter("filter");
		if (StringUtils.hasText(single)) {
			String t = single.trim();
			if (t.startsWith("{")) {
				try {
					LinkedHashMap<String, Object> parsed =
							objectMapper.readValue(t, new TypeReference<LinkedHashMap<String, Object>>() {});
					return parsed != null ? parsed : new LinkedHashMap<>();
				} catch (JsonProcessingException ignored) {
					// fall through to bracket / flat parsing
				}
			}
		}
		Map<String, Object> flat = FlexibleHttpServletParameterMap.toObjectMap(request);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e : flat.entrySet()) {
			Matcher m = FILTER_BRACKET_KEY.matcher(e.getKey());
			if (!m.matches()) {
				continue;
			}
			String inner = m.group(1);
			putMerging(out, inner, e.getValue());
		}
		return out;
	}

	private static void putMerging(Map<String, Object> m, String key, Object incoming) {
		if (!m.containsKey(key)) {
			m.put(key, incoming);
			return;
		}
		m.put(key, mergeValueObjects(m.get(key), incoming));
	}

	private static Object mergeValueObjects(Object a, Object b) {
		List<Object> out = new ArrayList<>();
		appendAsListElements(a, out);
		appendAsListElements(b, out);
		return out;
	}

	private static void appendAsListElements(Object v, List<Object> out) {
		if (v instanceof List<?> list) {
			out.addAll(list);
		} else {
			out.add(v);
		}
	}
}
