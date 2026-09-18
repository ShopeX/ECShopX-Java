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

package cn.shopex.ecshopx.shuyun.auth;

import cn.shopex.ecshopx.shuyun.config.ShuyunOpenPlatformProperties;
import cn.shopex.ecshopx.shuyun.domain.CompanyShuyunOpenPlatformConfig;
import cn.shopex.ecshopx.shuyun.domain.ShuyunOfflineBenefit;
import cn.shopex.ecshopx.shuyun.mapper.ShuyunOfflineBenefitMapper;
import cn.shopex.ecshopx.shuyun.service.openplatform.OpenPlatformConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 入站验签 + 租户解析。对齐 PHP {@code ShuyunOpenPlatformInboundSignedCallbackPreparer}。
 */
@Service
public class InboundSignedCallbackPreparer {

	private final OpenPlatformConfigService openPlatformConfigService;
	private final ShuyunOfflineBenefitMapper offlineBenefitMapper;
	private final CallbackSignatureVerifier signatureVerifier;
	private final ShuyunOpenPlatformProperties properties;
	private final ObjectMapper objectMapper;

	public InboundSignedCallbackPreparer(
			OpenPlatformConfigService openPlatformConfigService,
			ShuyunOfflineBenefitMapper offlineBenefitMapper,
			CallbackSignatureVerifier signatureVerifier,
			ShuyunOpenPlatformProperties properties,
			ObjectMapper objectMapper) {
		this.openPlatformConfigService = openPlatformConfigService;
		this.offlineBenefitMapper = offlineBenefitMapper;
		this.signatureVerifier = signatureVerifier;
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	public InboundPrepareResult prepare(HttpServletRequest request, String rawBody, InboundSignedPrepareMode mode) {
		Map<String, Object> decoded;
		try {
			Object root = objectMapper.readValue(rawBody == null ? "" : rawBody, Object.class);
			if (!(root instanceof Map<?, ?>)) {
				return err(invalidJson(mode));
			}
			decoded = objectMapper.convertValue(root, new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			return err(invalidJson(mode));
		}

		String appIdFromQuery = trimStringish(request.getParameter("appId"));
		String appIdFromBody = trimStringish(decoded.get("appId"));
		String platCodeFromBody = resolvePlatCodeForTenant(decoded);

		CompanyShuyunOpenPlatformConfig config = null;
		InboundPrepareResult early = null;
		if (StringUtils.hasText(appIdFromQuery)) {
			config = openPlatformConfigService.findByAppId(appIdFromQuery);
		} else if (StringUtils.hasText(appIdFromBody)) {
			config = openPlatformConfigService.findByAppId(appIdFromBody);
		} else if (StringUtils.hasText(platCodeFromBody)) {
			List<CompanyShuyunOpenPlatformConfig> byPlat = findEnabledByPlatCode(platCodeFromBody);
			if (byPlat.size() > 1) {
				return err(ambiguousPlatCode(mode));
			}
			config = byPlat.isEmpty() ? null : byPlat.get(0);
		}

		if (config == null && mode == InboundSignedPrepareMode.OFFLINE_BENEFIT) {
			ResolveShadow r = resolveByShadowBenefitId(decoded, mode);
			if (r.err != null) {
				return err(r.err);
			}
			config = r.config;
		}

		if (config == null) {
			String benefitIdOnly = trimStringish(decoded.get("benefitId"));
			if (mode == InboundSignedPrepareMode.OFFLINE_BENEFIT && StringUtils.hasText(benefitIdOnly)) {
				return err(offlineBenefitUnknownShadow());
			}
			if (!StringUtils.hasText(appIdFromQuery)
					&& !StringUtils.hasText(appIdFromBody)
					&& !StringUtils.hasText(platCodeFromBody)) {
				return err(appIdRequired(mode));
			}
			return err(unknownApp(mode));
		}

		if (mode == InboundSignedPrepareMode.LOYALTY_MEMBER_GRADE_CHANGE) {
			if (config.getIsEnabled() == null || config.getIsEnabled() != 1) {
				return err(loyalty("40303", "LOYALTY_GRADE_CALLBACK_DISABLED"));
			}
		}

		String callbackSecret =
				properties.getCallbackIdentitySecret() == null
						? ""
						: properties.getCallbackIdentitySecret().trim();
		if (!StringUtils.hasText(callbackSecret)) {
			return err(secretNotConfigured(mode));
		}

		String sign = resolveCallbackSign(request);
		if (!signatureVerifier.verifyHttpCallback(callbackSecret, request, sign)) {
			return err(invalidSign(mode));
		}

		if (mode == InboundSignedPrepareMode.OFFLINE_BENEFIT && !isEligible(config)) {
			Map<String, Object> body = new LinkedHashMap<>();
			body.put("code", 403);
			body.put("msg", "SHUYUN_OFFLINE_BENEFIT_NOT_ELIGIBLE");
			return err(ResponseEntity.status(403).body(body));
		}

		return new InboundPrepareResult.Ok(new PreparedInboundCallback(config.getCompanyId(), decoded));
	}

	public String resolvePlatCodeForTenant(Map<String, Object> decoded) {
		String pc = trimStringish(decoded.get("platCode"));
		if (StringUtils.hasText(pc)) {
			return pc;
		}
		Object shops = decoded.get("limitShops");
		if (!(shops instanceof List<?> list)) {
			return "";
		}
		for (Object row : list) {
			if (row instanceof Map<?, ?> m) {
				String p = trimStringish(m.get("platCode"));
				if (StringUtils.hasText(p)) {
					return p;
				}
			}
		}
		return "";
	}

	public String resolveCallbackSign(HttpServletRequest request) {
		String fromQuery = request.getParameter("sign");
		if (StringUtils.hasText(fromQuery)) {
			return fromQuery.trim();
		}
		for (String name : List.of("SY-Request-Sign", "Sy-Request-Sign")) {
			String h = request.getHeader(name);
			if (StringUtils.hasText(h)) {
				return h.trim();
			}
		}
		return "";
	}

	public static boolean isEligible(CompanyShuyunOpenPlatformConfig row) {
		if (row == null) {
			return false;
		}
		if (!StringUtils.hasText(row.getAuthValue())) {
			return false;
		}
		if (row.getIsEnabled() == null || row.getIsEnabled() != 1) {
			return false;
		}
		if (!StringUtils.hasText(row.getAppId()) || !StringUtils.hasText(row.getAppSecret())) {
			return false;
		}
		if (!StringUtils.hasText(row.getAccessToken())) {
			return false;
		}
		return !"1".equals(row.getIsOverDue());
	}

	private List<CompanyShuyunOpenPlatformConfig> findEnabledByPlatCode(String platCode) {
		String normalized = platCode.trim().toLowerCase(Locale.ROOT);
		return openPlatformConfigService.findEnabledByNormalizedPlatCode(normalized);
	}

	private ResolveShadow resolveByShadowBenefitId(Map<String, Object> decoded, InboundSignedPrepareMode mode) {
		String bid = trimStringish(decoded.get("benefitId"));
		if (!StringUtils.hasText(bid)) {
			return ResolveShadow.empty();
		}
		List<ShuyunOfflineBenefit> rows =
				offlineBenefitMapper.selectList(
						new LambdaQueryWrapper<ShuyunOfflineBenefit>()
								.eq(ShuyunOfflineBenefit::getBenefitId, bid));
		if (rows.isEmpty()) {
			return ResolveShadow.empty();
		}
		if (rows.size() > 1) {
			return ResolveShadow.error(ambiguousBenefitId(mode));
		}
		Long companyId = rows.get(0).getCompanyId();
		CompanyShuyunOpenPlatformConfig config =
				companyId == null ? null : openPlatformConfigService.findByCompanyId(companyId);
		return ResolveShadow.ok(config);
	}

	private static String trimStringish(Object value) {
		if (value == null) {
			return "";
		}
		if (value instanceof String || value instanceof Number) {
			return String.valueOf(value).trim();
		}
		return "";
	}

	private static InboundPrepareResult err(ResponseEntity<Map<String, Object>> response) {
		return new InboundPrepareResult.Err(response);
	}

	private static ResponseEntity<Map<String, Object>> loyalty(String code, String message) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("msg", message);
		body.put("code", code);
		body.put("success", "false");
		return ResponseEntity.ok(body);
	}

	private static ResponseEntity<Map<String, Object>> offline(int http, int code, String msg) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("code", code);
		body.put("msg", msg);
		return ResponseEntity.status(http).body(body);
	}

