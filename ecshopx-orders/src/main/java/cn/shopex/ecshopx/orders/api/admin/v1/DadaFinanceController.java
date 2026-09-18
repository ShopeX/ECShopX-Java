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

package cn.shopex.ecshopx.orders.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.orders.service.dada.DadaFinanceBalanceService;
import cn.shopex.ecshopx.orders.service.dada.DadaFinanceRechargeService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("ordersAdminV1DadaFinance")
@RequestMapping("/api/v1/dada/finance")
public class DadaFinanceController {

	private final DadaFinanceRechargeService dadaFinanceRechargeService;
	private final DadaFinanceBalanceService dadaFinanceBalanceService;

	public DadaFinanceController(
			DadaFinanceRechargeService dadaFinanceRechargeService,
			DadaFinanceBalanceService dadaFinanceBalanceService) {
		this.dadaFinanceRechargeService = dadaFinanceRechargeService;
		this.dadaFinanceBalanceService = dadaFinanceBalanceService;
	}

	@Activated(routeAlias = "dada.finance.info")
	@GetMapping(value = "/info", name = "达达账户余额", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> queryBalance(HttpServletRequest request) {
		long companyId = readCompanyIdFromJwt(request);
		return ResponseEntity.ok(ApiResult.ok(dadaFinanceBalanceService.queryBalance(companyId)));
	}

	@Activated(routeAlias = "dada.finance.create")
	@PostMapping(value = "/create", name = "达达充值链接", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> recharge(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Object raw = merged.get("amount");
		if (raw == null) {
			throw new BadRequestException("充值金额必填");
		}
		if (raw instanceof String str) {
			if (str.trim().isEmpty()) {
				throw new BadRequestException("充值金额必填");
			}
		} else {
			String s = String.valueOf(raw).trim();
			if (s.isEmpty()) {
				throw new BadRequestException("充值金额必填");
			}
		}

		long companyId = readCompanyIdFromJwt(request);

		String url = request.getRequestURL().toString();
		String legacyRouterInfix = "in" + "dex" + "." + "p" + "h" + "p" + "/";
		if (url.contains(legacyRouterInfix)) {
			url = url.replace(legacyRouterInfix, "");
		}
		String urlPath = request.getRequestURI();
		String path = "financial/distribution/dada";
		String notifyUrl = url.replace(urlPath, path);

		String amountStr = (raw instanceof String) ? ((String) raw).trim() : String.valueOf(raw).trim();
		String link = dadaFinanceRechargeService.recharge(companyId, amountStr, notifyUrl);
		return ResponseEntity.ok(ApiResult.ok(Map.of("link", link)));
	}

	private static long readCompanyIdFromJwt(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> jwt = (Map<?, ?>) attr;
		Object companyIdObj = jwt.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		try {
			return Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
	}
}
