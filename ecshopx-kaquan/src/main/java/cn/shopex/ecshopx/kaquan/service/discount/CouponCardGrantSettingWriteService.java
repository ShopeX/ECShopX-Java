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

package cn.shopex.ecshopx.kaquan.service.discount;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CouponCardGrantSettingWriteService {

	private static final Logger log = LoggerFactory.getLogger(CouponCardGrantSettingWriteService.class);

	private static final String REDIS_KEY_PREFIX = "coupongrantset";

	private final StringRedisTemplate redis;

	public CouponCardGrantSettingWriteService(@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redis) {
		this.redis = redis;
	}

	public boolean save(long companyId, Map<String, Object> requestData) {
		String coupons = resolveCouponsString(requestData);
		String limitCycle = optionalFieldString(requestData, "limit_cycle");
		String grantPerUserTotal = optionalFieldString(requestData, "grant_per_user_total");
		String grantTotal = optionalFieldString(requestData, "grant_total");

		Map<String, String> hash = new HashMap<>(4);
		hash.put("coupons", coupons);
		hash.put("limit_cycle", limitCycle);
		hash.put("grant_per_user_total", grantPerUserTotal);
		hash.put("grant_total", grantTotal);

		String redisKey = REDIS_KEY_PREFIX + companyId;
		try {
			redis.opsForHash().putAll(redisKey, hash);
			return true;
		} catch (RuntimeException e) {
			log.warn("Redis putAll failed for key {}", redisKey, e);
			return false;
		}
	}

	/**
	 * 读取当前店铺优惠券发放管理配置（Redis Hash 全量字段），与 {@link #save(long, Map)} 使用同一 key。
	 */
	public Map<String, String> loadAll(long companyId) {
		String redisKey = REDIS_KEY_PREFIX + companyId;
		try {
			Map<Object, Object> raw = redis.opsForHash().entries(redisKey);
			Map<String, String> entries = new LinkedHashMap<>();
			if (raw != null) {
				for (Map.Entry<Object, Object> e : raw.entrySet()) {
					Object v = e.getValue();
					entries.put(String.valueOf(e.getKey()), v == null ? "" : String.valueOf(v));
				}
			}
			return entries;
		} catch (RuntimeException e) {
			log.warn("Redis entries (HGETALL) failed for key {}", redisKey, e);
			return Collections.emptyMap();
		}
	}

	private static String resolveCouponsString(Map<String, Object> requestData) {
		Object couponsRaw = requestData != null ? requestData.get("coupons") : null;
		if (isCouponsFalsy(couponsRaw)) {
			return "";
		}
		return String.valueOf(couponsRaw);
	}

	private static boolean isCouponsFalsy(Object o) {
		if (o == null) {
			return true;
		}
		if (Boolean.FALSE.equals(o)) {
			return true;
		}
		if (o instanceof Number n) {
			return n.doubleValue() == 0.0 && !Double.isNaN(n.doubleValue());
		}
		if (o instanceof String s) {
			return s.isEmpty() || "0".equals(s);
		}
		if (o instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (o instanceof Map<?, ?> m) {
			return m.isEmpty();
		}
		return false;
	}

	private static String optionalFieldString(Map<String, Object> requestData, String key) {
		if (requestData == null) {
			return "";
		}
		Object value = requestData.get(key);
		if (value == null) {
			return "";
		}
		return String.valueOf(value);
	}
}
