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

package cn.shopex.ecshopx.kaquan.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.kaquan.service.membercard.MemberCardGradeQueryService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeBuyDistributorShopResolveService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeListQueryService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeOrderBuyCreateService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeUserVipGradeGetService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.pay.VipGradeMembercardPaymentOrchestratorService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.members.service.stats.MemberTotalConsumptionReadService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
@RestController("kaquanVipGradeFrontV1")
@RequestMapping("/api/v1/h5app")
public class VipGradeController {

	private static final Set<String> ADAPAY_BSPAY = Set.of("adapay", "bspay");

	private static final String MSG_H5_COMPANY_UNAUTHORIZED = "无权访问该API,非法访问！";

	private final VipGradeOrderBuyCreateService vipGradeOrderBuyCreateService;
	private final VipGradeBuyDistributorShopResolveService vipGradeBuyDistributorShopResolveService;
	private final VipGradeMembercardPaymentOrchestratorService vipGradeMembercardPaymentOrchestratorService;
	private final MemberAccountService memberAccountService;
	private final MemberCardGradeQueryService memberCardGradeQueryService;
	private final VipGradeListQueryService vipGradeListQueryService;
	private final MemberTotalConsumptionReadService memberTotalConsumptionReadService;
	private final CompanyDefaultCurrencyService companyDefaultCurrencyService;
	private final VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService;

	public VipGradeController(
			VipGradeOrderBuyCreateService vipGradeOrderBuyCreateService,
			VipGradeBuyDistributorShopResolveService vipGradeBuyDistributorShopResolveService,
			VipGradeMembercardPaymentOrchestratorService vipGradeMembercardPaymentOrchestratorService,
			MemberAccountService memberAccountService,
			MemberCardGradeQueryService memberCardGradeQueryService,
			VipGradeListQueryService vipGradeListQueryService,
			MemberTotalConsumptionReadService memberTotalConsumptionReadService,
			CompanyDefaultCurrencyService companyDefaultCurrencyService,
			VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService) {
		this.vipGradeOrderBuyCreateService = vipGradeOrderBuyCreateService;
		this.vipGradeBuyDistributorShopResolveService = vipGradeBuyDistributorShopResolveService;
		this.vipGradeMembercardPaymentOrchestratorService = vipGradeMembercardPaymentOrchestratorService;
		this.memberAccountService = memberAccountService;
		this.memberCardGradeQueryService = memberCardGradeQueryService;
		this.vipGradeListQueryService = vipGradeListQueryService;
		this.memberTotalConsumptionReadService = memberTotalConsumptionReadService;
		this.companyDefaultCurrencyService = companyDefaultCurrencyService;
		this.vipGradeUserVipGradeGetService = vipGradeUserVipGradeGetService;
	}

	@GetMapping("/wxapp/vipgrades/uservip")
	public ResponseEntity<ApiResult<Map<String, Object>>> getUserVipGrade(HttpServletRequest request) {
		Object rawClaims = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(rawClaims instanceof Map<?, ?>)) {
			throw new ResourceException("获取会员信息失败，信息有误");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> claims = (Map<String, Object>) rawClaims;
		long userId = parsePositiveUserIdFromClaimsForMemberOrThrow(claims);
		long companyId = parsePositiveCompanyIdFromRequestOrThrow(request);
		String mobileStr = resolveH5BuyerMobile(claims, userId, companyId);
		if (!StringUtils.hasText(mobileStr)) {
			throw new ResourceException("获取会员信息失败，信息有误");
		}
		Map<String, Object> vipgrade = vipGradeUserVipGradeGetService.userVipGradeGet(companyId, userId, false);
		if (vipgrade != null && vipgrade.containsKey("is_open")) {
			Object v = vipgrade.get("is_open");
			if (!isVipGradeOpenForUserVipResponse(v)) {
				Map<String, Object> payload = new LinkedHashMap<>();
				payload.put("grade_name", "");
				return ResponseEntity.ok(ApiResult.ok(payload));
			}
		}
		return ResponseEntity.ok(ApiResult.ok(vipgrade));
	}

