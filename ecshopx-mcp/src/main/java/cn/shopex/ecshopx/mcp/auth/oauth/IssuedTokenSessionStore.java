package cn.shopex.ecshopx.mcp.auth.oauth;

import cn.shopex.ecshopx.mcp.auth.TokenKind;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class IssuedTokenSessionStore {

	private final ConcurrentMap<String, TokenKind> refreshToKind = new ConcurrentHashMap<>();

	public void register(String refreshToken, TokenKind tokenKind) {
		if (refreshToken == null || refreshToken.isBlank() || tokenKind == null) {
			return;
		}
		refreshToKind.put(refreshToken, tokenKind);
	}

	public Optional<TokenKind> resolve(String refreshToken) {
		return Optional.ofNullable(refreshToKind.get(refreshToken));
	}

	public void rotate(String previousRefreshToken, String newRefreshToken, TokenKind tokenKind) {
		if (previousRefreshToken != null && !previousRefreshToken.isBlank()) {
			refreshToKind.remove(previousRefreshToken);
		}
		register(newRefreshToken, tokenKind);
	}
}