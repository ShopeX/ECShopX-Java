package cn.shopex.ecshopx.mcp.auth.oauth;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Short-lived store between distributor login and shop selection on the authorize page.
 */
@Component
public class PendingDistributorAuthStore {

	static final long DEFAULT_TTL_SECONDS = 600L;

	private final long ttlSeconds;

	private final ConcurrentMap<String, PendingAuth> pending = new ConcurrentHashMap<>();

	public PendingDistributorAuthStore() {
		this(DEFAULT_TTL_SECONDS);
	}

	PendingDistributorAuthStore(long ttlSeconds) {
		this.ttlSeconds = Math.max(1L, ttlSeconds);
	}

	public String put(PendingAuth auth) {
		String id = UUID.randomUUID().toString();
		pending.put(id, auth);
		return id;
	}

	public Optional<PendingAuth> peek(String pendingId) {
		PendingAuth auth = pending.get(pendingId);
		if (auth == null) {
			return Optional.empty();
		}
		if (isExpired(auth)) {
			pending.remove(pendingId);
			return Optional.empty();
		}
		return Optional.of(auth);
	}

	public Optional<PendingAuth> consume(String pendingId) {
		Optional<PendingAuth> found = peek(pendingId);
		found.ifPresent(a -> pending.remove(pendingId));
		return found;
	}

	private boolean isExpired(PendingAuth auth) {
		long now = System.currentTimeMillis() / 1000L;
		return now - auth.issuedAt() >= ttlSeconds;
	}

	public record PendingAuth(
			String clientId,
			String redirectUri,
			String state,
			String codeChallenge,
			String codeChallengeMethod,
			String businessJwt,
			List<ShopOption> shops,
			long issuedAt) {

		public static PendingAuth create(
				String clientId,
				String redirectUri,
				String state,
				String codeChallenge,
				String codeChallengeMethod,
				String businessJwt,
				List<ShopOption> shops) {
			return new PendingAuth(
					clientId,
					redirectUri,
					state,
					codeChallenge,
					codeChallengeMethod,
					businessJwt,
					List.copyOf(shops),
					System.currentTimeMillis() / 1000L);
		}
	}

	public record ShopOption(long distributorId, String name) {
	}
}
