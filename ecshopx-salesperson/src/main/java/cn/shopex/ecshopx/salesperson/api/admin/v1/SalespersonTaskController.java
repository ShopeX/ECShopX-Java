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

package cn.shopex.ecshopx.salesperson.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.salesperson.service.SalespersonTaskCreateService;
import cn.shopex.ecshopx.salesperson.service.SalespersonTaskInfoService;
import cn.shopex.ecshopx.salesperson.service.SalespersonTaskListService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
		notFound = true
)
@AdminAuth
@ShopLog
@RestController("salespersonAdminV1SalespersonTask")
@RequestMapping("/api/v1/salesperson/task")
public class SalespersonTaskController {

	private final SalespersonTaskCreateService salespersonTaskCreateService;
	private final SalespersonTaskListService salespersonTaskListService;
	private final SalespersonTaskInfoService salespersonTaskInfoService;

	public SalespersonTaskController(SalespersonTaskCreateService salespersonTaskCreateService,
			SalespersonTaskListService salespersonTaskListService,
			SalespersonTaskInfoService salespersonTaskInfoService) {
		this.salespersonTaskCreateService = salespersonTaskCreateService;
		this.salespersonTaskListService = salespersonTaskListService;
		this.salespersonTaskInfoService = salespersonTaskInfoService;
	}

	@Activated(routeAlias = "salesperson.task.list")
	@GetMapping(name = "获取导购任务列表")
	public ResponseEntity<Map<String, Object>> lists(HttpServletRequest request,
			@RequestParam(name = "status", required = false) String status,
			@RequestParam(name = "page", defaultValue = "1") int page,
			@RequestParam(name = "page_size", defaultValue = "10") int pageSize) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString());

		Map<String, Object> payload = salespersonTaskListService.lists(companyId, status, page, pageSize);
		return ResponseEntity.ok(Map.of("data", payload));
	}

	@Activated(routeAlias = "salesperson.task.statistics")
	@GetMapping(value = "/statistics", name = "获取导购任务统计列表")
	public ResponseEntity<Map<String, Object>> statistics(HttpServletRequest request,
			@RequestParam(name = "task_id", required = false) String taskIdParam,
			@RequestParam(name = "page", defaultValue = "1") int page,
			@RequestParam(name = "page_size", defaultValue = "10") int pageSize) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString());

		Long taskId = parseStatisticsTaskId(taskIdParam);

		Map<String, Object> payload = salespersonTaskListService.statistics(companyId, taskId, page, pageSize);
		return ResponseEntity.ok(Map.of("data", payload));
	}

	private static Long parseStatisticsTaskId(String taskIdParam) {
		if (taskIdParam == null) {
			return null;
		}
		String trimmed = taskIdParam.trim();
		if (!StringUtils.hasText(trimmed)) {
			return null;
		}
		try {
			return Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@Activated(routeAlias = "salesperson.task.info")
	@GetMapping(value = "/{taskId}", name = "获取导购任务详情")
	public ResponseEntity<Map<String, Object>> info(HttpServletRequest request, @PathVariable("taskId") String taskId) {
		long parsedTaskId;
		try {
			parsedTaskId = Long.parseLong(taskId.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("无效的任务ID", 422);
		}
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString());

		Map<String, Object> payload = salespersonTaskInfoService.info(companyId, parsedTaskId);
		return ResponseEntity.ok(Map.of("data", payload));
	}

	@Activated(routeAlias = "salesperson.task.create")
	@PostMapping(name = "创建导购任务")
	public ResponseEntity<Map<String, Object>> create(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString());

		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		salespersonTaskCreateService.create(companyId, merged);
		return ResponseEntity.ok(Map.of("data", Map.of("status", true)));
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<?> handleBadRequest(BadRequestException ex) {
		int statusCode = ex.getEmbeddedStatusCode() != null ? ex.getEmbeddedStatusCode() : 422;
		return dingo422(ex.getMessage(), ex.getFieldErrors(), statusCode);
	}

	@ExceptionHandler(ResourceException.class)
	public ResponseEntity<?> handleResource(ResourceException ex) {
		int statusCode = ex.getEmbeddedStatusCode() != null ? ex.getEmbeddedStatusCode() : 422;
		return dingo422(ex.getMessage(), ex.getFieldErrors(), statusCode);
	}

	private static ResponseEntity<?> dingo422(String message, Map<String, List<String>> errors, int statusCode) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", message);
		data.put("status_code", statusCode);
		if (errors != null && !errors.isEmpty()) {
			data.put("errors", errors);
		}
		return ResponseEntity.ok(Map.of("data", data));
	}

	@Activated(routeAlias = "salesperson.task.update")
	@PutMapping(value = "/{taskId}", name = "修改导购任务")
	public ResponseEntity<Map<String, Object>> update(HttpServletRequest request,
			@PathVariable("taskId") String taskId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long parsedTaskId;
		try {
			parsedTaskId = Long.parseLong(taskId.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("无效的任务ID", 422);
		}
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString());

		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		salespersonTaskCreateService.update(companyId, parsedTaskId, merged);
		return ResponseEntity.ok(Map.of("data", Map.of("status", true)));
	}

	@Activated(routeAlias = "salesperson.task.cancel")
	@DeleteMapping(value = "/{taskId}", name = "取消导购任务")
	public ResponseEntity<Map<String, Object>> cancel(HttpServletRequest request,
			@PathVariable("taskId") String taskId) {
		long parsedTaskId;
		try {
			parsedTaskId = Long.parseLong(taskId.trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("未查询到更新数据");
		}
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString());

		salespersonTaskCreateService.cancel(companyId, parsedTaskId);
		return ResponseEntity.ok(Map.of("data", Map.of("status", true)));
	}

	private Map<String, Object> mergeInputLikeFlexibleResolver(HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			LinkedHashMap<String, Object> input = new LinkedHashMap<>(parameterMapToMap(request));
			if (body != null) {
				input.putAll(body);
			}
			return input;
		}
		return body != null ? body : new LinkedHashMap<>();
	}

	private static Map<String, Object> parameterMapToMap(HttpServletRequest request) {
		Map<String, Object> m = new LinkedHashMap<>();
		request.getParameterMap().forEach((k, v) -> {
			if (v != null && v.length > 0 && StringUtils.hasText(v[0])) {
				m.put(k, v[0]);
			}
		});
		return m;
	}
}
