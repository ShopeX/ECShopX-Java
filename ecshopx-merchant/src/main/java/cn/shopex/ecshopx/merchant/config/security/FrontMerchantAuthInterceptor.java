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

import cn.shopex.ecshopx.common.annotation.FrontMerchantAuth;
import cn.shopex.ecshopx.merchant.config.MerchantWxappJwtProperties;
import cn.shopex.ecshopx.merchant.domain.MerchantSettlementApply;
import cn.shopex.ecshopx.merchant.mapper.MerchantSettlementApplyMapper;
import cn.shopex.ecshopx.merchant.service.wxapp.MerchantWxappAuthAttributes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class FrontMerchantAuthInterceptor implements HandlerInterceptor {

	private static final String MSG_UNABLE_TO_AUTHENTICATE = "Unable to authenticate user.";

	private final MerchantWxappJwtProperties jwtProperties;
	private final MerchantSettlementApplyMapper merchantSettlementApplyMapper;
	private final ObjectMapper objectMapper;

	public FrontMerchantAuthInterceptor(
			MerchantWxappJwtProperties jwtProperties,
			MerchantSettlementApplyMapper merchantSettlementApplyMapper,
			ObjectMapper objectMapper) {
		this.jwtProperties = jwtProperties;
		this.merchantSettlementApplyMapper = merchantSettlementApplyMapper;
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
		String h = request.getHeader("Authorization");
		if (h == null || h.length() < 8 || !h.regionMatches(true, 0, "Bearer ", 0, 7)) {
			writeDingoUnauthorized(response);
			return false;
		}
		String token = h.substring(7).trim();
		if (token.isEmpty()) {
			writeDingoUnauthorized(response);
			return false;
		}
		byte[] secret = jwtProperties.resolveSecretBytes();
		if (secret.length == 0) {
			writeDingoUnauthorized(response);
			return false;
		}
		SignedJWT jwt;
		JWTClaimsSet claims;
		try {
			jwt = SignedJWT.parse(token);
			if (!jwt.verify(new MACVerifier(secret))) {
				writeDingoUnauthorized(response);
				return false;
			}
			claims = jwt.getJWTClaimsSet();
		} catch (JOSEException | java.text.ParseException e) {
			writeDingoUnauthorized(response);
			return false;
		}
		String iss = claims.getIssuer();
		if (iss == null || !iss.equals(jwtProperties.getIssuer())) {
			writeDingoUnauthorized(response);
			return false;
		}
		long leewayMs = jwtProperties.getLeewaySeconds() * 1000L;
		Date now = new Date();
		Date exp = claims.getExpirationTime();
		if (exp != null && now.getTime() > exp.getTime() + leewayMs) {
			writeDingoUnauthorized(response);
			return false;
		}
		Date nbf = claims.getNotBeforeTime();
		if (nbf != null && now.getTime() + leewayMs < nbf.getTime()) {
			writeDingoUnauthorized(response);
			return false;
		}
		Long companyId = parsePositiveLong(claims.getClaim("company_id"));
		Long accountId = parsePositiveLong(claims.getClaim("account_id"));
		if (companyId == null || accountId == null) {
			writeDingoUnauthorized(response);
			return false;
		}
		MerchantSettlementApply apply = merchantSettlementApplyMapper.selectById(accountId);
		if (apply == null) {
			writeDingoUnauthorized(response);
			return false;
		}
		if (apply.getCompanyId() == null || !companyId.equals(apply.getCompanyId())) {
			writeDingoUnauthorized(response);
			return false;
		}
		var attrs = new MerchantWxappAuthAttributes(companyId, accountId);
		request.setAttribute(MerchantWxappAuthAttributes.REQUEST_ATTR, attrs);
		return true;
	}

	private static boolean hasAnnotation(HandlerMethod hm) {
		if (hm.getMethodAnnotation(FrontMerchantAuth.class) != null) {
			return true;
		}
		return hm.getBeanType().isAnnotationPresent(FrontMerchantAuth.class);
	}

	private void writeDingoUnauthorized(HttpServletResponse response) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		Map<String, Object> data = new HashMap<>();
		data.put("message", MSG_UNABLE_TO_AUTHENTICATE);
		data.put("code", 401001);
		data.put("status_code", HttpServletResponse.SC_UNAUTHORIZED);
		objectMapper.writeValue(response.getOutputStream(), Map.of("data", data));
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
