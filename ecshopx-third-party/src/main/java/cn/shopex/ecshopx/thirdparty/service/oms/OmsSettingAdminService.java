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

package cn.shopex.ecshopx.thirdparty.service.oms;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class OmsSettingAdminService {

	private static final String REDIS_KEY_PREFIX = "OmsSettingReidsId:";

	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public OmsSettingAdminService(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public void setOmsSetting(long companyId, Object configRaw) {
		String payload = normalizeConfigForRedisStorage(configRaw);
		String key = redisKey(companyId);
		try {
			stringRedisTemplate.opsForValue().set(key, payload);
		} catch (DataAccessException e) {
			throw new ResourceException("Redis 不可用，请稍后重试");
		}
	}

	public Object getOmsSetting(long companyId) {
		String key = redisKey(companyId);
		String raw;
		try {
			raw = stringRedisTemplate.opsForValue().get(key);
		} catch (DataAccessException e) {
			throw new ResourceException("Redis 不可用，请稍后重试");
		}
		if (raw == null) {
			return new ArrayList<Object>();
		}
		if (raw.isEmpty()) {
			return new ArrayList<Object>();
		}
		if ("0".equals(raw)) {
			return new ArrayList<Object>();
		}
		try {
			return objectMapper.readValue(raw, Object.class);
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	private String normalizeConfigForRedisStorage(Object configRaw) {
		if (configRaw == null) {
			return "";
		}
		if (configRaw instanceof CharSequence cs) {
			return cs.toString();
		}
		if (configRaw instanceof Number n) {
			return String.valueOf(n);
		}
		if (configRaw instanceof Boolean b) {
			return String.valueOf(b);
		}
		if (configRaw instanceof Map<?, ?> || configRaw instanceof Collection<?> || configRaw.getClass().isArray()) {
			try {
				return objectMapper.writeValueAsString(configRaw);
			} catch (JsonProcessingException e) {
				throw new BadRequestException("配置序列化失败");
			}
		}
		try {
			return objectMapper.writeValueAsString(configRaw);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("配置序列化失败");
		}
	}

	private static String redisKey(long companyId) {
		return REDIS_KEY_PREFIX + sha1Hex(String.valueOf(companyId));
	}

	private static String sha1Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] d = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(d.length * 2);
			for (byte b : d) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
