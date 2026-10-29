package cn.shopex.ecshopx.distribution.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DistributorPlatformStoreBuySupportTest {

	@Test
	void isEnabledRawMatchesPhpSemantics() {
		assertTrue(DistributorPlatformStoreBuySupport.isEnabledRaw(Map.of("is_platform_store_buy", true)));
		assertTrue(DistributorPlatformStoreBuySupport.isEnabledRaw(Map.of("is_platform_store_buy", 1)));
		assertTrue(DistributorPlatformStoreBuySupport.isEnabledRaw(Map.of("is_platform_store_buy", "1")));
		assertTrue(DistributorPlatformStoreBuySupport.isEnabledRaw(Map.of("is_platform_store_buy", "true")));
		assertFalse(DistributorPlatformStoreBuySupport.isEnabledRaw(Map.of("is_platform_store_buy", false)));
		assertFalse(DistributorPlatformStoreBuySupport.isEnabledRaw(Map.of("is_platform_store_buy", 0)));
		assertFalse(DistributorPlatformStoreBuySupport.isEnabledRaw(Map.of()));
	}

	@Test
	void headquartersShipmentAllowsFastBuyWhenPlatformStoreBuyOff() {
		assertDoesNotThrow(
				() -> DistributorPlatformStoreBuySupport.assertShopadminFastBuyAllowedForItem(
						Map.of("is_total_store", true), false));
	}

	@Test
	void storeShipmentRequiresPlatformStoreBuyWhenOff() {
		ResourceException ex =
				assertThrows(
						ResourceException.class,
						() -> DistributorPlatformStoreBuySupport.assertShopadminFastBuyAllowedForItem(
								Map.of("is_total_store", false), false));
		assertTrue(ex.getMessage().contains("该店铺未开启云仓可购买"));
	}

	@Test
	void storeShipmentAllowsWhenPlatformStoreBuyOn() {
		assertDoesNotThrow(
				() -> DistributorPlatformStoreBuySupport.assertShopadminFastBuyAllowedForItem(
						Map.of("is_total_store", false), true));
	}
}
