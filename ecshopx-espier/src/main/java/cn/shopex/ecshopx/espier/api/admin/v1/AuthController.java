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

package cn.shopex.ecshopx.espier.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.AdminLog;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.api.admin.v1.dto.OperatorLoginRequest;
import cn.shopex.ecshopx.espier.service.OperatorLoginFacade;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("espierAdminV1Auth")
@RequestMapping("/api/v1/operator")
public class AuthController {

	private final OperatorLoginFacade operatorLoginFacade;

	public AuthController(OperatorLoginFacade operatorLoginFacade) {
		this.operatorLoginFacade = operatorLoginFacade;
	}

	@AdminLog
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			unauthorizedHttpOk = true,
			notFound = true)
	@PostMapping(value = "/login", name = "登录")
	public ResponseEntity<ApiResult<Map<String, String>>> login(@FlexibleBody @Valid OperatorLoginRequest body) {
		String jwt = operatorLoginFacade.login(body);
		return ResponseEntity.ok(ApiResult.ok(Map.of("token", jwt)));
	}

	@AdminLog
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@PostMapping(value = "/getLevel", name = "获取登录验证等级")
	public ResponseEntity<ApiResult<Map<String, String>>> getLevel() {
		return ResponseEntity.ok(ApiResult.ok(Map.of("level", operatorLoginFacade.getLoginCheckLevel())));
	}
}
