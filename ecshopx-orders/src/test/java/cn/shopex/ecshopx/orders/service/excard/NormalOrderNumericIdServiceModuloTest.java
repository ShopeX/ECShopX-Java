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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * ECX-11443: Redis seq must be truncated with {@code % 100000} so the order id never grows past
 * day + 3 + 5 + 4 digits (aligned with PromotionGroupsTeamIdGenerator).
 */
@ExtendWith(MockitoExtension.class)
class NormalOrderNumericIdServiceModuloTest {

	private static final long START_TIME = 1325347200L;

	@Mock private StringRedisTemplate redisTemplate;
	@Mock private HashOperations<String, Object, Object> hashOps;

	private NormalOrderNumericIdService service;

	@BeforeEach
	void setUp() {
		when(redisTemplate.opsForHash()).thenReturn(hashOps);
		when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);
		service = new NormalOrderNumericIdService(redisTemplate);
	}

	@Test
	@DisplayName("Redis seq >= 100000 keeps a fixed-length order id (seq % 100000)")
	void generate_whenRedisSeqOverflows_truncatesToFiveDigits() {
		when(hashOps.increment(anyString(), anyString(), anyLong())).thenReturn(100_001L);

		long id = service.generate(7L);

		String s = Long.toString(id);
		long day = (Instant.now().getEpochSecond() - START_TIME) / 86400L;
		// Without % 100000, seq is 6 digits → length day+13; with mod, day+12.
		assertThat(s).hasSize(String.valueOf(day).length() + 3 + 5 + 4);
		assertThat(s).endsWith("0007");
		// Seq digits (between minute and user) must be 00001 (= 100001 % 100000), not 100001.
		int dayLen = String.valueOf(day).length();
		assertThat(s.substring(dayLen + 3, dayLen + 8)).isEqualTo("00001");
	}
}
