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

import cn.shopex.ecshopx.adapay.service.AdapayDrawCashWithdrawService;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.goods.web.DatapassBlockResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
@RestController("adapayDrawCashAdminV1")
@RequestMapping("/api/v1/adapay")
public class AdapayDrawCashController {

	private final AdapayDrawCashWithdrawService adapayDrawCashWithdrawService;

	public AdapayDrawCashController(AdapayDrawCashWithdrawService adapayDrawCashWithdrawService) {
		this.adapayDrawCashWithdrawService = adapayDrawCashWithdrawService;
	}

	@DataPass
	@Activated(routeAlias = "adapay.drawcash.getList")
	@GetMapping(value = "/drawcash/getList", name = "提现记录")
	public ResponseEntity<ApiResult<Map<String, Object>>> getList(
			HttpServletRequest request,
			@RequestParam(value = "params", required = false) String params) {
		OperatorJwtClaims jwt = readOperatorJwtClaims(request);
		boolean blocked = DatapassBlockResolver.isBlocked(request);
		Map<String, Object> data =
				adapayDrawCashWithdrawService.getList(
						jwt.companyId(),
						jwt.operatorId(),
						jwt.distributorId(),
						jwt.operatorType(),
						params,
						blocked);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "adapay.withdraw")
	@PostMapping(value = "/withdraw", name = "汇付提现申请")
	public ResponseEntity<ApiResult<Map<String, Object>>> withdraw(
			HttpServletRequest request,
			@RequestParam(value = "cash_amt", required = false) String cashAmtParam,
			@RequestParam(value = "cash_type", required = false) String cashTypeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		OperatorJwtClaims jwt = readOperatorJwtClaims(request);
		String cashAmt = resolveWithdrawInput(cashAmtParam, body, "cash_amt");
		String cashType = resolveWithdrawInput(cashTypeParam, body, "cash_type");
		adapayDrawCashWithdrawService.withdraw(
				jwt.companyId(),
				jwt.operatorId(),
				jwt.distributorId(),
				jwt.operatorType(),
				cashAmt,
				cashType);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	private record OperatorJwtClaims(
			long companyId, long operatorId, Long distributorId, String operatorType) {}

	private static OperatorJwtClaims readOperatorJwtClaims(HttpServletRequest request) {
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
		long jwtOperatorId = 0L;
		Object operatorIdRaw = jwtMap.get("operator_id");
		if (operatorIdRaw instanceof Number) {
			jwtOperatorId = ((Number) operatorIdRaw).longValue();
		}
		String jwtOperatorType = "";
		Object jwtOpTypeRaw = jwtMap.get("operator_type");
		if (jwtOpTypeRaw != null) {
			jwtOperatorType = String.valueOf(jwtOpTypeRaw);
		}
		Long jwtDistributorId = null;
		Object distRaw = jwtMap.get("distributor_id");
		if (distRaw instanceof Number) {
			jwtDistributorId = ((Number) distRaw).longValue();
		}
		return new OperatorJwtClaims(companyId, jwtOperatorId, jwtDistributorId, jwtOperatorType);
	}

	private static String resolveWithdrawInput(
			String param, Map<String, Object> body, String bodyKey) {
		if (StringUtils.hasText(param != null ? param.trim() : null)) {
			return param.trim();
		}
		if (body == null) {
			return null;
		}
		Object raw = body.get(bodyKey);
		if (raw == null) {
			return null;
		}
		if (raw instanceof String s) {
			return StringUtils.hasText(s) ? s.trim() : null;
		}
		return String.valueOf(raw);
	}
}
