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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.orders.service.invoice.BaiwangInvoiceSettingService;
import cn.shopex.ecshopx.orders.service.invoice.InvoiceResendEmailService;
import cn.shopex.ecshopx.orders.service.invoice.InvoiceRetryFailedService;
import cn.shopex.ecshopx.orders.service.invoice.OrderInvoiceProtocolService;
import cn.shopex.ecshopx.companys.service.setting.InvoiceSettingRedisService;
import cn.shopex.ecshopx.orders.service.invoice.OrderInvoiceSettingService;
import cn.shopex.ecshopx.orders.service.invoice.OrderInvoiceDetailService;
import cn.shopex.ecshopx.orders.service.invoice.OrderInvoiceListService;
import cn.shopex.ecshopx.orders.service.invoice.OrderInvoiceLogListService;
import cn.shopex.ecshopx.orders.service.invoice.OrderInvoiceUpdateService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@RestController("ordersAdminV1Invoice")
@RequestMapping("/api/v1/order/invoice")
public class InvoiceController {

	private final BaiwangInvoiceSettingService baiwangInvoiceSettingService;
	private final OrderInvoiceProtocolService orderInvoiceProtocolService;
	private final InvoiceResendEmailService invoiceResendEmailService;
	private final InvoiceRetryFailedService invoiceRetryFailedService;
	private final OrderInvoiceSettingService orderInvoiceSettingService;
	private final InvoiceSettingRedisService invoiceSettingRedisService;
	private final OrderInvoiceUpdateService orderInvoiceUpdateService;
	private final OrderInvoiceDetailService orderInvoiceDetailService;
	private final OrderInvoiceListService orderInvoiceListService;
	private final OrderInvoiceLogListService orderInvoiceLogListService;

