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

package cn.shopex.ecshopx.reservation.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.merchant.port.ShopOperatorCompanyActivation;
import cn.shopex.ecshopx.merchant.port.ShopOperatorLogsWrite;
import cn.shopex.ecshopx.reservation.port.ReservationRoutePermissionPort;
import cn.shopex.ecshopx.reservation.service.WorkShiftTypeCreateService;
import cn.shopex.ecshopx.reservation.service.WorkShiftTypeDeleteService;
import cn.shopex.ecshopx.reservation.service.WorkShiftTypeListService;
import cn.shopex.ecshopx.reservation.service.WorkShiftTypeUpdateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.ShopLog;

@AdminAuth
@ShopLog
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422)
@RestController("reservationWorkShiftTypeAdminV1")
@RequestMapping("/api/v1")
public class WorkShiftTypeController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final ReservationRoutePermissionPort reservationRoutePermissionPort;
	private final ShopOperatorCompanyActivation shopOperatorCompanyActivation;
	private final WorkShiftTypeCreateService workShiftTypeCreateService;
	private final WorkShiftTypeListService workShiftTypeListService;
	private final WorkShiftTypeUpdateService workShiftTypeUpdateService;
	private final WorkShiftTypeDeleteService workShiftTypeDeleteService;
	private final ShopOperatorLogsWrite shopOperatorLogsWrite;
	private final ObjectMapper objectMapper;

	public WorkShiftTypeController(
			ReservationRoutePermissionPort reservationRoutePermissionPort,
			ShopOperatorCompanyActivation shopOperatorCompanyActivation,
			WorkShiftTypeCreateService workShiftTypeCreateService,
			WorkShiftTypeListService workShiftTypeListService,
			WorkShiftTypeUpdateService workShiftTypeUpdateService,
			WorkShiftTypeDeleteService workShiftTypeDeleteService,
			ShopOperatorLogsWrite shopOperatorLogsWrite,
			ObjectMapper objectMapper) {
		this.reservationRoutePermissionPort = reservationRoutePermissionPort;
		this.shopOperatorCompanyActivation = shopOperatorCompanyActivation;
		this.workShiftTypeCreateService = workShiftTypeCreateService;
		this.workShiftTypeListService = workShiftTypeListService;
		this.workShiftTypeUpdateService = workShiftTypeUpdateService;
		this.workShiftTypeDeleteService = workShiftTypeDeleteService;
		this.shopOperatorLogsWrite = shopOperatorLogsWrite;
		this.objectMapper = objectMapper;
	}

	@Activated(routeAlias = "shift.type.create")
	@PostMapping(value = "/shifttype", name = "添加排班类型")
	public ResponseEntity<ApiResult<Map<String, Object>>> createShiftType(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
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

		reservationRoutePermissionPort.assertShiftTypeCreateAllowed(user);
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		Map<String, Object> input = mergeInputLikeFlexibleResolver(request, body);
		validateCreateShiftTypeInput(input);

		String typeName = input.get("typeName") != null ? input.get("typeName").toString().trim() : "";
		String beginTime = input.get("beginTime") != null ? input.get("beginTime").toString().trim() : "";
		String endTime = input.get("endTime") != null ? input.get("endTime").toString().trim() : "";

		Map<String, Object> statusPayload =
				workShiftTypeCreateService.createShiftType(companyId, typeName, beginTime, endTime);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/shifttype");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(input));
		} catch (Exception e) {
			logCtx.put("params", input.toString());
		}
		logCtx.put("operator_name", "添加排班类型");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		try {
			shopOperatorLogsWrite.addLogs(logCtx);
		} catch (Exception ignored) {
			// 操作日志失败不影响主流程
		}

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", statusPayload)));
	}

	/** 校验顺序：typeName → typeId；多字段错误时用全角逗号拼接为一条消息。 */
	private static void validateUpdateShiftTypeInput(Map<String, Object> input) {
		List<String> parts = new ArrayList<>();
		if (isBlank(input.get("typeName"))) {
			parts.add("类型名称必填");
		}
		if (isBlank(input.get("typeId"))) {
			parts.add("类型ID必填");
		}
		if (!parts.isEmpty()) {
			throw new BadRequestException(String.join("\uFF0C", parts));
		}
	}

	/** 校验顺序：typeName → beginTime → endTime；多字段错误时用全角逗号拼接为一条消息。 */
	private static void validateCreateShiftTypeInput(Map<String, Object> input) {
		List<String> parts = new ArrayList<>();
		if (isBlank(input.get("typeName"))) {
			parts.add("类型名称必填");
		}
		if (isBlank(input.get("beginTime"))) {
			parts.add("开始时间必填");
		}
		if (isBlank(input.get("endTime"))) {
			parts.add("结束时间必填");
		}
		if (!parts.isEmpty()) {
			throw new BadRequestException(String.join("\uFF0C", parts));
		}
	}

	private Map<String, Object> mergeInputLikeFlexibleResolver(HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			LinkedHashMap<String, Object> merged =
					new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
			if (body != null) {
				merged.putAll(body);
			}
			return merged;
		}
		return body != null ? body : new LinkedHashMap<>();
	}

	private static boolean isBlank(Object v) {
		if (v == null) {
			return true;
		}
		return !StringUtils.hasText(v.toString().trim());
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}

	private static long parseShiftTypeId(Object raw) {
		try {
			if (raw instanceof Number n) {
				return n.longValue();
			}
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("类型ID格式错误");
		}
	}

	private static String clientIp(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		if (StringUtils.hasText(xff)) {
			return xff.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	@Activated(routeAlias = "shift.type.delete")
	@DeleteMapping(value = "/shifttype/{tyepId}", name = "删除排班类型")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteShiftType(
			HttpServletRequest request, @PathVariable("tyepId") String tyepId) {
		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
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

		reservationRoutePermissionPort.assertShiftTypeDeleteAllowed(user);
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		boolean result = workShiftTypeDeleteService.deleteShiftType(companyId, tyepId);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/shifttype/" + tyepId);
		logCtx.put("ip", clientIp(request));
		try {
			Map<String, Object> logParams = new LinkedHashMap<>();
			logParams.put("tyepId", tyepId);
			logCtx.put("params", objectMapper.writeValueAsString(logParams));
		} catch (Exception e) {
			logCtx.put("params", "{\"tyepId\":\"" + tyepId + "\"}");
		}
		logCtx.put("operator_name", "删除排班类型");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		try {
			shopOperatorLogsWrite.addLogs(logCtx);
		} catch (Exception ignored) {
			// 操作日志失败不影响主流程
		}

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", result)));
	}

	@Activated(routeAlias = "shift.type.update")
	@PatchMapping(value = "/shifttype", name = "编辑排班类型")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateShiftType(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
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

		reservationRoutePermissionPort.assertShiftTypeUpdateAllowed(user);
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		Map<String, Object> input = mergeInputLikeFlexibleResolver(request, body);
		validateUpdateShiftTypeInput(input);

		String typeName = input.get("typeName") != null ? input.get("typeName").toString().trim() : "";
		long typeId = parseShiftTypeId(input.get("typeId"));

		Map<String, Object> statusPayload =
				workShiftTypeUpdateService.updateShiftTypeName(companyId, typeId, typeName);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/shifttype");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(input));
		} catch (Exception e) {
			logCtx.put("params", input.toString());
		}
		logCtx.put("operator_name", "编辑排班类型");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		try {
			shopOperatorLogsWrite.addLogs(logCtx);
		} catch (Exception ignored) {
			// 操作日志失败不影响主流程
		}

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", statusPayload)));
	}

	@Activated(routeAlias = "shift.type.getlist")
	@GetMapping(value = "/shifttype", name = "排班类型列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getListShiftType(HttpServletRequest request) {
		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
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

		reservationRoutePermissionPort.assertShiftTypeGetListAllowed(user);
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		Map<String, Object> data = workShiftTypeListService.listValidShiftTypes(companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}
}
