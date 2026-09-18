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

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.dto.EmployeeAccountManagementListQuery;
import cn.shopex.ecshopx.companys.dto.EmployeeCreateRequest;
import cn.shopex.ecshopx.companys.dto.EmployeeUpdateRequest;
import cn.shopex.ecshopx.companys.service.employee.EmployeeCreateOrchestrationService;
import cn.shopex.ecshopx.companys.service.employee.EmployeeManagementDeleteService;
import cn.shopex.ecshopx.companys.service.employee.EmployeeManagementDetailService;
import cn.shopex.ecshopx.companys.service.employee.EmployeeManagementListService;
import cn.shopex.ecshopx.companys.service.employee.EmployeeUpdateOrchestrationService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

@AdminAuth
@ShopLog
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@RestController("companysAdminV1Employee")
@RequestMapping("/api/v1/account/management")
public class EmployeeController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final EmployeeCreateOrchestrationService employeeCreateOrchestrationService;
	private final EmployeeUpdateOrchestrationService employeeUpdateOrchestrationService;
	private final EmployeeManagementListService employeeManagementListService;
	private final EmployeeManagementDetailService employeeManagementDetailService;
	private final EmployeeManagementDeleteService employeeManagementDeleteService;

	public EmployeeController(
			EmployeeCreateOrchestrationService employeeCreateOrchestrationService,
			EmployeeUpdateOrchestrationService employeeUpdateOrchestrationService,
			EmployeeManagementListService employeeManagementListService,
			EmployeeManagementDetailService employeeManagementDetailService,
			EmployeeManagementDeleteService employeeManagementDeleteService) {
		this.employeeCreateOrchestrationService = employeeCreateOrchestrationService;
		this.employeeUpdateOrchestrationService = employeeUpdateOrchestrationService;
		this.employeeManagementListService = employeeManagementListService;
		this.employeeManagementDetailService = employeeManagementDetailService;
		this.employeeManagementDeleteService = employeeManagementDeleteService;
	}

	@Activated(routeAlias = "account.create")
	@PostMapping(name = "创建企业员工")
	public ResponseEntity<ApiResult<Map<String, Object>>> createData(
			HttpServletRequest request, @FlexibleBody EmployeeCreateRequest body) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException("Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> data = employeeCreateOrchestrationService.execute(body, jwt);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DataPass(pathAlias = "account.list")
	@Activated(routeAlias = "account.list")
	@GetMapping(name = "获取企业员工信息列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getListData(
			HttpServletRequest request,
			@RequestParam(value = "operator_id", required = false) List<String> operatorId,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "username", required = false) String username,
			@RequestParam(value = "role_id", required = false) String roleId,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "login_name", required = false) String loginName,
			@RequestParam(value = "payment_method", required = false) String paymentMethod,
			@RequestParam(value = "staff_type", required = false) String staffType,
			@RequestParam(value = "distributor_id", required = false) String distributorId,
			@RequestParam(value = "is_disable", required = false) String isDisable,
			@RequestParam(value = "supplier_name", required = false) String supplierName,
			@RequestParam(value = "operator_type", required = false) String operatorType) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException("Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		EmployeeAccountManagementListQuery query = new EmployeeAccountManagementListQuery();
		query.setOperatorId(operatorId);
		query.setMobile(mobile);
		query.setUsername(username);
		query.setRoleId(roleId);
		query.setPage(page);
		query.setPageSize(pageSize);
		query.setLoginName(loginName);
		query.setPaymentMethod(paymentMethod);
		query.setStaffType(staffType);
		query.setDistributorId(distributorId);
		query.setIsDisable(isDisable);
		query.setSupplierName(supplierName);
		query.setOperatorType(operatorType);
		int datapassBlock = parseDatapassBlock(request);
		String cc = request.getParameter("country_code");
		String requestLangTag = StringUtils.hasText(cc) ? cc.trim() : "zh-CN";
		Map<String, Object> data =
				employeeManagementListService.getListData(jwt, query, datapassBlock, requestLangTag);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private int parseDatapassBlock(HttpServletRequest request) {
		Object attr = request.getAttribute("x-datapass-block");
		if (attr instanceof Number n && n.intValue() != 0) {
			return 1;
		}
		if (Boolean.TRUE.equals(attr)) {
			return 1;
		}
		if (attr != null) {
			String t = attr.toString().trim();
			if (!t.isEmpty() && !"0".equals(t) && !"false".equalsIgnoreCase(t)) {
				return 1;
			}
		}
		String p = request.getParameter("x-datapass-block");
		if (p == null || p.trim().isEmpty() || "0".equals(p.trim()) || "false".equalsIgnoreCase(p.trim())) {
			return 0;
		}
		return 1;
	}

	@Activated(routeAlias = "account.info")
	@GetMapping(value = "/{operator_id}", name = "获取企业员工信息")
	public ResponseEntity<ApiResult<Object>> getInfoData(
			HttpServletRequest request, @PathVariable("operator_id") String operatorIdStr) {
		if (!StringUtils.hasText(operatorIdStr)) {
			throw new BadRequestException("id必填");
		}
		long targetOperatorId;
		try {
			targetOperatorId = Long.parseLong(operatorIdStr.trim());
		} catch (NumberFormatException ex) {
			throw new BadRequestException("无效的 operator_id");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException("Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		String cc = request.getParameter("country_code");
		String requestLangTag = StringUtils.hasText(cc) ? cc.trim() : "zh-CN";
		Map<String, Object> row =
				employeeManagementDetailService.getInfoData(jwt, targetOperatorId, requestLangTag);
		return ResponseEntity.ok(
				row == null ? ApiResult.ok(Collections.emptyList()) : ApiResult.ok(row));
	}

	@Activated(routeAlias = "account.update")
	@PatchMapping(value = "/{operator_id}", name = "更改企业员工信息")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateData(
			HttpServletRequest request,
			@PathVariable("operator_id") String operatorIdStr,
			@FlexibleBody EmployeeUpdateRequest body) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException("Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		long operatorId;
		try {
			operatorId = Long.parseLong(operatorIdStr.trim());
		} catch (NumberFormatException ex) {
			throw new BadRequestException("无效的 operator_id");
		}
		if (body == null) {
			body = new EmployeeUpdateRequest();
		}
		Map<String, Object> data = employeeUpdateOrchestrationService.execute(body, jwt, operatorId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "account.delete")
	@DeleteMapping(value = "/{operator_id}", name = "删除企业员工信息")
	public ResponseEntity<ApiResult<Object>> deleteData(
			HttpServletRequest request, @PathVariable("operator_id") String operatorId) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException("Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		employeeManagementDeleteService.deleteData(operatorId, jwt);
		return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
	}
}
