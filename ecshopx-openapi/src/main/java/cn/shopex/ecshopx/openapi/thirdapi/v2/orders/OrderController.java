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

package cn.shopex.ecshopx.openapi.thirdapi.v2.orders;

import cn.shopex.ecshopx.common.annotation.OpenapiResponse;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.openapi.OpenapiEnvelope;
import cn.shopex.ecshopx.common.openapi.OpenapiEnabledLogisticsListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderCancelPort;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderCancelReasonsPort;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderConfirmCancelPort;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderDetailPort;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderIncrListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderListV2Port;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderSoldListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderWriteoffPort;
import cn.shopex.ecshopx.common.openapi.OpenapiShippingTemplatesListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiTradeListPort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiMemberQueryParams;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiOrderListParams;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@OpenapiResponse
@RestController("openapiV2Order")
@RequestMapping("/api/openapi/internal/v2")
public class OrderController extends OpenapiBaseController {

	private final OpenapiOrderWriteoffPort orderWriteoffPort;
	private final OpenapiOrderIncrListPort orderIncrListPort;
	private final OpenapiOrderSoldListPort orderSoldListPort;
	private final OpenapiOrderListV2Port orderListV2Port;
	private final OpenapiOrderDetailPort orderDetailPort;
	private final OpenapiOrderCancelReasonsPort cancelReasonsPort;
	private final OpenapiOrderCancelPort orderCancelPort;
	private final OpenapiEnabledLogisticsListPort enabledLogisticsListPort;
	private final OpenapiOrderConfirmCancelPort confirmCancelPort;
	private final OpenapiShippingTemplatesListPort shippingTemplatesListPort;
	private final OpenapiTradeListPort tradeListPort;

	public OrderController(
			OpenapiOrderWriteoffPort orderWriteoffPort,
			OpenapiOrderIncrListPort orderIncrListPort,
			OpenapiOrderSoldListPort orderSoldListPort,
			OpenapiOrderListV2Port orderListV2Port,
			OpenapiOrderDetailPort orderDetailPort,
			OpenapiOrderCancelReasonsPort cancelReasonsPort,
			OpenapiOrderCancelPort orderCancelPort,
			OpenapiEnabledLogisticsListPort enabledLogisticsListPort,
			OpenapiOrderConfirmCancelPort confirmCancelPort,
			OpenapiShippingTemplatesListPort shippingTemplatesListPort,
			OpenapiTradeListPort tradeListPort) {
		this.orderWriteoffPort = orderWriteoffPort;
		this.orderIncrListPort = orderIncrListPort;
		this.orderSoldListPort = orderSoldListPort;
		this.orderListV2Port = orderListV2Port;
		this.orderDetailPort = orderDetailPort;
		this.cancelReasonsPort = cancelReasonsPort;
		this.orderCancelPort = orderCancelPort;
		this.enabledLogisticsListPort = enabledLogisticsListPort;
		this.confirmCancelPort = confirmCancelPort;
		this.shippingTemplatesListPort = shippingTemplatesListPort;
		this.tradeListPort = tradeListPort;
	}

	@PostMapping(value = "/ecx.order.list", name = "开放接口会员订单列表")
	public OpenapiEnvelope list(
			HttpServletRequest request,
			@RequestParam(name = "unionid", required = false) String unionidParam,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		boolean mobilePresent = OpenapiMemberQueryParams.isParamPresent(mobileParam, body, "mobile");
		String mobileRaw = OpenapiRequestParams.mergeString(mobileParam, body, "mobile");
		String unionidRaw = OpenapiRequestParams.mergeString(unionidParam, body, "unionid");
		boolean mobileTruthy = OpenapiMemberQueryParams.isPhpTruthy(mobileRaw);
		OpenapiOrderListParams.PageSpec pageSpec =
				OpenapiOrderListParams.resolve(pageParam, pageSizeParam, body);
		Map<String, Object> data =
				orderListV2Port.list(
						companyId,
						mobileParam,
						unionidParam,
						body,
						mobilePresent,
						mobileRaw,
						mobileTruthy,
						unionidRaw,
						pageSpec.page(),
						pageSpec.pageSize(),
						pageSpec.pageOverridden());
		String message = resolveListMessage(data);
		return new OpenapiEnvelope("success", "E0000", message, data);
	}

	@Activated(routeAlias = "order.writeoff.set")
	@PostMapping(value = "/ecx.order.writeoff", name = "开放接口自提订单核销")
	public Map<String, Object> orderWriteoff(
			HttpServletRequest request,
			@RequestParam(name = "order_id", required = false) String orderIdParam,
			@RequestParam(name = "pickupcode", required = false) String pickupcodeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		orderWriteoffPort.executeWriteoff(
				companyId,
				OpenapiRequestParams.originalString(orderIdParam, body, "order_id"),
				OpenapiRequestParams.originalString(pickupcodeParam, body, "pickupcode"));
		return Map.of("status", true);
	}

