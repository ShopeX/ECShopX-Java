package cn.shopex.ecshopx.mcp.auth.oauth;

import cn.shopex.ecshopx.mcp.auth.TokenKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthorizeControllerTest {

	private static final Pattern CODE_PARAM = Pattern.compile("[?&]code=([^&]+)");

	private MockMvc mockMvc;

	private InMemoryOAuthClientRegistry registry;

	private LoginBridgeService loginBridge;

	private AuthorizationCodeStore codeStore;

	private String clientId;

	@BeforeEach
	void setUp() {
		registry = new InMemoryOAuthClientRegistry();
		OAuthClientRegistration client = registry.register(
				List.of("http://localhost:5173/callback"), "test", "none");
		clientId = client.clientId();
		loginBridge = mock(LoginBridgeService.class);
		codeStore = new AuthorizationCodeStore();
		mockMvc = MockMvcBuilders.standaloneSetup(new AuthorizeController(
						registry, loginBridge, codeStore, new AuthorizePageRenderer()))
				.defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
				.build();
	}

	@Test
	void getAuthorizeRejectsUnknownClient() throws Exception {
		mockMvc.perform(get("/api/mcp/oauth/authorize")
						.param("response_type", "code")
						.param("client_id", "missing")
						.param("redirect_uri", "http://localhost:5173/callback")
						.param("code_challenge", "challenge"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void getAuthorizeRejectsPlainPkceMethod() throws Exception {
		mockMvc.perform(get("/api/mcp/oauth/authorize")
						.param("response_type", "code")
						.param("client_id", clientId)
						.param("redirect_uri", "http://localhost:5173/callback")
						.param("code_challenge", "challenge")
						.param("code_challenge_method", "plain"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void getAuthorizeReturnsHtmlForValidClient() throws Exception {
		mockMvc.perform(get("/api/mcp/oauth/authorize")
						.param("response_type", "code")
						.param("client_id", clientId)
						.param("redirect_uri", "http://localhost:5173/callback")
						.param("code_challenge", "challenge"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("请登录")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("ecxbg.jpg")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"logintype\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"merchant\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(">超级管理员<")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(">平台管理员<")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(">店员<")))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("value=\"dealer\""))))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(">经销商<"))))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Powered by shopex")))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("门店 POS"))))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("会员 MEMBER"))));
	}

	@Test
	void postLoginRedirectsWithCodeOnSuccess() throws Exception {
		when(loginBridge.login(any(TokenKind.class), any(Map.class)))
				.thenReturn(LoginBridgeService.LoginResult.success("biz.jwt"));

		MvcResult result = mockMvc.perform(post("/api/mcp/oauth/authorize/login")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("response_type", "code")
						.param("client_id", clientId)
						.param("redirect_uri", "http://localhost:5173/callback")
						.param("code_challenge", "challenge")
						.param("logintype", "distributor")
						.param("username", "admin")
						.param("password", "pwd"))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", org.hamcrest.Matchers.containsString("code=")))
				.andReturn();

		String location = result.getResponse().getHeader("Location");
		Matcher matcher = CODE_PARAM.matcher(location);
		assertTrue(matcher.find());
		String code = URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
		var stored = codeStore.find(code);
		assertTrue(stored.isPresent());
		assertEquals("biz.jwt", stored.get().businessJwt());
		assertEquals(TokenKind.OPERATOR, stored.get().tokenKind());
		assertNotNull(stored.get().codeChallenge());
	}

	@Test
	void postLoginRerendersPageOnFailure() throws Exception {
		when(loginBridge.login(any(TokenKind.class), any(Map.class)))
				.thenReturn(LoginBridgeService.LoginResult.failure("invalid credentials"));

		mockMvc.perform(post("/api/mcp/oauth/authorize/login")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("response_type", "code")
						.param("client_id", clientId)
						.param("redirect_uri", "http://localhost:5173/callback")
						.param("code_challenge", "challenge")
						.param("logintype", "admin")
						.param("username", "admin")
						.param("password", "bad"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("invalid credentials")));
	}

	@Test
	void postLoginRejectsUnknownLogintype() throws Exception {
		mockMvc.perform(post("/api/mcp/oauth/authorize/login")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("response_type", "code")
						.param("client_id", clientId)
						.param("redirect_uri", "http://localhost:5173/callback")
						.param("code_challenge", "challenge")
						.param("logintype", "member")
						.param("username", "admin")
						.param("password", "pwd"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Unsupported logintype")));
	}
}