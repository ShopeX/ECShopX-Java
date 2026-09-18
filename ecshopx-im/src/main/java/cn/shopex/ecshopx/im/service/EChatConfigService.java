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

package cn.shopex.ecshopx.im.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
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
public class EChatConfigService {

	private final StringRedisTemplate redis;
	private final ObjectMapper objectMapper;

	public EChatConfigService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redis,
			ObjectMapper objectMapper) {
		this.redis = redis;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getInfo(long companyId) {
		String raw = redis.opsForValue().get(redisKey(companyId));
		if (!StringUtils.hasText(raw)) {
			return defaultInfo();
		}
		try {
			Map<String, Object> parsed =
					objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
			if (parsed == null) {
				return defaultInfo();
			}
			LinkedHashMap<String, Object> out = new LinkedHashMap<>(parsed);
			if (!out.containsKey("is_open") || out.get("is_open") == null) {
				out.put("is_open", Boolean.FALSE);
			}
			if (!out.containsKey("echat_url") || out.get("echat_url") == null) {
				out.put("echat_url", "");
			}
			return out;
		} catch (Exception e) {
			return defaultInfo();
		}
	}

	public Map<String, Object> saveInfo(long companyId, Map<String, Object> data) {
		Object isOpen = data.get("is_open");
		if (isOpen == null || !StringUtils.hasText(String.valueOf(isOpen).trim())) {
			throw new BadRequestException("开启状态必填");
		}
		Object echatUrl = data.get("echat_url");
		if (echatUrl == null || !StringUtils.hasText(String.valueOf(echatUrl).trim())) {
			throw new BadRequestException("一洽客服链接地址必填");
		}

		String json;
		try {
			json = objectMapper.writeValueAsString(data);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
		redis.opsForValue().set(redisKey(companyId), json);
		return getInfo(companyId);
	}

	private static Map<String, Object> defaultInfo() {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("is_open", Boolean.FALSE);
		m.put("echat_url", "");
		return m;
	}

	private static String redisKey(long companyId) {
		return "im:echat:" + companyId;
	}
}
