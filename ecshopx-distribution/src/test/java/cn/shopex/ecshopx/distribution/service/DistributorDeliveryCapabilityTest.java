package cn.shopex.ecshopx.distribution.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DistributorDeliveryCapabilityTest {

	@Test
	void expressOnly_usesLogisticsStore() {
		DistributorDeliveryCapability cap = DistributorDeliveryCapability.of(true, false, false);
		assertEquals(DistributorDeliveryCapability.DisplayMode.LOGISTICS_ONLY, cap.displayMode());
		assertEquals(30, cap.combineDisplayStore(10, 30));
	}

	@Test
	void selfDeliveryOnly_usesLocalStore() {
		DistributorDeliveryCapability cap = DistributorDeliveryCapability.of(false, true, false);
		assertEquals(DistributorDeliveryCapability.DisplayMode.LOCAL_ONLY, cap.displayMode());
		assertEquals(10, cap.combineDisplayStore(10, 30));
	}

	@Test
	void zitiOnly_usesLocalStore() {
		DistributorDeliveryCapability cap = DistributorDeliveryCapability.of(false, false, true);
		assertEquals(DistributorDeliveryCapability.DisplayMode.LOCAL_ONLY, cap.displayMode());
		assertEquals(10, cap.combineDisplayStore(10, 30));
	}

	@Test
	void expressAndLocal_sums() {
		DistributorDeliveryCapability cap = DistributorDeliveryCapability.of(true, false, true);
		assertEquals(DistributorDeliveryCapability.DisplayMode.SUM, cap.displayMode());
		assertEquals(40, cap.combineDisplayStore(10, 30));
	}

	@Test
	void noneConfigured_fallsBackToSum() {
		DistributorDeliveryCapability cap = DistributorDeliveryCapability.of(false, false, false);
		assertEquals(DistributorDeliveryCapability.DisplayMode.SUM, cap.displayMode());
		assertEquals(40, cap.combineDisplayStore(10, 30));
	}

	@Test
	void noPlatformSelf_logisticsOnlyFallback_usesSupplierStore() {
		DistributorDeliveryCapability cap = DistributorDeliveryCapability.logisticsOnlyFallback();
		assertEquals(DistributorDeliveryCapability.DisplayMode.LOGISTICS_ONLY, cap.displayMode());
		assertEquals(30, cap.combineDisplayStore(10, 30));
	}
}
