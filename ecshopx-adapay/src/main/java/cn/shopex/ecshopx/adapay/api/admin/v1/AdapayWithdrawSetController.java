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

import cn.shopex.ecshopx.adapay.service.AdapayWithdrawSetSaveService;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
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
		unauthorized = false,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("adapayWithdrawSetAdminV1")
@RequestMapping("/api/v1/adapay")
public class AdapayWithdrawSetController {

	private final AdapayWithdrawSetSaveService adapayWithdrawSetSaveService;

	public AdapayWithdrawSetController(AdapayWithdrawSetSaveService adapayWithdrawSetSaveService) {
		this.adapayWithdrawSetSaveService = adapayWithdrawSetSaveService;
	}

	@Activated(routeAlias = "adapay.withdraw.get")
	@GetMapping(value = "/withdrawset", name = "获取提现设置")
	public ResponseEntity<ApiResult<Map<String, Object>>> index(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		long distributorId = parseDistributorIdForIndex(jwtMap);
		Map<String, Object> data = adapayWithdrawSetSaveService.index(companyId, distributorId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long parseDistributorIdForIndex(Map<String, Object> jwtMap) {
		Object distRaw = jwtMap.get("distributor_id");
		if (distRaw == null) {
			throw new ResourceException("请选择店铺");
		}
		if (distRaw instanceof String s) {
			if (s.isEmpty() || "0".equals(s)) {
				throw new ResourceException("请选择店铺");
			}
			try {
				long v = Long.parseLong(s.trim());
				if (v <= 0L) {
					throw new ResourceException("请选择店铺");
				}
				return v;
			} catch (NumberFormatException e) {
				throw new ResourceException("请选择店铺");
			}
		}
		if (distRaw instanceof Number n) {
			if (n.longValue() <= 0L) {
				throw new ResourceException("请选择店铺");
			}
			return n.longValue();
		}
		String t = String.valueOf(distRaw);
		if (t.isEmpty() || "0".equals(t)) {
			throw new ResourceException("请选择店铺");
		}
		try {
			long v = Long.parseLong(t);
			if (v <= 0L) {
				throw new ResourceException("请选择店铺");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new ResourceException("请选择店铺");
		}
	}

	@Activated(routeAlias = "adapay.withdraw.save")
	@PostMapping(value = "/withdrawset", name = "提现设置")
	public ResponseEntity<ApiResult<Map<String, Object>>> save(
			HttpServletRequest request,
			@RequestParam(value = "isAuto", required = false) String isAutoParam,
			@RequestParam(value = "cash_amt", required = false) String cashAmtParam,
			@RequestParam(value = "cash_type", required = false) String cashTypeParam,
			@RequestParam(value = "distributor_id", required = false) String distributorIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw)) {
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
		long jwtDistributorId = 0L;
		Object distRaw = jwtMap.get("distributor_id");
		if (distRaw instanceof Number) {
			jwtDistributorId = ((Number) distRaw).longValue();
		}
		if (jwtDistributorId <= 0L) {
			throw new ResourceException("请选择店铺");
		}

		Map<String, Object> merged = body != null ? new LinkedHashMap<>(body) : new LinkedHashMap<>();
		mergeParam(merged, "isAuto", isAutoParam);
		mergeParam(merged, "cash_amt", cashAmtParam);
		mergeParam(merged, "cash_type", cashTypeParam);
		mergeParam(merged, "distributor_id", distributorIdParam);

		adapayWithdrawSetSaveService.save(companyId, jwtDistributorId, jwtOperatorId, merged);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	private static void mergeParam(Map<String, Object> merged, String key, String requestParam) {
		if (StringUtils.hasText(requestParam != null ? requestParam.trim() : null)) {
			merged.put(key, requestParam.trim());
		}
	}
}
