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

package cn.shopex.ecshopx.merchant.config.security;

import cn.shopex.ecshopx.merchant.config.MerchantWxappJwtProperties;
import cn.shopex.ecshopx.merchant.domain.MerchantSettlementApply;
import cn.shopex.ecshopx.merchant.mapper.MerchantSettlementApplyMapper;
import cn.shopex.ecshopx.merchant.service.wxapp.MerchantWxappAuthAttributes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class MerchantWxappJwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String RESET_PATH = "/api/v1/h5app/wxapp/merchant/password/reset";

	private static final String SETTLEMENT_APPLY_PATTERN = "/api/v1/h5app/wxapp/merchant/settlementapply/**";

	private static final String AUDITSTATUS_GET_PATH = "/api/v1/h5app/wxapp/merchant/settlementapply/auditstatus";

	private static final String DETAIL_GET_PATH = "/api/v1/h5app/wxapp/merchant/settlementapply/detail";

	private static final String STEP_GET_PATH = "/api/v1/h5app/wxapp/merchant/settlementapply/step";

	private static final String TYPE_LIST_GET_PATH = "/api/v1/h5app/wxapp/merchant/type/list";

	private static final String MSG_UNABLE_TO_AUTHENTICATE = "Unable to authenticate user.";

	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	private final MerchantWxappJwtProperties jwtProperties;
	private final MerchantSettlementApplyMapper merchantSettlementApplyMapper;
	private final ObjectMapper objectMapper;

	public MerchantWxappJwtAuthenticationFilter(
			MerchantWxappJwtProperties jwtProperties,
			MerchantSettlementApplyMapper merchantSettlementApplyMapper,
			ObjectMapper objectMapper) {
		this.jwtProperties = jwtProperties;
		this.merchantSettlementApplyMapper = merchantSettlementApplyMapper;
		this.objectMapper = objectMapper;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = resolvePath(request);
		if ("GET".equalsIgnoreCase(request.getMethod())
				&& (AUDITSTATUS_GET_PATH.equals(path)
						|| DETAIL_GET_PATH.equals(path)
						|| STEP_GET_PATH.equals(path)
						|| TYPE_LIST_GET_PATH.equals(path))) {
			return false;
		}
		if (!"POST".equalsIgnoreCase(request.getMethod())) {
			return true;
		}
		if (RESET_PATH.equals(path)) {
			return false;
		}
		if (pathMatcher.match(SETTLEMENT_APPLY_PATTERN, path)) {
			return false;
		}
		return true;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String h = request.getHeader("Authorization");
		if (h == null || h.length() < 8 || !h.regionMatches(true, 0, "Bearer ", 0, 7)) {
			writeDingoUnauthorized(response, MSG_UNABLE_TO_AUTHENTICATE, 401001, HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}
		String token = h.substring(7).trim();
		if (token.isEmpty()) {
			writeDingoUnauthorized(response, MSG_UNABLE_TO_AUTHENTICATE, 401001, HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}
		byte[] secret = jwtProperties.resolveSecretBytes();
		if (secret.length == 0) {
			writeDingoUnauthorized(response, MSG_UNABLE_TO_AUTHENTICATE, 401001, HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}
		SignedJWT jwt;
		JWTClaimsSet claims;
		try {
			jwt = SignedJWT.parse(token);
			if (!jwt.verify(new MACVerifier(secret))) {
				writeDingoUnauthorized(response, MSG_UNABLE_TO_AUTHENTICATE, 401001, HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}
			claims = jwt.getJWTClaimsSet();
		} catch (JOSEException | java.text.ParseException e) {
			writeDingoUnauthorized(response, MSG_UNABLE_TO_AUTHENTICATE, 401001, HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}
		String iss = claims.getIssuer();
		if (iss == null || !iss.equals(jwtProperties.getIssuer())) {
			writeDingoUnauthorized(response, MSG_UNABLE_TO_AUTHENTICATE, 401001, HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}
		long leewayMs = jwtProperties.getLeewaySeconds() * 1000L;
		Date now = new Date();
		Date exp = claims.getExpirationTime();
		if (exp != null && now.getTime() > exp.getTime() + leewayMs) {
			writeDingoUnauthorized(response, MSG_UNABLE_TO_AUTHENTICATE, 401001, HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}
		Date nbf = claims.getNotBeforeTime();
		if (nbf != null && now.getTime() + leewayMs < nbf.getTime()) {
			writeDingoUnauthorized(response, MSG_UNABLE_TO_AUTHENTICATE, 401001, HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}
		Long companyId = parsePositiveLong(claims.getClaim("company_id"));
		Long accountId = parsePositiveLong(claims.getClaim("account_id"));
		if (companyId == null || accountId == null) {
			writeDingoUnauthorized(response, MSG_UNABLE_TO_AUTHENTICATE, 401001, HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}
		MerchantSettlementApply apply = merchantSettlementApplyMapper.selectById(accountId);
		if (apply == null) {
			writeDingoUnauthorized(response, MSG_UNABLE_TO_AUTHENTICATE, 401001, HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}
		if (apply.getCompanyId() == null || !companyId.equals(apply.getCompanyId())) {
			writeDingoUnauthorized(response, MSG_UNABLE_TO_AUTHENTICATE, 401001, HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}
		var attrs = new MerchantWxappAuthAttributes(companyId, accountId);
		request.setAttribute(MerchantWxappAuthAttributes.REQUEST_ATTR, attrs);
		UsernamePasswordAuthenticationToken authentication =
				UsernamePasswordAuthenticationToken.authenticated(attrs, null, Collections.emptyList());
		SecurityContextHolder.getContext().setAuthentication(authentication);
		filterChain.doFilter(request, response);
	}

	private static String resolvePath(HttpServletRequest request) {
		String path = request.getServletPath();
		if (path != null && !path.isEmpty()) {
			return path;
		}
		String uri = request.getRequestURI();
		if (uri == null) {
			return "";
		}
		int q = uri.indexOf('?');
		if (q >= 0) {
			uri = uri.substring(0, q);
		}
		String context = request.getContextPath();
		if (context != null && !context.isEmpty() && uri.startsWith(context)) {
			uri = uri.substring(context.length());
		}
		return uri;
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
		Map<String, Object> body = new HashMap<>();
		body.put("data", data);
		objectMapper.writeValue(response.getOutputStream(), body);
	}

	private static Long parsePositiveLong(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			long x = n.longValue();
			return x > 0 ? x : null;
		}
		try {
			long x = Long.parseLong(v.toString().trim());
			return x > 0 ? x : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
