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

package cn.shopex.ecshopx.systemlink.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.systemlink.service.third.ThirdShopexErpSettingAdminService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true
)
@AdminAuth
@ShopLog
@RestController("systemLinkThirdAdminV1")
@RequestMapping("/api/v1")
public class ThirdController {

	private final ThirdShopexErpSettingAdminService thirdShopexErpSettingAdminService;

	public ThirdController(ThirdShopexErpSettingAdminService thirdShopexErpSettingAdminService) {
		this.thirdShopexErpSettingAdminService = thirdShopexErpSettingAdminService;
	}

	@Activated(routeAlias = "third.shopexerp.setting.set")
	@PostMapping(value = "/third/shopexerp/setting", name = "shopexerp配置信息保存")
	public ResponseEntity<Map<String, Object>> setShopexErpSetting(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		thirdShopexErpSettingAdminService.setShopexErpSetting(companyId, merged);
		return ResponseEntity.ok(Map.of("data", Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "third.shopexerp.setting.get")
	@GetMapping(value = "/third/shopexerp/setting", name = "获取shopexerp配置信息保存")
	public ResponseEntity<Map<String, Object>> getShopexErpSetting(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		Map<String, Object> payload = thirdShopexErpSettingAdminService.getShopexErpSetting(companyId);
		Map<String, Object> body = new LinkedHashMap<>(2);
		body.put("data", payload);
		return ResponseEntity.ok(body);
	}

	private static Map<String, Object> mergeInputLikeFlexibleResolver(HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			LinkedHashMap<String, Object> input = new LinkedHashMap<>(parameterMapToMap(request));
			if (body != null) {
				input.putAll(body);
			}
			return input;
		}
		if (body != null) {
			return new LinkedHashMap<>(body);
		}
		return new LinkedHashMap<>(parameterMapToMap(request));
	}

	private static Map<String, Object> parameterMapToMap(HttpServletRequest request) {
		Map<String, Object> m = new LinkedHashMap<>();
		request.getParameterMap().forEach((k, v) -> {
			if (v != null && v.length > 0 && StringUtils.hasText(v[0])) {
				m.put(k, v[0]);
			}
		});
		return m;
	}

	private static long readRequiredLong(Map<?, ?> ud, String key) {
		Object v = ud.get(key);
		if (v == null) {
			throw new UnauthorizedException("未登录");
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
	}
}
