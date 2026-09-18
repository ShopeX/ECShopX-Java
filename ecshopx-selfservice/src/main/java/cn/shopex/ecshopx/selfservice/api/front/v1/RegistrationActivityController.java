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
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.selfservice.service.RegistrationActivityFrontCancelRecordService;
import cn.shopex.ecshopx.selfservice.service.RegistrationActivityFrontGetRegistrationActivityListService;
import cn.shopex.ecshopx.selfservice.service.RegistrationActivityFrontGetRegistrationActivityService;
import cn.shopex.ecshopx.selfservice.service.RegistrationActivityFrontGetRegistrationRecordListService;
import cn.shopex.ecshopx.selfservice.service.RegistrationActivityFrontGetRegistrationRecordInfoService;
import cn.shopex.ecshopx.selfservice.service.RegistrationActivityFrontRegistrationSubmitService;
import cn.shopex.ecshopx.selfservice.support.RegistrationActivityFrontCancelRecordMessageKeys;
import cn.shopex.ecshopx.selfservice.support.RegistrationActivityFrontSubmitMessageKeys;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.NONE,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("selfserviceFrontV1RegistrationActivity")
@RequestMapping("/api/v1/h5app")
public class RegistrationActivityController {

	private final RegistrationActivityFrontRegistrationSubmitService registrationActivityFrontRegistrationSubmitService;
	private final RegistrationActivityFrontCancelRecordService registrationActivityFrontCancelRecordService;
	private final RegistrationActivityFrontGetRegistrationActivityService registrationActivityFrontGetRegistrationActivityService;
	private final MessageSource messageSource;
	private final MemberAccountService memberAccountService;
	private final RegistrationActivityFrontGetRegistrationActivityListService registrationActivityFrontGetRegistrationActivityListService;
	private final RegistrationActivityFrontGetRegistrationRecordInfoService registrationActivityFrontGetRegistrationRecordInfoService;
	private final RegistrationActivityFrontGetRegistrationRecordListService registrationActivityFrontGetRegistrationRecordListService;

	public RegistrationActivityController(
			RegistrationActivityFrontRegistrationSubmitService registrationActivityFrontRegistrationSubmitService,
			RegistrationActivityFrontCancelRecordService registrationActivityFrontCancelRecordService,
			RegistrationActivityFrontGetRegistrationActivityService registrationActivityFrontGetRegistrationActivityService,
			MessageSource messageSource,
			MemberAccountService memberAccountService,
			RegistrationActivityFrontGetRegistrationActivityListService registrationActivityFrontGetRegistrationActivityListService,
			RegistrationActivityFrontGetRegistrationRecordInfoService registrationActivityFrontGetRegistrationRecordInfoService,
			RegistrationActivityFrontGetRegistrationRecordListService registrationActivityFrontGetRegistrationRecordListService) {
		this.registrationActivityFrontRegistrationSubmitService = registrationActivityFrontRegistrationSubmitService;
		this.registrationActivityFrontCancelRecordService = registrationActivityFrontCancelRecordService;
		this.registrationActivityFrontGetRegistrationActivityService = registrationActivityFrontGetRegistrationActivityService;
		this.messageSource = messageSource;
		this.memberAccountService = memberAccountService;
		this.registrationActivityFrontGetRegistrationActivityListService = registrationActivityFrontGetRegistrationActivityListService;
		this.registrationActivityFrontGetRegistrationRecordInfoService = registrationActivityFrontGetRegistrationRecordInfoService;
		this.registrationActivityFrontGetRegistrationRecordListService = registrationActivityFrontGetRegistrationRecordListService;
	}

