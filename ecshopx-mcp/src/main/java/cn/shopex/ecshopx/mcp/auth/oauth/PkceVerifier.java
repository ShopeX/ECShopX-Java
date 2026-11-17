package cn.shopex.ecshopx.mcp.auth.oauth;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class PkceVerifier {

	public boolean verify(String challenge, String method, String verifier) {
		if (challenge == null || challenge.isBlank() || verifier == null || verifier.isBlank()) {
			return false;
		}
		String effectiveMethod = (method == null || method.isBlank()) ? "S256" : method;
		if ("plain".equalsIgnoreCase(effectiveMethod)) {
			return challenge.equals(verifier);
		}
		if ("S256".equalsIgnoreCase(effectiveMethod)) {
			return challenge.equals(s256(verifier));
		}
		return false;
	}

	private static String s256(String verifier) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 not available", ex);
		}
	}
}