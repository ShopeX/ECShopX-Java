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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.merchant.port.ShopOperatorCompanyActivation;
import cn.shopex.ecshopx.merchant.port.ShopOperatorLogsWrite;
import cn.shopex.ecshopx.reservation.port.ReservationRoutePermissionPort;
import cn.shopex.ecshopx.reservation.service.DefaultWorkShiftDeleteService;
import cn.shopex.ecshopx.reservation.service.DefaultWorkShiftUpsertService;
import cn.shopex.ecshopx.reservation.service.WorkShiftListService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.ShopLog;

@AdminAuth
@ShopLog
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422)
@RestController("reservationDefaultWorkShiftAdminV1")
@RequestMapping("/api/v1")
public class DefaultWorkShiftController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final ReservationRoutePermissionPort reservationRoutePermissionPort;
	private final ShopOperatorCompanyActivation shopOperatorCompanyActivation;
	private final DefaultWorkShiftUpsertService defaultWorkShiftUpsertService;
	private final DefaultWorkShiftDeleteService defaultWorkShiftDeleteService;
	private final WorkShiftListService workShiftListService;
	private final ShopOperatorLogsWrite shopOperatorLogsWrite;
	private final ObjectMapper objectMapper;

	public DefaultWorkShiftController(
			ReservationRoutePermissionPort reservationRoutePermissionPort,
			ShopOperatorCompanyActivation shopOperatorCompanyActivation,
			DefaultWorkShiftUpsertService defaultWorkShiftUpsertService,
			DefaultWorkShiftDeleteService defaultWorkShiftDeleteService,
			WorkShiftListService workShiftListService,
			ShopOperatorLogsWrite shopOperatorLogsWrite,
			ObjectMapper objectMapper) {
		this.reservationRoutePermissionPort = reservationRoutePermissionPort;
		this.shopOperatorCompanyActivation = shopOperatorCompanyActivation;
		this.defaultWorkShiftUpsertService = defaultWorkShiftUpsertService;
		this.defaultWorkShiftDeleteService = defaultWorkShiftDeleteService;
		this.workShiftListService = workShiftListService;
		this.shopOperatorLogsWrite = shopOperatorLogsWrite;
		this.objectMapper = objectMapper;
	}

	@Activated(routeAlias = "shift.default.create")
	@PostMapping(value = "/workshift/default", name = "新增门店默认排班")
	public ResponseEntity<ApiResult<Map<String, Object>>> createDefaultWorkShift(
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

		reservationRoutePermissionPort.assertWorkShiftCreateAllowed(user);
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		Map<String, Object> input = mergeInputLikeFlexibleResolver(request, body);
		validateCreateDefaultWorkShiftInput(input);

		Object shopRaw =
				input.get("shop_id") != null ? input.get("shop_id") : input.get("shopId");
		long shopId = parseShopId(shopRaw);

		@SuppressWarnings("unchecked")
		List<Object> defaultDataList = (List<Object>) input.get("defaultData");
		Map<String, Map<String, String>> workShiftData = new LinkedHashMap<>();
		for (Object o : defaultDataList) {
			@SuppressWarnings("unchecked")
			Map<String, Object> item = (Map<String, Object>) o;
			String nameStr = item.get("name").toString().trim();
			String valueStr = validateAndStringifyDefaultDataValue(item.get("value"));
			workShiftData.put(nameStr, Map.of("typeId", valueStr));
		}

		Map<String, Object> statusPayload =
				defaultWorkShiftUpsertService.upsertDefaultWorkShift(
						companyId, shopId, workShiftData);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/workshift/default");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(input));
		} catch (Exception e) {
			logCtx.put("params", input.toString());
		}
		logCtx.put("operator_name", "新增门店默认排班");
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

	private static void validateCreateDefaultWorkShiftInput(Map<String, Object> input) {
		Object shopRaw = input.get("shop_id") != null ? input.get("shop_id") : input.get("shopId");
		if (isBlank(shopRaw)) {
			throw new BadRequestException("门店id必填");
		}
		if (!input.containsKey("defaultData") || input.get("defaultData") == null) {
			throw new BadRequestException("排班数据不能为空");
		}

		Object dd = input.get("defaultData");
		if (!(dd instanceof List<?> list)) {
			throw new BadRequestException("排班数据格式无效");
		}
		if (list.isEmpty()) {
			throw new BadRequestException("排班数据不能为空");
		}
		for (Object o : list) {
			if (!(o instanceof Map<?, ?> item)) {
				throw new BadRequestException("排班数据项格式无效");
			}
			if (!item.containsKey("name") || isBlank(item.get("name"))) {
				throw new BadRequestException("排班数据项 name 不能为空");
			}
			if (!item.containsKey("value")) {
				throw new BadRequestException("排班数据项 value 不能为空");
			}
			validateAndStringifyDefaultDataValue(item.get("value"));
		}
	}

	private static String validateAndStringifyDefaultDataValue(Object value) {
		if (value == null) {
			throw new BadRequestException("排班数据项 value 不能为空");
		}
		if (value instanceof Map<?, ?> || value instanceof List<?>) {
			throw new BadRequestException("排班数据项 value 格式无效");
		}
		String s = value.toString().trim();
		if (s.isEmpty()) {
			throw new BadRequestException("排班数据项 value 不能为空");
		}
		if (!"-1".equals(s)) {
			try {
				Long.parseLong(s);
			} catch (NumberFormatException ex) {
				throw new BadRequestException("排班数据项 value 格式无效");
			}
		}
		return s;
	}

	private static long parseShopId(Object raw) {
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException ex) {
			throw new BadRequestException("shop_id 格式无效");
		}
	}

	private Map<String, Object> mergeInputLikeFlexibleResolver(
			HttpServletRequest request, Map<String, Object> body) {
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

	private static String clientIp(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		if (StringUtils.hasText(xff)) {
			return xff.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	@Activated(routeAlias = "shift.default.delete")
	@DeleteMapping(value = "/workshift/default", name = "删除门店默认排班")
	public ResponseEntity<Void> deleteDefaultWorkShift(
			HttpServletRequest request,
			@RequestParam(value = "shop_id", required = false) String shopIdUnderscore,
			@RequestParam(value = "shopId", required = false) String shopIdCamel) {
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

		reservationRoutePermissionPort.assertShiftDefaultDeleteAllowed(user);
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		String shopIdStr = firstNonBlank(shopIdUnderscore, shopIdCamel);
		if (!StringUtils.hasText(shopIdStr)) {
			shopIdStr = request.getParameter("shop_id");
		}
		if (!StringUtils.hasText(shopIdStr)) {
			shopIdStr = request.getParameter("shopId");
		}
		String trimmed = shopIdStr == null ? "" : shopIdStr.trim();
		if (!StringUtils.hasText(trimmed)) {
			throw new BadRequestException("门店id必填");
		}
		long shopId = parseShopId(trimmed);

		defaultWorkShiftDeleteService.deleteDefaultWorkShift(companyId, shopId);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/workshift/default");
		logCtx.put("ip", clientIp(request));
		Map<String, Object> params = Map.of("shop_id", shopId);
		try {
			logCtx.put("params", objectMapper.writeValueAsString(params));
		} catch (Exception e) {
			logCtx.put("params", String.valueOf(shopId));
		}
		logCtx.put("operator_name", "删除门店默认排班");
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

		return ResponseEntity.ok().build();
	}

	@Activated(routeAlias = "shift.default.getlist")
	@GetMapping(value = "/workshift/default", name = "获取门店默认排班")
	public ResponseEntity<ApiResult<Object>> getDefaultWorkShift(
			HttpServletRequest request,
			@RequestParam(value = "shop_id", required = false) String shopIdUnderscore,
			@RequestParam(value = "shopId", required = false) String shopIdCamel) {
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

		reservationRoutePermissionPort.assertShiftDefaultGetListAllowed(user);
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		String shopIdStr = firstNonBlank(shopIdUnderscore, shopIdCamel);
		if (!StringUtils.hasText(shopIdStr)) {
			shopIdStr = request.getParameter("shop_id");
		}
		if (!StringUtils.hasText(shopIdStr)) {
			shopIdStr = request.getParameter("shopId");
		}
		String trimmed = shopIdStr == null ? "" : shopIdStr.trim();
		if (!StringUtils.hasText(trimmed)) {
			throw new ResourceException("门店id，");
		}
		long shopId;
		try {
			shopId = Long.parseLong(trimmed);
		} catch (NumberFormatException ex) {
			throw new ResourceException("门店id格式无效");
		}

		Map<String, Object> root = workShiftListService.getDefaultWorkShiftRootForApi(companyId, shopId);
		if (root == null || root.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		return ResponseEntity.ok(ApiResult.ok(root));
	}

	private static String firstNonBlank(String a, String b) {
		if (StringUtils.hasText(a)) {
			return a;
		}
		if (StringUtils.hasText(b)) {
			return b;
		}
		return "";
	}
}
