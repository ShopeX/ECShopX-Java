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

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.web.WxappMemberAuthAttributes;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappPaymentTradeDetailService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("ordersFrontV1Payment")
@RequestMapping("/api/v1/h5app/wxapp/trade")
public class PaymentController {

	private final WxappPaymentTradeDetailService wxappPaymentTradeDetailService;

	public PaymentController(WxappPaymentTradeDetailService wxappPaymentTradeDetailService) {
		this.wxappPaymentTradeDetailService = wxappPaymentTradeDetailService;
	}

	@GetMapping(value = "/detail", name = "支付单详情")
	public ResponseEntity<ApiResult<Object>> getTradeDetail(
			HttpServletRequest request,
			@RequestParam(name = "trade_id", required = false) String tradeId) {
		String t = tradeId == null ? "" : tradeId.trim();
		if (!StringUtils.hasText(t)) {
			throw new BadRequestException("支付单ID异常", 400);
		}
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authMap)) {
			throw new UnauthorizedException("未登录");
		}
		String companyIdStr = authString(authMap.get("company_id"));
		if (!StringUtils.hasText(companyIdStr)) {
			throw new UnauthorizedException("未登录");
		}
		String userIdStr = authString(authMap.get("user_id"));
		if (!StringUtils.hasText(userIdStr)) {
			userIdStr = Long.toString(0L);
		}
		Object data = wxappPaymentTradeDetailService.getTradeDetail(t, companyIdStr, userIdStr);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static String authString(Object v) {
		if (v == null) {
			return "";
		}
		if (v instanceof Number n) {
			return Long.toString(n.longValue());
		}
		if (v instanceof CharSequence cs) {
			return cs.toString().trim();
		}
		return String.valueOf(v).trim();
	}
}
