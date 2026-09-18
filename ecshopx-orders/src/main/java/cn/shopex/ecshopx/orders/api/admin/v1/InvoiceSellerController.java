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
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.orders.service.invoiceseller.InvoiceSellerAdminCreateService;
import cn.shopex.ecshopx.orders.service.invoiceseller.InvoiceSellerAdminDetailService;
import cn.shopex.ecshopx.orders.service.invoiceseller.InvoiceSellerAdminListService;
import cn.shopex.ecshopx.orders.service.invoiceseller.InvoiceSellerAdminUpdateService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RestController("ordersAdminV1InvoiceSeller")
@RequestMapping("/api/v1/order/invoice-seller")
public class InvoiceSellerController {

	private final InvoiceSellerAdminCreateService invoiceSellerAdminCreateService;
	private final InvoiceSellerAdminUpdateService invoiceSellerAdminUpdateService;
	private final InvoiceSellerAdminDetailService invoiceSellerAdminDetailService;
	private final InvoiceSellerAdminListService invoiceSellerAdminListService;

	public InvoiceSellerController(
			InvoiceSellerAdminCreateService invoiceSellerAdminCreateService,
			InvoiceSellerAdminUpdateService invoiceSellerAdminUpdateService,
			InvoiceSellerAdminDetailService invoiceSellerAdminDetailService,
			InvoiceSellerAdminListService invoiceSellerAdminListService) {
		this.invoiceSellerAdminCreateService = invoiceSellerAdminCreateService;
		this.invoiceSellerAdminUpdateService = invoiceSellerAdminUpdateService;
		this.invoiceSellerAdminDetailService = invoiceSellerAdminDetailService;
		this.invoiceSellerAdminListService = invoiceSellerAdminListService;
	}

	@Activated(routeAlias = "get.invoice.seller.list")
	@GetMapping(value = "/list", name = "发票销售方列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getSellerList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false, defaultValue = "1") String pageStr,
			@RequestParam(name = "page_size", required = false, defaultValue = "20") String pageSizeStr,
			@RequestParam(name = "seller_company_name", required = false) String sellerCompanyName,
			@RequestParam(name = "seller_tax_no", required = false) String sellerTaxNo) {
		long companyId = readCompanyIdFromJwt(request);
		int page = parseIntDefault(pageStr, 1);
		if (page < 1) {
			page = 1;
		}
		int pageSize = parseIntDefault(pageSizeStr, 20);
		if (pageSize < 1) {
			pageSize = 20;
		}
		Optional<String> sellerCompanyNameLike = StringUtils.hasText(sellerCompanyName)
				? Optional.of(sellerCompanyName.trim())
				: Optional.empty();
		Optional<String> sellerTaxNoEq = StringUtils.hasText(sellerTaxNo)
				? Optional.of(sellerTaxNo.trim())
				: Optional.empty();
		return ApiResult.ok(invoiceSellerAdminListService.getSellerList(companyId, sellerCompanyNameLike,
				sellerTaxNoEq, page, pageSize));
	}

	@GetMapping(value = "/info/{id}", name = "发票销售方详情", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getSellerDetail(@PathVariable("id") String id) {
		return ApiResult.ok(invoiceSellerAdminDetailService.getSellerDetail(id));
	}

	@PostMapping(value = "/create", name = "新增销售方", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> createSeller(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = readCompanyIdFromJwt(request);
		Map<String, Object> data = invoiceSellerAdminCreateService.createSeller(companyId, merged);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "update.invoice.seller")
	@PostMapping(value = "/update/{id}", name = "修改销售方", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> updateSeller(
			HttpServletRequest request,
			@PathVariable("id") String id,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		return ApiResult.ok(invoiceSellerAdminUpdateService.updateSeller(id, merged));
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
