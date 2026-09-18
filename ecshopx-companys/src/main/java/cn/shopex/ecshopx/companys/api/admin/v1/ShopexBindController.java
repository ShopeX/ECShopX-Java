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

import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.service.shopex.ShopexAdminBindService;
import cn.shopex.ecshopx.companys.web.CompanysAdminRequestMerge;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("companysAdminV1ShopexBind")
@RequestMapping("/api/v1")
public class ShopexBindController {

	private final ShopexAdminBindService shopexAdminBindService;

	public ShopexBindController(ShopexAdminBindService shopexAdminBindService) {
		this.shopexAdminBindService = shopexAdminBindService;
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@ShopLog
	@AdminAuth
	@GetMapping(value = "/operator/shopex-bind/status", name = "Shopex绑定状态")
	public ResponseEntity<ApiResult<Map<String, Object>>> status(HttpServletRequest request) {
		long operatorId = requireAdminOperatorId(request, "仅商家超级管理员可查看绑定状态");
		return ResponseEntity.ok(ApiResult.ok(shopexAdminBindService.getStatusForOperatorId(operatorId)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@ShopLog
	@AdminAuth
	@PostMapping(value = "/operator/shopex-bind", name = "Shopex账号绑定")
	public ResponseEntity<ApiResult<Map<String, Object>>> bind(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		long operatorId = requireAdminOperatorId(request, "仅商家超级管理员可绑定 Shopex");
		Map<String, Object> credentials =
				CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body == null ? Map.of() : body);
		return ResponseEntity.ok(ApiResult.ok(shopexAdminBindService.bindForAdminOperator(operatorId, credentials)));
	}

	private static long requireAdminOperatorId(HttpServletRequest request, String nonAdminMessage) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		if (!"admin".equals(String.valueOf(jwt.get("operator_type")))) {
			throw new ForbiddenException(nonAdminMessage);
		}
		Long operatorId = toLong(jwt.get("operator_id"));
		if (operatorId == null || operatorId <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		return operatorId;
	}

	private static Long toLong(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String text = String.valueOf(raw).trim();
		if (text.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(text);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
