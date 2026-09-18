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

package cn.shopex.ecshopx.companys.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.service.operator.OperatorAppSmsCodeService;
import cn.shopex.ecshopx.companys.web.CompanysAdminRequestMerge;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = false,
		notFound = false)
@RestController("companysAdminV1OperatorAppAnonymous")
@RequestMapping("/api/v1")
public class OperatorAppAnonymousController {

	private final OperatorAppSmsCodeService operatorAppSmsCodeService;

	public OperatorAppAnonymousController(OperatorAppSmsCodeService operatorAppSmsCodeService) {
		this.operatorAppSmsCodeService = operatorAppSmsCodeService;
	}

	@PostMapping(value = "/operator/app/sms/code", name = "App短信验证码")
	public ResponseEntity<ApiResult<Map<String, Object>>> getAppSmsCode(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> input = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		String mobile = stringVal(input.get("mobile"));
		String type = stringVal(input.get("type"));
		operatorAppSmsCodeService.sendAppSmsCode(mobile, type);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private static String stringVal(Object o) {
		if (o == null) {
			return null;
		}
		String s = o.toString().trim();
		return s.isEmpty() ? null : s;
	}
}
