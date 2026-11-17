package cn.shopex.ecshopx.mcp.auth.oauth;

import cn.shopex.ecshopx.mcp.auth.TokenKind;
import cn.shopex.ecshopx.mcp.config.McpProperties;
import cn.shopex.ecshopx.mcp.loopback.ApiLoopbackClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginBridgeServiceTest {

	private MockWebServer server;

	private LoginBridgeService loginBridge;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() throws Exception {
		server = new MockWebServer();
		server.start();
		McpProperties props = new McpProperties();
		props.setApiBaseUrl(server.url("/").toString().replaceAll("/$", ""));
		ApiLoopbackClient client = new ApiLoopbackClient(props, RestClient.builder());
		loginBridge = new LoginBridgeService(client, objectMapper);
	}

	@AfterEach
	void tearDown() throws Exception {
		server.shutdown();
	}

	@Test
	void operatorLoginPostsCredentialsAndReadsDataToken() throws Exception {
		server.enqueue(jsonOk("{\"token\":\"op.jwt\"}"));

		LoginBridgeService.LoginResult result = loginBridge.login(
				TokenKind.OPERATOR,
				Map.of("username", "admin", "password", "secret", "logintype", "localadmin"));

		assertTrue(result.success());
		assertEquals("op.jwt", result.jwt());

		RecordedRequest req = server.takeRequest();
		assertEquals("POST", req.getMethod());
		assertEquals("/api/v1/operator/login", req.getPath());
		JsonNode body = objectMapper.readTree(req.getBody().readUtf8());
		assertEquals("admin", body.path("username").asText());
		assertEquals("secret", body.path("password").asText());
		assertEquals("localadmin", body.path("logintype").asText());
	}

	@Test
	void posLoginReadsTokenField() throws Exception {
		server.enqueue(jsonOk("{\"token\":\"pos.jwt\",\"employee_no\":\"E001\"}"));

		LoginBridgeService.LoginResult result = loginBridge.login(
				TokenKind.POS, Map.of("employee_no", "E001", "password", "pwd"));

		assertTrue(result.success());
		assertEquals("pos.jwt", result.jwt());

		RecordedRequest req = server.takeRequest();
		assertEquals("/api/v1/pos/auth/login", req.getPath());
	}

	@Test
	void memberLoginUsesH5PathAndRejectsFalseToken() throws Exception {
		server.enqueue(jsonOk("{\"token\":false}"));

		LoginBridgeService.LoginResult result = loginBridge.login(
				TokenKind.MEMBER,
				Map.of("company_id", "1", "username", "13800000000", "password", "pwd", "auth_type", "local"));

		assertFalse(result.success());

		RecordedRequest req = server.takeRequest();
		assertEquals("/api/v1/h5app/wxapp/login", req.getPath());
	}

	@Test
	void memberLoginExtractsJwtFromDataToken() throws Exception {
		server.enqueue(jsonOk("{\"token\":\"mem.jwt\"}"));

		LoginBridgeService.LoginResult result = loginBridge.login(
				TokenKind.MEMBER,
				Map.of("company_id", "1", "username", "13800000000", "password", "pwd"));

		assertTrue(result.success());
		assertEquals("mem.jwt", result.jwt());
	}

	@Test
	void non2xxResponseIsFailure() {
		server.enqueue(new MockResponse().setResponseCode(401).setBody("{\"code\":401,\"msg\":\"fail\"}"));

		LoginBridgeService.LoginResult result = loginBridge.login(
				TokenKind.OPERATOR, Map.of("username", "a", "password", "b"));

		assertFalse(result.success());
	}

	private MockResponse jsonOk(String dataJson) {
		return new MockResponse()
				.setHeader("Content-Type", "application/json")
				.setBody("{\"code\":200,\"msg\":\"success\",\"data\":" + dataJson + "}");
	}
}