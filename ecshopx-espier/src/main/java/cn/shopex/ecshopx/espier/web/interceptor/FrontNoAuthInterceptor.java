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

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.port.ali.AliMiniAppCompanyIdLookupPort;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.companys.service.domain.CompanyDomainLookupService;
import cn.shopex.ecshopx.members.config.H5JwtProperties;
import cn.shopex.ecshopx.members.mapper.WechatAuthorizerLookupMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.members.service.h5.H5JwtUserLoginInfoEnrichService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class FrontNoAuthInterceptor implements HandlerInterceptor {

	private final H5JwtProperties h5JwtProperties;
	private final CompanyDomainLookupService companyDomainLookupService;
	private final WechatAuthorizerLookupMapper wechatAuthorizerLookupMapper;
	private final ObjectProvider<AliMiniAppCompanyIdLookupPort> aliMiniAppCompanyIdLookupPort;
	private final MemberAccountService memberAccountService;
	private final H5JwtUserLoginInfoEnrichService h5JwtUserLoginInfoEnrichService;
	private final ObjectMapper objectMapper;
	private final boolean systemIsSaas;
	private final int systemCompanysId;
	private final String systemMainCompanysIdRaw;

	public FrontNoAuthInterceptor(
			H5JwtProperties h5JwtProperties,
			CompanyDomainLookupService companyDomainLookupService,
			WechatAuthorizerLookupMapper wechatAuthorizerLookupMapper,
			ObjectProvider<AliMiniAppCompanyIdLookupPort> aliMiniAppCompanyIdLookupPort,
			MemberAccountService memberAccountService,
			H5JwtUserLoginInfoEnrichService h5JwtUserLoginInfoEnrichService,
			ObjectMapper objectMapper,
			@Value("${common.system-is-saas:false}") boolean systemIsSaas,
			@Value("${common.system-companys-id:0}") int systemCompanysId,
			@Value("${common.system-main-companys-id:}") String systemMainCompanysIdRaw) {
		this.h5JwtProperties = h5JwtProperties;
		this.companyDomainLookupService = companyDomainLookupService;
		this.wechatAuthorizerLookupMapper = wechatAuthorizerLookupMapper;
		this.aliMiniAppCompanyIdLookupPort = aliMiniAppCompanyIdLookupPort;
		this.memberAccountService = memberAccountService;
		this.h5JwtUserLoginInfoEnrichService = h5JwtUserLoginInfoEnrichService;
		this.objectMapper = objectMapper;
		this.systemIsSaas = systemIsSaas;
		this.systemCompanysId = systemCompanysId;
		this.systemMainCompanysIdRaw = systemMainCompanysIdRaw != null ? systemMainCompanysIdRaw : "";
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
		Integer resolved = resolveFromBearerJwt(request);
		if (resolved != null) {
			return true;
		}
		request.removeAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		try {
			int companyId = resolveAnonymousCompanyId(request);
			request.setAttribute(H5FrontAuthAttributes.H5_COMPANY_ID, companyId);
			String appid = request.getParameter("appid");
			if (!StringUtils.hasText(appid)) {
				appid = request.getHeader("authorizer-appid");
			}
			String openId = request.getParameter("open_id");
			if (!StringUtils.hasText(openId)) {
				openId = "";
			}
			String unionid = request.getParameter("union_id");
			if (!StringUtils.hasText(unionid)) {
				unionid = "";
			}
			Map<String, Object> anonAuth =
					h5JwtUserLoginInfoEnrichService.buildAnonymousAuthClaims(
							companyId, appid, openId, unionid);
			Object anonCompany = anonAuth.get("company_id");
			if (anonCompany instanceof Number n && n.longValue() > 0L && n.longValue() <= Integer.MAX_VALUE) {
				request.setAttribute(H5FrontAuthAttributes.H5_COMPANY_ID, n.intValue());
			}
			request.setAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS, anonAuth);
			return true;
		} catch (UnauthorizedException ex) {
			if (isNoAuthDecryptPhoneNumberHandler(hm)) {
				request.setAttribute(H5FrontAuthAttributes.H5_COMPANY_ID, 0);
				return true;
			}
			writeUnauthorizedJson(response, ex.getMessage(), hm);
			return false;
		}
	}

	private static boolean isNoAuthDecryptPhoneNumberHandler(HandlerMethod hm) {
		return "cn.shopex.ecshopx.members.api.front.v1.MembersController".equals(hm.getBeanType().getName())
				&& "getNoAuthDecryptPhoneNumber".equals(hm.getMethod().getName());
	}

	private static boolean hasAnnotation(HandlerMethod hm) {
		if (hm.getMethodAnnotation(FrontNoAuth.class) != null) {
			return true;
		}
		return hm.getBeanType().isAnnotationPresent(FrontNoAuth.class);
	}

	private Integer resolveFromBearerJwt(HttpServletRequest request) {
		String authHeader = request.getHeader("Authorization");
		if (!StringUtils.hasText(authHeader) || authHeader.length() < 7
				|| !authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
			return null;
		}
		String token = authHeader.substring(7).trim();
		if (!StringUtils.hasText(token)) {
			return null;
		}
		byte[] secret = h5JwtProperties.resolveSecretBytes();
		if (secret.length == 0) {
			return null;
		}
		try {
			SignedJWT jwt = SignedJWT.parse(token);
			if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())) {
				return null;
			}
			if (!jwt.verify(new MACVerifier(secret))) {
				return null;
			}
			JWTClaimsSet claims = jwt.getJWTClaimsSet();
			if (!h5JwtProperties.getIssuer().equals(claims.getIssuer())) {
				return null;
			}
			if (!validateTimeClaims(claims) || !validateRequiredClaims(claims)) {
				return null;
			}
			Integer resolved = parsePositiveCompanyIdClaim(claims.getClaim("company_id"));
			if (resolved == null) {
				return null;
			}
			request.setAttribute(H5FrontAuthAttributes.H5_COMPANY_ID, resolved);
			Map<String, Object> claimMap = new LinkedHashMap<>(claims.toJSONObject());
			h5JwtUserLoginInfoEnrichService.enrichClaims(claimMap);
			enrichClaimsWithMemberMobileIfMissing(claimMap);
			Object enrichedCompany = claimMap.get("company_id");
			Integer enrichedCid = parsePositiveCompanyIdClaim(enrichedCompany);
			if (enrichedCid != null) {
				request.setAttribute(H5FrontAuthAttributes.H5_COMPANY_ID, enrichedCid);
			}
			request.setAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS, claimMap);
			return enrichedCid != null ? enrichedCid : resolved;
		} catch (JOSEException | java.text.ParseException e) {
			return null;
		}
	}

	/**
	 * JWT custom claims omit {@code mobile} for some login paths; load the member row so H5 handlers
	 * see the same effective mobile as password-login payloads.
	 */
	private void enrichClaimsWithMemberMobileIfMissing(Map<String, Object> claimMap) {
		Object existing = claimMap.get("mobile");
		if (existing != null && StringUtils.hasText(String.valueOf(existing).trim())) {
			return;
		}
		long userId = parseLongClaim(claimMap.get("user_id"));
		long companyId = parseLongClaim(claimMap.get("company_id"));
		if (userId <= 0L || companyId <= 0L) {
			return;
		}
		String mobile = memberAccountService.resolveMemberMobileForH5Context(userId, companyId);
		if (StringUtils.hasText(mobile)) {
			claimMap.put("mobile", mobile);
		}
	}

	private static long parseLongClaim(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private boolean validateTimeClaims(JWTClaimsSet claims) {
		Instant now = Instant.now();
		long leewaySec = h5JwtProperties.getLeewaySeconds();
		Date exp = claims.getExpirationTime();
		if (exp != null && now.isAfter(exp.toInstant().plusSeconds(leewaySec))) {
			return false;
		}
		Date nbf = claims.getNotBeforeTime();
		if (nbf != null && now.plusSeconds(leewaySec).isBefore(nbf.toInstant())) {
			return false;
		}
		Date iat = claims.getIssueTime();
		if (iat != null && now.plusSeconds(leewaySec).isBefore(iat.toInstant())) {
			return false;
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

	private int resolveAnonymousCompanyId(HttpServletRequest request) {
		String appid = request.getParameter("appid");
		if (!StringUtils.hasText(appid)) {
			appid = request.getHeader("authorizer-appid");
		}
		if (StringUtils.hasText(appid)) {
			Integer companyId = null;
			if (appid.startsWith("wx")) {
				Long wid = wechatAuthorizerLookupMapper.selectCompanyIdByAuthorizerAppid(appid);
				if (wid != null && wid > 0 && wid <= Integer.MAX_VALUE) {
					companyId = wid.intValue();
				}
			} else {
				AliMiniAppCompanyIdLookupPort port = aliMiniAppCompanyIdLookupPort.getIfAvailable();
				if (port != null) {
					Long aid = port.findCompanyIdByAuthorizerAppid(appid);
					if (aid != null && aid > 0 && aid <= Integer.MAX_VALUE) {
						companyId = aid.intValue();
					}
				}
			}
			if (companyId == null || companyId <= 0) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
			return companyId;
		}

		Integer fixedSystem = resolveFixedSystemCompanyId();
		if (fixedSystem != null && fixedSystem > 0) {
			return fixedSystem;
		}

		Integer fromQuery = parsePositiveIntOrNull(request.getParameter("company_id"));
		if (fromQuery != null) {
			return fromQuery;
		}

		Integer fromDomain = resolveCompanyIdFromOriginDomain(request);
		if (fromDomain != null && fromDomain > 0) {
			return fromDomain;
		}

		throw new UnauthorizedException("无权访问该API,非法访问！");
	}

	/** PHP {@code FrontNoAuthMiddleWare}: non-saas / main-tenant config wins before query and Origin. */
	private Integer resolveFixedSystemCompanyId() {
		if (!systemIsSaas) {
			if (systemCompanysId > 0) {
				return systemCompanysId;
			}
		} else {
			Integer mainId = parsePositiveIntOrNull(systemMainCompanysIdRaw.trim());
			if (mainId != null && mainId > 0) {
				return mainId;
			}
		}
		return null;
	}

	/** PHP {@code FrontNoAuthMiddleWare}: query {@code company_id} before Origin domain lookup. */
	private Integer resolveCompanyIdFromOriginDomain(HttpServletRequest request) {
		Map<String, Object> cred = new LinkedHashMap<>();
		cred.put("origin", request.getHeader("Origin"));
		companyDomainLookupService.resolveAndPutCompanyId(cred);
		return objectToPositiveIntOrNull(cred.get("company_id"));
	}

	private static DingoResponse resolveDingoAnnotation(HandlerMethod hm) {
		DingoResponse ann = hm.getMethodAnnotation(DingoResponse.class);
		if (ann == null) {
			ann = hm.getBeanType().getAnnotation(DingoResponse.class);
		}
		return ann;
	}

	/**
	 * When {@code @DingoResponse(unauthorized = true)} is present on the handler, match Dingo-style
	 * error JSON (nested {@code data}) instead of root-level {@link ApiResult}, consistent with
	 * {@code GlobalExceptionHandler} for the same annotation.
	 */
	private void writeUnauthorizedJson(HttpServletResponse response, String message, HandlerMethod hm)
			throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType("application/json;charset=UTF-8");
		String msg = message != null && !message.isEmpty() ? message : "未授权";

		DingoResponse ann = resolveDingoAnnotation(hm);
		if (ann != null && ann.unauthorized()) {
			Map<String, Object> body;
			if ("该账号已被禁用.".equals(msg)) {
				Map<String, Object> data = new LinkedHashMap<>();
				data.put("message", "该账号已被禁用.");
				data.put("code", 401002);
				data.put("status_code", 401);
				body = Map.of("data", data);
			} else {
				Map<String, Object> data = new LinkedHashMap<>();
				data.put("message", "Unable to authenticate user.");
				data.put("code", 401001);
				data.put("status_code", 401);
				body = Map.of("data", data);
			}
			objectMapper.writeValue(response.getOutputStream(), body);
			return;
		}

		objectMapper.writeValue(response.getOutputStream(), ApiResult.fail(401, msg));
	}

	private static Integer parsePositiveCompanyIdClaim(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			return (v > 0 && v <= Integer.MAX_VALUE) ? (int) v : null;
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

	private static Integer objectToPositiveIntOrNull(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			long lv = n.longValue();
			return (lv > 0 && lv <= Integer.MAX_VALUE) ? (int) lv : null;
		}
		return parsePositiveIntOrNull(String.valueOf(o));
	}
}
