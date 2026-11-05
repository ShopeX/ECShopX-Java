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

package cn.shopex.ecshopx.employeepurchase.service.passphrase;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.TooManyRequestsException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/** ECX-10917：未认证行为上报限流，防刷库。 */
@ExtendWith(MockitoExtension.class)
class PassphraseBehaviorRateLimitServiceTest {

	@Mock
	private StringRedisTemplate stringRedisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private PassphraseBehaviorRateLimitService service;

	@BeforeEach
	void setUp() {
		service = new PassphraseBehaviorRateLimitService(stringRedisTemplate);
	}

	@Test
	void withinLimit_passes() {
		when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.increment(any(String.class))).thenReturn(1L, 2L, 10L);

		assertThatCode(
						() ->
								service.assertAllowed(
										1L, 150L, 101L, null, "visitor-a", "1.2.3.4"))
				.doesNotThrowAnyException();
	}

	@Test
 void overLimit_throwsTooManyRequests() {
		when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.increment(any(String.class))).thenReturn(11L);

		assertThatThrownBy(
						() ->
								service.assertAllowed(
										1L, 150L, 101L, null, "visitor-a", "1.2.3.4"))
				.isInstanceOf(TooManyRequestsException.class)
				.hasMessage("操作过于频繁，请稍后再试");
	}

	@Test
	void firstHit_setsWindowTtl() {
		when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.increment(any(String.class))).thenReturn(1L);

		service.assertAllowed(1L, 150L, 101L, 1001L, null, null);

		verify(stringRedisTemplate).expire(any(String.class), eq(Duration.ofSeconds(60)));
	}

	@Test
	void subsequentHit_doesNotResetTtl() {
		when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.increment(any(String.class))).thenReturn(5L);

		service.assertAllowed(1L, 150L, 101L, 1001L, null, null);

		verify(stringRedisTemplate, never()).expire(any(String.class), any(Duration.class));
	}

	@Test
	void subjectPriority_userOverVisitorOverIp() {
		when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.increment(any(String.class))).thenReturn(1L);

		// user 优先：有 userId 时键含 u 前缀
		service.assertAllowed(1L, 150L, 101L, 1001L, "visitor-a", "1.2.3.4");
		verify(valueOperations).increment("ep_behavior_rl:1:150:101:u1001");

		// visitor 次之
		service.assertAllowed(1L, 150L, 101L, null, "visitor-a", "1.2.3.4");
		verify(valueOperations).increment("ep_behavior_rl:1:150:101:vvisitor-a");

		// IP 兜底
		service.assertAllowed(1L, 150L, 101L, null, null, "1.2.3.4");
		verify(valueOperations).increment("ep_behavior_rl:1:150:101:ip1.2.3.4");
	}
}
