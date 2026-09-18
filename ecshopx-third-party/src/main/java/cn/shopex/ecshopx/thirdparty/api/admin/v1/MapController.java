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

package cn.shopex.ecshopx.thirdparty.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.thirdparty.service.map.MapConfigAdminService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
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
		notFound = false
)
@AdminAuth
@ShopLog
@RestController("thirdPartyMapAdminV1")
@RequestMapping("/api/v1")
public class MapController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final MapConfigAdminService mapConfigAdminService;

	public MapController(MapConfigAdminService mapConfigAdminService) {
		this.mapConfigAdminService = mapConfigAdminService;
	}

	@Activated(routeAlias = "third.map.setting.set")
	@PostMapping(value = "/third/map/setting", name = "更新第三方地图定位的类型")
	public ResponseEntity<Map<String, Object>> set(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);

		String mt = firstString(merged.get("map_type"));
		if (!StringUtils.hasText(mt)) {
			throw new BadRequestException("地图配置类型必填！");
		}
		String appKey = firstString(merged.get("app_key"));
		if (!StringUtils.hasText(appKey)) {
			throw new BadRequestException("key必填！");
		}
		if (merged.get("is_default") == null) {
			throw new BadRequestException("参数有误");
		}
		if (!isValidIsDefaultInput(merged.get("is_default"))) {
			throw new BadRequestException("参数有误");
		}
		String secret = merged.get("app_secret") == null ? "" : merged.get("app_secret").toString();
		int isDef = normalizeIsDefault(merged.get("is_default"));
		long companyId = readRequiredLong(ud, "company_id");

		Optional<Map<String, Object>> result =
				mapConfigAdminService.set(companyId, mt, appKey, secret, isDef);
		if (result.isEmpty()) {
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("data", Collections.emptyList());
			return ResponseEntity.ok(out);
		}
		return ResponseEntity.ok(Map.of("data", result.get()));
	}

	@Activated(routeAlias = "third.map.setting.get")
	@GetMapping(value = "/third/map/setting", name = "获取第三方地图定位的类型")
	public ResponseEntity<Map<String, Object>> get(HttpServletRequest request) {
		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		Map<String, Object> payload = mapConfigAdminService.get(companyId);
		return ResponseEntity.ok(Map.of("data", payload));
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

	private static String firstString(Object raw) {
		if (raw == null) {
			return null;
		}
		String t = raw.toString().trim();
		return StringUtils.hasText(t) ? t : null;
	}

	private static boolean isValidIsDefaultInput(Object v) {
		if (v instanceof Boolean) {
			return true;
		}
		if (v instanceof Number n) {
			int i = n.intValue();
			if (i != 0 && i != 1) {
				return false;
			}
			return n.doubleValue() == i;
		}
		if (v instanceof String s) {
			String t = s.trim();
			return "0".equals(t) || "1".equals(t);
		}
		return false;
	}

	private static int normalizeIsDefault(Object v) {
		if (v instanceof Boolean b) {
			return b ? 1 : 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		if (v instanceof String s) {
			String t = s.trim();
			if ("1".equals(t)) {
				return 1;
			}
			if ("0".equals(t)) {
				return 0;
			}
		}
		throw new IllegalStateException("normalizeIsDefault: unreachable after validation");
	}
}
