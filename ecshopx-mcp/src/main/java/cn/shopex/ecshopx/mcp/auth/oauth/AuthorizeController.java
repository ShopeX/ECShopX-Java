package cn.shopex.ecshopx.mcp.auth.oauth;

import cn.shopex.ecshopx.mcp.auth.TokenKind;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/mcp/oauth/authorize")
public class AuthorizeController {

	private final OAuthClientRegistry clientRegistry;

	private final LoginBridgeService loginBridgeService;

	private final AuthorizationCodeStore authorizationCodeStore;

	private final AuthorizePageRenderer pageRenderer;

	public AuthorizeController(
			OAuthClientRegistry clientRegistry,
			LoginBridgeService loginBridgeService,
			AuthorizationCodeStore authorizationCodeStore,
			AuthorizePageRenderer pageRenderer) {
		this.clientRegistry = clientRegistry;
		this.loginBridgeService = loginBridgeService;
		this.authorizationCodeStore = authorizationCodeStore;
		this.pageRenderer = pageRenderer;
	}

	@GetMapping(produces = MediaType.TEXT_HTML_VALUE)
	public ResponseEntity<String> authorizePage(
			@RequestParam String response_type,
			@RequestParam String client_id,
			@RequestParam String redirect_uri,
			@RequestParam(required = false) String state,
			@RequestParam String code_challenge,
			@RequestParam(required = false, defaultValue = "S256") String code_challenge_method) {
		try {
			assertAuthorizeParams(response_type, client_id, redirect_uri, code_challenge);
			OAuthAuthorizeSupport.assertS256ChallengeMethod(code_challenge_method);
			OAuthClientRegistration registration = OAuthAuthorizeSupport.requireClient(clientRegistry, client_id);
			OAuthAuthorizeSupport.assertRedirectUriRegistered(registration, redirect_uri);
		}
		catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().body(escapePlain(ex.getMessage()));
		}
		String html = pageRenderer.render(new AuthorizePageRenderer.AuthorizeViewModel(
				client_id, redirect_uri, state, code_challenge, code_challenge_method,
				OperatorLoginTypes.DEFAULT, null));
		return htmlOk(html);
	}

	@PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public ResponseEntity<String> authorizeLogin(
			@RequestParam String response_type,
			@RequestParam String client_id,
			@RequestParam String redirect_uri,
			@RequestParam(required = false) String state,
			@RequestParam String code_challenge,
			@RequestParam(required = false, defaultValue = "S256") String code_challenge_method,
			@RequestParam String username,
			@RequestParam String password,
			@RequestParam(required = false) String logintype) {
		String normalizedLogintype;
		try {
			normalizedLogintype = OperatorLoginTypes.normalize(logintype);
		}
		catch (IllegalArgumentException ex) {
			AuthorizePageRenderer.AuthorizeViewModel baseView = new AuthorizePageRenderer.AuthorizeViewModel(
					client_id, redirect_uri, state, code_challenge, code_challenge_method,
					OperatorLoginTypes.DEFAULT, null);
			return badRequestHtml(baseView, ex.getMessage());
		}

		AuthorizePageRenderer.AuthorizeViewModel baseView = new AuthorizePageRenderer.AuthorizeViewModel(
				client_id, redirect_uri, state, code_challenge, code_challenge_method, normalizedLogintype, null);
		try {
			assertAuthorizeParams(response_type, client_id, redirect_uri, code_challenge);
			OAuthAuthorizeSupport.assertS256ChallengeMethod(code_challenge_method);
			OAuthClientRegistration registration = OAuthAuthorizeSupport.requireClient(clientRegistry, client_id);
			OAuthAuthorizeSupport.assertRedirectUriRegistered(registration, redirect_uri);
		}
		catch (IllegalArgumentException ex) {
			return badRequestHtml(baseView, ex.getMessage());
		}

		Map<String, String> credentials = new HashMap<>();
		credentials.put("username", username);
		credentials.put("password", password);
		credentials.put("logintype", normalizedLogintype);

		LoginBridgeService.LoginResult login = loginBridgeService.login(TokenKind.OPERATOR, credentials);
		if (!login.success()) {
			return badRequestHtml(baseView, login.errorMessage());
		}

		String code = authorizationCodeStore.issue(
				client_id,
				RedirectUriPolicy.validateOne(redirect_uri),
				code_challenge,
				code_challenge_method,
				TokenKind.OPERATOR,
				login.jwt());
		String location = OAuthAuthorizeSupport.appendQuery(
				RedirectUriPolicy.validateOne(redirect_uri), code, state);
		return ResponseEntity.status(HttpStatus.FOUND)
				.header(HttpHeaders.LOCATION, location)
				.build();
	}

	private ResponseEntity<String> badRequestHtml(AuthorizePageRenderer.AuthorizeViewModel base, String error) {
		String html = pageRenderer.render(new AuthorizePageRenderer.AuthorizeViewModel(
				base.clientId(),
				base.redirectUri(),
				base.state(),
				base.codeChallenge(),
				base.codeChallengeMethod(),
				base.logintype(),
				error));
		return htmlOk(html);
	}

	private static ResponseEntity<String> htmlOk(String html) {
		return ResponseEntity.ok()
				.contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
				.body(html);
	}

	private static void assertAuthorizeParams(
			String responseType, String clientId, String redirectUri, String codeChallenge) {
		if (!"code".equals(responseType)) {
			throw new IllegalArgumentException("response_type must be code");
		}
		if (!StringUtils.hasText(clientId)) {
			throw new IllegalArgumentException("client_id is required");
		}
		if (!StringUtils.hasText(redirectUri)) {
			throw new IllegalArgumentException("redirect_uri is required");
		}
		if (!StringUtils.hasText(codeChallenge)) {
			throw new IllegalArgumentException("code_challenge is required");
		}
	}

	private static String escapePlain(String message) {
		return message == null ? "Bad request" : message;
	}
}
