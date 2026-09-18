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

package cn.shopex.ecshopx.wechat.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.wechat.service.wxa.WxappStatsRetaininfoService;
import cn.shopex.ecshopx.wechat.service.wxa.WxappStatsSummaryByDateService;
import cn.shopex.ecshopx.wechat.service.wxa.WxappStatsSummaryTrendService;
import cn.shopex.ecshopx.wechat.service.wxa.WxappStatsUserPortraitService;
import cn.shopex.ecshopx.wechat.service.wxa.WxappStatsVisitDistributionService;
import cn.shopex.ecshopx.wechat.service.wxa.WxappStatsVisitPageService;
import cn.shopex.ecshopx.wechat.service.wxa.WxappStatsVisitTrendService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@RestController("wechatAdminV1WxappStats")
@RequestMapping("/api/v1/wxa/stats")
public class WxappStatsController {

	private static final DateTimeFormatter BASIC_ISO_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	private final WxappStatsRetaininfoService wxappStatsRetaininfoService;
	private final WxappStatsSummaryByDateService wxappStatsSummaryByDateService;
	private final WxappStatsSummaryTrendService wxappStatsSummaryTrendService;
	private final WxappStatsUserPortraitService wxappStatsUserPortraitService;
	private final WxappStatsVisitDistributionService wxappStatsVisitDistributionService;
	private final WxappStatsVisitTrendService wxappStatsVisitTrendService;
	private final WxappStatsVisitPageService wxappStatsVisitPageService;

