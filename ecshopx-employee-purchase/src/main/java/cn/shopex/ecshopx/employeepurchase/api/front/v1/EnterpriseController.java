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

package cn.shopex.ecshopx.employeepurchase.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.employeepurchase.service.FrontEnterpriseListService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@RestController("employeepurchaseEnterpriseFrontV1")
@RequestMapping("/api/v1/h5app")
public class EnterpriseController {

	private final FrontEnterpriseListService frontEnterpriseListService;
	private final LangueProperties langueProperties;

	public EnterpriseController(
			FrontEnterpriseListService frontEnterpriseListService, LangueProperties langueProperties) {
		this.frontEnterpriseListService = frontEnterpriseListService;
		this.langueProperties = langueProperties;
	}

	@GetMapping("/wxapp/enterprises")
	@FrontNoAuth
	public ResponseEntity<ApiResult<Map<String, Object>>> getEnterprisesList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "enterprise_sn", required = false) String enterpriseSn,
			@RequestParam(value = "enterprise_id", required = false) Long enterpriseId) {
		long companyId = parseCompanyIdFromRequest(request);

		Map<String, Object> data =
				frontEnterpriseListService.listForWxappEnterprises(
						companyId, page, pageSize, enterpriseSn, enterpriseId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@GetMapping("/wxapp/user/enterprises")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getUserEnterprisesList(
			HttpServletRequest request,
			@RequestParam(value = "disabled", required = false) String disabled,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") long distributorId,
			@RequestParam(value = "activity_id", required = false, defaultValue = "0") long activityId) {
		long companyId = parseCompanyIdFromRequest(request);
		long userId = parseUserIdFromRequest(request);

		List<Map<String, Object>> list =
				frontEnterpriseListService.listUserEnterprises(
						companyId, userId, disabled, distributorId, activityId);
		return ResponseEntity.ok(ApiResult.ok(list));
	}

	@FrontAuth
	@GetMapping("/wxapp/user/enterprise/distributor")
	public ResponseEntity<ApiResult<Map<String, Object>>> getUserEnterpriseDistributor(
			HttpServletRequest request,
			@RequestParam(value = "enterprise_id", required = false) Long enterpriseId) {
		long companyId = parseCompanyIdFromRequest(request);

		if (enterpriseId == null || enterpriseId <= 0L) {
			throw new ResourceException("企业ID不能为空");
		}

		String requestLang = RequestLangTag.current(langueProperties);

		Map<String, Object> data =
				frontEnterpriseListService.getEnterpriseDistributorForUser(
						companyId, enterpriseId, requestLang);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long parseCompanyIdFromRequest(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (companyAttr instanceof Number n) {
			companyId = n.longValue();
		} else if (companyAttr instanceof String s && !s.isBlank()) {
			try {
				companyId = Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
		} else {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		return companyId;
	}

	private static long parseUserIdFromRequest(HttpServletRequest request) {
		Object rawClaims = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(rawClaims instanceof Map<?, ?> rawMap)) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> claims = (Map<String, Object>) rawMap;
		return parseUserIdFromClaims(claims);
	}

	private static long parseUserIdFromClaims(Map<String, Object> claims) {
		Object claimUid = claims.get("user_id");
		if (claimUid == null || !authUserIdTruthy(claimUid)) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		long userId = parseLongFlexible(claimUid, 0L);
		if (userId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		return userId;
	}

	private static boolean authUserIdTruthy(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Number n) {
			return n.longValue() > 0L;
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			return false;
		}
		try {
			return Long.parseLong(s) > 0L;
		} catch (NumberFormatException e) {
			return true;
		}
	}

	private static long parseLongFlexible(Object raw, long defaultVal) {
		if (raw == null) {
			return defaultVal;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}
}
