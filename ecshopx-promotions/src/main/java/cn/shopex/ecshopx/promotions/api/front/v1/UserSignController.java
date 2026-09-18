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

package cn.shopex.ecshopx.promotions.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.promotions.service.usersign.UserSignFrontSignInService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("promotionsFrontV1UserSign")
@RequestMapping("/api/v1/h5app/wxapp")
public class UserSignController {

	private final UserSignFrontSignInService userSignFrontSignInService;

	public UserSignController(UserSignFrontSignInService userSignFrontSignInService) {
		this.userSignFrontSignInService = userSignFrontSignInService;
	}

	@PostMapping(value = "/sign", name = "用户签到", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> signIn(HttpServletRequest request, Locale locale) {
		long companyId = parseCompanyIdFromRequest(request);
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long userId = parseAuthUserIdFromClaims(claims);
		if (userId <= 0L) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		boolean matchedRule = userSignFrontSignInService.signIn(companyId, userId, locale);
		return toSignInApiResult(matchedRule);
	}

	@GetMapping(value = "/sign/weekly/list", name = "签到周列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getUserSignWeeklyList(HttpServletRequest request, Locale locale) {
		long companyId = parseCompanyIdFromRequest(request);
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long userId = parseAuthUserIdFromClaims(claims);
		if (userId <= 0L) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		boolean matchedRule = userSignFrontSignInService.signIn(companyId, userId, locale);
		return toSignInApiResult(matchedRule);
	}

	private ResponseEntity<ApiResult<Object>> toSignInApiResult(boolean matchedRule) {
		if (!matchedRule) {
			return ResponseEntity.ok(ApiResult.ok(new LinkedHashMap<>()));
		}
		return ResponseEntity.ok(ApiResult.ok(null));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readH5AuthClaimsMap(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (raw instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		return Collections.emptyMap();
	}

	private static long parseAuthUserIdFromClaims(Map<String, Object> claims) {
		Object v = claims == null ? null : claims.get("user_id");
		if (v == null) {
			return 0L;
		}
		try {
			long id = (v instanceof Number n) ? n.longValue() : Long.parseLong(String.valueOf(v).trim());
			return id > 0L ? id : 0L;
		} catch (NumberFormatException e) {
			return 0L;
		}
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
}