	@GetMapping(value = "/ecx.orders.incr.get", name = "开放接口增量订单搜索")
	public Object getIncrOrderList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "start_modified", required = false) String startModifiedParam,
			@RequestParam(name = "end_modified", required = false) String endModifiedParam,
			@RequestParam(name = "shop_code", required = false) String shopCodeParam,
			@RequestParam(name = "is_self", required = false) String isSelfParam,
			@RequestParam(name = "order_status", required = false) String orderStatusParam,
			@RequestParam(name = "pay_status", required = false) String payStatusParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2OrderIncrListParams.PageSpec pageSpec =
				OpenapiThirdApiV2OrderIncrListParams.resolve(pageParam, pageSizeParam, body);

		String mobileRaw = OpenapiRequestParams.originalString(mobileParam, body, "mobile");
		String shopCodeRaw = OpenapiRequestParams.originalString(shopCodeParam, body, "shop_code");
		String startModifiedRaw =
				OpenapiRequestParams.originalString(startModifiedParam, body, "start_modified");
		String endModifiedRaw = OpenapiRequestParams.originalString(endModifiedParam, body, "end_modified");
		String isSelfRaw = OpenapiRequestParams.originalString(isSelfParam, body, "is_self");
		String orderStatusRaw = OpenapiRequestParams.originalString(orderStatusParam, body, "order_status");
		String payStatusRaw = OpenapiRequestParams.originalString(payStatusParam, body, "pay_status");

		return orderIncrListPort.executeIncrOrderList(
				companyId,
				pageSpec.page(),
				pageSpec.pageSize(),
				OpenapiMemberQueryParams.isPhpTruthy(mobileRaw),
				mobileRaw,
				OpenapiMemberQueryParams.isPhpTruthy(shopCodeRaw),
				shopCodeRaw,
				OpenapiMemberQueryParams.isPhpTruthy(startModifiedRaw),
				startModifiedRaw,
				OpenapiMemberQueryParams.isPhpTruthy(endModifiedRaw),
				endModifiedRaw,
				OpenapiMemberQueryParams.isParamPresent(isSelfParam, body, "is_self"),
				isSelfRaw,
				OpenapiMemberQueryParams.isParamPresent(orderStatusParam, body, "order_status"),
				orderStatusRaw,
				OpenapiMemberQueryParams.isParamPresent(payStatusParam, body, "pay_status"),
				payStatusRaw);
	}

	@GetMapping(value = "/ecx.order.cancel.reasons.get", name = "开放接口订单取消原因列表")
	public Map<String, String> getCancelReasons(HttpServletRequest request) {
		return cancelReasonsPort.listAllAsStringKeyMap();
	}

	@GetMapping(value = "/ecx.trades.get", name = "开放接口交易单列表")
	public Map<String, Object> getTradeList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2TradeListParams.PageSpec pageSpec =
				OpenapiThirdApiV2TradeListParams.resolve(pageParam, pageSizeParam, body);
		return tradeListPort.list(companyId, pageSpec.page(), pageSpec.pageSize());
	}

	@GetMapping(value = "/ecx.shipping.templates.get", name = "开放接口运费模板列表")
	public Map<String, Object> getShippingtemplates(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2ShippingTemplatesListParams.PageSpec pageSpec =
				OpenapiThirdApiV2ShippingTemplatesListParams.resolve(pageParam, pageSizeParam, body);
		return shippingTemplatesListPort.list(companyId, pageSpec.page(), pageSpec.pageSize());
	}

	@GetMapping(value = "/ecx.logistics.enabled.get", name = "开放接口开启物流公司列表")
	public List<Map<String, Object>> getEnabledLogisticsList(
			HttpServletRequest request,
			@RequestParam(name = "distributor_id", required = false) String distributorIdParam,
			@RequestParam(name = "supplier_id", required = false) String supplierIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		int distributorId = parseOptionalIntDefaultZero(distributorIdParam, body, "distributor_id");
		int supplierId = parseOptionalIntDefaultZero(supplierIdParam, body, "supplier_id");
		return enabledLogisticsListPort.listEnabled(companyId, distributorId, supplierId);
	}

	@PostMapping(value = "/ecx.order.cancel.confirm", name = "开放接口确认订单取消审核")
	public Map<String, Object> confirmCancel(
			HttpServletRequest request,
			@RequestParam(name = "order_id", required = false) String orderIdParam,
			@RequestParam(name = "cancel_handle", required = false) String cancelHandleParam,
			@RequestParam(name = "reject_reason", required = false) String rejectReasonParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		confirmCancelPort.executeConfirmCancel(
				companyId,
				OpenapiRequestParams.originalString(orderIdParam, body, "order_id"),
				OpenapiRequestParams.originalString(cancelHandleParam, body, "cancel_handle"),
				OpenapiRequestParams.originalString(rejectReasonParam, body, "reject_reason"));
		return Map.of("status", true);
	}

	@PostMapping(value = "/ecx.order.cancel", name = "开放接口取消订单")
	public Map<String, Object> orderCancel(
			HttpServletRequest request,
			@RequestParam(name = "order_id", required = false) String orderIdParam,
			@RequestParam(name = "cancel_reason_id", required = false) String cancelReasonIdParam,
			@RequestParam(name = "cancel_reason", required = false) String cancelReasonParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		orderCancelPort.executeCancel(
				companyId,
				OpenapiRequestParams.originalString(orderIdParam, body, "order_id"),
				OpenapiRequestParams.originalString(cancelReasonIdParam, body, "cancel_reason_id"),
				OpenapiRequestParams.originalString(cancelReasonParam, body, "cancel_reason"));
		return Map.of("status", true);
	}

	@GetMapping(value = "/ecx.order.get", name = "开放接口订单详情")
	public Map<String, Object> getDetail(
			HttpServletRequest request,
			@RequestParam(name = "order_id", required = false) String orderIdParam,
			@RequestParam(name = "ziti_code", required = false) String zitiCodeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		return orderDetailPort.getOrderDetail(
				companyId,
				OpenapiRequestParams.originalString(orderIdParam, body, "order_id"),
				OpenapiRequestParams.originalString(zitiCodeParam, body, "ziti_code"));
	}

	@GetMapping(value = "/ecx.orders.sold.get", name = "开放接口订单搜索")
	public Object getList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "time_begin", required = false) String timeBeginParam,
			@RequestParam(name = "time_end", required = false) String timeEndParam,
			@RequestParam(name = "shop_code", required = false) String shopCodeParam,
			@RequestParam(name = "is_self", required = false) String isSelfParam,
			@RequestParam(name = "order_status", required = false) String orderStatusParam,
			@RequestParam(name = "pay_status", required = false) String payStatusParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2OrderSoldListParams.PageSpec pageSpec =
				OpenapiThirdApiV2OrderSoldListParams.resolve(pageParam, pageSizeParam, body);

		String mobileRaw = OpenapiRequestParams.originalString(mobileParam, body, "mobile");
		String timeBeginRaw = OpenapiRequestParams.originalString(timeBeginParam, body, "time_begin");
		String timeEndRaw = OpenapiRequestParams.originalString(timeEndParam, body, "time_end");
		String shopCodeRaw = OpenapiRequestParams.originalString(shopCodeParam, body, "shop_code");
		String isSelfRaw = OpenapiRequestParams.originalString(isSelfParam, body, "is_self");
		String orderStatusRaw = OpenapiRequestParams.originalString(orderStatusParam, body, "order_status");
		String payStatusRaw = OpenapiRequestParams.originalString(payStatusParam, body, "pay_status");

		return orderSoldListPort.executeSoldOrderList(
				companyId,
				pageSpec.page(),
				pageSpec.pageSize(),
				OpenapiMemberQueryParams.isPhpTruthy(mobileRaw),
				mobileRaw,
				OpenapiMemberQueryParams.isPhpTruthy(timeBeginRaw),
				timeBeginRaw,
				OpenapiMemberQueryParams.isPhpTruthy(timeEndRaw),
				timeEndRaw,
				OpenapiMemberQueryParams.isPhpTruthy(shopCodeRaw),
				shopCodeRaw,
				OpenapiMemberQueryParams.isParamPresent(isSelfParam, body, "is_self"),
				isSelfRaw,
				OpenapiMemberQueryParams.isParamPresent(orderStatusParam, body, "order_status"),
				orderStatusRaw,
				OpenapiMemberQueryParams.isParamPresent(payStatusParam, body, "pay_status"),
				payStatusRaw);
	}

	private static String resolveListMessage(Map<String, Object> data) {
		Object count = data != null ? data.get("count") : null;
		long c = count instanceof Number n ? n.longValue() : 0L;
		return c <= 0L ? "成功" : "操作成功";
	}

	/**
	 * 可选整型参数：Query/Body 合并（Body 含键优先，{@link OpenapiRequestParams#originalString}）。
	 * 键缺失 → 0；存在但 blank/null → 0；存在且可解析为 int → 解析值；非法非空字符串 → 0。
	 */
	private static int parseOptionalIntDefaultZero(
			String queryParam, Map<String, Object> body, String key) {
		boolean presentInBody = body != null && body.containsKey(key);
		boolean presentInQuery = queryParam != null;
		if (!presentInBody && !presentInQuery) {
			return 0;
		}
		String raw = OpenapiRequestParams.originalString(queryParam, body, key);
		if (raw == null || raw.isBlank()) {
			return 0;
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
