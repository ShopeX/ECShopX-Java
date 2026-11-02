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

package cn.shopex.ecshopx.companys.api.pos.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.auth.PosJwtIssuerPort;
import cn.shopex.ecshopx.common.auth.PosJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.service.posauth.PosAuthMessages;
import cn.shopex.ecshopx.companys.service.posauth.PosAuthService;
import cn.shopex.ecshopx.companys.service.posauth.PosMeService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** POS 终端鉴权入口（ECX-10532 登录）。 */
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		unauthorizedPassMessage = true,
		notFound = false)
@RestController("companysPosV1PosAuth")
@RequestMapping("/api/v1/pos/auth")
public class PosAuthController {

	private final PosAuthService posAuthService;
	private final PosJwtIssuerPort posJwtIssuerPort;
	private final PosMeService posMeService;
	private final MessageSource messageSource;

	public PosAuthController(
			PosAuthService posAuthService,
			PosJwtIssuerPort posJwtIssuerPort,
			PosMeService posMeService,
			MessageSource messageSource) {
		this.posAuthService = posAuthService;
		this.posJwtIssuerPort = posJwtIssuerPort;
		this.posMeService = posMeService;
		this.messageSource = messageSource;
	}

	@PostMapping(value = "/login", name = "POS账号登录")
	public ResponseEntity<ApiResult<Map<String, Object>>> login(@FlexibleBody Map<String, Object> body) {
		Map<String, Object> b = body == null ? Map.of() : body;
		String employeeNo = str(b.get("employee_no"));
		String password = str(b.get("password"));
		return ResponseEntity.ok(ApiResult.ok(posAuthService.login(employeeNo, password)));
	}

	@PostMapping(value = "/logout", name = "POS账号登出")
	public ResponseEntity<ApiResult<Map<String, Object>>> logout(HttpServletRequest request) {
		if (request.getAttribute(PosJwtRequestAttributes.POS_JWT_USER_DATA) == null) {
			throw new UnauthorizedException(PosAuthMessages.loginInvalid(messageSource));
		}
		String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (auth == null || !auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
			throw new UnauthorizedException(PosAuthMessages.loginInvalid(messageSource));
		}
		String token = auth.substring(7).trim();
		posJwtIssuerPort.invalidate(token);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@GetMapping(value = "/me", name = "POS账号角色及权限")
	@SuppressWarnings("unchecked")
	public ResponseEntity<ApiResult<Map<String, Object>>> me(HttpServletRequest request) {
		Object raw = request.getAttribute(PosJwtRequestAttributes.POS_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException(PosAuthMessages.loginInvalid(messageSource));
		}
		return ResponseEntity.ok(ApiResult.ok(posMeService.me((Map<String, Object>) raw)));
	}

	private static String str(Object v) {
		return v == null ? "" : v.toString();
	}
}
