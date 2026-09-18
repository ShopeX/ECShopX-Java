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

package cn.shopex.ecshopx.theme.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.theme.service.PagesAdPlaceWxappAdListService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
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
@FrontNoAuth
@RequiredArgsConstructor
@RestController("themeFrontV1PagesAdPlace")
@RequestMapping("/api/v1/h5app/wxapp/ad")
public class PagesAdPlaceController {

	private final PagesAdPlaceWxappAdListService pagesAdPlaceWxappAdListService;

	@GetMapping(value = "/list", name = "广告列表")
	public ApiResult<List<Map<String, Object>>> getAdList(
			HttpServletRequest request,
			@RequestParam(name = "ad_type", required = false) String adType,
			@RequestParam(name = "page_type", required = false) String pageType,
			@RequestParam(name = "regionauth_id", required = false, defaultValue = "0") String regionauthIdParam,
			@RequestParam(name = "distributor_id", required = false) String distributorIdParam) {
		long companyId = parseCompanyIdFromRequest(request);
		long userId = parseAuthUserIdFromClaims(readH5AuthClaimsMap(request));
		long regionauthId = parseLongDefaultZero(regionauthIdParam);
		Long distributorId = parseOptionalPositiveLongQuery(distributorIdParam);
		List<Map<String, Object>> data =
				pagesAdPlaceWxappAdListService.getAdList(
						companyId, userId, adType, pageType, regionauthId, distributorId);
		return ApiResult.ok(data);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readH5AuthClaimsMap(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (raw instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		return Collections.emptyMap();
	}

	private static long parseCompanyIdFromRequest(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (companyAttr instanceof Number n) {
			companyId = n.longValue();
		} else if (companyAttr instanceof String s && StringUtils.hasText(s)) {
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

	private static long parseAuthUserIdFromClaims(Map<String, Object> claims) {
		Object v = claims == null ? null : claims.get("user_id");
		if (v == null) {
			return 0L;
		}
		try {
			long parsed = (v instanceof Number n) ? n.longValue() : Long.parseLong(String.valueOf(v).trim());
			return parsed > 0L ? parsed : 0L;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long parseLongDefaultZero(String raw) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return 0L;
		}
		String trim = raw.trim();
		try {
			long v = Long.parseLong(trim);
			if (v < 0L) {
				throw new BadRequestException("The regionauth id must be non-negative.");
			}
			return v;
		} catch (NumberFormatException ex) {
			throw new BadRequestException("The regionauth id must be an integer.");
		}
	}

	private static Long parseOptionalPositiveLongQuery(String raw) {
		if (raw == null) {
			return null;
		}
		String trim = raw.trim();
		if (trim.isEmpty()) {
			return null;
		}
		try {
			long v = Long.parseLong(trim);
			return v > 0L ? v : null;
		} catch (NumberFormatException ex) {
			throw new BadRequestException("The distributor id must be an integer.");
		}
	}
}
