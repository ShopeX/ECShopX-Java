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

package cn.shopex.ecshopx.orders.service.excard;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class NormalOrderNumericIdService {

	private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
	private static final long START_TIME = 1325347200L;
	private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(CN);

	private final StringRedisTemplate redisTemplate;

	public NormalOrderNumericIdService(@Qualifier("companysRedisTemplate") StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public long generate(long userId) {
		long time = Instant.now().getEpochSecond();
		long day = (time - START_TIME) / 86400L;
		ZonedDateTime z = Instant.ofEpochSecond(time).atZone(CN);
		long startOfDay = z.toLocalDate().atStartOfDay(CN).toEpochSecond();
		long minute = (time - startOfDay) / 90L;
		String dateKey = YMD.format(Instant.ofEpochSecond(time));
		long inc = ThreadLocalRandom.current().nextInt(1, 10);
		Long redisId = redisTemplate.opsForHash().increment(dateKey, String.valueOf(minute), inc);
		redisTemplate.expire(dateKey, java.time.Duration.ofDays(1));
		long rid = redisId != null ? redisId : inc;
		String idStr = day
				+ String.format("%03d", Math.min(minute, 999L))
				+ String.format("%05d", rid)
				+ String.format("%04d", Math.floorMod(userId, 10_000L));
		return Long.parseLong(idStr);
	}
}
