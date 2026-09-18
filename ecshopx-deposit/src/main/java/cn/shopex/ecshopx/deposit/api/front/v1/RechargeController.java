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

package cn.shopex.ecshopx.deposit.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.deposit.service.DepositWxappMemberRechargeService;
import cn.shopex.ecshopx.deposit.service.RechargeAgreementReadService;
import cn.shopex.ecshopx.deposit.service.RechargeRuleListService;
import cn.shopex.ecshopx.members.service.h5.H5BearerJwtClaimsService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@FrontAuth
@DingoResponse(
    resource = DingoResponse.ResourceStyle.DINGO,
    badRequest = DingoResponse.BadRequestStyle.DINGO_400,
    unauthorized = true
)
@RestController("depositRechargeFrontV1")
@RequestMapping("/api/v1/h5app")
public class RechargeController {

	private final DepositWxappMemberRechargeService depositWxappMemberRechargeService;
	private final H5BearerJwtClaimsService h5BearerJwtClaimsService;
	private final RechargeAgreementReadService rechargeAgreementReadService;
	private final RechargeRuleListService rechargeRuleListService;

	public RechargeController(
			DepositWxappMemberRechargeService depositWxappMemberRechargeService,
			H5BearerJwtClaimsService h5BearerJwtClaimsService,
			RechargeAgreementReadService rechargeAgreementReadService,
			RechargeRuleListService rechargeRuleListService) {
		this.depositWxappMemberRechargeService = depositWxappMemberRechargeService;
		this.h5BearerJwtClaimsService = h5BearerJwtClaimsService;
		this.rechargeAgreementReadService = rechargeAgreementReadService;
		this.rechargeRuleListService = rechargeRuleListService;
	}

	@PostMapping("/wxapp/deposit/recharge")
	public ResponseEntity<ApiResult<Map<String, Object>>> recharge(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> claims =
				h5BearerJwtClaimsService
						.verifyAndExtractClaims(request)
						.orElseThrow(() -> new UnauthorizedException("Unable to authenticate user."));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		merged.put("client_ip", clientIp(request));
		Map<String, Object> data = depositWxappMemberRechargeService.recharge(claims, merged);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PostMapping("/wxapp/deposit/recharge_new")
	public ResponseEntity<ApiResult<Map<String, Object>>> rechargeNew(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> claims =
				h5BearerJwtClaimsService
						.verifyAndExtractClaims(request)
						.orElseThrow(() -> new UnauthorizedException("Unable to authenticate user."));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> data = depositWxappMemberRechargeService.rechargeNew(claims, merged);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping("/wxapp/deposit/rechargerules")
	public ResponseEntity<ApiResult<Map<String, Object>>> getRechargeRuleList(HttpServletRequest request) {
		Map<String, Object> claims =
				h5BearerJwtClaimsService
						.verifyAndExtractClaims(request)
						.orElseThrow(() -> new UnauthorizedException("Unable to authenticate user."));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}
		long companyId = requireCompanyIdFromClaims(claims);
		int pageSize = parsePositiveIntQuery(request, "pageSize", 20);
		int page = parsePositiveIntQuery(request, "page", 1);
		Map<String, Object> payload =
				rechargeRuleListService.getRechargeRuleListPageForWxapp(String.valueOf(companyId), pageSize, page);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@GetMapping("/wxapp/deposit/recharge/agreement")
	public ResponseEntity<ApiResult<Object>> getRechargeAgreementByCompanyId(HttpServletRequest request) {
		Map<String, Object> claims =
				h5BearerJwtClaimsService
						.verifyAndExtractClaims(request)
						.orElseThrow(() -> new UnauthorizedException("Unable to authenticate user."));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}
		long companyId = requireCompanyIdFromClaims(claims);
		Object payload = rechargeAgreementReadService.getAgreementPayload(companyId);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	private long requireCompanyIdFromClaims(Map<String, Object> claims) {
		Object raw = claims.get("company_id");
		if (raw == null) {
			throw new BadRequestException("缺少企业信息");
		}
		String trimmed = raw.toString().trim();
		if (!StringUtils.hasText(trimmed)) {
			throw new BadRequestException("缺少企业信息");
		}
		try {
			long id = Long.parseLong(trimmed);
			if (id <= 0) {
				throw new BadRequestException("缺少企业信息");
			}
			return id;
		} catch (NumberFormatException ex) {
			throw new BadRequestException("缺少企业信息");
		}
	}

	private Map<String, Object> mergeInputLikeFlexibleResolver(HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			LinkedHashMap<String, Object> input = new LinkedHashMap<>(parameterMapToMap(request));
			if (body != null) {
				input.putAll(body);
			}
			return input;
		}
		return body != null ? body : new LinkedHashMap<>();
	}

	private static Map<String, Object> parameterMapToMap(HttpServletRequest request) {
		Map<String, Object> m = new LinkedHashMap<>();
		request.getParameterMap().forEach((k, v) -> {
			if (v != null && v.length > 0 && StringUtils.hasText(v[0])) {
				m.put(k, v[0]);
			}
		});
		return m;
	}

	private static String clientIp(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		if (StringUtils.hasText(xff)) {
			int comma = xff.indexOf(',');
			return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
		}
		return request.getRemoteAddr() != null ? request.getRemoteAddr() : "";
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

	private static int parsePositiveIntQuery(HttpServletRequest request, String name, int defaultValue) {
		String v = request.getParameter(name);
		if (v == null || v.isEmpty()) {
			return defaultValue;
		}
		try {
			int n = Integer.parseInt(String.valueOf(v).trim());
			return n > 0 ? n : defaultValue;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
}
