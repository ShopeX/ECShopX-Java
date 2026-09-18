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

package cn.shopex.ecshopx.community.api.front.v1.chief;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.community.service.CommunityChiefCashWithdrawalApplyService;
import cn.shopex.ecshopx.community.service.CommunityChiefCashWithdrawalFrontQueryService;
import cn.shopex.ecshopx.community.service.CommunityChiefService;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("communityFrontChiefV1CashWithdrawal")
@RequestMapping("/api/v1/h5app")
public class CommunityChiefCashWithdrawalController {

	private final CommunityChiefCashWithdrawalFrontQueryService communityChiefCashWithdrawalFrontQueryService;
	private final CommunityChiefService communityChiefService;
	private final CommunityChiefCashWithdrawalApplyService communityChiefCashWithdrawalApplyService;

	public CommunityChiefCashWithdrawalController(
			CommunityChiefCashWithdrawalFrontQueryService communityChiefCashWithdrawalFrontQueryService,
			CommunityChiefService communityChiefService,
			CommunityChiefCashWithdrawalApplyService communityChiefCashWithdrawalApplyService) {
		this.communityChiefCashWithdrawalFrontQueryService = communityChiefCashWithdrawalFrontQueryService;
		this.communityChiefService = communityChiefService;
		this.communityChiefCashWithdrawalApplyService = communityChiefCashWithdrawalApplyService;
	}

