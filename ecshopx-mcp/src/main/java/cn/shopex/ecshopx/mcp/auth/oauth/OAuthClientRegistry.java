package cn.shopex.ecshopx.mcp.auth.oauth;

import java.util.List;
import java.util.Optional;

public interface OAuthClientRegistry {

	OAuthClientRegistration register(List<String> redirectUris, String clientName, String tokenEndpointAuthMethod);

	Optional<OAuthClientRegistration> findByClientId(String clientId);
}