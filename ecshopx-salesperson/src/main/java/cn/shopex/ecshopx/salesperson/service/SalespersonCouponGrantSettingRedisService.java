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

package cn.shopex.ecshopx.salesperson.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SalespersonCouponGrantSettingRedisService {

	private static final Logger log = LoggerFactory.getLogger(SalespersonCouponGrantSettingRedisService.class);

	private static final String REDIS_KEY_PREFIX = "coupongrantset";

	private final StringRedisTemplate redis;

	public SalespersonCouponGrantSettingRedisService(@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redis) {
		this.redis = redis;
	}

	public Map<String, String> readGrantSettingHash(long companyId) {
		String key = REDIS_KEY_PREFIX + companyId;
		try {
			Map<Object, Object> raw = redis.opsForHash().entries(key);
			if (raw.isEmpty()) {
				return Collections.emptyMap();
			}
			Map<String, String> out = new LinkedHashMap<>(raw.size());
			for (Map.Entry<Object, Object> e : raw.entrySet()) {
				Object v = e.getValue();
				out.put(String.valueOf(e.getKey()), v != null ? String.valueOf(v) : "");
			}
			return out;
		} catch (RuntimeException e) {
			log.warn("Redis HGETALL failed for key {}", key, e);
			return Collections.emptyMap();
		}
	}

	public void writeGrantLimitFields(long companyId, String limitCycle, String grantPerUserTotal, String grantTotal) {
		String key = REDIS_KEY_PREFIX + companyId;
		Map<String, String> map = new LinkedHashMap<>(3);
		map.put("limit_cycle", limitCycle != null ? limitCycle : "");
		map.put("grant_per_user_total", grantPerUserTotal != null ? grantPerUserTotal : "");
		map.put("grant_total", grantTotal != null ? grantTotal : "");
		try {
			redis.opsForHash().putAll(key, map);
		} catch (RuntimeException e) {
			log.warn("Redis putAll failed for key {}", key, e);
		}
	}
}