	public InvoiceController(BaiwangInvoiceSettingService baiwangInvoiceSettingService,
			OrderInvoiceProtocolService orderInvoiceProtocolService,
			InvoiceResendEmailService invoiceResendEmailService,
			InvoiceRetryFailedService invoiceRetryFailedService,
			OrderInvoiceSettingService orderInvoiceSettingService,
			InvoiceSettingRedisService invoiceSettingRedisService,
			OrderInvoiceUpdateService orderInvoiceUpdateService,
			OrderInvoiceDetailService orderInvoiceDetailService,
			OrderInvoiceListService orderInvoiceListService,
			OrderInvoiceLogListService orderInvoiceLogListService) {
		this.baiwangInvoiceSettingService = baiwangInvoiceSettingService;
		this.orderInvoiceProtocolService = orderInvoiceProtocolService;
		this.invoiceResendEmailService = invoiceResendEmailService;
		this.invoiceRetryFailedService = invoiceRetryFailedService;
		this.orderInvoiceSettingService = orderInvoiceSettingService;
		this.invoiceSettingRedisService = invoiceSettingRedisService;
		this.orderInvoiceUpdateService = orderInvoiceUpdateService;
		this.orderInvoiceDetailService = orderInvoiceDetailService;
		this.orderInvoiceListService = orderInvoiceListService;
		this.orderInvoiceLogListService = orderInvoiceLogListService;
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@Activated(routeAlias = "get.invoice.list")
	@GetMapping(value = "/list", name = "订单发票列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getInvoiceList(HttpServletRequest request) {
		long companyId = readCompanyIdFromJwt(request);
		return ApiResult.ok(orderInvoiceListService.getInvoiceList(companyId, request));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@GetMapping(value = "/info/{id}", name = "订单发票详情", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getInvoiceDetail(HttpServletRequest request, @PathVariable("id") String id) {
		long companyId = readCompanyIdFromJwt(request);
		return ApiResult.ok(orderInvoiceDetailService.getInvoiceDetail(companyId, id));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@Activated(routeAlias = "get.invoice.update")
	@PostMapping(value = "/update/{id}", name = "订单发票更新", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Object> updateInvoice(HttpServletRequest request, @PathVariable("id") String id,
			@FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = readCompanyIdFromJwt(request);
		long operatorId = readOperatorIdFromJwt(request);
		return ApiResult.ok(orderInvoiceUpdateService.updateInvoice(companyId, operatorId, id, merged));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@Activated(routeAlias = "get.invoice.update.remark")
	@PostMapping(value = "/updateremark/{id}", name = "发票备注更新", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Object> updateInvoiceRemark(HttpServletRequest request, @PathVariable("id") String id,
			@FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Object raw = merged.get("remark");
		String remark = (raw == null) ? "" : String.valueOf(raw);
		long companyId = readCompanyIdFromJwt(request);
		long operatorId = readOperatorIdFromJwt(request);
		return ApiResult.ok(orderInvoiceUpdateService.updateInvoiceRemark(companyId, operatorId, id, remark));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@GetMapping(value = "/log/list", name = "发票日志列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getInvoiceLogList(HttpServletRequest request) {
		long companyId = readCompanyIdFromJwt(request);
		return ApiResult.ok(orderInvoiceLogListService.getInvoiceLogList(companyId, request));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@PostMapping(value = "/resend", name = "重发邮件", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> resendInvoice(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = readCompanyIdFromJwt(request);
		invoiceResendEmailService.resendInvoice(companyId, merged);
		return ApiResult.ok(Map.of("status", Boolean.TRUE));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@PostMapping(value = "/retryFailedInvoice", name = "重新开票", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> retryFailedInvoice(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = readCompanyIdFromJwt(request);
		long operatorId = readOperatorIdFromJwt(request);
		invoiceRetryFailedService.retryFailedInvoice(companyId, operatorId, merged);
		return ApiResult.ok(Map.of("status", Boolean.TRUE));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@GetMapping(value = "/setting", name = "开票配置", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Object> getInvoiceSetting(HttpServletRequest request) {
		long companyId = readCompanyIdFromJwt(request);
		Object payload = invoiceSettingRedisService.getInvoiceSetting(companyId);
		return ApiResult.ok(payload);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@PostMapping(value = "/setting", name = "设置开票配置", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> setInvoiceSetting(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = readCompanyIdFromJwt(request);
		orderInvoiceSettingService.setInvoiceSetting(companyId, merged);
		return ApiResult.ok(Map.of("status", Boolean.TRUE));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@PostMapping(value = "/baiwangInvoiceSetting", name = "百旺发票配置", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> setBaiwangInvoiceSetting(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = readCompanyIdFromJwt(request);
		Map<String, Object> result = baiwangInvoiceSettingService.setBaiwangInvoiceSetting(companyId, merged);
		if (!Boolean.TRUE.equals(result.get("success"))) {
			Object msgObj = result.get("message");
			String msg = (msgObj == null || String.valueOf(msgObj).isEmpty()) ? "请求失败" : String.valueOf(msgObj);
			throw new ResourceException(msg);
		}
		return ApiResult.ok(result);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@GetMapping(value = "/baiwangInvoiceSetting", name = "百旺发票配置", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getBaiwangInvoiceSetting(HttpServletRequest request) {
		long companyId = readCompanyIdFromJwt(request);
		return ApiResult.ok(baiwangInvoiceSettingService.getBaiwangInvoiceSetting(companyId));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@PostMapping(value = "/protocol", name = "设置发票协议", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> setInvoiceProtocol(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = readCompanyIdFromJwt(request);
		orderInvoiceProtocolService.setInvoiceProtocol(companyId, merged);
		return ApiResult.ok(Map.of("status", Boolean.TRUE));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@GetMapping(value = "/protocol", name = "发票协议", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getInvoiceProtocol(HttpServletRequest request) {
		long companyId = readCompanyIdFromJwt(request);
		Map<String, Object> protocol = orderInvoiceProtocolService.getInvoiceProtocol(companyId);
		if (protocol == null) {
			LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
			payload.put("data", null);
			return ApiResult.ok(payload);
		}
		return ApiResult.ok(protocol);
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

	private static long readOperatorIdFromJwt(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> jwt = (Map<?, ?>) attr;
		Object operatorIdObj = jwt.get("operator_id");
		if (operatorIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		try {
			return Long.parseLong(String.valueOf(operatorIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
	}
}
