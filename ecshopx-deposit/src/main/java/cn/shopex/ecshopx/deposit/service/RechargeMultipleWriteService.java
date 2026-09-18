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

package cn.shopex.ecshopx.deposit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RechargeMultipleWriteService {

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	public RechargeMultipleWriteService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redisTemplate,
			ObjectMapper objectMapper) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
	}

	public void setRechargeMultiple(long companyId, Map<String, Object> data) throws JsonProcessingException {
		String key = "RechargeMultiple:" + companyId;
		String json = objectMapper.writeValueAsString(data);
		redisTemplate.opsForValue().set(key, json);
	}

	/**
	 * Reads {@code RechargeMultiple:{companyId}} from Redis. Missing or empty value yields the default four-field map;
	 * invalid JSON yields {@code null}.
	 */
	public Map<String, Object> getRechargeMultipleByCompanyId(long companyId) {
		String key = "RechargeMultiple:" + companyId;
		String raw = redisTemplate.opsForValue().get(key);
		if (raw == null || raw.isEmpty()) {
			return defaultRechargeMultipleMap();
		}
		try {
			return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
		} catch (IOException e) {
			return null;
		}
	}

	private static Map<String, Object> defaultRechargeMultipleMap() {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("start_time", Integer.valueOf(0));
		m.put("end_time", Integer.valueOf(0));
		m.put("is_open", Boolean.FALSE);
		m.put("multiple", Integer.valueOf(1));
		return m;
	}
}
