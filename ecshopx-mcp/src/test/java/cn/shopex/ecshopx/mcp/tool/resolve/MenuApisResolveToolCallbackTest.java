package cn.shopex.ecshopx.mcp.tool.resolve;

import cn.shopex.ecshopx.mcp.config.McpProperties;
import cn.shopex.ecshopx.mcp.loopback.ApiLoopbackClient;
import cn.shopex.ecshopx.mcp.tool.catalog.MenuApiCatalogLoader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuApisResolveToolCallbackTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Test
	void resolveByQueryIntersectsWithPermissionMenus() throws Exception {
		try (MockWebServer server = startPermissionServer("tradenormalorders")) {
			MenuApisResolveToolCallback cb = callback(server);
			String result = cb.call("{\"query\":\"orders\"}");
			JsonNode arr = MAPPER.readTree(result);
			assertTrue(arr.isArray());
			assertTrue(arr.size() > 0);
			boolean hasOrders = false;
			for (JsonNode node : arr) {
				if ("/api/v1/orders".equals(node.get("path").asText())) {
					hasOrders = true;
				}
			}
			assertTrue(hasOrders);
		}
	}

	@Test
	void resolveByRouteFiltersToVisibleMenus() throws Exception {
		try (MockWebServer server = startPermissionServer("tradenormalorders")) {
			MenuApisResolveToolCallback cb = callback(server);
			String result = cb.call("{\"route\":\"/api/v1/orders\"}");
			JsonNode arr = MAPPER.readTree(result);
			assertFalse(arr.isEmpty());
		}
	}

	@Test
	void rejectsWhenNoQueryOrRouteOrPermission() {
		McpProperties props = new McpProperties();
		MenuApisResolveToolCallback cb = new MenuApisResolveToolCallback(
				MenuApiCatalogLoader.loadClasspath(),
				new ApiLoopbackClient(props, RestClient.builder()),
				MAPPER);
		assertThrows(IllegalArgumentException.class, () -> cb.call("{}"));
	}

	private static MenuApisResolveToolCallback callback(MockWebServer server) {
		McpProperties props = new McpProperties();
		props.setApiBaseUrl(server.url("/").toString().replaceAll("/$", ""));
		return new MenuApisResolveToolCallback(
				MenuApiCatalogLoader.loadClasspath(),
				new ApiLoopbackClient(props, RestClient.builder()),
				MAPPER);
	}

	private static MockWebServer startPermissionServer(String permissionKey) throws Exception {
		MockWebServer server = new MockWebServer();
		String body = "{\"data\":[{\"permission\":\"" + permissionKey + "\",\"url\":\"" + permissionKey + "\"}]}";
		server.enqueue(new MockResponse().setResponseCode(200).setBody(body));
		server.start();
		return server;
	}
}