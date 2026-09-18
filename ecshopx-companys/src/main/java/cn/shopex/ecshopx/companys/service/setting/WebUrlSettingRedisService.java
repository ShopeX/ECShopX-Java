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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Redis key {@code webUrlSetting:{companyId}}: JSON object with four fixed keys or other JSON as
 * stored. Whole key replaced on write; no TTL.
 */
@Service
public class WebUrlSettingRedisService {

	private static final Logger log = LoggerFactory.getLogger(WebUrlSettingRedisService.class);

	private static final String KEY_PREFIX = "webUrlSetting:";
	private static final String[] FOUR_KEYS = {"mycoach", "aftersales", "classhour", "arranged"};

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final H5WebUrlMobileEncryptionService h5WebUrlMobileEncryptionService;

	public WebUrlSettingRedisService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			H5WebUrlMobileEncryptionService h5WebUrlMobileEncryptionService) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.h5WebUrlMobileEncryptionService = h5WebUrlMobileEncryptionService;
	}

	private static String key(long companyId) {
		return KEY_PREFIX + companyId;
	}

	/**
	 * Builds the four-key payload: each key is always present; missing keys in {@code merged} map
	 * to {@code null}.
	 */
	public Map<String, Object> buildFourKeysFromMerged(Map<String, Object> merged) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>(4);
		for (String k : FOUR_KEYS) {
			out.put(k, merged != null && merged.containsKey(k) ? merged.get(k) : null);
		}
		return out;
	}

	public Map<String, Object> saveFourKeys(long companyId, Map<String, Object> fourKeys) {
		try {
			String json = objectMapper.writeValueAsString(fourKeys);
			companysRedisTemplate.opsForValue().set(key(companyId), json);
		} catch (JsonProcessingException e) {
			log.error("Failed to serialize web URL setting for companyId={}", companyId, e);
		} catch (DataAccessException e) {
			log.error("Failed to write web URL setting to Redis for companyId={}", companyId, e);
		}
		return fourKeys;
	}

	/**
	 * Returns decoded JSON: empty list when Redis value missing/blank or no usable structure;
	 * {@code null} when stored value is JSON {@code null} or syntactically invalid JSON;
	 * {@link Map} for objects; {@link java.util.List} for arrays; scalar for number/string/boolean
	 * roots.
	 */
	public Object readPayload(long companyId) {
		String raw;
		try {
			raw = companysRedisTemplate.opsForValue().get(key(companyId));
		} catch (DataAccessException e) {
			log.warn("Failed to read web URL setting from Redis for companyId={}", companyId, e);
			return Collections.emptyList();
		}
		if (raw == null || raw.isBlank()) {
			return Collections.emptyList();
		}
		try {
			JsonNode root = objectMapper.readTree(raw);
			if (root == null || root.isMissingNode()) {
				return Collections.emptyList();
			}
			if (root.isNull()) {
				return null;
			}
			if (root.isObject()) {
				return objectMapper.convertValue(root, new TypeReference<LinkedHashMap<String, Object>>() {});
			}
			if (root.isArray()) {
				return objectMapper.convertValue(root, new TypeReference<ArrayList<Object>>() {});
			}
			if (root.isNumber() || root.isTextual() || root.isBoolean()) {
				return objectMapper.convertValue(root, Object.class);
			}
			log.warn("Unexpected JSON root for web URL setting companyId={}", companyId);
			return Collections.emptyList();
		} catch (JsonProcessingException e) {
			log.warn("Failed to parse web URL setting JSON for companyId={}", companyId, e);
			return null;
		}
	}

	/**
	 * H5 read path: distinct from {@link #readPayload(long)} — blank Redis uses request default subset;
	 * invalid JSON or JSON {@code null} root yields {@code null}; object roots optionally get mobile
	 * userData query on selected URL keys.
	 */
	public Object getWebUrlSettingForH5(long companyId, Map<String, Object> requestDefaultSubset, String mobile) {
		Map<String, Object> defaults = requestDefaultSubset;
		String raw;
		try {
			raw = companysRedisTemplate.opsForValue().get(key(companyId));
		} catch (DataAccessException e) {
			log.warn("Failed to read web URL setting from Redis for companyId={}", companyId, e);
			throw new ResourceException("读取外部链接配置失败");
		}
		if (raw == null || raw.isBlank()) {
			return new LinkedHashMap<>(defaults);
		}
		JsonNode root;
		try {
			root = objectMapper.readTree(raw);
		} catch (JsonProcessingException e) {
			log.warn("H5 web URL setting JSON parse failed, companyId={}", companyId, e);
			return null;
		}
		if (root.isNull()) {
			return null;
		}
		if (root.isObject()) {
			LinkedHashMap<String, Object> map =
					objectMapper.convertValue(root, new TypeReference<LinkedHashMap<String, Object>>() {});
			if (StringUtils.hasText(mobile)) {
				return h5WebUrlMobileEncryptionService.applyMobileUserDataQuery(mobile, map);
			}
			return map;
		}
		return objectMapper.convertValue(root, Object.class);
	}
}
