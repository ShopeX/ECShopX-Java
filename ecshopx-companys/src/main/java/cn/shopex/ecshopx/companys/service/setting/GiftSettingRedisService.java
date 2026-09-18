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
 * Redis key {@code giftSetting:{companyId}}: JSON object read for GET is returned as a map (all keys
 * from Redis preserved); {@code minus_shop_gift_store} and {@code check_gift_store} are filled with
 * {@code false} when missing or JSON-null. {@link #saveGiftSetting} writes a two-field JSON object
 * (booleans); whole key replaced on write; no TTL. Jackson default boolean serialization
 * ({@code true}/{@code false}) on save.
 */
@Service
public class GiftSettingRedisService {

	private static final Logger log = LoggerFactory.getLogger(GiftSettingRedisService.class);

	private static final String KEY_PREFIX = "giftSetting:";
	private static final String FIELD_MINUS_SHOP_GIFT_STORE = "minus_shop_gift_store";
	private static final String FIELD_CHECK_GIFT_STORE = "check_gift_store";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public GiftSettingRedisService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	private static String key(long companyId) {
		return KEY_PREFIX + companyId;
	}

	private static LinkedHashMap<String, Object> defaultGiftSettingMap() {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>(2);
		out.put(FIELD_MINUS_SHOP_GIFT_STORE, Boolean.FALSE);
		out.put(FIELD_CHECK_GIFT_STORE, Boolean.FALSE);
		return out;
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

	public Map<String, Object> getGiftSetting(long companyId) {
		try {
			String raw = companysRedisTemplate.opsForValue().get(key(companyId));
			if (raw == null || raw.isBlank()) {
				return defaultGiftSettingMap();
			}
			LinkedHashMap<String, Object> out =
					objectMapper.readValue(raw, new TypeReference<LinkedHashMap<String, Object>>() {});
			if (out == null) {
				return defaultGiftSettingMap();
			}
			if (out.get(FIELD_MINUS_SHOP_GIFT_STORE) == null) {
				out.put(FIELD_MINUS_SHOP_GIFT_STORE, Boolean.FALSE);
			}
			if (out.get(FIELD_CHECK_GIFT_STORE) == null) {
				out.put(FIELD_CHECK_GIFT_STORE, Boolean.FALSE);
			}
			return out;
		} catch (Exception e) {
			log.warn("Failed to read gift setting for companyId={}", companyId, e);
			return defaultGiftSettingMap();
		}
	}

	public Map<String, Object> saveGiftSetting(long companyId, Map<String, Object> inputData) {
		Object rawMinus =
				inputData == null ? null : inputData.get(FIELD_MINUS_SHOP_GIFT_STORE);
		Object rawCheck = inputData == null ? null : inputData.get(FIELD_CHECK_GIFT_STORE);
		boolean minus = equalsTrueStringLiteral(rawMinus);
		boolean check = equalsTrueStringLiteral(rawCheck);
		Map<String, Object> payload = new LinkedHashMap<>(2);
		payload.put(FIELD_MINUS_SHOP_GIFT_STORE, minus);
		payload.put(FIELD_CHECK_GIFT_STORE, check);
		try {
			String json = objectMapper.writeValueAsString(payload);
			companysRedisTemplate.opsForValue().set(key(companyId), json);
		} catch (JsonProcessingException e) {
			log.warn("Failed to serialize gift setting for companyId={}", companyId, e);
			throw new ResourceException("保存赠品设置失败");
		} catch (DataAccessException e) {
			log.warn("Failed to write gift setting to Redis for companyId={}", companyId, e);
			throw new ResourceException("保存赠品设置失败");
		}
		return Map.of("status", Boolean.TRUE);
	}
}
