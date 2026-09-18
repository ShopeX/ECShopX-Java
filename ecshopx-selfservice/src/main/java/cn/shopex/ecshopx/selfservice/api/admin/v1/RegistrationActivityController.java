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
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.selfservice.dto.admin.v1.RegistrationActivityCreateRequest;
import cn.shopex.ecshopx.selfservice.service.RegistrationActivityCreateInputMerger;
import cn.shopex.ecshopx.selfservice.service.RegistrationActivityCreateService;
import cn.shopex.ecshopx.selfservice.service.RegistrationActivityDataInfoService;
import cn.shopex.ecshopx.selfservice.service.RegistrationActivityDatalistService;
import cn.shopex.ecshopx.selfservice.service.RegistrationActivityDeleteService;
import cn.shopex.ecshopx.selfservice.service.RegistrationActivityEasylistService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = false,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("selfserviceRegistrationActivityAdminV1")
@RequestMapping("/api/v1")
public class RegistrationActivityController {

	private final RegistrationActivityCreateService registrationActivityCreateService;
	private final RegistrationActivityCreateInputMerger registrationActivityCreateInputMerger;
	private final RegistrationActivityEasylistService registrationActivityEasylistService;
	private final RegistrationActivityDatalistService registrationActivityDatalistService;
	private final RegistrationActivityDataInfoService registrationActivityDataInfoService;
	private final RegistrationActivityDeleteService registrationActivityDeleteService;
	private final LangueProperties langueProperties;

	public RegistrationActivityController(
			RegistrationActivityCreateService registrationActivityCreateService,
			RegistrationActivityCreateInputMerger registrationActivityCreateInputMerger,
			RegistrationActivityEasylistService registrationActivityEasylistService,
			RegistrationActivityDatalistService registrationActivityDatalistService,
			RegistrationActivityDataInfoService registrationActivityDataInfoService,
			RegistrationActivityDeleteService registrationActivityDeleteService,
			LangueProperties langueProperties) {
		this.registrationActivityCreateService = registrationActivityCreateService;
		this.registrationActivityCreateInputMerger = registrationActivityCreateInputMerger;
		this.registrationActivityEasylistService = registrationActivityEasylistService;
		this.registrationActivityDatalistService = registrationActivityDatalistService;
		this.registrationActivityDataInfoService = registrationActivityDataInfoService;
		this.registrationActivityDeleteService = registrationActivityDeleteService;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "selfhelp.registrationActivity.add")
	@PostMapping(value = "/selfhelp/registrationActivity/create", name = "新增报名活动")
	public ResponseEntity<ApiResult<Map<String, Object>>> createData(
			HttpServletRequest httpRequest, @FlexibleBody RegistrationActivityCreateRequest body) {
		if (body == null) {
			throw new BadRequestException("请求体不能为空");
		}
		registrationActivityCreateInputMerger.mergeQueryIntoBody(httpRequest, body);
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) httpRequest.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		long companyId = parseCompanyId(jwt);
		String langTag = RequestLangTag.current(langueProperties);
		return ResponseEntity.ok(
				ApiResult.ok(registrationActivityCreateService.createData(body, companyId, langTag)));
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

	@Activated(routeAlias = "selfhelp.registrationActivity.edit")
	@PutMapping(value = "/selfhelp/registrationActivity/update", name = "更新报名活动")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateData(
			HttpServletRequest httpRequest, @FlexibleBody RegistrationActivityCreateRequest body) {
		if (body == null) {
			throw new BadRequestException("请求体不能为空");
		}
		registrationActivityCreateInputMerger.mergeQueryIntoBody(httpRequest, body);
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
		return ResponseEntity.ok(
				ApiResult.ok(registrationActivityCreateService.updateData(body, companyId, langTag, locale)));
	}

	@Activated(routeAlias = "selfhelp.registrationActivity.list")
	@GetMapping(value = "/selfhelp/registrationActivity/list", name = "获取报名活动列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDatalist(
			HttpServletRequest httpRequest,
			@RequestParam(name = "page", required = false) String page,
			@RequestParam(name = "pageSize", required = false) String pageSize,
			@RequestParam(name = "start_time", required = false) String startTime,
			@RequestParam(name = "end_time", required = false) String endTime,
			@RequestParam(name = "status", required = false) String status,
			@RequestParam(name = "is_valid", required = false) String isValid,
			@RequestParam(name = "distributor_id", required = false) String distributorId,
			@RequestParam(name = "field_title", required = false) String fieldTitle) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) httpRequest.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		long companyId = parseCompanyId(jwt);
		String langTag = RequestLangTag.current(langueProperties);
		Locale locale = httpRequest.getLocale();
		return ResponseEntity.ok(
				ApiResult.ok(registrationActivityDatalistService.getDatalist(
						companyId,
						langTag,
						locale,
						page,
						pageSize,
						startTime,
						endTime,
						status,
						isValid,
						distributorId,
						fieldTitle)));
	}

	@Activated(routeAlias = "selfhelp.registrationActivity.info")
	@GetMapping(value = "/selfhelp/registrationActivity/get", name = "获取报名活动详情")
	public ResponseEntity<ApiResult<Object>> getDataInfo(
			HttpServletRequest httpRequest,
			@RequestParam(name = "activity_id", required = false) String activityId) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) httpRequest.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		long companyId = parseCompanyId(jwt);
		String langTag = RequestLangTag.current(langueProperties);
		Object body = registrationActivityDataInfoService.getDataInfo(activityId, companyId, langTag);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	@Activated(routeAlias = "selfhelp.registrationActivity.delete")
	@PostMapping(value = "/selfhelp/registrationActivity/del", name = "删除报名活动")
	public ResponseEntity<ApiResult<Object>> deleteData(
			HttpServletRequest httpRequest, @FlexibleBody(required = false) Map<String, Object> body) {
		Object data = registrationActivityDeleteService.deleteData(
				httpRequest, body == null ? Collections.emptyMap() : body);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "selfhelp.registrationActivity.invalid")
	@PostMapping(value = "/selfhelp/registrationActivity/invalid", name = "过期报名活动")
	public ResponseEntity<ApiResult<Object>> restoreData(HttpServletRequest httpRequest) {
		Object data = registrationActivityCreateService.restoreData(httpRequest);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "selfhelp.registrationActivity.easylist")
	@GetMapping(value = "/selfhelp/registrationActivity/easylist", name = "获取报名活动列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getEasyDatalist(
			HttpServletRequest httpRequest,
			@RequestParam(name = "page", required = false) String page,
			@RequestParam(name = "pageSize", required = false) String pageSize,
			@RequestParam(name = "start_time", required = false) String startTime,
			@RequestParam(name = "end_time", required = false) String endTime,
			@RequestParam(name = "status", required = false) String status,
			@RequestParam(name = "is_valid", required = false) String isValid,
			@RequestParam(name = "distributor_id", required = false) String distributorId) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) httpRequest.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		long companyId = parseCompanyId(jwt);
		String langTag = RequestLangTag.current(langueProperties);
		return ResponseEntity.ok(
				ApiResult.ok(registrationActivityEasylistService.getEasyDatalist(
						companyId,
						langTag,
						page,
						pageSize,
						startTime,
						endTime,
						status,
						isValid,
						distributorId)));
	}
}
