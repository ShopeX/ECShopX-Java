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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.deposit.service.DepositTradeListService;
import cn.shopex.ecshopx.deposit.service.UserDepositBalanceReadService;
import cn.shopex.ecshopx.deposit.service.WxappDepositToPointService;
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

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("depositDepositFrontV1")
@RequestMapping("/api/v1/h5app")
public class DepositController {

	private final WxappDepositToPointService wxappDepositToPointService;
	private final H5BearerJwtClaimsService h5BearerJwtClaimsService;
	private final UserDepositBalanceReadService userDepositBalanceReadService;
	private final DepositTradeListService depositTradeListService;

	public DepositController(
			WxappDepositToPointService wxappDepositToPointService,
			H5BearerJwtClaimsService h5BearerJwtClaimsService,
			UserDepositBalanceReadService userDepositBalanceReadService,
			DepositTradeListService depositTradeListService) {
		this.wxappDepositToPointService = wxappDepositToPointService;
		this.h5BearerJwtClaimsService = h5BearerJwtClaimsService;
		this.userDepositBalanceReadService = userDepositBalanceReadService;
		this.depositTradeListService = depositTradeListService;
	}

	@GetMapping("/wxapp/deposit/list")
	public ResponseEntity<ApiResult<Map<String, Object>>> lists(HttpServletRequest request) {
		Map<String, Object> claims =
				h5BearerJwtClaimsService
						.verifyAndExtractClaims(request)
						.orElseThrow(() -> new UnauthorizedException("Unable to authenticate user."));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}
		long companyId = parseCompanyIdFromClaims(claims);
		long userId = parseUserIdFromClaims(claims);
		String outinType = request.getParameter("outin_type");
		int pageSize = parsePositiveIntQuery(request, "pageSize", 20);
		int page = parsePositiveIntQuery(request, "page", 1);
		Map<String, Object> payload =
				depositTradeListService.getWxappMemberDepositTradeListPage(
						String.valueOf(companyId),
						String.valueOf(userId),
						outinType,
						pageSize,
						page);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@GetMapping("/wxapp/deposit/info")
	public ResponseEntity<ApiResult<Map<String, Object>>> info(HttpServletRequest request) {
		Map<String, Object> claims =
				h5BearerJwtClaimsService
						.verifyAndExtractClaims(request)
						.orElseThrow(() -> new UnauthorizedException("Unable to authenticate user."));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}
		long companyId = parseCompanyIdFromClaims(claims);
		long userId = parseUserIdFromClaims(claims);
		long depositFen = userDepositBalanceReadService.getUserDepositTotal(companyId, userId);
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("company_id", String.valueOf(companyId));
		data.put("user_id", String.valueOf(userId));
		data.put("deposit", String.valueOf(depositFen));
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PostMapping("/wxapp/deposit/to/point")
	public ResponseEntity<ApiResult<Map<String, Object>>> depositToPoint(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> claims =
				h5BearerJwtClaimsService
						.verifyAndExtractClaims(request)
						.orElseThrow(() -> new UnauthorizedException("Unable to authenticate user."));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		long moneyFen = parseMoneyFen(merged.get("money"));
		Map<String, Object> data = wxappDepositToPointService.execute(claims, moneyFen);
		return ResponseEntity.ok(ApiResult.ok(data));
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

	private static long parseMoneyFen(Object o) {
		if (o == null) {
			return 0L;
		}
		return Long.parseLong(o.toString().trim());
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

	private static long parseCompanyIdFromClaims(Map<String, Object> claims) {
		Object o = claims.get("company_id");
		if (o == null) {
			throw new ResourceException("商户ID必填");
		}
		String t = o.toString().trim();
		if (t.isEmpty()) {
			throw new ResourceException("商户ID必填");
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new ResourceException("商户ID必填");
		}
	}

	private static long parseUserIdFromClaims(Map<String, Object> claims) {
		Object o = claims.get("user_id");
		if (o == null) {
			throw new ResourceException("用户ID必填");
		}
		String t = o.toString().trim();
		if (t.isEmpty()) {
			throw new ResourceException("用户ID必填");
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new ResourceException("用户ID必填");
		}
	}
}
