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

package cn.shopex.ecshopx.wechat.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.config.WechatLangueProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class WxappShareSettingService {

	private static final String REDIS_KEY_PREFIX = "shareSetting:";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final WechatLangueProperties langueProperties;

	public WxappShareSettingService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			WechatLangueProperties langueProperties) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.langueProperties = langueProperties;
	}

	public Map<String, Object> getFrontShareScene(long companyId, String countryCodeRaw, String shareIndex) {
		assertLangueConfigured();
		String raw = companysRedisTemplate.opsForValue().get(REDIS_KEY_PREFIX + companyId);
		Map<String, Object> rootMap = parseRootMap(raw);
		String langTag = resolveLang(countryCodeRaw);
		Map<String, Object> langSlice = extractLangSlice(rootMap, langTag);
		String key = shareIndex != null ? shareIndex : "index";
		Object sceneObj = langSlice.get(key);
		if (sceneObj instanceof Map<?, ?> sm) {
			Map<String, Object> out = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : sm.entrySet()) {
				if (e.getKey() != null) {
					out.put(e.getKey().toString(), e.getValue());
				}
			}
			return out;
		}
		Map<String, Object> empty = new LinkedHashMap<>();
		empty.put("title", "");
		empty.put("desc", "");
		empty.put("imageUrl", "");
		return empty;
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
}
