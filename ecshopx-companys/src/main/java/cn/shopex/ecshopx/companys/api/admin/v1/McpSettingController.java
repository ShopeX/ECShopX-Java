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

package cn.shopex.ecshopx.companys.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.service.setting.McpSettingRedisService;
import cn.shopex.ecshopx.companys.web.CompanysAdminRequestMerge;
import jakarta.servlet.http.HttpServletRequest;
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
		notFound = false)
@AdminAuth
@ShopLog
@RestController("companysAdminV1McpSetting")
@RequestMapping("/api/v1")
public class McpSettingController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final McpSettingRedisService mcpSettingRedisService;

	public McpSettingController(McpSettingRedisService mcpSettingRedisService) {
		this.mcpSettingRedisService = mcpSettingRedisService;
	}

	@Activated(routeAlias = "setting.mcp.get")
	@GetMapping(value = "/setting/mcp", name = "获取 MCP 设置")
	public ResponseEntity<ApiResult<Map<String, Object>>> getMcpSetting(HttpServletRequest request) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> data = mcpSettingRedisService.getSetting(companyId, requestBaseUrl(request));
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "setting.mcp.set")
	@PostMapping(value = "/setting/mcp", name = "保存 MCP 设置")
	public ResponseEntity<ApiResult<Map<String, Object>>> setMcpSetting(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> merged = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> data =
				mcpSettingRedisService.saveSetting(companyId, merged, requestBaseUrl(request));
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static String requestBaseUrl(HttpServletRequest request) {
		String forwardedProto = request.getHeader("X-Forwarded-Proto");
		String forwardedHost = request.getHeader("X-Forwarded-Host");
		if (StringUtils.hasText(forwardedProto) && StringUtils.hasText(forwardedHost)) {
			return forwardedProto.trim() + "://" + forwardedHost.trim();
		}
		StringBuffer url = request.getRequestURL();
		String uri = request.getRequestURI();
		if (url != null && StringUtils.hasText(uri) && url.length() >= uri.length()) {
			return url.substring(0, url.length() - uri.length());
		}
		return "";
	}

	private static long readCompanyIdFromOperatorJwt(HttpServletRequest request) {
		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud) || ud.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		Object v = ud.get("company_id");
		if (v == null) {
			throw new UnauthorizedException("未登录");
		}
		try {
			return Long.parseLong(String.valueOf(v));
		}
		catch (NumberFormatException ex) {
			throw new UnauthorizedException("未登录");
		}
	}
}
