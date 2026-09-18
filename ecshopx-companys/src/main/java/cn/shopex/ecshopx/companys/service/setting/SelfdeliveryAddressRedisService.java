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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SelfdeliveryAddressRedisService {

	private static final Logger log = LoggerFactory.getLogger(SelfdeliveryAddressRedisService.class);

	private static final String KEY_PREFIX = "selfDeliveryAddress:";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public SelfdeliveryAddressRedisService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	private static String key(long companyId) {
		return KEY_PREFIX + companyId;
	}

	public List<Map<String, Object>> readWholeList(long companyId) {
		String raw = companysRedisTemplate.opsForValue().get(key(companyId));
		if (raw == null || raw.isBlank()) {
			return new ArrayList<>();
		}
		try {
			return objectMapper.readValue(raw, new TypeReference<List<Map<String, Object>>>() {});
		} catch (JsonProcessingException e) {
			try {
				Map<String, Object> map = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
				return new ArrayList<>(List.of(map));
			} catch (JsonProcessingException e2) {
				log.warn("Failed to parse selfdelivery address JSON for companyId={}", companyId, e2);
				return new ArrayList<>();
			}
		}
	}

	public void saveWholeList(long companyId, List<Map<String, Object>> rows) {
		try {
			String json = objectMapper.writeValueAsString(rows);
			companysRedisTemplate.opsForValue().set(key(companyId), json);
		} catch (JsonProcessingException e) {
			log.error("Failed to write selfdelivery address to Redis for companyId={}", companyId, e);
		} catch (DataAccessException e) {
			log.error("Failed to write selfdelivery address to Redis for companyId={}", companyId, e);
		}
	}
}
