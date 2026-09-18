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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.LinkedHashMap;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TradeBasicSettingRedisService {

	private static final String KEY_PREFIX = "tradeBasicSetting:";

	private final StringRedisTemplate sharedStringRedisTemplate;
	private final ObjectMapper objectMapper;

	public TradeBasicSettingRedisService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			ObjectMapper objectMapper) {
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	private static String key(long companyId) {
		return KEY_PREFIX + sha1HexUtf8(String.valueOf(companyId));
	}

	private static String sha1HexUtf8(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(digest.length * 2);
			for (byte b : digest) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-1 not available", e);
		}
	}

	public Object getSetting(long companyId) {
		String raw = sharedStringRedisTemplate.opsForValue().get(key(companyId));
		if (raw == null || raw.isBlank()) {
			return Collections.emptyList();
		}
		JsonNode root;
		try {
			root = objectMapper.readTree(raw);
		} catch (JsonProcessingException e) {
			LinkedHashMap<String, Object> fallback = new LinkedHashMap<>();
			normalizeIsOpen(fallback);
			return fallback;
		}
		LinkedHashMap<String, Object> map;
		if (root.isObject()) {
			map = objectMapper.convertValue(root, new TypeReference<LinkedHashMap<String, Object>>() {});
			if (map == null) {
				map = new LinkedHashMap<>();
			}
		} else if (root.isArray() && root.isEmpty()) {
			map = new LinkedHashMap<>();
		} else if (root.isNull()) {
			// JSON null root: same as empty object before is_open normalization.
			map = new LinkedHashMap<>();
		} else {
			throw new IllegalStateException(
					"trade basic setting root must be object or empty array, companyId=" + companyId);
		}
		normalizeIsOpen(map);
		return map;
	}

	public void setSetting(long companyId, Object config) {
		String redisKey = key(companyId);
		try {
			String json = objectMapper.writeValueAsString(config);
			sharedStringRedisTemplate.opsForValue().set(redisKey, json);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Failed to serialize trade basic setting", e);
		}
	}

	private static void normalizeIsOpen(LinkedHashMap<String, Object> map) {
		if (!map.containsKey("is_open") || map.get("is_open") == null) {
			map.put("is_open", Boolean.FALSE);
			return;
		}
		Object v = map.get("is_open");
		if (Boolean.TRUE.equals(v)) {
			map.put("is_open", Boolean.TRUE);
			return;
		}
		if (v instanceof String s && "true".equals(s)) {
			map.put("is_open", Boolean.TRUE);
			return;
		}
		map.put("is_open", Boolean.FALSE);
	}
}
