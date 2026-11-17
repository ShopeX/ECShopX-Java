package cn.shopex.ecshopx.mcp.auth.oauth;

import cn.shopex.ecshopx.mcp.auth.TokenKind;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TokenControllerTest {

	private static final String VERIFIER =
			"dBjftJeZ4CVP-mB92K27uhbUJU1p1r_w5WNFgzU-Ws1FlsrnSFNuGmh7-S-lEb6-G_0w4vK96Tw17kFKVSfqJzQ";

	private final ObjectMapper objectMapper = new ObjectMapper();

	private MockMvc mockMvc;

	private InMemoryOAuthClientRegistry registry;

	private AuthorizationCodeStore codeStore;

	private IssuedTokenSessionStore sessionStore;

	private LoginBridgeService loginBridge;

	private PkceVerifier pkceVerifier;

	private String clientId;

	private String redirectUri;

	@BeforeEach
	void setUp() throws Exception {
		registry = new InMemoryOAuthClientRegistry();
		OAuthClientRegistration client = registry.register(
				List.of("http://localhost:5173/callback"), "test", "none");
		clientId = client.clientId();
		redirectUri = "http://localhost:5173/callback";
		codeStore = new AuthorizationCodeStore();
		sessionStore = new IssuedTokenSessionStore();
		loginBridge = mock(LoginBridgeService.class);
		pkceVerifier = new PkceVerifier();
		mockMvc = MockMvcBuilders.standaloneSetup(new TokenController(
						registry, codeStore, sessionStore, loginBridge, pkceVerifier))
				.defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
				.build();
	}

	@Test
	void authorizationCodeGrantReturnsAccessTokenAndRefreshForOperator() throws Exception {
		String challenge = s256(VERIFIER);
		String jwt = jwtWithExp(System.currentTimeMillis() / 1000L + 3600);
		String code = codeStore.issue(
				clientId, redirectUri, challenge, "S256", TokenKind.OPERATOR, jwt);

		MvcResult result = mockMvc.perform(post("/api/mcp/oauth/token")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("grant_type", "authorization_code")
						.param("code", code)
						.param("client_id", clientId)
						.param("redirect_uri", redirectUri)
						.param("code_verifier", VERIFIER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.access_token").value(jwt))
				.andExpect(jsonPath("$.token_type").value("Bearer"))
				.andExpect(jsonPath("$.refresh_token").value(jwt))
				.andReturn();

		JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
		assertTrue(body.has("expires_in"));
		assertTrue(codeStore.find(code).isEmpty(), "code must be single-use");
		assertEquals(TokenKind.OPERATOR, sessionStore.resolve(jwt).orElseThrow());
	}

	@Test
	void authorizationCodeGrantOmitsRefreshForPos() throws Exception {
		String challenge = s256(VERIFIER);
		String jwt = jwtWithExp(System.currentTimeMillis() / 1000L + 3600);
		String code = codeStore.issue(clientId, redirectUri, challenge, "S256", TokenKind.POS, jwt);

		mockMvc.perform(post("/api/mcp/oauth/token")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("grant_type", "authorization_code")
						.param("code", code)
						.param("client_id", clientId)
						.param("redirect_uri", redirectUri)
						.param("code_verifier", VERIFIER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.access_token").value(jwt))
				.andExpect(jsonPath("$.refresh_token").doesNotExist());
	}

	@Test
	void authorizationCodeRejectsWrongVerifier() throws Exception {
		String challenge = s256(VERIFIER);
		String code = codeStore.issue(
				clientId, redirectUri, challenge, "S256", TokenKind.OPERATOR, "jwt");

		mockMvc.perform(post("/api/mcp/oauth/token")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("grant_type", "authorization_code")
						.param("code", code)
						.param("client_id", clientId)
						.param("redirect_uri", redirectUri)
						.param("code_verifier", VERIFIER + "x"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("invalid_grant"));
	}

	@Test
	void authorizationCodeRejectsNonS256StoredMethod() throws Exception {
		String code = codeStore.issue(
				clientId, redirectUri, "plain-challenge", "plain", TokenKind.OPERATOR, "jwt");

		mockMvc.perform(post("/api/mcp/oauth/token")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("grant_type", "authorization_code")
						.param("code", code)
						.param("client_id", clientId)
						.param("redirect_uri", redirectUri)
						.param("code_verifier", "plain-challenge"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("invalid_grant"));
	}

	@Test
	void expiredAuthorizationCodeRejected() throws Exception {
		String challenge = s256(VERIFIER);
		AuthorizationCodeStore store = new AuthorizationCodeStore(1L);
		String code = store.issue(clientId, redirectUri, challenge, "S256", TokenKind.OPERATOR, "jwt");
		Thread.sleep(1100L);

		mockMvc = MockMvcBuilders.standaloneSetup(new TokenController(
						registry, store, sessionStore, loginBridge, pkceVerifier))
				.build();

		mockMvc.perform(post("/api/mcp/oauth/token")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("grant_type", "authorization_code")
						.param("code", code)
						.param("client_id", clientId)
						.param("redirect_uri", redirectUri)
						.param("code_verifier", VERIFIER))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("invalid_grant"));
	}

	@Test
	void refreshTokenGrantLoopsBackAndReturnsNewJwt() throws Exception {
		String oldJwt = "old.jwt";
		String newJwt = jwtWithExp(System.currentTimeMillis() / 1000L + 7200);
		sessionStore.register(oldJwt, TokenKind.OPERATOR);
		when(loginBridge.refresh(eq(TokenKind.OPERATOR), eq(oldJwt)))
				.thenReturn(LoginBridgeService.RefreshResult.success(newJwt));

		mockMvc.perform(post("/api/mcp/oauth/token")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("grant_type", "refresh_token")
						.param("refresh_token", oldJwt))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.access_token").value(newJwt))
				.andExpect(jsonPath("$.refresh_token").value(newJwt));

		assertFalse(sessionStore.resolve(oldJwt).isPresent());
		assertEquals(TokenKind.OPERATOR, sessionStore.resolve(newJwt).orElseThrow());
	}

	@Test
	void refreshTokenGrantRejectsPos() throws Exception {
		sessionStore.register("pos.jwt", TokenKind.POS);

		mockMvc.perform(post("/api/mcp/oauth/token")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("grant_type", "refresh_token")
						.param("refresh_token", "pos.jwt"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("invalid_grant"));
	}

	@Test
	void refreshTokenGrantRejectsUnknownToken() throws Exception {
		mockMvc.perform(post("/api/mcp/oauth/token")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("grant_type", "refresh_token")
						.param("refresh_token", "unknown"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("invalid_grant"));
	}

	private static String s256(String verifier) throws Exception {
		var digest = java.security.MessageDigest.getInstance("SHA-256");
		byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
		return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
	}

	private static String jwtWithExp(long expEpochSec) {
		String payload = Base64.getUrlEncoder()
				.withoutPadding()
				.encodeToString(("{\"exp\":" + expEpochSec + "}").getBytes(StandardCharsets.UTF_8));
		return "hdr." + payload + ".sig";
	}
}