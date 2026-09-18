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

package cn.shopex.ecshopx.shuyun.service.openplatform;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** Redis setIfAbsent 去重 / merge。 */
@Service
public class OpenPlatformDispatchDedupeService {

	private static final Logger log = LoggerFactory.getLogger(OpenPlatformDispatchDedupeService.class);

	private final StringRedisTemplate stringRedisTemplate;

	public OpenPlatformDispatchDedupeService(StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	/** @return true=首次获得锁可继续派发；false=窗口内已派发过 */
	public boolean tryAcquire(String key, int ttlSeconds) {
		if (ttlSeconds <= 0) {
			return true;
		}
		Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(ttlSeconds));
		boolean acquired = Boolean.TRUE.equals(ok);
		if (!acquired) {
			log.info("{} key={}", OrderSyncDispatchCacheKeys.LOG_DISPATCH_DEDUPED, key);
		}
		return acquired;
	}
}
