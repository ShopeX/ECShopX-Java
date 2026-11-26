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
import cn.shopex.ecshopx.common.openapi.OpenapiSalespersonBathStatusUpdatePort;
import cn.shopex.ecshopx.common.openapi.OpenapiSalespersonDestroyPort;
import cn.shopex.ecshopx.common.openapi.OpenapiSalespersonPushPort;
import cn.shopex.ecshopx.common.openapi.OpenapiSalespersonPushPort.OpenapiSalespersonPushInput;
import cn.shopex.ecshopx.common.openapi.OpenapiSalespersonUpdateStoresPort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxSalespersonBathStatusUpdateBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxSalespersonBathStatusUpdateSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxSalespersonDestroyBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxSalespersonDestroySuccessEnvelopeDoc;
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
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OpenAPI v1 handler scaffold — methods added during migration. */
@Tag(name = "开放接口-导购V1")
@OpenapiResponse
@RestController("openapiV1ShopSalesperson")
@RequestMapping("/api/openapi/internal/v1")
public class ShopSalespersonController extends OpenapiBaseController {

	private final OpenapiSalespersonPushPort salespersonPushPort;
	private final OpenapiSalespersonBathStatusUpdatePort bathStatusUpdatePort;
	private final OpenapiSalespersonDestroyPort destroyPort;
	private final OpenapiSalespersonUpdateStoresPort updateStoresPort;

	public ShopSalespersonController(
			OpenapiSalespersonPushPort salespersonPushPort,
			OpenapiSalespersonBathStatusUpdatePort bathStatusUpdatePort,
			OpenapiSalespersonDestroyPort destroyPort,
			OpenapiSalespersonUpdateStoresPort updateStoresPort) {
		this.salespersonPushPort = salespersonPushPort;
		this.bathStatusUpdatePort = bathStatusUpdatePort;
		this.destroyPort = destroyPort;
		this.updateStoresPort = updateStoresPort;
	}

