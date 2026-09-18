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

package cn.shopex.ecshopx.employeepurchase.service;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class EmployeePurchaseEmailVcodeRedisService {

	private final StringRedisTemplate stringRedisTemplate;

	public EmployeePurchaseEmailVcodeRedisService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	private static String redisKey(@NonNull String email) {
		return "employee-purchase-email:" + email;
	}

	public void storeVcodeWithTtl(@NonNull String email, @NonNull String vcode, long ttlSeconds) {
		String key = redisKey(email);
		stringRedisTemplate.opsForValue().set(key, vcode, Duration.ofSeconds(ttlSeconds));
	}

	public boolean verifyAndConsume(@NonNull String email, @NonNull String vcode) {
		String key = redisKey(email);
		String stored = stringRedisTemplate.opsForValue().get(key);
		if (stored != null && stored.equals(vcode)) {
			stringRedisTemplate.delete(key);
			return true;
		}
		return false;
	}
}
