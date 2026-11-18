package cn.shopex.ecshopx.mcp.tool.resolve;

import cn.shopex.ecshopx.mcp.config.McpProperties;
import cn.shopex.ecshopx.mcp.loopback.ApiLoopbackClient;
import cn.shopex.ecshopx.mcp.tool.catalog.MenuApiCatalog;
import cn.shopex.ecshopx.mcp.tool.catalog.MenuApiCatalogLoader;
import cn.shopex.ecshopx.mcp.tool.catalog.MenuApiCatalogs;
import cn.shopex.ecshopx.mcp.tool.catalog.MenuApiEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminApiCallToolCallbackTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Test
	void rejectsPathWithoutApiV1Prefix() {
		AdminApiCallToolCallback cb = classpathCallback(new McpProperties());
		assertThrows(IllegalArgumentException.class,
				() -> cb.call("{\"method\":\"GET\",\"path\":\"/evil\"}"));
	}

	@Test
	void rejectsUnknownCatalogPath() {
		AdminApiCallToolCallback cb = classpathCallback(new McpProperties());
		assertThrows(IllegalArgumentException.class,
				() -> cb.call("{\"method\":\"GET\",\"path\":\"/api/v1/no-such\"}"));
	}

	@Test
	void rejectsDestructiveRisk() {
		var catalog = MenuApiCatalogs.of(List.of(new MenuApiEntry(
				List.of("admin"), List.of("admin"), "DELETE", "/api/v1/wipe", "destructive", List.of())));
		AdminApiCallToolCallback cb = new AdminApiCallToolCallback(
				catalog, new ApiLoopbackClient(new McpProperties(), RestClient.builder()), MAPPER);
		assertThrows(IllegalArgumentException.class,
				() -> cb.call("{\"method\":\"DELETE\",\"path\":\"/api/v1/wipe\",\"force\":true}"));
	}

	@Test
	void rejectsWriteWithoutConfirm() throws Exception {
		try (MockWebServer server = permissionServer("tradenormalorders")) {
			AdminApiCallToolCallback cb = callbackWithServer(server, MenuApiCatalogLoader.loadClasspath());
			assertThrows(IllegalArgumentException.class,
					() -> cb.call(
							"{\"method\":\"PUT\",\"path\":\"/api/v1/remarks/{orderId}\",\"orderId\":\"1\",\"remark\":\"x\"}"));
		}
	}

	@Test
	void rejectsWhenMenuDisjointAndNotForced() throws Exception {
		try (MockWebServer server = permissionServer("unrelated_menu")) {
			AdminApiCallToolCallback cb = callbackWithServer(server, MenuApiCatalogLoader.loadClasspath());
			assertThrows(IllegalArgumentException.class,
					() -> cb.call("{\"method\":\"GET\",\"path\":\"/api/v1/orders\"}"));
		}
	}

	@Test
	void successfulGetWithMenuHit() throws Exception {
		MockWebServer server = new MockWebServer();
		server.enqueue(permissionBody("tradenormalorders"));
		server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"items\":[]}"));
		server.start();
		AdminApiCallToolCallback cb = callbackWithServer(server, MenuApiCatalogLoader.loadClasspath());
		try {
			String result = cb.call("{\"method\":\"GET\",\"path\":\"/api/v1/orders\",\"page\":1}");
			assertTrue(result.contains("items"));

			RecordedRequest permission = server.takeRequest();
			assertEquals("/api/v1/permission", permission.getPath());
			RecordedRequest api = server.takeRequest();
			assertEquals("GET", api.getMethod());
			assertEquals("/api/v1/orders?page=1", api.getPath());
			assertFalse(api.getRequestUrl().queryParameterNames().contains("confirm"));
			assertFalse(api.getRequestUrl().queryParameterNames().contains("force"));
		}
		finally {
			server.shutdown();
		}
	}

	private static MockResponse permissionBody(String key) {
		return new MockResponse().setResponseCode(200)
				.setBody("{\"data\":[{\"permission\":\"" + key + "\",\"url\":\"" + key + "\"}]}");
	}

	private static MockWebServer permissionServer(String key) throws Exception {
		MockWebServer server = new MockWebServer();
		server.enqueue(permissionBody(key));
		server.start();
		return server;
	}

	private static AdminApiCallToolCallback classpathCallback(McpProperties props) {
		return new AdminApiCallToolCallback(
				MenuApiCatalogLoader.loadClasspath(),
				new ApiLoopbackClient(props, RestClient.builder()),
				MAPPER);
	}

	private static AdminApiCallToolCallback callbackWithServer(MockWebServer server, MenuApiCatalog catalog) {
		McpProperties props = new McpProperties();
		props.setApiBaseUrl(server.url("/").toString().replaceAll("/$", ""));
		return new AdminApiCallToolCallback(catalog, new ApiLoopbackClient(props, RestClient.builder()), MAPPER);
	}
}