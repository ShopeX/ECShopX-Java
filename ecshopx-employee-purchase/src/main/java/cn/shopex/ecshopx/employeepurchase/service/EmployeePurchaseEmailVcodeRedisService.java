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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class EmployeePurchaseEmailVcodeRedisService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

	private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

	private final StringRedisTemplate stringRedisTemplate;

	public EmployeePurchaseEmailVcodeRedisService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	private static String redisKey(@NonNull String email) {
		return "employee-purchase-email:" + email;
	}

	private static String cooldownKey(long companyId, @NonNull String email) {
		return "employee-purchase-email-cd:" + companyId + ":" + email;
	}

	private static String dailyEmailKey(long companyId, @NonNull String email) {
		return "employee-purchase-email-day:" + companyId + ":" + LocalDate.now(SHANGHAI).format(DAY_FORMAT) + ":"
				+ email;
	}

	private static String dailyIpKey(long companyId, @NonNull String clientIp) {
		return "employee-purchase-email-ip:" + companyId + ":" + LocalDate.now(SHANGHAI).format(DAY_FORMAT) + ":"
				+ clientIp;
	}

	private long incrementDaily(String key) {
		Long count = stringRedisTemplate.opsForValue().increment(key);
		if (count != null && count == 1L) {
			stringRedisTemplate.expire(key, Duration.ofHours(24));
		}
		return count == null ? 0L : count;
	}

	public boolean hasSendCooldown(long companyId, @NonNull String email) {
		return Boolean.TRUE.equals(stringRedisTemplate.hasKey(cooldownKey(companyId, email)));
	}

	public void rememberSendCooldown(long companyId, @NonNull String email) {
		stringRedisTemplate.opsForValue().set(cooldownKey(companyId, email), "1", Duration.ofSeconds(60));
	}

	public long nextDailyEmailSendCount(long companyId, @NonNull String email) {
		return incrementDaily(dailyEmailKey(companyId, email));
	}

	public long nextDailyIpSendCount(long companyId, @NonNull String clientIp) {
		return incrementDaily(dailyIpKey(companyId, clientIp));
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
