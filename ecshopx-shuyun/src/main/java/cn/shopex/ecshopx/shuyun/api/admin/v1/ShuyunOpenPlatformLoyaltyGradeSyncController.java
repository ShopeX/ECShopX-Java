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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.shuyun.service.openplatform.LoyaltyGradeSyncService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** B3：手动同步数云会员等级档案。 */
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("shuyunOpenPlatformLoyaltyGradeSyncAdminV1")
@RequestMapping("/api/v1")
public class ShuyunOpenPlatformLoyaltyGradeSyncController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final LoyaltyGradeSyncService loyaltyGradeSyncService;

	public ShuyunOpenPlatformLoyaltyGradeSyncController(LoyaltyGradeSyncService loyaltyGradeSyncService) {
		this.loyaltyGradeSyncService = loyaltyGradeSyncService;
	}

	@Activated(routeAlias = "shuyun.open_platform.loyalty.grade.sync")
	@PostMapping(value = "/shuyun/open-platform/loyalty/grade/sync", name = "数云开放网关-等级手动同步")
	public ResponseEntity<ApiResult<Map<String, Object>>> postManualSync(HttpServletRequest request) {
		long companyId = companyId(request);
		try {
			Map<String, Object> report = loyaltyGradeSyncService.syncByCompanyIdWithReport(companyId);
			return ResponseEntity.ok(ApiResult.ok(report));
		} catch (ResourceException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new ResourceException(
					e.getMessage() == null || e.getMessage().isBlank() ? "等级同步失败" : e.getMessage());
		}
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
