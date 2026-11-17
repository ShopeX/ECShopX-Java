package cn.shopex.ecshopx.mcp.auth.oauth;

import java.util.List;

public record OAuthClientRegistration(
		String clientId,
		List<String> redirectUris,
		String clientName,
		String tokenEndpointAuthMethod,
		long clientIdIssuedAt) {
}