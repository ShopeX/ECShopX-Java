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

package cn.shopex.ecshopx.espier.web.interceptor;

import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.members.service.h5.H5BearerJwtClaimsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class FrontAuthInterceptor implements HandlerInterceptor {

	private static final String MSG_UNABLE_TO_AUTHENTICATE = "Unable to authenticate user.";
	private static final String MSG_DISABLED = "该账号已被禁用.";

	private final H5BearerJwtClaimsService h5BearerJwtClaimsService;
	private final ObjectMapper objectMapper;

	public FrontAuthInterceptor(H5BearerJwtClaimsService h5BearerJwtClaimsService, ObjectMapper objectMapper) {
		this.h5BearerJwtClaimsService = h5BearerJwtClaimsService;
		this.objectMapper = objectMapper;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		if (!(handler instanceof HandlerMethod hm)) {
			return true;
		}
		if (!hasAnnotation(hm)) {
			return true;
		}
		Optional<Map<String, Object>> claimsOpt = h5BearerJwtClaimsService.verifyAndExtractClaims(request);
		if (claimsOpt.isEmpty()) {
			writeDingoUnauthorized(response, MSG_UNABLE_TO_AUTHENTICATE, 401001, HttpServletResponse.SC_UNAUTHORIZED);
			return false;
		}
		Map<String, Object> claims = claimsOpt.get();
		if (isAccountDisabled(claims.get("disabled"))) {
			writeDingoUnauthorized(response, MSG_DISABLED, 403001, HttpServletResponse.SC_FORBIDDEN);
			return false;
		}
		request.setAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS, claims);
		Object companyId = claims.get("company_id");
		if (companyId != null) {
			request.setAttribute(H5FrontAuthAttributes.H5_COMPANY_ID, parsePositiveInt(companyId));
		}
		return true;
	}

	private static boolean hasAnnotation(HandlerMethod hm) {
		if (hm.getMethodAnnotation(FrontNoAuth.class) != null) {
			return false;
		}
		if (hm.getMethodAnnotation(FrontAuth.class) != null) {
			return true;
		}
		return hm.getBeanType().isAnnotationPresent(FrontAuth.class);
	}

	private void writeDingoUnauthorized(HttpServletResponse response, String message, int code, int statusCode)
			throws IOException {
		response.setStatus(statusCode);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		Map<String, Object> data = new HashMap<>();
		data.put("message", message);
		data.put("code", code);
		data.put("status_code", statusCode);
		objectMapper.writeValue(response.getOutputStream(), Map.of("data", data));
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

	private static Integer parsePositiveInt(Object raw) {
		if (raw instanceof Number n) {
			long v = n.longValue();
			return (v > 0 && v <= Integer.MAX_VALUE) ? (int) v : null;
		}
		try {
			int v = Integer.parseInt(raw.toString().trim());
			return v > 0 ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