	@PostMapping("/wxapp/vipgrades/buy")
	public ResponseEntity<ApiResult<Map<String, Object>>> buyDataVipGrade(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body,
			@RequestParam Map<String, String> queryParams) {
		Map<String, Object> merged = mergeBuyDataVipGradeInput(request, body);
		queryParams.forEach((k, v) -> merged.putIfAbsent(k, v));

		String vipGradeId = stringTrimmed(merged.get("vip_grade_id"));
		if (!StringUtils.hasText(vipGradeId)) {
			throw new ResourceException("会员卡购买失败.");
		}
		String cardType = stringTrimmed(merged.get("card_type"));
		if (!StringUtils.hasText(cardType)) {
			throw new ResourceException("会员卡购买失败.");
		}

		Map<String, Object> claims = readH5ClaimsOrThrow(request);
		long userId = parsePositiveUserIdFromClaimsOrThrow(claims);
		long companyId = parsePositiveCompanyIdFromRequestOrThrow(request);
		String mobileStr = resolveH5BuyerMobile(claims, userId, companyId);
		if (!StringUtils.hasText(mobileStr)) {
			throw new ResourceException("购买信息有误");
		}

		long distributorId = parseLongFlexible(merged.get("distributor_id"), 0L);

		Map<String, Object> result = vipGradeOrderBuyCreateService.createDataForBuy(
				companyId, userId, mobileStr, vipGradeId, cardType, distributorId);
		if (result == null || result.isEmpty()) {
			throw new ResourceException("购买失败");
		}

		long shopIdFromInput = readShopIdDefaultZero(merged);
		long finalShopId =
				distributorId != 0L
						? vipGradeBuyDistributorShopResolveService.resolveShopId(companyId, distributorId, shopIdFromInput)
						: shopIdFromInput;

		String openId = claims.get("open_id") == null ? "" : String.valueOf(claims.get("open_id")).trim();
		String wxaAppidClaim =
				claims.get("wxapp_appid") == null ? "" : String.valueOf(claims.get("wxapp_appid")).trim();
		String woaAppId = claims.get("woa_appid") == null ? "" : String.valueOf(claims.get("woa_appid")).trim();
		String wxaAppIdForPayment = wxaAppidClaim;

		Object payTypeRaw = merged.get("pay_type");
		String payType =
				payTypeRaw == null || payTypeRaw.toString().trim().isEmpty()
						? "wxpay"
						: payTypeRaw.toString().trim();
		String payTypeLc = payType.toLowerCase(Locale.ROOT);

		int priceFen = intFromResult(result.get("price"));
		int payFeeFen = intFromResult(result.get("total_fee"));
		Object orderIdObj = result.get("order_id");
		if (orderIdObj == null) {
			throw new ResourceException("购买失败");
		}
		String orderId = orderIdObj.toString().trim();

		String title = result.get("title") == null ? "" : String.valueOf(result.get("title")).trim();

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("company_id", companyId);
		data.put("user_id", userId);
		data.put("total_fee", priceFen);
		data.put("pay_fee", payFeeFen);
		data.put("detail", title);
		data.put("body", title);
		data.put("order_id", orderId);
		data.put("mobile", mobileStr);
		data.put("pay_type", payTypeLc);
		data.put("fee_type", result.get("fee_type"));
		data.put("fee_rate", result.get("fee_rate"));
		data.put("fee_symbol", result.get("fee_symbol"));
		data.put("distributor_id", distributorId);
		data.put("shop_id", finalShopId);
		data.put("trade_source_type", "membercard");
		Object returnUrl = merged.get("return_url");
		data.put("return_url", returnUrl == null ? "" : String.valueOf(returnUrl).trim());
		data.put("open_id", openId);
		data.put("wxa_appid", wxaAppIdForPayment);
		data.put("authorizer_appid", woaAppId);

		if (ADAPAY_BSPAY.contains(payTypeLc)) {
			Object payChannelRaw = merged.get("pay_channel");
			String payChannel = payChannelRaw == null ? "" : payChannelRaw.toString().trim();
			if (!StringUtils.hasText(payChannel)) {
				throw new BadRequestException(payTypeLc + "请选择支付渠道");
			}
			data.put("pay_channel", payChannel);
		}

		if ("alipaymini".equals(payTypeLc)) {
			Object ali = claims.get("alipay_user_id");
			if (ali == null || ali.toString().trim().isEmpty()) {
				throw new BadRequestException("请在支付宝小程序授权登录");
			}
			data.put("alipay_user_id", ali.toString().trim());
		}

		Map<String, Object> payResult =
				vipGradeMembercardPaymentOrchestratorService.pay(woaAppId, wxaAppIdForPayment, data, false);
		payResult.put("pay_fee", data.get("pay_fee"));
		payResult.put("total_fee", data.get("total_fee"));
		return ResponseEntity.ok(ApiResult.ok(payResult));
	}

