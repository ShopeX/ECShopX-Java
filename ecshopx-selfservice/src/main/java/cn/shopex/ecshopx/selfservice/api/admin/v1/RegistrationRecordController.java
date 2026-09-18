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
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.selfservice.dto.admin.v1.RegistrationRecordRegistrationVerifyCommand;
import cn.shopex.ecshopx.selfservice.dto.admin.v1.RegistrationRecordUpdateDataInfoCommand;
import cn.shopex.ecshopx.selfservice.service.RegistrationRecordRegistrationReviewInputMerger;
import cn.shopex.ecshopx.selfservice.service.RegistrationRecordRegistrationReviewService;
import cn.shopex.ecshopx.selfservice.service.RegistrationRecordDataInfoService;
import cn.shopex.ecshopx.selfservice.service.RegistrationRecordRegistrationVerifyInputMerger;
import cn.shopex.ecshopx.selfservice.service.RegistrationRecordRegistrationVerifyService;
import cn.shopex.ecshopx.selfservice.service.RegistrationRecordUpdateDataInfoInputMerger;
import cn.shopex.ecshopx.selfservice.service.RegistrationRecordUpdateDataInfoService;
import cn.shopex.ecshopx.selfservice.service.export.RegistrationRecordExportOrchestratorService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("selfserviceRegistrationRecordAdminV1")
@RequestMapping("/api/v1")
public class RegistrationRecordController {

	private final RegistrationRecordUpdateDataInfoService registrationRecordUpdateDataInfoService;
	private final RegistrationRecordUpdateDataInfoInputMerger registrationRecordUpdateDataInfoInputMerger;
	private final RegistrationRecordRegistrationVerifyInputMerger registrationRecordRegistrationVerifyInputMerger;
	private final RegistrationRecordRegistrationVerifyService registrationRecordRegistrationVerifyService;
	private final RegistrationRecordRegistrationReviewInputMerger registrationRecordRegistrationReviewInputMerger;
	private final RegistrationRecordRegistrationReviewService registrationRecordRegistrationReviewService;
	private final RegistrationRecordExportOrchestratorService registrationRecordExportOrchestratorService;
	private final RegistrationRecordDataInfoService registrationRecordDataInfoService;
	private final MessageSource messageSource;
	private final LangueProperties langueProperties;

	public RegistrationRecordController(
			RegistrationRecordUpdateDataInfoService registrationRecordUpdateDataInfoService,
			RegistrationRecordUpdateDataInfoInputMerger registrationRecordUpdateDataInfoInputMerger,
			RegistrationRecordRegistrationVerifyInputMerger registrationRecordRegistrationVerifyInputMerger,
			RegistrationRecordRegistrationVerifyService registrationRecordRegistrationVerifyService,
			RegistrationRecordRegistrationReviewInputMerger registrationRecordRegistrationReviewInputMerger,
			RegistrationRecordRegistrationReviewService registrationRecordRegistrationReviewService,
			RegistrationRecordExportOrchestratorService registrationRecordExportOrchestratorService,
			RegistrationRecordDataInfoService registrationRecordDataInfoService,
			MessageSource messageSource,
			LangueProperties langueProperties) {
		this.registrationRecordUpdateDataInfoService = registrationRecordUpdateDataInfoService;
		this.registrationRecordUpdateDataInfoInputMerger = registrationRecordUpdateDataInfoInputMerger;
		this.registrationRecordRegistrationVerifyInputMerger = registrationRecordRegistrationVerifyInputMerger;
		this.registrationRecordRegistrationVerifyService = registrationRecordRegistrationVerifyService;
		this.registrationRecordRegistrationReviewInputMerger = registrationRecordRegistrationReviewInputMerger;
		this.registrationRecordRegistrationReviewService = registrationRecordRegistrationReviewService;
		this.registrationRecordExportOrchestratorService = registrationRecordExportOrchestratorService;
		this.registrationRecordDataInfoService = registrationRecordDataInfoService;
		this.messageSource = messageSource;
		this.langueProperties = langueProperties;
	}

