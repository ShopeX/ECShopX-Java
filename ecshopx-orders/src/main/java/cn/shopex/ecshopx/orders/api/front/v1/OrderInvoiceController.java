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
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.web.WxappMemberAuthAttributes;
import cn.shopex.ecshopx.orders.service.front.userinvoice.UserOrderInvoiceWxappListService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappOrderListFilterAssembler;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = true)
@FrontAuth
@RestController("ordersFrontV1OrderInvoice")
@RequestMapping("/api/v1/h5app/wxapp")
public class OrderInvoiceController {

	private final UserOrderInvoiceWxappListService userOrderInvoiceWxappListService;

	public OrderInvoiceController(UserOrderInvoiceWxappListService userOrderInvoiceWxappListService) {
		this.userOrderInvoiceWxappListService = userOrderInvoiceWxappListService;
	}

	@GetMapping(value = "/orders/invoice", name = "订单发票列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getInvoiceList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String page,
			@RequestParam(name = "pageSize", required = false) String pageSize) {
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

		if (WxappOrderListFilterAssembler.looseAuthUserIdFalsy(auth.get("user_id"))) {
			LinkedHashMap<String, Object> early = new LinkedHashMap<>();
			early.put("total_count", 0L);
			early.put("list", new ArrayList<>());
			return ResponseEntity.ok(ApiResult.ok(early));
		}

		if (!auth.containsKey("company_id") || longVal(auth.get("company_id")) == 0L) {
			throw new UnauthorizedException("未登录");
		}

		long userId = longVal(auth.get("user_id"));
		long companyId = longVal(auth.get("company_id"));
		int pageInt = parsePositiveIntOrDefault(page, 1);
		int pageSizeInt = parsePositiveIntOrDefault(pageSize, 20);

		Map<String, Object> data = userOrderInvoiceWxappListService.getInvoiceList(userId, companyId, pageInt, pageSizeInt);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static int parsePositiveIntOrDefault(Object raw, int dflt) {
		if (raw == null) {
			return dflt;
		}
		if (raw instanceof Number n) {
			int v = n.intValue();
			return v > 0 ? v : dflt;
		}
		try {
			int v = Integer.parseInt(String.valueOf(raw).trim());
			return v > 0 ? v : dflt;
		} catch (NumberFormatException e) {
			return dflt;
		}
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
