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

package cn.shopex.ecshopx.selfservice.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.selfservice.service.UserDailyRecordDateListService;
import cn.shopex.ecshopx.selfservice.service.UserDailyRecordPersonalRecordService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("selfserviceFrontV1FormTemplate")
@RequestMapping("/api/v1/h5app")
public class FormTemplateController {

	private final UserDailyRecordDateListService userDailyRecordDateListService;
	private final UserDailyRecordPersonalRecordService userDailyRecordPersonalRecordService;
	private final MessageSource messageSource;

	public FormTemplateController(
			UserDailyRecordDateListService userDailyRecordDateListService,
			UserDailyRecordPersonalRecordService userDailyRecordPersonalRecordService,
			MessageSource messageSource) {
		this.userDailyRecordDateListService = userDailyRecordDateListService;
		this.userDailyRecordPersonalRecordService = userDailyRecordPersonalRecordService;
		this.messageSource = messageSource;
	}

	@GetMapping(value = "/wxapp/selfform/statisticalAnalysis", name = "自助表单分析")
	public ResponseEntity<ApiResult<Map<String, Object>>> statisticalAnalysis(
			HttpServletRequest request,
			@RequestParam(value = "days", required = false) String daysRaw,
			@RequestParam(value = "user_id", required = false) String userIdRaw,
			@RequestParam(value = "timeChoosed", required = false) String timeChoosed,
			@RequestParam(value = "form_type", required = false, defaultValue = "physical") String formType,
			@RequestParam(value = "shop_id", required = false) String shopIdRaw,
			Locale locale) {
		int size = parseDaysPageSize(daysRaw);
		// Uses JVM default time zone for calendar-day grouping when no explicit TZ is configured.
		int todayYmdInt =
				Integer.parseInt(LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.BASIC_ISO_DATE));
		int recordDateLte = todayYmdInt;
		if (timeChoosed == null || !"undefined".equals(timeChoosed)) {
			recordDateLte = parseYmdIntOrDefault(timeChoosed, todayYmdInt);
		}
		long companyId = resolveCompanyId(request);
		long userId = parseOptionalLongQuery(userIdRaw);
		if (userId <= 0L) {
			userId = parseClaimsUserId(request);
		}
		Long shopId = resolveShopIdForQuery(shopIdRaw);
		if (userId <= 0L) {
			LinkedHashMap<String, Object> emptyPayload = new LinkedHashMap<>();
			emptyPayload.put("list", new ArrayList<>());
			emptyPayload.put("keyindex", new ArrayList<>());
			return ResponseEntity.ok(ApiResult.ok(emptyPayload));
		}
		if (size > 10) {
			throw new ResourceException(
					messageSource.getMessage(
							"selfservice.user_daily_record.only_get_statistics_within_10_days", null, locale));
		}
		Map<String, Object> data =
				userDailyRecordPersonalRecordService.statisticalAnalysis(
						companyId, userId, size, recordDateLte, shopId, formType);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/wxapp/selfform/physical/datelist", name = "自助表单日期列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getRecordDateList(
			HttpServletRequest request,
			@RequestParam(value = "form_type", required = false, defaultValue = "physical") String formType,
			@RequestParam(value = "page", required = false, defaultValue = "1") String pageRaw,
			@RequestParam(value = "pageSize", required = false, defaultValue = "100") String pageSizeRaw) {
		long companyId = resolveCompanyId(request);
		long userId = parseClaimsUserId(request);
		if (userId <= 0L) {
			LinkedHashMap<String, Object> empty = new LinkedHashMap<>();
			empty.put("list", new ArrayList<>());
			empty.put("total_count", 0L);
			return ResponseEntity.ok(ApiResult.ok(empty));
		}
		int page = parsePage(pageRaw);
		int pageSize = parsePageSize(pageSizeRaw);
		Map<String, Object> data =
				userDailyRecordDateListService.getRecordDateList(companyId, formType, Long.valueOf(userId), page, pageSize);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static int parseDaysPageSize(String daysRaw) {
		if (daysRaw == null || !StringUtils.hasText(daysRaw.trim())) {
			return 5;
		}
		String t = daysRaw.trim();
		if ("0".equals(t)) {
			return 5;
		}
		int v;
		try {
			v = Integer.parseInt(t);
		} catch (NumberFormatException ex) {
			throw new BadRequestException("days 格式不正确");
		}
		if (v == 0) {
			return 5;
		}
		return v;
	}

	private static int parseYmdIntOrDefault(String raw, int defaultYmd) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return defaultYmd;
		}
		String t = raw.trim();
		if (t.length() != 8) {
			throw new BadRequestException("timeChoosed 格式不正确");
		}
		int v;
		try {
			v = Integer.parseInt(t);
		} catch (NumberFormatException ex) {
			throw new BadRequestException("timeChoosed 格式不正确");
		}
		try {
			LocalDate.parse(t, DateTimeFormatter.BASIC_ISO_DATE);
		} catch (DateTimeParseException ex) {
			throw new BadRequestException("timeChoosed 格式不正确");
		}
		return v;
	}

	private static long parseOptionalLongQuery(String raw) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return 0L;
		}
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException ex) {
			throw new BadRequestException("user_id 格式不正确");
		}
	}

	private static Long resolveShopIdForQuery(String shopIdRaw) {
		if (!StringUtils.hasText(shopIdRaw)) {
			return null;
		}
		try {
			long v = Long.parseLong(shopIdRaw.trim());
			if (v == 0L) {
				return null;
			}
			return v;
		} catch (NumberFormatException ex) {
			throw new BadRequestException("shop_id 格式不正确");
		}
	}

	private static long parseClaimsUserId(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(raw instanceof Map<?, ?> claims)) {
			return 0L;
		}
		return parseLongClaim(claims, "user_id");
	}

	private static long resolveCompanyId(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (raw instanceof Map<?, ?> claims) {
			long fromClaims = parseLongClaim(claims, "company_id");
			if (fromClaims > 0L) {
				return fromClaims;
			}
		}
		Object cid = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		if (cid instanceof Number n) {
			return n.longValue();
		}
		if (cid instanceof String s && StringUtils.hasText(s)) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		return 0L;
	}

	private static long parseLongClaim(Map<?, ?> claims, String key) {
		if (!claims.containsKey(key)) {
			return 0L;
		}
		Object v = claims.get(key);
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return 0L;
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		String t = String.valueOf(v).trim();
		if (t.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int parsePage(String pageRaw) {
		int page;
		try {
			page = Integer.parseInt(pageRaw.trim());
		} catch (NumberFormatException ex) {
			throw new BadRequestException("page 格式不正确");
		}
		if (page <= 0) {
			return 1;
		}
		return page;
	}

	private static int parsePageSize(String pageSizeRaw) {
		try {
			return Integer.parseInt(pageSizeRaw.trim());
		} catch (NumberFormatException ex) {
			throw new BadRequestException("pageSize 格式不正确");
		}
	}
}
