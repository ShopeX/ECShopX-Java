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

package cn.shopex.ecshopx.orders.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.espier.api.admin.v1.EspierAdminJwtControllerSupport;
import cn.shopex.ecshopx.orders.service.admin.StatementsAdminComfirmStatementService;
import cn.shopex.ecshopx.orders.service.admin.StatementsAdminExportDetailService;
import cn.shopex.ecshopx.orders.service.admin.StatementsAdminExportSummarizedService;
import cn.shopex.ecshopx.orders.service.admin.StatementsAdminGetDetailService;
import cn.shopex.ecshopx.orders.service.admin.StatementsAdminGetSummarizedService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("ordersAdminV1Statements")
@RequestMapping("/api/v1/statement")
public class StatementsController {

	private final StatementsAdminComfirmStatementService statementsAdminComfirmStatementService;
	private final StatementsAdminExportDetailService statementsAdminExportDetailService;
	private final StatementsAdminExportSummarizedService statementsAdminExportSummarizedService;
	private final StatementsAdminGetDetailService statementsAdminGetDetailService;
	private final StatementsAdminGetSummarizedService statementsAdminGetSummarizedService;

	public StatementsController(
			StatementsAdminComfirmStatementService statementsAdminComfirmStatementService,
			StatementsAdminExportDetailService statementsAdminExportDetailService,
			StatementsAdminExportSummarizedService statementsAdminExportSummarizedService,
			StatementsAdminGetDetailService statementsAdminGetDetailService,
			StatementsAdminGetSummarizedService statementsAdminGetSummarizedService) {
		this.statementsAdminComfirmStatementService = statementsAdminComfirmStatementService;
		this.statementsAdminExportDetailService = statementsAdminExportDetailService;
		this.statementsAdminExportSummarizedService = statementsAdminExportSummarizedService;
		this.statementsAdminGetDetailService = statementsAdminGetDetailService;
		this.statementsAdminGetSummarizedService = statementsAdminGetSummarizedService;
	}

	@Activated(routeAlias = "statement.summarized.get")
	@GetMapping(value = "/summarized", name = "结算汇总", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getSummarized(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "pageSize", required = false) String pageSizeRaw,
			@RequestParam(value = "distributor_id", required = false) String distributorIdRaw,
			@RequestParam(value = "merchant_id", required = false) String merchantIdRaw,
			@RequestParam(value = "supplier_id", required = false) String supplierIdRaw,
			@RequestParam(value = "merchant_type", required = false) String merchantTypeRaw,
			@RequestParam(value = "statement_status", required = false) String statementStatusRaw,
			@RequestParam(value = "start_time", required = false) String startTimeRaw,
			@RequestParam(value = "end_time", required = false) String endTimeRaw) {
		long companyId = readCompanyIdFromJwt(request);
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> jwt = (Map<?, ?>) attr;
		String operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(jwt.get("operator_type"));
		Long distributorIdOrNull = EspierAdminJwtControllerSupport.parseLongOrNull(jwt.get("distributor_id"));
		Long merchantIdOrNull = EspierAdminJwtControllerSupport.parseLongOrNull(jwt.get("merchant_id"));
		Long operatorIdObj = EspierAdminJwtControllerSupport.parseLongOrNull(jwt.get("operator_id"));
		long operatorId = operatorIdObj == null ? 0L : operatorIdObj;

		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		if (distributorIdRaw != null) {
			params.put("distributor_id", distributorIdRaw);
		}
		if (merchantIdRaw != null) {
			params.put("merchant_id", merchantIdRaw);
		}
		if (supplierIdRaw != null) {
			params.put("supplier_id", supplierIdRaw);
		}
		if (merchantTypeRaw != null) {
			params.put("merchant_type", merchantTypeRaw);
		}
		if (statementStatusRaw != null) {
			params.put("statement_status", statementStatusRaw);
		}
		if (startTimeRaw != null) {
			params.put("start_time", startTimeRaw);
		}
		if (endTimeRaw != null) {
			params.put("end_time", endTimeRaw);
		}

		return ApiResult.ok(
				statementsAdminGetSummarizedService.getSummarized(
						companyId,
						operatorId,
						operatorType,
						distributorIdOrNull,
						merchantIdOrNull,
						params,
						pageRaw,
						pageSizeRaw));
	}

