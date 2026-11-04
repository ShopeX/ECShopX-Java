package cn.shopex.ecshopx.shuyun.auth;

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

@ExtendWith(MockitoExtension.class)
class InboundSignedCallbackReplayGuardTest {

	@Mock
	private StringRedisTemplate stringRedisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private InboundSignedCallbackReplayGuard guard;

	@BeforeEach
	void setUp() {
		lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		guard = new InboundSignedCallbackReplayGuard(stringRedisTemplate);
	}

	@Test
	void assertFreshAndOnce_whenTimestampSkewTooLarge_rejects() {
		long nowMs = 1_710_000_000_000L;
		String old = String.valueOf(nowMs - 301_000L);

		assertThat(guard.assertFreshAndOnce("sig", old, 300, 600, () -> nowMs)).isFalse();
	}

	@Test
	void assertFreshAndOnce_whenFirstAcquire_accepts() {
		long nowMs = 1_710_000_000_000L;
		String ts = String.valueOf(nowMs);
		when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);

		assertThat(guard.assertFreshAndOnce("sig", ts, 300, 600, () -> nowMs)).isTrue();

		ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
		verify(valueOperations).setIfAbsent(keyCaptor.capture(), eq("1"), eq(Duration.ofSeconds(600)));
		assertThat(keyCaptor.getValue()).startsWith("shuyun:cb:replay:");
	}

	@Test
	void assertFreshAndOnce_whenRedisAlreadySeen_rejects() {
		long nowMs = 1_710_000_000_000L;
		String ts = String.valueOf(nowMs);
		when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(false);

		assertThat(guard.assertFreshAndOnce("sig", ts, 300, 600, () -> nowMs)).isFalse();
	}

	@Test
	void assertFreshAndOnce_whenTimestampMissing_rejects() {
		assertThat(guard.assertFreshAndOnce("sig", "", 300, 600, System::currentTimeMillis)).isFalse();
		assertThat(guard.assertFreshAndOnce("sig", null, 300, 600, System::currentTimeMillis)).isFalse();
	}
}
