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
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis key {@code ShareParametersSetting:{companyId}}: JSON object with
 * {@code distributor_param_status}. Whole key replaced on write; no TTL.
 */
@Service
public class ShareParametersSettingRedisService {

	private static final Logger log = LoggerFactory.getLogger(ShareParametersSettingRedisService.class);

	private static final String KEY_PREFIX = "ShareParametersSetting:";
	private static final String FIELD_DISTRIBUTOR_PARAM_STATUS = "distributor_param_status";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public ShareParametersSettingRedisService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	private static String key(long companyId) {
		return KEY_PREFIX + companyId;
	}

	private static boolean normalizeDistributorParamStatusForWrite(Object raw) {
		return Boolean.TRUE.equals(raw);
	}

	private static Map<String, Object> defaultDistributorParamStatusPayload() {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>(1);
		out.put(FIELD_DISTRIBUTOR_PARAM_STATUS, Boolean.FALSE);
		return out;
	}

	public Map<String, Object> save(long companyId, Map<String, Object> merged) {
		Object raw = merged == null ? null : merged.get(FIELD_DISTRIBUTOR_PARAM_STATUS);
		boolean flag = normalizeDistributorParamStatusForWrite(raw);
		LinkedHashMap<String, Object> data = new LinkedHashMap<>(1);
		data.put(FIELD_DISTRIBUTOR_PARAM_STATUS, Boolean.valueOf(flag));
		try {
			String json = objectMapper.writeValueAsString(data);
			companysRedisTemplate.opsForValue().set(key(companyId), json);
		} catch (JsonProcessingException e) {
			log.error("Failed to serialize share parameters setting for companyId={}", companyId, e);
		} catch (DataAccessException e) {
			log.error("Failed to write share parameters setting to Redis for companyId={}", companyId, e);
		}
		return data;
	}

	public Map<String, Object> read(long companyId) {
		String raw;
		try {
			raw = companysRedisTemplate.opsForValue().get(key(companyId));
		} catch (DataAccessException e) {
			log.warn("Failed to read share parameters setting from Redis for companyId={}", companyId, e);
			return defaultDistributorParamStatusPayload();
		}
		if (raw == null || raw.isBlank()) {
			return defaultDistributorParamStatusPayload();
		}
		try {
			JsonNode root = objectMapper.readTree(raw);
			if (!root.isObject()) {
				return null;
			}
			Map<String, Object> m =
					objectMapper.convertValue(root, new TypeReference<LinkedHashMap<String, Object>>() {});
			if (m == null) {
				return null;
			}
			return m;
		} catch (JsonProcessingException e) {
			return null;
		}
	}
}
