package cn.shopex.ecshopx.mcp.auth.oauth;

import cn.shopex.ecshopx.mcp.auth.TokenKind;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/mcp/oauth")
public class TokenController {

	private final OAuthClientRegistry clientRegistry;

	private final AuthorizationCodeStore authorizationCodeStore;

	private final IssuedTokenSessionStore issuedTokenSessionStore;

	private final LoginBridgeService loginBridgeService;

	private final PkceVerifier pkceVerifier;

	public TokenController(
			OAuthClientRegistry clientRegistry,
			AuthorizationCodeStore authorizationCodeStore,
			IssuedTokenSessionStore issuedTokenSessionStore,
			LoginBridgeService loginBridgeService,
			PkceVerifier pkceVerifier) {
		this.clientRegistry = clientRegistry;
		this.authorizationCodeStore = authorizationCodeStore;
		this.issuedTokenSessionStore = issuedTokenSessionStore;
		this.loginBridgeService = loginBridgeService;
		this.pkceVerifier = pkceVerifier;
	}

	@PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public ResponseEntity<Map<String, Object>> token(
			@RequestParam String grant_type,
			@RequestParam(required = false) String code,
			@RequestParam(required = false) String client_id,
			@RequestParam(required = false) String redirect_uri,
			@RequestParam(required = false) String code_verifier,
			@RequestParam(required = false) String refresh_token) {
		return switch (grant_type) {
			case "authorization_code" -> exchangeAuthorizationCode(code, client_id, redirect_uri, code_verifier);
			case "refresh_token" -> exchangeRefreshToken(refresh_token);
			default -> oauthError("unsupported_grant_type", "Grant type is not supported");
		};
	}

	private ResponseEntity<Map<String, Object>> exchangeAuthorizationCode(
			String code, String clientId, String redirectUri, String codeVerifier) {
		if (!StringUtils.hasText(code)
				|| !StringUtils.hasText(clientId)
				|| !StringUtils.hasText(redirectUri)
				|| !StringUtils.hasText(codeVerifier)) {
			return oauthError("invalid_request", "Missing required parameters");
		}
		try {
			OAuthAuthorizeSupport.requireClient(clientRegistry, clientId);
		}
		catch (IllegalArgumentException ex) {
			return oauthError("invalid_client", ex.getMessage());
		}

		String normalizedRedirect;
		try {
			normalizedRedirect = RedirectUriPolicy.validateOne(redirectUri);
		}
		catch (IllegalArgumentException ex) {
			return oauthError("invalid_request", ex.getMessage());
		}

		Optional<AuthorizationCodeStore.StoredAuthorizationCode> storedOpt = authorizationCodeStore.peek(code);
		if (storedOpt.isEmpty()) {
			return oauthError("invalid_grant", "Authorization code is invalid or expired");
		}
		AuthorizationCodeStore.StoredAuthorizationCode stored = storedOpt.get();

		if (!stored.clientId().equals(clientId)) {
			return oauthError("invalid_grant", "client_id does not match");
		}
		if (!stored.redirectUri().equals(normalizedRedirect)) {
			return oauthError("invalid_grant", "redirect_uri does not match");
		}
		if (!OAuthAuthorizeSupport.isS256ChallengeMethod(stored.codeChallengeMethod())) {
			return oauthError("invalid_grant", "code_challenge_method must be S256");
		}
		if (!pkceVerifier.verify(stored.codeChallenge(), stored.codeChallengeMethod(), codeVerifier)) {
			return oauthError("invalid_grant", "PKCE verification failed");
		}

		Optional<AuthorizationCodeStore.StoredAuthorizationCode> consumed = authorizationCodeStore.consume(code);
		if (consumed.isEmpty()) {
			return oauthError("invalid_grant", "Authorization code is invalid or expired");
		}

		return tokenResponse(consumed.get().businessJwt(), consumed.get().tokenKind());
	}

	private ResponseEntity<Map<String, Object>> exchangeRefreshToken(String refreshToken) {
		if (!StringUtils.hasText(refreshToken)) {
			return oauthError("invalid_request", "refresh_token is required");
		}
		Optional<TokenKind> kindOpt = issuedTokenSessionStore.resolve(refreshToken);
		if (kindOpt.isEmpty()) {
			return oauthError("invalid_grant", "Unknown or expired refresh token");
		}
		TokenKind kind = kindOpt.get();
		if (kind == TokenKind.POS) {
			return oauthError("invalid_grant", "POS tokens cannot be refreshed");
		}

		LoginBridgeService.RefreshResult refresh = loginBridgeService.refresh(kind, refreshToken);
		if (!refresh.success()) {
			return oauthError("invalid_grant", refresh.errorMessage());
		}

		issuedTokenSessionStore.rotate(refreshToken, refresh.jwt(), kind);
		return tokenResponse(refresh.jwt(), kind);
	}

	private ResponseEntity<Map<String, Object>> tokenResponse(String accessJwt, TokenKind tokenKind) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("access_token", accessJwt);
		body.put("token_type", "Bearer");
		BusinessJwtSupport.expiresInSeconds(accessJwt).ifPresent(exp -> body.put("expires_in", exp));
		if (tokenKind == TokenKind.OPERATOR || tokenKind == TokenKind.MEMBER) {
			body.put("refresh_token", accessJwt);
			issuedTokenSessionStore.register(accessJwt, tokenKind);
		}
		return ResponseEntity.ok(body);
	}

	private static ResponseEntity<Map<String, Object>> oauthError(String error, String description) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("error", error);
		if (description != null && !description.isBlank()) {
			body.put("error_description", description);
		}
		return ResponseEntity.badRequest().body(body);
	}
}