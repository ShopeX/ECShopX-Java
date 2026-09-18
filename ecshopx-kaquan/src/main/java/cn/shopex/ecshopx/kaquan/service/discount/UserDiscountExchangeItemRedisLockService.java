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

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserDiscountExchangeItemRedisLockService {

	private static final String KEY_PREFIX = "lock:discount:item:";

	private final StringRedisTemplate companysRedisTemplate;

	public UserDiscountExchangeItemRedisLockService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.companysRedisTemplate = companysRedisTemplate;
	}

	public void executeWithLockOrThrow(long companyId, long itemId, Runnable lockedAction) {
		String redisKey = KEY_PREFIX + companyId + ":" + itemId;
		boolean locked = false;
		for (int attempt = 0; attempt < 3; attempt++) {
			Boolean ok = companysRedisTemplate.opsForValue().setIfAbsent(redisKey, "1", Duration.ofSeconds(6));
			if (Boolean.TRUE.equals(ok)) {
				locked = true;
				break;
			}
			try {
				Thread.sleep(500L);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new ResourceException("兑换券使用失败，请稍后重试..");
			}
		}
		if (!locked) {
			throw new ResourceException("兑换券使用失败，请稍后重试..");
		}
		try {
			lockedAction.run();
		} finally {
			companysRedisTemplate.delete(redisKey);
		}
	}
}
