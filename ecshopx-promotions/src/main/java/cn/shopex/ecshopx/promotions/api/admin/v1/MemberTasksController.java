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
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.promotions.service.export.MemberTaskCompletionExportService;
import cn.shopex.ecshopx.promotions.service.export.MemberTaskOverviewExportService;
import cn.shopex.ecshopx.promotions.service.membertask.MemberTaskCompletionListService;
import cn.shopex.ecshopx.promotions.service.membertask.MemberTaskDetailService;
import cn.shopex.ecshopx.promotions.service.membertask.MemberTaskGrantRetryService;
import cn.shopex.ecshopx.promotions.service.membertask.MemberTaskLifecycleService;
import cn.shopex.ecshopx.promotions.service.membertask.MemberTaskListService;
import cn.shopex.ecshopx.promotions.service.membertask.MemberTaskSaveService;
import cn.shopex.ecshopx.promotions.service.membertask.MemberTaskStatsService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("promotionsAdminV1MemberTasks")
@RequestMapping("/api/v1/promotions")
public class MemberTasksController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final MemberTaskListService memberTaskListService;
	private final MemberTaskDetailService memberTaskDetailService;
	private final MemberTaskSaveService memberTaskSaveService;
	private final MemberTaskLifecycleService memberTaskLifecycleService;
	private final MemberTaskGrantRetryService memberTaskGrantRetryService;
	private final MemberTaskStatsService memberTaskStatsService;
	private final MemberTaskCompletionExportService memberTaskCompletionExportService;
	private final MemberTaskOverviewExportService memberTaskOverviewExportService;
	private final MemberTaskCompletionListService memberTaskCompletionListService;
	private final LangueProperties langueProperties;

	public MemberTasksController(
			MemberTaskListService memberTaskListService,
			MemberTaskDetailService memberTaskDetailService,
			MemberTaskSaveService memberTaskSaveService,
			MemberTaskLifecycleService memberTaskLifecycleService,
			MemberTaskGrantRetryService memberTaskGrantRetryService,
			MemberTaskStatsService memberTaskStatsService,
			MemberTaskCompletionExportService memberTaskCompletionExportService,
			MemberTaskOverviewExportService memberTaskOverviewExportService,
			MemberTaskCompletionListService memberTaskCompletionListService,
			LangueProperties langueProperties) {
		this.memberTaskListService = memberTaskListService;
		this.memberTaskDetailService = memberTaskDetailService;
		this.memberTaskSaveService = memberTaskSaveService;
		this.memberTaskLifecycleService = memberTaskLifecycleService;
		this.memberTaskGrantRetryService = memberTaskGrantRetryService;
		this.memberTaskStatsService = memberTaskStatsService;
		this.memberTaskCompletionExportService = memberTaskCompletionExportService;
		this.memberTaskOverviewExportService = memberTaskOverviewExportService;
		this.memberTaskCompletionListService = memberTaskCompletionListService;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "promotions.member_task.list.get")
	@GetMapping(value = "/member-tasks", name = "会员任务列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> list(
			HttpServletRequest request,
			@RequestParam(value = "task_id", required = false) String taskIdKeyword,
			@RequestParam(value = "name", required = false) String nameKeyword,
			@RequestParam(value = "type_detail", required = false) String typeDetail,
			@RequestParam(value = "display_status", required = false) String displayStatus,
			@RequestParam(value = "filter_begin", required = false) String filterBeginRaw,
			@RequestParam(value = "filter_end", required = false) String filterEndRaw,
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "page_size", required = false) String pageSizeRaw) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		int page = parseLeadingIntQueryParam(pageRaw, 1);
		int pageSize = parseLeadingIntQueryParam(pageSizeRaw, 20);
		Integer filterBegin = parseOptionalEpoch(filterBeginRaw);
		Integer filterEnd = parseOptionalEpoch(filterEndRaw);
		Locale locale = LocaleContextHolder.getLocale();
		RequestLangTag.current(langueProperties);
		Map<String, Object> result =
				memberTaskListService.list(
						companyId,
						taskIdKeyword,
						nameKeyword,
						typeDetail,
						displayStatus,
						filterBegin,
						filterEnd,
						page,
						pageSize,
						locale);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "promotions.member_task.detail.get")
	@GetMapping(value = "/member-tasks/{taskId}", name = "会员任务详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> detail(
			HttpServletRequest request, @PathVariable("taskId") long taskId) {
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		Locale locale = LocaleContextHolder.getLocale();
		RequestLangTag.current(langueProperties);
		return ResponseEntity.ok(
				ApiResult.ok(memberTaskDetailService.detail(companyId, taskId, locale)));
	}

	@Activated(routeAlias = "promotions.member_task.create.post")
	@PostMapping(value = "/member-tasks", name = "新建会员任务")
	public ResponseEntity<ApiResult<Map<String, Object>>> create(
			HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		Locale locale = LocaleContextHolder.getLocale();
		RequestLangTag.current(langueProperties);
		long now = System.currentTimeMillis() / 1000L;
		Map<String, Object> result =
				memberTaskSaveService.create(
						companyId, body == null ? Map.of() : body, now, locale);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "promotions.member_task.update.put")
	@PutMapping(value = "/member-tasks/{taskId}", name = "编辑会员任务")
	public ResponseEntity<ApiResult<Map<String, Object>>> update(
			HttpServletRequest request,
			@PathVariable("taskId") long taskId,
			@RequestBody(required = false) Map<String, Object> body) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		Locale locale = LocaleContextHolder.getLocale();
		RequestLangTag.current(langueProperties);
		long now = System.currentTimeMillis() / 1000L;
		Map<String, Object> result =
				memberTaskSaveService.update(
						companyId, taskId, body == null ? Map.of() : body, now, locale);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "promotions.member_task.publish.post")
	@PostMapping(value = "/member-tasks/{taskId}/publish", name = "发布会员任务")
	public ResponseEntity<ApiResult<Map<String, Object>>> publish(
			HttpServletRequest request, @PathVariable("taskId") long taskId) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		Locale locale = LocaleContextHolder.getLocale();
		RequestLangTag.current(langueProperties);
		long now = System.currentTimeMillis() / 1000L;
		return ResponseEntity.ok(
				ApiResult.ok(memberTaskLifecycleService.publish(companyId, taskId, now, locale)));
	}

	@Activated(routeAlias = "promotions.member_task.off_shelf.post")
	@PostMapping(value = "/member-tasks/{taskId}/off-shelf", name = "下架会员任务")
	public ResponseEntity<ApiResult<Map<String, Object>>> offShelf(
			HttpServletRequest request, @PathVariable("taskId") long taskId) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		Locale locale = LocaleContextHolder.getLocale();
		RequestLangTag.current(langueProperties);
		long now = System.currentTimeMillis() / 1000L;
		return ResponseEntity.ok(
				ApiResult.ok(memberTaskLifecycleService.offShelf(companyId, taskId, now, locale)));
	}

	@Activated(routeAlias = "promotions.member_task.grant_retry.post")
	@PostMapping(value = "/member-tasks/{taskId}/grants/{grantId}/retry", name = "会员任务补发奖励")
	public ResponseEntity<ApiResult<Map<String, Object>>> retryGrant(
			HttpServletRequest request,
			@PathVariable("taskId") long taskId,
			@PathVariable("grantId") long grantId) {
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		Locale locale = LocaleContextHolder.getLocale();
		RequestLangTag.current(langueProperties);
		Map<String, Object> result =
				memberTaskGrantRetryService.retry(companyId, taskId, grantId, locale);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "promotions.member_task.completions.get")
	@GetMapping(value = "/member-tasks/{taskId}/completions", name = "会员任务完成明细分页")
	public ResponseEntity<ApiResult<Map<String, Object>>> completions(
			HttpServletRequest request,
			@PathVariable("taskId") long taskId,
			@RequestParam(value = "grant_status", required = false) String grantStatus,
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "page_size", required = false) String pageSizeRaw) {
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		int page = parseLeadingIntQueryParam(pageRaw, 1);
		int pageSize = parseLeadingIntQueryParam(pageSizeRaw, 20);
		Locale locale = LocaleContextHolder.getLocale();
		RequestLangTag.current(langueProperties);
		return ResponseEntity.ok(
				ApiResult.ok(
						memberTaskCompletionListService.listCompletions(
								companyId, taskId, grantStatus, page, pageSize, locale)));
	}

	@Activated(routeAlias = "promotions.member_task.export_overview.get")
	@GetMapping(value = "/member-tasks/export-overview", name = "会员任务完成概览导出")
	public ResponseEntity<ApiResult<Map<String, Object>>> exportOverview(
			HttpServletRequest request,
			@RequestParam(value = "task_id", required = false) String taskIdKeyword,
			@RequestParam(value = "name", required = false) String nameKeyword,
			@RequestParam(value = "type_detail", required = false) String typeDetail,
			@RequestParam(value = "display_status", required = false) String displayStatus,
			@RequestParam(value = "filter_begin", required = false) String filterBeginRaw,
			@RequestParam(value = "filter_end", required = false) String filterEndRaw) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		long operatorId = readOperatorId(jwt);
		long merchantId = readMerchantId(jwt);
		Integer filterBegin = parseOptionalEpoch(filterBeginRaw);
		Integer filterEnd = parseOptionalEpoch(filterEndRaw);
		Locale locale = LocaleContextHolder.getLocale();
		RequestLangTag.current(langueProperties);
		String datapass = request.getHeader("x-datapass-block");
		memberTaskOverviewExportService.exportOverview(
				companyId,
				operatorId,
				merchantId,
				taskIdKeyword,
				nameKeyword,
				typeDetail,
				displayStatus,
				filterBegin,
				filterEnd,
				datapass,
				locale);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "promotions.member_task.stats.get")
	@GetMapping(value = "/member-tasks/{taskId}/stats", name = "会员任务统计")
	public ResponseEntity<ApiResult<Map<String, Object>>> stats(
			HttpServletRequest request, @PathVariable("taskId") long taskId) {
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		Locale locale = LocaleContextHolder.getLocale();
		RequestLangTag.current(langueProperties);
		return ResponseEntity.ok(
				ApiResult.ok(memberTaskStatsService.stats(companyId, taskId, locale)));
	}

	@Activated(routeAlias = "promotions.member_task.export.get")
	@GetMapping(value = "/member-tasks/{taskId}/export", name = "会员任务导出")
	public ResponseEntity<ApiResult<Map<String, Object>>> export(
			HttpServletRequest request,
			@PathVariable("taskId") long taskId,
			@RequestParam(value = "grant_status", required = false) String grantStatus) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		long operatorId = readOperatorId(jwt);
		long merchantId = readMerchantId(jwt);
		Locale locale = LocaleContextHolder.getLocale();
		RequestLangTag.current(langueProperties);
		String datapass = request.getHeader("x-datapass-block");
		memberTaskCompletionExportService.exportCompletions(
				companyId, operatorId, merchantId, taskId, grantStatus, datapass, locale);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	private static Integer parseOptionalEpoch(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		long v = LeadingNumberParser.parseAsLong(raw.trim());
		if (v <= 0L || v > Integer.MAX_VALUE) {
			return null;
		}
		return (int) v;
	}

	private static int parseLeadingIntQueryParam(String raw, int defaultWhenBlank) {
		if (raw == null || raw.trim().isEmpty()) {
			return defaultWhenBlank;
		}
		long v = LeadingNumberParser.parseAsLong(raw.trim());
		if (v > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		if (v < Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}
		return (int) v;
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

	private static long readOperatorId(Map<String, Object> ud) {
		Object v = ud.get("operator_id");
		if (v == null) {
			return 0L;
		}
		try {
			return v instanceof Number n ? n.longValue() : Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long readMerchantId(Map<String, Object> ud) {
		Object v = ud.get("merchant_id");
		if (v == null) {
			return 0L;
		}
		try {
			return v instanceof Number n ? n.longValue() : Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
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
}
