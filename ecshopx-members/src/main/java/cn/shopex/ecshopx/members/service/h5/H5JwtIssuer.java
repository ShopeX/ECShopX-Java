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

package cn.shopex.ecshopx.members.service.h5;

import cn.shopex.ecshopx.common.auth.H5JwtBlacklistPort;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.members.config.H5JwtProperties;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class H5JwtIssuer {

	private static final Set<String> REFRESH_REGISTERED_CLAIMS =
			Set.of("iss", "iat", "exp", "nbf", "sub", "jti");

	private static final Set<String> RESERVED_CUSTOM_KEYS =
			Set.of("iss", "sub", "aud", "exp", "nbf", "iat", "jti");

	private final H5JwtProperties jwtProperties;

	private final H5JwtBlacklistPort h5JwtBlacklistPort;

	public H5JwtIssuer(H5JwtProperties jwtProperties, H5JwtBlacklistPort h5JwtBlacklistPort) {
		this.jwtProperties = jwtProperties;
		this.h5JwtBlacklistPort = h5JwtBlacklistPort;
	}

	public String issue(H5GenericUser user) throws JOSEException {
		byte[] secret = jwtProperties.resolveSecretBytes();
		if (secret.length == 0) {
			throw new IllegalStateException("ecshopx.h5.jwt.secret-base64 must be configured");
		}
		Date now = new Date();
		long expMs = now.getTime() + (long) jwtProperties.getTtlMinutes() * 60_000L;
		JWTClaimsSet.Builder b = new JWTClaimsSet.Builder()
				.issuer(jwtProperties.getIssuer())
				.subject(user.getJwtSubject())
				.jwtID(UUID.randomUUID().toString())
				.issueTime(now)
				.notBeforeTime(now)
				.expirationTime(new Date(expMs));
		Set<String> reserved = Set.of("iss", "sub", "aud", "exp", "nbf", "iat", "jti");
		for (Map.Entry<String, Object> e : user.getJwtCustomClaims().entrySet()) {
			if (e.getKey() != null && e.getValue() != null && !reserved.contains(e.getKey())) {
				b.claim(e.getKey(), e.getValue());
			}
		}
		JWTClaimsSet claims = b.build();
		SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
		jwt.sign(new MACSigner(secret));
		return jwt.serialize();
	}

	public String refreshAccessToken(String compactJwt) {
		if (compactJwt == null || compactJwt.isBlank()) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		if (h5JwtBlacklistPort.isBlacklisted(compactJwt)) {
			throw new UnauthorizedException(H5JwtBlacklistPort.TOKEN_BLACKLISTED_MESSAGE);
		}
		byte[] secret = jwtProperties.resolveSecretBytes();
		if (secret.length == 0) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		SignedJWT jwt;
		try {
			jwt = SignedJWT.parse(compactJwt);
		} catch (ParseException e) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		try {
			if (!jwt.verify(new MACVerifier(secret))) {
				throw new UnauthorizedException("Unable to authenticate user.");
			}
		} catch (JOSEException e) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		JWTClaimsSet claims;
		try {
			claims = jwt.getJWTClaimsSet();
		} catch (ParseException e) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		if (!jwtProperties.getIssuer().equals(claims.getIssuer())) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		if (!validateRequiredClaimsForRefresh(claims)) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		Date iatDate = claims.getIssueTime();
		if (iatDate == null) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		long iatSec = iatDate.getTime() / 1000L;
		long nowSec = Instant.now().getEpochSecond();
		if (nowSec - iatSec > (long) jwtProperties.getRefreshTtlMinutes() * 60L) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		Date expDate = claims.getExpirationTime();
		if (expDate == null) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		long expirationEpochSeconds = expDate.getTime() / 1000L;
		h5JwtBlacklistPort.addToBlacklist(compactJwt, expirationEpochSeconds);
		LinkedHashMap<String, Object> attrs = new LinkedHashMap<>(claims.toJSONObject());
		attrs.keySet().removeIf(RESERVED_CUSTOM_KEYS::contains);
		if (!attrs.containsKey("id") && StringUtils.hasText(claims.getSubject())) {
			attrs.put("id", claims.getSubject());
		}
		try {
			return issue(new H5GenericUser(attrs));
		} catch (JOSEException e) {
			throw new IllegalStateException(e);
		}
	}

	private boolean validateRequiredClaimsForRefresh(JWTClaimsSet claims) {
		for (String name : jwtProperties.getRequiredClaims()) {
			if (!REFRESH_REGISTERED_CLAIMS.contains(name)) {
				continue;
			}
			boolean ok = switch (name) {
				case "iss" -> claims.getIssuer() != null;
				case "sub" -> claims.getSubject() != null;
				case "jti" -> claims.getJWTID() != null;
				case "iat" -> claims.getIssueTime() != null;
				case "exp" -> claims.getExpirationTime() != null;
				case "nbf" -> claims.getNotBeforeTime() != null;
				default -> false;
			};
			if (!ok) {
				return false;
			}
		}
		return true;
	}
}
