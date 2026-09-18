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

import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.OperatorDistributorSelectionService;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.espier.security.OperatorJwtCompactCodec;
import cn.shopex.ecshopx.espier.security.ShopAppMemberTokenOperatorEnrichmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

	private static final String DINGO_BAD_CREDENTIALS_MESSAGE =
			"Failed to authenticate because of bad credentials or an invalid authorization header.";

	private final CompanysActivationService companysActivationService;
	private final OperatorDistributorSelectionService operatorDistributorSelectionService;
	private final ShopAppMemberTokenOperatorEnrichmentService shopAppMemberTokenOperatorEnrichmentService;
	private final ObjectMapper objectMapper;
	private final SecretKey signingKey;

	public AdminAuthInterceptor(
			CompanysActivationService companysActivationService,
			OperatorDistributorSelectionService operatorDistributorSelectionService,
			ShopAppMemberTokenOperatorEnrichmentService shopAppMemberTokenOperatorEnrichmentService,
			ObjectMapper objectMapper,
			@Value("${JWT_SECRET:}") String jwtSecret) {
		this.companysActivationService = companysActivationService;
		this.operatorDistributorSelectionService = operatorDistributorSelectionService;
		this.shopAppMemberTokenOperatorEnrichmentService = shopAppMemberTokenOperatorEnrichmentService;
		this.objectMapper = objectMapper;
		this.signingKey = buildKey(jwtSecret);
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
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		if (!(handler instanceof HandlerMethod hm)) {
			return true;
		}
		if (!hasAnnotation(hm)) {
			return true;
		}
		String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (auth == null || !auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
			writeUnauthorized(response, DINGO_BAD_CREDENTIALS_MESSAGE);
			return false;
		}
		String token = auth.substring(7).trim();
		if (token.isEmpty()) {
			writeUnauthorized(response, DINGO_BAD_CREDENTIALS_MESSAGE);
			return false;
		}
		try {
			Map<String, Object> userData =
					OperatorJwtCompactCodec.verifyAndParse(objectMapper, token, signingKey);
			long nowSec = Instant.now().getEpochSecond();
			if (nowSec > OperatorJwtCompactCodec.claimEpochSeconds(userData, "exp")) {
				writeUnauthorized(response, "登录验证错误");
				return false;
			}
			try {
				shopAppMemberTokenOperatorEnrichmentService.enrichIfApplicable(userData);
				companysActivationService.checkUserAuth(userData);
			} catch (ResourceException e) {
				String msg = e.getMessage();
				writeForbidden(response, msg != null && !msg.isEmpty() ? msg : "未登录");
				return false;
			}
			companysActivationService.attachOperatorIdFromSessionClaims(userData);
			attachSelectedDistributor(userData);
			request.setAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA, userData);
		} catch (Exception e) {
			writeUnauthorized(response, "登录验证错误");
			return false;
		}
		return true;
	}

	private void attachSelectedDistributor(Map<String, Object> userData) {
		if (!"distributor".equals(String.valueOf(userData.get("operator_type")))) {
			return;
		}
		Long operatorId = longOrNull(userData.get("operator_id"));
		if (operatorId == null || operatorId <= 0L) {
			operatorId = longOrNull(userData.get("id"));
		}
		Long companyId = longOrNull(userData.get("company_id"));
		if (operatorId == null || operatorId <= 0L || companyId == null || companyId <= 0L) {
			userData.put("distributor_id", 0L);
			return;
		}
		userData.put(
				"distributor_id",
				operatorDistributorSelectionService.readSelectedDistributorId(operatorId, companyId).orElse(0L));
	}

	private static Long longOrNull(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String text = String.valueOf(raw).trim();
		if (text.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(text);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static boolean hasAnnotation(HandlerMethod hm) {
		if (hm.getMethodAnnotation(AdminAuth.class) != null) {
			return true;
		}
		return hm.getBeanType().isAnnotationPresent(AdminAuth.class);
	}

	private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType("application/json;charset=UTF-8");
		Map<String, Object> data = new HashMap<>();
		data.put("message", message);
		data.put("status_code", HttpServletResponse.SC_UNAUTHORIZED);
		objectMapper.writeValue(response.getOutputStream(), Map.of("data", data));
	}

	private void writeForbidden(HttpServletResponse response, String message) throws IOException {
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType("application/json;charset=UTF-8");
		objectMapper.writeValue(response.getOutputStream(), ApiResult.fail(HttpServletResponse.SC_FORBIDDEN, message));
	}
}
