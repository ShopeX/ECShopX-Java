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
			OAuthClientRegistration registration = OAuthAuthorizeSupport.requireClient(clientRegistry, client_id);
			OAuthAuthorizeSupport.assertRedirectUriRegistered(registration, redirect_uri);
		}
		catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().body(escapePlain(ex.getMessage()));
		}
		String html = pageRenderer.render(new AuthorizePageRenderer.AuthorizeViewModel(
				client_id, redirect_uri, state, code_challenge, code_challenge_method, "OPERATOR", null));
		return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
	}

	@PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public ResponseEntity<String> authorizeLogin(
			@RequestParam String response_type,
			@RequestParam String client_id,
			@RequestParam String redirect_uri,
			@RequestParam(required = false) String state,
			@RequestParam String code_challenge,
			@RequestParam(required = false, defaultValue = "S256") String code_challenge_method,
			@RequestParam String token_kind,
			@RequestParam(required = false) String username,
			@RequestParam(required = false) String password,
			@RequestParam(required = false) String logintype,
			@RequestParam(required = false) String employee_no,
			@RequestParam(required = false) String pos_password,
			@RequestParam(required = false) String company_id,
			@RequestParam(required = false) String member_username,
			@RequestParam(required = false) String member_password) {
		AuthorizePageRenderer.AuthorizeViewModel baseView = new AuthorizePageRenderer.AuthorizeViewModel(
				client_id, redirect_uri, state, code_challenge, code_challenge_method, token_kind, null);
		try {
			assertAuthorizeParams(response_type, client_id, redirect_uri, code_challenge);
			OAuthClientRegistration registration = OAuthAuthorizeSupport.requireClient(clientRegistry, client_id);
			OAuthAuthorizeSupport.assertRedirectUriRegistered(registration, redirect_uri);
		}
		catch (IllegalArgumentException ex) {
			return badRequestHtml(baseView, ex.getMessage());
		}

		final TokenKind kind;
		try {
			kind = TokenKind.valueOf(token_kind.trim().toUpperCase());
		}
		catch (IllegalArgumentException ex) {
			return badRequestHtml(baseView, "Invalid token_kind");
		}

		Map<String, String> credentials = credentialsFor(kind, username, password, logintype, employee_no,
				pos_password, company_id, member_username, member_password);
		LoginBridgeService.LoginResult login = loginBridgeService.login(kind, credentials);
		if (!login.success()) {
			return badRequestHtml(baseView, login.errorMessage());
		}

		String code = authorizationCodeStore.issue(
				client_id,
				RedirectUriPolicy.validateOne(redirect_uri),
				code_challenge,
				code_challenge_method,
				kind,
				login.jwt());
		String location = OAuthAuthorizeSupport.appendQuery(
				RedirectUriPolicy.validateOne(redirect_uri), code, state);
		return ResponseEntity.status(HttpStatus.FOUND)
				.header(HttpHeaders.LOCATION, location)
				.build();
	}

	private static Map<String, String> credentialsFor(
			TokenKind kind,
			String username,
			String password,
			String logintype,
			String employeeNo,
			String posPassword,
			String companyId,
			String memberUsername,
			String memberPassword) {
		Map<String, String> map = new HashMap<>();
		switch (kind) {
			case OPERATOR -> {
				map.put("username", username);
				map.put("password", password);
				if (StringUtils.hasText(logintype)) {
					map.put("logintype", logintype);
				}
			}
			case POS -> {
				map.put("employee_no", employeeNo);
				map.put("password", StringUtils.hasText(posPassword) ? posPassword : password);
			}
			case MEMBER -> {
				map.put("company_id", companyId);
				map.put("username", StringUtils.hasText(memberUsername) ? memberUsername : username);
				map.put("password", StringUtils.hasText(memberPassword) ? memberPassword : password);
			}
		}
		return map;
	}

	private ResponseEntity<String> badRequestHtml(AuthorizePageRenderer.AuthorizeViewModel base, String error) {
		String html = pageRenderer.render(new AuthorizePageRenderer.AuthorizeViewModel(
				base.clientId(),
				base.redirectUri(),
				base.state(),
				base.codeChallenge(),
				base.codeChallengeMethod(),
				base.tokenKind(),
				error));
		return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.TEXT_HTML).body(html);
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