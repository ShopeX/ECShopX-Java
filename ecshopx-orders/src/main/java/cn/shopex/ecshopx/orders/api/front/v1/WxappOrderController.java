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

package cn.shopex.ecshopx.orders.api.front.v1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.web.WxappMemberAuthAttributes;
import cn.shopex.ecshopx.orders.service.epidemic.OrderEpidemicRegisterFrontDelService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappOfflinePaymentUpdateVoucherService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappOfflinePaymentUploadVoucherService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappOrderBindUserOrderService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappOrderCancelService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappOrderCreateService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappOrderDeliveryService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappOrderFreightFeeService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappOrderJsPayConfigService;
import cn.shopex.ecshopx.orders.service.admin.AdminCancelDeliveryStaffService;
import cn.shopex.ecshopx.orders.service.admin.AdminConfirmDeliveryPackagService;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderUpdateDeliveryService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappNormalOrderConfirmReceiptService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappOrderDetailNewService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappOrderDetailService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappEpidemicRegisterInfoService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappEpidemicRegisterMixedCatsService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappOrderCountOrderAndRightsLogService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappOrderCountOrdersService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappGroupOrderDetailService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappGroupOrderListQuery;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappGroupOrderListService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappOrderListService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappOrderOfflineBackaccountService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappOrderPickupCodeService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappOrderParamMergeSupport;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappOrderTrackerPullService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappOrderZitiQrCodeResult;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappOrderZitiQrCodeService;
import jakarta.servlet.http.HttpServletRequest;

/**
 * H5 / wxapp-facing order HTTP endpoints.
 *
 * <p><b>{@code POST /order/delivery} (self-delivery shipment)</b>: delegates to
 * {@link cn.shopex.ecshopx.orders.service.front.wxapp.WxappOrderDeliveryService#delivery(jakarta.servlet.http.HttpServletRequest, java.util.Map, java.util.Map)},
 * which merges tenant and member context and runs the same physical delivery core as admin. Order process log (OPL)
 * dispatch is a single publication on that domain path via {@link cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort};
 * this controller does not publish OPL itself. Method-level {@link #delivery} documents the same contract.
 *
 * <p>To re-verify the shared core publish and async consume envelope for admin-shaped payloads, from the
 * {@code ecshopx-java} directory run:
 *
 * <pre>
 * mvn -pl ecshopx-orders -Dtest=AdminOrderDeliveryOrderProcessLogDispatchPublishProbeTest,AdminApiV1OrderDeliveryOrderProcessLogEventAsyncConsumeTest test
 * </pre>
 *
 * <p><b>Merchant vs courier for {@code /order/updateDelivery/{delivery_id}}</b>: this endpoint delegates to
 * {@link cn.shopex.ecshopx.orders.service.admin.AdminOrderUpdateDeliveryService#updateDelivery(long, long, String, Map)}
 * only. Order process log (OPL) dispatch is never invoked from this controller.
 *
 * <ul>
 *   <li><b>Merchant branch</b> — when the order {@code receipt_type} is {@code merchant}, OPL is emitted inside
 *       {@link cn.shopex.ecshopx.orders.service.admin.AdminOrderUpdateDeliveryService} on that branch through the
 *       shared publish port.</li>
 *   <li><b>Courier branch</b> — when the order is not merchant self-delivery, OPL after the courier update path is
 *       emitted only from that same service on the courier branch through the same shared port.</li>
 * </ul>
 *
 * <p>Wxapp passes operator id {@code 0L} (no back-office operator on this channel), {@code operator_type=admin},
 * and merged request fields including {@code orders_delivery_id}, {@code company_id}, {@code delivery_corp}, and
 * {@code delivery_code}. Payload shape for the courier case is covered by the courier-branch tests below; the
 * probe test uses a non-zero operator id only as a Mockito stub, not as a production Wxapp value.
 *
 * <p>To re-verify dispatch for admin update-delivery (merchant and courier probe plus async consume), from the
 * {@code ecshopx-java} directory run:
 *
 * <pre>
 * mvn -pl ecshopx-orders -Dtest=AdminOrderUpdateDeliveryMerchantBranchOrderProcessLogDispatchPublishProbeTest,AdminOrderUpdateDeliveryMerchantBranchOrderProcessLogDispatchAsyncConsumeTest,AdminOrderUpdateDeliveryCourierBranchOrderProcessLogDispatchPublishProbeTest,AdminOrderUpdateDeliveryCourierBranchOrderProcessLogDispatchAsyncConsumeTest test
 * </pre>
 */
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@RestController("ordersFrontV1WxappOrder")
@RequestMapping("/api/v1/h5app/wxapp")
public class WxappOrderController {

