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
import cn.shopex.ecshopx.promotions.service.membertask.MemberTaskListService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
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
	private final LangueProperties langueProperties;

	public MemberTasksController(
			MemberTaskListService memberTaskListService, LangueProperties langueProperties) {
		this.memberTaskListService = memberTaskListService;
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
