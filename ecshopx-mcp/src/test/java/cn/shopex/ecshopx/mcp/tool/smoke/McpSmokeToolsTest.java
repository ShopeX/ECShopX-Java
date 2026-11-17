package cn.shopex.ecshopx.mcp.tool.smoke;

import cn.shopex.ecshopx.mcp.config.McpProperties;
import cn.shopex.ecshopx.mcp.loopback.ApiLoopbackClient;
import cn.shopex.ecshopx.mcp.tool.HttpToolDeclaration;
import cn.shopex.ecshopx.mcp.tool.LoopbackToolExecutor;
import cn.shopex.ecshopx.mcp.tool.ToolDeclarationRegistrar;
import cn.shopex.ecshopx.mcp.tool.ToolAuthContext;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpSmokeToolsTest {

	@Test
	void smokeDeclarationUsesConfiguredSmokePath() {
		McpProperties properties = new McpProperties();
		properties.setSmokePath("/api/v1/custom/smoke");
		HttpToolDeclaration declaration = McpSmokeTools.smokeDeclaration(properties);
		assertEquals(McpSmokeTools.TOOL_NAME, declaration.name());
		assertEquals("GET", declaration.method());
		assertEquals("/api/v1/custom/smoke", declaration.pathTemplate());
	}

	@Test
	void smokeToolCallbackExecutesLoopbackGetWithBearer() throws Exception {
		MockWebServer server = new MockWebServer();
		server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\":true}"));
		server.start();

		McpProperties properties = new McpProperties();
		properties.setApiBaseUrl(server.url("/").toString().replaceAll("/$", ""));
		HttpToolDeclaration declaration = McpSmokeTools.smokeDeclaration(properties);
		LoopbackToolExecutor executor = new LoopbackToolExecutor(
				new ApiLoopbackClient(properties, RestClient.builder()));
		ToolCallback callback = ToolDeclarationRegistrar.toToolCallback(declaration, executor);

		try {
			ToolAuthContext.setBearer("Bearer smoke-jwt");
			String callbackResult = callback.call("{}");
			assertTrue(callbackResult.contains("ok"));
		}
		finally {
			ToolAuthContext.clear();
		}

		RecordedRequest request = server.takeRequest();
		assertEquals("GET", request.getMethod());
		assertEquals("/api/v1/pos/auth/me", request.getPath());
		assertEquals("Bearer smoke-jwt", request.getHeader("Authorization"));
		server.shutdown();
	}
}