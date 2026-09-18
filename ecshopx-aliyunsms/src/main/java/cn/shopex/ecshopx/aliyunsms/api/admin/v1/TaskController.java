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

import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsShopRoutePermissionService;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsTaskAddService;
import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsTaskInfoService;
import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsTaskListService;
import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsTaskListService.TaskListQuery;
import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsTaskModifyService;
import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsTaskRevokeService;
import cn.shopex.ecshopx.aliyunsms.web.AliyunsmsAdminFlexibleInputMerge;
import cn.shopex.ecshopx.aliyunsms.web.AliyunsmsAdminSignInfoQueryId;
import cn.shopex.ecshopx.aliyunsms.web.AliyunsmsTaskUserIdList;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422
)
@RestController("aliyunsmsTaskAdminV1")
@RequestMapping("/api/v1/aliyunsms")
public class TaskController {

	private final CompanysActivationService companysActivationService;
	private final AliyunsmsShopRoutePermissionService aliyunsmsShopRoutePermissionService;
	private final AliyunsmsTaskAddService aliyunsmsTaskAddService;
	private final AliyunsmsTaskModifyService aliyunsmsTaskModifyService;
	private final AliyunsmsTaskRevokeService aliyunsmsTaskRevokeService;
	private final AliyunsmsTaskInfoService aliyunsmsTaskInfoService;
	private final AliyunsmsTaskListService aliyunsmsTaskListService;
	private final OperatorLogsWriteService operatorLogsWriteService;
	private final ObjectMapper objectMapper;

	public TaskController(
			CompanysActivationService companysActivationService,
			AliyunsmsShopRoutePermissionService aliyunsmsShopRoutePermissionService,
			AliyunsmsTaskAddService aliyunsmsTaskAddService,
			AliyunsmsTaskModifyService aliyunsmsTaskModifyService,
			AliyunsmsTaskRevokeService aliyunsmsTaskRevokeService,
			AliyunsmsTaskInfoService aliyunsmsTaskInfoService,
			AliyunsmsTaskListService aliyunsmsTaskListService,
			OperatorLogsWriteService operatorLogsWriteService,
			ObjectMapper objectMapper) {
		this.companysActivationService = companysActivationService;
		this.aliyunsmsShopRoutePermissionService = aliyunsmsShopRoutePermissionService;
		this.aliyunsmsTaskAddService = aliyunsmsTaskAddService;
		this.aliyunsmsTaskModifyService = aliyunsmsTaskModifyService;
		this.aliyunsmsTaskRevokeService = aliyunsmsTaskRevokeService;
		this.aliyunsmsTaskInfoService = aliyunsmsTaskInfoService;
		this.aliyunsmsTaskListService = aliyunsmsTaskListService;
		this.operatorLogsWriteService = operatorLogsWriteService;
		this.objectMapper = objectMapper;
	}

