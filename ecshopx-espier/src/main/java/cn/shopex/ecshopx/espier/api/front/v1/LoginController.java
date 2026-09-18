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

package cn.shopex.ecshopx.espier.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.service.front.FrontWxappNewLoginService;
import cn.shopex.ecshopx.members.service.h5.H5LoginRequestAssembler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * H5 app login endpoints. When a request path creates a new member record, follow-on work is
 * coordinated from the member account layer after the surrounding transaction commits; this
 * controller forwards to application services and does not publish dispatch messages itself.
 */
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontNoAuth
@RestController("espierFrontV1Login")
@RequestMapping("/api/v1/h5app")
public class LoginController {

	private final H5LoginRequestAssembler h5LoginRequestAssembler;

	private final FrontWxappNewLoginService frontWxappNewLoginService;

	public LoginController(
			H5LoginRequestAssembler h5LoginRequestAssembler,
			FrontWxappNewLoginService frontWxappNewLoginService) {
		this.h5LoginRequestAssembler = h5LoginRequestAssembler;
		this.frontWxappNewLoginService = frontWxappNewLoginService;
	}

	@PostMapping(value = "/wxapp/new_login", name = "C端新登录")
	public ResponseEntity<Map<String, Object>> login(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = h5LoginRequestAssembler.merge(request, body);
		return ResponseEntity.ok(Map.of("data", frontWxappNewLoginService.login(request, merged)));
	}
}
