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
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.web.WxappMemberAuthAttributes;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappTradeSettingGetSettingService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = true)
@FrontNoAuth
@RestController("ordersFrontV1TradeSetting")
@RequestMapping("/api/v1/h5app/wxapp/trade")
public class TradeSettingController {

	private final WxappTradeSettingGetSettingService wxappTradeSettingGetSettingService;

	public TradeSettingController(WxappTradeSettingGetSettingService wxappTradeSettingGetSettingService) {
		this.wxappTradeSettingGetSettingService = wxappTradeSettingGetSettingService;
	}

	@GetMapping(value = "/setting", name = "交易配置", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getSetting(HttpServletRequest request) {
		Map<String, Object> auth = resolveFrontNoAuthClaims(request);
		long companyId = longVal(auth.get("company_id"));
		if (companyId <= 0L) {
			companyId = longVal(request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID));
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		Map<String, Object> payload = wxappTradeSettingGetSettingService.getSetting(companyId);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	private Map<String, Object> resolveFrontNoAuthClaims(HttpServletRequest request) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth instanceof Map<?, ?> authRaw) {
			return toStringKeyMap(authRaw);
		}
		rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (rawAuth instanceof Map<?, ?> authRaw2) {
			return toStringKeyMap(authRaw2);
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID));
		auth.put("user_id", 0L);
		return auth;
	}

	private static Map<String, Object> toStringKeyMap(Map<?, ?> raw) {
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : raw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		return auth;
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
