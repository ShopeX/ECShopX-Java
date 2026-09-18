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
import cn.shopex.ecshopx.selfservice.dto.admin.v1.FormSettingCreateRequest;
import cn.shopex.ecshopx.selfservice.service.FormSettingCreateInputMerger;
import cn.shopex.ecshopx.selfservice.service.FormSettingCreateService;
import cn.shopex.ecshopx.selfservice.service.FormSettingDataInfoService;
import cn.shopex.ecshopx.selfservice.service.FormSettingDatalistService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
@RestController("selfserviceFormSettingAdminV1")
@RequestMapping("/api/v1")
public class FormSettingController {

	private final FormSettingCreateService formSettingCreateService;
	private final FormSettingCreateInputMerger formSettingCreateInputMerger;
	private final FormSettingDatalistService formSettingDatalistService;
	private final FormSettingDataInfoService formSettingDataInfoService;
	private final LangueProperties langueProperties;

	public FormSettingController(
			FormSettingCreateService formSettingCreateService,
			FormSettingCreateInputMerger formSettingCreateInputMerger,
			FormSettingDatalistService formSettingDatalistService,
			FormSettingDataInfoService formSettingDataInfoService,
			LangueProperties langueProperties) {
		this.formSettingCreateService = formSettingCreateService;
		this.formSettingCreateInputMerger = formSettingCreateInputMerger;
		this.formSettingDatalistService = formSettingDatalistService;
		this.formSettingDataInfoService = formSettingDataInfoService;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "selfhelp.form.add")
	@PostMapping(value = "/selfhelp/formdata", name = "新增表单元素配置项")
	public ResponseEntity<ApiResult<Map<String, Object>>> createData(
			HttpServletRequest httpRequest, @FlexibleBody FormSettingCreateRequest body) {
		if (body == null) {
			throw new BadRequestException("请求体不能为空");
		}
		formSettingCreateInputMerger.mergeQueryIntoBody(httpRequest, body);
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) httpRequest.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		long companyId = parseCompanyId(jwt);
		String langTag = RequestLangTag.current(langueProperties);
		Map<String, Object> data = formSettingCreateService.createData(body, companyId, langTag);
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

	@Activated(routeAlias = "selfhelp.form.edit")
	@PutMapping(value = "/selfhelp/formdata", name = "更新表单元素配置项")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateData(
			HttpServletRequest httpRequest, @FlexibleBody FormSettingCreateRequest body) {
		if (body == null) {
			throw new BadRequestException("请求体不能为空");
		}
		formSettingCreateInputMerger.mergeQueryIntoBody(httpRequest, body);
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
		Map<String, Object> data = formSettingCreateService.updateData(body, companyId, langTag, locale);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "selfhelp.form.list")
	@GetMapping(value = "/selfhelp/formdata", name = "获取表单元素配置项列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDatalist(
			HttpServletRequest httpRequest,
			@RequestParam(value = "page", required = false) String page,
			@RequestParam(value = "pageSize", required = false) String pageSize,
			@RequestParam(value = "form_element", required = false) String formElement,
			@RequestParam(value = "field_title", required = false) String fieldTitle,
			@RequestParam(value = "is_valid", required = false) String isValid,
			@RequestParam(value = "country_code", required = false) String countryCode) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) httpRequest.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		long companyId = parseCompanyId(jwt);
		String lang = countryCode;
		Map<String, Object> data =
				formSettingDatalistService.getDatalist(companyId, page, pageSize, formElement, fieldTitle, isValid, lang);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "selfhelp.form.info")
	@GetMapping(value = "/selfhelp/formdata/{id}", name = "获取表单元素配置项详情")
	public ApiResult<Object> getDataInfo(
			@PathVariable("id") String id,
			@RequestParam(value = "country_code", required = false) String countryCode) {
		Object data = formSettingDataInfoService.getDataInfo(id, countryCode);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "selfhelp.form.delete")
	@PostMapping(value = "/selfhelp/formdata/discard/{id}", name = "废弃表单元素配置项")
	public ResponseEntity<ApiResult<Object>> deleteData(@PathVariable("id") String id) {
		Object data = formSettingCreateService.deleteData(id);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "selfhelp.form.delete")
	@PostMapping(value = "/selfhelp/formdata/restore/{id}", name = "恢复表单元素配置项")
	public ResponseEntity<ApiResult<Object>> restoreData(
			HttpServletRequest httpRequest, @PathVariable("id") String id) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) httpRequest.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		long companyId = parseCompanyId(jwt);
		Object data = formSettingCreateService.restoreData(id, companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}
}
