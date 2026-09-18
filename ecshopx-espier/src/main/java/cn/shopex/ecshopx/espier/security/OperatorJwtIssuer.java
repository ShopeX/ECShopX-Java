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

import cn.shopex.ecshopx.common.auth.OperatorJwtBlacklistPort;
import cn.shopex.ecshopx.common.auth.OperatorJwtIssuerPort;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OperatorJwtIssuer implements OperatorJwtIssuerPort {

	private final CompanysActivationService companysActivationService;
	private final OperatorJwtBlacklistPort operatorJwtBlacklistPort;
	private final ObjectMapper objectMapper;
	private final SecretKey signingKey;
	private final long ttlSeconds;
	private final int jwtRefreshTtlMinutes;
	private final String jwtIssuer;

	public OperatorJwtIssuer(
			CompanysActivationService companysActivationService,
			OperatorJwtBlacklistPort operatorJwtBlacklistPort,
			ObjectMapper objectMapper,
			@Value("${JWT_SECRET:}") String jwtSecret,
			@Value("${JWT_TTL:120}") int jwtTtlMinutes,
			@Value("${JWT_REFRESH_TTL:20160}") int jwtRefreshTtlMinutes,
			@Value("${JWT_ISSUER:ecshopx}") String jwtIssuer) {
		this.companysActivationService = companysActivationService;
		this.operatorJwtBlacklistPort = operatorJwtBlacklistPort;
		this.objectMapper = objectMapper;
		this.signingKey = buildKey(jwtSecret);
		this.ttlSeconds = jwtTtlMinutes * 60L;
		this.jwtRefreshTtlMinutes = jwtRefreshTtlMinutes;
		this.jwtIssuer = jwtIssuer != null && !jwtIssuer.isBlank() ? jwtIssuer : "ecshopx";
	}

	private static SecretKey buildKey(String jwtSecret) {
		if (jwtSecret == null || jwtSecret.isBlank()) {
			throw new IllegalStateException("JWT_SECRET is required");
		}
		byte[] keyBytes;
		try {
			keyBytes = Base64.getDecoder().decode(jwtSecret);
		} catch (IllegalArgumentException e) {
			keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
		}
		return Keys.hmacShaKeyFor(keyBytes);
	}

	public String issueToken(Map<String, Object> newOperatorFromGetLoginToken) {
		companysActivationService.checkUserAuth(newOperatorFromGetLoginToken);
		Map<String, Object> claims = normalizeClaims(newOperatorFromGetLoginToken, objectMapper);
		String subject = resolveSubject(claims);
		if (subject == null || subject.isBlank()) {
			throw new IllegalStateException("operator JWT requires non-blank id (sub) from getLoginToken");
		}
		claims.remove("sub");
		claims.remove("iss");
		claims.remove("jti");
		claims.remove("nbf");
		claims.remove("iat");
		claims.remove("exp");
		long nowSec = System.currentTimeMillis() / 1000L;
		Map<String, Object> payload = new LinkedHashMap<>(claims);
		payload.put("sub", subject);
		payload.put("iss", jwtIssuer);
		payload.put("jti", UUID.randomUUID().toString());
		payload.put("iat", nowSec);
		payload.put("nbf", nowSec);
		payload.put("exp", nowSec + ttlSeconds);
		return OperatorJwtCompactCodec.compactHs256(objectMapper, payload, signingKey);
	}

	private static String resolveSubject(Map<String, Object> claims) {
		Object sub = claims.get("sub");
		if (sub != null) {
			String s = sub.toString();
			if (!s.isBlank()) {
				return s;
			}
		}
		Object id = claims.get("id");
		return id != null ? id.toString() : null;
	}

	@Override
	public String refreshAccessToken(String compactJwt) {
		if (compactJwt == null || compactJwt.isBlank()) {
			throw new UnauthorizedException("登录验证错误");
		}
		if (operatorJwtBlacklistPort.isBlacklisted(compactJwt)) {
			throw new UnauthorizedException("登录验证错误");
		}
		Map<String, Object> userData;
		try {
			userData = OperatorJwtCompactCodec.verifyAndParse(objectMapper, compactJwt, signingKey);
		} catch (JwtException | IllegalArgumentException ex) {
			throw new UnauthorizedException("登录验证错误");
		}
		long iatSec = OperatorJwtCompactCodec.claimEpochSeconds(userData, "iat");
		long nowSec = Instant.now().getEpochSecond();
		if (nowSec - iatSec > (long) jwtRefreshTtlMinutes * 60L) {
			throw new UnauthorizedException("登录验证错误");
		}
		long expirationEpochSeconds = OperatorJwtCompactCodec.claimEpochSeconds(userData, "exp");
		operatorJwtBlacklistPort.addToBlacklist(compactJwt, expirationEpochSeconds);
		companysActivationService.attachOperatorIdFromSessionClaims(userData);
		return issueToken(userData);
	}

	/**
	 * JJWT 仅接受可 JSON 序列化的简单类型；MyBatis Map 中可能出现 {@link java.math.BigInteger} 等，会导致构建失败。
	 */
	private static Map<String, Object> normalizeClaims(Map<String, Object> raw, ObjectMapper objectMapper) {
		Map<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e : raw.entrySet()) {
			Object v = e.getValue();
			if (v == null) {
				continue;
			}
			String k = e.getKey();
			if (v instanceof Boolean b) {
				out.put(k, b);
			} else if (v instanceof Number n) {
				out.put(k, n.longValue());
			} else if (v instanceof String s) {
				out.put(k, s);
			} else if (v instanceof Map<?, ?> || v instanceof Collection<?>) {
				try {
					out.put(k, objectMapper.writeValueAsString(v));
				} catch (JsonProcessingException ex) {
					throw new IllegalArgumentException("JWT claim not serializable: " + k, ex);
				}
			} else {
				out.put(k, v.toString());
			}
		}
		return out;
	}
}
