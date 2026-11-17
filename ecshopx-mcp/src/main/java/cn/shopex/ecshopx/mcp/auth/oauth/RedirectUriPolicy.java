package cn.shopex.ecshopx.mcp.auth.oauth;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

final class RedirectUriPolicy {

	private RedirectUriPolicy() {
	}

	static List<String> validateAll(List<String> redirectUris) {
		if (redirectUris == null || redirectUris.isEmpty()) {
			throw new IllegalArgumentException("redirect_uris is required");
		}
		List<String> normalized = new ArrayList<>(redirectUris.size());
		for (String uri : redirectUris) {
			normalized.add(validateOne(uri));
		}
		return List.copyOf(normalized);
	}

	static String validateOne(String raw) {
		if (raw == null || raw.isBlank()) {
			throw new IllegalArgumentException("redirect_uri must not be blank");
		}
		final URI uri;
		try {
			uri = new URI(raw.trim());
		}
		catch (URISyntaxException ex) {
			throw new IllegalArgumentException("redirect_uri is not a valid URI");
		}
		String scheme = uri.getScheme();
		if (scheme == null) {
			throw new IllegalArgumentException("redirect_uri must include a scheme");
		}
		if ("https".equalsIgnoreCase(scheme)) {
			return raw.trim();
		}
		if ("http".equalsIgnoreCase(scheme)) {
			String host = uri.getHost();
			if (host != null && ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host))) {
				return raw.trim();
			}
		}
		throw new IllegalArgumentException(
				"redirect_uri must use https or http://localhost / http://127.0.0.1");
	}
}