	@PostMapping(value = "/wxapp/community/chief/cash_withdrawal", name = "团长提现申请")
	public ResponseEntity<ApiResult<Map<String, Object>>> applyCashWithdrawal(
			HttpServletRequest request,
			@RequestParam(name = "money", required = false) String moneyParam,
			@RequestParam(name = "pay_type", required = false) String payTypeParam) {
		long companyId = parseCompanyIdFromRequest(request);
		@SuppressWarnings("unchecked")
		Map<String, Object> claims = requireClaims(request);
		long moneyCentsFloor = parseMoneyCentsFloor(moneyParam);
		Object un = claims.get("username");
		String accountNameFromAuth = un == null ? null : String.valueOf(un);
		Map<String, Object> data =
				communityChiefCashWithdrawalApplyService.applyForH5(
						companyId, claims, moneyCentsFloor, payTypeParam, accountNameFromAuth);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@GetMapping(value = "/wxapp/community/chief/cash_withdrawal", name = "团长提现列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getCashWithdrawalList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "pageSize", required = false) String pageSizeParam) {
		long companyId = parseCompanyIdFromRequest(request);
		@SuppressWarnings("unchecked")
		Map<String, Object> claims = requireClaims(request);
		long chiefId = communityChiefService.resolveChiefIdForH5(companyId, claims);
		int page = parsePageParam(pageParam);
		int pageSize = parsePageSizeParam(pageSizeParam);
		Map<String, Object> data =
				communityChiefCashWithdrawalFrontQueryService.listForChiefH5(companyId, chiefId, page, pageSize);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/wxapp/community/chief/cash_withdrawal/account", name = "提现账户")
	public ResponseEntity<ApiResult<Map<String, Object>>> getCashWithdrawalAccount(HttpServletRequest request) {
		long companyId = parseCompanyIdFromRequest(request);
		@SuppressWarnings("unchecked")
		Map<String, Object> claims = requireClaims(request);
		long chiefId = communityChiefService.resolveChiefIdForH5(companyId, claims);
		Map<String, Object> data = communityChiefService.getCashWithdrawalAccountPayloadForH5(companyId, chiefId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PostMapping(value = "/wxapp/community/chief/cash_withdrawal/account", name = "更新提现账户")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateCashWithdrawalAccount(
			HttpServletRequest request,
			@RequestParam(name = "alipay_name", required = false) String alipayName,
			@RequestParam(name = "alipay_account", required = false) String alipayAccount,
			@RequestParam(name = "bank_name", required = false) String bankName,
			@RequestParam(name = "bankcard_no", required = false) String bankcardNo) {
		long companyId = parseCompanyIdFromRequest(request);
		@SuppressWarnings("unchecked")
		Map<String, Object> claims = requireClaims(request);
		long chiefId = communityChiefService.resolveChiefIdForH5(companyId, claims);
		Map<String, String> updates = new LinkedHashMap<>();
		if (isTruthyRequestValue(alipayName)) {
			updates.put("alipay_name", alipayName == null ? "" : alipayName.trim());
		}
		if (isTruthyRequestValue(alipayAccount)) {
			updates.put("alipay_account", alipayAccount == null ? "" : alipayAccount.trim());
		}
		if (isTruthyRequestValue(bankName)) {
			updates.put("bank_name", bankName == null ? "" : bankName.trim());
		}
		if (isTruthyRequestValue(bankcardNo)) {
			updates.put("bankcard_no", bankcardNo == null ? "" : bankcardNo.trim());
		}
		communityChiefService.updateCashWithdrawalAccountFieldsForH5(chiefId, updates);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@GetMapping(value = "/wxapp/community/chief/cash_withdrawal/count", name = "提现统计")
	public ResponseEntity<ApiResult<Map<String, Object>>> cashWithdrawalCount(HttpServletRequest request) {
		long companyId = parseCompanyIdFromRequest(request);
		@SuppressWarnings("unchecked")
		Map<String, Object> claims = requireClaims(request);
		long chiefId = communityChiefService.resolveChiefIdForH5(companyId, claims);
		Map<String, Object> data =
				communityChiefCashWithdrawalFrontQueryService.buildCashWithdrawalCountForChiefH5(companyId, chiefId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long parseCompanyIdFromRequest(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long cid;
		if (companyAttr instanceof Number n) {
			cid = n.longValue();
		} else if (companyAttr instanceof String s && StringUtils.hasText(s)) {
			try {
				cid = Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
		} else {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		if (cid <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		return cid;
	}

	@SuppressWarnings("unchecked")
	/**
	 * 与 H5 请求 {@code input('key', null)} 真值一致：{@code null}、字面量空串、trim 后为 {@code "0"} 为假；
	 * 含非空白字符的串在 trim 前为真；仅空白字符的串 trim 前非空故为真，写入值为 trim 后（可为空串）。
	 */
	private static boolean isTruthyRequestValue(String raw) {
		if (raw == null || raw.isEmpty()) {
			return false;
		}
		String trimmed = raw.trim();
		if ("0".equals(trimmed)) {
			return false;
		}
		return true;
	}

	private static Map<String, Object> requireClaims(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(raw instanceof Map<?, ?> m)) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		return (Map<String, Object>) m;
	}

	private static int parsePageParam(String pageParam) {
		if (!StringUtils.hasText(pageParam) || !StringUtils.hasText(pageParam.trim())) {
			throw new BadRequestException("分页参数错误");
		}
		int page;
		try {
			page = Integer.parseInt(pageParam.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("分页参数错误");
		}
		if (page < 1) {
			throw new BadRequestException("分页参数错误");
		}
		return page;
	}

	private static long parseMoneyCentsFloor(String moneyParam) {
		if (!StringUtils.hasText(moneyParam) || !StringUtils.hasText(moneyParam.trim())) {
			return 0L;
		}
		try {
			BigDecimal bd = new BigDecimal(moneyParam.trim()).setScale(0, RoundingMode.FLOOR);
			return bd.longValueExact();
		} catch (NumberFormatException | ArithmeticException e) {
			throw new BadRequestException("提现金额格式错误");
		}
	}

	private static int parsePageSizeParam(String pageSizeParam) {
		if (!StringUtils.hasText(pageSizeParam) || !StringUtils.hasText(pageSizeParam.trim())) {
			throw new BadRequestException("每页最多查询50条数据");
		}
		int pageSize;
		try {
			pageSize = Integer.parseInt(pageSizeParam.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("每页最多查询50条数据");
		}
		if (pageSize < 1) {
			throw new BadRequestException("每页最多查询50条数据");
		}
		if (pageSize > 50) {
			throw new BadRequestException("每页最多查询50条数据");
		}
		return pageSize;
	}
}
