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

package cn.shopex.ecshopx.companys.service.setting;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.config.LangueProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ShareSettingRedisService {

	private static final Logger log = LoggerFactory.getLogger(ShareSettingRedisService.class);

	private static final String REDIS_KEY_PREFIX = "shareSetting:";

	private static final String[] SCENE_KEYS = {
		"index", "planting", "itemlist", "group", "seckill", "coupon"
	};

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final LangueProperties langueProperties;

	public ShareSettingRedisService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			LangueProperties langueProperties) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.langueProperties = langueProperties;
	}

	public void saveSingleLangWholeKeyReplace(
			long companyId, Map<String, Object> nestedScenes, String countryCodeRaw) {
		assertLangueConfigured();
		String lang = resolveLang(countryCodeRaw);
		Map<String, Object> scenes = nestedScenes != null ? nestedScenes : new LinkedHashMap<>();
		Map<String, Object> root = new LinkedHashMap<>(1);
		root.put(lang, scenes);
		try {
			String json = objectMapper.writeValueAsString(root);
			companysRedisTemplate.opsForValue().set(REDIS_KEY_PREFIX + companyId, json);
		} catch (JsonProcessingException e) {
			log.error("Failed to serialize share setting for companyId={}", companyId, e);
		} catch (DataAccessException e) {
			log.error("Failed to write share setting to Redis for companyId={}", companyId, e);
		}
	}

	public Map<String, Object> getEffective(long companyId, String countryCodeRaw) {
		assertLangueConfigured();
		String raw = companysRedisTemplate.opsForValue().get(REDIS_KEY_PREFIX + companyId);
		Map<String, Object> rootMap = parseRootMap(raw);
		String langTag = resolveLang(countryCodeRaw);
		Map<String, Object> langSlice = extractLangSlice(rootMap, langTag);
		Map<String, Object> defaultData = buildDefaultData();
		Map<String, Object> result = new LinkedHashMap<>(defaultData);
		for (Map.Entry<String, Object> e : langSlice.entrySet()) {
			result.put(e.getKey(), e.getValue());
		}
		return result;
	}

	private void assertLangueConfigured() {
		List<String> list = langueProperties.getList();
		if (list == null || list.isEmpty()) {
			throw new ResourceException("不存在的多语言配置,请检查 application.yml 中 langue.list 配置");
		}
	}

	private Map<String, Object> parseRootMap(String raw) {
		if (raw == null || raw.isBlank()) {
			return new LinkedHashMap<>();
		}
		try {
			Map<String, Object> root =
					objectMapper.readValue(raw.trim(), new TypeReference<Map<String, Object>>() {});
			return root != null ? root : new LinkedHashMap<>();
		} catch (Exception e) {
			return new LinkedHashMap<>();
		}
	}

	private static String resolveLang(String countryCodeRaw) {
		if (countryCodeRaw == null) {
			return "zh-CN";
		}
		String t = countryCodeRaw.trim();
		if (t.isEmpty()) {
			return "zh-CN";
		}
		return t;
	}

	private static Map<String, Object> extractLangSlice(Map<String, Object> rootMap, String langTag) {
		Object slice = rootMap.get(langTag);
		if (!(slice instanceof Map<?, ?> sm)) {
			return new LinkedHashMap<>();
		}
		Map<String, Object> langSlice = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : sm.entrySet()) {
			if (e.getKey() != null) {
				langSlice.put(e.getKey().toString(), e.getValue());
			}
		}
		return langSlice;
	}

	private static Map<String, Object> buildDefaultData() {
		Map<String, Object> m = new LinkedHashMap<>();
		for (String k : SCENE_KEYS) {
			Map<String, Object> inner = new LinkedHashMap<>();
			inner.put("title", "");
			inner.put("desc", "");
			inner.put("imageUrl", "");
			m.put(k, inner);
		}
		return m;
	}
}
