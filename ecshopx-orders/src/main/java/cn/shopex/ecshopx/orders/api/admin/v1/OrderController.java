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
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.espier.api.admin.v1.EspierAdminJwtControllerSupport;
import cn.shopex.ecshopx.orders.service.admin.AdminCancelDeliveryStaffService;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderCancelService;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderConfirmCancelService;
import cn.shopex.ecshopx.orders.service.admin.AdminProcessDrugOrdersService;
import cn.shopex.ecshopx.orders.service.admin.AdminConfirmDeliveryPackagService;
import cn.shopex.ecshopx.orders.service.admin.AdminConfirmDeliveryStaffService;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderConfirmReceiptService;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderDeliveryService;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderMarkDownService;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderProcessLogListService;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderSetInvoicedService;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderUpdateDeliveryOldService;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderUpdateDeliveryService;
import cn.shopex.ecshopx.orders.service.admin.AdminDeliveryTrackerPullService;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderCancelInfoService;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderDetailService;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderListService;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderUpdateRemarksService;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderUpdateInvoiceNumberService;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderQrWriteoffService;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderWriteoffService;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderValiditySettingSetService;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import cn.shopex.ecshopx.orders.service.localdelivery.LocalDeliveryBusinessReceiptService;
import cn.shopex.ecshopx.orders.service.localdelivery.LocalDeliveryConfirmGoodsService;
import cn.shopex.ecshopx.orders.service.userinvoice.UserOrderInvoiceAdminGetInvoiceListService;
import cn.shopex.ecshopx.orders.service.userinvoice.UserOrderInvoiceAdminSetInvoiceService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;

@AdminAuth
@ShopLog
@RestController("ordersAdminV1Order")
@RequestMapping("/api/v1")
public class OrderController {

	private final UserOrderInvoiceAdminSetInvoiceService userOrderInvoiceAdminSetInvoiceService;
	private final UserOrderInvoiceAdminGetInvoiceListService userOrderInvoiceAdminGetInvoiceListService;
	private final LocalDeliveryBusinessReceiptService localDeliveryBusinessReceiptService;
	private final LocalDeliveryConfirmGoodsService localDeliveryConfirmGoodsService;
	private final AdminNormalOrderConfirmReceiptService adminNormalOrderConfirmReceiptService;
	private final AdminOrderDeliveryService adminOrderDeliveryService;
	private final AdminOrderSetInvoicedService adminOrderSetInvoicedService;
	private final AdminOrderUpdateInvoiceNumberService adminOrderUpdateInvoiceNumberService;
	private final AdminOrderMarkDownService adminOrderMarkDownService;
	private final AdminOrderProcessLogListService adminOrderProcessLogListService;
	private final AdminCancelDeliveryStaffService adminCancelDeliveryStaffService;
	private final AdminConfirmDeliveryPackagService adminConfirmDeliveryPackagService;
	private final AdminConfirmDeliveryStaffService adminConfirmDeliveryStaffService;
	private final AdminOrderCancelService adminOrderCancelService;
	private final AdminOrderConfirmCancelService adminOrderConfirmCancelService;
	private final AdminProcessDrugOrdersService adminProcessDrugOrdersService;
	private final AdminOrderValiditySettingSetService adminOrderValiditySettingSetService;
	private final OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;
	private final AdminOrderQrWriteoffService adminOrderQrWriteoffService;
	private final AdminOrderWriteoffService adminOrderWriteoffService;
	private final AdminOrderUpdateDeliveryService adminOrderUpdateDeliveryService;
	private final AdminOrderUpdateDeliveryOldService adminOrderUpdateDeliveryOldService;
	private final AdminOrderUpdateRemarksService adminOrderUpdateRemarksService;
	private final AdminDeliveryTrackerPullService adminDeliveryTrackerPullService;
	private final AdminOrderCancelInfoService adminOrderCancelInfoService;
	private final AdminOrderDetailService adminOrderDetailService;
	private final AdminOrderListService adminOrderListService;

