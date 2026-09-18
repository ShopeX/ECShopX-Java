package cn.shopex.ecshopx.espier.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class LocalStoragePublicUrlTest {

	@Test
	@DisplayName("configured local.url always wins over request Host")
	void configuredUrlTakesPrecedence() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/espier/images");
		request.setScheme("http");
		request.setServerName("127.0.0.1");
		request.setServerPort(18080);
		request.addHeader("Host", "127.0.0.1:8080");

		StorageProperties props = new StorageProperties();
		props.getLocal().setUrl("https://testboss.luole.asia/storage");

		String url = LocalStoragePublicUrl.resolve(
				request, props, "demo-ecshopx/image/1/a.jpeg");

		assertEquals(
				"https://testboss.luole.asia/storage/demo-ecshopx/image/1/a.jpeg", url);
	}

	@Test
	@DisplayName("trailing slash on configured url is normalized")
	void configuredUrlTrailingSlashNormalized() {
		StorageProperties props = new StorageProperties();
		props.getLocal().setUrl("https://testboss.luole.asia/storage/");

		String url = LocalStoragePublicUrl.resolve(
				null, props, "/demo-ecshopx/image/1/a.jpeg");

		assertEquals(
				"https://testboss.luole.asia/storage/demo-ecshopx/image/1/a.jpeg", url);
	}

	@Test
	@DisplayName("blank configured url falls back to request Host")
	void blankConfiguredFallsBackToRequest() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/espier/images");
		request.setScheme("http");
		request.setServerName("127.0.0.1");
		request.setServerPort(18080);
		request.addHeader("Host", "127.0.0.1:8080");

		StorageProperties props = new StorageProperties();
		props.getLocal().setUrl("");

		String url = LocalStoragePublicUrl.resolve(
				request, props, "demo-ecshopx/image/1/a.jpeg");

		assertEquals(
				"http://127.0.0.1:8080/storage/demo-ecshopx/image/1/a.jpeg", url);
	}
}