	/**
	 * Wxapp {@code /order/cancel/deliverystaff} passes no storefront operator; service maps blank type to OPL {@code
	 * operator_type = system} and {@code operator_id = 0}.
	 */
	private static final String WXAPP_CANCEL_DELIVERY_STAFF_OPERATOR_TYPE = "";

	private static final long WXAPP_CANCEL_DELIVERY_STAFF_OPERATOR_ID = 0L;

	/**
	 * Wxapp {@code POST /order/deliverypackag/confirm} has no back-office operator; {@link
	 * AdminConfirmDeliveryPackagService} maps blank {@code operator_type} to {@code system} and keeps {@code
	 * operator_id} at {@code 0} for order-process log payloads.
	 */
	private static final String WXAPP_CONFIRM_DELIVERY_PACKAG_OPERATOR_TYPE = "";

	private static final long WXAPP_CONFIRM_DELIVERY_PACKAG_OPERATOR_ID = 0L;

	private final OrderEpidemicRegisterFrontDelService orderEpidemicRegisterFrontDelService;

	private final WxappOfflinePaymentUploadVoucherService wxappOfflinePaymentUploadVoucherService;

	private final WxappOfflinePaymentUpdateVoucherService wxappOfflinePaymentUpdateVoucherService;

	private final WxappOrderBindUserOrderService wxappOrderBindUserOrderService;

	private final WxappOrderCreateService wxappOrderCreateService;

	private final WxappOrderFreightFeeService wxappOrderFreightFeeService;

	private final WxappOrderCancelService wxappOrderCancelService;

	private final AdminCancelDeliveryStaffService adminCancelDeliveryStaffService;

	private final AdminConfirmDeliveryPackagService adminConfirmDeliveryPackagService;

	private final WxappNormalOrderConfirmReceiptService wxappNormalOrderConfirmReceiptService;

	private final WxappOrderDeliveryService wxappOrderDeliveryService;

	private final WxappOrderJsPayConfigService wxappOrderJsPayConfigService;

	private final AdminOrderUpdateDeliveryService adminOrderUpdateDeliveryService;

	private final WxappOrderDetailService wxappOrderDetailService;

	private final WxappOrderDetailNewService wxappOrderDetailNewService;

	private final WxappOrderListService wxappOrderListService;

	private final WxappOrderPickupCodeService wxappOrderPickupCodeService;

	private final WxappOrderCountOrderAndRightsLogService wxappOrderCountOrderAndRightsLogService;

	private final WxappEpidemicRegisterInfoService wxappEpidemicRegisterInfoService;

	private final WxappEpidemicRegisterMixedCatsService wxappEpidemicRegisterMixedCatsService;

	private final WxappOrderCountOrdersService wxappOrderCountOrdersService;

	private final WxappGroupOrderListService wxappGroupOrderListService;

	private final WxappGroupOrderDetailService wxappGroupOrderDetailService;

	private final WxappOrderOfflineBackaccountService wxappOrderOfflineBackaccountService;

	private final WxappOrderTrackerPullService wxappOrderTrackerPullService;

	private final WxappOrderZitiQrCodeService wxappOrderZitiQrCodeService;

