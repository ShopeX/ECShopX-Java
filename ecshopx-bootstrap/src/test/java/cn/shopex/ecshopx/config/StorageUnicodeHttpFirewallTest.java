package cn.shopex.ecshopx.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.RequestRejectedException;

class StorageUnicodeHttpFirewallTest {

	private final HttpFirewall firewall = new StrictHttpFirewallConfig().strictHttpFirewall();

	@Test
	@DisplayName("/storage/** with Chinese filename is allowed")
	void storagePathWithChineseAllowed() {
		MockHttpServletRequest request = new MockHttpServletRequest(
				"GET",
				"/storage/demo-ecshopx/image/1/2026/09/03/f3829f216f2b3be909ac8600f086eb14微信图片_20260903093903_24_9.jpg.jpeg");
		assertDoesNotThrow(() -> firewall.getFirewalledRequest(request));
	}

	@Test
	@DisplayName("/storage/** with mojibake / special bytes in name is allowed")
	void storagePathWithSpecialCharsAllowed() {
		MockHttpServletRequest request = new MockHttpServletRequest(
				"GET",
				"/storage/demo-ecshopx/image/1/2026/08/26/1665642d076a7e585a5ee64135e00b51╨∞╧πΓ_║∩╠╥_04.jpg.jpeg");
		assertDoesNotThrow(() -> firewall.getFirewalledRequest(request));
	}

	@Test
	@DisplayName("non-storage API path with Chinese still rejected by StrictHttpFirewall")
	void nonStorageChineseRejected() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/espier/微信");
		assertThrows(RequestRejectedException.class, () -> firewall.getFirewalledRequest(request));
	}
}
