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

package cn.shopex.ecshopx.employeepurchase.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.employeepurchase.service.EmployeeCreateService;
import cn.shopex.ecshopx.employeepurchase.service.EmployeeDetailService;
import cn.shopex.ecshopx.employeepurchase.service.EmployeeListService;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseEmployeeExportFacadeService;
import cn.shopex.ecshopx.employeepurchase.service.dto.EmployeeAdminExportQuery;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.goods.web.DatapassBlockResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("employeepurchaseEmployeeAdminV1")
@RequestMapping("/api/v1")
public class EmployeeController {

	private final EmployeeCreateService employeeCreateService;
	private final EmployeeListService employeeListService;
	private final EmployeePurchaseEmployeeExportFacadeService exportFacadeService;
	private final EmployeeDetailService employeeDetailService;

	public EmployeeController(
			EmployeeCreateService employeeCreateService,
			EmployeeListService employeeListService,
			EmployeePurchaseEmployeeExportFacadeService exportFacadeService,
			EmployeeDetailService employeeDetailService) {
		this.employeeCreateService = employeeCreateService;
		this.employeeListService = employeeListService;
		this.exportFacadeService = exportFacadeService;
		this.employeeDetailService = employeeDetailService;
	}

	@Activated(routeAlias = "employeepurchase.employee.list")
	@GetMapping(value = "/employees", name = "获取企业员工列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getList(
			HttpServletRequest request,
			@RequestParam("page") int page,
			@RequestParam("pageSize") int pageSize,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "account", required = false) String account,
			@RequestParam(value = "email", required = false) String email,
			@RequestParam(value = "member_mobile", required = false) String member_mobile,
			@RequestParam(value = "enterprise_id", required = false) String enterprise_id,
			@RequestParam(value = "distributor_id", required = false) String distributor_id) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Map<String, Object> data =
				employeeListService.getList(
						page,
						pageSize,
						mobile,
						account,
						email,
						member_mobile,
						enterprise_id,
						distributor_id,
						operatorJwt,
						request);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "employeepurchase.employee.info")
	@GetMapping(value = "/employee/{employeeId}", name = "获取企业员工信息")
	public ResponseEntity<ApiResult<Object>> getInfo(
			HttpServletRequest request, @PathVariable("employeeId") String employeeId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object data = employeeDetailService.getInfoData(employeeId, operatorJwt);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "employeepurchase.employee.create")
	@PostMapping(value = "/employee", name = "添加企业员工")
	public ResponseEntity<ApiResult<Map<String, Object>>> create(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> data = employeeCreateService.create(merged, operatorJwt);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "employeepurchase.employee.update")
	@PutMapping(value = "/employee/{employeeId}", name = "更新企业员工信息")
	public ResponseEntity<ApiResult<Map<String, Object>>> update(
			@PathVariable("employeeId") String employeeId,
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> data = employeeCreateService.updateOneBy(employeeId, merged, operatorJwt);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "employeepurchase.employee.delete")
	@DeleteMapping(value = "/employee/{employeeId}", name = "删除企业员工信息")
	public ResponseEntity<ApiResult<Map<String, Object>>> delete(
			@PathVariable("employeeId") String employeeId, HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Map<String, Object> data = employeeCreateService.deleteByCompanyAndEmployeeId(employeeId, operatorJwt);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "employeepurchase.employees.export")
	@GetMapping(value = "/employees/export", name = "导出企业员工信息")
	public ResponseEntity<ApiResult<Map<String, Object>>> exportData(
			HttpServletRequest request,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "account", required = false) String account,
			@RequestParam(value = "email", required = false) String email,
			@RequestParam(value = "member_mobile", required = false) String member_mobile,
			@RequestParam(value = "enterprise_id", required = false) String enterprise_id,
			@RequestParam(value = "distributor_id", required = false) String distributor_id) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		long operatorId = readOptionalOperatorIdForExport(ud);
		EmployeeAdminExportQuery query =
				employeeListService.resolveAdminExportQuery(mobile, account, email, member_mobile, enterprise_id,
						distributor_id, operatorJwt);
		long count = employeeListService.countForExport(query);
		if (count <= 0) {
			throw new ResourceException("导出有误,暂无数据导出");
		}
		if (count > 15000) {
			throw new ResourceException("导出有误，最高导出15000条数据");
		}
		boolean datapassBlock = DatapassBlockResolver.isBlocked(request);
		exportFacadeService.submitExport(query, operatorId, datapassBlock);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	private static long readOptionalOperatorIdForExport(Map<?, ?> ud) {
		Object v = ud.get("operator_id");
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Map<String, Object> mergeInputLikeFlexibleResolver(
			HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			LinkedHashMap<String, Object> input = new LinkedHashMap<>(parameterMapToMapLikeResolver(request));
			if (body != null) {
				input.putAll(body);
			}
			return input;
		}
		return body != null ? body : new LinkedHashMap<>();
	}

	private static Map<String, Object> parameterMapToMapLikeResolver(HttpServletRequest request) {
		Map<String, Object> m = new LinkedHashMap<>();
		request.getParameterMap().forEach((k, v) -> {
			if (v != null && v.length > 0 && StringUtils.hasText(v[0])) {
				m.put(k, v[0]);
			}
		});
		return m;
	}
}
