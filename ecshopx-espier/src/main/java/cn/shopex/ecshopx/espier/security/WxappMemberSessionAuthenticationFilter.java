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

import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.WxappMemberAuthAttributes;
import cn.shopex.ecshopx.members.service.wxapp.WxappMemberAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class WxappMemberSessionAuthenticationFilter extends OncePerRequestFilter {

	private static final String HEADER_WXAPP_SESSION = "x-wxapp-session";

	private static final String MSG_MISSING_SESSION_HEADER = "缺少请求头 x-wxapp-session。";

	private final WxappMemberAuthService wxappMemberAuthService;

	private final ObjectMapper objectMapper;

	public WxappMemberSessionAuthenticationFilter(
			WxappMemberAuthService wxappMemberAuthService, ObjectMapper objectMapper) {
		this.wxappMemberAuthService = wxappMemberAuthService;
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String session = request.getHeader(HEADER_WXAPP_SESSION);
		if (!StringUtils.hasText(session)) {
			writeDingoStyle(response, MSG_MISSING_SESSION_HEADER, 400);
			return;
		}
		Map<String, Object> auth;
		try {
			auth = wxappMemberAuthService.resolveSession(session.trim());
		} catch (UnauthorizedException ex) {
			String msg = ex.getMessage();
			writeDingoStyle(
					response, msg != null && !msg.isEmpty() ? msg : "认证失败。", 401);
			return;
		}
		request.setAttribute(WxappMemberAuthAttributes.REQUEST_ATTR, auth);
		UsernamePasswordAuthenticationToken token =
				UsernamePasswordAuthenticationToken.authenticated(auth, null, Collections.emptyList());
		SecurityContextHolder.getContext().setAuthentication(token);
		filterChain.doFilter(request, response);
	}

	private void writeDingoStyle(HttpServletResponse response, String message, int statusCode)
			throws IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType("application/json;charset=UTF-8");
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", message != null ? message : "");
		data.put("status_code", statusCode);
		objectMapper.writeValue(response.getOutputStream(), Map.of("data", data));
	}
}
