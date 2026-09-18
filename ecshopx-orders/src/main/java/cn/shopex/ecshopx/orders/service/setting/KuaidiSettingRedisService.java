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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class KuaidiSettingRedisService {

	private static final String KUAIDI_TYPE_OPEN_PREFIX = "kuaidiTypeOpenConfig:";
	private static final String KDNIAO_SETTING_PREFIX = "kdniaoKuaidiSetting:";
	private static final String KUAIDI100_SETTING_PREFIX = "kuaidi100KuaidiSetting:";

	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public KuaidiSettingRedisService(
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

	private static boolean isOpenForKuaidiTypeOpenKey(JsonNode params) {
		if (params == null) {
			return false;
		}
		if (!params.isObject()) {
			return false;
		}
		if (!params.has("is_open")) {
			return false;
		}
		JsonNode n = params.get("is_open");
		if (n.isNull() || n.isMissingNode()) {
			return false;
		}
		if (n.isBoolean()) {
			return n.booleanValue();
		}
		if (n.isTextual()) {
			return "true".equals(n.asText());
		}
		return false;
	}

	private String readKuaidiSettingRedisRaw(long companyId, String kuaidiType) {
		String suffix = sha1HexUtf8(String.valueOf(companyId));
		String settingPrefix =
				"kdniao".equals(kuaidiType) ? KDNIAO_SETTING_PREFIX : KUAIDI100_SETTING_PREFIX;
		String settingKey = settingPrefix + suffix;
		return stringRedisTemplate.opsForValue().get(settingKey);
	}

	/**
	 * Reads the raw open-type flag stored under {@code kuaidiTypeOpenConfig:{sha1(companyId)}}; missing
	 * keys yield null. Callers compare to {@code "kuaidi100"} exactly for branch selection.
	 */
	public String getKuaidiTypeOpenConfigRaw(long companyId) {
		String suffix = sha1HexUtf8(String.valueOf(companyId));
		return stringRedisTemplate.opsForValue().get(KUAIDI_TYPE_OPEN_PREFIX + suffix);
	}

	public void setKuaidiSetting(long companyId, String kuaidiType, JsonNode configPayload) {
		String suffix = sha1HexUtf8(String.valueOf(companyId));
		String openKey = KUAIDI_TYPE_OPEN_PREFIX + suffix;
		String settingPrefix =
				"kdniao".equals(kuaidiType) ? KDNIAO_SETTING_PREFIX : KUAIDI100_SETTING_PREFIX;
		String settingKey = settingPrefix + suffix;
		if (isOpenForKuaidiTypeOpenKey(configPayload)) {
			stringRedisTemplate.opsForValue().set(openKey, kuaidiType);
		} else {
			stringRedisTemplate.delete(openKey);
		}
		try {
			stringRedisTemplate
					.opsForValue()
					.set(settingKey, objectMapper.writeValueAsString(configPayload));
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Failed to serialize kuaidi setting", e);
		}
	}

	/**
	 * Loads kuaidi JSON config from Redis for the company. Missing or blank values yield an empty
	 * object node; invalid JSON is treated as empty object (callers treat missing credentials as
	 * invalid configuration).
	 */
	public JsonNode getKuaidiSettingJson(long companyId, String kuaidiType) {
		if (!"kdniao".equals(kuaidiType) && !"kuaidi100".equals(kuaidiType)) {
			throw new IllegalArgumentException("kuaidiType must be kdniao or kuaidi100");
		}
		String raw = readKuaidiSettingRedisRaw(companyId, kuaidiType);
		if (raw == null || raw.isBlank()) {
			return objectMapper.createObjectNode();
		}
		try {
			JsonNode node = objectMapper.readTree(raw);
			if (node == null || !node.isObject()) {
				return objectMapper.createObjectNode();
			}
			return node;
		} catch (JsonProcessingException e) {
			return objectMapper.createObjectNode();
		}
	}

	public Object getKuaidiSetting(long companyId, String kuaidiType) {
		if (!"kdniao".equals(kuaidiType) && !"kuaidi100".equals(kuaidiType)) {
			throw new IllegalStateException("kuaidiType must be kdniao or kuaidi100");
		}
		String raw = readKuaidiSettingRedisRaw(companyId, kuaidiType);
		if (raw == null || raw.isBlank()) {
			return List.of();
		}
		JsonNode node;
		try {
			node = objectMapper.readTree(raw);
		} catch (JsonProcessingException e) {
			return List.of();
		}
		if (node == null || !node.isObject()) {
			return List.of();
		}
		LinkedHashMap<String, Object> data =
				objectMapper.convertValue(node, new TypeReference<LinkedHashMap<String, Object>>() {});
		String suffix = sha1HexUtf8(String.valueOf(companyId));
		String openRaw = stringRedisTemplate.opsForValue().get(KUAIDI_TYPE_OPEN_PREFIX + suffix);
		boolean isOpen = kuaidiType.equals(openRaw);
		data.put("is_open", isOpen);
		return data;
	}
}
