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

package cn.shopex.ecshopx.bspay.api.admin.v1;

import cn.shopex.ecshopx.bspay.service.BsPayRegionsListService;
import cn.shopex.ecshopx.bspay.service.BsPayUserAuditStateService;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("bspayUserAdminV1")
@RequestMapping("/api/v1/bspay")
public class UserController {

	private final BsPayUserAuditStateService bsPayUserAuditStateService;
	private final BsPayRegionsListService bsPayRegionsListService;

	public UserController(
			BsPayUserAuditStateService bsPayUserAuditStateService,
			BsPayRegionsListService bsPayRegionsListService) {
		this.bsPayUserAuditStateService = bsPayUserAuditStateService;
		this.bsPayRegionsListService = bsPayRegionsListService;
	}

	@Activated(routeAlias = "bspay.user.audit_state")
	@GetMapping(value = "/user/audit_state", name = "用户对象审核状态")
	public ResponseEntity<ApiResult<Map<String, Object>>> getAuditState(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Map<String, Object> data = bsPayUserAuditStateService.getAuditState(jwtMap);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "bspay.regions")
	@GetMapping(value = "/regions", name = "获取二级所有地区")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getRegions() {
		List<Map<String, Object>> data = bsPayRegionsListService.getRegions();
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "bspay.regions.third")
	@GetMapping(value = "/regions/third", name = "获取三级所有地区")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getRegionsThird() {
		List<Map<String, Object>> data = bsPayRegionsListService.getRegionsThird();
		return ResponseEntity.ok(ApiResult.ok(data));
	}
}
