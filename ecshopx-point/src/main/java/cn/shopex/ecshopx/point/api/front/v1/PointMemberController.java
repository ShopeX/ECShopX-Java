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

package cn.shopex.ecshopx.point.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.point.service.PointMemberInfoService;
import cn.shopex.ecshopx.point.service.PointMemberListService;
import jakarta.servlet.http.HttpServletRequest;
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
@FrontAuth
@RestController("pointMemberFrontV1")
@RequestMapping("/api/v1/h5app")
public class PointMemberController {

	private final PointMemberListService pointMemberListService;
	private final PointMemberInfoService pointMemberInfoService;

	public PointMemberController(
			PointMemberListService pointMemberListService, PointMemberInfoService pointMemberInfoService) {
		this.pointMemberListService = pointMemberListService;
		this.pointMemberInfoService = pointMemberInfoService;
	}

	@GetMapping(value = "/wxapp/point/member", name = "获取会员积分记录列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> lists(HttpServletRequest request,
			@RequestParam(value = "page_no", defaultValue = "1") int pageNo,
			@RequestParam(value = "page_size", defaultValue = "10") int pageSize,
			@RequestParam(value = "outin_type", required = false) String outinType) {
		long companyId = parseCompanyIdFromRequest(request);
		long userId = parseUserIdFromRequest(request);
		int pn = pageNo < 1 ? 1 : pageNo;
		int ps = pageSize < 1 ? 1 : pageSize;
		Map<String, Object> data = pointMemberListService.lists(companyId, userId, pn, ps, outinType);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/wxapp/point/member/info", name = "获取会员积分总数")
	public ResponseEntity<ApiResult<Map<String, Object>>> info(HttpServletRequest request) {
		long companyId = parseCompanyIdFromRequest(request);
		long userId = parseUserIdFromRequest(request);
		Map<String, Object> data = pointMemberInfoService.info(companyId, userId);
		return ResponseEntity.ok(ApiResult.ok(data));
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

	@SuppressWarnings("unchecked")
	private static long parseUserIdFromRequest(HttpServletRequest request) {
		Object rawClaims = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(rawClaims instanceof Map<?, ?> rawMap)) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		Map<String, Object> claims = (Map<String, Object>) rawMap;
		Object v = claims.get("user_id");
		long userId;
		if (v instanceof Number n) {
			userId = n.longValue();
		} else if (v != null) {
			try {
				userId = Long.parseLong(v.toString().trim());
			} catch (NumberFormatException e) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
		} else {
			userId = 0L;
		}
		if (userId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		return userId;
	}
}
