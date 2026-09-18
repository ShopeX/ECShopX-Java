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

package cn.shopex.ecshopx.point.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.point.service.PointMemberListService;
import cn.shopex.ecshopx.point.service.PointMemberManualAdjustmentService;
import cn.shopex.ecshopx.point.service.PointMemberPointCountReadService;
import cn.shopex.ecshopx.point.service.export.PointMemberLogExportOrchestratorService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
@RestController("pointMemberAdminV1")
@RequestMapping("/api/v1")
public class PointMemberController {

	private final PointMemberManualAdjustmentService pointMemberManualAdjustmentService;
	private final PointMemberPointCountReadService pointMemberPointCountReadService;
	private final PointMemberListService pointMemberListService;
	private final PointMemberLogExportOrchestratorService pointMemberLogExportOrchestratorService;

	public PointMemberController(
			PointMemberManualAdjustmentService pointMemberManualAdjustmentService,
			PointMemberPointCountReadService pointMemberPointCountReadService,
			PointMemberListService pointMemberListService,
			PointMemberLogExportOrchestratorService pointMemberLogExportOrchestratorService) {
		this.pointMemberManualAdjustmentService = pointMemberManualAdjustmentService;
		this.pointMemberPointCountReadService = pointMemberPointCountReadService;
		this.pointMemberListService = pointMemberListService;
		this.pointMemberLogExportOrchestratorService = pointMemberLogExportOrchestratorService;
	}

	@Activated(routeAlias = "point.member.list")
	@GetMapping(value = "/point/member", name = "用户积分列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> lists(
			HttpServletRequest request,
			@RequestParam(value = "page", defaultValue = "1") int page,
			@RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
			@RequestParam(value = "user_id", required = false, defaultValue = "0") long userIdParam,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "username", required = false) String username,
			@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "date_begin", required = false) Long dateBegin,
			@RequestParam(value = "date_end", required = false) Long dateEnd) {
		long companyId = parseCompanyIdFromJwt(request);
		Map<String, Object> inner = pointMemberListService.lists(
				companyId, page, pageSize, userIdParam, mobile, username, name, dateBegin, dateEnd);
		return ResponseEntity.ok(ApiResult.ok(inner));
	}

	@Activated(routeAlias = "point.member.list.export")
	@GetMapping(value = "/point/member/export", name = "导出用户积分列表")
	public ResponseEntity<Map<String, Object>> exportData(
			HttpServletRequest request,
			@RequestParam(value = "page", defaultValue = "1") int page,
			@RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
			@RequestParam(value = "user_id", required = false, defaultValue = "0") long userIdParam,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "username", required = false) String username,
			@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "date_begin", required = false) Long dateBegin,
			@RequestParam(value = "date_end", required = false) Long dateEnd) {
		long companyId = parseCompanyIdFromJwt(request);
		boolean shouldQueue = resolveShouldQueue(request);
		@SuppressWarnings("unchecked")
		Map<String, Object> ud =
				(Map<String, Object>) request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		long operatorId = 0L;
		if (ud != null) {
			Object oid = ud.get("operator_id");
			if (oid != null) {
				try {
					operatorId = parseLongLoose(oid);
				} catch (NumberFormatException ignored) {
					operatorId = 0L;
				}
			}
		}
		String operatorType = "";
		if (ud != null && ud.get("operator_type") != null) {
			operatorType = String.valueOf(ud.get("operator_type"));
		}
		pointMemberLogExportOrchestratorService.export(
				companyId,
				operatorId,
				operatorType,
				page,
				pageSize,
				userIdParam,
				mobile,
				username,
				name,
				dateBegin,
				dateEnd,
				shouldQueue);
		return ResponseEntity.ok(Map.of("data", Map.of("status", Boolean.TRUE)));
	}

	private static boolean resolveShouldQueue(HttpServletRequest request) {
		String[] values = request.getParameterMap().get("should_queue");
		if (values == null || values.length == 0) {
			return true;
		}
		String raw = values[0] != null ? values[0].trim() : "";
		if (raw.isEmpty()) {
			return false;
		}
		String lower = raw.toLowerCase(Locale.ROOT);
		return "true".equals(lower) || "1".equals(lower) || "yes".equals(lower);
	}

	@Activated(routeAlias = "point.adjustment")
	@PostMapping(value = "/point/adjustment", name = "积分调整")
	public ResponseEntity<ApiResult<Map<String, Object>>> adjustment(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> data = pointMemberManualAdjustmentService.adjust(request, merged);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "member.pointcount.index")
	@GetMapping(value = "/member/pointcount/index", name = "获取积分总览页数据")
	public ResponseEntity<Map<String, Object>> getPointCountIndex(HttpServletRequest request) {
		long companyId = parseCompanyIdFromJwt(request);
		Map<String, Object> inner = pointMemberPointCountReadService.getMemberPointTotal(companyId);
		Map<String, Object> envelope = new LinkedHashMap<>();
		envelope.put("data", inner);
		return ResponseEntity.ok(envelope);
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<?> handleBadRequest(BadRequestException ex) {
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

	private Map<String, Object> mergeInputLikeFlexibleResolver(HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			LinkedHashMap<String, Object> input = new LinkedHashMap<>(parameterMapToMapLikeResolver(request));
			if (body != null) {
				input.putAll(body);
			}
			return input;
		}
		return body != null ? body : new LinkedHashMap<>();
	}

	private static Map<String, Object> parameterMapToMapLikeResolver(HttpServletRequest request) {
		Map<String, Object> m = new LinkedHashMap<>();
		request.getParameterMap().forEach((k, v) -> {
			if (v != null && v.length > 0 && StringUtils.hasText(v[0])) {
				m.put(k, v[0]);
			}
		});
		return m;
	}

	private static long parseCompanyIdFromJwt(HttpServletRequest request) {
		Object rawUd = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawUd instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		try {
			return parseLongLoose(cid);
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
	}

	private static long parseLongLoose(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}
}
