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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
				Map.of("username", "admin", "password", "secret", "logintype", "merchant"));

		assertTrue(result.success());
		assertEquals("op.jwt", result.jwt());

		RecordedRequest req = server.takeRequest();
		assertEquals("POST", req.getMethod());
		assertEquals("/api/v1/operator/login", req.getPath());
		JsonNode body = objectMapper.readTree(req.getBody().readUtf8());
		assertEquals("admin", body.path("username").asText());
		assertEquals("secret", body.path("password").asText());
		assertEquals("merchant", body.path("logintype").asText());
	}

	@Test
	void operatorLoginDefaultsLogintypeToAdmin() throws Exception {
		server.enqueue(jsonOk("{\"token\":\"op.jwt\"}"));

		LoginBridgeService.LoginResult result = loginBridge.login(
				TokenKind.OPERATOR, Map.of("username", "a", "password", "b"));

		assertTrue(result.success());
		JsonNode body = objectMapper.readTree(server.takeRequest().getBody().readUtf8());
		assertEquals("admin", body.path("logintype").asText());
	}

	@Test
	void rejectsUnsupportedLogintype() {
		assertThrows(IllegalArgumentException.class,
				() -> loginBridge.login(TokenKind.OPERATOR,
						Map.of("username", "a", "password", "b", "logintype", "member")));
		assertEquals(0, server.getRequestCount());
	}

	@Test
	void rejectsNonOperatorIdentity() {
		assertThrows(IllegalArgumentException.class,
				() -> loginBridge.login(TokenKind.POS, Map.of("employee_no", "E001", "password", "pwd")));
		assertThrows(IllegalArgumentException.class,
				() -> loginBridge.refresh(TokenKind.MEMBER, "mem.jwt"));
		assertEquals(0, server.getRequestCount());
	}

	@Test
	void operatorRefreshReadsAuthorizationHeader() throws Exception {
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.setHeader("Authorization", "Bearer op.refreshed")
				.setBody("{\"code\":200,\"msg\":\"success\",\"data\":{\"result\":true}}"));

		LoginBridgeService.RefreshResult result = loginBridge.refresh(TokenKind.OPERATOR, "op.old");

		assertTrue(result.success());
		assertEquals("op.refreshed", result.jwt());

		RecordedRequest req = server.takeRequest();
		assertEquals("GET", req.getMethod());
		assertEquals("/api/v1/token/refresh", req.getPath());
		assertEquals("Bearer op.old", req.getHeader("Authorization"));
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
