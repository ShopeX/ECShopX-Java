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

package cn.shopex.ecshopx.kaquan.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.kaquan.service.discount.AdminUserCardListFacadeService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(resource = DingoResponse.ResourceStyle.DINGO, badRequest = DingoResponse.BadRequestStyle.DINGO_422, unauthorized = true,
		notFound = false)
@AdminAuth
@RestController("kaquanUserDiscountAdminV1")
@RequestMapping("/api/v1")
public class UserDiscountController {

	private final AdminUserCardListFacadeService adminUserCardListFacadeService;

	public UserDiscountController(AdminUserCardListFacadeService adminUserCardListFacadeService) {
		this.adminUserCardListFacadeService = adminUserCardListFacadeService;
	}

	@Activated(routeAlias = "user.card.list")
	@GetMapping(value = "/getUserCardList", name = "获取用户可用的优惠券")
	public ResponseEntity<Map<String, Object>> getUserCardList(
			@RequestParam(value = "user_id", required = false, defaultValue = "0") long userId,
			@RequestParam(value = "code", required = false) String code,
			@RequestParam(value = "card_id", required = false) String cardId,
			@RequestParam(value = "amount", required = false) String amount,
			@RequestParam(value = "page_no", required = false, defaultValue = "1") int pageNo,
			@RequestParam(value = "page_size", required = false, defaultValue = "20") int pageSize,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") long distributorId,
			@RequestParam(value = "items", required = false) String items,
			HttpServletRequest request) {
		if (userId == 0L) {
			return ResponseEntity.ok(Map.of("data", Map.of("list", List.of(), "count", 0)));
		}
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		Object operatorIdRaw = operatorJwt.get("operator_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		if (!(operatorIdRaw instanceof Number)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		long operatorId = ((Number) operatorIdRaw).longValue();
		boolean hasItemsParam = request.getParameter("items") != null;
		Map<String, Object> body = adminUserCardListFacadeService.buildList(companyId, operatorId, userId, distributorId, code, cardId,
				amount, hasItemsParam, pageNo, pageSize);
		return ResponseEntity.ok(Map.of("data", body));
	}
}
