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
import cn.shopex.ecshopx.espier.api.admin.v1.EspierAdminJwtControllerSupport;
import cn.shopex.ecshopx.common.dispatch.NormalOrderPaySuccessDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.orders.service.offline.OfflinePaymentDoCheckService;
import cn.shopex.ecshopx.orders.service.offline.OfflinePaymentGetInfoService;
import cn.shopex.ecshopx.orders.service.offline.OfflinePaymentGetListService;
import cn.shopex.ecshopx.orders.service.offline.export.OfflinePaymentExportService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@RestController("ordersAdminV1OfflinePayment")
@RequestMapping("/api/v1/order/offline_payment")
public class OfflinePaymentController {

	private final OfflinePaymentDoCheckService offlinePaymentDoCheckService;
	private final OfflinePaymentExportService offlinePaymentExportService;
	private final OfflinePaymentGetInfoService offlinePaymentGetInfoService;
	private final OfflinePaymentGetListService offlinePaymentGetListService;
	private final ObjectMapper objectMapper;

	public OfflinePaymentController(
			OfflinePaymentDoCheckService offlinePaymentDoCheckService,
			OfflinePaymentExportService offlinePaymentExportService,
			OfflinePaymentGetInfoService offlinePaymentGetInfoService,
			OfflinePaymentGetListService offlinePaymentGetListService,
			ObjectMapper objectMapper) {
		this.offlinePaymentDoCheckService = offlinePaymentDoCheckService;
		this.offlinePaymentExportService = offlinePaymentExportService;
		this.offlinePaymentGetInfoService = offlinePaymentGetInfoService;
		this.offlinePaymentGetListService = offlinePaymentGetListService;
		this.objectMapper = objectMapper;
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@Activated(routeAlias = "order.offline_payment.get_list")
	@GetMapping(value = "/get_list", name = "线下转账列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getList(HttpServletRequest request) {
		Map<String, Object> query = FlexibleHttpServletParameterMap.toObjectMap(request);
		long companyId = readCompanyIdFromJwt(request);
		return ApiResult.ok(offlinePaymentGetListService.getList(companyId, query));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@Activated(routeAlias = "order.offline_payment.get_info")
	@GetMapping(value = "/get_info", name = "线下转账详情", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getInfo(HttpServletRequest request) {
		Map<String, Object> query = FlexibleHttpServletParameterMap.toObjectMap(request);
		long companyId = readCompanyIdFromJwt(request);
		return ApiResult.ok(offlinePaymentGetInfoService.getInfo(companyId, query));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	/**
	 * Offline transfer review for {@code POST /api/v1/order/offline_payment/do_check}. Delegates to
	 * {@link OfflinePaymentDoCheckService#doCheck(long, long, String, String, Map)}.
	 *
	 * <p>On approval, the service completes persistence, moves the trade to {@code SUCCESS}, and publishes a
	 * trade-finish payload. Synchronous trade-finish listeners may bridge eligible mall orders to
	 * {@link NormalOrderPaySuccessDispatchPublisher}, which in the assembled application maps to Bus event name
	 * {@link OrdersDispatchEventNames#EVENT_NORMAL_ORDER_PAY_SUCCESS}. A refusal does not run that pay-success path.
	 */
	@Activated(routeAlias = "order.offline_payment.do_check")
	@PostMapping(value = "/do_check", name = "线下转账审核", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> doCheck(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = (body == null) ? new LinkedHashMap<>() : new LinkedHashMap<>(body);
		long companyId = readCompanyIdFromJwt(request);
		long operatorId = readOperatorIdFromJwt(request);
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		String operatorType = "";
		String operatorName = "";
		if (attr instanceof Map<?, ?> jwt) {
			String ot = EspierAdminJwtControllerSupport.optionalTrimmedString(jwt.get("operator_type"));
			operatorType = ot == null ? "" : ot;
			Object m = jwt.get("mobile");
			operatorName = m == null ? "" : String.valueOf(m);
		}
		Map<String, Object> resultRow =
				offlinePaymentDoCheckService.doCheck(companyId, operatorId, operatorType, operatorName, merged);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("result", resultRow);
		return ApiResult.ok(data);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@Activated(routeAlias = "order.offline_payment.export_data")
	@PostMapping(value = "/export_data", name = "导出线下转账", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> exportData(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = readCompanyIdFromJwt(request);
		long operatorId = readOperatorIdFromJwt(request);
		offlinePaymentExportService.exportData(companyId, operatorId, merged);
		try {
			String json = objectMapper.writeValueAsString(Map.of("status", true));
			return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
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