	public WxappStatsController(
			WxappStatsRetaininfoService wxappStatsRetaininfoService,
			WxappStatsSummaryByDateService wxappStatsSummaryByDateService,
			WxappStatsSummaryTrendService wxappStatsSummaryTrendService,
			WxappStatsUserPortraitService wxappStatsUserPortraitService,
			WxappStatsVisitDistributionService wxappStatsVisitDistributionService,
			WxappStatsVisitTrendService wxappStatsVisitTrendService,
			WxappStatsVisitPageService wxappStatsVisitPageService) {
		this.wxappStatsRetaininfoService = wxappStatsRetaininfoService;
		this.wxappStatsSummaryByDateService = wxappStatsSummaryByDateService;
		this.wxappStatsSummaryTrendService = wxappStatsSummaryTrendService;
		this.wxappStatsUserPortraitService = wxappStatsUserPortraitService;
		this.wxappStatsVisitDistributionService = wxappStatsVisitDistributionService;
		this.wxappStatsVisitTrendService = wxappStatsVisitTrendService;
		this.wxappStatsVisitPageService = wxappStatsVisitPageService;
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.stats.summarytrend")
	@PostMapping(value = "/summarybydate", name = "某天概况趋势", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getSummaryByDate(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}

		Object rawWxa = merged.get("wxaAppId");
		if (isMissingWxaAppId(rawWxa)) {
			throw new BadRequestException("wxappid 必填");
		}
		String wxaAppId =
				rawWxa instanceof CharSequence ? rawWxa.toString().trim() : String.valueOf(rawWxa).trim();

		String yesterdayYmd =
				LocalDate.now(ZoneId.systemDefault()).minusDays(1).format(BASIC_ISO_DATE);
		Object rawDate = merged.get("date");
		String dateStr;
		if (isEmptyRequestValue(rawDate)) {
			dateStr = yesterdayYmd;
		} else {
			dateStr = normalizeDateInputToString(rawDate);
			if (!dateStr.matches("\\d{8}")) {
				throw new BadRequestException("date 须为 8 位 yyyyMMdd");
			}
			try {
				LocalDate.parse(dateStr, BASIC_ISO_DATE);
			} catch (DateTimeParseException e) {
				throw new BadRequestException("date 非法或不存在该日历日");
			}
		}

		return ResponseEntity.ok(
				ApiResult.ok(wxappStatsSummaryByDateService.getSummaryByDate(wxaAppId, dateStr)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.stats.summarytrend")
	@PostMapping(value = "/summarytrend", name = "概况趋势", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getSummaryTrend(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}

		Object rawWxa = merged.get("wxaAppId");
		if (isMissingWxaAppId(rawWxa)) {
			throw new BadRequestException("wxappid 必填");
		}
		String wxaAppId =
				rawWxa instanceof CharSequence ? rawWxa.toString().trim() : String.valueOf(rawWxa).trim();

		return ResponseEntity.ok(
				ApiResult.ok(wxappStatsSummaryTrendService.getSummaryTrend(wxaAppId, merged.get("queryType"))));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.stats.visitpage")
	@PostMapping(value = "/visitpage", name = "访问页面", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getVisitPage(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Object rawWxa = merged.get("wxaAppId");
		if (isMissingWxaAppId(rawWxa)) {
			throw new BadRequestException("wxappid 必填");
		}
		String wxaAppId =
				rawWxa instanceof CharSequence ? rawWxa.toString().trim() : String.valueOf(rawWxa).trim();
		return ResponseEntity.ok(
				ApiResult.ok(wxappStatsVisitPageService.getVisitPage(wxaAppId, merged.get("queryType"))));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.stats.visittrend")
	@PostMapping(value = "/visittrend", name = "访问趋势", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getVisitTrend(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}

		Object rawWxa = merged.get("wxaAppId");
		if (isMissingWxaAppId(rawWxa)) {
			throw new BadRequestException("wxappid 必填");
		}
		String wxaAppId =
				rawWxa instanceof CharSequence ? rawWxa.toString().trim() : String.valueOf(rawWxa).trim();

		return ResponseEntity.ok(
				ApiResult.ok(wxappStatsVisitTrendService.getVisitTrend(wxaAppId, merged.get("queryType"))));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.stats.visitdistribution")
	@PostMapping(value = "/visitdistribution", name = "访问分布", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getVisitDistribution(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Object rawWxa = merged.get("wxaAppId");
		if (isMissingWxaAppId(rawWxa)) {
			throw new BadRequestException("wxappid 必填");
		}
		String wxaAppId =
				rawWxa instanceof CharSequence ? rawWxa.toString().trim() : String.valueOf(rawWxa).trim();
		return ResponseEntity.ok(
				ApiResult.ok(
						wxappStatsVisitDistributionService.getVisitDistribution(wxaAppId, merged.get("queryType"))));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.stats.retaininfo")
	@PostMapping(value = "/retaininfo", name = "访问留存", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getRetaininfo(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Object rawWxa = merged.get("wxaAppId");
		if (isMissingWxaAppId(rawWxa)) {
			throw new BadRequestException("wxappid 必填");
		}
		String wxaAppId =
				rawWxa instanceof CharSequence ? rawWxa.toString().trim() : String.valueOf(rawWxa).trim();
		Object q = merged.get("queryType");
		String queryType = q == null ? null : (q instanceof CharSequence ? q.toString() : String.valueOf(q));
		return ResponseEntity.ok(ApiResult.ok(wxappStatsRetaininfoService.getRetaininfo(wxaAppId, queryType)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.stats.userportrait")
	@PostMapping(value = "/userportrait", name = "用户画像", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getUserPortrait(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Object rawWxa = merged.get("wxaAppId");
		if (isMissingWxaAppId(rawWxa)) {
			throw new BadRequestException("wxappid 必填");
		}
		String wxaAppId =
				rawWxa instanceof CharSequence ? rawWxa.toString().trim() : String.valueOf(rawWxa).trim();
		Object q = merged.get("queryType");
		String queryType = q == null ? null : (q instanceof CharSequence ? q.toString() : String.valueOf(q));
		return ResponseEntity.ok(ApiResult.ok(wxappStatsUserPortraitService.getUserPortrait(wxaAppId, queryType)));
	}

	private static String normalizeDateInputToString(Object rawDate) {
		if (rawDate instanceof CharSequence cs) {
			return cs.toString().trim();
		}
		if (rawDate instanceof Number n) {
			if (n.doubleValue() != n.longValue()) {
				throw new BadRequestException("date 须为 yyyyMMdd 整数日期");
			}
			return String.format(Locale.US, "%d", n.longValue());
		}
		return String.valueOf(rawDate).trim();
	}

	private static boolean isMissingWxaAppId(Object raw) {
		return isEmptyRequestValue(raw);
	}

	private static boolean isEmptyRequestValue(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw instanceof Boolean b) {
			return Boolean.FALSE.equals(b);
		}
		if (raw instanceof Number n) {
			return n.doubleValue() == 0.0;
		}
		if (raw instanceof CharSequence cs) {
			String t = cs.toString().trim();
			return t.isEmpty() || "0".equals(t);
		}
		if (raw instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (raw instanceof Map<?, ?> m) {
			return m.isEmpty();
		}
		if (raw instanceof Object[] arr) {
			return arr.length == 0;
		}
		String t = String.valueOf(raw).trim();
		return t.isEmpty() || "0".equals(t);
	}
}
