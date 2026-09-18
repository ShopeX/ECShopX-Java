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
 * Redis key {@code mailSetting:{companyId}}: JSON with SMTP + activation domain fields. Whole key
 * replaced on write; no TTL.
 */
@Service
public class MailSettingRedisService {

	private static final Logger log = LoggerFactory.getLogger(MailSettingRedisService.class);

	private static final String KEY_PREFIX = "mailSetting:";

	public static final String EMAIL_SMTP_PORT = "EMAIL_SMTP_PORT";
	public static final String EMAIL_RELAY_HOST = "EMAIL_RELAY_HOST";
	public static final String EMAIL_SENDER = "EMAIL_SENDER";
	public static final String EMAIL_USER = "EMAIL_USER";
	public static final String EMAIL_PASSWORD = "EMAIL_PASSWORD";
	public static final String EMAIL_ACTIVATION_H5_DOMAIN = "EMAIL_ACTIVATION_H5_DOMAIN";
	public static final String EMAIL_ACTIVATION_PC_DOMAIN = "EMAIL_ACTIVATION_PC_DOMAIN";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public MailSettingRedisService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	private static String key(long companyId) {
		return KEY_PREFIX + companyId;
	}

	private static LinkedHashMap<String, Object> emptyMailSettingMap() {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>(7);
		m.put(EMAIL_SMTP_PORT, "");
		m.put(EMAIL_RELAY_HOST, "");
		m.put(EMAIL_SENDER, "");
		m.put(EMAIL_USER, "");
		m.put(EMAIL_PASSWORD, "");
		m.put(EMAIL_ACTIVATION_H5_DOMAIN, "");
		m.put(EMAIL_ACTIVATION_PC_DOMAIN, "");
		return m;
	}

	private static String stringField(Map<String, Object> parsed, String fieldKey) {
		if (parsed == null || !parsed.containsKey(fieldKey) || parsed.get(fieldKey) == null) {
			return "";
		}
		return parsed.get(fieldKey).toString();
	}

	/**
	 * Reads {@code mailSetting:{companyId}} from Redis. On missing key, blank value, parse error, or
	 * partial JSON, returns all seven fields as strings (missing entries as empty string).
	 */
	public Map<String, Object> readMailSetting(long companyId) {
		LinkedHashMap<String, Object> defaults = emptyMailSettingMap();
		try {
			String raw = companysRedisTemplate.opsForValue().get(key(companyId));
			if (raw == null || raw.isBlank()) {
				return defaults;
			}
			Map<String, Object> parsed =
					objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
			LinkedHashMap<String, Object> out = new LinkedHashMap<>(7);
			out.put(EMAIL_SMTP_PORT, stringField(parsed, EMAIL_SMTP_PORT));
			out.put(EMAIL_RELAY_HOST, stringField(parsed, EMAIL_RELAY_HOST));
			out.put(EMAIL_SENDER, stringField(parsed, EMAIL_SENDER));
			out.put(EMAIL_USER, stringField(parsed, EMAIL_USER));
			out.put(EMAIL_PASSWORD, stringField(parsed, EMAIL_PASSWORD));
			out.put(EMAIL_ACTIVATION_H5_DOMAIN, stringField(parsed, EMAIL_ACTIVATION_H5_DOMAIN));
			out.put(EMAIL_ACTIVATION_PC_DOMAIN, stringField(parsed, EMAIL_ACTIVATION_PC_DOMAIN));
			return out;
		} catch (JsonProcessingException e) {
			log.warn("Failed to parse mail setting JSON for companyId={}", companyId, e);
			return emptyMailSettingMap();
		} catch (DataAccessException e) {
			log.warn("Failed to read mail setting from Redis for companyId={}", companyId, e);
			return emptyMailSettingMap();
		}
	}

	public void saveMailSetting(long companyId, Map<String, String> config) {
		try {
			String json = objectMapper.writeValueAsString(config);
			companysRedisTemplate.opsForValue().set(key(companyId), json);
		} catch (JsonProcessingException e) {
			log.warn("Failed to serialize mail setting for companyId={}", companyId, e);
			throw new ResourceException("保存邮件配置失败");
		} catch (DataAccessException e) {
			log.warn("Failed to write mail setting to Redis for companyId={}", companyId, e);
			throw new ResourceException("保存邮件配置失败");
		}
	}
}
