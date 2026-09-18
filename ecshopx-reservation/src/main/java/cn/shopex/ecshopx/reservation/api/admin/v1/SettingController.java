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
import cn.shopex.ecshopx.common.util.ValuePresence;
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
import cn.shopex.ecshopx.reservation.service.ReservationSettingGetService;
import cn.shopex.ecshopx.reservation.service.ReservationSettingSaveService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
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
@RestController("reservationSettingAdminV1")
@RequestMapping("/api/v1")
public class SettingController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private static final Set<String> LIMIT_TYPES = Set.of("not_open", "limit_days", "limit_nums");

	private final ReservationRoutePermissionPort reservationRoutePermissionPort;

	private final ShopOperatorCompanyActivation shopOperatorCompanyActivation;

	private final ReservationSettingSaveService reservationSettingSaveService;

	private final ReservationSettingGetService reservationSettingGetService;

	private final ShopOperatorLogsWrite shopOperatorLogsWrite;

	private final ObjectMapper objectMapper;

	public SettingController(
			ReservationRoutePermissionPort reservationRoutePermissionPort,
			ShopOperatorCompanyActivation shopOperatorCompanyActivation,
			ReservationSettingSaveService reservationSettingSaveService,
			ReservationSettingGetService reservationSettingGetService,
			ShopOperatorLogsWrite shopOperatorLogsWrite,
			ObjectMapper objectMapper) {
		this.reservationRoutePermissionPort = reservationRoutePermissionPort;
		this.shopOperatorCompanyActivation = shopOperatorCompanyActivation;
		this.reservationSettingSaveService = reservationSettingSaveService;
		this.reservationSettingGetService = reservationSettingGetService;
		this.shopOperatorLogsWrite = shopOperatorLogsWrite;
		this.objectMapper = objectMapper;
	}

	@Activated(routeAlias = "reservation.setting.save")
	@PostMapping(value = "/reservation/setting", name = "保存预约的详细配置")
	public ResponseEntity<ApiResult<Map<String, Object>>> setSetting(
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

		reservationRoutePermissionPort.assertReservationSettingSaveAllowed(user);
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		Map<String, Object> input = mergeInputLikeFlexibleResolver(request, body);
		validateSetSettingStep1(input);
		int cancelMinuteParsed = parseCancelMinuteStrict(input.get("cancelMinute"));
		int minLimitHour = parseIntLoose(input.get("minLimitHour"));
		if (minLimitHour >= cancelMinuteParsed) {
			throw new ResourceException("取消预约提前分钟数必须大于最小可预约周期值");
		}
		validateLimitWhenNeeded(input);

		Map<String, Object> paramsData = buildParamsData(companyId, input, cancelMinuteParsed);

		Map<String, Object> status = reservationSettingSaveService.saveSetting(companyId, paramsData);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/reservation/setting");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(paramsData));
		} catch (Exception e) {
			logCtx.put("params", paramsData.toString());
		}
		logCtx.put("operator_name", "保存预约的详细配置");
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

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", status)));
	}

	@Activated(routeAlias = "reservation.setting.get")
	@GetMapping(value = "/reservation/setting", name = "预约配置详细信息")
	public ResponseEntity<ApiResult<Object>> getSetting(HttpServletRequest request) {
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

		reservationRoutePermissionPort.assertReservationSettingGetAllowed(user);
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		Object data = reservationSettingGetService.buildResponse(companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private void validateSetSettingStep1(Map<String, Object> input) {
		StringBuilder err = new StringBuilder();
		if (isBlank(input.get("reservationMode"))) {
			err.append("预约模式必填，");
		}
		if (isBlank(input.get("condition"))) {
			err.append("预约条件必填，");
		}
		if (isBlank(input.get("interval"))) {
			err.append("预约时间间隔必填，");
		}
		appendMaxLimitDayErrors(input.get("maxLimitDay"), err);
		appendSmsDelayErrors(input.get("sms_delay"), err);
		appendMinLimitHourErrors(input.get("minLimitHour"), err);
		if (isBlank(input.get("resourceName"))) {
			err.append("资源位名称必填，");
		}
		if (isBlank(input.get("cancelMinute"))) {
			err.append("取消预约最少提前n分钟，");
		}
		if (input.containsKey("limitType") && input.get("limitType") != null) {
			String lt = input.get("limitType").toString().trim();
			if (!lt.isEmpty() && !LIMIT_TYPES.contains(lt)) {
				err.append("预约限制必填，");
			}
		}
		if (!err.isEmpty()) {
			throw new ResourceException(err.toString());
		}
	}

	private static void appendMaxLimitDayErrors(Object raw, StringBuilder err) {
		if (isBlank(raw)) {
			err.append("最多可以提前的天数必填，");
			return;
		}
		try {
			int v = parseIntLoose(raw);
			if (v < 1) {
				err.append("最小为1，");
			}
		} catch (NumberFormatException e) {
			err.append("必是整数，");
		}
	}

	private static void appendSmsDelayErrors(Object raw, StringBuilder err) {
		if (isBlank(raw)) {
			err.append("预约短信通知提醒必须为整数，");
			return;
		}
		try {
			int v = parseIntLoose(raw);
			if (v < 1) {
				err.append("预约短信通知提醒必须为整数，");
			}
		} catch (NumberFormatException e) {
			err.append("预约短信通知提醒必须为整数，");
		}
	}

	private static void appendMinLimitHourErrors(Object raw, StringBuilder err) {
		if (isBlank(raw)) {
			err.append("最少可以提前的分钟数，");
			return;
		}
		try {
			int v = parseIntLoose(raw);
			if (v < 30) {
				err.append("最小为30，");
			}
		} catch (NumberFormatException e) {
			err.append("必为整数，");
		}
	}

	private static int parseCancelMinuteStrict(Object raw) {
		String s = String.valueOf(raw).trim();
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException("取消预约最少提前分钟数必须为整数");
		}
	}

	private static void validateLimitWhenNeeded(Map<String, Object> input) {
		Object ltObj = input.get("limitType");
		if (ltObj == null) {
			return;
		}
		String lt = ltObj.toString().trim();
		if (!"limit_days".equals(lt) && !"limit_nums".equals(lt)) {
			return;
		}
		Object lim = input.get("limit");
		if (!isNumeric(lim)) {
			throw new ResourceException("预约限制数据必须为整数");
		}
	}

	private Map<String, Object> buildParamsData(
			long companyId, Map<String, Object> input, int cancelMinuteParsed) {
		Map<String, Object> paramsData = new LinkedHashMap<>();
		paramsData.put("companyId", companyId);
		paramsData.put("reservationMode", intOrResource(input.get("reservationMode"), "预约模式必填，"));
		paramsData.put("condition", intOrResource(input.get("condition"), "预约条件必填，"));
		paramsData.put("interval", intOrResource(input.get("interval"), "预约时间间隔必填，"));
		paramsData.put("maxLimitDay", intOrResource(input.get("maxLimitDay"), "最多可以提前的天数必填，"));
		paramsData.put("minLimitHour", intOrResource(input.get("minLimitHour"), "最少可以提前的分钟数，"));
		paramsData.put("cancelMinute", cancelMinuteParsed);
		paramsData.put("resourceName", Objects.toString(input.get("resourceName"), "").trim());
		paramsData.put("sms_delay", input.get("sms_delay"));

		if (input.containsKey("limitType") && input.get("limitType") != null) {
			String lt = input.get("limitType").toString().trim();
			if (!lt.isEmpty() && !"not_open".equals(lt) && ValuePresence.hasEffectiveValue(input.get("limit"))) {
				Map<String, Object> lim = new LinkedHashMap<>();
				lim.put("limit_type", lt);
				int limitVal = (int) parseNumericToLong(input.get("limit"));
				lim.put(lt, limitVal);
				paramsData.put("reservationNumLimit", lim);
			} else if ("not_open".equals(lt)) {
				paramsData.put("reservationNumLimit", new LinkedHashMap<String, Object>());
			}
		}
		return paramsData;
	}

	private static long parseNumericToLong(Object lim) {
		String s = Objects.toString(lim, "").trim();
		try {
			BigDecimal bd = new BigDecimal(s);
			return bd.longValue();
		} catch (NumberFormatException e) {
			throw new ResourceException("预约限制数据必须为整数");
		}
	}

	private static boolean isNumeric(Object v) {
		if (v == null) {
			return false;
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			return false;
		}
		try {
			new BigDecimal(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private Map<String, Object> mergeInputLikeFlexibleResolver(HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			LinkedHashMap<String, Object> input =
					new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
			if (body != null) {
				input.putAll(body);
			}
			return input;
		}
		return body != null ? body : new LinkedHashMap<>();
	}

	private static boolean isBlank(Object v) {
		if (v == null) {
			return true;
		}
		return !StringUtils.hasText(v.toString().trim());
	}

	private static int parseIntLoose(Object o) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(o.toString().trim());
	}

	private static int intOrResource(Object o, String msg) {
		try {
			return parseIntLoose(o);
		} catch (NumberFormatException e) {
			throw new ResourceException(msg);
		}
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
}
