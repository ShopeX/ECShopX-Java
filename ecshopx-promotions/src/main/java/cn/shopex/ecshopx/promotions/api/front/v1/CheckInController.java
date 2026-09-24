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

package cn.shopex.ecshopx.promotions.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.companys.language.CompanyLanguageResolver;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.web.locale.RequestMessageLocale;
import cn.shopex.ecshopx.promotions.service.checkin.CheckInFrontCreateService;
import cn.shopex.ecshopx.promotions.service.checkin.CheckInFrontListService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
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
@RestController("promotionsFrontV1CheckIn")
@RequestMapping("/api/v1/h5app/wxapp/promotion/checkin")
public class CheckInController {

	private static final ZoneId CHECKIN_ZONE = ZoneId.of("Asia/Shanghai");

	private final CheckInFrontCreateService checkInFrontCreateService;
	private final CheckInFrontListService checkInFrontListService;
	private final LangueProperties langueProperties;
	private final CompanyLanguageResolver companyLanguageResolver;

	public CheckInController(
			CheckInFrontCreateService checkInFrontCreateService,
			CheckInFrontListService checkInFrontListService,
			LangueProperties langueProperties,
			CompanyLanguageResolver companyLanguageResolver) {
		this.checkInFrontCreateService = checkInFrontCreateService;
		this.checkInFrontListService = checkInFrontListService;
		this.langueProperties = langueProperties;
		this.companyLanguageResolver = companyLanguageResolver;
	}

	@PostMapping(value = "/create", name = "签到", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> createCheckIn(
			HttpServletRequest request, @RequestParam(name = "check_day", required = false) String checkDay) {
		long companyId = parseCompanyIdFromRequest(request);
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long userId = parseAuthUserIdFromClaims(claims);
		if (userId <= 0L) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		Locale locale = RequestMessageLocale.messageLocale(
				langueProperties, request, null, companyLanguageResolver.getDefaultLanguage(companyId));
		String checkDayYmd = normalizeCheckDayYmd(checkDay);
		Map<String, Object> row = checkInFrontCreateService.createCheckIn(companyId, userId, checkDayYmd, locale);
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@GetMapping(value = "/getlist", name = "签到列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getCheckInList(
			HttpServletRequest request,
			@RequestParam(name = "check_type", required = false) String checkType,
			@RequestParam(name = "start_date", required = false) String startDate,
			@RequestParam(name = "end_date", required = false) String endDate) {
		long companyId = parseCompanyIdFromRequest(request);
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long userId = parseAuthUserIdFromClaims(claims);
		if (userId <= 0L) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		Map<String, Object> result =
				checkInFrontListService.getCheckInList(companyId, userId, checkType, startDate, endDate);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	private static String normalizeCheckDayYmd(String checkDay) {
		if (!ValuePresence.hasEffectiveValue(checkDay)) {
			return DateTimeFormatter.BASIC_ISO_DATE.format(LocalDate.now(CHECKIN_ZONE));
		}
		String t = checkDay.trim();
		LocalDate resolved = null;
		if (t.matches("^\\d{8}$")) {
			try {
				int y = Integer.parseInt(t.substring(0, 4));
				int m = Integer.parseInt(t.substring(4, 6));
				int d = Integer.parseInt(t.substring(6, 8));
				resolved = LocalDate.of(y, m, d);
			} catch (NumberFormatException | DateTimeException e) {
				resolved = null;
			}
		}
		if (resolved == null && t.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
			try {
				resolved = LocalDate.parse(t, DateTimeFormatter.ISO_LOCAL_DATE);
			} catch (DateTimeParseException e) {
				resolved = null;
			}
		}
		if (resolved == null && t.matches("^\\d{10}$")) {
			try {
				long sec = Long.parseLong(t);
				resolved = LocalDate.ofInstant(Instant.ofEpochSecond(sec), CHECKIN_ZONE);
			} catch (DateTimeException | NumberFormatException e) {
				resolved = null;
			}
		}
		if (resolved == null) {
			resolved = LocalDate.ofInstant(Instant.EPOCH, CHECKIN_ZONE);
		}
		return DateTimeFormatter.BASIC_ISO_DATE.format(resolved);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readH5AuthClaimsMap(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (raw instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		return Collections.emptyMap();
	}

	private static long parseAuthUserIdFromClaims(Map<String, Object> claims) {
		Object v = claims == null ? null : claims.get("user_id");
		if (v == null) {
			return 0L;
		}
		try {
			long id = (v instanceof Number n) ? n.longValue() : Long.parseLong(String.valueOf(v).trim());
			return id > 0L ? id : 0L;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long parseCompanyIdFromRequest(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (companyAttr instanceof Number n) {
			companyId = n.longValue();
		} else if (companyAttr instanceof String s && StringUtils.hasText(s)) {
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
}
