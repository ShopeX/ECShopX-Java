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
import cn.shopex.ecshopx.common.auth.PosJwtRequestAttributes;
import cn.shopex.ecshopx.common.auth.PosJwtSessionValidatorPort;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * POS JWT 鉴权：仅当 Bearer 为 POS token（token_kind=pos / iss=POS_JWT_ISSUER）时介入；
 * 白名单外拒绝；会话快照与库不一致（G-L9）拒绝。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
public class PosJwtAuthenticationFilter extends OncePerRequestFilter {

	public static final String POS_JWT_USER_DATA = PosJwtRequestAttributes.POS_JWT_USER_DATA;

	private static final Logger log = LoggerFactory.getLogger(PosJwtAuthenticationFilter.class);

	private final ObjectMapper objectMapper;
	private final SecretKey signingKey;
	private final String posIssuer;
	private final PosJwtBlacklistPort posJwtBlacklistPort;
	private final PosJwtSessionValidatorPort posJwtSessionValidatorPort;

	public PosJwtAuthenticationFilter(
			ObjectMapper objectMapper,
			@Value("${JWT_SECRET:}") String jwtSecret,
			@Value("${POS_JWT_ISSUER:ecshopx-pos}") String posIssuer,
			PosJwtBlacklistPort posJwtBlacklistPort,
			PosJwtSessionValidatorPort posJwtSessionValidatorPort) {
		this.objectMapper = objectMapper;
		this.signingKey = buildKey(jwtSecret);
		this.posIssuer = posIssuer != null && !posIssuer.isBlank() ? posIssuer : "ecshopx-pos";
		this.posJwtBlacklistPort = posJwtBlacklistPort;
		this.posJwtSessionValidatorPort = posJwtSessionValidatorPort;
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
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getServletPath();
		if ("POST".equalsIgnoreCase(request.getMethod()) && "/api/v1/pos/auth/login".equals(path)) {
			return true;
		}
		String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (auth == null || !auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
			return true;
		}
		String token = auth.substring(7).trim();
		if (token.isEmpty()) {
			return true;
		}
		return !looksLikePosToken(token);
	}

	private boolean looksLikePosToken(String compact) {
		try {
			Map<String, Object> claims =
					OperatorJwtCompactCodec.verifyAndParse(objectMapper, compact, signingKey);
			Object kind = claims.get(PosJwtRequestAttributes.CLAIM_TOKEN_KIND);
			if (PosJwtRequestAttributes.TOKEN_KIND_POS.equals(String.valueOf(kind))) {
				return true;
			}
			return posIssuer.equals(String.valueOf(claims.get("iss")));
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String path = request.getServletPath();
		String method = request.getMethod();
		String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
		String token = auth.substring(7).trim();
		if (PosJwtPathWhitelist.isExplicitlyDeniedAdmin(path)
				|| !PosJwtPathWhitelist.isAllowed(method, path)) {
			log.info("POS JWT denied path={} method={}", path, method);
			writeUnauthorized(response, "登录验证错误");
			return;
		}
		if (posJwtBlacklistPort.isBlacklisted(token)) {
			writeUnauthorized(response, "登录验证错误");
			return;
		}
		try {
			Map<String, Object> userData =
					OperatorJwtCompactCodec.verifyAndParse(objectMapper, token, signingKey);
			long nowSec = Instant.now().getEpochSecond();
			if (nowSec > OperatorJwtCompactCodec.claimEpochSeconds(userData, "exp")) {
				writeUnauthorized(response, "登录验证错误");
				return;
			}
			if (!PosJwtRequestAttributes.TOKEN_KIND_POS.equals(
							String.valueOf(userData.get(PosJwtRequestAttributes.CLAIM_TOKEN_KIND)))
					&& !posIssuer.equals(String.valueOf(userData.get("iss")))) {
				writeUnauthorized(response, "登录验证错误");
				return;
			}
			posJwtSessionValidatorPort.assertSessionStillValid(userData);
			request.setAttribute(POS_JWT_USER_DATA, userData);
		} catch (UnauthorizedException e) {
			writeUnauthorized(response, e.getMessage() != null ? e.getMessage() : "登录验证错误");
			return;
		} catch (Exception e) {
			writeUnauthorized(response, "登录验证错误");
			return;
		}
		filterChain.doFilter(request, response);
	}

	private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType("application/json;charset=UTF-8");
		Map<String, Object> data = new HashMap<>();
		data.put("message", message);
		data.put("status_code", HttpServletResponse.SC_UNAUTHORIZED);
		Map<String, Object> body = new HashMap<>();
		body.put("data", data);
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}