	public WxappOrderController(
			OrderEpidemicRegisterFrontDelService orderEpidemicRegisterFrontDelService,
			WxappOfflinePaymentUploadVoucherService wxappOfflinePaymentUploadVoucherService,
			WxappOfflinePaymentUpdateVoucherService wxappOfflinePaymentUpdateVoucherService,
			WxappOrderBindUserOrderService wxappOrderBindUserOrderService,
			WxappOrderCreateService wxappOrderCreateService,
			WxappOrderFreightFeeService wxappOrderFreightFeeService,
			WxappOrderCancelService wxappOrderCancelService,
			AdminCancelDeliveryStaffService adminCancelDeliveryStaffService,
			AdminConfirmDeliveryPackagService adminConfirmDeliveryPackagService,
			WxappNormalOrderConfirmReceiptService wxappNormalOrderConfirmReceiptService,
			WxappOrderDeliveryService wxappOrderDeliveryService,
			WxappOrderJsPayConfigService wxappOrderJsPayConfigService,
			AdminOrderUpdateDeliveryService adminOrderUpdateDeliveryService,
			WxappOrderDetailService wxappOrderDetailService,
			WxappOrderDetailNewService wxappOrderDetailNewService,
			WxappOrderListService wxappOrderListService,
			WxappOrderPickupCodeService wxappOrderPickupCodeService,
			WxappOrderCountOrderAndRightsLogService wxappOrderCountOrderAndRightsLogService,
			WxappEpidemicRegisterInfoService wxappEpidemicRegisterInfoService,
			WxappEpidemicRegisterMixedCatsService wxappEpidemicRegisterMixedCatsService,
			WxappOrderCountOrdersService wxappOrderCountOrdersService,
			WxappGroupOrderListService wxappGroupOrderListService,
			WxappGroupOrderDetailService wxappGroupOrderDetailService,
			WxappOrderOfflineBackaccountService wxappOrderOfflineBackaccountService,
			WxappOrderTrackerPullService wxappOrderTrackerPullService,
			WxappOrderZitiQrCodeService wxappOrderZitiQrCodeService) {
		this.orderEpidemicRegisterFrontDelService = orderEpidemicRegisterFrontDelService;
		this.wxappOfflinePaymentUploadVoucherService = wxappOfflinePaymentUploadVoucherService;
		this.wxappOfflinePaymentUpdateVoucherService = wxappOfflinePaymentUpdateVoucherService;
		this.wxappOrderBindUserOrderService = wxappOrderBindUserOrderService;
		this.wxappOrderCreateService = wxappOrderCreateService;
		this.wxappOrderFreightFeeService = wxappOrderFreightFeeService;
		this.wxappOrderCancelService = wxappOrderCancelService;
		this.adminCancelDeliveryStaffService = adminCancelDeliveryStaffService;
		this.adminConfirmDeliveryPackagService = adminConfirmDeliveryPackagService;
		this.wxappNormalOrderConfirmReceiptService = wxappNormalOrderConfirmReceiptService;
		this.wxappOrderDeliveryService = wxappOrderDeliveryService;
		this.wxappOrderJsPayConfigService = wxappOrderJsPayConfigService;
		this.adminOrderUpdateDeliveryService = adminOrderUpdateDeliveryService;
		this.wxappOrderDetailService = wxappOrderDetailService;
		this.wxappOrderDetailNewService = wxappOrderDetailNewService;
		this.wxappOrderListService = wxappOrderListService;
		this.wxappOrderPickupCodeService = wxappOrderPickupCodeService;
		this.wxappOrderCountOrderAndRightsLogService = wxappOrderCountOrderAndRightsLogService;
		this.wxappEpidemicRegisterInfoService = wxappEpidemicRegisterInfoService;
		this.wxappEpidemicRegisterMixedCatsService = wxappEpidemicRegisterMixedCatsService;
		this.wxappOrderCountOrdersService = wxappOrderCountOrdersService;
		this.wxappGroupOrderListService = wxappGroupOrderListService;
		this.wxappGroupOrderDetailService = wxappGroupOrderDetailService;
		this.wxappOrderOfflineBackaccountService = wxappOrderOfflineBackaccountService;
		this.wxappOrderTrackerPullService = wxappOrderTrackerPullService;
		this.wxappOrderZitiQrCodeService = wxappOrderZitiQrCodeService;
	}

