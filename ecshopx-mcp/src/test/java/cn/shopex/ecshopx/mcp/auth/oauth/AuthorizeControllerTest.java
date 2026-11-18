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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthorizeControllerTest {

	private static final Pattern CODE_PARAM = Pattern.compile("[?&]code=([^&\"'\\s]+)");

	private MockMvc mockMvc;

	private InMemoryOAuthClientRegistry registry;

	private LoginBridgeService loginBridge;

	private AuthorizationCodeStore codeStore;

	private DistributorShopSelectionService shopSelectionService;

	private PendingDistributorAuthStore pendingDistributorAuthStore;

	private String clientId;

	@BeforeEach
	void setUp() {
		registry = new InMemoryOAuthClientRegistry();
		OAuthClientRegistration client = registry.register(
				List.of("http://localhost:5173/callback"), "test", "none");
		clientId = client.clientId();
		loginBridge = mock(LoginBridgeService.class);
		codeStore = new AuthorizationCodeStore();
		shopSelectionService = mock(DistributorShopSelectionService.class);
		pendingDistributorAuthStore = new PendingDistributorAuthStore();
		mockMvc = MockMvcBuilders.standaloneSetup(new AuthorizeController(
						registry,
						loginBridge,
						codeStore,
						new AuthorizePageRenderer(),
						shopSelectionService,
						pendingDistributorAuthStore))
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
				.andExpect(content().string(org.hamcrest.Matchers.containsString("请登录并完成MCP授权")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("登录并授权")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("用户名")))
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
	void postLoginShowsSuccessPageWithDoneLink() throws Exception {
		when(loginBridge.login(any(TokenKind.class), any(Map.class)))
				.thenReturn(LoginBridgeService.LoginResult.success("biz.jwt"));

		MvcResult result = mockMvc.perform(post("/api/mcp/oauth/authorize/login")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("response_type", "code")
						.param("client_id", clientId)
						.param("redirect_uri", "http://localhost:5173/callback")
						.param("code_challenge", "challenge")
						.param("logintype", "admin")
						.param("username", "admin")
						.param("password", "pwd"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("授权成功")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("MCP 授权已完成。")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("你可以关闭此页面，返回应用继续使用。")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("完成")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("code=")))
				.andReturn();

		String body = result.getResponse().getContentAsString();
		Matcher matcher = CODE_PARAM.matcher(body);
		assertTrue(matcher.find());
		String code = URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
		var stored = codeStore.find(code);
		assertTrue(stored.isPresent());
		assertEquals("biz.jwt", stored.get().businessJwt());
		assertEquals(TokenKind.OPERATOR, stored.get().tokenKind());
		assertNotNull(stored.get().codeChallenge());
		verify(shopSelectionService, never()).listShops(any());
	}

	@Test
	void postDistributorLoginAutoSelectsSingleShop() throws Exception {
		when(loginBridge.login(any(TokenKind.class), any(Map.class)))
				.thenReturn(LoginBridgeService.LoginResult.success("dist.jwt"));
		when(shopSelectionService.listShops("dist.jwt"))
				.thenReturn(DistributorShopSelectionService.ListResult.success(
						List.of(new PendingDistributorAuthStore.ShopOption(11L, "单店"))));
		when(shopSelectionService.selectShop("dist.jwt", 11L))
				.thenReturn(DistributorShopSelectionService.SelectResult.ok());

		mockMvc.perform(post("/api/mcp/oauth/authorize/login")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("response_type", "code")
						.param("client_id", clientId)
						.param("redirect_uri", "http://localhost:5173/callback")
						.param("code_challenge", "challenge")
						.param("logintype", "distributor")
						.param("username", "shop")
						.param("password", "pwd"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("授权成功")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("code=")));

		verify(shopSelectionService).selectShop("dist.jwt", 11L);
	}

	@Test
	void postDistributorLoginShowsShopSelectForMultipleShops() throws Exception {
		when(loginBridge.login(any(TokenKind.class), any(Map.class)))
				.thenReturn(LoginBridgeService.LoginResult.success("dist.jwt"));
		when(shopSelectionService.listShops("dist.jwt"))
				.thenReturn(DistributorShopSelectionService.ListResult.success(List.of(
						new PendingDistributorAuthStore.ShopOption(11L, "店铺A"),
						new PendingDistributorAuthStore.ShopOption(22L, "店铺B"))));

		mockMvc.perform(post("/api/mcp/oauth/authorize/login")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("response_type", "code")
						.param("client_id", clientId)
						.param("redirect_uri", "http://localhost:5173/callback")
						.param("code_challenge", "challenge")
						.param("logintype", "distributor")
						.param("username", "shop")
						.param("password", "pwd"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("选择店铺")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("店铺A")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("店铺B")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"pending_id\"")));

		verify(shopSelectionService, never()).selectShop(any(), anyLong());
	}

	@Test
	void postSelectShopRedirectsWithCode() throws Exception {
		String pendingId = pendingDistributorAuthStore.put(PendingDistributorAuthStore.PendingAuth.create(
				clientId,
				"http://localhost:5173/callback",
				null,
				"challenge",
				"S256",
				"dist.jwt",
				List.of(
						new PendingDistributorAuthStore.ShopOption(11L, "店铺A"),
						new PendingDistributorAuthStore.ShopOption(22L, "店铺B"))));
		when(shopSelectionService.selectShop("dist.jwt", 22L))
				.thenReturn(DistributorShopSelectionService.SelectResult.ok());

		MvcResult result = mockMvc.perform(post("/api/mcp/oauth/authorize/select-shop")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("pending_id", pendingId)
						.param("set_distributor_id", "22"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("授权成功")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("完成")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("code=")))
				.andReturn();

		String body = result.getResponse().getContentAsString();
		Matcher matcher = CODE_PARAM.matcher(body);
		assertTrue(matcher.find());
		String code = URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
		assertTrue(codeStore.find(code).isPresent());
		assertTrue(pendingDistributorAuthStore.peek(pendingId).isEmpty());
		verify(shopSelectionService).selectShop(eq("dist.jwt"), eq(22L));
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
