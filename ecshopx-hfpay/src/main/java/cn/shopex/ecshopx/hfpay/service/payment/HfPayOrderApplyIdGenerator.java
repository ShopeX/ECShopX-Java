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

package cn.shopex.ecshopx.hfpay.service.payment;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class HfPayOrderApplyIdGenerator {

	private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

	private final StringRedisTemplate redis;

	public HfPayOrderApplyIdGenerator(@Qualifier("companysRedisTemplate") StringRedisTemplate redis) {
		this.redis = redis;
	}

	public String nextOrderId() {
		Long n = redis.opsForValue().increment("hfpay_order_id");
		refreshEndOfDayTtl("hfpay_order_id");
		String day = LocalDate.now(ZoneId.systemDefault()).format(DAY);
		return day + String.format("%09d", n == null ? 0L : n);
	}

	public String nextApplyId() {
		Long n = redis.opsForValue().increment("hfpay_apply_id");
		refreshEndOfDayTtl("hfpay_apply_id");
		String day = LocalDate.now(ZoneId.systemDefault()).format(DAY);
		return day + String.format("%08d", n == null ? 0L : n);
	}

	private void refreshEndOfDayTtl(String key) {
		long sec = secondsUntilEndOfDay();
		redis.expire(key, Duration.ofSeconds(Math.max(sec, 1L)));
	}

	private static long secondsUntilEndOfDay() {
		ZoneId zone = ZoneId.systemDefault();
		ZonedDateTime now = ZonedDateTime.now(zone);
		ZonedDateTime end = now.toLocalDate().atTime(23, 59, 59).atZone(zone);
		return Duration.between(now, end).getSeconds();
	}
}
