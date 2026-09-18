package cn.shopex.ecshopx.espier.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UpgradeAgreementQueryServiceVersionCompareTest {

	@Test
	void contractTableA_mainPathAndPrerelease() {
		assertEquals(true, UpgradeAgreementQueryService.remoteNewerThanLocalForUpgrade("1.0.1", "1.0.0"));
		assertEquals(false, UpgradeAgreementQueryService.remoteNewerThanLocalForUpgrade("1.0.0", "1.0.0"));
		assertEquals(false, UpgradeAgreementQueryService.remoteNewerThanLocalForUpgrade("1.0.0", "1.0.1"));
		assertEquals(false, UpgradeAgreementQueryService.remoteNewerThanLocalForUpgrade("1.0.0-rc1", "1.0.0"));
		assertEquals(true, UpgradeAgreementQueryService.remoteNewerThanLocalForUpgrade("1.0.0", "1.0.0-rc1"));
		assertEquals(true, UpgradeAgreementQueryService.remoteNewerThanLocalForUpgrade("2.0.0", "-"));
		assertEquals(false, UpgradeAgreementQueryService.remoteNewerThanLocalForUpgrade("-", "1.0.0"));
		assertEquals(true, UpgradeAgreementQueryService.remoteNewerThanLocalForUpgrade("1.0.0", "-"));
		assertEquals(false, UpgradeAgreementQueryService.remoteNewerThanLocalForUpgrade("0.0.1-SNAPSHOT", "0.0.1"));
		assertEquals(true, UpgradeAgreementQueryService.remoteNewerThanLocalForUpgrade("0.0.1", "0.0.1-SNAPSHOT"));
		assertEquals(false, UpgradeAgreementQueryService.remoteNewerThanLocalForUpgrade("1.0.0-alpha", "1.0.0-beta"));
		assertEquals(true, UpgradeAgreementQueryService.remoteNewerThanLocalForUpgrade("1.0.0", "0.0.1-SNAPSHOT"));
	}

	@Test
	void contractTableB_plusBuildMetadata() {
		assertEquals(true, UpgradeAgreementQueryService.remoteNewerThanLocalForUpgrade("1.0.0+foo", "1.0.0"));
		assertEquals(false, UpgradeAgreementQueryService.remoteNewerThanLocalForUpgrade("1.0.0", "1.0.0+foo"));
	}
}
