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

package cn.shopex.ecshopx.openapi.thirdapi.v1;

import cn.shopex.ecshopx.common.annotation.OpenapiResponse;
import cn.shopex.ecshopx.common.openapi.OpenapiOperatorResetPasswordPort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OpenAPI v1 handler scaffold — methods added during migration. */
@OpenapiResponse
@RestController("openapiV1Operator")
@RequestMapping("/api/openapi/internal/v1")
public class OperatorController extends OpenapiBaseController {

	private final OpenapiOperatorResetPasswordPort resetPasswordPort;

	public OperatorController(OpenapiOperatorResetPasswordPort resetPasswordPort) {
		this.resetPasswordPort = resetPasswordPort;
	}

	@PostMapping(value = "/exc.operator.resetpwd", name = "管理员重置密码通知Token失效")
	public Map<String, Object> resetPassword(
			@RequestParam(name = "shopexid", required = false) String shopexIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		String shopexId =
				OpenapiRequestParams.mergeRequiredString(
						shopexIdParam, body, "shopexid", "shopexid必填");
		resetPasswordPort.invalidateAdminSession(shopexId);
		return Map.of("status", Boolean.TRUE);
	}
}