	public OrderController(
			UserOrderInvoiceAdminSetInvoiceService userOrderInvoiceAdminSetInvoiceService,
			UserOrderInvoiceAdminGetInvoiceListService userOrderInvoiceAdminGetInvoiceListService,
			LocalDeliveryBusinessReceiptService localDeliveryBusinessReceiptService,
			LocalDeliveryConfirmGoodsService localDeliveryConfirmGoodsService,
			AdminNormalOrderConfirmReceiptService adminNormalOrderConfirmReceiptService,
			AdminOrderDeliveryService adminOrderDeliveryService,
			AdminOrderSetInvoicedService adminOrderSetInvoicedService,
			AdminOrderUpdateInvoiceNumberService adminOrderUpdateInvoiceNumberService,
			AdminOrderMarkDownService adminOrderMarkDownService,
			AdminOrderProcessLogListService adminOrderProcessLogListService,
			AdminCancelDeliveryStaffService adminCancelDeliveryStaffService,
			AdminConfirmDeliveryPackagService adminConfirmDeliveryPackagService,
			AdminConfirmDeliveryStaffService adminConfirmDeliveryStaffService,
			AdminOrderCancelService adminOrderCancelService,
			AdminOrderConfirmCancelService adminOrderConfirmCancelService,
			AdminProcessDrugOrdersService adminProcessDrugOrdersService,
			AdminOrderValiditySettingSetService adminOrderValiditySettingSetService,
			OrderValiditySettingRedisReadService orderValiditySettingRedisReadService,
			AdminOrderQrWriteoffService adminOrderQrWriteoffService,
			AdminOrderWriteoffService adminOrderWriteoffService,
			AdminOrderUpdateDeliveryService adminOrderUpdateDeliveryService,
			AdminOrderUpdateDeliveryOldService adminOrderUpdateDeliveryOldService,
			AdminOrderUpdateRemarksService adminOrderUpdateRemarksService,
			AdminDeliveryTrackerPullService adminDeliveryTrackerPullService,
			AdminOrderCancelInfoService adminOrderCancelInfoService,
			AdminOrderDetailService adminOrderDetailService,
			AdminOrderListService adminOrderListService) {
		this.userOrderInvoiceAdminSetInvoiceService = userOrderInvoiceAdminSetInvoiceService;
		this.userOrderInvoiceAdminGetInvoiceListService = userOrderInvoiceAdminGetInvoiceListService;
		this.localDeliveryBusinessReceiptService = localDeliveryBusinessReceiptService;
		this.localDeliveryConfirmGoodsService = localDeliveryConfirmGoodsService;
		this.adminNormalOrderConfirmReceiptService = adminNormalOrderConfirmReceiptService;
		this.adminOrderDeliveryService = adminOrderDeliveryService;
		this.adminOrderSetInvoicedService = adminOrderSetInvoicedService;
		this.adminOrderUpdateInvoiceNumberService = adminOrderUpdateInvoiceNumberService;
		this.adminOrderMarkDownService = adminOrderMarkDownService;
		this.adminOrderProcessLogListService = adminOrderProcessLogListService;
		this.adminCancelDeliveryStaffService = adminCancelDeliveryStaffService;
		this.adminConfirmDeliveryPackagService = adminConfirmDeliveryPackagService;
		this.adminConfirmDeliveryStaffService = adminConfirmDeliveryStaffService;
		this.adminOrderCancelService = adminOrderCancelService;
		this.adminOrderConfirmCancelService = adminOrderConfirmCancelService;
		this.adminProcessDrugOrdersService = adminProcessDrugOrdersService;
		this.adminOrderValiditySettingSetService = adminOrderValiditySettingSetService;
		this.orderValiditySettingRedisReadService = orderValiditySettingRedisReadService;
		this.adminOrderQrWriteoffService = adminOrderQrWriteoffService;
		this.adminOrderWriteoffService = adminOrderWriteoffService;
		this.adminOrderUpdateDeliveryService = adminOrderUpdateDeliveryService;
		this.adminOrderUpdateDeliveryOldService = adminOrderUpdateDeliveryOldService;
		this.adminOrderUpdateRemarksService = adminOrderUpdateRemarksService;
		this.adminDeliveryTrackerPullService = adminDeliveryTrackerPullService;
		this.adminOrderCancelInfoService = adminOrderCancelInfoService;
		this.adminOrderDetailService = adminOrderDetailService;
		this.adminOrderListService = adminOrderListService;
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@DataPass
	@Activated(routeAlias = "order.list.get")
	@GetMapping(value = "/orders", name = "订单列表")
	public ApiResult<Map<String, Object>> getOrderList(HttpServletRequest request) {
		long companyId = readCompanyIdFromJwt(request);
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> rawJwt)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) rawJwt;
		return ApiResult.ok(adminOrderListService.getOrderList(companyId, jwt, request));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@DataPass
	@Activated(routeAlias = "order.info.get")
	@GetMapping(value = "/order/{order_id}", name = "订单详情")
	public ApiResult<Map<String, Object>> getOrderDetail(
			HttpServletRequest request, @PathVariable("order_id") String orderId) {
		long companyId = readCompanyIdFromJwt(request);
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> rawJwt)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) rawJwt;
		return ApiResult.ok(adminOrderDetailService.getOrderDetail(companyId, orderId, jwt, request));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@DataPass
	@Activated(routeAlias = "order.process.log.get")
	@GetMapping(value = "/order/process/{orderId}", name = "订单操作详情")
	public ApiResult<List<Map<String, Object>>> getOrderProcessLog(
			HttpServletRequest request, @PathVariable("orderId") String orderId) {
		long companyId = readCompanyIdFromJwt(request);
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		String operatorType = null;
		if (attr instanceof Map<?, ?> jwt) {
			operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(jwt.get("operator_type"));
		}
		long operatorIdJwt = readOperatorIdFromJwt(request);
		boolean dataMask = parseDatapassBlockForOrderProcessLog(request) != 0;
		List<Map<String, Object>> rows =
				adminOrderProcessLogListService.getOrderProcessLog(
						companyId, operatorType, operatorIdJwt, orderId, dataMask);
		return ApiResult.ok(rows);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "invoice.list.get")
	@GetMapping(value = "/fapiaolist", name = "发票列表")
	public ApiResult<Map<String, Object>> getInvoiceList(
			HttpServletRequest request,
			@RequestParam(name = "user_id", required = false, defaultValue = "0") String userIdStr,
			@RequestParam(name = "id", required = false, defaultValue = "0") String idStr,
			@RequestParam(name = "order_id", required = false, defaultValue = "0") String orderIdStr,
			@RequestParam(name = "status", required = false, defaultValue = "0") String statusStr,
			@RequestParam(name = "page", required = false, defaultValue = "1") String pageStr,
			@RequestParam(name = "pageSize", required = false, defaultValue = "20") String pageSizeStr) {
		long companyId = readCompanyIdFromJwt(request);
		long uid = parseLongDefault(userIdStr, 0L);
		Long filterUserId = uid == 0L ? null : Long.valueOf(uid);
		Long filterId =
				parseLongDefault(idStr, 0L) == 0L
						? null
						: Long.valueOf(parseLongDefault(idStr, 0L));
		String orderIdRaw = orderIdStr == null ? "" : orderIdStr.trim();
		String filterOrderId =
				orderIdRaw.isEmpty() || "0".equals(orderIdRaw) ? null : orderIdRaw;
		int filterStatus = parseIntDefault(statusStr, 0);
		int page = parseIntDefault(pageStr, 1);
		if (page < 1) {
			page = 1;
		}
		int pageSize = parseIntDefault(pageSizeStr, 20);
		if (pageSize < 1) {
			pageSize = 20;
		}
		return ApiResult.ok(
				userOrderInvoiceAdminGetInvoiceListService.getInvoiceList(
						companyId, filterUserId, filterId, filterOrderId, filterStatus, page, pageSize));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "invoice.set")
	@GetMapping(value = "/fapiaoset", name = "发票操作")
	public ApiResult<Map<String, Object>> setInvoice(
			HttpServletRequest request,
			@RequestParam(name = "user_id", required = false, defaultValue = "0") String userIdStr,
			@RequestParam(name = "id", required = false, defaultValue = "0") String idStr,
			@RequestParam(name = "order_id", required = false, defaultValue = "0") String orderIdStr,
			@RequestParam(name = "status", required = false, defaultValue = "0") String statusStr,
			@RequestParam(name = "page", required = false, defaultValue = "1") String pageStr,
			@RequestParam(name = "pageSize", required = false, defaultValue = "20") String pageSizeStr) {
		long companyId = readCompanyIdFromJwt(request);
		long uid = parseLongDefault(userIdStr, 0L);
		Long filterUserId = uid == 0L ? null : uid;
		long requestInvoiceId = parseLongDefault(idStr, 0L);
		String orderIdRaw = orderIdStr == null ? "" : orderIdStr.trim();
		String filterOrderId =
				orderIdRaw.isEmpty() || "0".equals(orderIdRaw) ? null : orderIdRaw;
		int status = parseIntDefault(statusStr, 0);
		int page = parseIntDefault(pageStr, 1);
		if (page < 1) {
			page = 1;
		}
		int pageSize = parseIntDefault(pageSizeStr, 20);
		if (pageSize < 1) {
			pageSize = 20;
		}
		Map<String, Object> payload =
				userOrderInvoiceAdminSetInvoiceService.setInvoice(
						companyId, status, requestInvoiceId, filterUserId, filterOrderId, page, pageSize);
		return ApiResult.ok(payload);
	}

	private static long parseWriteoffOrderIdOrNotFound(String orderIdPath) {
		if (orderIdPath == null || orderIdPath.isBlank()) {
			throw new ResourceException("此订单不存在！");
		}
		try {
			long id = Long.parseLong(orderIdPath.trim());
			if (id <= 0L) {
				throw new ResourceException("此订单不存在！");
			}
			return id;
		} catch (NumberFormatException e) {
			throw new ResourceException("此订单不存在！");
		}
	}

	private static String mergeWriteoffPickupcode(String pickupcodeParam, Map<String, Object> body) {
		if (body != null && body.containsKey("pickupcode")) {
			Object raw = body.get("pickupcode");
			if (raw != null) {
				String t = String.valueOf(raw).trim();
				if (StringUtils.hasText(t)) {
					return t;
				}
			}
		}
		if (pickupcodeParam != null) {
			String t = pickupcodeParam.trim();
			if (StringUtils.hasText(t)) {
				return t;
			}
		}
		return null;
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

	private static int parseDatapassBlockForOrderProcessLog(HttpServletRequest request) {
		Object attr = request.getAttribute("x-datapass-block");
		if (attr instanceof Number n && n.intValue() != 0) {
			return 1;
		}
		if (Boolean.TRUE.equals(attr)) {
			return 1;
		}
		if (attr != null) {
			String t = attr.toString().trim();
			if (!t.isEmpty() && !"0".equals(t) && !"false".equalsIgnoreCase(t)) {
				return 1;
			}
		}
		String p = request.getParameter("x-datapass-block");
		if (p == null || p.trim().isEmpty() || "0".equals(p.trim()) || "false".equalsIgnoreCase(p.trim())) {
			return 0;
		}
		return 1;
	}

	private static long parseLongDefault(String s, long dflt) {
		if (s == null || s.isBlank()) {
			return dflt;
		}
		try {
			return Long.parseLong(s.trim());
		} catch (NumberFormatException e) {
			return dflt;
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

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "order.cancel.info")
	@GetMapping(value = "/order/{order_id}/cancelinfo", name = "取消订单申请信息")
	public ApiResult<Object> getOrderCancelInfo(
			HttpServletRequest request,
			@PathVariable("order_id") String orderId,
			@RequestParam(name = "order_type", required = false) String orderType) {
		long companyId = readCompanyIdFromJwt(request);
		return ApiResult.ok(
				adminOrderCancelInfoService.getOrderCancelInfo(companyId, orderId, orderType));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "order.cancel.info")
	@PostMapping(
			value = "/order/{order_id}/confirmcancel",
			name = "确认取消审核",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> confirmOrderCancel(
			HttpServletRequest request,
			@PathVariable("order_id") String orderId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromJwt(request);
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		String operatorType = "";
		if (attr instanceof Map<?, ?> jwt) {
			operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(jwt.get("operator_type"));
		}
		long operatorId = readOperatorIdFromJwt(request);
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		merged.put("order_id", orderId);
		Map<String, Object> data =
				adminOrderConfirmCancelService.confirmOrderCancel(
						companyId, operatorType, operatorId, orderId, merged);
		return ApiResult.ok(data);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "order.process.drug")
	@PutMapping(
			value = "/order/{order_id}/processdrug",
			name = "处方单处理",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> processDrugOrders(
			HttpServletRequest request,
			@PathVariable("order_id") String orderId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromJwt(request);
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		String operatorType = "";
		if (attr instanceof Map<?, ?> jwt) {
			operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(jwt.get("operator_type"));
		}
		long operatorId = readOperatorIdFromJwt(request);
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		merged.put("order_id", orderId);
		adminProcessDrugOrdersService.processDrugOrders(
				companyId, operatorType, operatorId, orderId, merged);
		return ApiResult.ok(Map.of("status", true));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "ordersetting.set")
	@PostMapping(
			value = "/orders/setting/set",
			name = "订单配置设置",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> setOrderSetting(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromJwt(request);
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Map<String, Object> data = adminOrderValiditySettingSetService.setOrderSetting(companyId, merged);
		return ApiResult.ok(data);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "orders.setting.get")
	@GetMapping(value = "/orders/setting/get", name = "订单配置", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getOrderSetting(HttpServletRequest request) {
		long companyId = readCompanyIdFromJwt(request);
		Map<String, Object> data = orderValiditySettingRedisReadService.getOrderSetting(companyId);
		return ApiResult.ok(data);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	/**
	 * Admin {@code POST /order/{order_id}/cancel}: delegates to {@link AdminOrderCancelService#cancelOrder}. When the
	 * order is still awaiting shipment, whole-order cancellation runs in {@link cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderFullCancelService},
	 * which schedules {@link cn.shopex.ecshopx.common.dispatch.TradeRefundDispatchPublisher#publish} only after the
	 * surrounding database transaction commits (dispatch bus fan-out for SaaS ERP trade refund).
	 */
	@Activated(routeAlias = "order.cancel")
	@PostMapping(value = "/order/{order_id}/cancel", name = "取消订单", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> cancelOrder(
			HttpServletRequest request,
			@PathVariable("order_id") String orderId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromJwt(request);
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		String operatorType = "";
		if (attr instanceof Map<?, ?> jwt) {
			operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(jwt.get("operator_type"));
		}
		long operatorId = readOperatorIdFromJwt(request);
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		merged.put("order_id", orderId);
		Map<String, Object> data =
				adminOrderCancelService.cancelOrder(companyId, operatorType, operatorId, orderId, merged);
		return ApiResult.ok(data);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	/**
	 * Admin batch shipment: {@code POST /api/v1/delivery} with {@code delivery_type=batch} and operator context from
	 * the authenticated admin session.
	 *
	 * @apiNote
	 * <p>The merchant admin wxapp shipment HTTP path {@code POST /api/v1/admin/wxapp/order/delivery} binds tenant scope
	 * from the operator JWT as {@code company_id}, forces batch shipment via {@code delivery_type=batch}, and carries
	 * salesperson {@code operator_type} / {@code operator_id}. This handler mirrors that contract with
	 * {@code readCompanyIdFromJwt}, {@code merged.put("delivery_type", "batch")}, JWT-derived {@code operator_type} and
	 * {@code operator_id}, and delegation to {@link cn.shopex.ecshopx.orders.service.admin.AdminOrderDeliveryService#delivery}.
	 * The surface path here is {@code POST /api/v1/delivery}. Orchestration and marketing-center fan-out evidence:
	 * {@link cn.shopex.ecshopx.orders.service.admin.AdminOrderDeliveryServiceAdminApiOrdersDeliveryNormalOrderDeliveryTriggerProbeTest}.</p>
	 * <p>Order process log publication is handled only inside {@link AdminOrderDeliveryService#delivery} and its
	 * downstream delivery pipeline (via {@link OrderProcessLogPublishPort}); this controller method must not invoke a
	 * second publish for the same shipment.
	 * <p>{@link cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames#EVENT_NORMAL_ORDER_DELIVERY} is emitted only
	 * from {@link cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDeliveryCoreService} after the surrounding
	 * transaction commits. Unit tests: {@code AdminOrderDeliveryServiceAdminApiOrdersDeliveryNormalOrderDeliveryTriggerProbeTest},
	 * {@code AdminNormalOrderDeliveryCoreServiceWxappApiOrderDeliveryNormalOrderDeliveryDispatchPublishProbeTest} (no
	 * extra bus publish at this HTTP layer), including
	 * {@code deliveryNormalPhysical_salespersonBatch_afterCommit_invokesNormalOrderDeliveryPublisherOnceWithOrderAndCompanyPayload}
	 * for salesperson + batch merged params through core to the after-commit publisher.
	 * <p>{@code mvn -pl ecshopx-orders test -Dtest=AdminOrderDeliveryServiceAdminApiOrdersDeliveryNormalOrderDeliveryTriggerProbeTest}
	 * covers admin delivery orchestration and the marketing-center listener path via {@code DispatchConsumerRuntime}.
	 * <p>Marketing fan-out child {@code DispatchMessage} field literals are asserted in
	 * {@code delivery_whenNormalPhysicalSalespersonBatch_thenMarketingChildDispatchMessageLiteralsMatchPlanAndDispatchConsumerRuntimeConsumes}.
	 * <p><strong>DmCrm</strong> normal-order-delivery fan-out for the shipment sync slice is asserted in
	 * {@link cn.shopex.ecshopx.orders.service.admin.AdminOrderDeliveryServiceAdminApiOrdersDeliveryNormalOrderDeliveryTriggerProbeTest}
	 * {@code delivery_whenNormalPhysicalSalespersonBatch_thenPlanSection6DmCrmChildDispatchMessageLiteralsAndDispatchConsumerRuntimeConsumes},
	 * which locks plan section 6 DmCrm child {@code DispatchMessage} literals and records {@code DispatchConsumerRuntime}
	 * consumption evidence for that downstream path.</p>
	 * <p>The core service probe {@code deliveryNormalPhysical_salespersonBatch_afterCommit_event237DmCrmAnchor_publishOnce_twoKeyMap_9002_22}
	 * in {@link cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDeliveryCoreServiceWxappApiOrderDeliveryNormalOrderDeliveryDispatchPublishProbeTest}
	 * records the post-commit parent shipment-domain publish carrying only {@code order_id} and {@code company_id}.</p>
	 * <p>Further {@link cn.shopex.ecshopx.orders.service.admin.AdminOrderDeliveryServiceAdminApiOrdersDeliveryNormalOrderDeliveryTriggerProbeTest}
	 * coverage asserts AdminApi shipment orchestration through core into the shared dispatch bus: parent and DmCrm child
	 * {@code DispatchMessage} field shapes plus {@code DispatchConsumerRuntime} consumption are locked in
	 * {@code delivery_whenNormalPhysicalSalespersonBatch_event237_entry03_thenDmCrmParentAndChildDispatchMessage606505AndDispatchConsumerRuntimeConsumes}.</p>
	 */
	@Activated(routeAlias = "order.delivery")
	@PostMapping(value = "/delivery", name = "订单发货", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> delivery(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		String orderIdRaw = deliveryOrderIdFromBody(body);
		if (orderIdRaw == null) {
			throw new ResourceException("订单号缺少！");
		}
		long companyId = readCompanyIdFromJwt(request);
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		String operatorType = "";
		if (attr instanceof Map<?, ?> jwt) {
			operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(jwt.get("operator_type"));
		}
		long operatorId = readOperatorIdFromJwt(request);
		Map<String, Object> merged = new LinkedHashMap<>();
		if (body != null) {
			merged.putAll(body);
		}
		merged.put("order_id", orderIdRaw);
		Map<String, Object> result =
				adminOrderDeliveryService.delivery(companyId, operatorType, operatorId, merged);
		return ApiResult.ok(result);
	}

	private static String deliveryOrderIdFromBody(Map<String, Object> body) {
		if (body == null || !body.containsKey("order_id")) {
			return null;
		}
		String t = String.valueOf(body.get("order_id")).trim();
		if (t.isEmpty() || "0".equals(t)) {
			return null;
		}
		return t;
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "order.delivery.update")
	@PutMapping(
			value = "/delivery/{orders_delivery_id}",
			name = "发货信息修改",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> updateDelivery(
			HttpServletRequest request,
			@PathVariable("orders_delivery_id") String ordersDeliveryId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		String pathOrdersDeliveryId = ordersDeliveryId == null ? "" : ordersDeliveryId.trim();
		long companyId = readCompanyIdFromJwt(request);
		long operatorId = readOperatorIdFromJwt(request);
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		requireDeliveryCorpAndCodeForUpdate(merged);
		merged.put("orders_delivery_id", pathOrdersDeliveryId);
		merged.put("company_id", companyId);
		merged.put("operator_type", "admin");
		merged.put("operator_id", operatorId);
		Map<String, Object> data =
				adminOrderUpdateDeliveryService.updateDelivery(companyId, operatorId, pathOrdersDeliveryId, merged);
		return ApiResult.ok(data);
	}

	private static void requireDeliveryCorpAndCodeForUpdate(Map<String, Object> merged) {
		Object dc = merged.get("delivery_corp");
		if (dc == null || !StringUtils.hasText(String.valueOf(dc).trim())) {
			throw new BadRequestException("物流公司编码必填");
		}
		Object code = merged.get("delivery_code");
		if (code == null || !StringUtils.hasText(String.valueOf(code).trim())) {
			throw new BadRequestException("物流公司快递号必填");
		}
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "order.delivery.update")
	@PutMapping(
			value = "/old_delivery/{orderId}",
			name = "发货信息修改旧",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> updateDeliveryOld(
			HttpServletRequest request,
			@PathVariable("orderId") String orderId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		String pathOrderIdRaw = orderId == null ? "" : orderId.trim();
		long companyId = readCompanyIdFromJwt(request);
		long operatorId = readOperatorIdFromJwt(request);
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		merged.put("order_id", pathOrderIdRaw);
		merged.put("company_id", companyId);
		merged.put("operator_type", "admin");
		merged.put("operator_id", operatorId);
		Map<String, Object> data =
				adminOrderUpdateDeliveryOldService.updateDeliveryOld(
						companyId, operatorId, pathOrderIdRaw, merged);
		return ApiResult.ok(data);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "order.remarks.update")
	@PutMapping(
			value = "/remarks/{orderId}",
			name = "订单备注",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> updateRemarks(
			HttpServletRequest request,
			@PathVariable("orderId") String orderId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		String pathOrderIdRaw = orderId == null ? "" : orderId.trim();
		long companyId = readCompanyIdFromJwt(request);
		long operatorId = readOperatorIdFromJwt(request);
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		merged.put("order_id", pathOrderIdRaw);
		merged.put("company_id", companyId);
		merged.put("operator_type", "admin");
		merged.put("operator_id", operatorId);
		Map<String, Object> data =
				adminOrderUpdateRemarksService.updateRemarks(
						companyId, operatorId, pathOrderIdRaw, merged);
		return ApiResult.ok(data);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "order.delivery.details")
	@GetMapping(value = "/delivery/details", name = "物流详情", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<List<LinkedHashMap<String, String>>> trackerpull(HttpServletRequest request) {
		long companyId = readCompanyIdFromJwt(request);
		Map<String, Object> merged = FlexibleHttpServletParameterMap.toObjectMap(request);
		String deliveryCorp =
				merged.get("delivery_corp") == null ? "" : String.valueOf(merged.get("delivery_corp")).trim();
		String deliveryCode =
				merged.get("delivery_code") == null ? "" : String.valueOf(merged.get("delivery_code")).trim();
		boolean useKuaidi100 =
				merged.containsKey("delivery_corp_source")
						&& "kuaidi100".equals(String.valueOf(merged.get("delivery_corp_source")).trim());
		return ApiResult.ok(
				adminDeliveryTrackerPullService.trackerpull(
						companyId, deliveryCorp, deliveryCode, useKuaidi100));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "order.confirmReceipt")
	@PostMapping(value = "/confirmReceipt", name = "确认送达", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> confirmReceipt(
			HttpServletRequest request,
			@RequestParam(name = "order_id", required = false) String orderIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		String orderIdRaw = firstNonBlankOrderId(orderIdParam, body);
		if (orderIdRaw == null) {
			throw new BadRequestException("参数有误！订单号不存在！");
		}
		long companyId = readCompanyIdFromJwt(request);
		long operatorId = readOperatorIdFromJwt(request);
		Map<String, Object> data =
				adminNormalOrderConfirmReceiptService.confirmReceipt(companyId, operatorId, orderIdRaw);
		return ApiResult.ok(data);
	}

	private static String firstNonBlankOrderId(String orderIdParam, Map<String, Object> body) {
		if (orderIdParam != null) {
			String t = orderIdParam.trim();
			if (!t.isEmpty() && !"0".equals(t)) {
				return t;
			}
		}
		if (body == null || body.get("order_id") == null) {
			return null;
		}
		String t = String.valueOf(body.get("order_id")).trim();
		if (t.isEmpty() || "0".equals(t)) {
			return null;
		}
		return t;
	}

	private static String resolveOrderIdForAdminCancelDeliveryStaff(Map<String, Object> merged) {
		if (merged == null) {
			throw new BadRequestException("订单号必填");
		}
		if (!merged.containsKey("order_id")) {
			throw new BadRequestException("订单号必填");
		}
		Object v = merged.get("order_id");
		if (v == null) {
			throw new BadRequestException("订单号必填");
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty()) {
			throw new BadRequestException("订单号必填");
		}
		return s;
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "order.invoicenumber.set")
	@PostMapping(value = "/invoice/number", name = "设置发票号", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> updateInvoiceNumber(
			HttpServletRequest request,
			@RequestParam(name = "order_id", required = false) String orderIdParam,
			@RequestParam(name = "invoice_number", required = false) String invoiceNumberParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromJwt(request);
		Map<String, Object> merged = new LinkedHashMap<>();
		if (body != null) {
			merged.putAll(body);
		}
		if (orderIdParam != null) {
			String t = orderIdParam.trim();
			if (!t.isEmpty() && !"0".equals(t)) {
				merged.put("order_id", t);
			}
		}
		if (invoiceNumberParam != null) {
			merged.put("invoice_number", invoiceNumberParam.trim());
		}
		Map<String, Object> data =
				adminOrderUpdateInvoiceNumberService.updateInvoiceNumber(companyId, merged);
		return ApiResult.ok(data);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "order.invoiced.invoiced")
	@PostMapping(value = "/invoice/invoiced", name = "设置开票状态", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> setInvoiced(
			HttpServletRequest request,
			@RequestParam(name = "order_id", required = false) String orderIdParam,
			@RequestParam(name = "status", required = false) String statusParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromJwt(request);
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		String operatorType = "";
		if (attr instanceof Map<?, ?> jwt) {
			operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(jwt.get("operator_type"));
		}
		long operatorId = readOperatorIdFromJwt(request);
		Map<String, Object> merged = new LinkedHashMap<>();
		if (body != null) {
			merged.putAll(body);
		}
		if (orderIdParam != null) {
			String t = orderIdParam.trim();
			if (!t.isEmpty() && !"0".equals(t)) {
				merged.put("order_id", t);
			}
		}
		if (statusParam != null) {
			merged.put("status", statusParam);
		}
		Map<String, Object> data =
				adminOrderSetInvoicedService.setInvoiced(companyId, operatorType, operatorId, merged);
		return ApiResult.ok(data);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "order.writeoff.info.get")
	@GetMapping(value = "/writeoff/{order_id}", name = "自提核销信息")
	public ApiResult<Map<String, Object>> getOrderWriteoffInfo(
			HttpServletRequest request, @PathVariable("order_id") String orderIdPath) {
		long companyId = readCompanyIdFromJwt(request);
		long orderId = parseWriteoffOrderIdOrNotFound(orderIdPath);
		return ApiResult.ok(adminOrderWriteoffService.getOrderWriteoffInfo(companyId, orderId));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "order.writeoff.set")
	@PostMapping(value = "/writeoff/{order_id}", name = "自提核销", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> orderWriteoff(
			HttpServletRequest request,
			@PathVariable("order_id") String orderIdPath,
			@RequestParam(name = "pickupcode", required = false) String pickupcodeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromJwt(request);
		long operatorId = readOperatorIdFromJwt(request);
		long orderId = parseWriteoffOrderIdOrNotFound(orderIdPath);
		String mergedPickup = mergeWriteoffPickupcode(pickupcodeParam, body);
		Map<String, Object> data =
				adminOrderWriteoffService.orderWriteoff(companyId, orderId, operatorId, mergedPickup);
		return ApiResult.ok(data);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "order.writeoff.qr.set")
	@PostMapping(value = "/qr_writeoff", name = "扫码核销", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> orderWriteoffQR(
			HttpServletRequest request,
			@RequestParam(name = "code", required = false) String codeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromJwt(request);
		long operatorId = readOperatorIdFromJwt(request);
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		List<Long> shopIds = List.of();
		List<Long> distributorIds = List.of();
		if (attr instanceof Map<?, ?> jwt) {
			shopIds = extractOperatorShopIds(jwt.get("shop_ids"));
			distributorIds = extractOperatorDistributorIds(jwt.get("distributor_ids"));
		}
		Map<String, Object> merged = new LinkedHashMap<>();
		if (body != null) {
			merged.putAll(body);
		}
		if (codeParam != null) {
			String t = codeParam.trim();
			if (StringUtils.hasText(t) && !"0".equals(t)) {
				merged.put("code", t);
			}
		}
		Object rawCode = merged.get("code");
		String code = requireNonEmptyQrCode(rawCode);
		Map<String, Object> data =
				adminOrderQrWriteoffService.orderWriteoffQR(
						companyId, operatorId, shopIds, distributorIds, code);
		return ApiResult.ok(data);
	}

	private static String requireNonEmptyQrCode(Object rawCode) {
		if (rawCode == null) {
			throw new ResourceException("code参数必填");
		}
		if (rawCode instanceof java.util.Collection<?>) {
			throw new ResourceException("code参数必填");
		}
		if (rawCode instanceof Map<?, ?>) {
			throw new ResourceException("code参数必填");
		}
		if (rawCode.getClass().isArray()) {
			throw new ResourceException("code参数必填");
		}
		if (rawCode instanceof Boolean) {
			throw new ResourceException("code参数必填");
		}
		String s;
		if (rawCode instanceof CharSequence) {
			s = rawCode.toString().trim();
		} else if (rawCode instanceof Number) {
			s = Long.toString(((Number) rawCode).longValue());
		} else {
			throw new ResourceException("code参数必填");
		}
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			throw new ResourceException("code参数必填");
		}
		return s;
	}

	private static List<Long> extractOperatorShopIds(Object raw) {
		return extractIdsFromNestedList(raw, "shop_id");
	}

	private static List<Long> extractOperatorDistributorIds(Object raw) {
		return extractIdsFromNestedList(raw, "distributor_id");
	}

	private static List<Long> extractIdsFromNestedList(Object raw, String key) {
		List<Long> out = new ArrayList<>();
		if (!(raw instanceof List<?> list)) {
			return out;
		}
		for (Object row : list) {
			if (row instanceof Map<?, ?> m) {
				Object id = m.get(key);
				if (id == null) {
					continue;
				}
				try {
					out.add(Long.parseLong(String.valueOf(id).trim()));
				} catch (NumberFormatException ignored) {
				}
			} else if (row instanceof Number n) {
				out.add(n.longValue());
			}
		}
		return out;
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "order.businessreceipt")
	@PostMapping(value = "/businessreceipt/{orderId}", name = "达达商家接单")
	public ApiResult<Map<String, Object>> businessReceipt(
			HttpServletRequest request, @PathVariable("orderId") String orderId) {
		String raw = orderId == null ? "" : orderId.trim();
		long companyId = readCompanyIdFromJwt(request);
		long operatorId = readOperatorIdFromJwt(request);
		localDeliveryBusinessReceiptService.businessReceipt(companyId, raw, operatorId);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ApiResult.ok(data);
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

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "order.confirm.goods")
	@PostMapping(value = "/confirm/goods/{orderId}", name = "达达确认退回")
	public ApiResult<Map<String, Object>> confirmGoods(
			HttpServletRequest request, @PathVariable("orderId") String orderId) {
		String raw = orderId == null ? "" : orderId.trim();
		long companyId = readCompanyIdFromJwt(request);
		localDeliveryConfirmGoodsService.confirmGoods(companyId, raw);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ApiResult.ok(data);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@DataPass
	@Activated(routeAlias = "order.markdown")
	@PostMapping(value = "/order/markdown", name = "订单改价", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> markDown(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		String orderIdRaw = firstNonBlankOrderId(null, merged);
		if (orderIdRaw == null) {
			throw new BadRequestException("订单号必填");
		}
		merged.put("order_id", orderIdRaw);
		long companyId = readCompanyIdFromJwt(request);
		Map<String, Object> data = adminOrderMarkDownService.markDown(companyId, merged);
		return ApiResult.ok(data);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@DataPass
	@Activated(routeAlias = "order.markdown.confirm")
	@PostMapping(value = "/order/markdown/confirm", name = "改价确认", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> confirmMarkDown(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		String orderIdRaw = firstNonBlankOrderId(null, merged);
		if (orderIdRaw == null) {
			throw new BadRequestException("订单号必填");
		}
		merged.put("order_id", orderIdRaw);
		long companyId = readCompanyIdFromJwt(request);
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		String operatorType = "";
		if (attr instanceof Map<?, ?> jwt) {
			operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(jwt.get("operator_type"));
		}
		long operatorId = readOperatorIdFromJwt(request);
		Map<String, Object> data =
				adminOrderMarkDownService.confirmMarkDown(
						companyId, operatorId, operatorType == null ? "" : operatorType, merged);
		return ApiResult.ok(data);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "order.deliverypackag.confirm")
	@PostMapping(
			value = "/order/deliverypackag/confirm",
			name = "打包确认",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> confirmDeliveryPackag(
			HttpServletRequest request,
			@RequestParam(name = "order_id", required = false) String orderIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		String orderIdRaw = resolveOrderIdForAdminCancelDeliveryStaff(merged);
		long companyId = readCompanyIdFromJwt(request);
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		String operatorType = "";
		if (attr instanceof Map<?, ?> jwt) {
			operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(jwt.get("operator_type"));
		}
		long operatorId = readOperatorIdFromJwt(request);
		adminConfirmDeliveryPackagService.confirmDeliveryPackag(
				companyId, operatorType == null ? "" : operatorType, operatorId, orderIdRaw);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ApiResult.ok(data);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "order.deliverystaff.confirm")
	@PostMapping(
			value = "/order/deliverystaff/confirm",
			name = "自配送员确认",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> confirmDeliveryStaff(
			HttpServletRequest request,
			@RequestParam(name = "order_id", required = false) String orderIdParam,
			@RequestParam(name = "self_delivery_operator_id", required = false)
					String selfDeliveryOperatorIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		String orderIdRaw = firstNonBlankOrderId(orderIdParam, body);
		if (orderIdRaw == null) {
			throw new BadRequestException("订单号必填");
		}
		String opRaw = firstNonBlankSelfDeliveryOperatorId(selfDeliveryOperatorIdParam, body);
		if (opRaw == null) {
			throw new BadRequestException("必须选择一个配送员");
		}
		long selfDeliveryOpId;
		try {
			selfDeliveryOpId = Long.parseLong(opRaw);
		} catch (NumberFormatException e) {
			throw new BadRequestException("必须选择一个配送员");
		}
		long companyId = readCompanyIdFromJwt(request);
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		String operatorType = "";
		if (attr instanceof Map<?, ?> jwt) {
			operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(jwt.get("operator_type"));
		}
		long operatorId = readOperatorIdFromJwt(request);
		adminConfirmDeliveryStaffService.confirmDeliveryStaff(
				companyId,
				operatorType == null ? "" : operatorType,
				operatorId,
				orderIdRaw,
				selfDeliveryOpId);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ApiResult.ok(data);
	}

	private static String firstNonBlankSelfDeliveryOperatorId(
			String param, Map<String, Object> body) {
		if (param != null) {
			String t = param.trim();
			if (!t.isEmpty()) {
				return t;
			}
		}
		if (body == null || !body.containsKey("self_delivery_operator_id")) {
			return null;
		}
		Object v = body.get("self_delivery_operator_id");
		if (v == null) {
			return null;
		}
		String t = String.valueOf(v).trim();
		if (t.isEmpty()) {
			return null;
		}
		return t;
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "order.deliverystaff.cancel")
	@PostMapping(
			value = "/order/cancel/deliverystaff",
			name = "自配送取消",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> cancelDeliveryStaff(
			HttpServletRequest request,
			@RequestParam(name = "order_id", required = false) String orderIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		String orderIdRaw = resolveOrderIdForAdminCancelDeliveryStaff(merged);
		long companyId = readCompanyIdFromJwt(request);
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		String operatorType = "";
		if (attr instanceof Map<?, ?> jwt) {
			operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(jwt.get("operator_type"));
		}
		long operatorId = readOperatorIdFromJwt(request);
		adminCancelDeliveryStaffService.cancelDeliveryStaff(
				companyId, operatorType == null ? "" : operatorType, operatorId, orderIdRaw);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ApiResult.ok(data);
	}
}
