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

package cn.shopex.ecshopx.tdkset.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.config.LangueProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TdkGivenSaveService {

	private final LangueProperties langueProperties;
	private final StringRedisTemplate redis;
	private final ObjectMapper objectMapper;

	public TdkGivenSaveService(
			LangueProperties langueProperties,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redis,
			ObjectMapper objectMapper) {
		this.langueProperties = langueProperties;
		this.redis = redis;
		this.objectMapper = objectMapper;
	}

	public void saveSet(String type, long companyId, Map<String, Object> reasonData, String countryCodeRaw) {
		assertLangueConfigured();
		String langTag = resolveLang(countryCodeRaw);
		Map<String, Object> outer = new LinkedHashMap<>();
		outer.put(langTag, reasonData);

		String json;
		try {
			json = objectMapper.writeValueAsString(outer);
		} catch (Exception e) {
			throw new ResourceException("保存失败");
		}

		String key = "TdkGiven_" + type + "_" + companyId;
		try {
			redis.opsForValue().set(key, json);
		} catch (RuntimeException e) {
			throw new ResourceException("保存失败");
		}
	}

	public Map<String, Object> getGivenSetInfo(String type, long companyId, String countryCodeRaw) {
		String key = "TdkGiven_" + type + "_" + companyId;
		String raw = redis.opsForValue().get(key);
		if (raw == null || raw.isEmpty() || "null".equals(raw)) {
			return emptyTdkGivenPayload();
		}
		Map<String, Object> outer;
		try {
			outer = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			return emptyTdkGivenPayload();
		}
		if (outer == null) {
			return emptyTdkGivenPayload();
		}
		assertLangueConfigured();
		String langTag = resolveLang(countryCodeRaw);
		Object slice = outer.get(langTag);
		if (!(slice instanceof Map<?, ?> sliceMapRaw)) {
			return emptyTdkGivenPayload();
		}
		Map<String, Object> sliceMap = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : sliceMapRaw.entrySet()) {
			sliceMap.put(String.valueOf(e.getKey()), e.getValue());
		}
		if (sliceMap.isEmpty()) {
			return emptyTdkGivenPayload();
		}
		return new LinkedHashMap<>(sliceMap);
	}

	/** Global GET: language key miss after Redis decode → empty list (多语言索引逻辑); given GET unchanged. */
	public Object getGlobalSetInfo(long companyId, String countryCodeRaw) {
		return loadGlobalTdkPayload(Long.toString(companyId), countryCodeRaw);
	}

	public Object getGlobalSetInfo(String companyKeySuffix, String countryCodeRaw) {
		return loadGlobalTdkPayload(companyKeySuffix, countryCodeRaw);
	}

	private Object loadGlobalTdkPayload(String companyKeySuffix, String countryCodeRaw) {
		String suffix = (companyKeySuffix == null) ? "" : companyKeySuffix;
		String key = "TdkGlobal_" + suffix;
		String raw = redis.opsForValue().get(key);
		if (raw == null || raw.isEmpty() || "null".equals(raw)) {
			return emptyTdkGivenPayload();
		}
		Map<String, Object> outer;
		try {
			outer = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			return emptyTdkGivenPayload();
		}
		if (outer == null) {
			return emptyTdkGivenPayload();
		}
		assertLangueConfigured();
		String langTag = resolveLang(countryCodeRaw);
		Object slice = outer.get(langTag);
		if (!(slice instanceof Map<?, ?> sliceMapRaw)) {
			return Collections.emptyList();
		}
		Map<String, Object> sliceMap = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : sliceMapRaw.entrySet()) {
			sliceMap.put(String.valueOf(e.getKey()), e.getValue());
		}
		if (sliceMap.isEmpty()) {
			return Collections.emptyList();
		}
		return new LinkedHashMap<>(sliceMap);
	}

	public void saveGlobalSet(long companyId, Map<String, Object> reasonData, String countryCodeRaw) {
		assertLangueConfigured();
		String langTag = resolveLang(countryCodeRaw);
		Map<String, Object> outer = new LinkedHashMap<>();
		outer.put(langTag, reasonData);

		String json;
		try {
			json = objectMapper.writeValueAsString(outer);
		} catch (Exception e) {
			throw new ResourceException("保存失败");
		}

		String key = "TdkGlobal_" + companyId;
		try {
			redis.opsForValue().set(key, json);
		} catch (RuntimeException e) {
			throw new ResourceException("保存失败");
		}
	}

	private void assertLangueConfigured() {
		List<String> list = langueProperties.getList();
		if (list == null || list.isEmpty()) {
			throw new ResourceException("不存在的多语言配置,请检查 application.yml 中 langue.list 配置");
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

	private static Map<String, Object> emptyTdkGivenPayload() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("title", "");
		m.put("mate_description", "");
		m.put("mate_keywords", "");
		return m;
	}
}
