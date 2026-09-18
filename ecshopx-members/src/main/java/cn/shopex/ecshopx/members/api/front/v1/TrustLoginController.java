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

package cn.shopex.ecshopx.members.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.members.service.admin.AdminTrustLoginListService;
import cn.shopex.ecshopx.members.service.front.TrustLoginParamsService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
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
		notFound = true)
@FrontNoAuth
@RestController("membersFrontV1TrustLogin")
@RequestMapping("/api/v1/h5app/wxapp")
public class TrustLoginController {

	private final AdminTrustLoginListService adminTrustLoginListService;
	private final TrustLoginParamsService trustLoginParamsService;

	public TrustLoginController(
			AdminTrustLoginListService adminTrustLoginListService,
			TrustLoginParamsService trustLoginParamsService) {
		this.adminTrustLoginListService = adminTrustLoginListService;
		this.trustLoginParamsService = trustLoginParamsService;
	}

	@GetMapping(value = "/trustlogin/params", name = "信任登录参数", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getTrustLoginParams(
			HttpServletRequest request,
			@RequestParam(name = "trustlogin_tag", required = false, defaultValue = "weixin") String trustloginTag,
			@RequestParam(name = "version_tag", required = false, defaultValue = "standard") String versionTag,
			@RequestParam(name = "redirect_url", required = false, defaultValue = "") String redirectUrl) {
		long companyId = requireCompanyId(request);
		String origin = request.getHeader("Origin");
		String h5Host = (origin != null && !origin.isBlank()) ? origin : "";
		Object data = trustLoginParamsService.getTrustLoginParams(companyId, trustloginTag, versionTag, h5Host, redirectUrl);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/trustlogin/list", name = "信任登录列表C端", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getTrustLoginList(
			HttpServletRequest request,
			@RequestParam(name = "version_tag", required = false, defaultValue = "standard") String versionTag) {
		long companyId = requireCompanyId(request);
		List<Map<String, Object>> list = adminTrustLoginListService.getTrustLoginListForFront(companyId, versionTag);
		return ResponseEntity.ok(ApiResult.ok(list));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readH5AuthClaimsMap(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (raw instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		return Collections.emptyMap();
	}

	private static long requireCompanyId(HttpServletRequest request) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long fromAttr = parsePositiveLongOrZero(request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID));
		if (fromAttr > 0L) {
			return fromAttr;
		}
		long fromClaims = parsePositiveLongOrZero(claims != null ? claims.get("company_id") : null);
		if (fromClaims > 0L) {
			return fromClaims;
		}
		long fromQuery = parsePositiveLongOrZero(FlexibleHttpServletParameterMap.toObjectMap(request).get("company_id"));
		if (fromQuery > 0L) {
			return fromQuery;
		}
		throw new UnauthorizedException("无权访问该API,非法访问！");
	}

	private static long parsePositiveLongOrZero(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof String s && StringUtils.hasText(s)) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
