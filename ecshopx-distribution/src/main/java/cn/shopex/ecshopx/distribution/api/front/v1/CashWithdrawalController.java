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

package cn.shopex.ecshopx.distribution.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.distribution.service.CashWithdrawalApplyService;
import cn.shopex.ecshopx.distribution.service.CashWithdrawalSalesmanApplyService;
import cn.shopex.ecshopx.distribution.service.CashWithdrawalWxappListService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("distributionFrontV1CashWithdrawal")
@RequestMapping("/api/v1/h5app")
public class CashWithdrawalController {

	private final CashWithdrawalApplyService cashWithdrawalApplyService;
	private final CashWithdrawalSalesmanApplyService cashWithdrawalSalesmanApplyService;
	private final CashWithdrawalWxappListService cashWithdrawalWxappListService;
	private final MessageSource messageSource;

	public CashWithdrawalController(
			CashWithdrawalApplyService cashWithdrawalApplyService,
			CashWithdrawalSalesmanApplyService cashWithdrawalSalesmanApplyService,
			CashWithdrawalWxappListService cashWithdrawalWxappListService,
			MessageSource messageSource) {
		this.cashWithdrawalApplyService = cashWithdrawalApplyService;
		this.cashWithdrawalSalesmanApplyService = cashWithdrawalSalesmanApplyService;
		this.cashWithdrawalWxappListService = cashWithdrawalWxappListService;
		this.messageSource = messageSource;
	}

	@PostMapping(value = "/wxapp/cash_withdrawal", name = "佣金提现申请", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> applyCashWithdrawal(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Object moneyRaw = merged.get("money");
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		Locale locale = request.getLocale();
		Map<String, Object> row =
				cashWithdrawalApplyService.applyCashWithdrawal(companyId, claims, moneyRaw, locale);
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@GetMapping(value = "/wxapp/cash_withdrawals", name = "提现申请列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getCashWithdrawalList(HttpServletRequest request) {
		Map<String, Object> merged = FlexibleHttpServletParameterMap.toObjectMap(request);
		Object pageObj = merged.get("page");
		Object pageSizeObj = merged.get("pageSize");
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		String userId = Objects.toString(claims.get("user_id"), "").trim();
		if (!StringUtils.hasText(userId)) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		Locale locale = request.getLocale();
		Map<String, Object> data =
				cashWithdrawalWxappListService.getCashWithdrawalList(companyId, userId, pageObj, pageSizeObj, locale);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PostMapping(
			value = "/wxapp/salesman/applyCashWithdrawal",
			name = "业务员佣金提现申请",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> salesmanApplyCashWithdrawal(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		Locale locale = request.getLocale();
		long distributorId = parsePositiveLongOrZero(merged.get("distributor_id"));
		if (distributorId <= 0L) {
			throw new ResourceException(
					messageSource.getMessage(
							"distribution.cash_withdrawal.select_distributor_first",
							null,
							"请选择店铺后提现",
							locale));
		}
		Object moneyRaw = merged.get("money");
		Map<String, Object> row = cashWithdrawalSalesmanApplyService.salesmanApplyCashWithdrawal(
				companyId, distributorId, claims, moneyRaw, locale);
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@GetMapping(value = "/wxapp/salesman/getCashWithdrawalList", name = "业务员提现申请列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> salesmanGetCashWithdrawalList(HttpServletRequest request) {
		Map<String, Object> merged = FlexibleHttpServletParameterMap.toObjectMap(request);
		Object pageObj = merged.get("page");
		Object pageSizeObj = merged.get("pageSize");
		Object distributorIdObj = merged.get("distributor_id");
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		String userId = Objects.toString(claims.get("user_id"), "").trim();
		if (!StringUtils.hasText(userId)) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		Locale locale = request.getLocale();
		Map<String, Object> data = cashWithdrawalWxappListService.salesmanGetCashWithdrawalList(
				companyId, userId, pageObj, pageSizeObj, distributorIdObj, locale);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readH5AuthClaimsMap(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (raw instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		return Collections.emptyMap();
	}

	private static long resolveCompanyId(HttpServletRequest request, Map<String, Object> claims) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long fromAttr = parsePositiveLongOrZero(companyAttr);
		if (fromAttr > 0L) {
			return fromAttr;
		}
		long fromClaims = parsePositiveLongOrZero(claims != null ? claims.get("company_id") : null);
		if (fromClaims > 0L) {
			return fromClaims;
		}
		throw new UnauthorizedException("Unable to authenticate user.");
	}

	private static long parsePositiveLongOrZero(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof String s && StringUtils.hasText(s)) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
