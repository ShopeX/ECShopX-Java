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
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.espier.api.admin.v1.EspierAdminJwtControllerSupport;
import cn.shopex.ecshopx.orders.service.shippingtemplate.ShippingTemplateAdminCreateService;
import cn.shopex.ecshopx.orders.service.shippingtemplate.ShippingTemplateAdminDeleteService;
import cn.shopex.ecshopx.orders.service.shippingtemplate.ShippingTemplateAdminInfoService;
import cn.shopex.ecshopx.orders.service.shippingtemplate.ShippingTemplateAdminListService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RestController("ordersAdminV1ShippingTemplate")
@RequestMapping("/api/v1/shipping/templates")
public class ShippingTemplateController {

	private final ShippingTemplateAdminCreateService shippingTemplateAdminCreateService;
	private final ShippingTemplateAdminDeleteService shippingTemplateAdminDeleteService;
	private final ShippingTemplateAdminInfoService shippingTemplateAdminInfoService;
	private final ShippingTemplateAdminListService shippingTemplateAdminListService;
	private final LangueProperties langueProperties;

	public ShippingTemplateController(
			ShippingTemplateAdminCreateService shippingTemplateAdminCreateService,
			ShippingTemplateAdminDeleteService shippingTemplateAdminDeleteService,
			ShippingTemplateAdminInfoService shippingTemplateAdminInfoService,
			ShippingTemplateAdminListService shippingTemplateAdminListService,
			LangueProperties langueProperties) {
		this.shippingTemplateAdminCreateService = shippingTemplateAdminCreateService;
		this.shippingTemplateAdminDeleteService = shippingTemplateAdminDeleteService;
		this.shippingTemplateAdminInfoService = shippingTemplateAdminInfoService;
		this.shippingTemplateAdminListService = shippingTemplateAdminListService;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "shipping.templates.list")
	@GetMapping(value = "/list", name = "运费模板列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getShippingTemplatesList(
			HttpServletRequest request,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "is_free", required = false) String isFree,
			@RequestParam(value = "valuation", required = false) String valuation,
			@RequestParam(value = "page", required = false) String page,
			@RequestParam(value = "pageSize", required = false) String pageSize) {
		long companyId = readCompanyIdFromJwt(request);
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		String acceptLanguage = RequestLangTag.current(langueProperties);
		Map<String, Object> payload =
				shippingTemplateAdminListService.getShippingTemplatesList(
						companyId, jwt, status, isFree, valuation, page, pageSize, acceptLanguage);
		return ApiResult.ok(payload);
	}

	@Activated(routeAlias = "shipping.templates.info")
	@GetMapping(value = "/info/{id}", name = "运费模板详情", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Object> getShippingTemplatesInfo(
			@PathVariable("id") String id, HttpServletRequest request) {
		long companyId = readCompanyIdFromJwt(request);
		String acceptLanguage = RequestLangTag.current(langueProperties);
		Object payload =
				shippingTemplateAdminInfoService.getShippingTemplatesInfo(id, companyId, acceptLanguage);
		return ApiResult.ok(payload);
	}

	@Activated(routeAlias = "shipping.templates.create")
	@PostMapping(value = "/create", name = "添加运费模板", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> createShippingTemplates(
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
		Long distributorIdOrNull = EspierAdminJwtControllerSupport.parseLongOrNull(jwt.get("distributor_id"));
		long distributorId = distributorIdOrNull != null ? distributorIdOrNull : 0L;
		String operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(jwt.get("operator_type"));
		long supplierId;
		if ("supplier".equals(operatorType)) {
			Long sid = EspierAdminJwtControllerSupport.parseLongOrNull(jwt.get("operator_id"));
			supplierId = sid != null ? sid : 0L;
		} else {
			supplierId = 0L;
		}
		Map<String, Object> data =
				shippingTemplateAdminCreateService.createShippingTemplates(companyId, distributorId, supplierId, merged);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "shipping.templates.update")
	@PutMapping(value = "/update/{id}", name = "更新运费模板", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> updateShippingTemplates(
			@PathVariable("id") String id,
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = readCompanyIdFromJwt(request);
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		Long distributorIdOrNull = EspierAdminJwtControllerSupport.parseLongOrNull(jwt.get("distributor_id"));
		long distributorId = distributorIdOrNull != null ? distributorIdOrNull : 0L;
		String operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(jwt.get("operator_type"));
		long supplierId;
		if ("supplier".equals(operatorType)) {
			Long sid = EspierAdminJwtControllerSupport.parseLongOrNull(jwt.get("operator_id"));
			supplierId = sid != null ? sid : 0L;
		} else {
			supplierId = 0L;
		}
		Map<String, Object> data =
				shippingTemplateAdminCreateService.updateShippingTemplates(
						id, companyId, distributorId, supplierId, merged);
		return ApiResult.ok(data);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "shipping.templates.delete")
	@DeleteMapping(value = "/delete/{id}", name = "删除运费模板", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> deleteShippingTemplates(@PathVariable("id") String id, HttpServletRequest request) {
		long companyId = readCompanyIdFromJwt(request);
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		Long distributorIdOrNull = EspierAdminJwtControllerSupport.parseLongOrNull(jwt.get("distributor_id"));
		long distributorId = distributorIdOrNull != null ? distributorIdOrNull : 0L;
		String operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(jwt.get("operator_type"));
		long supplierId;
		if ("supplier".equals(operatorType)) {
			Long sid = EspierAdminJwtControllerSupport.parseLongOrNull(jwt.get("operator_id"));
			supplierId = sid != null ? sid : 0L;
		} else {
			supplierId = 0L;
		}
		shippingTemplateAdminDeleteService.deleteShippingTemplates(id, companyId, distributorId, supplierId);
		return ResponseEntity.ok().build();
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
