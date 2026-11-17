package cn.shopex.ecshopx.mcp.auth.oauth;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class InMemoryOAuthClientRegistry implements OAuthClientRegistry {

	private final ConcurrentMap<String, OAuthClientRegistration> clients = new ConcurrentHashMap<>();

	@Override
	public OAuthClientRegistration register(List<String> redirectUris, String clientName,
			String tokenEndpointAuthMethod) {
		List<String> validated = RedirectUriPolicy.validateAll(redirectUris);
		String authMethod = (tokenEndpointAuthMethod == null || tokenEndpointAuthMethod.isBlank())
				? "none"
				: tokenEndpointAuthMethod;
		long issuedAt = System.currentTimeMillis() / 1000L;
		String clientId = UUID.randomUUID().toString();
		OAuthClientRegistration registration = new OAuthClientRegistration(
				clientId, validated, clientName, authMethod, issuedAt);
		clients.put(clientId, registration);
		return registration;
	}

	@Override
	public Optional<OAuthClientRegistration> findByClientId(String clientId) {
		return Optional.ofNullable(clients.get(clientId));
	}
}