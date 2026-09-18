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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis key {@code sendOmsSetting:{companyId}}: JSON with only {@code ziti_send_oms}. Whole key
 * replaced on write; no TTL.
 */
@Service
public class SendOmsSettingRedisService {

	private static final Logger log = LoggerFactory.getLogger(SendOmsSettingRedisService.class);

	private static final String KEY_PREFIX = "sendOmsSetting:";
	private static final String FIELD_ZITI_SEND_OMS = "ziti_send_oms";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public SendOmsSettingRedisService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	private static String key(long companyId) {
		return KEY_PREFIX + companyId;
	}

	/**
	 * True when value is {@code Boolean} true or the string literal {@code "true"} (via
	 * {@link String#valueOf(Object)}, case-sensitive, no trim).
	 */
	private static boolean equalsTrueStringLiteral(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b.booleanValue();
		}
		return "true".equals(String.valueOf(raw));
	}

	private static Map<String, Object> defaultZitiSendOmsPayload() {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>(1);
		out.put(FIELD_ZITI_SEND_OMS, Boolean.FALSE);
		return out;
	}

	public Map<String, Object> readZitiSendOms(long companyId) {
		String raw = companysRedisTemplate.opsForValue().get(key(companyId));
		if (raw == null || raw.isBlank()) {
			return defaultZitiSendOmsPayload();
		}
		try {
			Map<String, Object> parsed =
					objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
			boolean v = equalsTrueStringLiteral(parsed.get(FIELD_ZITI_SEND_OMS));
			LinkedHashMap<String, Object> out = new LinkedHashMap<>(1);
			out.put(FIELD_ZITI_SEND_OMS, v);
			return out;
		} catch (JsonProcessingException e) {
			log.warn("Failed to parse send OMS setting JSON for companyId={}", companyId, e);
			return defaultZitiSendOmsPayload();
		}
	}

	public Map<String, Object> saveZitiSendOms(long companyId, Map<String, Object> merged) {
		Object rawZiti = merged == null ? null : merged.get(FIELD_ZITI_SEND_OMS);
		boolean ziti = equalsTrueStringLiteral(rawZiti);
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>(1);
		payload.put(FIELD_ZITI_SEND_OMS, ziti);
		try {
			String json = objectMapper.writeValueAsString(payload);
			companysRedisTemplate.opsForValue().set(key(companyId), json);
		} catch (JsonProcessingException e) {
			log.error("Failed to serialize send OMS setting for companyId={}", companyId, e);
		} catch (DataAccessException e) {
			log.error("Failed to write send OMS setting to Redis for companyId={}", companyId, e);
		}
		return Map.of("status", Boolean.TRUE);
	}
}
