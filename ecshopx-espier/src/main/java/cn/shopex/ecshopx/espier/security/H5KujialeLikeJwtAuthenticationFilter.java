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

import cn.shopex.ecshopx.members.service.h5.H5BearerJwtClaimsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT gate for {@code POST /api/v1/h5app/wxapp/kujiale/like} only.
 *
 * <p>Registered as a servlet {@code Filter} bean; must {@link #shouldNotFilter} all other requests so
 * anonymous {@code /api/v1/h5app/**} routes are not affected.
 */
@Component
public class H5KujialeLikeJwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String LIKE_PATH = "/api/v1/h5app/wxapp/kujiale/like";

	private static final String MSG_UNABLE_TO_AUTHENTICATE = "Unable to authenticate user.";
	private static final String MSG_DISABLED = "该账号已被禁用.";

	private final H5BearerJwtClaimsService h5BearerJwtClaimsService;
	private final ObjectMapper objectMapper;

	public H5KujialeLikeJwtAuthenticationFilter(
			H5BearerJwtClaimsService h5BearerJwtClaimsService,
			ObjectMapper objectMapper) {
		this.h5BearerJwtClaimsService = h5BearerJwtClaimsService;
		this.objectMapper = objectMapper;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		if (!"POST".equalsIgnoreCase(request.getMethod())) {
			return true;
		}
		String path = request.getServletPath();
		return path == null || !LIKE_PATH.equals(path);
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Optional<Map<String, Object>> claimsOpt = h5BearerJwtClaimsService.verifyAndExtractClaims(request);
		if (claimsOpt.isEmpty()) {
			writeDingoUnauthorized(response, MSG_UNABLE_TO_AUTHENTICATE, 401001, HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}
		Map<String, Object> claims = claimsOpt.get();
		if (isAccountDisabled(claims.get("disabled"))) {
			writeDingoUnauthorized(response, MSG_DISABLED, 403001, HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		request.setAttribute(H5KujialeLikeAuthAttributes.JWT_CLAIMS, claims);
		UsernamePasswordAuthenticationToken auth =
				UsernamePasswordAuthenticationToken.authenticated(claims, null, Collections.emptyList());
		SecurityContextHolder.getContext().setAuthentication(auth);
		filterChain.doFilter(request, response);
	}

	/**
	 * 兼容风格响应体：{@code data.message} / {@code data.code} / {@code data.status_code}；HTTP 状态码为 {@code 401} / {@code 403}。
	 */
	private void writeDingoUnauthorized(HttpServletResponse response, String message, int code, int statusCode)
			throws IOException {
		response.setStatus(statusCode);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		Map<String, Object> data = new HashMap<>();
		data.put("message", message);
		data.put("code", code);
		data.put("status_code", statusCode);
		Map<String, Object> body = new HashMap<>();
		body.put("data", data);
		objectMapper.writeValue(response.getOutputStream(), body);
	}

	private static boolean isAccountDisabled(Object v) {
		if (v == null) {
			return false;
		}
		if (Boolean.TRUE.equals(v)) {
			return true;
		}
		String s = v.toString().trim();
		return "1".equals(s) || "true".equalsIgnoreCase(s);
	}
}
