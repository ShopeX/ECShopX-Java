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
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.systemlink.service.omsqueuelog.OmsQueueLogAdminService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false
)
@AdminAuth
@ShopLog
@RestController("systemLinkOmsQueueLogAdminV1")
@RequestMapping("/api/v1")
public class OmsQueueLogController {

	private final OmsQueueLogAdminService omsQueueLogAdminService;

	public OmsQueueLogController(OmsQueueLogAdminService omsQueueLogAdminService) {
		this.omsQueueLogAdminService = omsQueueLogAdminService;
	}

	@Activated(routeAlias = "omsqueuelog.get")
	@GetMapping(value = "/omsqueuelog", name = "获取oms通信日志列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getLogList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false, defaultValue = "1") int page,
			@RequestParam(name = "pageSize", required = false, defaultValue = "20") int pageSize,
			@RequestParam(name = "api_type", required = false) String apiType,
			@RequestParam(name = "status", required = false) String status,
			@RequestParam(name = "content", required = false) String content,
			@RequestParam(name = "updated", required = false) List<String> updated) {
		long companyId = resolveCompanyIdForOmsQueueLog(request);
		int effectivePage = Math.max(1, page);
		Map<String, Object> body = omsQueueLogAdminService.getLogList(
				companyId, effectivePage, pageSize, apiType, status, content, updated);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	private static Map<String, Object> readOperatorClaimsMapOrEmpty(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> map)) {
			return Collections.emptyMap();
		}
		Map<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : map.entrySet()) {
			Object k = e.getKey();
			out.put(k == null ? "" : k.toString(), e.getValue());
		}
		return out;
	}

	private static long parseRequiredPositiveLongClaim(Map<String, Object> claims, String key) {
		Object v = claims.get(key);
		if (v == null) {
			throw new UnauthorizedException("未登录");
		}
		long value;
		if (v instanceof Number n) {
			value = n.longValue();
		} else {
			try {
				value = Long.parseLong(v.toString());
			} catch (NumberFormatException e) {
				throw new UnauthorizedException("未登录");
			}
		}
		if (value <= 0) {
			throw new UnauthorizedException("未登录");
		}
		return value;
	}

	private static long resolveCompanyIdForOmsQueueLog(HttpServletRequest request) {
		return parseRequiredPositiveLongClaim(readOperatorClaimsMapOrEmpty(request), "company_id");
	}
}
