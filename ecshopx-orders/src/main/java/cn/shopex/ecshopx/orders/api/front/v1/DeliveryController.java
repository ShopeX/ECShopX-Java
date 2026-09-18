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
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappDeliveryInfoService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappDeliveryListsService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
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
@RestController("ordersFrontV1Delivery")
@RequestMapping("/api/v1/h5app/wxapp")
public class DeliveryController {

	private final WxappDeliveryListsService wxappDeliveryListsService;
	private final WxappDeliveryInfoService wxappDeliveryInfoService;

	public DeliveryController(
			WxappDeliveryListsService wxappDeliveryListsService,
			WxappDeliveryInfoService wxappDeliveryInfoService) {
		this.wxappDeliveryListsService = wxappDeliveryListsService;
		this.wxappDeliveryInfoService = wxappDeliveryInfoService;
	}

	@GetMapping(
			value = "/delivery/lists",
			name = "发货单列表",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> lists(
			HttpServletRequest request,
			@RequestParam(name = "order_id", required = false) String orderIdParam) {
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

		Long orderId = null;
		if (StringUtils.hasText(orderIdParam)) {
			String trimmed = orderIdParam.trim();
			if (!trimmed.isEmpty()) {
				try {
					orderId = Long.parseLong(trimmed);
				} catch (NumberFormatException ignored) {
					orderId = null;
				}
			}
		}

		Map<String, Object> body = wxappDeliveryListsService.lists(companyId, orderId);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	@GetMapping(
			value = "/delivery/trackerpull",
			name = "发货单物流",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<List<LinkedHashMap<String, String>>>> deliveryInfo(
			HttpServletRequest request,
			@RequestParam(value = "delivery_id", required = false) String deliveryIdParam) {
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

		if (deliveryIdParam == null || !StringUtils.hasText(deliveryIdParam.trim())) {
			throw new BadRequestException("缺少发货单id");
		}
		String deliveryIdTrimmed = deliveryIdParam.trim();
		Object authUserIdRaw = auth.get("user_id");

		List<LinkedHashMap<String, String>> list =
				wxappDeliveryInfoService.deliveryInfo(companyId, deliveryIdTrimmed, authUserIdRaw);
		return ResponseEntity.ok(ApiResult.ok(list));
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
}
