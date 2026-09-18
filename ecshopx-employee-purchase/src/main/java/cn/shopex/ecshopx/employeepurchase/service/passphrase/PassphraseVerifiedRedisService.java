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

package cn.shopex.ecshopx.employeepurchase.service.passphrase;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class PassphraseVerifiedRedisService {

	private static final long MIN_TTL_SECONDS = 3600L;
	private static final long MAX_TTL_SECONDS = 90L * 86400L;

	private final StringRedisTemplate stringRedisTemplate;

	public PassphraseVerifiedRedisService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	public static String buildKey(long companyId, long activityId, long enterpriseId, long userId) {
		return "ep_passphrase_verified:"
				+ companyId
				+ ":"
				+ activityId
				+ ":"
				+ enterpriseId
				+ ":"
				+ userId;
	}

	public void markVerified(long companyId, long activityId, long enterpriseId, long userId, long activityEndEpochSec) {
		long ttl = resolveTtlSeconds(activityEndEpochSec);
		String key = buildKey(companyId, activityId, enterpriseId, userId);
		stringRedisTemplate.opsForValue().set(key, "1", java.time.Duration.ofSeconds(ttl));
	}

	public boolean isVerified(long companyId, long activityId, long enterpriseId, long userId) {
		if (userId <= 0L) {
			return false;
		}
		String key = buildKey(companyId, activityId, enterpriseId, userId);
		String v = stringRedisTemplate.opsForValue().get(key);
		return "1".equals(v);
	}

	static long resolveTtlSeconds(long activityEndEpochSec) {
		long now = System.currentTimeMillis() / 1000L;
		long ttl = activityEndEpochSec - now + 86400L;
		if (ttl < MIN_TTL_SECONDS) {
			return MIN_TTL_SECONDS;
		}
		if (ttl > MAX_TTL_SECONDS) {
			return MAX_TTL_SECONDS;
		}
		return ttl;
	}
}
