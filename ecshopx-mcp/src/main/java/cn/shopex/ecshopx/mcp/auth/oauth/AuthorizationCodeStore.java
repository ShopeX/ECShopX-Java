package cn.shopex.ecshopx.mcp.auth.oauth;

import cn.shopex.ecshopx.mcp.auth.TokenKind;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class AuthorizationCodeStore {

	private final ConcurrentMap<String, StoredAuthorizationCode> codes = new ConcurrentHashMap<>();

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
		return Optional.ofNullable(codes.get(code));
	}

	public Optional<StoredAuthorizationCode> consume(String code) {
		return Optional.ofNullable(codes.remove(code));
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