	@Activated(routeAlias = "statement.summarized.export")
	@PostMapping(value = "/summarized/export", name = "导出结算汇总", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> exportSummarized(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = readCompanyIdFromJwt(request);
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> jwt = (Map<?, ?>) attr;
		String operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(jwt.get("operator_type"));
		Long distributorIdOrNull = EspierAdminJwtControllerSupport.parseLongOrNull(jwt.get("distributor_id"));
		Long merchantIdOrNull = EspierAdminJwtControllerSupport.parseLongOrNull(jwt.get("merchant_id"));
		Long operatorIdObj = EspierAdminJwtControllerSupport.parseLongOrNull(jwt.get("operator_id"));
		long operatorId = operatorIdObj == null ? 0L : operatorIdObj;

		statementsAdminExportSummarizedService.exportSummarized(
				companyId, operatorId, operatorType, distributorIdOrNull, merchantIdOrNull, merged);
		return ApiResult.ok(Map.of("status", Boolean.TRUE));
	}

	@Activated(routeAlias = "statement.confirm.post")
	@PostMapping(value = "/confirm/{statement_id}", name = "确认结算", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> comfirmStatement(
			HttpServletRequest request, @PathVariable("statement_id") String statementId) {
		long statementIdLong = parseStatementIdPath(statementId);
		long companyId = readCompanyIdFromJwt(request);
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> jwt = (Map<?, ?>) attr;
		String operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(jwt.get("operator_type"));
		Long distributorIdOrNull = EspierAdminJwtControllerSupport.parseLongOrNull(jwt.get("distributor_id"));
		Long merchantIdOrNull = EspierAdminJwtControllerSupport.parseLongOrNull(jwt.get("merchant_id"));
		Long operatorIdObj = EspierAdminJwtControllerSupport.parseLongOrNull(jwt.get("operator_id"));
		long operatorId = operatorIdObj == null ? 0L : operatorIdObj;

		Map<String, Object> data =
				statementsAdminComfirmStatementService.comfirmStatement(
						companyId,
						statementIdLong,
						operatorType,
						distributorIdOrNull,
						merchantIdOrNull,
						operatorId);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "statement.detail.get")
	@GetMapping(value = "/detail/{statement_id}", name = "结算明细", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getDetail(
			HttpServletRequest request,
			@PathVariable("statement_id") String statementId,
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "pageSize", required = false) String pageSizeRaw,
			@RequestParam(value = "start_time", required = false) String startTimeRaw,
			@RequestParam(value = "end_time", required = false) String endTimeRaw,
			@RequestParam(value = "order_id", required = false) String orderIdRaw) {
		long companyId = readCompanyIdFromJwt(request);
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> jwt = (Map<?, ?>) attr;
		String operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(jwt.get("operator_type"));
		Long distributorIdOrNull = EspierAdminJwtControllerSupport.parseLongOrNull(jwt.get("distributor_id"));
		Long merchantIdOrNull = EspierAdminJwtControllerSupport.parseLongOrNull(jwt.get("merchant_id"));
		Long operatorIdObj = EspierAdminJwtControllerSupport.parseLongOrNull(jwt.get("operator_id"));
		long operatorId = operatorIdObj == null ? 0L : operatorIdObj;

		Map<String, Object> data =
				statementsAdminGetDetailService.getDetail(
						companyId,
						statementId,
						operatorType,
						distributorIdOrNull,
						merchantIdOrNull,
						operatorId,
						startTimeRaw,
						endTimeRaw,
						orderIdRaw,
						pageRaw,
						pageSizeRaw);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "statement.detail.export")
	@PostMapping(value = "/detail/export", name = "导出结算明细", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> exportDetail(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = readCompanyIdFromJwt(request);
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> jwt = (Map<?, ?>) attr;
		String operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(jwt.get("operator_type"));
		Long distributorIdOrNull = EspierAdminJwtControllerSupport.parseLongOrNull(jwt.get("distributor_id"));
		Long merchantIdOrNull = EspierAdminJwtControllerSupport.parseLongOrNull(jwt.get("merchant_id"));
		Long operatorIdObj = EspierAdminJwtControllerSupport.parseLongOrNull(jwt.get("operator_id"));
		long operatorId = operatorIdObj == null ? 0L : operatorIdObj;

		statementsAdminExportDetailService.exportDetail(
				companyId, operatorId, operatorType, distributorIdOrNull, merchantIdOrNull, merged);
		return ApiResult.ok(Map.of("status", Boolean.TRUE));
	}

	private static long parseStatementIdPath(String statementId) {
		String tid = statementId == null ? "" : statementId.trim();
		if (tid.isEmpty()) {
			throw new BadRequestException("参数格式错误");
		}
		try {
			return Long.parseLong(tid);
		} catch (NumberFormatException e) {
			throw new ResourceException("结算单不存在");
		}
	}

	private static long readCompanyIdFromJwt(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> jwt = (Map<?, ?>) attr;
		Object companyIdObj = jwt.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		try {
			return Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
	}
}
