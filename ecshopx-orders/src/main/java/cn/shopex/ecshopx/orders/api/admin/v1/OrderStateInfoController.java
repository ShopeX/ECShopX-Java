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
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.orders.service.admin.OrderStateInfoPayOrderInfoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@RestController("ordersAdminV1OrderStateInfo")
@RequestMapping("/api/v1/order")
public class OrderStateInfoController {

	private final OrderStateInfoPayOrderInfoService orderStateInfoPayOrderInfoService;

	public OrderStateInfoController(OrderStateInfoPayOrderInfoService orderStateInfoPayOrderInfoService) {
		this.orderStateInfoPayOrderInfoService = orderStateInfoPayOrderInfoService;
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@Activated(routeAlias = "order.info.get")
	@GetMapping(value = "/payorderinfo/{trade_id}", name = "支付单状态", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Object> getPayOrderInfo(
			HttpServletRequest request,
			@PathVariable("trade_id") String tradeId,
			@RequestParam(value = "pay_type", required = false) String payType) {
		return ApiResult.ok(orderStateInfoPayOrderInfoService.getPayOrderInfo(request, tradeId, payType));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@Activated(routeAlias = "order.info.get")
	@GetMapping(value = "/refundorderinfo/{refund_bn}", name = "退款单状态", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Object> getRefundOrderInfo(
			HttpServletRequest request,
			@PathVariable("refund_bn") String refundBn,
			@RequestParam(value = "pay_type", required = false) String payType) {
		return ApiResult.ok(orderStateInfoPayOrderInfoService.getRefundOrderInfo(request, refundBn, payType));
	}
}
