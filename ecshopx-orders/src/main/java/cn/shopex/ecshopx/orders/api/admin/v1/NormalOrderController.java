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
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.orders.service.normal.shopadmin.NormalOrderAdminCheckoutService;
import cn.shopex.ecshopx.orders.service.normal.shopadmin.NormalOrderAdminCreateUserOrderService;
import cn.shopex.ecshopx.orders.service.normal.shopadmin.NormalOrderAdminPaymentQueryService;
import cn.shopex.ecshopx.orders.service.normal.shopadmin.NormalOrderAdminPaymentService;
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
		notFound = false)
@AdminAuth
@RestController("ordersAdminV1NormalOrder")
@RequestMapping("/api/v1")
public class NormalOrderController {

	private final NormalOrderAdminCreateUserOrderService normalOrderAdminCreateUserOrderService;
	private final NormalOrderAdminCheckoutService normalOrderAdminCheckoutService;
	private final NormalOrderAdminPaymentService normalOrderAdminPaymentService;
	private final NormalOrderAdminPaymentQueryService normalOrderAdminPaymentQueryService;

	public NormalOrderController(
			NormalOrderAdminCreateUserOrderService normalOrderAdminCreateUserOrderService,
			NormalOrderAdminCheckoutService normalOrderAdminCheckoutService,
			NormalOrderAdminPaymentService normalOrderAdminPaymentService,
			NormalOrderAdminPaymentQueryService normalOrderAdminPaymentQueryService) {
		this.normalOrderAdminCreateUserOrderService = normalOrderAdminCreateUserOrderService;
		this.normalOrderAdminCheckoutService = normalOrderAdminCheckoutService;
		this.normalOrderAdminPaymentService = normalOrderAdminPaymentService;
		this.normalOrderAdminPaymentQueryService = normalOrderAdminPaymentQueryService;
	}

	@Activated(routeAlias = "order.checkout")
	@PostMapping(value = "/checkout", name = "购物车结算", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> checkout(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = readCompanyIdFromJwt(request);
		long operatorId = readOperatorIdFromJwt(request);
		Map<String, Object> data = normalOrderAdminCheckoutService.checkout(companyId, operatorId, request, merged);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "order.create")
	@PostMapping(value = "/order/create", name = "代客下单", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> createUserOrder(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = readCompanyIdFromJwt(request);
		long operatorId = readOperatorIdFromJwt(request);
		Map<String, Object> data =
				normalOrderAdminCreateUserOrderService.createUserOrder(companyId, operatorId, request, merged);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "order.payment")
	@PostMapping(value = "/order/payment", name = "下单支付", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> payment(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Map<String, Object> data = normalOrderAdminPaymentService.payment(request, merged);
		return ApiResult.ok(data);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@Activated(routeAlias = "order.payment.query")
	@GetMapping(value = "/order/payment/query", name = "支付结果查询", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> queryPayment(HttpServletRequest request) {
		boolean tradeIdKeyPresent = request.getParameterMap().containsKey("trade_id");
		String raw = request.getParameter("trade_id");
		if (tradeIdKeyPresent && (raw == null || raw.isEmpty() || "0".equals(raw))) {
			throw new BadRequestException("支付单号不存在", 400);
		}
		String tradeId = tradeIdKeyPresent ? raw : null;
		Map<String, Object> data = normalOrderAdminPaymentQueryService.queryPayment(request, tradeId);
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
