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

package cn.shopex.ecshopx.hfpay.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.hfpay.service.bank.HfpayBankGetInfoService;
import cn.shopex.ecshopx.hfpay.service.bank.HfpayBankListService;
import cn.shopex.ecshopx.hfpay.service.bank.HfpayBankSaveService;
import cn.shopex.ecshopx.hfpay.service.bank.HfpayBankUnbindService;
import cn.shopex.ecshopx.hfpay.service.enterapply.HfpayHfFileRequestMergeService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
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
@RestController("hfpayBankFrontV1")
@RequestMapping("/api/v1/h5app")
public class HfpayBankController {

	private final HfpayBankUnbindService hfpayBankUnbindService;
	private final HfpayBankSaveService hfpayBankSaveService;
	private final HfpayHfFileRequestMergeService hfpayHfFileRequestMergeService;
	private final HfpayBankGetInfoService hfpayBankGetInfoService;
	private final HfpayBankListService hfpayBankListService;

	public HfpayBankController(
			HfpayBankUnbindService hfpayBankUnbindService,
			HfpayBankSaveService hfpayBankSaveService,
			HfpayHfFileRequestMergeService hfpayHfFileRequestMergeService,
			HfpayBankGetInfoService hfpayBankGetInfoService,
			HfpayBankListService hfpayBankListService) {
		this.hfpayBankUnbindService = hfpayBankUnbindService;
		this.hfpayBankSaveService = hfpayBankSaveService;
		this.hfpayHfFileRequestMergeService = hfpayHfFileRequestMergeService;
		this.hfpayBankGetInfoService = hfpayBankGetInfoService;
		this.hfpayBankListService = hfpayBankListService;
	}

	@GetMapping(value = "/wxapp/hfpay/bankinfo", name = "获取单条银行卡信息")
	public ResponseEntity<ApiResult<Object>> getInfo(
			HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false) String distributorId) {
		long companyId = parseCompanyIdFromRequest(request);
		Long optionalUserId = parseOptionalAuthUserIdFromRequest(request);
		Object data = hfpayBankGetInfoService.getBankCardAsResponseData(companyId, optionalUserId, distributorId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/wxapp/hfpay/banklist", name = "获取多条银行卡信息")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getList(
			HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false) String distributorId) {
		long companyId = parseCompanyIdFromRequest(request);
		Long optionalUserId = parseOptionalAuthUserIdFromRequest(request);
		List<Map<String, Object>> data = hfpayBankListService.listBankCardsAsMaps(companyId, optionalUserId, distributorId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PostMapping(value = "/wxapp/hfpay/banksave", name = "保存提现银行卡")
	public ResponseEntity<ApiResult<Map<String, Object>>> save(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = parseCompanyIdFromRequest(request);
		long userId = parseRequiredAuthUserIdFromRequest(request);
		Map<String, Object> merged = hfpayHfFileRequestMergeService.merge(request, body);
		Map<String, Object> data = hfpayBankSaveService.saveWithdrawBankCard(companyId, userId, merged);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PostMapping(value = "/wxapp/hfpay/bankdel", name = "解除并删除绑定银行卡")
	public ResponseEntity<ApiResult<Map<String, Object>>> unBindBank(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = parseCompanyIdFromRequest(request);
		Long optionalUserId = parseOptionalAuthUserIdFromRequest(request);
		Map<String, Object> merged = hfpayHfFileRequestMergeService.merge(request, body);
		boolean result = hfpayBankUnbindService.unbindBank(companyId, optionalUserId, merged);
		return ResponseEntity.ok(ApiResult.ok(Map.of("result", result)));
	}

	private static long parseCompanyIdFromRequest(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (companyAttr instanceof Number n) {
			companyId = n.longValue();
		} else if (companyAttr instanceof String s && !s.isBlank()) {
			try {
				companyId = Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
		} else {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		return companyId;
	}

	private static long parseRequiredAuthUserIdFromRequest(HttpServletRequest request) {
		Object rawClaims = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(rawClaims instanceof Map<?, ?> rawMap)) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> claims = (Map<String, Object>) rawMap;
		Object claimUid = claims.get("user_id");
		if (claimUid == null) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		long userId = parseLongFlexible(claimUid, 0L);
		if (userId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		return userId;
	}

	private static Long parseOptionalAuthUserIdFromRequest(HttpServletRequest request) {
		Object rawClaims = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(rawClaims instanceof Map<?, ?> rawMap)) {
			return null;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> claims = (Map<String, Object>) rawMap;
		Object claimUid = claims.get("user_id");
		if (claimUid == null || !authUserIdTruthy(claimUid)) {
			return null;
		}
		long userId = parseLongFlexible(claimUid, 0L);
		if (userId <= 0L) {
			return null;
		}
		return userId;
	}

	private static boolean authUserIdTruthy(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Number n) {
			return n.longValue() > 0L;
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			return false;
		}
		try {
			return Long.parseLong(s) > 0L;
		} catch (NumberFormatException e) {
			return true;
		}
	}

	private static long parseLongFlexible(Object raw, long defaultVal) {
		if (raw == null) {
			return defaultVal;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}
}
