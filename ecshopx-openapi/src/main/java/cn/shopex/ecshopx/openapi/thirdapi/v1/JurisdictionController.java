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
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxJurisdictionGetuserBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxJurisdictionGetuserSuccessEnvelopeDoc;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "开放接口-账号权限")
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

	@Operation(
			summary = "开放接口查询员工账号",
			description =
					"按手机号查询员工账号（operators，operator_type=staff），返回 25 字段单对象"
							+ "（含 shop_ids/distributor_ids JSON 列解码，缺失键补 null）。"
							+ " 本方法不读取鉴权上下文 company_id（跨租户按 mobile+staff 定位，对齐 PHP 旧契约）；"
							+ " 公网入口仍经 openapi sign 签名（method=ecx.jurisdiction.getuser, version=1.0）转发；"
							+ " 直连 internal 路径不做鉴权校验。"
							+ " 参数双通道：body 含同名键时以 body 为准（null/空白不回退 query），否则读 query。"
							+ " type 仅 \"staff\" 查库；非 staff 或查无记录返回 status=fail, code=E0001「操作失败」，"
							+ " 且 data 为空数组 []（非 null）。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.jurisdiction.getuser")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态。"
								+ " 成功：status=success, code=E0000, message=操作成功，data 见 EcxJurisdictionGetuserDataDoc；"
								+ " 查无员工/type 非 staff：status=fail, code=E0001, message=操作失败, data=[]；"
								+ " 网关签名失败：E4001/E4002/E4003/E4004（data 为回显请求参数）。见 EcxJurisdictionGetuserFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxJurisdictionGetuserSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"操作成功\","
															+ "\"data\":{\"operator_id\":5,\"mobile\":\"13800000000\","
															+ "\"login_name\":null,\"operator_type\":\"staff\",\"password\":null,"
															+ "\"company_id\":100,\"is_disable\":false,\"merchant_id\":null,"
															+ "\"distributor_ids\":[{\"distributor_id\":5,\"name\":\"示例门店\"}],"
															+ "\"shop_ids\":[],\"username\":\"张三\",\"head_portrait\":null,"
															+ "\"regionauth_id\":0,\"contact\":null,\"split_ledger_info\":null,"
															+ "\"adapay_open_account_time\":null,\"dealer_parent_id\":null,"
															+ "\"is_dealer_main\":null,\"is_distributor_main\":null,"
															+ "\"shopex_bind_account\":null,\"eid\":null,\"passport_uid\":null,"
															+ "\"created\":1764115200,\"updated\":null,\"is_merchant_main\":null}}"),
									@ExampleObject(
											name = "fail-not-found",
											summary = "查无员工或 type 非 staff（data 为空数组）",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\",\"message\":\"操作失败\",\"data\":[]}"),
									@ExampleObject(
											name = "fail-sign",
											summary = "公网网关签名失败",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"sign 不合法\","
															+ "\"data\":{\"method\":\"ecx.jurisdiction.getuser\"}}")
								}))
	})
	@PostMapping("/ecx.jurisdiction.getuser")
	public OpenapiEnvelope getuser(
			@Parameter(description = "员工手机号（operators.mobile 精确匹配）（body 同名键优先）", example = "13800000000")
					@RequestParam(name = "mobile", required = false)
					String mobileParam,
			@Parameter(
							description = "账号类型；仅 \"staff\" 查库，其他值返回 E0001 操作失败（body 同名键优先）",
							required = true,
							schema = @Schema(allowableValues = {"staff"}),
							example = "staff")
					@RequestParam(name = "type", required = false)
					String typeParam,
			@RequestBody(
							description =
									"可选 JSON body；字段与 query 同名双通道，body 含键时优先"
											+ "（null/空白视为未提供且不回退 query）",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxJurisdictionGetuserBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
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
