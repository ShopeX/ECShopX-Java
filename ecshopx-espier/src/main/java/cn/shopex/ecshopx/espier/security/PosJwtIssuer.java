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

import cn.shopex.ecshopx.common.auth.PosJwtBlacklistPort;
import cn.shopex.ecshopx.common.auth.PosJwtIssuerPort;
import cn.shopex.ecshopx.common.auth.PosJwtRequestAttributes;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PosJwtIssuer implements PosJwtIssuerPort {

	private final PosJwtBlacklistPort posJwtBlacklistPort;
	private final ObjectMapper objectMapper;
	private final SecretKey signingKey;
	private final long ttlSeconds;
	private final String jwtIssuer;

	public PosJwtIssuer(
			PosJwtBlacklistPort posJwtBlacklistPort,
			ObjectMapper objectMapper,
			@Value("${JWT_SECRET:}") String jwtSecret,
			@Value("${JWT_TTL:120}") int jwtTtlMinutes,
			@Value("${POS_JWT_ISSUER:ecshopx-pos}") String jwtIssuer) {
		this.posJwtBlacklistPort = posJwtBlacklistPort;
		this.objectMapper = objectMapper;
		this.signingKey = buildKey(jwtSecret);
		this.ttlSeconds = jwtTtlMinutes * 60L;
		this.jwtIssuer = jwtIssuer != null && !jwtIssuer.isBlank() ? jwtIssuer : "ecshopx-pos";
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

	@Override
	public String issueToken(Map<String, Object> claimsIn) {
		Map<String, Object> claims = new LinkedHashMap<>();
		if (claimsIn != null) {
			for (Map.Entry<String, Object> e : claimsIn.entrySet()) {
				Object v = e.getValue();
				if (v == null) {
					continue;
				}
				if (v instanceof Boolean || v instanceof String) {
					claims.put(e.getKey(), v);
				} else if (v instanceof Number n) {
					claims.put(e.getKey(), n.longValue());
				} else if (v instanceof Map<?, ?> || v instanceof Collection<?>) {
					claims.put(e.getKey(), v.toString());
				} else {
					claims.put(e.getKey(), v.toString());
				}
			}
		}
		claims.put(PosJwtRequestAttributes.CLAIM_TOKEN_KIND, PosJwtRequestAttributes.TOKEN_KIND_POS);
		Object id = claims.get("id");
		if (id == null || id.toString().isBlank()) {
			throw new IllegalStateException("POS JWT requires id");
		}
		long nowSec = System.currentTimeMillis() / 1000L;
		claims.put("sub", id.toString());
		claims.put("iss", jwtIssuer);
		claims.put("jti", UUID.randomUUID().toString());
		claims.put("iat", nowSec);
		claims.put("nbf", nowSec);
		claims.put("exp", nowSec + ttlSeconds);
		// Align with PHP JWT_ALGO=HS512 + Lcobucci Base64URL for DigiOS / standard JWT verifiers
		return OperatorJwtCompactCodec.compactHs512Url(objectMapper, claims, signingKey);
	}

	@Override
	public void invalidate(String compactJwt) {
		if (compactJwt == null || compactJwt.isBlank()) {
			throw new UnauthorizedException("登录验证错误");
		}
		Map<String, Object> userData = OperatorJwtCompactCodec.verifyAndParse(objectMapper, compactJwt, signingKey);
		long exp = OperatorJwtCompactCodec.claimEpochSeconds(userData, "exp");
		posJwtBlacklistPort.addToBlacklist(compactJwt, exp);
	}

	SecretKey signingKey() {
		return signingKey;
	}

	String issuer() {
		return jwtIssuer;
	}
}