	private static ResponseEntity<Map<String, Object>> invalidJson(InboundSignedPrepareMode mode) {
		return mode == InboundSignedPrepareMode.LOYALTY_MEMBER_GRADE_CHANGE
				? loyalty("40001", "INVALID_JSON")
				: offline(400, 400, "INVALID_JSON");
	}

	private static ResponseEntity<Map<String, Object>> appIdRequired(InboundSignedPrepareMode mode) {
		return mode == InboundSignedPrepareMode.LOYALTY_MEMBER_GRADE_CHANGE
				? loyalty("40301", "APP_ID_REQUIRED")
				: offline(403, 403, "APP_ID_REQUIRED");
	}

	private static ResponseEntity<Map<String, Object>> unknownApp(InboundSignedPrepareMode mode) {
		return mode == InboundSignedPrepareMode.LOYALTY_MEMBER_GRADE_CHANGE
				? loyalty("40302", "UNKNOWN_APP")
				: offline(403, 403, "UNKNOWN_APP");
	}

	private static ResponseEntity<Map<String, Object>> secretNotConfigured(InboundSignedPrepareMode mode) {
		return mode == InboundSignedPrepareMode.LOYALTY_MEMBER_GRADE_CHANGE
				? loyalty("40304", "CALLBACK_IDENTITY_SECRET_NOT_CONFIGURED")
				: offline(403, 403, "CALLBACK_IDENTITY_SECRET_NOT_CONFIGURED");
	}

