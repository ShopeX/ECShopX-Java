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

package cn.shopex.ecshopx.deposit.service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class DepositTradeIdGenerator {

	private static final long START_TIME = 1325347200L;

	private final StringRedisTemplate redis;

	public DepositTradeIdGenerator(@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redis) {
		this.redis = redis;
	}

	public String nextDepositTradeId(long userId) {
		long now = Instant.now().getEpochSecond();
		long day = (now - START_TIME) / 86400L;
		ZoneId zone = ZoneId.systemDefault();
		ZonedDateTime zdt = Instant.now().atZone(zone);
		long startOfDay = zdt.toLocalDate().atStartOfDay(zone).toEpochSecond();
		long minute = (now - startOfDay) / 90L;
		String ymd = DateTimeFormatter.ofPattern("yyyyMMdd").format(zdt);
		String key = "deposit_trade_id" + ymd;
		String field = String.valueOf(minute);
		Long redisId = redis.opsForHash().increment(key, field, 1L);
		redis.expire(key, Duration.ofSeconds(86400));
		String id =
				day
						+ pad(minute, 3)
						+ pad(redisId != null ? redisId : 0L, 5)
						+ pad(Math.floorMod(userId, 10_000L), 4);
		return "CZ" + id;
	}

	private static String pad(long value, int width) {
		String s = Long.toString(value);
		if (s.length() >= width) {
			return s;
		}
		return "0".repeat(width - s.length()) + s;
	}
}
