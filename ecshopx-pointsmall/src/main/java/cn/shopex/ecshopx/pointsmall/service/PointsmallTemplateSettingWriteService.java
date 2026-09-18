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

package cn.shopex.ecshopx.pointsmall.service;

import cn.shopex.ecshopx.pointsmall.validation.PointsmallTemplateSettingValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class PointsmallTemplateSettingWriteService {

	private static final String REDIS_HASH_KEY = "pointsmall_template_setting";

	private static final String HSET_LUA = "return redis.call('HSET', KEYS[1], ARGV[1], ARGV[2])";

	private static final DefaultRedisScript<Long> HSET_STATUS_SCRIPT = new DefaultRedisScript<>();

	static {
		HSET_STATUS_SCRIPT.setScriptText(HSET_LUA);
		HSET_STATUS_SCRIPT.setResultType(Long.class);
	}

	private final StringRedisTemplate redis;
	private final ObjectMapper objectMapper;
	private final PointsmallTemplateSettingValidator templateSettingValidator;

	public PointsmallTemplateSettingWriteService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redis,
			ObjectMapper objectMapper,
			PointsmallTemplateSettingValidator templateSettingValidator) {
		this.redis = redis;
		this.objectMapper = objectMapper;
		this.templateSettingValidator = templateSettingValidator;
	}

	public long saveTemplateSetting(long companyId, Map<String, Object> rawParams) {
		LinkedHashMap<String, Object> data = formatTemplateData(rawParams);
		templateSettingValidator.validate(data);
		final String jsonString;
		try {
			jsonString = objectMapper.writeValueAsString(data);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Failed to serialize pointsmall template setting", e);
		}
		Long raw = redis.execute(
				HSET_STATUS_SCRIPT,
				Collections.singletonList(REDIS_HASH_KEY),
				String.valueOf(companyId),
				jsonString);
		if (raw == null) {
			throw new IllegalStateException("Redis HSET returned null for pointsmall_template_setting");
		}
		return raw.longValue();
	}

	private LinkedHashMap<String, Object> formatTemplateData(Map<String, Object> raw) {
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("pc_banner", normalizePcBanner(raw.get("pc_banner")));

		LinkedHashMap<String, Object> screenMap = copyScreenMap(raw.get("screen"));
		for (String key : new ArrayList<>(screenMap.keySet())) {
			if ("brand_openstatus".equals(key)
					|| "cat_openstatus".equals(key)
					|| "point_openstatus".equals(key)) {
				Object val = screenMap.get(key);
				screenMap.put(key, Objects.toString(val, "").equals("true"));
			}
		}
		screenMap.put("point_section", normalizePointSection(screenMap.get("point_section")));
		data.put("screen", screenMap);
		return data;
	}

	private static List<Object> normalizePcBanner(Object v) {
		if (v == null) {
			return new ArrayList<>();
		}
		if (v instanceof Collection<?> c) {
			return new ArrayList<>(c);
		}
		return new ArrayList<>();
	}

	private static LinkedHashMap<String, Object> copyScreenMap(Object s) {
		LinkedHashMap<String, Object> screenMap = new LinkedHashMap<>();
		if (!(s instanceof Map<?, ?> rawMap)) {
			return screenMap;
		}
		for (Map.Entry<?, ?> e : rawMap.entrySet()) {
			if (e.getKey() instanceof String k) {
				screenMap.put(k, e.getValue());
			}
		}
		return screenMap;
	}

	private static List<Object> normalizePointSection(Object ps) {
		if (ps == null) {
			return new ArrayList<>();
		}
		if (!(ps instanceof Collection<?> col)) {
			return new ArrayList<>();
		}
		List<Object> sections = new ArrayList<>();
		for (Object item : col) {
			if (item instanceof List<?> l) {
				if (l.size() >= 2) {
					List<Object> pair = new ArrayList<>(2);
					pair.add(l.get(0));
					pair.add(l.get(1));
					sections.add(pair);
				} else {
					sections.add(item);
				}
			} else if (item instanceof Map<?, ?> m) {
				if (mapBothSlotsKeyed(m)) {
					List<Object> pair = new ArrayList<>(2);
					pair.add(mapSlotGet(m, 0));
					pair.add(mapSlotGet(m, 1));
					sections.add(pair);
				} else {
					sections.add(m);
				}
			} else {
				sections.add(item);
			}
		}
		return sections;
	}

	private static boolean mapBothSlotsKeyed(Map<?, ?> m) {
		return mapHasSlotKey(m, 0) && mapHasSlotKey(m, 1);
	}

	private static boolean mapHasSlotKey(Map<?, ?> m, int index) {
		return m.containsKey(index) || m.containsKey(String.valueOf(index));
	}

	private static Object mapSlotGet(Map<?, ?> m, int index) {
		if (m.containsKey(index)) {
			return m.get(index);
		}
		if (m.containsKey(String.valueOf(index))) {
			return m.get(String.valueOf(index));
		}
		return null;
	}
}
