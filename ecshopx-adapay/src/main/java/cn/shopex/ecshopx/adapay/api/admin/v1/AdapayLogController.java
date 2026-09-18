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

package cn.shopex.ecshopx.adapay.api.admin.v1;

import cn.shopex.ecshopx.adapay.service.AdapayLogListService;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("adapayLogAdminV1")
@RequestMapping("/api/v1/adapay")
public class AdapayLogController {

	private static final Set<String> LOG_TYPES = Set.of("merchant", "distributor", "dealer");

	private final AdapayLogListService adapayLogListService;

	public AdapayLogController(AdapayLogListService adapayLogListService) {
		this.adapayLogListService = adapayLogListService;
	}

	@Activated(routeAlias = "adapay.log.list")
	@GetMapping(value = "/log/list", name = "操作日志列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getList(
			HttpServletRequest request,
			@RequestParam(value = "log_type", required = false) String logType,
			@RequestParam(value = "page", required = false) String page,
			@RequestParam(value = "page_size", required = false) String pageSize,
			@RequestParam(value = "rel_id", required = false) String relId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		String lt = logType == null ? "" : logType.trim().toLowerCase(Locale.ROOT);
		if (lt.isEmpty() || !LOG_TYPES.contains(lt)) {
			throw new ResourceException("日志类型必填");
		}
		int pageInt = parsePositiveIntQuery(page, "页码必填");
		int pageSizeInt = parsePositiveIntQuery(pageSize, "页条数必填");

		Map<String, Object> data =
				adapayLogListService.getList(companyId, lt, pageInt, pageSizeInt, relId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static int parsePositiveIntQuery(String raw, String resourceMessage) {
		String s = raw == null ? "" : raw.trim();
		if (s.isEmpty()) {
			throw new ResourceException(resourceMessage);
		}
		int v;
		try {
			v = Integer.parseInt(s);
		} catch (NumberFormatException e) {
			throw new ResourceException(resourceMessage);
		}
		if (v < 1) {
			throw new ResourceException(resourceMessage);
		}
		return v;
	}
}
