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

package cn.shopex.ecshopx.promotions.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.promotions.service.PointupvaluationActivityCreateService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
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
@RestController("promotionsAdminV1PointupvaluationActivity")
@RequestMapping("/api/v1/promotions/pointupvaluation")
public class PointupvaluationActivityController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final PointupvaluationActivityCreateService pointupvaluationActivityCreateService;
	private final MessageSource messageSource;
	private final LangueProperties langueProperties;

	public PointupvaluationActivityController(
			PointupvaluationActivityCreateService pointupvaluationActivityCreateService,
			MessageSource messageSource,
			LangueProperties langueProperties) {
		this.pointupvaluationActivityCreateService = pointupvaluationActivityCreateService;
		this.messageSource = messageSource;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "promotions.pointupvaluation.list")
	@GetMapping(value = "/lists", name = "积分升值列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getActivityList(HttpServletRequest request) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		String countryCodeTrimmed = Objects.toString(request.getParameter("country_code"), "").trim();
		String resolved = RequestLangTag.current(langueProperties);
		String langTag = StringUtils.hasText(countryCodeTrimmed) ? countryCodeTrimmed : resolved;
		Locale locale = Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		int page = parsePointupvaluationPageOrDefault(request.getParameter("page"), locale);
		int pageSize = parsePointupvaluationPageSizeOrDefault(request.getParameter("pageSize"), locale);
		String title = request.getParameter("title");
		String activityStatus = request.getParameter("activity_status");
		String beginTime = request.getParameter("begin_time");
		String endTime = request.getParameter("end_time");
		Map<String, Object> data =
				pointupvaluationActivityCreateService.getActivityList(
						companyId, title, activityStatus, beginTime, endTime, page, pageSize, langTag);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private int parsePointupvaluationPageOrDefault(String pageRaw, Locale locale) {
		if (pageRaw == null || pageRaw.trim().isEmpty()) {
			return 1;
		}
		return parseStrictPositivePaginationInt(pageRaw.trim(), locale);
	}

	private int parsePointupvaluationPageSizeOrDefault(String pageSizeRaw, Locale locale) {
		if (pageSizeRaw == null || pageSizeRaw.trim().isEmpty()) {
			return 20;
		}
		return parseStrictPositivePaginationInt(pageSizeRaw.trim(), locale);
	}

	private int parseStrictPositivePaginationInt(String s, Locale locale) {
		try {
			long v = Long.parseLong(s);
			if (v < 1L || v > Integer.MAX_VALUE) {
				throw new BadRequestException(
						messageSource.getMessage("promotions.active_article.pagination_invalid", null, locale));
			}
			return (int) v;
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.active_article.pagination_invalid", null, locale));
		}
	}

	@Activated(routeAlias = "promotions.pointupvaluation.add")
	@PostMapping(value = "/create", name = "创建积分升值")
	public ResponseEntity<ApiResult<Map<String, Object>>> createActivity(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> merged = mergeInput(request, body);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		merged.put("company_id", companyId);
		String countryCodeTrimmed = Objects.toString(merged.get("country_code"), "").trim();
		String resolved = RequestLangTag.current(langueProperties);
		String langTag = StringUtils.hasText(countryCodeTrimmed) ? countryCodeTrimmed : resolved;
		Map<String, Object> row = pointupvaluationActivityCreateService.createActivity(merged, langTag);
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	private static Map<String, Object> mergeInput(HttpServletRequest request, Map<String, Object> body) {
		LinkedHashMap<String, Object> input = new LinkedHashMap<>();
		request.getParameterMap()
				.forEach(
						(k, v) -> {
							if (v != null && v.length > 0 && StringUtils.hasText(v[0])) {
								input.put(k, v[0]);
							}
						});
		if (body != null) {
			input.putAll(body);
		}
		return input;
	}

	private static Map<String, Object> readOperatorJwtMap(HttpServletRequest request) {
		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud) || ud.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			Object k = e.getKey();
			if (k != null) {
				out.put(k.toString(), e.getValue());
			}
		}
		return out;
	}

	private static long readCompanyIdFromOperatorJwtMap(Map<String, Object> ud) {
		Object v = ud.get("company_id");
		if (v == null) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		try {
			long id = v instanceof Number n ? n.longValue() : Long.parseLong(v.toString().trim());
			if (id <= 0L) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
			return id;
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
	}

	@Activated(routeAlias = "promotions.pointupvaluation.update")
	@PutMapping(value = "/update", name = "修改积分升值")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateActivity(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> merged = mergeInput(request, body);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		merged.put("company_id", companyId);
		String countryCodeTrimmed = Objects.toString(merged.get("country_code"), "").trim();
		String resolved = RequestLangTag.current(langueProperties);
		String langTag = StringUtils.hasText(countryCodeTrimmed) ? countryCodeTrimmed : resolved;
		Map<String, Object> row = pointupvaluationActivityCreateService.updateActivity(merged, langTag);
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@Activated(routeAlias = "promotions.pointupvaluation.info")
	@GetMapping(value = "/getinfo", name = "积分升值详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getActivityInfo(HttpServletRequest request) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		String countryCodeTrimmed = Objects.toString(request.getParameter("country_code"), "").trim();
		String resolved = RequestLangTag.current(langueProperties);
		String langTag = StringUtils.hasText(countryCodeTrimmed) ? countryCodeTrimmed : resolved;
		String raw = request.getParameter("activity_id");
		Map<String, Object> row = pointupvaluationActivityCreateService.getActivityInfo(companyId, raw, langTag);
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@Activated(routeAlias = "promotions.pointupvaluation.statusupdate")
	@PutMapping(value = "/updatestatus", name = "积分升值状态")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateStatus(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeInput(request, body);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		merged.put("company_id", companyId);
		pointupvaluationActivityCreateService.updateStatus(merged);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}
}
