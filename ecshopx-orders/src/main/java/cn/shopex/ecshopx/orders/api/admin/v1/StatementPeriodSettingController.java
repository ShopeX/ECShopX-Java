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
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.orders.service.admin.StatementPeriodSettingAdminGetDefaultSettingService;
import cn.shopex.ecshopx.orders.service.admin.StatementPeriodSettingAdminGetDistributorSettingService;
import cn.shopex.ecshopx.orders.service.admin.StatementPeriodSettingAdminGetSupplierSettingService;
import cn.shopex.ecshopx.orders.service.admin.StatementPeriodSettingAdminSaveSettingService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("ordersAdminV1StatementPeriodSetting")
@RequestMapping("/api/v1/statement/period")
public class StatementPeriodSettingController {

	private final StatementPeriodSettingAdminSaveSettingService statementPeriodSettingAdminSaveSettingService;
	private final StatementPeriodSettingAdminGetDefaultSettingService statementPeriodSettingAdminGetDefaultSettingService;
	private final StatementPeriodSettingAdminGetDistributorSettingService
			statementPeriodSettingAdminGetDistributorSettingService;
	private final StatementPeriodSettingAdminGetSupplierSettingService
			statementPeriodSettingAdminGetSupplierSettingService;

	public StatementPeriodSettingController(
			StatementPeriodSettingAdminSaveSettingService statementPeriodSettingAdminSaveSettingService,
			StatementPeriodSettingAdminGetDefaultSettingService statementPeriodSettingAdminGetDefaultSettingService,
			StatementPeriodSettingAdminGetDistributorSettingService
					statementPeriodSettingAdminGetDistributorSettingService,
			StatementPeriodSettingAdminGetSupplierSettingService
					statementPeriodSettingAdminGetSupplierSettingService) {
		this.statementPeriodSettingAdminSaveSettingService = statementPeriodSettingAdminSaveSettingService;
		this.statementPeriodSettingAdminGetDefaultSettingService = statementPeriodSettingAdminGetDefaultSettingService;
		this.statementPeriodSettingAdminGetDistributorSettingService =
				statementPeriodSettingAdminGetDistributorSettingService;
		this.statementPeriodSettingAdminGetSupplierSettingService =
				statementPeriodSettingAdminGetSupplierSettingService;
	}

	@Activated(routeAlias = "statement.period.default.setting.get")
	@GetMapping(value = "/default/setting", name = "默认结算周期")
	public ApiResult<Object> getDefaultSetting(HttpServletRequest request) {
		String raw = request.getParameter("merchant_type");
		String merchantType = (raw == null) ? "distributor" : raw;
		long companyId = readCompanyIdFromJwt(request);
		Object data = statementPeriodSettingAdminGetDefaultSettingService.getDefaultSetting(companyId, merchantType);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "statement.period.distributor.setting.get")
	@GetMapping(value = "/distributor/setting", name = "店铺结算周期")
	public ApiResult<Map<String, Object>> getDistributorSetting(HttpServletRequest request) {
		long companyId = readCompanyIdFromJwt(request);
		Map<String, Object> data =
				statementPeriodSettingAdminGetDistributorSettingService.getDistributorSetting(
						companyId,
						request.getParameter("page"),
						request.getParameter("pageSize"),
						request.getParameter("distributor_id"),
						request.getParameter("merchant_id"));
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "statement.period.supplier.setting.get")
	@GetMapping(value = "/supplier/setting", name = "供应商结算周期")
	public ApiResult<Map<String, Object>> getSupplierSetting(HttpServletRequest request) {
		long companyId = readCompanyIdFromJwt(request);
		Map<String, Object> data =
				statementPeriodSettingAdminGetSupplierSettingService.getSupplierSetting(
						companyId,
						request.getParameter("page"),
						request.getParameter("pageSize"),
						request.getParameter("supplier_name"),
						request.getParameter("supplier_id"));
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "statement.period.setting.set")
	@PostMapping(value = "/setting", name = "保存结算周期", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> saveSetting(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		return ApiResult.ok(
				statementPeriodSettingAdminSaveSettingService.saveSetting(readCompanyIdFromJwt(request), merged));
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
