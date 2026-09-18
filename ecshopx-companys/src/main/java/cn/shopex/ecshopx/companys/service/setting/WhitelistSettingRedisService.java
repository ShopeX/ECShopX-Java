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
 * Redis key {@code WhitelistSetting:{companyId}}: JSON with {@code whitelist_status} (typically boolean;
 * legacy data may use string or other scalars) and {@code whitelist_tips} (string). Whole key replaced on
 * conditional write; no TTL.
 */
@Service
public class WhitelistSettingRedisService {

	private static final Logger log = LoggerFactory.getLogger(WhitelistSettingRedisService.class);

	private static final String KEY_PREFIX = "WhitelistSetting:";
	private static final String FIELD_WHITELIST_STATUS = "whitelist_status";
	private static final String FIELD_WHITELIST_TIPS = "whitelist_tips";
	private static final String DEFAULT_TIPS = "登录失败，手机号不在白名单内！";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public WhitelistSettingRedisService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	private static String key(long companyId) {
		return KEY_PREFIX + companyId;
	}

	/** True when value is {@code Boolean} true or the string literal {@code "true"} (case-sensitive). */
	private static boolean equalsTrueStringLiteral(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b.booleanValue();
		}
		return "true".equals(String.valueOf(raw));
	}

	private static boolean hasPresentKey(Map<String, Object> input, String key) {
		return input != null && input.containsKey(key) && input.get(key) != null;
	}

	/**
	 * Returns {@code whitelist_status} as parsed from Redis JSON, or {@link Boolean#FALSE} when the key is
	 * absent or the value is JSON null. Preserves scalar types (boolean, string, number) for read-only status
	 * APIs.
	 */
	public Object getWhitelistStatusFieldValue(long companyId) {
		Map<String, Object> parsed = null;
		try {
			String raw = companysRedisTemplate.opsForValue().get(key(companyId));
			if (raw != null && !raw.isBlank()) {
				parsed =
						objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
			}
		} catch (Exception e) {
			log.warn("Failed to read whitelist setting for companyId={}", companyId, e);
			parsed = null;
		}
		if (parsed == null) {
			return Boolean.FALSE;
		}
		Object v = parsed.get(FIELD_WHITELIST_STATUS);
		if (v == null) {
			return Boolean.FALSE;
		}
		return v;
	}

	private Map<String, Object> loadConfig(long companyId) {
		Map<String, Object> parsed = null;
		try {
			String raw = companysRedisTemplate.opsForValue().get(key(companyId));
			if (raw != null && !raw.isBlank()) {
				parsed =
						objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
			}
		} catch (Exception e) {
			log.warn("Failed to read whitelist setting for companyId={}", companyId, e);
			parsed = null;
		}
		if (parsed == null) {
			parsed = Map.of();
		}
		boolean status = equalsTrueStringLiteral(parsed.get(FIELD_WHITELIST_STATUS));
		String tips;
		if (!parsed.containsKey(FIELD_WHITELIST_TIPS) || parsed.get(FIELD_WHITELIST_TIPS) == null) {
			tips = DEFAULT_TIPS;
		} else {
			String t = String.valueOf(parsed.get(FIELD_WHITELIST_TIPS)).trim();
			tips = t.isEmpty() ? DEFAULT_TIPS : t;
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>(2);
		out.put(FIELD_WHITELIST_STATUS, status);
		out.put(FIELD_WHITELIST_TIPS, tips);
		return out;
	}

	public Map<String, Object> getMergedConfig(long companyId, Map<String, Object> mergedInput) {
		Map<String, Object> config = new LinkedHashMap<>(loadConfig(companyId));
		boolean shouldWrite =
				hasPresentKey(mergedInput, FIELD_WHITELIST_STATUS)
						|| hasPresentKey(mergedInput, FIELD_WHITELIST_TIPS);
		if (shouldWrite) {
			if (hasPresentKey(mergedInput, FIELD_WHITELIST_STATUS)) {
				config.put(
						FIELD_WHITELIST_STATUS,
						equalsTrueStringLiteral(mergedInput.get(FIELD_WHITELIST_STATUS)));
			}
			if (hasPresentKey(mergedInput, FIELD_WHITELIST_TIPS)) {
				String t = String.valueOf(mergedInput.get(FIELD_WHITELIST_TIPS)).trim();
				config.put(FIELD_WHITELIST_TIPS, t.isEmpty() ? DEFAULT_TIPS : t);
			}
			try {
				String json = objectMapper.writeValueAsString(config);
				companysRedisTemplate.opsForValue().set(key(companyId), json);
			} catch (JsonProcessingException e) {
				log.warn("Failed to serialize whitelist setting for companyId={}", companyId, e);
				throw new ResourceException("保存白名单设置失败");
			} catch (DataAccessException e) {
				log.warn("Failed to write whitelist setting to Redis for companyId={}", companyId, e);
				throw new ResourceException("保存白名单设置失败");
			}
		}
		return config;
	}
}
