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

package cn.shopex.ecshopx.aliyunsms.api.admin.v1;

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsRecordListDatapassService;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsRecordListService;
import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsRecordListService.RecordListQuery;
import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsShopRoutePermissionService;
import cn.shopex.ecshopx.aliyunsms.web.AliyunsmsDatapassBlockResolver;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO
)
@RestController("aliyunsmsRecordAdminV1")
@RequestMapping("/api/v1/aliyunsms")
public class RecordController {

	private final CompanysActivationService companysActivationService;
	private final AliyunsmsShopRoutePermissionService aliyunsmsShopRoutePermissionService;
	private final AliyunsmsRecordListDatapassService aliyunsmsRecordListDatapassService;
	private final AliyunsmsRecordListService aliyunsmsRecordListService;

	public RecordController(
			CompanysActivationService companysActivationService,
			AliyunsmsShopRoutePermissionService aliyunsmsShopRoutePermissionService,
			AliyunsmsRecordListDatapassService aliyunsmsRecordListDatapassService,
			AliyunsmsRecordListService aliyunsmsRecordListService) {
		this.companysActivationService = companysActivationService;
		this.aliyunsmsShopRoutePermissionService = aliyunsmsShopRoutePermissionService;
		this.aliyunsmsRecordListDatapassService = aliyunsmsRecordListDatapassService;
		this.aliyunsmsRecordListService = aliyunsmsRecordListService;
	}

	@DataPass
	@Activated(routeAlias = "aliyunsms.record.getList")
	@GetMapping(value = "/record/list", name = "aliyunsms.record.getList")
	public ResponseEntity<ApiResult<Map<String, Object>>> getList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "page_size", required = false) Integer pageSizeUnderscore,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "template_type", required = false) String templateType,
			@RequestParam(value = "template_code", required = false) String templateCode,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "sms_content", required = false) String smsContent,
			@RequestParam(value = "task_name", required = false) String taskName,
			@RequestParam(value = "task_id", required = false) String taskId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}

		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		aliyunsmsShopRoutePermissionService.assertRecordGetList(user);
		aliyunsmsRecordListDatapassService.apply(request, user);

		RecordListQuery q = new RecordListQuery();
		q.setPage(parsePage(request.getParameter("page")));
		q.setPageSize(resolvePageSize(request));
		var pm = request.getParameterMap();
		q.setStatusKeyPresent(pm.containsKey("status"));
		q.setStatusValue(status);
		q.setTemplateTypeKeyPresent(pm.containsKey("template_type"));
		q.setTemplateTypeValue(templateType);
		q.setTemplateCode(templateCode);
		q.setMobile(mobile);
		q.setSmsContent(smsContent);
		q.setTaskName(taskName);
		q.setTaskId(taskId);
		q.setTimeStart(resolveTimeStart(request));

		Map<String, Object> data = aliyunsmsRecordListService.getList(companyId, q);
		boolean blocked = AliyunsmsDatapassBlockResolver.isBlocked(request);
		data.put("datapass_block", blocked ? 1 : 0);
		if (blocked) {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("list");
			if (list != null && !list.isEmpty()) {
				List<Map<String, Object>> masked = new ArrayList<>(list.size());
				for (Map<String, Object> row : list) {
					Map<String, Object> copy = new LinkedHashMap<>(row);
					Object m = copy.get("mobile");
					if (m != null) {
						copy.put("mobile", DataMasking.maskMobile(m.toString()));
					}
					masked.add(copy);
				}
				data.put("list", masked);
			}
		}

		return ResponseEntity.ok(ApiResult.ok(data));
	}

	/**
	 * Resolves list query parameters for the time range: {@code time_start[]=…&time_start[]=…}, repeated
	 * {@code time_start=…}, and optional {@code time_end[]} / {@code time_end} when only one {@code time_start}
	 * value is supplied (paired with end as the second bound). Bracketed keys are read from the raw parameter map.
	 */
	private static List<String> resolveTimeStart(HttpServletRequest request) {
		Map<String, String[]> pm = request.getParameterMap();
		List<String> parts = new ArrayList<>();
		if (pm.containsKey("time_start[]")) {
			addParamValues(parts, request.getParameterValues("time_start[]"));
		} else {
			addParamValues(parts, request.getParameterValues("time_start"));
		}
		if (parts.isEmpty()) {
			return null;
		}
		if (parts.size() == 1) {
			String end = firstParameterValue(request, pm, "time_end[]", "time_end");
			if (end != null) {
				parts.add(end);
			}
		}
		if (parts.size() > 2) {
			return List.of(parts.get(0), parts.get(1));
		}
		return Collections.unmodifiableList(parts);
	}

	private static void addParamValues(List<String> target, String[] values) {
		if (values == null) {
			return;
		}
		for (String s : values) {
			if (s != null) {
				target.add(s);
			}
		}
	}

	private static String firstParameterValue(
			HttpServletRequest request, Map<String, String[]> pm, String bracketKey, String plainKey) {
		String[] raw =
				pm.containsKey(bracketKey) ? request.getParameterValues(bracketKey) : request.getParameterValues(plainKey);
		if (raw == null || raw.length == 0) {
			return null;
		}
		return raw[0];
	}

	private static int resolvePageSize(HttpServletRequest request) {
		var pm = request.getParameterMap();
		if (pm.containsKey("pageSize")) {
			return parsePositiveIntOrDefault(request.getParameter("pageSize"), 10);
		}
		if (pm.containsKey("page_size")) {
			return parsePositiveIntOrDefault(request.getParameter("page_size"), 10);
		}
		return 10;
	}

	private static int parsePositiveIntOrDefault(String raw, int def) {
		if (raw == null) {
			return def;
		}
		try {
			int v = Integer.parseInt(raw.trim());
			return v > 0 ? v : def;
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static int parsePage(String p) {
		if (p == null) {
			return 1;
		}
		try {
			int v = Integer.parseInt(p.trim());
			return v > 0 ? v : 1;
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}
}
