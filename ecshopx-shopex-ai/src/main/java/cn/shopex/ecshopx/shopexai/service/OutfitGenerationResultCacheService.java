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

package cn.shopex.ecshopx.shopexai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class OutfitGenerationResultCacheService {

	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public OutfitGenerationResultCacheService(
			StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public void writePendingStatus(String cacheKey, int ttlSeconds, OutfitGenerationPendingCacheValue pending) {
		try {
			String json = objectMapper.writeValueAsString(pending);
			stringRedisTemplate
					.opsForValue()
					.set(cacheKey, json, Duration.ofSeconds(ttlSeconds));
		} catch (Exception e) {
			throw new IllegalStateException("failed to write pending outfit status", e);
		}
	}

	public void writePendingStatus(String cacheKey, int ttlSeconds) {
		writePendingStatus(cacheKey, ttlSeconds, OutfitGenerationPendingCacheValue.pendingNow());
	}

	public void saveResult(String cacheKey, Map<String, Object> body, int ttlSeconds) {
		try {
			String json = objectMapper.writeValueAsString(body);
			stringRedisTemplate
					.opsForValue()
					.set(cacheKey, json, Duration.ofSeconds(ttlSeconds));
		} catch (Exception e) {
			throw new IllegalStateException("failed to save outfit generation result", e);
		}
	}
}
