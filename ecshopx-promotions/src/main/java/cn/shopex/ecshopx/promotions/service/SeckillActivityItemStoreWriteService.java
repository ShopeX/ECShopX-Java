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

package cn.shopex.ecshopx.promotions.service;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SeckillActivityItemStoreWriteService {

	private static final Logger log = LoggerFactory.getLogger(SeckillActivityItemStoreWriteService.class);

	private final StringRedisTemplate stringRedisTemplate;

	public SeckillActivityItemStoreWriteService(StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	public void hsetStore(long companyId, long seckillId, long itemId, int store) {
		String key = "seckillActivityItemStore:" + companyId + ":" + seckillId;
		String field = "store_" + itemId;
		log.debug(
				"[DEBUG] SeckillActivityItemStoreWriteService hset companyId={} seckillId={} itemId={} store={}",
				companyId,
				seckillId,
				itemId,
				store);
		stringRedisTemplate.opsForHash().put(key, field, String.valueOf(store));
	}

	public void expireAtEndPlusOneDay(long companyId, long seckillId, int activityEndTimeEpochSeconds) {
		String key = "seckillActivityItemStore:" + companyId + ":" + seckillId;
		long expireAt = activityEndTimeEpochSeconds + 86400L;
		stringRedisTemplate.expireAt(key, Instant.ofEpochSecond(expireAt));
	}
}
