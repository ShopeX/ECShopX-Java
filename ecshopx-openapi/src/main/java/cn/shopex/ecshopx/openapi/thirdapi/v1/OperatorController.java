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
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.ExcOperatorResetpwdBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.ExcOperatorResetpwdSuccessEnvelopeDoc;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OpenAPI v1 handler scaffold — methods added during migration. */
@Tag(name = "开放接口-管理员账号V1")
@OpenapiResponse
@RestController("openapiV1Operator")
@RequestMapping("/api/openapi/internal/v1")
public class OperatorController extends OpenapiBaseController {

	private final OpenapiOperatorResetPasswordPort resetPasswordPort;

	public OperatorController(OpenapiOperatorResetPasswordPort resetPasswordPort) {
		this.resetPasswordPort = resetPasswordPort;
	}

	@Operation(
			summary = "管理员重置密码通知Token失效",
			description =
					"外部重置密码后通知本系统使管理员会话失效（不改密码本身）："
							+ " shopexid 为管理员手机号，按 operators.mobile 明文精确匹配 + operator_type=admin"
							+ "（跨租户 LIMIT 1，无 company_id 条件）；查无账号或 operator_id 非法报「账号信息不存在」。"
							+ " 命中后写 Redis 黑名单 black_token_{operatorId}_{operatorType}=当前秒（TTL 7 天，"
							+ " 该账号既有 JWT 需重新登录）并刷新 operators.updated。"
							+ " shopexid 必填非空白（mergeRequiredString，缺失报「shopexid必填」），"
							+ " 双通道：body 含键时以 body 为准（null/空白不回退 query），body 无键读 query 并 trim。"
							+ " 成功返回 {status:true} 由 OpenapiResponseBodyAdvice 包装（message 固定「成功」）。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=exc.operator.resetpwd, version=1.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：写 Redis 黑名单 + UPDATE operators.updated；无异步、无第三方调用、无事务注解。",
			operationId = "exc.operator.resetpwd.v1")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功, data={status:true}；"
								+ " 业务失败：status=fail, code=E0001（shopexid必填/账号信息不存在），data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 ExcOperatorResetpwdFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														ExcOperatorResetpwdSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "会话失效成功",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"status\":true}}"),
									@ExampleObject(
											name = "fail-missing-shopexid",
											summary = "shopexid 缺失/空白",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\",\"message\":\"shopexid必填\",\"data\":null}"),
									@ExampleObject(
											name = "fail-account-not-found",
											summary = "admin 账号查无",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\",\"message\":\"账号信息不存在\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/exc.operator.resetpwd", name = "管理员重置密码通知Token失效")
	public Map<String, Object> resetPassword(
			@Parameter(
							description =
									"管理员手机号（operators.mobile 明文 + operator_type=admin，跨租户 LIMIT 1）；"
											+ "必填非空白（body 同名键优先）",
							required = true,
							example = "13800000000")
					@RequestParam(name = "shopexid", required = false)
					String shopexIdParam,
			@RequestBody(
							description = "可选 JSON body；shopexid 与 query 同名双通道，body 含键时优先",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	ExcOperatorResetpwdBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		String shopexId =
				OpenapiRequestParams.mergeRequiredString(
						shopexIdParam, body, "shopexid", "shopexid必填");
		resetPasswordPort.invalidateAdminSession(shopexId);
		return Map.of("status", Boolean.TRUE);
	}
}
