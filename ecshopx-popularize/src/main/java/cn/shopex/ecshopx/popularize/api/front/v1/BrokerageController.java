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

package cn.shopex.ecshopx.popularize.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.popularize.mapper.PromoterWithdrawalHfpayGateMapper;
import cn.shopex.ecshopx.popularize.service.BrokerageShareQrcodeService;
import cn.shopex.ecshopx.popularize.service.PopularizeH5BrokerageCountReadService;
import cn.shopex.ecshopx.popularize.service.PopularizeCashWithdrawalListReadService;
import cn.shopex.ecshopx.popularize.service.PopularizeH5BrokerageListReadService;
import cn.shopex.ecshopx.popularize.service.PopularizeH5SecondBrokerageReadService;
import cn.shopex.ecshopx.popularize.service.PopularizePromoterCashWithdrawalApplyService;
import cn.shopex.ecshopx.popularize.service.PopularizeH5TaskBrokerageLogsBuyerWechatEnrichService;
import cn.shopex.ecshopx.popularize.service.PopularizeTaskBrokerageCountListQueryService;
import cn.shopex.ecshopx.popularize.service.PopularizeTaskBrokerageLogsListReadService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.http.MediaType;
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
@RestController("popularizeFrontV1Brokerage")
@RequestMapping("/api/v1/h5app/wxapp")
public class BrokerageController {

	private static final Pattern STRICT_UNSIGNED_INT = Pattern.compile("^[0-9]+$");

	private final PopularizePromoterCashWithdrawalApplyService popularizePromoterCashWithdrawalApplyService;

	private final PromoterWithdrawalHfpayGateMapper promoterWithdrawalHfpayGateMapper;

	private final BrokerageShareQrcodeService brokerageShareQrcodeService;

	private final PopularizeH5BrokerageCountReadService popularizeH5BrokerageCountReadService;

	private final PopularizeH5BrokerageListReadService popularizeH5BrokerageListReadService;

	private final PopularizeH5SecondBrokerageReadService popularizeH5SecondBrokerageReadService;

	private final PopularizeCashWithdrawalListReadService popularizeCashWithdrawalListReadService;

	private final PopularizeTaskBrokerageCountListQueryService popularizeTaskBrokerageCountListQueryService;

	private final PopularizeTaskBrokerageLogsListReadService popularizeTaskBrokerageLogsListReadService;

	private final PopularizeH5TaskBrokerageLogsBuyerWechatEnrichService
			popularizeH5TaskBrokerageLogsBuyerWechatEnrichService;

	public BrokerageController(
			PopularizePromoterCashWithdrawalApplyService popularizePromoterCashWithdrawalApplyService,
			PromoterWithdrawalHfpayGateMapper promoterWithdrawalHfpayGateMapper,
			BrokerageShareQrcodeService brokerageShareQrcodeService,
			PopularizeH5BrokerageCountReadService popularizeH5BrokerageCountReadService,
			PopularizeH5BrokerageListReadService popularizeH5BrokerageListReadService,
			PopularizeH5SecondBrokerageReadService popularizeH5SecondBrokerageReadService,
			PopularizeCashWithdrawalListReadService popularizeCashWithdrawalListReadService,
			PopularizeTaskBrokerageCountListQueryService popularizeTaskBrokerageCountListQueryService,
			PopularizeTaskBrokerageLogsListReadService popularizeTaskBrokerageLogsListReadService,
			PopularizeH5TaskBrokerageLogsBuyerWechatEnrichService popularizeH5TaskBrokerageLogsBuyerWechatEnrichService) {
		this.popularizePromoterCashWithdrawalApplyService = popularizePromoterCashWithdrawalApplyService;
		this.promoterWithdrawalHfpayGateMapper = promoterWithdrawalHfpayGateMapper;
		this.brokerageShareQrcodeService = brokerageShareQrcodeService;
		this.popularizeH5BrokerageCountReadService = popularizeH5BrokerageCountReadService;
		this.popularizeH5BrokerageListReadService = popularizeH5BrokerageListReadService;
		this.popularizeH5SecondBrokerageReadService = popularizeH5SecondBrokerageReadService;
		this.popularizeCashWithdrawalListReadService = popularizeCashWithdrawalListReadService;
		this.popularizeTaskBrokerageCountListQueryService = popularizeTaskBrokerageCountListQueryService;
		this.popularizeTaskBrokerageLogsListReadService = popularizeTaskBrokerageLogsListReadService;
		this.popularizeH5TaskBrokerageLogsBuyerWechatEnrichService = popularizeH5TaskBrokerageLogsBuyerWechatEnrichService;
	}