	private static ResponseEntity<Map<String, Object>> invalidSign(InboundSignedPrepareMode mode) {
		return mode == InboundSignedPrepareMode.LOYALTY_MEMBER_GRADE_CHANGE
				? loyalty("40305", "INVALID_SIGN")
				: offline(403, 403, "INVALID_SIGN");
	}

	private static ResponseEntity<Map<String, Object>> ambiguousPlatCode(InboundSignedPrepareMode mode) {
		return mode == InboundSignedPrepareMode.LOYALTY_MEMBER_GRADE_CHANGE
				? loyalty("40307", "AMBIGUOUS_PLAT_CODE")
				: offline(403, 403, "AMBIGUOUS_PLAT_CODE");
	}

	private static ResponseEntity<Map<String, Object>> ambiguousBenefitId(InboundSignedPrepareMode mode) {
		return mode == InboundSignedPrepareMode.LOYALTY_MEMBER_GRADE_CHANGE
				? loyalty("40308", "AMBIGUOUS_BENEFIT_ID")
				: offline(403, 403, "AMBIGUOUS_BENEFIT_ID");
	}

	private static ResponseEntity<Map<String, Object>> offlineBenefitUnknownShadow() {
		return offline(403, 403, "OFFLINE_BENEFIT_NOT_REGISTERED");
	}

	private static final class ResolveShadow {
		final CompanyShuyunOpenPlatformConfig config;
		final ResponseEntity<Map<String, Object>> err;

		private ResolveShadow(CompanyShuyunOpenPlatformConfig config, ResponseEntity<Map<String, Object>> err) {
			this.config = config;
			this.err = err;
		}

		static ResolveShadow empty() {
			return new ResolveShadow(null, null);
		}

		static ResolveShadow ok(CompanyShuyunOpenPlatformConfig config) {
			return new ResolveShadow(config, null);
		}

		static ResolveShadow error(ResponseEntity<Map<String, Object>> err) {
			return new ResolveShadow(null, err);
		}
	}
}
