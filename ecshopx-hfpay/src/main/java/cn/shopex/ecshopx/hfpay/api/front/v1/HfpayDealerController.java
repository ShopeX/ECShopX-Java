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

package cn.shopex.ecshopx.hfpay.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.hfpay.service.enterapply.HfpayDealerEnterapplySaveService;
import cn.shopex.ecshopx.hfpay.service.enterapply.HfpayEnterapplyReadService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
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
@RestController("hfpayDealerFrontV1")
@RequestMapping("/api/v1/h5app")
public class HfpayDealerController {

	private final HfpayDealerEnterapplySaveService hfpayDealerEnterapplySaveService;
	private final HfpayEnterapplyReadService hfpayEnterapplyReadService;

	public HfpayDealerController(
			HfpayDealerEnterapplySaveService hfpayDealerEnterapplySaveService,
			HfpayEnterapplyReadService hfpayEnterapplyReadService) {
		this.hfpayDealerEnterapplySaveService = hfpayDealerEnterapplySaveService;
		this.hfpayEnterapplyReadService = hfpayEnterapplyReadService;
	}

	@GetMapping(value = "/wxapp/hfpay/userapply", name = "获取经销员入驻信息")
	public ResponseEntity<ApiResult<Object>> getInfo(HttpServletRequest request) {
		long companyId = parseCompanyIdFromRequest(request);
		long userId = parseUserIdFromRequest(request);
		Map<String, Object> row = hfpayEnterapplyReadService.getEnterapplyByCompanyAndUser(companyId, userId);
		if (row == null) {
			return ResponseEntity.ok(ApiResult.ok(List.of()));
		}
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@PostMapping(value = "/wxapp/hfpay/applysave", name = "保存经销员入驻信息")
	public ResponseEntity<ApiResult<Map<String, Object>>> save(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = parseCompanyIdFromRequest(request);
		long userId = parseUserIdFromRequest(request);

		Map<String, Object> merged = body == null ? new LinkedHashMap<>() : new LinkedHashMap<>(body);
		merged.put("company_id", companyId);
		merged.put("user_id", userId);
		merged.put("apply_type", "3");
		merged.put("id_card_type", "10");

		Map<String, Object> data = hfpayDealerEnterapplySaveService.saveDealerApply(companyId, userId, merged);
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
