package cn.shopex.ecshopx.shuyun.service.openplatform;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class MemberOpenPlatformSyncStateTest {

	@Test
	void registeredWhenWxappSyncAtPresent() {
		assertTrue(MemberOpenPlatformSyncState.isRegisteredWithOpenPlatform(
				Map.of("shuyun_open_online_wxapp_sync_at", 1)));
	}

	@Test
	void registeredWhenOfflineRegDistributorPresent() {
		assertTrue(MemberOpenPlatformSyncState.isRegisteredWithOpenPlatform(Map.of("offline_reg_distributor", 9)));
	}

	@Test
	void notRegisteredWhenMarkersAbsent() {
		assertFalse(MemberOpenPlatformSyncState.isRegisteredWithOpenPlatform(Map.of()));
		assertFalse(MemberOpenPlatformSyncState.isRegisteredWithOpenPlatform(
				Map.of("shuyun_open_online_wxapp_sync_at", 0, "offline_reg_distributor", 0)));
	}
}
