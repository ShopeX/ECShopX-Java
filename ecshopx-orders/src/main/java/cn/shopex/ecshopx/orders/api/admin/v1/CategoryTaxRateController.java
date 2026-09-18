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

import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.orders.service.categorytaxrate.CategoryTaxRateAdminCreateService;
import cn.shopex.ecshopx.orders.service.categorytaxrate.CategoryTaxRateAdminDeleteService;
import cn.shopex.ecshopx.orders.service.categorytaxrate.CategoryTaxRateAdminDetailService;
import cn.shopex.ecshopx.orders.service.categorytaxrate.CategoryTaxRateAdminListService;
import cn.shopex.ecshopx.orders.service.categorytaxrate.CategoryTaxRateAdminUpdateService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
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
@RestController("ordersAdminV1CategoryTaxRate")
@RequestMapping("/api/v1/order/category-taxrate")
public class CategoryTaxRateController {

	private final CategoryTaxRateAdminCreateService categoryTaxRateAdminCreateService;
	private final CategoryTaxRateAdminUpdateService categoryTaxRateAdminUpdateService;
	private final CategoryTaxRateAdminDetailService categoryTaxRateAdminDetailService;
	private final CategoryTaxRateAdminListService categoryTaxRateAdminListService;
	private final CategoryTaxRateAdminDeleteService categoryTaxRateAdminDeleteService;

	public CategoryTaxRateController(CategoryTaxRateAdminCreateService categoryTaxRateAdminCreateService,
			CategoryTaxRateAdminUpdateService categoryTaxRateAdminUpdateService,
			CategoryTaxRateAdminDetailService categoryTaxRateAdminDetailService,
			CategoryTaxRateAdminListService categoryTaxRateAdminListService,
			CategoryTaxRateAdminDeleteService categoryTaxRateAdminDeleteService) {
		this.categoryTaxRateAdminCreateService = categoryTaxRateAdminCreateService;
		this.categoryTaxRateAdminUpdateService = categoryTaxRateAdminUpdateService;
		this.categoryTaxRateAdminDetailService = categoryTaxRateAdminDetailService;
		this.categoryTaxRateAdminListService = categoryTaxRateAdminListService;
		this.categoryTaxRateAdminDeleteService = categoryTaxRateAdminDeleteService;
	}

	@Activated(routeAlias = "get.category.taxrate.list")
	@GetMapping(value = "/list", name = "分类税率列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getTaxRateList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false, defaultValue = "1") String pageStr,
			@RequestParam(name = "page_size", required = false, defaultValue = "20") String pageSizeStr,
			@RequestParam(name = "sales_party_id", required = false) String salesPartyId,
			@RequestParam(name = "tax_rate_type", required = false) String taxRateType) {
		long companyId = readCompanyIdFromJwt(request);
		int page = parseIntDefault(pageStr, 1);
		if (page < 1) {
			page = 1;
		}
		int pageSize = parseIntDefault(pageSizeStr, 20);
		if (pageSize < 1) {
			pageSize = 20;
		}
		Optional<String> salesPartyIdLike = StringUtils.hasText(salesPartyId)
				? Optional.of(salesPartyId.trim())
				: Optional.empty();
		Optional<String> taxRateTypeEq = StringUtils.hasText(taxRateType)
				? Optional.of(taxRateType.trim())
				: Optional.empty();
		return ApiResult.ok(categoryTaxRateAdminListService.getTaxRateList(companyId, salesPartyIdLike,
				taxRateTypeEq, page, pageSize));
	}

	@GetMapping(value = "/info/{id}", name = "分类税率详情", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getTaxRateDetail(@PathVariable("id") String id) {
		Map<String, Object> data = categoryTaxRateAdminDetailService.getTaxRateDetail(id);
		return ApiResult.ok(data);
	}

	@PostMapping(value = "/create", name = "新增分类税率", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> createTaxRate(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = readCompanyIdFromJwt(request);
		Map<String, Object> data = categoryTaxRateAdminCreateService.createTaxRate(companyId, merged);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "update.category.taxrate")
	@PostMapping(value = "/update/{id}", name = "修改分类税率", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> updateTaxRate(HttpServletRequest request,
			@PathVariable("id") String id,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long rateId;
		try {
			rateId = Long.parseLong(id == null ? "" : id.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("id 格式错误");
		}
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = readCompanyIdFromJwt(request);
		merged.put("company_id", companyId);
		Map<String, Object> data = categoryTaxRateAdminUpdateService.updateTaxRate(companyId, rateId, merged);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "delete.category.taxrate")
	@PostMapping(value = "/delete/{id}", name = "删除分类税率", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> deleteTaxRate(HttpServletRequest request,
			@PathVariable("id") String id) {
		long rateId;
		try {
			rateId = Long.parseLong(id == null ? "" : id.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("id 格式错误");
		}
		long companyId = readCompanyIdFromJwt(request);
		Map<String, Object> data = categoryTaxRateAdminDeleteService.deleteTaxRate(companyId, rateId);
		return ApiResult.ok(data);
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

	private static int parseIntDefault(String s, int dflt) {
		if (s == null || s.isBlank()) {
			return dflt;
		}
		try {
			return Integer.parseInt(s.trim());
		} catch (NumberFormatException e) {
			return dflt;
		}
	}
}
