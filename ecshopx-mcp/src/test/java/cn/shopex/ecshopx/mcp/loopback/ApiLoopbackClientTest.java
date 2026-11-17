package cn.shopex.ecshopx.mcp.loopback;

import cn.shopex.ecshopx.mcp.config.McpProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiLoopbackClientTest {

	@Test
	void forwardsAuthorizationAndReturnsStatusBody() throws Exception {
		MockWebServer server = new MockWebServer();
		server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\":true}"));
		server.start();
		McpProperties props = new McpProperties();
		props.setApiBaseUrl(server.url("/").toString().replaceAll("/$", ""));
		ApiLoopbackClient client = new ApiLoopbackClient(props, RestClient.builder());
		LoopbackResponse resp = client.execute(new LoopbackRequest(
				"GET", "/api/v1/ping", Map.of(),
				Map.of("Authorization", "Bearer test-jwt"), null));
		assertEquals(200, resp.status());
		assertTrue(resp.body().contains("ok"));
		RecordedRequest req = server.takeRequest();
		assertEquals("Bearer test-jwt", req.getHeader("Authorization"));
		assertEquals("/api/v1/ping", req.getPath());
		server.shutdown();
	}

	@Test
	void rejectsPathWithoutLeadingSlash() {
		McpProperties props = new McpProperties();
		props.setApiBaseUrl("http://localhost:8080");
		ApiLoopbackClient client = new ApiLoopbackClient(props, RestClient.builder());
		assertThrows(IllegalArgumentException.class, () -> client.execute(
				new LoopbackRequest("GET", "api/v1/ping", Map.of(), Map.of(), null)));
	}

	@Test
	void rejectsPathContainingScheme() {
		McpProperties props = new McpProperties();
		props.setApiBaseUrl("http://localhost:8080");
		ApiLoopbackClient client = new ApiLoopbackClient(props, RestClient.builder());
		assertThrows(IllegalArgumentException.class, () -> client.execute(
				new LoopbackRequest("GET", "/http://evil/api", Map.of(), Map.of(), null)));
	}
}