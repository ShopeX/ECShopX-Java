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
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.employeepurchase.service.StoreHomePageFrontDetailService;
import cn.shopex.ecshopx.members.service.h5.H5BearerJwtClaimsService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("employeepurchaseStoreHomePageFrontV1")
@RequestMapping("/api/v1/h5app")
public class StoreHomePageFrontController {

	private final H5BearerJwtClaimsService h5BearerJwtClaimsService;
	private final StoreHomePageFrontDetailService storeHomePageFrontDetailService;

	public StoreHomePageFrontController(
			H5BearerJwtClaimsService h5BearerJwtClaimsService,
			StoreHomePageFrontDetailService storeHomePageFrontDetailService) {
		this.h5BearerJwtClaimsService = h5BearerJwtClaimsService;
		this.storeHomePageFrontDetailService = storeHomePageFrontDetailService;
	}

	@GetMapping("/wxapp/employeepurchase/store-home-page/{id}")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDetail(
			HttpServletRequest request,
			@PathVariable("id") String id,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") String distributorIdRaw,
			@RequestParam(value = "e_activity_id", required = false, defaultValue = "0") String eActivityIdRaw) {
		Map<String, Object> claims =
				h5BearerJwtClaimsService
						.verifyAndExtractClaims(request)
						.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}

		long companyId = parseCompanyIdFromClaims(claims);
		long userId = parseUserIdFromClaims(claims);
		long storeHomePageId = parseRequiredPositiveLong(id, "id 无效");
		int authDistributorId = parseIntFlexible(distributorIdRaw, 0);
		long eActivityId = parseLongFlexible(eActivityIdRaw, 0L);

		Map<String, Object> data =
				storeHomePageFrontDetailService.getDetailForFront(
						companyId, authDistributorId, storeHomePageId, userId, eActivityId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long parseCompanyIdFromClaims(Map<String, Object> claims) {
		Object co = claims.get("company_id");
		if (co instanceof Number n) {
			long v = n.longValue();
			if (v <= 0L) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
			return v;
		}
		try {
			long v = Long.parseLong(co.toString().trim());
			if (v <= 0L) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
	}

	private static long parseUserIdFromClaims(Map<String, Object> claims) {
		Object claimUid = claims.get("user_id");
		if (claimUid == null) {
			return 0L;
		}
		if (claimUid instanceof Number n) {
			return Math.max(0L, n.longValue());
		}
		try {
			return Math.max(0L, Long.parseLong(claimUid.toString().trim()));
		} catch (NumberFormatException e) {
			return 0L;
		}
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

	private static long parseRequiredPositiveLong(String raw, String message) {
		if (!StringUtils.hasText(raw)) {
			throw new ResourceException(message);
		}
		try {
			long v = Long.parseLong(raw.trim());
			if (v <= 0L) {
				throw new ResourceException(message);
			}
			return v;
		} catch (NumberFormatException e) {
			throw new ResourceException(message);
		}
	}

	private static long parseLongFlexible(String raw, long defaultVal) {
		if (!StringUtils.hasText(raw)) {
			return defaultVal;
		}
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static int parseIntFlexible(String raw, int defaultVal) {
		if (!StringUtils.hasText(raw)) {
			return defaultVal;
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}
}
