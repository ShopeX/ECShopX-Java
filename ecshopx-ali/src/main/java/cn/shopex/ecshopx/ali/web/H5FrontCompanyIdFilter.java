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

package cn.shopex.ecshopx.ali.web;

import cn.shopex.ecshopx.ali.domain.AliMiniAppSetting;
import cn.shopex.ecshopx.ali.mapper.AliMiniAppSettingMapper;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.companys.service.domain.CompanyDomainLookupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.shopex.ecshopx.members.config.H5JwtProperties;
import cn.shopex.ecshopx.members.mapper.WechatAuthorizerLookupMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class H5FrontCompanyIdFilter extends OncePerRequestFilter {

	private final H5JwtProperties h5JwtProperties;

	private final CompanyDomainLookupService companyDomainLookupService;

	private final WechatAuthorizerLookupMapper wechatAuthorizerLookupMapper;

	private final AliMiniAppSettingMapper aliMiniAppSettingMapper;

	private final boolean systemIsSaas;

	private final int systemCompanysId;

	private final String systemMainCompanysIdRaw;

	private final ObjectMapper objectMapper;

	public H5FrontCompanyIdFilter(
			H5JwtProperties h5JwtProperties,
			CompanyDomainLookupService companyDomainLookupService,
			WechatAuthorizerLookupMapper wechatAuthorizerLookupMapper,
			AliMiniAppSettingMapper aliMiniAppSettingMapper,
			ObjectMapper objectMapper,
			@Value("${common.system-is-saas:false}") boolean systemIsSaas,
			@Value("${common.system-companys-id:0}") int systemCompanysId,
			@Value("${common.system-main-companys-id:}") String systemMainCompanysIdRaw) {
		this.h5JwtProperties = h5JwtProperties;
		this.companyDomainLookupService = companyDomainLookupService;
		this.wechatAuthorizerLookupMapper = wechatAuthorizerLookupMapper;
		this.aliMiniAppSettingMapper = aliMiniAppSettingMapper;
		this.objectMapper = objectMapper;
		this.systemIsSaas = systemIsSaas;
		this.systemCompanysId = systemCompanysId;
		this.systemMainCompanysIdRaw = systemMainCompanysIdRaw != null ? systemMainCompanysIdRaw : "";
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Integer resolved = resolveFromBearerJwt(request);
		if (resolved != null) {
			request.setAttribute(H5FrontAuthAttributes.H5_COMPANY_ID, resolved);
			filterChain.doFilter(request, response);
			return;
		}
		try {
			int companyId = resolveAnonymousCompanyId(request);
			request.setAttribute(H5FrontAuthAttributes.H5_COMPANY_ID, companyId);
			filterChain.doFilter(request, response);
		} catch (UnauthorizedException ex) {
			writeUnauthorizedJson(response, ex.getMessage());
		}
	}

	private void writeUnauthorizedJson(HttpServletResponse response, String message) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType("application/json;charset=UTF-8");
		String msg = message != null && !message.isEmpty() ? message : "未授权";
		objectMapper.writeValue(response.getOutputStream(), ApiResult.fail(401, msg));
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
			return parsePositiveCompanyIdClaim(claims.getClaim("company_id"));
		} catch (JOSEException | java.text.ParseException e) {
			return null;
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
				AliMiniAppSetting row = aliMiniAppSettingMapper.selectOne(new LambdaQueryWrapper<AliMiniAppSetting>()
						.eq(AliMiniAppSetting::getAuthorizerAppid, appid)
						.last("LIMIT 1"));
				if (row != null && row.getCompanyId() != null) {
					long lid = row.getCompanyId();
					if (lid > 0 && lid <= Integer.MAX_VALUE) {
						companyId = (int) lid;
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

	private Integer resolveCompanyIdFromOriginDomain(HttpServletRequest request) {
		Map<String, Object> cred = new LinkedHashMap<>();
		cred.put("origin", request.getHeader("Origin"));
		companyDomainLookupService.resolveAndPutCompanyId(cred);
		return objectToPositiveIntOrNull(cred.get("company_id"));
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
			if (lv <= 0 || lv > Integer.MAX_VALUE) {
				return null;
			}
			return (int) lv;
		}
		return parsePositiveIntOrNull(String.valueOf(o));
	}
}