	@GetMapping(value = "/wxapp/registrationActivity", name = "报名活动详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getRegistrationActivity(
			HttpServletRequest request,
			@RequestParam(name = "activity_id", defaultValue = "0") long activityId) {
		Locale locale = request.getLocale();
		String tag = locale.toLanguageTag();
		String requestLangTag = (tag == null || tag.isBlank()) ? "zh-CN" : tag;
		long companyId = resolveCompanyId(request);
		long userId = parseClaimsUserId(request);
		Map<String, Object> data = registrationActivityFrontGetRegistrationActivityService.getRegistrationActivity(
				locale, requestLangTag, companyId, userId, activityId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/wxapp/registrationRecordList", name = "报名记录列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getRegistrationRecordList(
			HttpServletRequest request,
			@RequestParam(name = "status", required = false) String statusRaw,
			@RequestParam(name = "page", required = false) String pageRaw,
			@RequestParam(name = "pageSize", required = false) String pageSizeRaw,
			@RequestParam(name = "activity_id", required = false) String activityIdRaw) {
		Locale locale = request.getLocale();
		String tag = locale.toLanguageTag();
		String requestLangTag = (tag == null || tag.isBlank()) ? "zh-CN" : tag;
		long authCompanyId = resolveCompanyId(request);
		long authUserId = parseClaimsUserId(request);
		int page = parseRegistrationRecordListPage(pageRaw);
		int pageSize = parseRegistrationRecordListPageSize(pageSizeRaw);
		Map<String, Object> data = registrationActivityFrontGetRegistrationRecordListService.getRegistrationRecordList(
				locale, requestLangTag, authCompanyId, authUserId, statusRaw, page, pageSize, activityIdRaw);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/wxapp/registrationRecordInfo", name = "报名记录详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getRegistrationRecordInfo(
			HttpServletRequest request,
			@RequestParam(name = "record_id", required = false) String recordIdRaw) {
		Locale locale = request.getLocale();
		String tag = locale.toLanguageTag();
		String requestLangTag = (tag == null || tag.isBlank()) ? "zh-CN" : tag;
		String trimmed = recordIdRaw == null ? "" : recordIdRaw.trim();
		long recordId = LeadingNumberParser.parseAsLong(trimmed);
		long authUserId = parseClaimsUserId(request);
		Map<String, Object> data = registrationActivityFrontGetRegistrationRecordInfoService.getRegistrationRecordInfo(
				locale, requestLangTag, authUserId, recordId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PostMapping(value = "/wxapp/registrationSubmit", name = "报名提交")
	public ResponseEntity<ApiResult<Map<String, Object>>> registrationSubmit(
			HttpServletRequest request,
			@RequestParam(name = "activity_id", defaultValue = "0") long activityIdParam,
			@RequestParam(name = "record_id", defaultValue = "0") long recordIdParam,
			@RequestParam(name = "distributor_id", defaultValue = "0") long distributorIdParam,
			@RequestParam(name = "true_name", required = false) String trueName,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Locale locale = request.getLocale();
		long activityId = coalesceLong(activityIdParam, body, "activity_id");
		long recordId = coalesceLong(recordIdParam, body, "record_id");
		long distributorId = coalesceLong(distributorIdParam, body, "distributor_id");
		long userId = parseClaimsUserId(request);
		if (userId == 0L) {
			throw new ResourceException(
					messageSource.getMessage(RegistrationActivityFrontSubmitMessageKeys.ONLY_MEMBERS_CAN_REGISTER, null, locale));
		}
		long companyId = resolveCompanyId(request);
		String mobilePlain = resolveH5AuthMobilePlain(request, userId, companyId);
		String wxappAppid = parseClaimsString(request, "wxapp_appid");
		String openId = parseClaimsString(request, "open_id");

		if (activityId == 0L) {
			throw new ResourceException(
					messageSource.getMessage(RegistrationActivityFrontSubmitMessageKeys.PLEASE_SPECIFY_ACTIVITY, null, locale));
		}

		Object formdataContentRaw = resolveFormdataContent(request, body);
		String trimmedTrueName = trueName == null ? "" : trueName.trim();

		Map<String, Object> result =
				registrationActivityFrontRegistrationSubmitService.registrationSubmit(
						request,
						locale,
						userId,
						companyId,
						mobilePlain,
						wxappAppid,
						openId,
						activityId,
						recordId,
						distributorId,
						trimmedTrueName,
						formdataContentRaw);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@PostMapping(value = "/wxapp/joinActivity", name = "参加活动")
	public ResponseEntity<ApiResult<Map<String, Object>>> joinActivity(
			HttpServletRequest request,
			@RequestParam(name = "activity_id", defaultValue = "0") long activityIdParam,
			@RequestParam(name = "record_id", defaultValue = "0") long recordIdParam,
			@RequestParam(name = "distributor_id", defaultValue = "0") long distributorIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Locale locale = request.getLocale();
		long userId = parseClaimsUserId(request);
		long companyId = resolveCompanyId(request);
		long activityId = coalesceLong(activityIdParam, body, "activity_id");
		long recordId = coalesceLong(recordIdParam, body, "record_id");
		long distributorId = coalesceLong(distributorIdParam, body, "distributor_id");

		if (activityId == 0L) {
			throw new ResourceException(
					messageSource.getMessage(RegistrationActivityFrontSubmitMessageKeys.PLEASE_SPECIFY_ACTIVITY, null, locale));
		}
		if (userId == 0L) {
			throw new UnauthorizedException(
					messageSource.getMessage(RegistrationActivityFrontSubmitMessageKeys.ONLY_MEMBERS_CAN_REGISTER, null, locale));
		}
		if (recordId > 0L) {
			throw new ResourceException(
					messageSource.getMessage(
							RegistrationActivityFrontSubmitMessageKeys.JOIN_ACTIVITY_REGISTRATION_NOT_ALLOW_MODIFY, null, locale));
		}

		String mobilePlain = resolveH5AuthMobilePlain(request, userId, companyId);
		String wxappAppid = parseClaimsString(request, "wxapp_appid");
		String openId = parseClaimsString(request, "open_id");

		Map<String, Object> data =
				registrationActivityFrontRegistrationSubmitService.joinActivity(
						locale, userId, companyId, mobilePlain, wxappAppid, openId, activityId, distributorId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PostMapping(value = "/wxapp/cancelRecord", name = "取消记录")
	public ResponseEntity<ApiResult<Map<String, Object>>> cancelRecord(
			HttpServletRequest request,
			@RequestParam(name = "record_id", required = false) String recordIdQueryOrForm,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Locale locale = request.getLocale();
		long userId = parseClaimsUserId(request);
		if (userId == 0L) {
			throw new UnauthorizedException(
					messageSource.getMessage(
							RegistrationActivityFrontCancelRecordMessageKeys.PLEASE_LOGIN_BEFORE_CANCEL, null, locale));
		}
		long recordId = parseRecordIdFromRaw(resolveRecordIdRaw(recordIdQueryOrForm, body));
		if (recordId == 0L) {
			throw new ResourceException(
					messageSource.getMessage(
							RegistrationActivityFrontCancelRecordMessageKeys.PLEASE_SPECIFY_REGISTRATION_RECORD_ID,
							null,
							locale));
		}
		Map<String, Object> data = registrationActivityFrontCancelRecordService.cancelRecord(locale, userId, recordId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/wxapp/registrationActivityList", name = "报名活动列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getRegistrationActivityList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageRaw,
			@RequestParam(name = "pageSize", required = false) String pageSizeRaw,
			@RequestParam(name = "status", required = false) String statusRaw,
			@RequestParam(name = "activity_name", required = false) String activityName) {
		Locale locale = request.getLocale();
		String tag = locale.toLanguageTag();
		String requestLangTag = (tag == null || tag.isBlank()) ? "zh-CN" : tag;
		int page = pageRaw == null ? 1 : (int) LeadingNumberParser.parseAsLong(pageRaw.trim());
		int pageSize = pageSizeRaw == null ? 1 : (int) LeadingNumberParser.parseAsLong(pageSizeRaw.trim());
		int status = statusRaw == null ? 0 : (int) LeadingNumberParser.parseAsLong(statusRaw.trim());
		String activityNameTrimmed = activityName == null ? "" : activityName.trim();
		long companyId = resolveCompanyId(request);
		Map<String, Object> data = registrationActivityFrontGetRegistrationActivityListService.getRegistrationActivityList(
				locale,
				requestLangTag,
				companyId,
				parseClaimsUserId(request),
				page,
				pageSize,
				status,
				activityNameTrimmed);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static int parseRegistrationRecordListPage(String pageRaw) {
		if (pageRaw == null || pageRaw.isBlank()) {
			return 1;
		}
		long p = LeadingNumberParser.parseAsLong(pageRaw.trim());
		if (p <= 0L || p > Integer.MAX_VALUE) {
			throw new BadRequestException("page 格式不正确");
		}
		return (int) p;
	}

	private static int parseRegistrationRecordListPageSize(String pageSizeRaw) {
		if (pageSizeRaw == null || pageSizeRaw.isBlank()) {
			return 30;
		}
		long p = LeadingNumberParser.parseAsLong(pageSizeRaw.trim());
		if (p <= 0L || p > Integer.MAX_VALUE) {
			throw new BadRequestException("pageSize 格式不正确");
		}
		int ps = (int) p;
		if (ps > 200) {
			return 200;
		}
		return ps;
	}

	private static String resolveRecordIdRaw(String recordIdQueryOrForm, Map<String, Object> body) {
		String s1 = recordIdQueryOrForm == null ? "" : recordIdQueryOrForm.trim();
		if (StringUtils.hasText(s1)) {
			return s1;
		}
		if (body != null && body.get("record_id") != null) {
			Object v = body.get("record_id");
			String recordIdRaw;
			if (v instanceof Number n) {
				recordIdRaw = String.valueOf(n.longValue());
			} else {
				recordIdRaw = String.valueOf(v).trim();
			}
			if (StringUtils.hasText(recordIdRaw)) {
				return recordIdRaw;
			}
		}
		return null;
	}

	private static long parseRecordIdFromRaw(String recordIdRaw) {
		if (recordIdRaw == null) {
			return 0L;
		}
		String t = recordIdRaw.trim();
		if (!StringUtils.hasText(t)) {
			return 0L;
		}
		return LeadingNumberParser.parseAsLong(t);
	}

	private static Object resolveFormdataContent(HttpServletRequest request, Map<String, Object> body) {
		String flat = request.getParameter("formdata[content]");
		if (StringUtils.hasText(flat)) {
			return flat;
		}
		if (body == null) {
			return null;
		}
		Object fd = body.get("formdata");
		if (fd instanceof Map<?, ?> m) {
			return m.get("content");
		}
		return null;
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

	private static String parseClaimsMobile(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(raw instanceof Map<?, ?> claims)) {
			return "";
		}
		if (!claims.containsKey("mobile")) {
			return "";
		}
		Object v = claims.get("mobile");
		return v == null ? "" : String.valueOf(v).trim();
	}

	private String resolveH5AuthMobilePlain(HttpServletRequest request, long userId, long companyId) {
		String fromClaims = parseClaimsMobile(request);
		if (StringUtils.hasText(fromClaims)) {
			return fromClaims.trim();
		}
		if (userId <= 0L) {
			return "";
		}
		Map<String, Object> member = memberAccountService.getMemberInfo(userId, companyId);
		if (member == null || member.isEmpty()) {
			return "";
		}
		Object reg = member.get("region_mobile");
		if (reg != null && StringUtils.hasText(reg.toString().trim())) {
			return reg.toString().trim();
		}
		if (member.get("mobile") != null) {
			return String.valueOf(member.get("mobile")).trim();
		}
		return "";
	}

	private static String parseClaimsString(HttpServletRequest request, String key) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(raw instanceof Map<?, ?> claims)) {
			return "";
		}
		Object v = claims.get(key);
		return v == null ? "" : String.valueOf(v).trim();
	}

	private static long coalesceLong(long param, Map<String, Object> body, String key) {
		if (param != 0L) {
			return param;
		}
		if (body == null) {
			return 0L;
		}
		Object v = body.get(key);
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String t = String.valueOf(v).trim();
		if (!StringUtils.hasText(t)) {
			return 0L;
		}
		return LeadingNumberParser.parseAsLong(t);
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
}
