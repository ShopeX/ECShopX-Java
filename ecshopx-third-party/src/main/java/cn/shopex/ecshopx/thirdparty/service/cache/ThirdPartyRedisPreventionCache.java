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

package cn.shopex.ecshopx.thirdparty.service.cache;

import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ThirdPartyRedisPreventionCache {

	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public ThirdPartyRedisPreventionCache(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public Object getByPrevention(long companyId, String cacheName, int ttlSeconds, Supplier<Object> loader) {
		String preventionKey = "prevention:" + cacheName + ":" + sha1Hex(String.valueOf(companyId));
		String lockKey = md5Hex("lock:" + preventionKey);

		String raw;
		try {
			raw = stringRedisTemplate.opsForValue().get(preventionKey);
		} catch (DataAccessException e) {
			throw new ResourceException("地图配置缓存服务异常，请稍后重试");
		}

		int expireTime = 0;
		Object data = null;
		if (StringUtils.hasText(raw)) {
			try {
				JsonNode root = objectMapper.readTree(raw);
				if (root.isObject()) {
					JsonNode et = root.get("expire_time");
					if (et != null && et.isNumber()) {
						expireTime = et.intValue();
					}
					if (root.has("data")) {
						data = objectMapper.convertValue(root.get("data"), Object.class);
					}
				}
			} catch (JacksonException e) {
				expireTime = 0;
				data = null;
			}
		}

		long now = Instant.now().getEpochSecond();
		if (expireTime > 0 && expireTime >= now) {
			return data;
		}

		Boolean held;
		try {
			held = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(60));
		} catch (DataAccessException e) {
			throw new ResourceException("地图配置缓存服务异常，请稍后重试");
		}

		if (Boolean.TRUE.equals(held)) {
			try {
				if (ttlSeconds <= 0) {
					throw new ResourceException("操作失败！缓存时间必须大于0秒");
				}
				Object callbackData = loader.get();
				long writeNow = Instant.now().getEpochSecond();
				Map<String, Object> envelope = new LinkedHashMap<>(2);
				envelope.put("expire_time", writeNow + ttlSeconds);
				envelope.put("data", callbackData);
				String json;
				try {
					json = objectMapper.writeValueAsString(envelope);
				} catch (JacksonException e) {
					throw new ResourceException("地图配置缓存服务异常，请稍后重试");
				}
				try {
					stringRedisTemplate.opsForValue().set(preventionKey, json);
				} catch (DataAccessException e) {
					throw new ResourceException("地图配置缓存服务异常，请稍后重试");
				}
				return callbackData;
			} finally {
				try {
					stringRedisTemplate.delete(lockKey);
				} catch (RuntimeException ignored) {
					// best-effort lock release
				}
			}
		}

		return data;
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

	private static String md5Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
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