	@GetMapping(value = "/promoter/brokerages", name = "佣金列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Object> getBrokerageList(
			HttpServletRequest request,
			@RequestParam(value = "brokerage_source", required = false, defaultValue = "order") String brokerageSource,
			@RequestParam(value = "page", required = false, defaultValue = "1") String page,
			@RequestParam(value = "pageSize", required = false, defaultValue = "10") String pageSize,
			@RequestParam(value = "isSalesmanPage", required = false, defaultValue = "0") String isSalesmanPage,
			@RequestParam(value = "shopName", required = false, defaultValue = "0") String shopName,
			@RequestParam(value = "mobile", required = false, defaultValue = "0") String mobile,
			@RequestParam(value = "order_id", required = false, defaultValue = "0") String orderId,
			@RequestParam(value = "close_type", required = false) String closeType) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = parsePositiveLongOrZero(claims.get("user_id"));
		if (userId <= 0L) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		return ApiResult.ok(
				popularizeH5BrokerageListReadService.getBrokerageList(
						companyId,
						userId,
						brokerageSource,
						page,
						pageSize,
						isSalesmanPage,
						shopName,
						mobile,
						orderId,
						closeType));
	}

	@SuppressWarnings("unused")
	@GetMapping(value = "/promoter/second/brokerages", name = "B级业绩", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getSecondBrokerageList(
			HttpServletRequest request,
			@RequestParam(value = "brokerage_source", required = false, defaultValue = "order") String brokerageSource,
			@RequestParam(value = "page", required = false) String page,
			@RequestParam(value = "pageSize", required = false) String pageSize,
			@RequestParam(value = "promoter_user_id", required = false) String promoterUserIdRaw,
			@RequestParam(value = "close_type", required = false) String closeType) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long authUserId = parsePositiveLongOrZero(claims.get("user_id"));
		if (authUserId <= 0L) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}

		String pageRaw = request.getParameter("page");
		if (pageRaw == null || !StringUtils.hasText(pageRaw.trim())) {
			throw new ResourceException("分页参数错误");
		}
		String pageTrimmed = pageRaw.trim();
		if (!STRICT_UNSIGNED_INT.matcher(pageTrimmed).matches()) {
			throw new ResourceException("分页参数错误");
		}
		int pageNum;
		try {
			pageNum = Integer.parseInt(pageTrimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException("分页参数错误");
		}
		if (pageNum < 1) {
			throw new ResourceException("分页参数错误");
		}

		String pageSizeRaw = request.getParameter("pageSize");
		if (pageSizeRaw == null || !StringUtils.hasText(pageSizeRaw.trim())) {
			throw new ResourceException("每页最多查询50条数据");
		}
		String pageSizeTrimmed = pageSizeRaw.trim();
		if (!STRICT_UNSIGNED_INT.matcher(pageSizeTrimmed).matches()) {
			throw new ResourceException("每页最多查询50条数据");
		}
		int pageSizeNum;
		try {
			pageSizeNum = Integer.parseInt(pageSizeTrimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException("每页最多查询50条数据");
		}
		if (pageSizeNum < 1 || pageSizeNum > 50) {
			throw new ResourceException("每页最多查询50条数据");
		}

		if (promoterUserIdRaw == null || !StringUtils.hasText(promoterUserIdRaw.trim())) {
			throw new ResourceException("推广员的会员ID错误");
		}
		String promoterTrimmed = promoterUserIdRaw.trim();
		if (!STRICT_UNSIGNED_INT.matcher(promoterTrimmed).matches()
				|| (promoterTrimmed.length() > 1 && promoterTrimmed.charAt(0) == '0')) {
			throw new ResourceException("推广员的会员ID错误");
		}
		long promoterUserId;
		try {
			promoterUserId = Long.parseLong(promoterTrimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException("推广员的会员ID错误");
		}
		if (promoterUserId < 1L) {
			throw new ResourceException("推广员的会员ID错误");
		}

		Object payload =
				popularizeH5SecondBrokerageReadService.getSecondBrokerageList(
						companyId,
						promoterUserId,
						authUserId,
						brokerageSource,
						pageNum,
						pageSizeNum,
						closeType);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(ApiResult.ok(payload));
	}

	@GetMapping(value = "/promoter/brokerage/count", name = "佣金统计", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> brokerageCount(
			HttpServletRequest request,
			@RequestParam(value = "isSalesmanPage", required = false, defaultValue = "0") String isSalesmanPage,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") String distributorId) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = parsePositiveLongOrZero(claims.get("user_id"));
		if (userId <= 0L) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		return ApiResult.ok(
				popularizeH5BrokerageCountReadService.brokerageCount(
						companyId, userId, isSalesmanPage, distributorId));
	}

	@GetMapping(value = "/promoter/brokerage/point_count", name = "积分佣金", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> brokeragePointCount(HttpServletRequest request) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = parsePositiveLongOrZero(claims.get("user_id"));
		if (userId <= 0L) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		return ApiResult.ok(popularizeH5BrokerageCountReadService.brokeragePointCount(companyId, userId));
	}

	@GetMapping(
			value = "/promoter/taskBrokerage/logs",
			name = "任务佣金日志",
			produces = MediaType.APPLICATION_JSON_VALUE)
	@SuppressWarnings("unchecked")
	public ApiResult<Map<String, Object>> getTaskBrokerageList(HttpServletRequest request) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = parsePositiveLongOrZero(claims.get("user_id"));
		if (userId <= 0L) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}

		String pageRaw = request.getParameter("page");
		if (pageRaw == null || !StringUtils.hasText(pageRaw.trim())) {
			throw new ResourceException("分页参数错误");
		}
		String pageTrimmed = pageRaw.trim();
		int page;
		try {
			page = Integer.parseInt(pageTrimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException("分页参数错误");
		}
		if (page < 1) {
			throw new ResourceException("分页参数错误");
		}

		String pageSizeRaw = request.getParameter("pageSize");
		if (pageSizeRaw == null || !StringUtils.hasText(pageSizeRaw.trim())) {
			throw new ResourceException("每页最多查询50条数据");
		}
		String pageSizeTrimmed = pageSizeRaw.trim();
		int pageSize;
		try {
			pageSize = Integer.parseInt(pageSizeTrimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException("每页最多查询50条数据");
		}
		if (pageSize < 1 || pageSize > 50) {
			throw new ResourceException("每页最多查询50条数据");
		}

		String timeStartRaw = request.getParameter("time_start");
		String timeEndRaw = request.getParameter("time_end");
		boolean timeStartTruthy = PopularizeTaskBrokerageCountListQueryService.nonEmptyFilterObject(timeStartRaw);
		boolean timeEndTruthy = PopularizeTaskBrokerageCountListQueryService.nonEmptyFilterObject(timeEndRaw);
		String timeStartForFilter;
		String timeEndForFilter;
		if (!timeStartTruthy || !timeEndTruthy) {
			timeStartForFilter = null;
			timeEndForFilter = null;
		} else {
			try {
				PopularizeTaskBrokerageCountListQueryService.parseUpdatedRange(
						timeStartRaw.trim(), timeEndRaw.trim());
				timeStartForFilter = timeStartRaw.trim();
				timeEndForFilter = timeEndRaw.trim();
			} catch (Throwable t) {
				timeStartForFilter = null;
				timeEndForFilter = null;
			}
		}

		Map<String, Object> filter =
				popularizeTaskBrokerageLogsListReadService.buildLogsListFilter(
						companyId,
						Long.valueOf(userId),
						null,
						null,
						request.getParameter("status"),
						timeStartForFilter,
						timeEndForFilter,
						request.getParameter("plan_date"));
		Map<String, Object> data =
				popularizeTaskBrokerageLogsListReadService.getTaskBrokerageList(filter, page, pageSize);
		List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("list");
		if (list != null && !list.isEmpty()) {
			popularizeH5TaskBrokerageLogsBuyerWechatEnrichService.attachBuyerUsernameAndAvatar(companyId, list);
		}
		return ApiResult.ok(data);
	}

	@GetMapping(
			value = "/promoter/taskBrokerage/count",
			name = "任务佣金统计",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getTaskBrokerageCountList(HttpServletRequest request) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = parsePositiveLongOrZero(claims.get("user_id"));
		if (userId <= 0L) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}

		String pageRaw = request.getParameter("page");
		if (pageRaw == null || !StringUtils.hasText(pageRaw.trim())) {
			throw new ResourceException("分页参数错误");
		}
		String pageTrimmed = pageRaw.trim();
		int page;
		try {
			page = Integer.parseInt(pageTrimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException("分页参数错误");
		}
		if (page < 1) {
			throw new ResourceException("分页参数错误");
		}

		String pageSizeRaw = request.getParameter("pageSize");
		if (pageSizeRaw == null || !StringUtils.hasText(pageSizeRaw.trim())) {
			throw new ResourceException("每页最多查询50条数据");
		}
		String pageSizeTrimmed = pageSizeRaw.trim();
		int pageSize;
		try {
			pageSize = Integer.parseInt(pageSizeTrimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException("每页最多查询50条数据");
		}
		if (pageSize < 1 || pageSize > 50) {
			throw new ResourceException("每页最多查询50条数据");
		}

		String timeStart = request.getParameter("time_start");
		String timeEnd = request.getParameter("time_end");
		boolean explicitPlanDate =
				PopularizeTaskBrokerageCountListQueryService.nonEmptyFilterString(request.getParameter("plan_date"));
		Map<String, Object> filter =
				popularizeTaskBrokerageCountListQueryService.buildCountListFilter(
						companyId,
						userId,
						null,
						timeStart,
						timeEnd,
						explicitPlanDate ? request.getParameter("plan_date").trim() : null);
		if (!explicitPlanDate) {
			filter.put(
					"plan_date",
					PopularizeTaskBrokerageCountListQueryService.endOfMonthYmd(
							LocalDate.now(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ISO_LOCAL_DATE)));
		}

		Map<String, Object> data =
				popularizeTaskBrokerageCountListQueryService.getTaskBrokerageCountList(
						filter, "*", page, pageSize);
		if (explicitPlanDate) {
			data.put("total_rebate", popularizeTaskBrokerageCountListQueryService.sumRebateMoney(filter));
		}

		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(ApiResult.ok(data));
	}

	@PostMapping(value = "/promoter/cash_withdrawal", name = "佣金提现", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> applyCashWithdrawal(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Map<String, Object> claims = cashWithdrawalAuthClaimsView(readH5AuthClaimsMap(request));
		if (!hasNonNullMapKey(claims, "open_id") || !hasNonNullMapKey(claims, "wxapp_appid")) {
			throw new ResourceException("缺少参数");
		}
		double moneyNum = parseMoneyDoubleLoose(merged.get("money"));
		if (moneyNum < 100.0d) {
			throw new ResourceException("佣金提现最少为1元");
		}
		Object payTypeRaw = merged.get("pay_type");
		String payTypeNorm;
		if (payTypeRaw == null) {
			payTypeNorm = "wechat";
		} else {
			String payTypeStr = String.valueOf(payTypeRaw).trim();
			payTypeNorm = payTypeStr.isEmpty() ? "wechat" : payTypeStr;
		}
		if ("wechat".equals(payTypeNorm) && moneyNum > 80000.0d) {
			throw new ResourceException("佣金单次最多提现800元");
		}
		if ("hfpay".equals(payTypeNorm)) {
			String userIdStr = Objects.toString(claims.get("user_id"), "").trim();
			if (userIdStr.isEmpty()) {
				throw new UnauthorizedException("还未授权，请授权手机号");
			}
			long hfpayUserId = parsePositiveLongOrZero(userIdStr);
			if (hfpayUserId <= 0L) {
				throw new UnauthorizedException("还未授权，请授权手机号");
			}
			Long enterId = promoterWithdrawalHfpayGateMapper.selectVerifiedEnterapplyId(hfpayUserId);
			if (enterId == null) {
				throw new ResourceException("请先完成实名认证");
			}
			Long bankId = promoterWithdrawalHfpayGateMapper.selectCashBankCardId(hfpayUserId);
			if (bankId == null) {
				throw new ResourceException("请绑定提现银行卡");
			}
		}
		Map<String, Object> row =
				popularizePromoterCashWithdrawalApplyService.applyCashWithdrawal(
						request, claims, merged, payTypeNorm, moneyNum);
		return ResponseEntity.ok(ApiResult.ok(row));
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

	/**
	 * Builds a mutable view of H5 JWT claims with stable keys for cash-withdrawal checks: some login
	 * paths only expose {@code openid} or omit appid fields while the authenticated session still
	 * carries empty string placeholders in the guard payload.
	 */
	private static Map<String, Object> cashWithdrawalAuthClaimsView(Map<String, Object> raw) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>(raw != null ? raw : Map.of());
		if (!out.containsKey("open_id")) {
			out.put("open_id", out.containsKey("openid") ? out.get("openid") : "");
		}
		if (!out.containsKey("wxapp_appid")) {
			out.put(
					"wxapp_appid",
					out.containsKey("authorizer_appid")
							? out.get("authorizer_appid")
							: (out.containsKey("wxa_appid") ? out.get("wxa_appid") : ""));
		}
		return out;
	}

	private static boolean hasNonNullMapKey(Map<String, Object> map, String key) {
		return map != null && map.containsKey(key) && map.get(key) != null;
	}

	private static double parseMoneyDoubleLoose(Object moneyRaw) {
		if (moneyRaw == null) {
			return 0.0d;
		}
		if (moneyRaw instanceof Number n) {
			double d = n.doubleValue();
			if (Double.isNaN(d) || Double.isInfinite(d)) {
				return 0.0d;
			}
			return d;
		}
		String s = String.valueOf(moneyRaw).trim();
		if (s.isEmpty()) {
			return 0.0d;
		}
		try {
			double d = Double.parseDouble(s);
			if (Double.isNaN(d) || Double.isInfinite(d)) {
				return 0.0d;
			}
			return d;
		} catch (NumberFormatException e) {
			return 0.0d;
		}
	}

	@GetMapping(value = "/promoter/cash_withdrawal", name = "提现列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getCashWithdrawalList(HttpServletRequest request) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		String promoterUserIdStr = Objects.toString(claims.get("user_id"), "").trim();
		if (!StringUtils.hasText(promoterUserIdStr) || parsePositiveLongOrZero(promoterUserIdStr) <= 0L) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		long promoterUserId = parsePositiveLongOrZero(promoterUserIdStr);

		String pageRaw = request.getParameter("page");
		if (pageRaw == null || !StringUtils.hasText(pageRaw.trim())) {
			throw new ResourceException("分页参数错误");
		}
		String pageTrimmed = pageRaw.trim();
		int page;
		try {
			page = Integer.parseInt(pageTrimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException("分页参数错误");
		}
		if (page < 1) {
			throw new ResourceException("分页参数错误");
		}

		String pageSizeRaw = request.getParameter("pageSize");
		if (pageSizeRaw == null || !StringUtils.hasText(pageSizeRaw.trim())) {
			throw new ResourceException("每页最多查询50条数据");
		}
		String pageSizeTrimmed = pageSizeRaw.trim();
		int pageSize;
		try {
			pageSize = Integer.parseInt(pageSizeTrimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException("每页最多查询50条数据");
		}
		if (pageSize < 1 || pageSize > 50) {
			throw new ResourceException("每页最多查询50条数据");
		}

		return ApiResult.ok(
				popularizeCashWithdrawalListReadService.getCashWithdrawalList(
						companyId, promoterUserId, page, pageSize));
	}

	@GetMapping(value = "/brokerage/qrcode", name = "推广二维码", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getBrokerageQrcode(HttpServletRequest request) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		Map<String, Object> data = brokerageShareQrcodeService.getBrokerageQrcode(request, null, claims);
		return ApiResult.ok(data);
	}
}
