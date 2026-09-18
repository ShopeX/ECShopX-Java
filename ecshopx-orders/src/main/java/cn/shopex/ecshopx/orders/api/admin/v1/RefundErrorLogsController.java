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
import cn.shopex.ecshopx.orders.service.refund.RefundErrorLogsService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("ordersAdminV1RefundErrorLogs")
@RequestMapping("/api/v1/trade/refunderrorlogs")
public class RefundErrorLogsController {

	private final RefundErrorLogsService refundErrorLogsService;

	public RefundErrorLogsController(RefundErrorLogsService refundErrorLogsService) {
		this.refundErrorLogsService = refundErrorLogsService;
	}

	@Activated(routeAlias = "trade.refunderrorlogs.list")
	@GetMapping(value = "/list", name = "退款错误列表")
	public ApiResult<Map<String, Object>> getList(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) attr;
		return ApiResult.ok(refundErrorLogsService.getList(jwtMap, request));
	}

	@Activated(routeAlias = "trade.refunderrorlogs.resubmit")
	@PutMapping(value = "/resubmit/{id}", name = "重新提交退款")
	public ApiResult<Map<String, Object>> resubmitRefund(@PathVariable("id") String id) {
		return ApiResult.ok(refundErrorLogsService.resubmitRefund(id));
	}
}