	@PostMapping(value = "/ecx.salesperson.push", name = "新增编辑导购员同步")
	public OpenapiEnvelope pushSalesperson(
			HttpServletRequest request,
			@RequestParam(name = "salesperson_name", required = false) String salespersonNameParam,
			@RequestParam(name = "employee_status", required = false) String employeeStatusParam,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "work_userid", required = false) String workUseridParam,
			@RequestParam(name = "salesperson_status", required = false) String salespersonStatusParam,
			@RequestParam(name = "store_bn", required = false) String storeBnParam,
			@RequestParam(name = "salesperson_job", required = false) String salespersonJobParam,
			@RequestParam(name = "salesperson_avatar", required = false) String salespersonAvatarParam,
			@RequestParam(name = "work_qrcode_configid", required = false) String workQrcodeConfigidParam,
			@RequestParam(name = "NewUserID", required = false) String newUserIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String newUserIdRaw = OpenapiRequestParams.originalString(newUserIdParam, body, "NewUserID");
		OpenapiSalespersonPushInput input = new OpenapiSalespersonPushInput(
				OpenapiRequestParams.mergeString(salespersonNameParam, body, "salesperson_name"),
				OpenapiRequestParams.mergeString(employeeStatusParam, body, "employee_status"),
				OpenapiRequestParams.originalString(mobileParam, body, "mobile"),
				OpenapiMemberQueryParams.isParamPresent(mobileParam, body, "mobile"),
				OpenapiRequestParams.mergeString(workUseridParam, body, "work_userid"),
				OpenapiRequestParams.mergeString(salespersonStatusParam, body, "salesperson_status"),
				OpenapiRequestParams.mergeString(storeBnParam, body, "store_bn"),
				OpenapiRequestParams.mergeString(salespersonJobParam, body, "salesperson_job"),
				OpenapiRequestParams.originalString(salespersonAvatarParam, body, "salesperson_avatar"),
				OpenapiRequestParams.mergeString(workQrcodeConfigidParam, body, "work_qrcode_configid"),
				newUserIdRaw,
				OpenapiMemberQueryParams.isParamPresent(newUserIdParam, body, "NewUserID"),
				OpenapiMemberQueryParams.isPhpTruthy(newUserIdRaw));
		Map<String, Object> data = salespersonPushPort.pushSalesperson(companyId, input);
		return new OpenapiEnvelope("success", "E0000", "操作成功", data);
	}

	@Operation(
			summary = "批量更新导购状态",
			description =
					"按签名鉴权租户 company_id 批量更新导购启停状态："
							+ " employee_number 逗号分隔企微 userid，按租户 + shop_salesperson.work_userid IN 匹配；"
							+ " salesperson_status=\"1\" 置 is_valid=true，其他任何值（含缺失）置 is_valid=false。"
							+ " 全部查无匹配报「导购员信息查询错误」（E0001）；employee_number 缺失按空串匹配，通常也查无。"
							+ " 成功 data 恒为空数组 []、message=操作成功。"
							+ " 两参数双通道（body 含键时以 body 为准，null/空白视为未提供且不回退 query；body 无键读 query 并 trim）。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.salesperson.bathStatusUpdate, version=1.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：批量 UPDATE shop_salesperson.is_valid；无异步、无第三方调用、无事务注解。",
			operationId = "ecx.salesperson.bathStatusUpdate.v1")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态。"
								+ " 成功：status=success, code=E0000, message=操作成功, data 恒为空数组 []；"
								+ " 业务失败：status=fail, code=E0001 导购员信息查询错误，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxSalespersonBathStatusUpdateFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxSalespersonBathStatusUpdateSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "更新成功（data 恒为空数组）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"操作成功\",\"data\":[]}"),
									@ExampleObject(
											name = "fail-not-found",
											summary = "全部 work_userid 查无匹配",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\",\"message\":\"导购员信息查询错误\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.salesperson.bathStatusUpdate", name = "批量更新导购状态")
	public OpenapiEnvelope bathUpdateSalespersonStatus(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description =
									"导购企微 userid（shop_salesperson.work_userid），逗号分隔批量匹配；"
											+ "全部查无报「导购员信息查询错误」（缺失按空串匹配）（body 同名键优先）",
							required = true,
							example = "work-u")
					@RequestParam(name = "employee_number", required = false)
					String employeeNumberParam,
			@Parameter(
							description = "目标状态：\"1\" 启用（is_valid=true）；其他任何值（含缺失）置停用（body 同名键优先）",
							example = "1")
					@RequestParam(name = "salesperson_status", required = false)
					String salespersonStatusParam,
			@RequestBody(
							description = "可选 JSON body；字段与 query 同名双通道，body 含键时优先",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxSalespersonBathStatusUpdateBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String employeeNumber = OpenapiRequestParams.mergeString(employeeNumberParam, body, "employee_number");
		String salespersonStatus = OpenapiRequestParams.mergeString(salespersonStatusParam, body, "salesperson_status");
		bathStatusUpdatePort.bathUpdateSalespersonStatus(companyId, employeeNumber, salespersonStatus);
		return new OpenapiEnvelope("success", "E0000", "操作成功", List.of());
	}

	@Operation(
			summary = "删除导购",
			description =
					"按签名鉴权租户 company_id 删除单个导购：employee_number 单值精确匹配"
							+ " shop_salesperson.work_userid（LIMIT 1 取第一条，不支持逗号批量），"
							+ " 查无匹配报「导购员信息查询错误」（E0001）；缺失按 null 匹配，通常也查无。"
							+ " 命中后按 salesperson_id + company_id 同事务删除 shop_salesperson 与"
							+ " shop_rel_salesperson（店铺关联）两表记录。成功 data 恒为空数组 []、message=操作成功。"
							+ " employee_number 双通道（body 含键时以 body 为准，null/空白视为未提供且不回退 query；"
							+ " body 无键读 query 并 trim）。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.salesperson.destroy, version=1.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：事务内 DELETE 两表（AdminSalespersonDeleteService，rollbackFor=Exception）；"
							+ " 无异步、无第三方调用。",
			operationId = "ecx.salesperson.destroy.v1")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态。"
								+ " 成功：status=success, code=E0000, message=操作成功, data 恒为空数组 []；"
								+ " 业务失败：status=fail, code=E0001 导购员信息查询错误，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxSalespersonDestroyFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxSalespersonDestroySuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "删除成功（data 恒为空数组）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"操作成功\",\"data\":[]}"),
									@ExampleObject(
											name = "fail-not-found",
											summary = "work_userid 查无匹配",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\",\"message\":\"导购员信息查询错误\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.salesperson.destroy", name = "删除导购")
	public OpenapiEnvelope destroySalesperson(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description =
									"导购企微 userid（shop_salesperson.work_userid），单值精确匹配（不支持逗号批量）；"
											+ "查无报「导购员信息查询错误」（缺失按 null 匹配）（body 同名键优先）",
							required = true,
							example = "work-u")
					@RequestParam(name = "employee_number", required = false)
					String employeeNumberParam,
			@RequestBody(
							description = "可选 JSON body；employee_number 与 query 同名双通道，body 含键时优先",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxSalespersonDestroyBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String employeeNumber = OpenapiRequestParams.mergeString(employeeNumberParam, body, "employee_number");
		destroyPort.destroySalesperson(companyId, employeeNumber);
		return new OpenapiEnvelope("success", "E0000", "操作成功", List.of());
	}

	@PostMapping(value = "/ecx.salesperson.updateStores", name = "更新导购绑定店铺")
	public OpenapiEnvelope updateSalespersonStores(
			HttpServletRequest request,
			@RequestParam(name = "employee_number", required = false) String employeeNumberParam,
			@RequestParam(name = "store_bn", required = false) String storeBnParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String employeeNumber = OpenapiRequestParams.mergeString(employeeNumberParam, body, "employee_number");
		String storeBn = OpenapiRequestParams.mergeString(storeBnParam, body, "store_bn");
		updateStoresPort.updateSalespersonStores(companyId, employeeNumber, storeBn);
		return new OpenapiEnvelope("success", "E0000", "操作成功", List.of());
	}
}
