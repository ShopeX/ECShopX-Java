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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/** ECX-10950：冷却 60 秒，当日计数首次写入后 24 小时过期。 */
@ExtendWith(MockitoExtension.class)
class EmployeePurchaseEmailVcodeRedisServiceRateLimitTest {

	@Mock
	private StringRedisTemplate stringRedisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private EmployeePurchaseEmailVcodeRedisService service;

	@BeforeEach
	void setUp() {
		when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		service = new EmployeePurchaseEmailVcodeRedisService(stringRedisTemplate);
	}

	@Test
	void rememberSendCooldown_expiresAfter60Seconds() {
		service.rememberSendCooldown(11L, "a@example.com");

		verify(valueOperations)
				.set("employee-purchase-email-cd:11:a@example.com", "1", Duration.ofSeconds(60));
	}

	@Test
	void firstDailyEmailCount_expiresAfter24Hours() {
		String day = LocalDate.now(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.BASIC_ISO_DATE);
		String key = "employee-purchase-email-day:11:" + day + ":a@example.com";
		when(valueOperations.increment(key)).thenReturn(1L);

		assertThat(service.nextDailyEmailSendCount(11L, "a@example.com")).isEqualTo(1L);

		verify(stringRedisTemplate).expire(key, Duration.ofHours(24));
	}

	@Test
	void laterDailyIpCount_doesNotRefreshExpiry() {
		String day = LocalDate.now(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.BASIC_ISO_DATE);
		String key = "employee-purchase-email-ip:11:" + day + ":203.0.113.8";
		when(valueOperations.increment(key)).thenReturn(2L);

		assertThat(service.nextDailyIpSendCount(11L, "203.0.113.8")).isEqualTo(2L);

		verify(stringRedisTemplate, never()).expire(eq(key), any(Duration.class));
	}
}
