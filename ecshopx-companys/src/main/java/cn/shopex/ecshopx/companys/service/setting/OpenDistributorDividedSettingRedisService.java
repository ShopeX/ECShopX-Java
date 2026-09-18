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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OpenDistributorDividedSettingRedisService {

	private static final String KEY_PREFIX = "OpenDistributorDivided:";
	private static final String REDIS_IO_FAILED = "店铺隔离白名单设置读写失败";
	private static final String FIELD_OPEN_DISTRIBUTOR_DIVIDED = "open_distributor_divided";
	private static final String FIELD_STATUS = "status";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public OpenDistributorDividedSettingRedisService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	private static String redisKey(long companyId) {
		return KEY_PREFIX + companyId;
	}

	private static Map<String, Object> defaultInnerMap() {
		Map<String, Object> m = new LinkedHashMap<>(2);
		m.put(FIELD_STATUS, Boolean.FALSE);
		m.put("template_id", 0);
		return m;
	}

	private static boolean statusStringIsTrue(Object raw) {
		return raw instanceof String s && !s.isEmpty() && "true".equalsIgnoreCase(s);
	}

	public Map<String, Object> setOpenDistributorDivided(long companyId, Map<String, Object> inputdata) {
		if (inputdata == null
				|| !inputdata.containsKey(FIELD_OPEN_DISTRIBUTOR_DIVIDED)
				|| inputdata.get(FIELD_OPEN_DISTRIBUTOR_DIVIDED) == null) {
			log.warn("OpenDistributorDivided Redis write invalid input, companyId={}", companyId);
			throw new ResourceException(REDIS_IO_FAILED);
		}
		Object value = inputdata.get(FIELD_OPEN_DISTRIBUTOR_DIVIDED);
		Map<String, Object> data = new LinkedHashMap<>(1);
		data.put(FIELD_OPEN_DISTRIBUTOR_DIVIDED, value);

		String json;
		try {
			json = objectMapper.writeValueAsString(data);
		} catch (JsonProcessingException e) {
			log.warn("OpenDistributorDivided Redis write failed, companyId={}", companyId, e);
			throw new ResourceException(REDIS_IO_FAILED);
		}
		try {
			companysRedisTemplate.opsForValue().set(redisKey(companyId), json);
		} catch (DataAccessException e) {
			log.warn("OpenDistributorDivided Redis write failed, companyId={}", companyId, e);
			throw new ResourceException(REDIS_IO_FAILED);
		}
		return data;
	}

	public Map<String, Object> getOpenDistributorDivided(long companyId) {
		String raw;
		try {
			raw = companysRedisTemplate.opsForValue().get(redisKey(companyId));
		} catch (DataAccessException e) {
			log.warn("OpenDistributorDivided Redis read failed, companyId={}", companyId, e);
			throw new ResourceException(REDIS_IO_FAILED);
		}

		if (raw == null || raw.isBlank()) {
			return defaultInnerMap();
		}

		JsonNode root;
		try {
			root = objectMapper.readTree(raw);
		} catch (JsonProcessingException e) {
			throw new ResourceException(REDIS_IO_FAILED);
		}

		if (root == null || !root.isObject()) {
			return defaultInnerMap();
		}

		JsonNode inner = root.get(FIELD_OPEN_DISTRIBUTOR_DIVIDED);
		if (inner == null || !inner.isObject()) {
			return defaultInnerMap();
		}

		Map<String, Object> map = new LinkedHashMap<>();
		for (Iterator<Entry<String, JsonNode>> it = inner.fields(); it.hasNext(); ) {
			Entry<String, JsonNode> e = it.next();
			map.put(e.getKey(), objectMapper.convertValue(e.getValue(), Object.class));
		}
		map.put(FIELD_STATUS, statusStringIsTrue(map.get(FIELD_STATUS)));
		return map;
	}
}
