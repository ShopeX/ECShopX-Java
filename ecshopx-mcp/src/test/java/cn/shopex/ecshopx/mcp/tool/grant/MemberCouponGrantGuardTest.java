package cn.shopex.ecshopx.mcp.tool.grant;

import cn.shopex.ecshopx.mcp.config.McpProperties;
import cn.shopex.ecshopx.mcp.loopback.ApiLoopbackClient;
import cn.shopex.ecshopx.mcp.tool.LoopbackToolExecutor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberCouponGrantGuardTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Test
	void rejectsEmptyOrOversizedUserids() {
		McpProperties props = new McpProperties();
		props.setCouponGrantMaxRecipients(2);
		MemberCouponGrantToolCallback cb = new MemberCouponGrantToolCallback(
				new LoopbackToolExecutor(new ApiLoopbackClient(props, RestClient.builder())), props);
		assertThrows(IllegalArgumentException.class,
				() -> cb.call("{\"confirm\":true,\"userids\":[],\"couponsids\":[1]}"));
		assertThrows(IllegalArgumentException.class,
				() -> cb.call("{\"confirm\":true,\"userids\":[1,2,3],\"couponsids\":[9]}"));
	}

	@Test
	void successPostsGrantBodyWithoutConfirm() throws Exception {
		MockWebServer server = new MockWebServer();
		server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\":true}"));
		server.start();
		McpProperties props = new McpProperties();
		props.setApiBaseUrl(server.url("/").toString().replaceAll("/$", ""));
		MemberCouponGrantToolCallback cb = new MemberCouponGrantToolCallback(
				new LoopbackToolExecutor(new ApiLoopbackClient(props, RestClient.builder()), MAPPER), props);
		try {
			String result = cb.call("{\"confirm\":true,\"userids\":[1],\"couponsids\":[2]}");
			assertTrue(result.contains("ok"));

			RecordedRequest request = server.takeRequest();
			assertEquals("POST", request.getMethod());
			assertEquals("/api/v1/promotions/activity/give", request.getPath());
			JsonNode body = MAPPER.readTree(request.getBody().readUtf8());
			assertFalse(body.has("confirm"));
			assertEquals(1, body.get("userids").size());
			assertEquals(1, body.get("userids").get(0).asInt());
			assertEquals(1, body.get("couponsids").size());
			assertEquals(2, body.get("couponsids").get(0).asInt());
		}
		finally {
			server.shutdown();
		}
	}
}