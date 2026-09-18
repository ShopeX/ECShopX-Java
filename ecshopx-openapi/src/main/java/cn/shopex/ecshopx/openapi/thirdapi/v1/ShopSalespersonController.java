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
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OpenAPI v1 handler scaffold — methods added during migration. */
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

	@PostMapping(value = "/ecx.salesperson.bathStatusUpdate", name = "批量更新导购状态")
	public OpenapiEnvelope bathUpdateSalespersonStatus(
			HttpServletRequest request,
			@RequestParam(name = "employee_number", required = false) String employeeNumberParam,
			@RequestParam(name = "salesperson_status", required = false) String salespersonStatusParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String employeeNumber = OpenapiRequestParams.mergeString(employeeNumberParam, body, "employee_number");
		String salespersonStatus = OpenapiRequestParams.mergeString(salespersonStatusParam, body, "salesperson_status");
		bathStatusUpdatePort.bathUpdateSalespersonStatus(companyId, employeeNumber, salespersonStatus);
		return new OpenapiEnvelope("success", "E0000", "操作成功", List.of());
	}

	@PostMapping(value = "/ecx.salesperson.destroy", name = "删除导购")
	public OpenapiEnvelope destroySalesperson(
			HttpServletRequest request,
			@RequestParam(name = "employee_number", required = false) String employeeNumberParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
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
