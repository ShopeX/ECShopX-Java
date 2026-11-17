package cn.shopex.ecshopx.mcp.auth.oauth;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PkceVerifierTest {

	private final PkceVerifier verifier = new PkceVerifier();

	@Test
	void acceptsValidS256Challenge() throws Exception {
		String codeVerifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_w5WNFgzU-Ws1FlsrnSFNuGmh7-S-lEb6-G_0w4vK96Tw17kFKVSfqJzQ";
		String challenge = s256(codeVerifier);
		assertTrue(verifier.verify(challenge, "S256", codeVerifier));
	}

	@Test
	void rejectsWrongVerifierForS256() throws Exception {
		String codeVerifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_w5WNFgzU-Ws1FlsrnSFNuGmh7-S-lEb6-G_0w4vK96Tw17kFKVSfqJzQ";
		String challenge = s256(codeVerifier);
		assertFalse(verifier.verify(challenge, "S256", codeVerifier + "x"));
	}

	@Test
	void treatsNullMethodAsS256() throws Exception {
		String codeVerifier = "verifier-12345678901234567890123456789012";
		assertTrue(verifier.verify(s256(codeVerifier), null, codeVerifier));
	}

	private static String s256(String verifier) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
		return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
	}
}