	@Activated(routeAlias = "aliyunsms.task.add")
	@PostMapping(value = "/task/add", name = "添加群发任务")
	public ResponseEntity<ApiResult<Map<String, Object>>> addTask(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
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
		aliyunsmsShopRoutePermissionService.assertTaskAdd(user);

		Map<String, Object> merged = AliyunsmsAdminFlexibleInputMerge.merge(request, body);

		if (!merged.containsKey("task_name")) {
			throw new BadRequestException("缺少必填字段: task_name");
		}
		Object taskNameRaw = merged.get("task_name");
		String taskName =
				taskNameRaw == null
						? ""
						: (taskNameRaw instanceof String s ? s.trim() : String.valueOf(taskNameRaw).trim());
		if (taskName.isEmpty()) {
			throw new BadRequestException("任务名称必填");
		}

		int signId =
				parseRequiredMinInt(
						merged,
						"sign_id",
						1,
						"签名必填",
						"签名id必须是大于等于1的整数");
		int templateId =
				parseRequiredMinInt(
						merged,
						"template_id",
						1,
						"模板必填",
						"模板id必须是大于等于1的整数");

		long sendAtSeconds;
		if (!merged.containsKey("send_at") || isEmptySendAt(merged.get("send_at"))) {
			sendAtSeconds = java.time.Instant.now().getEpochSecond();
		} else {
			Object sendRaw = merged.get("send_at");
			String s = sendAtRawToString(sendRaw);
			String prefix = s.substring(0, Math.max(0, s.length() - 3));
			sendAtSeconds = parseLongSecondsOrBadRequest(prefix);
		}

		List<Long> userIdsOrNull = AliyunsmsTaskUserIdList.parseForTaskAdd(merged);

		aliyunsmsTaskAddService.addTask(companyId, taskName, signId, templateId, sendAtSeconds, userIdsOrNull);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/aliyunsms/task/add");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "添加群发任务");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		try {
			operatorLogsWriteService.addLogs(logCtx);
		} catch (Exception ignored) {
		}

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "aliyunsms.task.modify")
	@PostMapping(value = "/task/modify", name = "编辑群发任务")
	public ResponseEntity<ApiResult<Map<String, Object>>> modifyTask(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
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
		aliyunsmsShopRoutePermissionService.assertTaskModify(user);

		Map<String, Object> merged = AliyunsmsAdminFlexibleInputMerge.merge(request, body);

		int idInt = parseRequiredIntField(merged, "id", "id必填");
		long taskId = idInt;
		if (taskId <= 0) {
			throw new BadRequestException("id必填");
		}

		if (!merged.containsKey("task_name")) {
			throw new BadRequestException("缺少必填字段: task_name");
		}
		Object taskNameRaw = merged.get("task_name");
		String taskName =
				taskNameRaw == null
						? ""
						: (taskNameRaw instanceof String s ? s.trim() : String.valueOf(taskNameRaw).trim());
		if (taskName.isEmpty()) {
			throw new BadRequestException("任务名称必填");
		}

		int signId =
				parseRequiredMinInt(
						merged,
						"sign_id",
						1,
						"签名必填",
						"签名id必须是大于等于1的整数");
		int templateId =
				parseRequiredMinInt(
						merged,
						"template_id",
						1,
						"模板必填",
						"模板id必须是大于等于1的整数");

		long sendAtSeconds;
		if (!merged.containsKey("send_at") || isEmptySendAt(merged.get("send_at"))) {
			sendAtSeconds = java.time.Instant.now().getEpochSecond();
		} else {
			Object sendRaw = merged.get("send_at");
			String s = sendAtRawToString(sendRaw);
			String prefix = s.substring(0, Math.max(0, s.length() - 3));
			sendAtSeconds = parseLongSecondsOrBadRequest(prefix);
		}

		aliyunsmsTaskModifyService.modifyTask(companyId, taskId, taskName, signId, templateId, sendAtSeconds);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/aliyunsms/task/modify");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "编辑群发任务");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		try {
			operatorLogsWriteService.addLogs(logCtx);
		} catch (Exception ignored) {
		}

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "aliyunsms.task.list")
	@GetMapping(value = "/task/list", name = "群发任务列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) Integer pageIgnored,
			@RequestParam(value = "pagSize", required = false) Integer pagSizeIgnored,
			@RequestParam(value = "pageSize", required = false) Integer pageSizeIgnored,
			@RequestParam(value = "page_size", required = false) Integer pageSizeUnderscoreIgnored,
			@RequestParam(value = "task_name", required = false) String taskNameIgnored,
			@RequestParam(value = "template_name", required = false) String templateNameIgnored,
			@RequestParam(value = "status", required = false) String statusIgnored,
			@RequestParam(value = "time_start", required = false) String timeStartIgnored) {
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
		aliyunsmsShopRoutePermissionService.assertTaskGetList(user);

		TaskListQuery q = new TaskListQuery();
		q.setPage(parsePageRawForTaskList(request.getParameter("page")));
		q.setPageSize(resolvePageSize(request));
		applyTaskNameToQuery(request, q);
		applyTemplateNameToQuery(request, q);
		var pm = request.getParameterMap();
		q.setStatusKeyPresent(pm.containsKey("status"));
		q.setStatusValue(request.getParameter("status"));
		q.setTimeStartParts(resolveTimeStart(request));

		Map<String, Object> data = aliyunsmsTaskListService.getList(companyId, q);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "aliyunsms.task.info")
	@GetMapping(value = "/task/info", name = "aliyunsms.task.info")
	public ResponseEntity<ApiResult<Object>> getInfo(HttpServletRequest request) {
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
		aliyunsmsShopRoutePermissionService.assertTaskGetInfo(user);

		Optional<Long> idOpt = AliyunsmsAdminSignInfoQueryId.resolveSignInfoPrimaryKey(request);
		Long id = idOpt.orElse(null);
		Map<String, Object> data = aliyunsmsTaskInfoService.getInfo(companyId, id);
		if (data.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(List.of()));
		}
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "aliyunsms.task.revoke")
	@PostMapping(value = "/task/revoke", name = "群发任务撤销")
	public ResponseEntity<ApiResult<Map<String, Object>>> revokeTask(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
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
		aliyunsmsShopRoutePermissionService.assertTaskRevoke(user);

		Map<String, Object> merged = AliyunsmsAdminFlexibleInputMerge.merge(request, body);

		int idInt = parseRequiredIntField(merged, "id", "id必填");
		long taskId = idInt;
		if (taskId <= 0) {
			throw new BadRequestException("id必填");
		}

		aliyunsmsTaskRevokeService.revokeTask(companyId, taskId);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/aliyunsms/task/revoke");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "群发任务撤销");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		try {
			operatorLogsWriteService.addLogs(logCtx);
		} catch (Exception ignored) {
		}

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	private static boolean isEmptySendAt(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof Boolean b) {
			return !b;
		}
		if (v instanceof Number n) {
			return n.doubleValue() == 0.0;
		}
		if (v instanceof String s) {
			String t = s.trim();
			return t.isEmpty() || "0".equals(t);
		}
		if (v instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (v instanceof Map<?, ?> m) {
			return m.isEmpty();
		}
		return false;
	}

	private static String sendAtRawToString(Object raw) {
		if (raw == null) {
			return "";
		}
		if (raw instanceof String s) {
			return s.trim();
		}
		return String.valueOf(raw);
	}

	private static long parseLongSecondsOrBadRequest(String prefix) {
		String t = prefix == null ? "" : prefix.trim();
		if (t.isEmpty()) {
			throw new BadRequestException("发送时间格式错误");
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new BadRequestException("发送时间格式错误");
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

	private static String clientIp(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		if (StringUtils.hasText(xff)) {
			return xff.split(",")[0].trim();
		}
		return request.getRemoteAddr() != null ? request.getRemoteAddr() : "";
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
		if (pm.containsKey("pagSize")) {
			return parsePositiveIntOrDefault(request.getParameter("pagSize"), 10);
		}
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

	private static int parsePageRawForTaskList(String p) {
		if (p == null) {
			return 1;
		}
		try {
			return Integer.parseInt(p.trim());
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private static void applyTaskNameToQuery(HttpServletRequest request, TaskListQuery q) {
		var pm = request.getParameterMap();
		if (!pm.containsKey("task_name")) {
			q.setTaskNameContainsActive(false);
			return;
		}
		String raw = request.getParameter("task_name");
		if (raw == null || !StringUtils.hasText(raw) || "0".equals(raw.trim())) {
			q.setTaskNameContainsActive(false);
			return;
		}
		q.setTaskNameContainsActive(true);
		q.setTaskNameContains(raw);
	}

	private static void applyTemplateNameToQuery(HttpServletRequest request, TaskListQuery q) {
		var pm = request.getParameterMap();
		if (!pm.containsKey("template_name")) {
			q.setTemplateNameContainsActive(false);
			return;
		}
		String raw = request.getParameter("template_name");
		if (raw == null || !StringUtils.hasText(raw) || "0".equals(raw.trim())) {
			q.setTemplateNameContainsActive(false);
			return;
		}
		q.setTemplateNameContainsActive(true);
		q.setTemplateNameContains(raw);
	}

	private static int parseRequiredIntField(Map<String, Object> merged, String key, String message) {
		if (!merged.containsKey(key)) {
			throw new BadRequestException(message);
		}
		Object v = merged.get(key);
		if (v == null) {
			throw new BadRequestException(message);
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty()) {
			throw new BadRequestException(message);
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException(message);
		}
	}

	private static int parseRequiredMinInt(
			Map<String, Object> merged, String key, int min, String emptyMessage, String belowMinMessage) {
		int v = parseRequiredIntField(merged, key, emptyMessage);
		if (v < min) {
			throw new BadRequestException(belowMinMessage);
		}
		return v;
	}
}
