package cn.shopex.ecshopx.mcp.tool;

import cn.shopex.ecshopx.mcp.config.McpProperties;
import cn.shopex.ecshopx.mcp.loopback.ApiLoopbackClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoopbackToolExecutorTest {

	@Test
	void maps403ToToolError() throws Exception {
		MockWebServer server = new MockWebServer();
		server.enqueue(new MockResponse().setResponseCode(403).setBody("{\"message\":\"denied\"}"));
		server.start();
		LoopbackToolExecutor executor = executorFor(server);
		HttpToolDeclaration declGetMe = HttpToolDeclaration.read(
				"probe_get",
				"Probe GET",
				"{}",
				"GET",
				"/api/v1/pos/auth/me");
		ToolResult result = executor.execute(declGetMe, "{}", "Bearer x");
		assertTrue(result.isError());
		assertTrue(result.text().contains("403"));
		assertTrue(result.text().contains("denied"));
		server.shutdown();
	}

	@Test
	void maps200ToSuccess() throws Exception {
		MockWebServer server = new MockWebServer();
		server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"userId\":1}"));
		server.start();
		LoopbackToolExecutor executor = executorFor(server);
		HttpToolDeclaration decl = HttpToolDeclaration.read(
				"probe_get", "Probe GET", "{}", "GET", "/api/v1/pos/auth/me");
		ToolResult result = executor.execute(decl, "{}", "Bearer tok");
		assertFalse(result.isError());
		assertTrue(result.text().contains("userId"));
		RecordedRequest req = server.takeRequest();
		assertEquals("Bearer tok", req.getHeader("Authorization"));
		server.shutdown();
	}

	@Test
	void substitutesPathTemplateFromArgs() throws Exception {
		MockWebServer server = new MockWebServer();
		server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));
		server.start();
		LoopbackToolExecutor executor = executorFor(server);
		HttpToolDeclaration decl = HttpToolDeclaration.read(
				"get_item", "Get item", "{}", "GET", "/api/v1/items/{id}");
		executor.execute(decl, "{\"id\":\"99\"}", null);
		RecordedRequest req = server.takeRequest();
		assertEquals("/api/v1/items/99", req.getPath());
		server.shutdown();
	}

	@Test
	void ordersSearchInjectsAdminUiDefaultsIntoQuery() throws Exception {
		MockWebServer server = new MockWebServer();
		server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"list\":[]}"));
		server.start();
		LoopbackToolExecutor executor = executorFor(server);
		HttpToolDeclaration decl = HttpToolDeclaration.read(
				"orders_search", "orders", "{}", "GET", "/api/v1/orders");
		executor.execute(decl, "{\"status\":\"notship\"}", "Bearer t");
		RecordedRequest req = server.takeRequest();
		String path = req.getPath();
		assertTrue(path.contains("order_status=notship"));
		assertTrue(path.contains("order_type=normal"));
		assertTrue(path.contains("order_class_exclude="));
		assertTrue(path.contains("pageSize=20"));
		assertFalse(path.contains("?status=") || path.contains("&status="));
		server.shutdown();
	}

	@Test
	void goodsItemsSearchInjectsPhysicalListDefaults() throws Exception {
		MockWebServer server = new MockWebServer();
		server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"list\":[]}"));
		server.start();
		LoopbackToolExecutor executor = executorFor(server);
		HttpToolDeclaration decl = HttpToolDeclaration.read(
				"goods_items_search", "goods", "{}", "GET", "/api/v1/goods/items");
		executor.execute(decl, "{}", "Bearer t");
		RecordedRequest req = server.takeRequest();
		String path = req.getPath();
		assertTrue(path.contains("item_type=normal"));
		assertTrue(path.contains("page=1"));
		assertTrue(path.contains("pageSize=20"));
		server.shutdown();
	}

	private static LoopbackToolExecutor executorFor(MockWebServer server) {
		McpProperties props = new McpProperties();
		props.setApiBaseUrl(server.url("/").toString().replaceAll("/$", ""));
		ApiLoopbackClient client = new ApiLoopbackClient(props, RestClient.builder());
		return new LoopbackToolExecutor(client);
	}
}