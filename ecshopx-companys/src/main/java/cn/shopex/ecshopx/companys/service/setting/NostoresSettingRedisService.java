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
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NostoresSettingRedisService {

	private static final String KEY_PREFIX = "NostoresSetting:";
	private static final String FIELD_NOSTORES_STATUS = "nostores_status";
	private static final String REDIS_IO_FAILED = "无店铺展示设置读写失败";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public NostoresSettingRedisService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	private static String redisKey(long companyId) {
		return KEY_PREFIX + companyId;
	}

	/**
	 * Only the string {@code "true"} yields true; missing or null coalesces like string {@code "false"}.
	 */
	private static boolean normalizeNostoresStatus(Map<String, Object> map) {
		Object raw = map.get(FIELD_NOSTORES_STATUS);
		final Object coalesced;
		if (!map.containsKey(FIELD_NOSTORES_STATUS) || raw == null) {
			coalesced = "false";
		} else {
			coalesced = raw;
		}
		return coalesced instanceof String && "true".equals(coalesced);
	}

	public Map<String, Object> readSetting(long companyId) {
		String raw;
		try {
			raw = companysRedisTemplate.opsForValue().get(redisKey(companyId));
		} catch (DataAccessException e) {
			log.warn("NostoresSetting Redis read failed, companyId={}", companyId, e);
			throw new ResourceException(REDIS_IO_FAILED);
		}

		Map<String, Object> map;
		if (raw == null || raw.isBlank()) {
			map = new HashMap<>();
		} else {
			try {
				map = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
			} catch (JsonProcessingException e) {
				throw new ResourceException(REDIS_IO_FAILED);
			}
			if (map == null) {
				map = new HashMap<>();
			}
		}

		boolean normalized = normalizeNostoresStatus(map);
		map.put(FIELD_NOSTORES_STATUS, normalized);
		return map;
	}

	public Map<String, Object> getNostoresStatus(long companyId) {
		return readSetting(companyId);
	}

	public void writeWholePayload(long companyId, Map<String, Object> payload) {
		String json;
		try {
			json = objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException e) {
			log.warn("NostoresSetting Redis write failed, companyId={}", companyId, e);
			throw new ResourceException(REDIS_IO_FAILED);
		}
		try {
			companysRedisTemplate.opsForValue().set(redisKey(companyId), json);
		} catch (DataAccessException e) {
			log.warn("NostoresSetting Redis write failed, companyId={}", companyId, e);
			throw new ResourceException(REDIS_IO_FAILED);
		}
	}
}