	@FrontAuth
	@PostMapping(
			value = "/order/jspayconfig",
			name = "Js支付配置",
			consumes = {
				MediaType.APPLICATION_JSON_VALUE,
				MediaType.MULTIPART_FORM_DATA_VALUE,
				MediaType.APPLICATION_FORM_URLENCODED_VALUE
			},
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getJsPayConfig(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		long companyId = longVal(auth.get("company_id"));
		if (companyId <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		String pageUrl = merged.get("url") == null ? "" : String.valueOf(merged.get("url")).trim();
		Map<String, Object> data = wxappOrderJsPayConfigService.getJsPayConfig(companyId, pageUrl);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@PostMapping(
			value = "/order",
			name = "创建订单支付",
			consumes = {
				MediaType.APPLICATION_JSON_VALUE,
				MediaType.MULTIPART_FORM_DATA_VALUE,
				MediaType.APPLICATION_FORM_URLENCODED_VALUE
			},
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> createOrder(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Map<String, Object> data = wxappOrderCreateService.createOrder(request, merged, auth);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@GetMapping(value = "/epidemic/info", name = "疫情登记信息", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> epidemicRegisterInfo(HttpServletRequest request) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		long companyId = longVal(auth.get("company_id"));
		long userId = longVal(auth.get("user_id"));
		if (companyId <= 0L || userId <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> data = wxappEpidemicRegisterInfoService.epidemicRegisterInfo(companyId, userId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@GetMapping(value = "/epidemic/mixed/cat", name = "疫情登记类目", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> epidemicRegisterMixedCats() {
		Map<String, Object> data = wxappEpidemicRegisterMixedCatsService.epidemicRegisterMixedCats();
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@PostMapping(value = "/epidemic/info/del/{id}", name = "删除疫情登记", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> delEpidemicRegister(@PathVariable("id") String id) {
		orderEpidemicRegisterFrontDelService.delEpidemicRegister(id);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@DataPass
	@GetMapping(value = "/order/{order_id}", name = "订单详情", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getOrderDetail(
			HttpServletRequest request,
			@PathVariable("order_id") String orderId,
			@RequestParam(name = "invoice_list", required = false, defaultValue = "0") String invoiceList,
			@RequestParam(name = "from", required = false, defaultValue = "front_list") String from,
			@RequestParam(name = "promoter_user_id", required = false, defaultValue = "") String promoterUserId,
			@RequestParam(name = "isSalesmanPage", required = false, defaultValue = "") String isSalesmanPage,
			@RequestParam(name = "prescription_order_random", required = false, defaultValue = "")
					String prescriptionOrderRandom) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> paramMap = FlexibleHttpServletParameterMap.toObjectMap(request);
		List<String> selfDeliveryOperatorIdParam = selfDeliveryOperatorIdsFromRaw(paramMap.get("self_delivery_operator_id"));
		Object data =
				wxappOrderDetailService.getOrderDetail(
						request,
						auth,
						orderId,
						invoiceList,
						from,
						promoterUserId,
						isSalesmanPage,
						selfDeliveryOperatorIdParam,
						prescriptionOrderRandom);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static List<String> selfDeliveryOperatorIdsFromRaw(Object raw) {
		if (raw == null) {
			return Collections.emptyList();
		}
		if (raw instanceof String s) {
			if (s.trim().isEmpty()) {
				return Collections.emptyList();
			}
			return List.of(s.trim());
		}
		if (raw instanceof List<?> list) {
			List<String> out = new ArrayList<>();
			for (Object e : list) {
				String t = String.valueOf(e).trim();
				if (!t.isEmpty()) {
					out.add(t);
				}
			}
			return out;
		}
		return Collections.emptyList();
	}

	@FrontAuth
	@GetMapping(value = "/order_new/{order_id}", name = "订单详情新", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getOrderDetailNew(
			HttpServletRequest request, @PathVariable("order_id") String orderId) {
		Map<String, Object> auth = new LinkedHashMap<>();
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (rawAuth instanceof Map<?, ?> authRaw) {
			for (Map.Entry<?, ?> e : authRaw.entrySet()) {
				auth.put(String.valueOf(e.getKey()), e.getValue());
			}
		}
		Object data = wxappOrderDetailNewService.getOrderDetailNew(auth, orderId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@GetMapping(value = "/orders", name = "订单列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getOrderList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String page,
			@RequestParam(name = "pageSize", required = false) String pageSize,
			@RequestParam(name = "from", required = false) String from,
			@RequestParam(name = "time_start_begin", required = false) String timeStartBegin,
			@RequestParam(name = "time_start_end", required = false) String timeStartEnd,
			@RequestParam(name = "order_id", required = false) String orderId,
			@RequestParam(name = "mobile", required = false) String mobile,
			@RequestParam(name = "activity_id", required = false) String activityId,
			@RequestParam(name = "order_type", required = false) String orderType,
			@RequestParam(name = "invoice_list", required = false) String invoiceList,
			@RequestParam(name = "order_class", required = false) String orderClass,
			@RequestParam(name = "order_status", required = false) String orderStatus,
			@RequestParam(name = "delivery_status", required = false) String deliveryStatus,
			@RequestParam(name = "self_delivery_status", required = false) String selfDeliveryStatus,
			@RequestParam(name = "self_delivery_operator_id", required = false) String selfDeliveryOperatorId,
			@RequestParam(name = "status", required = false) String status,
			@RequestParam(name = "is_distribution", required = false) String isDistribution,
			@RequestParam(name = "distributor_id", required = false) String distributorId,
			@RequestParam(name = "promoter_user_id", required = false) String promoterUserId,
			@RequestParam(name = "isSalesmanPage", required = false) String isSalesmanPage,
			@RequestParam(name = "is_rate", required = false) String isRate) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		Object data = wxappOrderListService.getOrderList(request, auth);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@GetMapping(value = "/groupOrders", name = "拼团订单列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getGroupOrderList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String page,
			@RequestParam(name = "pageSize", required = false) String pageSize,
			@RequestParam(name = "team_status", required = false) String teamStatus,
			@RequestParam(name = "group_goods_type", required = false) String groupGoodsType) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		long userId = longVal(auth.get("user_id"));
		long companyId = longVal(auth.get("company_id"));
		boolean teamStatusKeyPresent = request.getParameterMap().containsKey("team_status");
		boolean groupGoodsTypeKeyPresent = request.getParameterMap().containsKey("group_goods_type");
		Map<String, Object> data =
				wxappGroupOrderListService.getGroupOrderList(
						new WxappGroupOrderListQuery(
								userId,
								companyId,
								page,
								pageSize,
								teamStatusKeyPresent,
								teamStatus,
								groupGoodsTypeKeyPresent,
								groupGoodsType));
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@GetMapping(value = "/orders/count", name = "订单权益统计", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> countOrderAndRightsLog(
			HttpServletRequest request,
			@RequestParam(name = "order_type", required = false) String orderType) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		boolean orderTypeKeyPresent = request.getParameterMap().containsKey("order_type");
		Map<String, Object> data =
				wxappOrderCountOrderAndRightsLogService.countOrderAndRightsLog(auth, orderType, orderTypeKeyPresent);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@GetMapping(value = "/orderscount", name = "订单数量", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> countOrders(HttpServletRequest request) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> data = wxappOrderCountOrdersService.countOrders(request, auth);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@GetMapping(value = "/trackerpull", name = "物流跟踪", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> trackerpull(
			HttpServletRequest request,
			@RequestParam(value = "order_type", required = false) String orderType,
			@RequestParam(value = "order_id", required = false) String orderId) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		Object data = wxappOrderTrackerPullService.trackerpull(auth, orderType, orderId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	/**
	 * Cancels a normal order for the authenticated wxapp buyer or eligible group chief.
	 *
	 * <p>Delegation: {@link cn.shopex.ecshopx.orders.service.front.wxapp.WxappOrderCancelService#cancelOrder}.
	 * When shipment is still {@code PENDING}, cancellation is the full normal-order path through
	 * {@link cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderFullCancelService#execute(long, java.lang.String, long, long, long, java.lang.String, long, java.lang.String, java.util.Map, java.lang.String)}.
	 * On the paid ({@code PAYED}) branch that service registers a single trade-refund publication to run after
	 * transaction commit via {@link cn.shopex.ecshopx.common.dispatch.TradeRefundDispatchPublisher#publish},
	 * matching the same post-commit dispatch contract as the admin full-cancel flow. Non-{@code PENDING} shipment
	 * uses partial cancellation and does not follow that full-cancel contract.
	 */
	@FrontAuth
	@PostMapping(
			value = "/order/cancel",
			name = "取消订单",
			consumes = {
				MediaType.APPLICATION_JSON_VALUE,
				MediaType.MULTIPART_FORM_DATA_VALUE,
				MediaType.APPLICATION_FORM_URLENCODED_VALUE
			},
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> cancelOrder(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Map<String, Object> data = wxappOrderCancelService.cancelOrder(request, merged, auth);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@PostMapping(
			value = "/order/confirmReceipt",
			name = "确认收货",
			consumes = {
				MediaType.APPLICATION_JSON_VALUE,
				MediaType.MULTIPART_FORM_DATA_VALUE,
				MediaType.APPLICATION_FORM_URLENCODED_VALUE
			},
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> confirmReceipt(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Map<String, Object> data = wxappNormalOrderConfirmReceiptService.confirmReceipt(request, merged, auth);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@GetMapping(value = "/ziticode", name = "自提码", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getZitiQRCode(
			HttpServletRequest request,
			@RequestParam(value = "order_type", defaultValue = "normal") String orderType,
			@RequestParam(value = "order_id", required = false) String orderId) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		long companyId = longVal(auth.get("company_id"));
		if (companyId <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		Object jwtUserId = auth.get("user_id");
		WxappOrderZitiQrCodeResult r =
				wxappOrderZitiQrCodeService.getZitiQRCode(companyId, jwtUserId, orderType, orderId);
		if (r.emptyMask()) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		return ResponseEntity.ok(ApiResult.ok(r.dataIfShown()));
	}

	@FrontAuth
	@PostMapping(
			value = "/order/bind/{order_id}",
			name = "绑定订单",
			consumes = {
				MediaType.APPLICATION_JSON_VALUE,
				MediaType.MULTIPART_FORM_DATA_VALUE,
				MediaType.APPLICATION_FORM_URLENCODED_VALUE
			},
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> bindUserOrder(
			HttpServletRequest request,
			@PathVariable("order_id") String orderId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		LinkedHashMap<String, Object> params = WxappOrderParamMergeSupport.applyDefaultsAndAuth(merged, auth);
		long companyId = longVal(params.get("company_id"));
		long jwtUserId = longVal(params.get("user_id"));
		if (companyId <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		if (jwtUserId <= 0L) {
			throw new ResourceException("还未授权，请授权手机号！");
		}

		if (orderId == null || orderId.isBlank() || "0".equals(orderId.trim())) {
			throw new BadRequestException("订单号必填");
		}
		Object authCodeRaw = merged.get("auth_code");
		String authCode = authCodeRaw == null ? "" : String.valueOf(authCodeRaw).trim();
		if (authCode.isBlank() || "0".equals(authCode)) {
			throw new BadRequestException("小票验证码必填");
		}

		wxappOrderBindUserOrderService.bindUserOrder(companyId, jwtUserId, orderId.trim(), authCode);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String resolveOrderIdForCancelDeliveryStaff(Map<String, Object> merged) {
		if (merged == null) {
			throw new ResourceException("订单号必填");
		}
		if (!merged.containsKey("order_id")) {
			throw new ResourceException("订单号必填");
		}
		Object v = merged.get("order_id");
		if (v == null) {
			throw new ResourceException("订单号必填");
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty()) {
			throw new ResourceException("订单号必填");
		}
		return s;
	}

	@FrontAuth
	/**
	 * Wxapp self-delivery: {@code POST /api/v1/h5app/wxapp/order/delivery} with an authenticated member session.
	 *
	 * @apiNote Order process log publication is handled only inside {@link WxappOrderDeliveryService#delivery} and its
	 * downstream delivery pipeline (via {@link cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort}), which
	 * runs the same physical delivery core as
	 * {@link cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDeliveryCoreService#deliveryNormalPhysical}; this
	 * controller method must not invoke a second publish for the same shipment. Compare
	 * {@link cn.shopex.ecshopx.orders.api.admin.v1.OrderController#delivery}.
	 * <p>This Spring route resolves {@code company_id} from the authenticated member context (JWT or H5 session claims
	 * on the request), which {@link WxappOrderDeliveryService#delivery} merges into the request map before
	 * {@link cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDeliveryCoreService#deliveryNormalPhysical}. The
	 * normal-order delivery dispatch event uses a two-key payload only ({@code order_id}, {@code company_id}).
	 * {@link cn.shopex.ecshopx.dispatch.DispatchMessage} literals and the marketing fan-out envelope for this channel
	 * are regression-locked in
	 * {@link cn.shopex.ecshopx.orders.service.front.wxapp.WxappOrderDeliveryServiceNormalOrderDeliveryTriggerProbeTest}.</p>
	 * <p>That same probe class also locks the third-party CRM normal-order-delivery child dispatch envelope and drives
	 * {@link cn.shopex.ecshopx.dispatch.DispatchConsumerRuntime#consume} against it in-process.</p>
	 * <p>{@code delivery_whenNormalPhysical_thenPlanSection6DmCrmChildDispatchMessageLiteralsAndWxappTraceMatchAndDispatchConsumerRuntimeConsumes_event237_entry02}
	 * in {@link cn.shopex.ecshopx.orders.service.front.wxapp.WxappOrderDeliveryServiceNormalOrderDeliveryTriggerProbeTest}
	 * regression-locks the two-key parent {@link cn.shopex.ecshopx.dispatch.DispatchMessage} template, the DmCrm child
	 * envelope, and in-process {@link cn.shopex.ecshopx.dispatch.DispatchConsumerRuntime#consume} for entry-02
	 * ({@code event237}).</p>
	 */
	@PostMapping(
			value = "/order/delivery",
			name = "自配送发货",
			consumes = {
				MediaType.APPLICATION_JSON_VALUE,
				MediaType.MULTIPART_FORM_DATA_VALUE,
				MediaType.APPLICATION_FORM_URLENCODED_VALUE
			},
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> delivery(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Map<String, Object> data = wxappOrderDeliveryService.delivery(request, merged, auth);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@PostMapping(
			value = "/order/updateDelivery/{delivery_id}",
			name = "更新自配送",
			consumes = {
				MediaType.APPLICATION_JSON_VALUE,
				MediaType.MULTIPART_FORM_DATA_VALUE,
				MediaType.APPLICATION_FORM_URLENCODED_VALUE
			},
			produces = MediaType.APPLICATION_JSON_VALUE)
	/**
	 * @apiNote OPL dispatch and merchant vs courier routing are documented on {@link WxappOrderController}; this
	 *           method only delegates to {@link AdminOrderUpdateDeliveryService}.
	 */
	public ResponseEntity<ApiResult<Map<String, Object>>> updateDelivery(
			HttpServletRequest request,
			@PathVariable("delivery_id") String deliveryId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		long companyId = longVal(auth.get("company_id"));
		if (companyId <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		String pathId = deliveryId == null ? "" : deliveryId.trim();
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		requireDeliveryCorpAndCodeForUpdate(merged);
		merged.put("orders_delivery_id", pathId);
		merged.put("company_id", companyId);
		merged.put("operator_type", "admin");
		Map<String, Object> data = adminOrderUpdateDeliveryService.updateDelivery(companyId, 0L, pathId, merged);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static void requireDeliveryCorpAndCodeForUpdate(Map<String, Object> merged) {
		Object dc = merged.get("delivery_corp");
		if (dc == null || !StringUtils.hasText(String.valueOf(dc).trim())) {
			throw new ResourceException("物流公司编码必填");
		}
		Object code = merged.get("delivery_code");
		if (code == null || !StringUtils.hasText(String.valueOf(code).trim())) {
			throw new ResourceException("物流公司快递号必填");
		}
	}

	@FrontAuth
	@PostMapping(
			value = "/order/cancel/deliverystaff",
			name = "取消配送",
			consumes = {
				MediaType.APPLICATION_JSON_VALUE,
				MediaType.MULTIPART_FORM_DATA_VALUE,
				MediaType.APPLICATION_FORM_URLENCODED_VALUE
			},
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> cancelDeliveryStaff(
			HttpServletRequest request,
			@RequestParam(name = "order_id", required = false) String orderIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		long companyId = longVal(auth.get("company_id"));
		if (companyId <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		String orderIdRaw = resolveOrderIdForCancelDeliveryStaff(merged);
		adminCancelDeliveryStaffService.cancelDeliveryStaff(
				companyId,
				WXAPP_CANCEL_DELIVERY_STAFF_OPERATOR_TYPE,
				WXAPP_CANCEL_DELIVERY_STAFF_OPERATOR_ID,
				orderIdRaw);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	/**
	 * Confirms self-delivery packaged status for the tenant order. Delegates to {@link
	 * AdminConfirmDeliveryPackagService#confirmDeliveryPackag(long, String, long, String)} with wxapp-shaped
	 * operator placeholders; order-process log publication stays inside that service via {@code
	 * OrderProcessLogPublishPort} (after commit), not from this controller.
	 */
	@FrontAuth
	@PostMapping(
			value = "/order/deliverypackag/confirm",
			name = "打包确认",
			consumes = {
				MediaType.APPLICATION_JSON_VALUE,
				MediaType.MULTIPART_FORM_DATA_VALUE,
				MediaType.APPLICATION_FORM_URLENCODED_VALUE
			},
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> confirmDeliveryPackag(
			HttpServletRequest request,
			@RequestParam(name = "order_id", required = false) String orderIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		long companyId = longVal(auth.get("company_id"));
		if (companyId <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		String orderIdRaw = resolveOrderIdForCancelDeliveryStaff(merged);
		adminConfirmDeliveryPackagService.confirmDeliveryPackag(
				companyId,
				WXAPP_CONFIRM_DELIVERY_PACKAG_OPERATOR_TYPE,
				WXAPP_CONFIRM_DELIVERY_PACKAG_OPERATOR_ID,
				orderIdRaw);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@GetMapping(
			value = "/order/offline/backaccount",
			name = "线下收款账户",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getOfflineAccount(
			HttpServletRequest request,
			@RequestParam(name = "country_code", required = false) String countryCode) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		long companyId = longVal(auth.get("company_id"));
		if (companyId <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> data = wxappOrderOfflineBackaccountService.getOfflineAccount(companyId, countryCode);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@PostMapping(
			value = "/order/offline/upload/voucher",
			name = "上传凭证",
			consumes = {
				MediaType.APPLICATION_JSON_VALUE,
				MediaType.MULTIPART_FORM_DATA_VALUE,
				MediaType.APPLICATION_FORM_URLENCODED_VALUE
			},
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> uploadOfflineVoucher(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Map<String, Object> data = wxappOfflinePaymentUploadVoucherService.uploadOfflineVoucher(request, merged, auth);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@GetMapping(
			value = "/order/offline/get/voucher",
			name = "获取凭证",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getOfflineVoucher(
			HttpServletRequest request,
			@RequestParam(value = "order_id", required = false) String orderIdParam) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		if (orderIdParam == null || orderIdParam.isBlank()) {
			throw new ResourceException("订单号缺少！");
		}
		long companyId = longVal(auth.get("company_id"));
		if (companyId <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		Object data = wxappOfflinePaymentUploadVoucherService.getOfflineVoucher(companyId, orderIdParam);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@PostMapping(
			value = "/order/offline/update/voucher",
			name = "修改凭证",
			consumes = {
				MediaType.APPLICATION_JSON_VALUE,
				MediaType.MULTIPART_FORM_DATA_VALUE,
				MediaType.APPLICATION_FORM_URLENCODED_VALUE
			},
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> updateOfflineVoucher(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Map<String, Object> data = wxappOfflinePaymentUpdateVoucherService.updateOfflineVoucher(request, merged, auth);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@GetMapping(value = "/pickupcode/{order_id}", name = "提货码", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getOrderPickupCode(
			HttpServletRequest request, @PathVariable("order_id") String orderId) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		long companyId = longVal(auth.get("company_id"));
		long jwtUserId = longVal(auth.get("user_id"));
		if (companyId <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		if (jwtUserId <= 0L) {
			throw new ResourceException("还未授权，请授权手机号！");
		}
		boolean ok = wxappOrderPickupCodeService.getOrderPickupCode(companyId, jwtUserId, orderId);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.valueOf(ok));
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontNoAuth
	@GetMapping(
			value = "/groupOrders/{teamId}",
			name = "拼团订单详情",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getGroupOrderDetail(
			HttpServletRequest request, @PathVariable("teamId") String teamId) {
		LinkedHashMap<String, Object> authMap;
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (!(rawAuth instanceof Map<?, ?>)) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (rawAuth instanceof Map<?, ?> authRaw) {
			authMap = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : authRaw.entrySet()) {
				authMap.put(String.valueOf(e.getKey()), e.getValue());
			}
		} else {
			authMap = new LinkedHashMap<>();
		}
		long requestCompanyId = parsePositiveLongOrZero(authMap.get("company_id"));
		if (requestCompanyId <= 0L) {
			requestCompanyId = parsePositiveLongOrZero(request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID));
			if (requestCompanyId > 0L) {
				authMap.put("company_id", requestCompanyId);
			}
		}
		if (parsePositiveLongOrZero(authMap.get("company_id")) <= 0L) {
			requestCompanyId =
					parsePositiveLongOrZero(FlexibleHttpServletParameterMap.toObjectMap(request).get("company_id"));
			if (requestCompanyId > 0L) {
				authMap.put("company_id", requestCompanyId);
			}
		}
		if (parsePositiveLongOrZero(authMap.get("company_id")) <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		requestCompanyId = parsePositiveLongOrZero(authMap.get("company_id"));
		Object data = wxappGroupOrderDetailService.getGroupOrderDetail(teamId, requestCompanyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontNoAuth
	@PostMapping(
			value = "/getFreightFee",
			name = "运费优惠信息",
			consumes = {
				MediaType.APPLICATION_JSON_VALUE,
				MediaType.MULTIPART_FORM_DATA_VALUE,
				MediaType.APPLICATION_FORM_URLENCODED_VALUE
			},
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getOrderFreightFeeInfo(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> authMap;
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (!(rawAuth instanceof Map<?, ?>)) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (rawAuth instanceof Map<?, ?> authRaw) {
			authMap = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : authRaw.entrySet()) {
				authMap.put(String.valueOf(e.getKey()), e.getValue());
			}
		} else {
			authMap = new LinkedHashMap<>();
		}
		long companyId = parsePositiveLongOrZero(authMap.get("company_id"));
		if (companyId <= 0L) {
			companyId = parsePositiveLongOrZero(request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID));
			if (companyId > 0L) {
				authMap.put("company_id", companyId);
			}
		}
		if (parsePositiveLongOrZero(authMap.get("company_id")) <= 0L) {
			companyId = parsePositiveLongOrZero(FlexibleHttpServletParameterMap.toObjectMap(request).get("company_id"));
			if (companyId > 0L) {
				authMap.put("company_id", companyId);
			}
		}
		if (parsePositiveLongOrZero(authMap.get("company_id")) <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		if (!authMap.containsKey("user_id") || authMap.get("user_id") == null) {
			authMap.put("user_id", 0L);
		}
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Map<String, Object> data = wxappOrderFreightFeeService.getOrderFreightFeeInfo(request, merged, authMap);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontNoAuth
	@PostMapping(
			value = "/order_new",
			name = "创建订单",
			consumes = {
				MediaType.APPLICATION_JSON_VALUE,
				MediaType.MULTIPART_FORM_DATA_VALUE,
				MediaType.APPLICATION_FORM_URLENCODED_VALUE
			},
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> createNewOrder(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> authMap;
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (raw instanceof Map<?, ?> authRaw) {
			authMap = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : authRaw.entrySet()) {
				authMap.put(String.valueOf(e.getKey()), e.getValue());
			}
		} else {
			authMap = new LinkedHashMap<>();
		}
		long companyId = parsePositiveLongOrZero(authMap.get("company_id"));
		if (companyId <= 0L) {
			companyId = parsePositiveLongOrZero(request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID));
			if (companyId > 0L) {
				authMap.put("company_id", companyId);
			}
		}
		if (parsePositiveLongOrZero(authMap.get("company_id")) <= 0L) {
			companyId = parsePositiveLongOrZero(FlexibleHttpServletParameterMap.toObjectMap(request).get("company_id"));
			if (companyId > 0L) {
				authMap.put("company_id", companyId);
			}
		}
		if (parsePositiveLongOrZero(authMap.get("company_id")) <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		if (!authMap.containsKey("user_id") || authMap.get("user_id") == null) {
			authMap.put("user_id", 0L);
		}
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Map<String, Object> data = wxappOrderCreateService.createNewOrder(request, merged, authMap);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long parsePositiveLongOrZero(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof String s && StringUtils.hasText(s)) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