	@FrontNoAuth
	@GetMapping(value = "/wxapp/vipgrades/list", name = "会员等级列表")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> listDataVipGrade(HttpServletRequest request) {
		long companyId = parsePositiveCompanyIdForH5FrontNoAuthList(request);
		List<Map<String, Object>> rows = loadWxappVipGradeRows(companyId);
		return ResponseEntity.ok(ApiResult.ok(rows));
	}

	@FrontNoAuth
	@GetMapping(value = "/wxapp/vipgrades/newlist", name = "会员等级列表(newlist)")
	public ResponseEntity<ApiResult<Map<String, Object>>> listDataVipGradeNewlist(HttpServletRequest request) {
		long companyId = parsePositiveCompanyIdForH5FrontNoAuthList(request);
		List<Map<String, Object>> rows = loadWxappVipGradeRows(companyId);
		CurrencyExchangeRate curEntity = companyDefaultCurrencyService.getCur(companyId);
		Map<String, Object> curMap = companyDefaultCurrencyService.toCurResponseMap(curEntity);
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("list", rows);
		payload.put("cur", curMap);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	private List<Map<String, Object>> loadWxappVipGradeRows(long companyId) {
		List<Map<String, Object>> rows = vipGradeListQueryService.listVipGradesForWxappMembercardGrades(companyId);
		applyVipGradePriceListFilter(rows);
		return rows;
	}

	@FrontNoAuth
	@GetMapping("/wxapp/membercard/grades")
	public ResponseEntity<ApiResult<Map<String, Object>>> getGradeList(HttpServletRequest request) {
		long companyId = parsePositiveCompanyIdForH5FrontNoAuthList(request);
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("member_card_list", memberCardGradeQueryService.getGradeListByCompanyId(companyId, false));
		result.put("vip_grade_list", vipGradeListQueryService.listVipGradesForWxappMembercardGrades(companyId));
		Long uid = parseOptionalPositiveUserIdFromRequest(request);
		if (uid != null) {
			result.put("total_consumption", memberTotalConsumptionReadService.getTotalConsumption(uid));
		}
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	private static void applyVipGradePriceListFilter(List<Map<String, Object>> rows) {
		if (rows == null) {
			return;
		}
		for (Map<String, Object> list : rows) {
			if (list == null) {
				continue;
			}
			Object pl = list.get("price_list");
			if (pl == null) {
				continue;
			}
			if (!(pl instanceof List<?> priceListRaw)) {
				continue;
			}
			if (priceListRaw.isEmpty()) {
				continue;
			}
			List<Map<String, Object>> pricelist = new ArrayList<>();
			for (Object elem : priceListRaw) {
				if (!(elem instanceof Map<?, ?> em)) {
					continue;
				}
				@SuppressWarnings("unchecked")
				Map<String, Object> item = (Map<String, Object>) em;
				if (isTruthyPriceListPriceValue(item.get("price"))) {
					pricelist.add(item);
				}
			}
			list.put("price_list", pricelist);
		}
	}

	private static boolean isTruthyPriceListPriceValue(Object price) {
		if (price == null) {
			return false;
		}
		if (price instanceof Boolean b) {
			return b;
		}
		if (price instanceof Number n) {
			double d = n.doubleValue();
			return d != 0.0d && !Double.isNaN(d);
		}
		if (price instanceof String s) {
			if (s.isEmpty()) {
				return false;
			}
			return !"0".equals(s);
		}
		return true;
	}

	private static long parsePositiveCompanyIdForH5FrontNoAuthList(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (companyAttr instanceof Number n) {
			companyId = n.longValue();
		} else if (companyAttr instanceof String s && StringUtils.hasText(s)) {
			try {
				companyId = Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new UnauthorizedException(MSG_H5_COMPANY_UNAUTHORIZED);
			}
		} else {
			throw new UnauthorizedException(MSG_H5_COMPANY_UNAUTHORIZED);
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException(MSG_H5_COMPANY_UNAUTHORIZED);
		}
		return companyId;
	}

	private static Long parseOptionalPositiveUserIdFromRequest(HttpServletRequest request) {
		Object rawClaims = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(rawClaims instanceof Map<?, ?> cm)) {
			return null;
		}
		Object raw = cm.get("user_id");
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : null;
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			return null;
		}
		try {
			long v = Long.parseLong(s);
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Map<String, Object> mergeBuyDataVipGradeInput(HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			LinkedHashMap<String, Object> input = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
			if (body != null) {
				input.putAll(body);
			}
			return input;
		}
		return body != null ? body : new LinkedHashMap<>();
	}

	private static long readShopIdDefaultZero(Map<String, Object> merged) {
		if (!merged.containsKey("shop_id")) {
			return 0L;
		}
		Object v = merged.get("shop_id");
		if (v == null) {
			return 0L;
		}
		return parseLongFlexible(v, 0L);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readH5ClaimsOrThrow(HttpServletRequest request) {
		Object rawClaims = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(rawClaims instanceof Map<?, ?>)) {
			throw new ResourceException("购买信息有误");
		}
		return (Map<String, Object>) rawClaims;
	}

	private static long parsePositiveUserIdFromClaimsOrThrow(Map<String, Object> claims) {
		Object claimUid = claims.get("user_id");
		if (claimUid == null || !h5UserIdTruthy(claimUid)) {
			throw new ResourceException("购买信息有误");
		}
		long userId = parseLongFlexible(claimUid, 0L);
		if (userId <= 0L) {
			throw new ResourceException("购买信息有误");
		}
		return userId;
	}

	private static long parsePositiveUserIdFromClaimsForMemberOrThrow(Map<String, Object> claims) {
		Object claimUid = claims.get("user_id");
		if (claimUid == null || !h5UserIdTruthy(claimUid)) {
			throw new ResourceException("获取会员信息失败，信息有误");
		}
		long userId = parseLongFlexible(claimUid, 0L);
		if (userId <= 0L) {
			throw new ResourceException("获取会员信息失败，信息有误");
		}
		return userId;
	}

	private static boolean isVipGradeOpenForUserVipResponse(Object v) {
		if (Boolean.TRUE.equals(v)) {
			return true;
		}
		return v instanceof Number n && n.intValue() == 1;
	}

	private static long parsePositiveCompanyIdFromRequestOrThrow(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (companyAttr instanceof Number n) {
			companyId = n.longValue();
		} else if (companyAttr instanceof String s && !s.isBlank()) {
			try {
				companyId = Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new ResourceException("购买信息有误");
			}
		} else {
			throw new ResourceException("购买信息有误");
		}
		if (companyId <= 0L) {
			throw new ResourceException("购买信息有误");
		}
		return companyId;
	}

	private static boolean h5UserIdTruthy(Object raw) {
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

	private static String stringTrimmed(Object o) {
		return o == null ? "" : o.toString().trim();
	}

	private static int intFromResult(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private String resolveH5BuyerMobile(Map<String, Object> claims, long userId, long companyId) {
		Object mobileClaim = claims.get("mobile");
		if (mobileClaim != null) {
			String fromClaim = String.valueOf(mobileClaim).trim();
			if (StringUtils.hasText(fromClaim)) {
				return fromClaim;
			}
		}
		Map<String, Object> info = memberAccountService.getMemberInfo(userId, companyId);
		Object region = info.get("region_mobile");
		if (region != null) {
			String s = String.valueOf(region).trim();
			if (StringUtils.hasText(s)) {
				return s;
			}
		}
		Object storedMobile = info.get("mobile");
		if (storedMobile != null) {
			String s = String.valueOf(storedMobile).trim();
			if (StringUtils.hasText(s)) {
				return s;
			}
		}
		return "";
	}
}
