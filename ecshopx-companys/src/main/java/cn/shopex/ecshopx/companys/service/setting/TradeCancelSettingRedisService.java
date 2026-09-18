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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TradeCancelSettingRedisService {

	private static final Logger log = LoggerFactory.getLogger(TradeCancelSettingRedisService.class);

	private static final String KEY_PREFIX = "tradeCancelSetting:";

	private final StringRedisTemplate sharedStringRedisTemplate;
	private final ObjectMapper objectMapper;

	public TradeCancelSettingRedisService(
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

	private static Map<String, Object> defaultCancelSetting() {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>(1);
		out.put("repeat_cancel", Boolean.FALSE);
		return out;
	}

	private void ensureRepeatCancelNormalized(Map<String, Object> map) {
		Object v = map.get("repeat_cancel");
		if (!map.containsKey("repeat_cancel") || v == null) {
			map.put("repeat_cancel", Boolean.FALSE);
			return;
		}
		if (v instanceof Boolean b) {
			map.put("repeat_cancel", b);
			return;
		}
		if (v instanceof Number n) {
			map.put("repeat_cancel", n.intValue() != 0);
			return;
		}
		if (v instanceof String s) {
			map.put("repeat_cancel", "true".equalsIgnoreCase(s) || "1".equals(s));
			return;
		}
		if (v instanceof Map || v instanceof Collection || (v.getClass().isArray())) {
			map.put("repeat_cancel", Boolean.FALSE);
			return;
		}
		map.put("repeat_cancel", Boolean.FALSE);
	}

	public Map<String, Object> getCancelSetting(long companyId) {
		String raw = sharedStringRedisTemplate.opsForValue().get(key(companyId));
		Map<String, Object> result;
		if (raw == null || raw.isBlank()) {
			result = defaultCancelSetting();
		} else {
			try {
				Map<String, Object> parsed =
						objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
				if (parsed == null) {
					result = defaultCancelSetting();
				} else {
					result = new LinkedHashMap<>(parsed);
				}
			} catch (JsonProcessingException e) {
				log.warn("Failed to parse trade cancel setting JSON for companyId={}", companyId, e);
				result = defaultCancelSetting();
			}
		}
		ensureRepeatCancelNormalized(result);
		return result;
	}

	public void setCancelSetting(long companyId, boolean repeatCancel) {
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>(1);
		payload.put("repeat_cancel", Boolean.valueOf(repeatCancel));
		try {
			sharedStringRedisTemplate
					.opsForValue()
					.set(key(companyId), objectMapper.writeValueAsString(payload));
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Failed to serialize trade cancel setting", e);
		}
	}
}
