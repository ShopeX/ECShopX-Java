package cn.shopex.ecshopx.employeepurchase.service.passphrase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PassphraseVerifiedRedisServiceTest {

	@Test
	@DisplayName("Redis key 复用 PHP 格式 ep_passphrase_verified")
	void buildKeyMatchesPhpFormat() {
		assertEquals(
				"ep_passphrase_verified:1:150:101:1001",
				PassphraseVerifiedRedisService.buildKey(1L, 150L, 101L, 1001L));
	}

	@Test
	@DisplayName("TTL 夹在 [3600, 90天]")
	void ttlClampedBetweenMinAndMax() {
		long now = System.currentTimeMillis() / 1000L;
		long ttlShort = PassphraseVerifiedRedisService.resolveTtlSeconds(now - 90000L);
		assertEquals(3600L, ttlShort);

		long ttlLong = PassphraseVerifiedRedisService.resolveTtlSeconds(now + 200L * 86400L);
		assertEquals(90L * 86400L, ttlLong);

		long end = now + 7L * 86400L;
		long ttlMid = PassphraseVerifiedRedisService.resolveTtlSeconds(end);
		assertTrue(ttlMid >= 3600L && ttlMid <= 90L * 86400L);
	}
}
