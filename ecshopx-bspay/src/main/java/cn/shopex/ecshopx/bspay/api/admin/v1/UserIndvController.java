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

import cn.shopex.ecshopx.bspay.service.UserEntGetService;
import cn.shopex.ecshopx.bspay.service.UserIndvCreateService;
import cn.shopex.ecshopx.bspay.service.UserIndvModifyService;
import cn.shopex.ecshopx.bspay.service.UserIndvUpdateService;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("bspayUserIndvAdminV1")
@RequestMapping("/api/v1/bspay")
public class UserIndvController {

	private final UserIndvCreateService userIndvCreateService;
	private final UserEntGetService userEntGetService;
	private final UserIndvModifyService userIndvModifyService;
	private final UserIndvUpdateService userIndvUpdateService;

	public UserIndvController(
			UserIndvCreateService userIndvCreateService,
			UserEntGetService userEntGetService,
			UserIndvModifyService userIndvModifyService,
			UserIndvUpdateService userIndvUpdateService) {
		this.userIndvCreateService = userIndvCreateService;
		this.userEntGetService = userEntGetService;
		this.userIndvModifyService = userIndvModifyService;
		this.userIndvUpdateService = userIndvUpdateService;
	}

	@Activated(routeAlias = "bspay.user_indv.info")
	@GetMapping(value = "/user_indv/get", name = "获取个人用户对象")
	public ResponseEntity<ApiResult<Map<String, Object>>> get(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (raw == null) {
			throw new UnauthorizedException("未登录");
		}
		if (!(raw instanceof Map<?, ?> rawMap)) {
			throw new UnauthorizedException("未登录");
		}
		if (rawMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) raw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		Map<String, Object> data = userEntGetService.get(companyId, jwtMap);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "bspay.user_indv.create")
	@PostMapping(value = "/user_indv/create", name = "创建个人用户对象")
	public ResponseEntity<ApiResult<Map<String, Object>>> create(
			HttpServletRequest request,
			@FlexibleBody Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (raw == null) {
			throw new UnauthorizedException("未登录");
		}
		if (!(raw instanceof Map<?, ?> rawMap)) {
			throw new UnauthorizedException("未登录");
		}
		if (rawMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) raw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		if (companyId <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		userIndvCreateService.create(companyId, jwtMap, body);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "bspay.user_indv.modify")
	@PostMapping(value = "/user_indv/modify", name = "修改个人用户对象(未开户)")
	public ResponseEntity<ApiResult<Map<String, Object>>> modify(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (raw == null) {
			throw new UnauthorizedException("未登录");
		}
		if (!(raw instanceof Map<?, ?> rawMap)) {
			throw new UnauthorizedException("未登录");
		}
		if (rawMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) raw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		if (companyId <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		userIndvModifyService.modify(companyId, jwtMap, body);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "bspay.user_indv.update")
	@PostMapping(value = "/user_indv/update", name = "更新个人用户对象")
	public ResponseEntity<ApiResult<Map<String, Object>>> update(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (raw == null) {
			throw new UnauthorizedException("未登录");
		}
		if (!(raw instanceof Map<?, ?> rawMap)) {
			throw new UnauthorizedException("未登录");
		}
		if (rawMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) raw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		if (companyId <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		userIndvUpdateService.update(companyId, jwtMap, body);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}
}
