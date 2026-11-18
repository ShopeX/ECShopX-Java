package cn.shopex.ecshopx.mcp.auth.oauth;

import cn.shopex.ecshopx.mcp.auth.TokenKind;
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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mcp/oauth/authorize")
public class AuthorizeController {

	private final OAuthClientRegistry clientRegistry;

	private final LoginBridgeService loginBridgeService;

	private final AuthorizationCodeStore authorizationCodeStore;

	private final AuthorizePageRenderer pageRenderer;

	private final DistributorShopSelectionService shopSelectionService;

	private final PendingDistributorAuthStore pendingDistributorAuthStore;

	public AuthorizeController(
			OAuthClientRegistry clientRegistry,
			LoginBridgeService loginBridgeService,
			AuthorizationCodeStore authorizationCodeStore,
			AuthorizePageRenderer pageRenderer,
			DistributorShopSelectionService shopSelectionService,
			PendingDistributorAuthStore pendingDistributorAuthStore) {
		this.clientRegistry = clientRegistry;
		this.loginBridgeService = loginBridgeService;
		this.authorizationCodeStore = authorizationCodeStore;
		this.pageRenderer = pageRenderer;
		this.shopSelectionService = shopSelectionService;
		this.pendingDistributorAuthStore = pendingDistributorAuthStore;
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

		if ("distributor".equalsIgnoreCase(normalizedLogintype)) {
			return completeDistributorLogin(
					client_id, redirect_uri, state, code_challenge, code_challenge_method, login.jwt(), baseView);
		}

		return redirectWithCode(client_id, redirect_uri, state, code_challenge, code_challenge_method, login.jwt());
	}

	@PostMapping(value = "/select-shop", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public ResponseEntity<String> selectShop(
			@RequestParam String pending_id,
			@RequestParam String set_distributor_id) {
		var pendingOpt = pendingDistributorAuthStore.peek(pending_id);
		if (pendingOpt.isEmpty()) {
			return ResponseEntity.badRequest().body(escapePlain("选店会话已过期，请重新登录授权"));
		}
		PendingDistributorAuthStore.PendingAuth pending = pendingOpt.get();
		long distributorId;
		try {
			distributorId = Long.parseLong(set_distributor_id.trim());
		}
		catch (NumberFormatException ex) {
			return shopSelectHtml(pending_id, pending.shops(), "请选择有效店铺");
		}
		boolean allowed = pending.shops().stream().anyMatch(s -> s.distributorId() == distributorId);
		if (!allowed) {
			return shopSelectHtml(pending_id, pending.shops(), "无权选择该店铺");
		}
		DistributorShopSelectionService.SelectResult selected =
				shopSelectionService.selectShop(pending.businessJwt(), distributorId);
		if (!selected.success()) {
			return shopSelectHtml(pending_id, pending.shops(), selected.errorMessage());
		}
		pendingDistributorAuthStore.consume(pending_id);
		return redirectWithCode(
				pending.clientId(),
				pending.redirectUri(),
				pending.state(),
				pending.codeChallenge(),
				pending.codeChallengeMethod(),
				pending.businessJwt());
	}

	private ResponseEntity<String> completeDistributorLogin(
			String clientId,
			String redirectUri,
			String state,
			String codeChallenge,
			String codeChallengeMethod,
			String jwt,
			AuthorizePageRenderer.AuthorizeViewModel baseView) {
		DistributorShopSelectionService.ListResult listed = shopSelectionService.listShops(jwt);
		if (!listed.success()) {
			return badRequestHtml(baseView, listed.errorMessage());
		}
		List<PendingDistributorAuthStore.ShopOption> shops = listed.shops();
		if (shops.isEmpty()) {
			return badRequestHtml(baseView, "该账号没有可管理的店铺");
		}
		if (shops.size() == 1) {
			DistributorShopSelectionService.SelectResult selected =
					shopSelectionService.selectShop(jwt, shops.get(0).distributorId());
			if (!selected.success()) {
				return badRequestHtml(baseView, selected.errorMessage());
			}
			return redirectWithCode(clientId, redirectUri, state, codeChallenge, codeChallengeMethod, jwt);
		}
		String pendingId = pendingDistributorAuthStore.put(PendingDistributorAuthStore.PendingAuth.create(
				clientId, redirectUri, state, codeChallenge, codeChallengeMethod, jwt, shops));
		return shopSelectHtml(pendingId, shops, null);
	}

	private ResponseEntity<String> redirectWithCode(
			String clientId,
			String redirectUri,
			String state,
			String codeChallenge,
			String codeChallengeMethod,
			String jwt) {
		String code = authorizationCodeStore.issue(
				clientId,
				RedirectUriPolicy.validateOne(redirectUri),
				codeChallenge,
				codeChallengeMethod,
				TokenKind.OPERATOR,
				jwt);
		String location = OAuthAuthorizeSupport.appendQuery(
				RedirectUriPolicy.validateOne(redirectUri), code, state);
		String html = pageRenderer.renderSuccess(new AuthorizePageRenderer.SuccessViewModel(location));
		return htmlOk(html);
	}

	private ResponseEntity<String> shopSelectHtml(
			String pendingId, List<PendingDistributorAuthStore.ShopOption> shops, String error) {
		String html = pageRenderer.renderShopSelect(
				new AuthorizePageRenderer.ShopSelectViewModel(pendingId, shops, error));
		return htmlOk(html);
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
