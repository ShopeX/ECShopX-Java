package cn.shopex.ecshopx.mcp.auth.oauth;

final class OAuthAuthorizeSupport {

	private OAuthAuthorizeSupport() {
	}

	static OAuthClientRegistration requireClient(OAuthClientRegistry registry, String clientId) {
		return registry.findByClientId(clientId)
				.orElseThrow(() -> new IllegalArgumentException("Unknown client_id"));
	}

	static void assertRedirectUriRegistered(OAuthClientRegistration registration, String redirectUri) {
		String normalized = RedirectUriPolicy.validateOne(redirectUri);
		if (!registration.redirectUris().contains(normalized)) {
			throw new IllegalArgumentException("redirect_uri does not match client registration");
		}
	}

	static String appendQuery(String redirectUri, String code, String state) {
		StringBuilder target = new StringBuilder(redirectUri);
		target.append(redirectUri.contains("?") ? '&' : '?');
		target.append("code=").append(urlEncode(code));
		if (state != null && !state.isBlank()) {
			target.append("&state=").append(urlEncode(state));
		}
		return target.toString();
	}

	private static String urlEncode(String value) {
		return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
	}
}