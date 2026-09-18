package cn.shopex.ecshopx.employeepurchase.service.passphrase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class PassphraseParticipateQuotaRedisServiceTest {

	@Mock
	private StringRedisTemplate stringRedisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private PassphraseParticipateQuotaRedisService service;

	@BeforeEach
	void setUp() {
		service = new PassphraseParticipateQuotaRedisService(stringRedisTemplate);
	}

	private void stubValueOps() {
		when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
	}

	@Test
	@DisplayName("Redis key 复用 PHP 格式 ep:pquota")
	void buildKeyMatchesPhpFormat() {
		assertEquals("ep:pquota:1:150:101", PassphraseParticipateQuotaRedisService.buildKey(1L, 150L, 101L));
	}

	@Test
	@DisplayName("QA-08/D4: releaseOneSlot 对 key INCR")
	void releaseOneSlotIncrementsKey() {
		stubValueOps();
		service.releaseOneSlot(1L, 150L, 101L);
		verify(valueOperations).increment("ep:pquota:1:150:101");
	}

	@Test
	@DisplayName("syncRemainingQuota 写入剩余名额")
	void syncRemainingQuotaSetsValue() {
		stubValueOps();
		service.syncRemainingQuota(1L, 150L, 101L, 5);
		verify(valueOperations).set(eq("ep:pquota:1:150:101"), eq("5"));
	}
}
