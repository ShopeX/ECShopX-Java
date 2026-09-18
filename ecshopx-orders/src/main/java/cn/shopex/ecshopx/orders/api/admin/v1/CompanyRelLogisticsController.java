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
import cn.shopex.ecshopx.common.web.ActivatedRequestAttributes;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.espier.api.admin.v1.EspierAdminJwtControllerSupport;
import cn.shopex.ecshopx.orders.service.companyrellogistics.CompanyRelLogisticsAdminCreateService;
import cn.shopex.ecshopx.orders.service.companyrellogistics.CompanyRelLogisticsAdminDeleteService;
import cn.shopex.ecshopx.orders.service.companyrellogistics.CompanyRelLogisticsAdminListService;
import cn.shopex.ecshopx.orders.service.companyrellogistics.CompanyRelLogisticsTradeListService;
import cn.shopex.ecshopx.orders.service.kdniao.KdniaoQinglongSettingAdminService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RestController("ordersAdminV1CompanyRelLogistics")
@RequestMapping("/api/v1")
public class CompanyRelLogisticsController {

	private final CompanyRelLogisticsAdminCreateService companyRelLogisticsAdminCreateService;
	private final CompanyRelLogisticsAdminDeleteService companyRelLogisticsAdminDeleteService;
	private final CompanyRelLogisticsAdminListService companyRelLogisticsAdminListService;
	private final CompanyRelLogisticsTradeListService companyRelLogisticsTradeListService;
	private final KdniaoQinglongSettingAdminService kdniaoQinglongSettingAdminService;

	public CompanyRelLogisticsController(
			CompanyRelLogisticsAdminCreateService companyRelLogisticsAdminCreateService,
			CompanyRelLogisticsAdminDeleteService companyRelLogisticsAdminDeleteService,
			CompanyRelLogisticsAdminListService companyRelLogisticsAdminListService,
			CompanyRelLogisticsTradeListService companyRelLogisticsTradeListService,
			KdniaoQinglongSettingAdminService kdniaoQinglongSettingAdminService) {
		this.companyRelLogisticsAdminCreateService = companyRelLogisticsAdminCreateService;
		this.companyRelLogisticsAdminDeleteService = companyRelLogisticsAdminDeleteService;
		this.companyRelLogisticsAdminListService = companyRelLogisticsAdminListService;
		this.companyRelLogisticsTradeListService = companyRelLogisticsTradeListService;
		this.kdniaoQinglongSettingAdminService = kdniaoQinglongSettingAdminService;
	}

	@Activated(routeAlias = "company.logistics.create")
	@PostMapping(value = "/company/logistics/create", name = "创建公司物流", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> createCompanyLogistics(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = readCompanyIdFromJwt(request);
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		String operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(jwt.get("operator_type"));
		Long operatorIdOrNull = EspierAdminJwtControllerSupport.parseLongOrNull(jwt.get("operator_id"));
		Map<String, Object> data =
				companyRelLogisticsAdminCreateService.createCompanyLogistics(companyId, operatorType, operatorIdOrNull, merged);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "company.logistics.list")
	@GetMapping(value = "/company/logistics/list", name = "公司启用物流", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getCompanyLogisticsList(
			HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") String distributorIdRaw,
			@RequestParam(value = "corp_name", required = false) String corpName,
			@RequestParam(value = "status", required = false) String status) {
		long companyId = readCompanyIdFromJwt(request);
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		String operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(jwt.get("operator_type"));
		Long operatorIdOrNull = EspierAdminJwtControllerSupport.parseLongOrNull(jwt.get("operator_id"));
		int distributorId = resolveDistributorId(request, distributorIdRaw, operatorType, jwt);
		return ApiResult.ok(
				companyRelLogisticsAdminListService.getCompanyLogisticsList(
						companyId, operatorType, operatorIdOrNull, distributorId, corpName, status));
	}

	@Activated(routeAlias = "company.logistics.delete")
	@DeleteMapping(value = "/company/logistics/{id}", name = "删除公司物流")
	public ResponseEntity<Void> deleteCompanyLogistics(
			HttpServletRequest request,
			@PathVariable("id") String id,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") String distributorIdRaw) {
		long companyId = readCompanyIdFromJwt(request);
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		String operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(jwt.get("operator_type"));
		Long operatorIdOrNull = EspierAdminJwtControllerSupport.parseLongOrNull(jwt.get("operator_id"));
		int distributorId = resolveDistributorId(request, distributorIdRaw, operatorType, jwt);
		companyRelLogisticsAdminDeleteService.deleteCompanyLogistics(
				companyId, operatorType, operatorIdOrNull, id, distributorId);
		return ResponseEntity.ok().build();
	}

	@Activated(routeAlias = "trade.logistics.list")
	@GetMapping(value = "/trade/logistics/list", name = "可用物流列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getLogisticsList(
			HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") String distributorIdRaw) {
		long companyId = readCompanyIdFromJwt(request);
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		String operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(jwt.get("operator_type"));
		Long operatorIdOrNull = EspierAdminJwtControllerSupport.parseLongOrNull(jwt.get("operator_id"));
		int distributorId = resolveDistributorId(request, distributorIdRaw, operatorType, jwt);
		return ApiResult.ok(
				companyRelLogisticsTradeListService.getLogisticsList(
						companyId, operatorType, operatorIdOrNull, distributorId));
	}

	@Activated(routeAlias = "company.logistics.qinglongcode")
	@PostMapping(value = "/company/logistics/qinglongcode", name = "设置青龙编码", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> setQinglongcode(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = readCompanyIdFromJwt(request);
		kdniaoQinglongSettingAdminService.setQinglongcode(companyId, merged);
		return ApiResult.ok(Map.of("status", true));
	}

	@Activated(routeAlias = "company.logistics.qinglongcode.info")
	@GetMapping(value = "/company/logistics/qinglongcode", name = "青龙编码", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getQinglongcode(HttpServletRequest request) {
		long companyId = readCompanyIdFromJwt(request);
		String code = kdniaoQinglongSettingAdminService.getQinglongcode(companyId);
		return ApiResult.ok(Map.of("qinglong_code", code));
	}

	private static int resolveDistributorId(
			HttpServletRequest request,
			String distributorIdRaw,
			String operatorType,
			Map<String, Object> jwt) {
		if ("distributor".equals(operatorType)) {
			Long selected = EspierAdminJwtControllerSupport.parseLongOrNull(jwt.get("distributor_id"));
			if (selected == null || selected <= 0L) {
				selected =
						EspierAdminJwtControllerSupport.parseLongOrNull(
								request.getAttribute(ActivatedRequestAttributes.DISTRIBUTOR_ID));
			}
			if (selected != null && selected > 0L) {
				return selected.intValue();
			}
		}
		return parseDistributorIdRaw(distributorIdRaw);
	}

	private static int parseDistributorIdRaw(String distributorIdRaw) {
		if (distributorIdRaw == null || distributorIdRaw.trim().isEmpty()) {
			return 0;
		}
		try {
			return Integer.parseInt(distributorIdRaw.trim());
		} catch (NumberFormatException e) {
			return 0;
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