	@DataPass
	@Activated(routeAlias = "selfhelp.registrationRecord.list")
	@GetMapping(value = "/selfhelp/registrationRecord/list", name = "报名记录列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDatalist(
			HttpServletRequest httpRequest,
			@RequestParam(name = "page", required = false, defaultValue = "0") String pageParam,
			@RequestParam(name = "pageSize", required = false, defaultValue = "0") String pageSizeParam,
			@RequestParam(name = "activity_id", required = false) String activityIdParam,
			@RequestParam(name = "start_time", required = false) String startTimeRaw,
			@RequestParam(name = "end_time", required = false) String endTimeRaw,
			@RequestParam(name = "mobile", required = false) String mobileRaw,
			@RequestParam(name = "status", required = false) String statusRaw,
			@RequestParam(name = "true_name", required = false) String trueNameRaw,
			@RequestParam(name = "is_white_list", required = false) String isWhiteListRaw) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) httpRequest.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		long companyId = parseCompanyId(jwt);
		String langTag = RequestLangTag.current(langueProperties);
		boolean datapassBlocked = resolveDatapassBlocked(httpRequest);
		Locale locale = LocaleContextHolder.getLocale();
		int page = parseNonNegativeIntDefaultZero(pageParam);
		int pageSize = parseNonNegativeIntDefaultZero(pageSizeParam);
		Long activityId = parsePositiveLongOrNull(activityIdParam);
		String mobileTrim = trimToNull(mobileRaw);
		String statusTrim = trimToNull(statusRaw);
		String trueNameTrim = trimToNull(trueNameRaw);
		Map<String, Object> resultMap =
				registrationRecordDataInfoService.getDatalist(
						companyId,
						langTag,
						locale,
						page,
						pageSize,
						activityId,
						startTimeRaw,
						endTimeRaw,
						mobileTrim,
						statusTrim,
						trueNameTrim,
						isWhiteListRaw,
						datapassBlocked);
		return ResponseEntity.ok(ApiResult.ok(resultMap));
	}

	private static int parseNonNegativeIntDefaultZero(String raw) {
		if (!StringUtils.hasText(raw)) {
			return 0;
		}
		try {
			int v = Integer.parseInt(raw.trim());
			return Math.max(0, v);
		} catch (NumberFormatException ex) {
			return 0;
		}
	}

	private static Long parsePositiveLongOrNull(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		try {
			long v = Long.parseLong(raw.trim());
			return v > 0L ? v : null;
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private static String trimToNull(String raw) {
		if (raw == null) {
			return null;
		}
		String t = StringUtils.trimWhitespace(raw);
		return StringUtils.hasText(t) ? t : null;
	}

	@DataPass
	@Activated(routeAlias = "selfhelp.registrationRecord.info")
	@GetMapping(value = "/selfhelp/registrationRecord/get", name = "获取表单模板详情")
	public ResponseEntity<ApiResult<Object>> getDataInfo(
			HttpServletRequest httpRequest,
			@RequestParam(name = "record_id", required = false) String recordIdParam) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) httpRequest.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Long recordId = parsePositiveRecordId(recordIdParam);
		if (recordId == null) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		Locale locale = LocaleContextHolder.getLocale();
		String langTag = RequestLangTag.current(langueProperties);
		boolean datapassBlocked = resolveDatapassBlocked(httpRequest);
		Map<String, Object> body =
				registrationRecordDataInfoService.getDataInfo(recordId, langTag, locale, datapassBlocked);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	private static Long parsePositiveRecordId(String recordIdParam) {
		if (recordIdParam == null) {
			return null;
		}
		String t = StringUtils.trimWhitespace(recordIdParam);
		if (!StringUtils.hasText(t) || "0".equals(t)) {
			return null;
		}
		try {
			long id = Long.parseLong(t);
			if (id <= 0L) {
				return null;
			}
			return id;
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private static boolean resolveDatapassBlocked(HttpServletRequest request) {
		if (truthyDatapassToken(request.getAttribute("x-datapass-block"))) {
			return true;
		}
		if (truthyDatapassToken(request.getParameter("x-datapass-block"))) {
			return true;
		}
		if (truthyDatapassToken(request.getHeader("x-datapass-block"))) {
			return true;
		}
		return false;
	}

	private static boolean truthyDatapassToken(Object token) {
		if (token == null) {
			return false;
		}
		if (token instanceof Number n) {
			return n.intValue() != 0;
		}
		if (Boolean.TRUE.equals(token)) {
			return true;
		}
		String t = token.toString().trim();
		if (t.isEmpty() || "0".equals(t) || "false".equalsIgnoreCase(t)) {
			return false;
		}
		return true;
	}

	@DataPass
	@Activated(routeAlias = "selfhelp.registrationRecord.update")
	@PostMapping(value = "/selfhelp/registrationRecord/update", name = "更新报名记录")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateDataInfo(
			HttpServletRequest httpRequest, @FlexibleBody(required = false) JsonNode body) {
		Locale locale = LocaleContextHolder.getLocale();
		RegistrationRecordUpdateDataInfoCommand cmd =
				registrationRecordUpdateDataInfoInputMerger.merge(httpRequest, body, locale);
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) httpRequest.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		long companyId = parseCompanyId(jwt);
		String langTag = RequestLangTag.current(langueProperties);
		Map<String, Object> resultMap =
				registrationRecordUpdateDataInfoService.updateDataInfo(companyId, cmd, langTag, locale);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("status", true);
		payload.put("result", resultMap);
		return ResponseEntity.ok(ApiResult.ok(payload));
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

	@Activated(routeAlias = "selfhelp.registrationRecord.review")
	@PutMapping(value = "/selfhelp/registrationReview", name = "报名审核")
	public ResponseEntity<ApiResult<Map<String, Object>>> registrationReview(
			HttpServletRequest httpRequest, @FlexibleBody(required = false) JsonNode body) {
		Locale locale = LocaleContextHolder.getLocale();
		var cmd = registrationRecordRegistrationReviewInputMerger.merge(httpRequest, body, locale);
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) httpRequest.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		long companyId = parseCompanyId(jwt);
		String langTag = RequestLangTag.current(langueProperties);
		Map<String, Object> row =
				registrationRecordRegistrationReviewService.registrationReview(companyId, cmd, langTag, locale);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", row);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "selfhelp.registrationRecord.verify")
	@PostMapping(value = "/selfhelp/registrationVerify", name = "报名记录核销")
	public ResponseEntity<ApiResult<Map<String, Object>>> registrationVerify(
			HttpServletRequest httpRequest, @FlexibleBody(required = false) JsonNode body) {
		Locale locale = LocaleContextHolder.getLocale();
		RegistrationRecordRegistrationVerifyCommand cmd =
				registrationRecordRegistrationVerifyInputMerger.merge(httpRequest, body, locale);
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) httpRequest.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		String mobile = jwt.get("mobile") == null ? null : String.valueOf(jwt.get("mobile"));
		Map<String, Object> row =
				registrationRecordRegistrationVerifyService.registrationVerify(cmd, mobile, locale);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", row);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "selfhelp.registrationRecord.verify_log")
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@GetMapping(value = "/selfhelp/registrationVerifyLog", name = "报名记录核销记录列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> registrationVerifyLog(
			HttpServletRequest httpRequest,
			@RequestParam(name = "page", required = false, defaultValue = "0") String pageParam,
			@RequestParam(name = "pageSize", required = false, defaultValue = "0") String pageSizeParam,
			@RequestParam(name = "activity_name", required = false) String ignoredActivityName,
			@RequestParam(name = "activity_id", required = false) String ignoredActivityId,
			@RequestParam(name = "verify_time", required = false) String ignoredVerifyTime,
			@RequestParam(name = "mobile", required = false) String ignoredMobile,
			@RequestParam(name = "distributor_id", required = false) String ignoredDistributorId) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) httpRequest.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		long companyId = parseCompanyId(jwt);
		String langTag = RequestLangTag.current(langueProperties);
		boolean datapassBlocked = resolveDatapassBlocked(httpRequest);
		Locale locale = LocaleContextHolder.getLocale();
		int page = parseNonNegativeIntDefaultZero(pageParam);
		int pageSize = parseNonNegativeIntDefaultZero(pageSizeParam);
		Map<String, Object> resultMap =
				registrationRecordDataInfoService.registrationVerifyLog(
						companyId, langTag, locale, page, pageSize, datapassBlocked);
		return ResponseEntity.ok(ApiResult.ok(resultMap));
	}

	@DataPass
	@Activated(routeAlias = "selfhelp.registrationRecord.export")
	@GetMapping(value = "/selfhelp/registrationRecord/export", name = "导出报名记录")
	public ResponseEntity<Map<String, Object>> exportRegistrationRecord(
			HttpServletRequest httpRequest,
			@RequestParam(name = "activity_id", required = false) String activityIdParam,
			@RequestParam(name = "start_time", required = false) String startTimeRaw,
			@RequestParam(name = "end_time", required = false) String endTimeRaw,
			@RequestParam(name = "mobile", required = false) String mobileRaw) {
		Locale locale = LocaleContextHolder.getLocale();
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) httpRequest.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		long companyId = parseCompanyId(jwt);
		long operatorId = parseOperatorId(jwt);
		long activityId = parseExportActivityId(activityIdParam, locale);
		String datapass = httpRequest.getParameter("x-datapass-block");
		if (!StringUtils.hasText(datapass)) {
			datapass = httpRequest.getHeader("x-datapass-block");
		}
		if (datapass == null) {
			datapass = "";
		}
		registrationRecordExportOrchestratorService.exportRegistrationRecord(
				companyId, operatorId, activityId, mobileRaw, startTimeRaw, endTimeRaw, datapass, locale);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("status", Boolean.TRUE);
		return ResponseEntity.ok(body);
	}

	private long parseExportActivityId(String activityIdParam, Locale locale) {
		if (!StringUtils.hasText(activityIdParam)) {
			throw new ResourceException(
					messageSource.getMessage("selfservice.registration_record.export_please_select_activity", null, locale));
		}
		String t = activityIdParam.trim();
		try {
			long v = Long.parseLong(t);
			if (v <= 0L) {
				throw new ResourceException(
						messageSource.getMessage("selfservice.registration_record.export_please_select_activity", null, locale));
			}
			return v;
		} catch (NumberFormatException ex) {
			throw new BadRequestException("activity_id 参数格式错误");
		}
	}

	private static long parseOperatorId(Map<String, Object> jwt) {
		Object oid = jwt.get("operator_id");
		if (oid == null) {
			return 0L;
		}
		if (oid instanceof Number n) {
			return n.longValue();
		}
		if (oid instanceof String s) {
			if (!StringUtils.hasText(s)) {
				return 0L;
			}
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException ex) {
				return 0L;
			}
		}
		try {
			return Long.parseLong(String.valueOf(oid).trim());
		} catch (NumberFormatException ex) {
			return 0L;
		}
	}
}
