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
import cn.shopex.ecshopx.espier.api.admin.v1.EspierAdminJwtControllerSupport;
import cn.shopex.ecshopx.orders.service.admin.AdminDeliveryListsService;
import cn.shopex.ecshopx.orders.service.admin.AdminDeliveryProcessLogListService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@RestController("ordersAdminV1Delivery")
@RequestMapping("/api/v1")
public class DeliveryController {

	private final AdminDeliveryListsService adminDeliveryListsService;
	private final AdminDeliveryProcessLogListService adminDeliveryProcessLogListService;

	public DeliveryController(
			AdminDeliveryListsService adminDeliveryListsService,
			AdminDeliveryProcessLogListService adminDeliveryProcessLogListService) {
		this.adminDeliveryListsService = adminDeliveryListsService;
		this.adminDeliveryProcessLogListService = adminDeliveryProcessLogListService;
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "order.delivery.lists")
	@GetMapping(value = "/delivery/lists", name = "发货单列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<List<LinkedHashMap<String, Object>>> lists(
			HttpServletRequest request,
			@RequestParam(name = "order_id", required = false) String orderIdParam) {
		long companyId = readCompanyIdFromJwt(request);
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		String operatorType = "";
		if (attr instanceof Map<?, ?> jwt) {
			operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(jwt.get("operator_type"));
		}
		long operatorId = readOperatorIdFromJwt(request);
		return ApiResult.ok(
				adminDeliveryListsService.lists(
						companyId, operatorType == null ? "" : operatorType, operatorId, orderIdParam));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "delivery.process.list")
	@GetMapping(value = "/delivery/process/list", name = "订单物流日志", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<LinkedHashMap<String, Object>> processLogList(
			HttpServletRequest request,
			@RequestParam(name = "order_id", required = false) String orderIdParam) {
		long companyId = readCompanyIdFromJwt(request);
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		String operatorType = "";
		if (attr instanceof Map<?, ?> jwt) {
			operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(jwt.get("operator_type"));
		}
		long operatorId = readOperatorIdFromJwt(request);
		return ApiResult.ok(
				adminDeliveryProcessLogListService.processLogList(
						companyId, operatorType == null ? "" : operatorType, operatorId, orderIdParam));
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
			return 0L;
		}
		Map<?, ?> jwt = (Map<?, ?>) attr;
		Object op = jwt.get("operator_id");
		if (op == null) {
			return 0L;
		}
		try {
			return Long.parseLong(String.valueOf(op).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
