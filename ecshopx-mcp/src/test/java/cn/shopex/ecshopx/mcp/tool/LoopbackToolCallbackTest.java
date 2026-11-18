package cn.shopex.ecshopx.mcp.tool;

import cn.shopex.ecshopx.mcp.config.McpProperties;
import cn.shopex.ecshopx.mcp.loopback.ApiLoopbackClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoopbackToolCallbackTest {

	@Test
	void throwsWhenExecutorReturnsToolError() throws Exception {
		MockWebServer server = new MockWebServer();
		server.enqueue(new MockResponse().setResponseCode(403).setBody("{\"message\":\"denied\"}"));
		server.start();
		LoopbackToolCallback callback = callbackFor(server);
		try {
			LoopbackToolExecutionException ex = assertThrows(LoopbackToolExecutionException.class,
					() -> callback.call("{}"));
			assertTrue(ex.getMessage().contains("403"));
			assertTrue(ex.getMessage().contains("denied"));
		}
		finally {
			server.shutdown();
		}
	}

	private static LoopbackToolCallback callbackFor(MockWebServer server) {
		McpProperties props = new McpProperties();
		props.setApiBaseUrl(server.url("/").toString().replaceAll("/$", ""));
		LoopbackToolExecutor executor = new LoopbackToolExecutor(
				new ApiLoopbackClient(props, RestClient.builder()));
		HttpToolDeclaration declaration = HttpToolDeclaration.read(
				"mcp_smoke_get", "Smoke GET", "{}", "GET", "/api/v1/pos/auth/me");
		ToolCallback toolCallback = ToolDeclarationRegistrar.toToolCallback(declaration, executor);
		return (LoopbackToolCallback) toolCallback;
	}
}