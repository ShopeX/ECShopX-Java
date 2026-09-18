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

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.adapay.service.AdapaySubMerchantDrawCashConfigService;
import cn.shopex.ecshopx.adapay.service.AdapaySubMerchantSaveSplitLedgerService;
import cn.shopex.ecshopx.adapay.service.AdapaySubMerchantSubApproveInfoService;
import cn.shopex.ecshopx.adapay.service.AdapaySubMerchantSubApproveListService;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@RestController("subMerchantAdminV1")
@RequestMapping("/api/v1/adapay")
public class SubMerchantController {

	private final AdapaySubMerchantDrawCashConfigService adapaySubMerchantDrawCashConfigService;
	private final AdapaySubMerchantSaveSplitLedgerService adapaySubMerchantSaveSplitLedgerService;
	private final AdapaySubMerchantSubApproveInfoService adapaySubMerchantSubApproveInfoService;
	private final AdapaySubMerchantSubApproveListService adapaySubMerchantSubApproveListService;
	private final LangueProperties langueProperties;

	public SubMerchantController(
			AdapaySubMerchantDrawCashConfigService adapaySubMerchantDrawCashConfigService,
			AdapaySubMerchantSaveSplitLedgerService adapaySubMerchantSaveSplitLedgerService,
			AdapaySubMerchantSubApproveInfoService adapaySubMerchantSubApproveInfoService,
			AdapaySubMerchantSubApproveListService adapaySubMerchantSubApproveListService,
				LangueProperties langueProperties) {
		this.adapaySubMerchantDrawCashConfigService = adapaySubMerchantDrawCashConfigService;
		this.adapaySubMerchantSaveSplitLedgerService = adapaySubMerchantSaveSplitLedgerService;
		this.adapaySubMerchantSubApproveInfoService = adapaySubMerchantSubApproveInfoService;
		this.adapaySubMerchantSubApproveListService = adapaySubMerchantSubApproveListService;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "adapay.sub_approve.list")
	@GetMapping(value = "/sub_approve/list", name = "子商户审批列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> subApproveLists(
			HttpServletRequest request,
			@RequestParam(name = "status", required = false) String status,
			@RequestParam(name = "user_name", required = false) String userName,
			@RequestParam(name = "address", required = false) String address,
			@RequestParam(name = "time_start", required = false) String timeStart,
			@RequestParam(name = "time_end", required = false) String timeEnd,
			@RequestParam(name = "page", required = false, defaultValue = "1") int page,
			@RequestParam(name = "pageSize", required = false) Integer pageSize,
			@RequestParam(name = "page_size", required = false) Integer pageSizeSnake) {
		long companyId = resolveCompanyId(request);
		int effectivePageSize = pageSize != null ? pageSize.intValue() : 20;
		Map<String, Object> data =
				adapaySubMerchantSubApproveListService.subApproveLists(
						companyId, status, userName, address, timeStart, timeEnd, page, effectivePageSize);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DataPass
	@Activated(routeAlias = "adapay.sub_approve.info")
	@GetMapping(value = "/sub_approve/info/{id}", name = "子商户审批详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> subApproveInfo(
			HttpServletRequest request, @PathVariable("id") String id) {
		long companyId = resolveCompanyId(request);
		String requestLang = RequestLangTag.current(langueProperties);
		Map<String, Object> result =
				adapaySubMerchantSubApproveInfoService.subApproveInfo(companyId, id, requestLang);
		if (parseDatapassBlock(request)) {
			applySubApproveDatapassMask(result);
		}
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "adapay.sub_approve.save_split_ledger")
	@PostMapping(value = "/sub_approve/save_split_ledger", name = "子商户审批保存分账信息")
	public ResponseEntity<ApiResult<Map<String, Object>>> saveSplitLedger(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
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
		Object opRaw = jwtMap.get("operator_id");
		if (opRaw instanceof Number) {
			jwtOperatorId = ((Number) opRaw).longValue();
		}

		adapaySubMerchantSaveSplitLedgerService.saveSplitLedger(
				companyId, jwtOperatorId, body == null ? Map.of() : body);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "adapay.sub_approve.draw_limit_set")
	@PostMapping(value = "/sub_approve/draw_limit", name = "保存子商户提现限额")
	public ResponseEntity<ApiResult<Map<String, Object>>> setDrawLimit(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
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

		adapaySubMerchantDrawCashConfigService.setDrawLimit(companyId, body == null ? null : body.get("draw_limit"));
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "adapay.sub_approve.draw_limit_get")
	@GetMapping(value = "/sub_approve/draw_limit", name = "获取子商户提现限额")
	public ResponseEntity<ApiResult<Object>> getDrawLimit(HttpServletRequest request) {
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
		Object payload = adapaySubMerchantDrawCashConfigService.getDrawLimit(companyId);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@Activated(routeAlias = "adapay.sub_approve.draw_limit_set")
	@PostMapping(value = "/sub_approve/draw_cash_config", name = "保存子商户提现限额")
	public ResponseEntity<ApiResult<Map<String, Object>>> setDrawCashConfig(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
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

		adapaySubMerchantDrawCashConfigService.setDrawCashConfig(companyId, body == null ? Map.of() : body);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "adapay.sub_approve.draw_limit_get")
	@GetMapping(value = "/sub_approve/draw_cash_config", name = "获取子商户提现限额")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDrawCashConfig(HttpServletRequest request) {
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
		Map<String, Object> data = adapaySubMerchantDrawCashConfigService.getDrawCashConfig(companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long resolveCompanyId(HttpServletRequest request) {
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
		return ((Number) companyIdRaw).longValue();
	}

	private static boolean parseDatapassBlock(HttpServletRequest request) {
		Object attr = request.getAttribute("x-datapass-block");
		if (attr instanceof Number n && n.intValue() != 0) {
			return true;
		}
		if (Boolean.TRUE.equals(attr)) {
			return true;
		}
		if (attr != null) {
			String t = attr.toString().trim();
			if (!t.isEmpty() && !"0".equals(t) && !"false".equalsIgnoreCase(t)) {
				return true;
			}
		}
		String p = request.getParameter("x-datapass-block");
		if (p == null || p.trim().isEmpty() || "0".equals(p.trim()) || "false".equalsIgnoreCase(p.trim())) {
			return false;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	private static void applySubApproveDatapassMask(
			Map<String, Object> result) {
		Map<String, Object> entryInfo = (Map<String, Object>) result.get("entry_info");
		if (entryInfo != null && !entryInfo.isEmpty()) {
			putMaskedMobile(entryInfo, "tel_no");
			String memberType = String.valueOf(entryInfo.getOrDefault("member_type", "")).trim();
			if ("corp".equals(memberType)) {
				putMaskedTruename(entryInfo, "legal_person");
				putMaskedBankcard(entryInfo, "card_no");
				putMaskedIdcard(entryInfo, "legal_cert_id");
			} else {
				putMaskedTruename(entryInfo, "user_name");
				putMaskedIdcard(entryInfo, "cert_id");
				putMaskedTruename(entryInfo, "bank_card_name");
				putMaskedMobile(entryInfo, "bank_tel_no");
				putMaskedBankcard(entryInfo, "bank_card_id");
				putMaskedIdcard(entryInfo, "bank_cert_id");
			}
		}
		Map<String, Object> entryApplyInfo = (Map<String, Object>) result.get("entry_apply_info");
		if (entryApplyInfo != null && !entryApplyInfo.isEmpty()) {
			putMaskedTruename(entryApplyInfo, "user_name");
		}
		Map<String, Object> dealerInfoMap = (Map<String, Object>) result.get("dealer_info");
		if (dealerInfoMap != null && !dealerInfoMap.isEmpty()) {
			putMaskedMobile(dealerInfoMap, "mobile");
		}
		Map<String, Object> distributorInfo = (Map<String, Object>) result.get("distributor_info");
		if (distributorInfo != null && !distributorInfo.isEmpty()) {
			putMaskedMobile(distributorInfo, "mobile");
		}
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static void putMaskedTruename(
			Map<String, Object> map, String key) {
		String s = str(map.get(key));
		if (s.trim().isEmpty()) {
			return;
		}
		map.put(key, DataMasking.maskTruename(s));
	}

	private static void putMaskedIdcard(Map<String, Object> map, String key) {
		String s = str(map.get(key));
		if (s.trim().isEmpty()) {
			return;
		}
		map.put(key, DataMasking.maskIdcard(s));
	}

	private static void putMaskedMobile(Map<String, Object> map, String key) {
		String s = str(map.get(key));
		if (s.trim().isEmpty()) {
			return;
		}
		map.put(key, DataMasking.maskMobile(s));
	}

	private static void putMaskedBankcard(Map<String, Object> map, String key) {
		String s = str(map.get(key));
		if (s.trim().isEmpty()) {
			return;
		}
		map.put(key, DataMasking.maskBankcard(s));
	}
}
