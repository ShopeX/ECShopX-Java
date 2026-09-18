/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.espier.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.SecretKey;

/**
 * Operator JWT compact 序列化：签发使用标准 Base64（ecshopx-admin {@code atob} 兼容），
 * 验签按 token 内原始 segment 字符串计算 HMAC。
 */
public final class OperatorJwtCompactCodec {

	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

	private OperatorJwtCompactCodec() {}

	public static String compactHs256(ObjectMapper objectMapper, Map<String, Object> payload, SecretKey signingKey) {
		try {
			String headerJson = objectMapper.writeValueAsString(Map.of("alg", "HS256"));
			String payloadJson = objectMapper.writeValueAsString(payload);
			String headerB64 = Base64.getEncoder().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
			String payloadB64 = Base64.getEncoder().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
			String signingInput = headerB64 + "." + payloadB64;
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(signingKey);
			byte[] signature = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
			return signingInput + "." + Base64.getEncoder().encodeToString(signature);
		} catch (JsonProcessingException | NoSuchAlgorithmException | InvalidKeyException e) {
			throw new IllegalStateException("Failed to issue operator JWT", e);
		}
	}

	public static Map<String, Object> verifyAndParse(ObjectMapper objectMapper, String compact, SecretKey signingKey) {
		if (compact == null || compact.isBlank()) {
			throw new JwtException("empty token");
		}
		try {
			return verifyStandardCompact(objectMapper, compact, signingKey);
		} catch (Exception standardEx) {
			try {
				Claims claims =
						Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(compact).getPayload();
				Map<String, Object> legacy = new LinkedHashMap<>();
				claims.forEach(legacy::put);
				return legacy;
			} catch (Exception legacyEx) {
				JwtException ex = new JwtException("登录验证错误");
				ex.addSuppressed(standardEx);
				ex.addSuppressed(legacyEx);
				throw ex;
			}
		}
	}

	public static long claimEpochSeconds(Map<String, Object> claims, String name) {
		Object v = claims.get(name);
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v == null) {
			throw new JwtException("missing claim: " + name);
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			throw new JwtException("invalid claim: " + name);
		}
	}

	private static Map<String, Object> verifyStandardCompact(
			ObjectMapper objectMapper, String compact, SecretKey signingKey)
			throws NoSuchAlgorithmException, InvalidKeyException, java.io.IOException {
		String[] parts = compact.split("\\.", 3);
		if (parts.length != 3 || parts[0].isEmpty() || parts[1].isEmpty() || parts[2].isEmpty()) {
			throw new JwtException("malformed token");
		}
		String signingInput = parts[0] + "." + parts[1];
		byte[] providedSig = decodeSegment(parts[2]);
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(signingKey);
		byte[] expectedSig = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
		if (!MessageDigest.isEqual(expectedSig, providedSig)) {
			throw new JwtException("invalid signature");
		}
		byte[] payloadBytes = decodeSegment(parts[1]);
		Map<String, Object> payload = objectMapper.readValue(payloadBytes, MAP_TYPE);
		return new LinkedHashMap<>(payload);
	}

	private static byte[] decodeSegment(String segment) {
		try {
			return Base64.getDecoder().decode(segment);
		} catch (IllegalArgumentException e) {
			return Base64.getUrlDecoder().decode(segment);
		}
	}
}
