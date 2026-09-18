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

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 对齐 PHP MemberActivityAggregateService / MemberActivityItemsAggregateService 的 setnx 锁。
 * 同一资源的扣减与返还须使用相同 key（见各 AggregateService 的 lockKey 方法）。
 */
@Service
public class EmployeePurchaseAggregateRedisLockService {

	private final StringRedisTemplate stringRedisTemplate;

	public EmployeePurchaseAggregateRedisLockService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	public void withLock(String key, Runnable action) {
		acquireLock(key);
		try {
			action.run();
		} finally {
			stringRedisTemplate.delete(key);
		}
	}

	private void acquireLock(String key) {
		while (!Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(key, "1"))) {
			try {
				Thread.sleep(ThreadLocalRandom.current().nextInt(1, 1000));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new ResourceException("操作繁忙，请重试");
			}
		}
	}
}
