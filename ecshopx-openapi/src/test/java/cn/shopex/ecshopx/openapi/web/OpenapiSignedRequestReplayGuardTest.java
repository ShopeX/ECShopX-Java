package cn.shopex.ecshopx.openapi.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/** ECX-10869：OpenAPI 签名请求 Redis 幂等防重放。 */
@ExtendWith(MockitoExtension.class)
class OpenapiSignedRequestReplayGuardTest {

	@Mock
	private StringRedisTemplate stringRedisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private OpenapiSignedRequestReplayGuard guard;

	@BeforeEach
	void setUp() {
		lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		guard = new OpenapiSignedRequestReplayGuard(stringRedisTemplate);
	}

	@Test
	void assertOnce_whenFirstAcquire_acceptsAndUsesPrefixedKey() {
		when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);

		assertThat(guard.assertOnce("app1", "SIGABC", "1710000000", 600)).isTrue();

		ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
		verify(valueOperations).setIfAbsent(keyCaptor.capture(), eq("1"), eq(Duration.ofSeconds(600)));
		assertThat(keyCaptor.getValue()).startsWith("openapi:auth:replay:");
	}

	@Test
	void assertOnce_whenRedisAlreadySeen_rejects() {
		when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(false);

		assertThat(guard.assertOnce("app1", "SIGABC", "1710000000", 600)).isFalse();
	}

	@Test
	void assertOnce_whenParamsMissing_rejects() {
		assertThat(guard.assertOnce("", "SIG", "1710000000", 600)).isFalse();
		assertThat(guard.assertOnce("app1", "", "1710000000", 600)).isFalse();
		assertThat(guard.assertOnce("app1", "SIG", "", 600)).isFalse();
		assertThat(guard.assertOnce(null, "SIG", "1710000000", 600)).isFalse();
	}

	@Test
	void assertOnce_whenRedisThrows_rejectsFailClosed() {
		when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class)))
				.thenThrow(new RuntimeException("redis down"));

		assertThat(guard.assertOnce("app1", "SIGABC", "1710000000", 600)).isFalse();
	}
}
