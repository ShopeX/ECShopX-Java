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

package cn.shopex.ecshopx.selfservice.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.selfservice.service.UserDailyRecordAllUserListService;
import cn.shopex.ecshopx.selfservice.service.UserDailyRecordDateListService;
import cn.shopex.ecshopx.selfservice.service.UserDailyRecordPersonalRecordService;
import cn.shopex.ecshopx.selfservice.service.UserDailyRecordPhysicalSettingService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
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
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("selfserviceUserDailyRecordAdminV1")
@RequestMapping("/api/v1")
public class UserDailyRecordController {

	private final UserDailyRecordPhysicalSettingService userDailyRecordPhysicalSettingService;
	private final UserDailyRecordAllUserListService userDailyRecordAllUserListService;
	private final UserDailyRecordDateListService userDailyRecordDateListService;
	private final UserDailyRecordPersonalRecordService userDailyRecordPersonalRecordService;
	private final LangueProperties langueProperties;

	public UserDailyRecordController(
			UserDailyRecordPhysicalSettingService userDailyRecordPhysicalSettingService,
			UserDailyRecordAllUserListService userDailyRecordAllUserListService,
			UserDailyRecordDateListService userDailyRecordDateListService,
			UserDailyRecordPersonalRecordService userDailyRecordPersonalRecordService,
			LangueProperties langueProperties) {
		this.userDailyRecordPhysicalSettingService = userDailyRecordPhysicalSettingService;
		this.userDailyRecordAllUserListService = userDailyRecordAllUserListService;
		this.userDailyRecordDateListService = userDailyRecordDateListService;
		this.userDailyRecordPersonalRecordService = userDailyRecordPersonalRecordService;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "selfhelp.setting.physical.set")
	@PostMapping(value = "/selfhelp/setting/physical", name = "配置体测表单")
	public ResponseEntity<ApiResult<Map<String, Object>>> settingPhysical(
			HttpServletRequest httpRequest,
			@RequestParam(value = "temp_id", required = false, defaultValue = "0") String tempId,
			@RequestParam(value = "status", required = false, defaultValue = "0") String status) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) httpRequest.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		long companyId = parseCompanyId(jwt);
		Locale locale = httpRequest.getLocale();
		String langTag = RequestLangTag.current(langueProperties);
		Map<String, Object> data =
				userDailyRecordPhysicalSettingService.settingPhysical(companyId, tempId, status, langTag, locale);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long parseCompanyId(Map<String, Object> jwt) {
		Object cid = jwt.get("company_id");
		if (cid == null) {
			throw new BadRequestException("company_id 无效");
		}
		long result;
		if (cid instanceof Number n) {
			result = n.longValue();
		} else if (cid instanceof String s) {
			if (!StringUtils.hasText(s)) {
				throw new BadRequestException("company_id 无效");
			}
			try {
				result = Long.parseLong(s.trim());
			} catch (NumberFormatException ex) {
				throw new BadRequestException("company_id 无效");
			}
		} else {
			try {
				result = Long.parseLong(String.valueOf(cid).trim());
			} catch (NumberFormatException ex) {
				throw new BadRequestException("company_id 无效");
			}
		}
		if (result <= 0L) {
			throw new BadRequestException("company_id 无效");
		}
		return result;
	}

	@Activated(routeAlias = "selfhelp.setting.physical.get")
	@GetMapping(value = "/selfhelp/setting/physical", name = "获取体测表单配置")
	public ResponseEntity<ApiResult<Map<String, Object>>> getSettingPhysical(HttpServletRequest httpRequest) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) httpRequest.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		long companyId = parseCompanyId(jwt);
		String langTag = RequestLangTag.current(langueProperties);
		Map<String, Object> data =
				userDailyRecordPhysicalSettingService.getSettingPhysical(companyId, langTag);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "selfhelp.physical.alluserlist")
	@SuppressWarnings("unused")
	@GetMapping(value = "/selfhelp/physical/alluserlist", name = "获取体测数据所有会员（最近一次的记录）")
	public ResponseEntity<ApiResult<Map<String, Object>>> getAllUserList(
			HttpServletRequest httpRequest,
			@RequestParam(value = "form_type", required = false, defaultValue = "physical") String formType,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "username", required = false) String username,
			@RequestParam(value = "page", required = false, defaultValue = "1") String pageIgnored,
			@RequestParam(value = "pageSize", required = false, defaultValue = "20") String pageSizeIgnored) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) httpRequest.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		long companyId = parseCompanyId(jwt);
		Map<String, Object> body =
				userDailyRecordAllUserListService.getAllUserList(companyId, formType, mobile, username);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	@Activated(routeAlias = "selfhelp.physical.userlist")
	@GetMapping(value = "/selfhelp/physical/userdata", name = "获取指定会员最近5次的体测记录")
	public ResponseEntity<ApiResult<Map<String, Object>>> getUserPersonalRecord(
			HttpServletRequest httpRequest,
			@RequestParam(value = "day", required = false, defaultValue = "5") String dayRaw,
			@RequestParam(value = "user_id", required = false) String userIdRaw,
			@RequestParam(value = "timeChoosed", required = false) String timeChoosedRaw,
			@RequestParam(value = "form_type", required = false, defaultValue = "physical") String formType,
			@RequestParam(value = "shop_id", required = false) String shopIdRaw) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) httpRequest.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		long companyId = parseCompanyId(jwt);
		int pageSize = parsePersonalRecordLimit(dayRaw);
		long userId = parseUserIdForPersonalRecord(userIdRaw);
		Long shopIdOrNull = parseShopIdLenient(shopIdRaw);
		int recordDateLte = computeRecordDateUpperBound(timeChoosedRaw);
		Map<String, Object> data = userDailyRecordPersonalRecordService.getUserPersonalRecord(
				companyId, userId, pageSize, recordDateLte, shopIdOrNull, formType);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "selfhelp.physical.datelist")
	@GetMapping(value = "/selfhelp/physical/datelist", name = "获取所有记录的日期列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getRecordDateList(
			HttpServletRequest httpRequest,
			@RequestParam(value = "form_type", required = false, defaultValue = "physical") String formType,
			@RequestParam(value = "user_id", required = false) String userIdRaw,
			@RequestParam(value = "page", required = false, defaultValue = "1") String pageRaw,
			@RequestParam(value = "pageSize", required = false, defaultValue = "100") String pageSizeRaw) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) httpRequest.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		long companyId = parseCompanyId(jwt);
		Long userId = parseOptionalUserId(userIdRaw);
		int page = parsePage(pageRaw);
		int pageSize = parsePageSize(pageSizeRaw);
		Map<String, Object> data =
				userDailyRecordDateListService.getRecordDateList(companyId, formType, userId, page, pageSize);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static Long parseOptionalUserId(String userIdRaw) {
		if (!StringUtils.hasText(userIdRaw)) {
			return null;
		}
		try {
			return Long.parseLong(userIdRaw.trim());
		} catch (NumberFormatException ex) {
			throw new BadRequestException("user_id 格式不正确");
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

	/** Query {@code day} is the max number of rows to return (not calendar days). */
	private static int parsePersonalRecordLimit(String dayRaw) {
		if (dayRaw == null) {
			return 5;
		}
		String t = dayRaw.trim();
		if (t.isEmpty()) {
			return 5;
		}
		try {
			return Integer.parseInt(t);
		} catch (NumberFormatException ex) {
			return 5;
		}
	}

	private static long parseUserIdForPersonalRecord(String userIdRaw) {
		if (!StringUtils.hasText(userIdRaw)) {
			return 0L;
		}
		String t = userIdRaw.trim();
		if ("0".equals(t)) {
			return 0L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException ex) {
			return 0L;
		}
	}

	private static Long parseShopIdLenient(String shopIdRaw) {
		if (!StringUtils.hasText(shopIdRaw)) {
			return null;
		}
		try {
			return Long.parseLong(shopIdRaw.trim());
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	/**
	 * Upper bound for {@code record_date} (yyyyMMdd). Uses {@link ZoneId#systemDefault()} so the
	 * calendar day matches JVM / deployment timezone operations.
	 */
	private static int computeRecordDateUpperBound(String timeChoosedRaw) {
		ZoneId zone = ZoneId.systemDefault();
		int todayYmd = todayAsYyyyMmDd(zone);
		if ("undefined".equals(timeChoosedRaw)) {
			return todayYmd;
		}
		String effective = timeChoosedRaw == null ? "" : timeChoosedRaw.trim();
		if (effective.isEmpty() || "0".equals(effective)) {
			return todayYmd;
		}
		if (effective.length() == 8 && effective.chars().allMatch(Character::isDigit)) {
			try {
				return Integer.parseInt(effective);
			} catch (NumberFormatException ex) {
				return todayYmd;
			}
		}
		return todayYmd;
	}

	private static int todayAsYyyyMmDd(ZoneId zone) {
		return Integer.parseInt(LocalDate.now(zone).format(DateTimeFormatter.ofPattern("yyyyMMdd")));
	}
}
