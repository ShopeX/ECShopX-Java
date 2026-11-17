package cn.shopex.ecshopx.mcp.auth.oauth;

import cn.shopex.ecshopx.mcp.auth.TokenKind;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class AuthorizationCodeStore {

	static final long DEFAULT_TTL_SECONDS = 600L;

	private final long ttlSeconds;

	private final ConcurrentMap<String, StoredAuthorizationCode> codes = new ConcurrentHashMap<>();

	public AuthorizationCodeStore() {
		this(DEFAULT_TTL_SECONDS);
	}

	AuthorizationCodeStore(long ttlSeconds) {
		this.ttlSeconds = Math.max(1L, ttlSeconds);
	}

	public String issue(
			String clientId,
			String redirectUri,
			String codeChallenge,
			String codeChallengeMethod,
			TokenKind tokenKind,
			String businessJwt) {
		String code = UUID.randomUUID().toString();
		StoredAuthorizationCode stored = new StoredAuthorizationCode(
				code,
				clientId,
				redirectUri,
				codeChallenge,
				codeChallengeMethod,
				tokenKind,
				businessJwt,
				System.currentTimeMillis() / 1000L);
		codes.put(code, stored);
		return code;
	}

	public Optional<StoredAuthorizationCode> find(String code) {
		StoredAuthorizationCode stored = codes.get(code);
		if (stored == null) {
			return Optional.empty();
		}
		if (isExpired(stored)) {
			codes.remove(code);
			return Optional.empty();
		}
		return Optional.of(stored);
	}

	public Optional<StoredAuthorizationCode> consume(String code) {
		Optional<StoredAuthorizationCode> found = find(code);
		found.ifPresent(s -> codes.remove(code));
		return found;
	}

	private boolean isExpired(StoredAuthorizationCode stored) {
		long now = System.currentTimeMillis() / 1000L;
		return now - stored.issuedAt() >= ttlSeconds;
	}

	public record StoredAuthorizationCode(
			String code,
			String clientId,
			String redirectUri,
			String codeChallenge,
			String codeChallengeMethod,
			TokenKind tokenKind,
			String businessJwt,
			long issuedAt) {
	}
}