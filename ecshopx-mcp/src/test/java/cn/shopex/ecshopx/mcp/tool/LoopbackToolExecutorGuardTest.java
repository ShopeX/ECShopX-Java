package cn.shopex.ecshopx.mcp.tool;

import cn.shopex.ecshopx.mcp.config.McpProperties;
import cn.shopex.ecshopx.mcp.loopback.ApiLoopbackClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LoopbackToolExecutorGuardTest {

	@Test
	void writeToolRejectsMissingConfirm() {
		HttpToolDeclaration decl = new HttpToolDeclaration(
				"order_cancel", "cancel",
				"{\"type\":\"object\",\"properties\":{\"order_id\":{\"type\":\"string\"},\"confirm\":{\"type\":\"boolean\"}},\"required\":[\"order_id\",\"confirm\"]}",
				"POST", "/api/v1/order/{order_id}/cancel", true, Set.of("order_id"));
		LoopbackToolExecutor executor = new LoopbackToolExecutor(
				new ApiLoopbackClient(new McpProperties(), RestClient.builder()));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> executor.execute(decl, "{\"order_id\":\"1\"}", "Bearer t"));
		assertTrue(ex.getMessage().contains("confirm"));
	}

	@Test
	void writeToolStripsConfirmFromBody() throws Exception {
		MockWebServer server = new MockWebServer();
		server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\":true}"));
		server.start();
		McpProperties props = new McpProperties();
		props.setApiBaseUrl(server.url("/").toString().replaceAll("/$", ""));
		HttpToolDeclaration decl = new HttpToolDeclaration(
				"order_cancel", "cancel", "{}", "POST",
				"/api/v1/order/{order_id}/cancel", true, Set.of("order_id"));
		LoopbackToolExecutor executor = new LoopbackToolExecutor(
				new ApiLoopbackClient(props, RestClient.builder()));
		ToolResult result = executor.execute(decl,
				"{\"order_id\":\"99\",\"confirm\":true,\"cancel_reason\":\"x\"}", "Bearer t");
		assertFalse(result.isError());
		RecordedRequest req = server.takeRequest();
		assertEquals("POST", req.getMethod());
		assertEquals("/api/v1/order/99/cancel", req.getPath());
		String body = req.getBody().readUtf8();
		assertFalse(body.contains("confirm"));
		assertTrue(body.contains("cancel_reason"));
		server.shutdown();
	}
}