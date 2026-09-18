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
 * Redis key {@code PresalePickupcodeSetting:{companyId}}: JSON with only {@code pickupcode_status}.
 * Whole key replaced on write; no TTL.
 */
@Service
public class PickupcodeSettingRedisService {

	private static final Logger log = LoggerFactory.getLogger(PickupcodeSettingRedisService.class);

	private static final String KEY_PREFIX = "PresalePickupcodeSetting:";
	private static final String FIELD_PICKUPCODE_STATUS = "pickupcode_status";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public PickupcodeSettingRedisService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	private static String key(long companyId) {
		return KEY_PREFIX + companyId;
	}

	/**
	 * Write branch when the merged input has key {@link #FIELD_PICKUPCODE_STATUS} present and its
	 * value is non-null (aligned with {@code isset} semantics for non-null values).
	 */
	private static boolean isWriteBranchForPickupcodeStatus(Map<String, Object> merged) {
		return merged != null
				&& merged.containsKey(FIELD_PICKUPCODE_STATUS)
				&& merged.get(FIELD_PICKUPCODE_STATUS) != null;
	}

	private static boolean toPickupcodeBoolean(Object raw) {
		if (raw instanceof Boolean b) {
			return b.booleanValue();
		}
		return "true".equals(String.valueOf(raw).trim());
	}

	private static Map<String, Object> defaultStatusPayload() {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>(1);
		out.put(FIELD_PICKUPCODE_STATUS, Boolean.FALSE);
		return out;
	}

	public Map<String, Object> handle(long companyId, Map<String, Object> merged) {
		if (isWriteBranchForPickupcodeStatus(merged)) {
			boolean flag = toPickupcodeBoolean(merged.get(FIELD_PICKUPCODE_STATUS));
			LinkedHashMap<String, Object> data = new LinkedHashMap<>(1);
			data.put(FIELD_PICKUPCODE_STATUS, flag);
			try {
				String json = objectMapper.writeValueAsString(data);
				companysRedisTemplate.opsForValue().set(key(companyId), json);
			} catch (JsonProcessingException e) {
				log.error("Failed to serialize pickupcode setting for companyId={}", companyId, e);
			} catch (DataAccessException e) {
				log.error("Failed to write pickupcode setting to Redis for companyId={}", companyId, e);
			}
			return data;
		}

		String raw = companysRedisTemplate.opsForValue().get(key(companyId));
		if (raw == null || raw.isBlank()) {
			return defaultStatusPayload();
		}
		try {
			Map<String, Object> parsed =
					objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
			boolean status = toPickupcodeBoolean(parsed.get(FIELD_PICKUPCODE_STATUS));
			LinkedHashMap<String, Object> out = new LinkedHashMap<>(1);
			out.put(FIELD_PICKUPCODE_STATUS, status);
			return out;
		} catch (JsonProcessingException e) {
			log.warn("Failed to parse pickupcode setting JSON for companyId={}", companyId, e);
			return defaultStatusPayload();
		}
	}
}
