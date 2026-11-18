package cn.shopex.ecshopx.mcp.tool.resolve;

import cn.shopex.ecshopx.mcp.config.McpProperties;
import cn.shopex.ecshopx.mcp.loopback.ApiLoopbackClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MenusPermissionSupportTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Test
	void collectsMenuKeysFromPermissionTree() throws Exception {
		MockWebServer server = new MockWebServer();
		String body = """
				{"data":[{"url":"tradenormalorders","aliasName":"orders","permission":"tradenormalorders","name":"订单",
				"children":[{"path":"/api/v1/orders","auth":"read"}]}]}""";
		server.enqueue(new MockResponse().setResponseCode(200).setBody(body));
		server.start();
		McpProperties props = new McpProperties();
		props.setApiBaseUrl(server.url("/").toString().replaceAll("/$", ""));
		ApiLoopbackClient client = new ApiLoopbackClient(props, RestClient.builder());
		try {
			Set<String> keys = MenusPermissionSupport.fetchMenuKeys(client, null, MAPPER);
			assertTrue(keys.contains("tradenormalorders"));
			assertTrue(keys.contains("orders"));
			assertTrue(keys.contains("/api/v1/orders"));
			assertTrue(keys.contains("read"));
		}
		finally {
			server.shutdown();
		}
	}
}