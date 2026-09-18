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

package cn.shopex.ecshopx.shuyun.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.shuyun.service.openplatform.OpenPlatformConfigService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
@RestController("shuyunOpenPlatformConfigAdminV1")
@RequestMapping("/api/v1")
public class ShuyunOpenPlatformConfigController {

	/** 与 OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA 同值；避免 shuyun→espier 循环依赖。 */
	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final OpenPlatformConfigService openPlatformConfigService;

	public ShuyunOpenPlatformConfigController(OpenPlatformConfigService openPlatformConfigService) {
		this.openPlatformConfigService = openPlatformConfigService;
	}

	@Activated(routeAlias = "shuyun.open_platform.config.get")
	@GetMapping(value = "/shuyun/open-platform/config", name = "数云开放网关配置-获取")
	public ResponseEntity<ApiResult<Map<String, Object>>> getConfig(HttpServletRequest request) {
		long companyId = companyId(request);
		Map<String, Object> data = openPlatformConfigService.getAdminView(companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "shuyun.open_platform.config.put")
	@PutMapping(value = "/shuyun/open-platform/config", name = "数云开放网关配置-保存")
	public ResponseEntity<ApiResult<Map<String, Object>>> putConfig(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = companyId(request);
		Map<String, Object> input = body == null ? Map.of() : body;
		openPlatformConfigService.saveFromAdmin(companyId, input);
		Map<String, Object> ok = new LinkedHashMap<>();
		ok.put("ok", true);
		return ResponseEntity.ok(ApiResult.ok(ok));
	}

	private static long companyId(HttpServletRequest request) {
		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> map)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = map.get("company_id");
		if (cid instanceof Number n) {
			return n.longValue();
		}
		if (cid != null) {
			try {
				return Long.parseLong(String.valueOf(cid).trim());
			} catch (NumberFormatException ignored) {
				// fall through
			}
		}
		throw new UnauthorizedException("未登录");
	}
}
