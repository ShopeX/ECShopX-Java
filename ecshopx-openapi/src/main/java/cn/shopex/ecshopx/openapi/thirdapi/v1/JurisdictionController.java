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
import cn.shopex.ecshopx.common.openapi.OpenapiEnvelope;
import cn.shopex.ecshopx.common.openapi.OpenapiJurisdictionGetuserPort;
import cn.shopex.ecshopx.common.openapi.OpenapiJurisdictionSysuserPort;
import cn.shopex.ecshopx.common.openapi.OpenapiJurisdictionSysuserPort.OpenapiJurisdictionSysuserInput;
import cn.shopex.ecshopx.common.openapi.OpenapiJurisdictionSysuserPort.SysuserBusinessFail;
import cn.shopex.ecshopx.common.openapi.OpenapiJurisdictionSysuserPort.SysuserLegacyZeroCode;
import cn.shopex.ecshopx.common.openapi.OpenapiJurisdictionSysuserPort.SysuserResult;
import cn.shopex.ecshopx.common.openapi.OpenapiJurisdictionSysuserPort.SysuserSuccess;
import cn.shopex.ecshopx.common.openapi.OpenapiLegacyZeroCodeFailException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OpenAPI v1 handler scaffold — methods added during migration. */
@OpenapiResponse
@RestController("openapiV1Jurisdiction")
@RequestMapping("/api/openapi/internal/v1")
public class JurisdictionController extends OpenapiBaseController {

	private final OpenapiJurisdictionSysuserPort sysuserPort;

	private final OpenapiJurisdictionGetuserPort getuserPort;

	public JurisdictionController(
			OpenapiJurisdictionSysuserPort sysuserPort, OpenapiJurisdictionGetuserPort getuserPort) {
		this.sysuserPort = sysuserPort;
		this.getuserPort = getuserPort;
	}

	@PostMapping("/ecx.jurisdiction.role")
	public OpenapiEnvelope role() {
		return OpenapiEnvelope.fail("E4007", "找不到方法", null);
	}

	@PostMapping("/ecx.jurisdiction.getuser")
	public OpenapiEnvelope getuser(
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "type", required = false) String typeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		String mobile = OpenapiRequestParams.mergeString(mobileParam, body, "mobile");
		String type = OpenapiRequestParams.mergeString(typeParam, body, "type");
		Optional<Map<String, Object>> operator = getuserPort.findStaffOperator(mobile, type);
		if (operator.isPresent()) {
			return new OpenapiEnvelope("success", "E0000", "操作成功", operator.get());
		}
		return OpenapiEnvelope.fail("E0001", "操作失败", List.of());
	}

	@PostMapping("/ecx.jurisdiction.sysuser")
	public OpenapiEnvelope sysuser(
			HttpServletRequest request,
			@RequestParam(name = "shopexid", required = false) String shopexIdParam,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "operator_type", required = false) String operatorTypeParam,
			@RequestParam(name = "login_name", required = false) String loginNameParam,
			@RequestParam(name = "username", required = false) String usernameParam,
			@RequestParam(name = "password", required = false) String passwordParam,
			@RequestParam(name = "eid", required = false) String eidParam,
			@RequestParam(name = "passport_uid", required = false) String passportUidParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiJurisdictionSysuserInput input =
				new OpenapiJurisdictionSysuserInput(
						OpenapiRequestParams.mergeString(shopexIdParam, body, "shopexid"),
						OpenapiRequestParams.mergeString(mobileParam, body, "mobile"),
						OpenapiRequestParams.mergeString(operatorTypeParam, body, "operator_type"),
						OpenapiRequestParams.mergeString(loginNameParam, body, "login_name"),
						OpenapiRequestParams.mergeString(usernameParam, body, "username"),
						OpenapiRequestParams.mergeString(passwordParam, body, "password"),
						OpenapiRequestParams.mergeString(eidParam, body, "eid"),
						OpenapiRequestParams.mergeString(passportUidParam, body, "passport_uid"));
		SysuserResult result = sysuserPort.syncSysuser(companyId, input);
		if (result instanceof SysuserSuccess) {
			return new OpenapiEnvelope(
					"success", "E0000", "操作成功", Map.of("status", true, "message", "保存成功"));
		}
		if (result instanceof SysuserBusinessFail fail) {
			return OpenapiEnvelope.fail("E0001", "操作失败", nestedFail(fail.dataMessage()));
		}
		if (result instanceof SysuserLegacyZeroCode legacy) {
			throw new OpenapiLegacyZeroCodeFailException(legacy.message());
		}
		throw new IllegalStateException("Unexpected SysuserResult: " + result);
	}

	private static Map<String, Object> nestedFail(String dataMessage) {
		Map<String, Object> nested = new LinkedHashMap<>();
		nested.put("status", false);
		nested.put("message", dataMessage);
		return nested;
	}
}
