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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderValiditySettingRedisReadService {

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public OrderValiditySettingRedisReadService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> readPlatformSetting(long companyId) {
		String raw =
				companysRedisTemplate
						.<String, String>opsForHash()
						.get(OrderValiditySettingDefaults.REDIS_KEY, String.valueOf(companyId));
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		if (StringUtils.hasText(raw)) {
			try {
				LinkedHashMap<String, Object> parsed =
						objectMapper.readValue(raw, new TypeReference<LinkedHashMap<String, Object>>() {});
				if (parsed != null) {
					result.putAll(parsed);
				}
			} catch (Exception ignored) {
			}
		}
		for (Map.Entry<String, Object> e : OrderValiditySettingDefaults.DEFAULTS.entrySet()) {
			String k = e.getKey();
			if (!result.containsKey(k) || result.get(k) == null) {
				result.put(k, e.getValue());
			}
		}
		return result;
	}

	public Map<String, Object> getOrderSetting(long companyId) {
		return readPlatformSetting(companyId);
	}
}
