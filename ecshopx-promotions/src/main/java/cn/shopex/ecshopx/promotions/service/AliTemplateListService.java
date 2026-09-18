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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.ali.service.alitemplate.AliOpenTemplateLibraryRedisAccessor;
import cn.shopex.ecshopx.promotions.service.alitemplate.AliTemplateSceneDefinitions;
import cn.shopex.ecshopx.promotions.service.alitemplate.AliTemplateSceneDefinitions.AliTemplateSceneDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AliTemplateListService {

	private final AliOpenTemplateLibraryRedisAccessor aliOpenTemplateLibraryRedisAccessor;

	public AliTemplateListService(AliOpenTemplateLibraryRedisAccessor aliOpenTemplateLibraryRedisAccessor) {
		this.aliOpenTemplateLibraryRedisAccessor = aliOpenTemplateLibraryRedisAccessor;
	}

	public Map<String, Object> getAliTemplateList(long companyId) {
		LinkedHashMap<String, Object> list = new LinkedHashMap<>();
		for (Map.Entry<String, AliTemplateSceneDefinition> e : AliTemplateSceneDefinitions.orderedEntries()) {
			String scenesName = e.getKey();
			AliTemplateSceneDefinition def = e.getValue();
			Optional<Map<String, Object>> redisOpt =
					aliOpenTemplateLibraryRedisAccessor.getTemplate((int) companyId, scenesName);
			Map<String, Object> map = redisOpt.orElseGet(Collections::emptyMap);
			String templateId = templateIdFromRedisMap(map);
			long sendTime = sendTimeMinutesFromRedisMap(map);
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("template_id", templateId);
			row.put("company_id", companyId);
			row.put("notice_type", "alipay");
			row.put("tmpl_type", def.getTmplType());
			row.put("title", def.getTitle());
			row.put("scenes_name", scenesName);
			row.put("scene_desc", def.getSceneDesc());
			row.put("content", deepCopyContent(def.getValue()));
			row.put("is_open", StringUtils.hasText(templateId));
			Map<String, Object> sendTimeDesc = deepCopySendTimeDesc(def.getSendTimeDesc());
			if (redisOpt.isPresent() && sendTime > 0L) {
				sendTimeDesc.put("value", sendTime);
			}
			row.put("send_time_desc", sendTimeDesc);
			list.put(scenesName, row);
		}
		return Map.of("list", list);
	}

	private static String templateIdFromRedisMap(Map<String, Object> map) {
		return Optional.ofNullable(map.get("template_id"))
				.map(Object::toString)
				.map(String::trim)
				.orElse("");
	}

	private static long sendTimeMinutesFromRedisMap(Map<String, Object> map) {
		Object v = map.get("send_time");
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException ex) {
			return 0L;
		}
	}

	private static List<Map<String, Object>> deepCopyContent(List<Map<String, String>> value) {
		List<Map<String, Object>> out = new ArrayList<>(value.size());
		for (Map<String, String> line : value) {
			LinkedHashMap<String, Object> m = new LinkedHashMap<>();
			for (Map.Entry<String, String> e : line.entrySet()) {
				m.put(e.getKey(), e.getValue());
			}
			out.add(m);
		}
		return out;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> deepCopySendTimeDesc(Map<String, Object> src) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e : src.entrySet()) {
			String k = e.getKey();
			Object v = e.getValue();
			if ("time_list".equals(k) && v instanceof List<?> rawList) {
				List<Integer> nums = new ArrayList<>(rawList.size());
				for (Object o : rawList) {
					if (o instanceof Number n) {
						nums.add(n.intValue());
					}
				}
				out.put(k, nums);
			} else if (v instanceof Map<?, ?> nested) {
				out.put(k, deepCopySendTimeDesc((Map<String, Object>) nested));
			} else {
				out.put(k, v);
			}
		}
		return out;
	}
}
