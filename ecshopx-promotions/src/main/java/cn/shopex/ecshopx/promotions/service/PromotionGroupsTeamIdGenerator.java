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

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class PromotionGroupsTeamIdGenerator {

	private static final long START_EPOCH = 1325347200L;
	private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final StringRedisTemplate companysRedisTemplate;

	public PromotionGroupsTeamIdGenerator(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.companysRedisTemplate = companysRedisTemplate;
	}

	public String genId(long headMemberId) {
		long now = System.currentTimeMillis() / 1000L;
		long day = (now - START_EPOCH) / 86400L;
		long dayStart = LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
		long minute = (now - dayStart) / 90L;
		String ymd = LocalDate.now(ZoneId.systemDefault()).format(YMD);
		String hashKey = "group:" + ymd;
		Long redisId =
				companysRedisTemplate.opsForHash().increment(hashKey, String.valueOf(minute), ThreadLocalRandom.current().nextInt(1, 10));
		companysRedisTemplate.expire(hashKey, java.time.Duration.ofSeconds(86400));
		long seq = redisId == null ? 1L : redisId;
		return String.format(
				"%d%03d%05d%04d",
				day,
				minute % 1000L,
				seq % 100000L,
				Math.floorMod(headMemberId, 10000L));
	}
}
