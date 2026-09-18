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
import cn.shopex.ecshopx.companys.service.setting.TradeBasicSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.TradeCancelSettingRedisService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("ordersAdminV1TradeSetting")
@RequestMapping("/api/v1/trade")
public class TradeSettingController {

	private final TradeCancelSettingRedisService tradeCancelSettingRedisService;
	private final TradeBasicSettingRedisService tradeBasicSettingRedisService;

	public TradeSettingController(
			TradeCancelSettingRedisService tradeCancelSettingRedisService,
			TradeBasicSettingRedisService tradeBasicSettingRedisService) {
		this.tradeCancelSettingRedisService = tradeCancelSettingRedisService;
		this.tradeBasicSettingRedisService = tradeBasicSettingRedisService;
	}

	@Activated(routeAlias = "trade.setting.set")
	@PostMapping(value = "/setting", name = "交易配置保存", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> setSetting(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = readCompanyIdFromJwt(request);
		Object config = merged.get("config");
		tradeBasicSettingRedisService.setSetting(companyId, config);
		LinkedHashMap<String, Object> data = new LinkedHashMap<>(1);
		data.put("status", Boolean.TRUE);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "trade.setting.get")
	@GetMapping(value = "/setting", name = "交易配置", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Object> getSetting(HttpServletRequest request) {
		long companyId = readCompanyIdFromJwt(request);
		Object data = tradeBasicSettingRedisService.getSetting(companyId);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "trade.cancel.setting.set")
	@PostMapping(value = "/cancel/setting", name = "取消订单配置保存", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> setCancelSetting(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = readCompanyIdFromJwt(request);
		boolean flag = normalizeRepeatCancel(merged.get("repeat_cancel"));
		tradeCancelSettingRedisService.setCancelSetting(companyId, flag);
		LinkedHashMap<String, Object> data = new LinkedHashMap<>(1);
		data.put("status", Boolean.TRUE);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "trade.cancel.setting.get")
	@GetMapping(value = "/cancel/setting", name = "取消订单配置", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getCancelSetting(HttpServletRequest request) {
		long companyId = readCompanyIdFromJwt(request);
		Map<String, Object> data = tradeCancelSettingRedisService.getCancelSetting(companyId);
		return ApiResult.ok(data);
	}

	private static boolean normalizeRepeatCancel(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof String s) {
			return "true".equals(s);
		}
		return false;
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
}
