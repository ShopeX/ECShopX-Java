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
import cn.shopex.ecshopx.members.config.H5JwtProperties;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class H5BearerJwtClaimsService {

	private final H5JwtProperties h5JwtProperties;

	private final H5JwtBlacklistPort h5JwtBlacklistPort;

	private final H5JwtUserLoginInfoEnrichService h5JwtUserLoginInfoEnrichService;

	public H5BearerJwtClaimsService(
			H5JwtProperties h5JwtProperties,
			H5JwtBlacklistPort h5JwtBlacklistPort,
			H5JwtUserLoginInfoEnrichService h5JwtUserLoginInfoEnrichService) {
		this.h5JwtProperties = h5JwtProperties;
		this.h5JwtBlacklistPort = h5JwtBlacklistPort;
		this.h5JwtUserLoginInfoEnrichService = h5JwtUserLoginInfoEnrichService;
	}

	public Optional<Map<String, Object>> verifyAndExtractClaims(HttpServletRequest request) {
		String authHeader = request.getHeader("Authorization");
		if (!StringUtils.hasText(authHeader) || authHeader.length() < 7
				|| !authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
			return Optional.empty();
		}
		String token = authHeader.substring(7).trim();
		if (!StringUtils.hasText(token)) {
			return Optional.empty();
		}
		if (h5JwtBlacklistPort.isBlacklisted(token)) {
			return Optional.empty();
		}
		byte[] secret = h5JwtProperties.resolveSecretBytes();
		if (secret.length == 0) {
			return Optional.empty();
		}
		try {
			SignedJWT jwt = SignedJWT.parse(token);
			if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())) {
				return Optional.empty();
			}
			if (!jwt.verify(new MACVerifier(secret))) {
				return Optional.empty();
			}
			JWTClaimsSet claims = jwt.getJWTClaimsSet();
			if (!h5JwtProperties.getIssuer().equals(claims.getIssuer())) {
				return Optional.empty();
			}
			if (!validateTimeClaims(claims) || !validateRequiredClaims(claims)) {
				return Optional.empty();
			}
			if (parsePositiveCompanyIdClaim(claims.getClaim("company_id")) == null) {
				return Optional.empty();
			}
			Map<String, Object> claimMap = new LinkedHashMap<>(claims.toJSONObject());
			h5JwtUserLoginInfoEnrichService.enrichClaims(claimMap);
			return Optional.of(claimMap);
		} catch (JOSEException | ParseException e) {
			return Optional.empty();
		}
	}

	private boolean validateTimeClaims(JWTClaimsSet claims) {
		Instant now = Instant.now();
		long leewaySec = h5JwtProperties.getLeewaySeconds();
		Date exp = claims.getExpirationTime();
		if (exp != null) {
			Instant expIns = exp.toInstant();
			if (now.isAfter(expIns.plusSeconds(leewaySec))) {
				return false;
			}
		}
		Date nbf = claims.getNotBeforeTime();
		if (nbf != null) {
			Instant nbfIns = nbf.toInstant();
			if (now.plusSeconds(leewaySec).isBefore(nbfIns)) {
				return false;
			}
		}
		Date iat = claims.getIssueTime();
		if (iat != null) {
			Instant iatIns = iat.toInstant();
			if (now.plusSeconds(leewaySec).isBefore(iatIns)) {
				return false;
			}
		}
		return true;
	}

	private boolean validateRequiredClaims(JWTClaimsSet claims) {
		for (String name : h5JwtProperties.getRequiredClaims()) {
			boolean ok = switch (name) {
				case "iss" -> claims.getIssuer() != null;
				case "sub" -> claims.getSubject() != null;
				case "jti" -> claims.getJWTID() != null;
				case "iat" -> claims.getIssueTime() != null;
				case "exp" -> claims.getExpirationTime() != null;
				case "nbf" -> claims.getNotBeforeTime() != null;
				default -> claims.getClaim(name) != null;
			};
			if (!ok) {
				return false;
			}
		}
		return true;
	}

	private static Integer parsePositiveCompanyIdClaim(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			if (v <= 0 || v > Integer.MAX_VALUE) {
				return null;
			}
			return (int) v;
		}
		if (raw instanceof String s) {
			return parsePositiveIntOrNull(s);
		}
		return parsePositiveIntOrNull(String.valueOf(raw));
	}

	private static Integer parsePositiveIntOrNull(String s) {
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			int v = Integer.parseInt(s.trim());
			return v > 0 ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
