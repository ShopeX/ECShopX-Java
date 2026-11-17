package cn.shopex.ecshopx.mcp.auth.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.OptionalLong;

final class BusinessJwtSupport {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private BusinessJwtSupport() {
	}

	static OptionalLong expiresInSeconds(String jwt) {
		if (jwt == null || jwt.isBlank()) {
			return OptionalLong.empty();
		}
		String[] parts = jwt.split("\\.");
		if (parts.length < 2) {
			return OptionalLong.empty();
		}
		try {
			byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
			JsonNode payload = MAPPER.readTree(new String(decoded, StandardCharsets.UTF_8));
			if (!payload.has("exp")) {
				return OptionalLong.empty();
			}
			long exp = payload.get("exp").asLong();
			long remaining = exp - (System.currentTimeMillis() / 1000L);
			return remaining > 0 ? OptionalLong.of(remaining) : OptionalLong.empty();
		}
		catch (Exception ex) {
			return OptionalLong.empty();
		}
	}
}