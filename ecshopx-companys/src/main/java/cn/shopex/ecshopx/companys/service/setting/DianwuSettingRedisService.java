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
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class DianwuSettingRedisService {

	private static final Logger log = LoggerFactory.getLogger(DianwuSettingRedisService.class);

	private static final String KEY_PREFIX = "DianwuSetting:";
	private static final String FIELD_DIANWU_SHOW_STATUS = "dianwu_show_status";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public DianwuSettingRedisService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	private static String key(long companyId) {
		return KEY_PREFIX + companyId;
	}

	private static boolean normalizeDianwuShowStatusForWrite(Object raw) {
		return Boolean.TRUE.equals(raw == null ? Boolean.FALSE : raw);
	}

	private Map<String, Object> defaultDianwuSetting() {
		Map<String, Object> m = new LinkedHashMap<>(1);
		m.put(FIELD_DIANWU_SHOW_STATUS, Boolean.FALSE);
		return m;
	}

	public Map<String, Object> getDianwuSetting(long companyId) {
		String raw;
		try {
			raw = companysRedisTemplate.opsForValue().get(key(companyId));
		} catch (DataAccessException e) {
			log.warn("Failed to read dianwu setting from Redis for companyId={}", companyId, e);
			throw new ResourceException("获取店务端设置失败");
		}
		if (raw == null || raw.isBlank()) {
			return defaultDianwuSetting();
		}
		LinkedHashMap<String, Object> map;
		try {
			map = objectMapper.readValue(raw, new TypeReference<LinkedHashMap<String, Object>>() {});
		} catch (JsonProcessingException e) {
			String prefix = raw.length() > 128 ? raw.substring(0, 128) + "..." : raw;
			log.warn("Invalid dianwu setting JSON for companyId={}, rawPrefix={}", companyId, prefix, e);
			return defaultDianwuSetting();
		}
		if (map == null) {
			return defaultDianwuSetting();
		}
		if (!map.containsKey(FIELD_DIANWU_SHOW_STATUS)) {
			map.put(FIELD_DIANWU_SHOW_STATUS, Boolean.FALSE);
		} else {
			Object v = map.get(FIELD_DIANWU_SHOW_STATUS);
			map.put(FIELD_DIANWU_SHOW_STATUS, normalizeDianwuShowStatusForWrite(v));
		}
		return map;
	}

	public Map<String, Object> saveDianwuSetting(long companyId, Map<String, Object> inputData) {
		boolean flag =
				normalizeDianwuShowStatusForWrite(
						inputData == null ? null : inputData.get(FIELD_DIANWU_SHOW_STATUS));
		Map<String, Object> data = new HashMap<>(1);
		data.put(FIELD_DIANWU_SHOW_STATUS, flag);
		try {
			String json = objectMapper.writeValueAsString(data);
			companysRedisTemplate.opsForValue().set(key(companyId), json);
		} catch (JsonProcessingException e) {
			log.warn("Failed to serialize dianwu setting for companyId={}", companyId, e);
			throw new ResourceException("保存店务端设置失败");
		} catch (DataAccessException e) {
			log.warn("Failed to write dianwu setting to Redis for companyId={}", companyId, e);
			throw new ResourceException("保存店务端设置失败");
		}
		return data;
	}
}
