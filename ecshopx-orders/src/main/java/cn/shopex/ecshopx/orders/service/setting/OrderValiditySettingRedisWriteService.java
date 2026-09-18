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

package cn.shopex.ecshopx.orders.service.setting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderValiditySettingRedisWriteService {

	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public OrderValiditySettingRedisWriteService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate stringRedisTemplate,
			ObjectMapper objectMapper) {
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public void writeCompanySettingJson(long companyId, Map<String, Object> mergedToStore) {
		String field = String.valueOf(companyId);
		String raw =
				stringRedisTemplate
						.<String, String>opsForHash()
						.get(OrderValiditySettingDefaults.REDIS_KEY, field);
		Map<String, Object> base = new LinkedHashMap<>(OrderValiditySettingDefaults.DEFAULTS);
		if (StringUtils.hasText(raw)) {
			try {
				Map<String, Object> parsed =
						objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
				if (parsed != null) {
					base.putAll(parsed);
				}
			} catch (Exception ignored) {
			}
		}
		base.putAll(mergedToStore);
		try {
			stringRedisTemplate
					.opsForHash()
					.put(
							OrderValiditySettingDefaults.REDIS_KEY,
							field,
							objectMapper.writeValueAsString(base));
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}
}
