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

package cn.shopex.ecshopx.orders.service.setting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SfbspSettingRedisService {

	private static final Logger log = LoggerFactory.getLogger(SfbspSettingRedisService.class);

	private static final String REDIS_KEY_PREFIX = "SFBSPConfigSetting:";

	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public SfbspSettingRedisService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate stringRedisTemplate,
			ObjectMapper objectMapper) {
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
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

	/** Null、空串、或单独字符 {@code "0"} 视为无有效配置（与存量接口约定一致）。 */
	private static boolean isEffectiveRedisSettingString(String raw) {
		if (raw == null || raw.isEmpty() || "0".equals(raw)) {
			return false;
		}
		return true;
	}

	public JsonNode getSfbspSetting(long companyId) {
		String suffix = sha1HexUtf8(String.valueOf(companyId));
		String key = REDIS_KEY_PREFIX + suffix;
		String raw = stringRedisTemplate.opsForValue().get(key);
		if (!isEffectiveRedisSettingString(raw)) {
			return objectMapper.getNodeFactory().arrayNode();
		}
		try {
			return objectMapper.readTree(raw);
		} catch (JsonProcessingException e) {
			String snippet = raw.length() > 200 ? raw.substring(0, 200) + "…" : raw;
			log.warn("顺丰 BSP 配置 Redis 值非合法 JSON，已回退为空数组: {}", snippet);
			return objectMapper.getNodeFactory().arrayNode();
		}
	}

	public void setSfbspSetting(long companyId, Object configRaw) {
		String suffix = sha1HexUtf8(String.valueOf(companyId));
		String key = REDIS_KEY_PREFIX + suffix;
		JsonNode n;
		if (configRaw == null) {
			n = objectMapper.getNodeFactory().nullNode();
		} else {
			n = objectMapper.valueToTree(configRaw);
		}
		try {
			String jsonString = objectMapper.writeValueAsString(n);
			stringRedisTemplate.opsForValue().set(key, jsonString);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Failed to serialize sfbsp setting", e);
		}
	}
}
