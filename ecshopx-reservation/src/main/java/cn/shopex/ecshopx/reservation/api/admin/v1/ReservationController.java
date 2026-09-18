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
import cn.shopex.ecshopx.reservation.domain.ReservationEveryDayTimePeriodPayload;
import cn.shopex.ecshopx.reservation.service.ReservationCreateService;
import cn.shopex.ecshopx.reservation.service.ReservationEveryDayTimePeriodParamValidator;
import cn.shopex.ecshopx.reservation.service.ReservationEveryDayTimePeriodService;
import cn.shopex.ecshopx.reservation.service.ReservationListParamValidator;
import cn.shopex.ecshopx.reservation.service.ReservationListQueryService;
import cn.shopex.ecshopx.reservation.service.ReservationListValidatedParams;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
@RestController("reservationAdminV1")
@RequestMapping("/api/v1")
public class ReservationController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final ReservationRoutePermissionPort reservationRoutePermissionPort;

	private final ShopOperatorCompanyActivation shopOperatorCompanyActivation;

	private final ReservationCreateService reservationCreateService;

	private final ReservationListParamValidator reservationListParamValidator;

	private final ReservationListQueryService reservationListQueryService;

	private final ShopOperatorLogsWrite shopOperatorLogsWrite;

	private final ObjectMapper objectMapper;

	private final ReservationEveryDayTimePeriodParamValidator reservationEveryDayTimePeriodParamValidator;

	private final ReservationEveryDayTimePeriodService reservationEveryDayTimePeriodService;

	public ReservationController(
			ReservationRoutePermissionPort reservationRoutePermissionPort,
			ShopOperatorCompanyActivation shopOperatorCompanyActivation,
			ReservationCreateService reservationCreateService,
			ReservationListParamValidator reservationListParamValidator,
			ReservationListQueryService reservationListQueryService,
			ShopOperatorLogsWrite shopOperatorLogsWrite,
			ObjectMapper objectMapper,
			ReservationEveryDayTimePeriodParamValidator reservationEveryDayTimePeriodParamValidator,
			ReservationEveryDayTimePeriodService reservationEveryDayTimePeriodService) {
		this.reservationRoutePermissionPort = reservationRoutePermissionPort;
		this.shopOperatorCompanyActivation = shopOperatorCompanyActivation;
		this.reservationCreateService = reservationCreateService;
		this.reservationListParamValidator = reservationListParamValidator;
		this.reservationListQueryService = reservationListQueryService;
		this.shopOperatorLogsWrite = shopOperatorLogsWrite;
		this.objectMapper = objectMapper;
		this.reservationEveryDayTimePeriodParamValidator = reservationEveryDayTimePeriodParamValidator;
		this.reservationEveryDayTimePeriodService = reservationEveryDayTimePeriodService;
	}

	@Activated(routeAlias = "reservation.create")
	@PostMapping(value = "/reservation", name = "商家主动占用资源位")
	public ResponseEntity<ApiResult<Map<String, Object>>> create(
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

		reservationRoutePermissionPort.assertReservationCreateAllowed(user);
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		Map<String, Object> input = mergeInputLikeFlexibleResolver(request, body);
		validateCreateInput(input);

		String dateDayRaw = Objects.toString(input.get("dateDay"), "");
		probeDateDayFormatsLeniently(dateDayRaw);
		String normalizedYmd = normalizeDateDayYmd(dateDayRaw);

		Map<String, Object> paramsData = buildParamsData(companyId, input, normalizedYmd);
		removeBlankOptionalParams(paramsData);

		reservationCreateService.createReservation(paramsData);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/reservation");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(paramsData));
		} catch (Exception e) {
			logCtx.put("params", paramsData.toString());
		}
		logCtx.put("operator_name", "商家主动占用资源位");
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
			// 操作日志失败不影响预约主流程
		}

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	private static void probeDateDayFormatsLeniently(String dateDayRaw) {
		if (!StringUtils.hasText(dateDayRaw)) {
			return;
		}
		try {
			LocalDate.parse(dateDayRaw.trim());
		} catch (Exception e) {
			try {
				Instant.parse(dateDayRaw.trim());
			} catch (Exception e2) {
				// 无法识别的日期格式不在这里阻断，交由后续规范化逻辑处理
			}
		}
	}

	private static String normalizeDateDayYmd(String dateDayRaw) {
		if (!StringUtils.hasText(dateDayRaw)) {
			throw new ResourceException("日期必填");
		}
		String s = dateDayRaw.trim();
		try {
			return LocalDate.parse(s).toString();
		} catch (Exception ignored) {
			// fall through
		}
		try {
			long sec = Long.parseLong(s);
			return Instant.ofEpochSecond(sec).atZone(ZoneId.systemDefault()).toLocalDate().toString();
		} catch (Exception e) {
			throw new ResourceException("日期必填");
		}
	}

	private static void validateCreateInput(Map<String, Object> input) {
		StringBuilder err = new StringBuilder();
		if (isBlank(input.get("shopId"))) {
			err.append("门店必选，");
		}
		if (isBlank(input.get("resourceLevelId"))) {
			err.append("资源位数据必填，");
		}
		if (isBlank(input.get("dateDay"))) {
			err.append("日期必填，");
		}
		if (isBlank(input.get("beginTime"))) {
			err.append("时段必选，");
		}
		if (isBlank(input.get("instead"))) {
			err.append("操作类型必填，");
		}
		String instead = Objects.toString(input.get("instead"), "");
		if ("user".equals(instead)) {
			if (isBlank(input.get("mobile"))) {
				err.append("代客预约时，手机号必填，");
			}
			if (isBlank(input.get("rightsId"))) {
				err.append("代客预约时,预约项目必填，");
			}
		}
		if (!err.isEmpty()) {
			throw new ResourceException(err.toString());
		}
	}

	private Map<String, Object> buildParamsData(long companyId, Map<String, Object> input, String ymd) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", companyId);
		m.put("shop_id", toLong(input.get("shopId")));
		m.put("shop_name", Objects.toString(input.get("shopName"), ""));
		m.put("resource_level_id", toLong(input.get("resourceLevelId")));
		m.put("resource_level_name", input.containsKey("resourceLevelName") ? Objects.toString(input.get("resourceLevelName"), "") : "");
		m.put("label_id", input.get("labelId"));
		m.put("label_name", input.containsKey("labelName") ? Objects.toString(input.get("labelName"), "") : "");
		m.put("rights_id", input.get("rightsId"));
		m.put("rights_name", input.containsKey("rightsName") ? Objects.toString(input.get("rightsName"), "") : "");
		m.put("date_day", ymd);
		m.put("begin_time", Objects.toString(input.get("beginTime"), ""));
		if (input.containsKey("endTime") && input.get("endTime") != null) {
			String et = Objects.toString(input.get("endTime"), "");
			if (StringUtils.hasText(et)) {
				m.put("end_time", et);
			}
		}
		m.put("num", 1);
		String instead = Objects.toString(input.get("instead"), "");
		m.put("status", "user".equals(instead) ? "success" : "system");
		m.put("user_name", input.containsKey("userName") ? Objects.toString(input.get("userName"), "") : "");
		m.put("mobile", input.containsKey("mobile") ? Objects.toString(input.get("mobile"), "") : "");
		m.put("sex", input.containsKey("sex") ? toIntObject(input.get("sex")) : 0);
		m.put("user_id", input.containsKey("userId") ? Objects.toString(input.get("userId"), "") : "");
		return m;
	}

	private static void removeBlankOptionalParams(Map<String, Object> m) {
		removeIfBlankString(m, "user_name");
		removeIfBlankString(m, "mobile");
		removeIfBlankString(m, "shop_name");
		removeIfBlankString(m, "resource_level_name");
		removeIfBlankString(m, "label_name");
		removeIfBlankString(m, "rights_name");
		removeIfBlankString(m, "end_time");

		Object uid = m.get("user_id");
		if (uid != null) {
			String us = uid.toString().trim();
			if (us.isEmpty()) {
				m.remove("user_id");
			} else {
				try {
					m.put("user_id", Long.parseLong(us));
				} catch (NumberFormatException e) {
					throw new ResourceException("用户id格式错误");
				}
			}
		}

		removeIfZeroOrBlankNonSexInt(m, "label_id");
		removeIfZeroOrBlankNonSexInt(m, "rights_id");
	}

	private static void removeIfZeroOrBlankNonSexInt(Map<String, Object> m, String key) {
		Object v = m.get(key);
		if (v == null) {
			return;
		}
		if (v instanceof Number n) {
			if (n.longValue() == 0L) {
				m.remove(key);
			}
			return;
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			m.remove(key);
		}
	}

	private static void removeIfBlankString(Map<String, Object> m, String key) {
		Object v = m.get(key);
		if (v == null) {
			m.remove(key);
			return;
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s)) {
			m.remove(key);
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

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}

	private static Integer toIntObject(Object o) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(o.toString().trim());
	}

	private static String clientIp(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		if (StringUtils.hasText(xff)) {
			return xff.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	@Activated(routeAlias = "reservation.get.list")
	@GetMapping(value = "/reservation", name = "查看预约记录")
	public ResponseEntity<ApiResult<Map<String, Object>>> getList(HttpServletRequest request) {
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

		reservationRoutePermissionPort.assertReservationGetListAllowed(user);
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		Map<String, String> queryMap = toQueryStringMap(request);
		ReservationListValidatedParams validated = reservationListParamValidator.validate(queryMap);

		if (!queryMap.containsKey("shopId")) {
			throw new BadRequestException("缺少必填字段: shopId");
		}

		Long shopIdOrNull = null;
		String shopRaw = queryMap.get("shopId");
		if (StringUtils.hasText(shopRaw == null ? "" : shopRaw.trim())) {
			try {
				shopIdOrNull = Long.parseLong(shopRaw.trim());
			} catch (NumberFormatException e) {
				throw new ResourceException("门店id无效");
			}
		}

		int agreementDateEpoch = agreementEpochForGetList(validated.dateDayRaw());

		Map<String, Object> data =
				reservationListQueryService.getReservationList(
						companyId,
						shopIdOrNull,
						agreementDateEpoch,
						validated.page(),
						validated.pageSize());

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/reservation");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(queryMap));
		} catch (Exception e) {
			logCtx.put("params", queryMap.toString());
		}
		logCtx.put("operator_name", "查看预约记录");
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
		}

		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static Map<String, String> toQueryStringMap(HttpServletRequest request) {
		Map<String, String> m = new LinkedHashMap<>();
		Enumeration<String> names = request.getParameterNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			m.put(name, request.getParameter(name));
		}
		return m;
	}

	private static int agreementEpochFromYmd(String ymd) {
		long sec = LocalDate.parse(ymd).atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
		return Math.toIntExact(sec);
	}

	/**
	 * 列表查询：日期在 ISO 日或 Unix 秒两种形式下解析；均失败时使用 0 作为 agreement 日 epoch，预约记录通常无匹配，
	 * 资源位列表仍可单独返回。
	 */
	private static int agreementEpochForGetList(String dateDayRaw) {
		String s = dateDayRaw.trim();
		try {
			String ymd = LocalDate.parse(s).toString();
			return agreementEpochFromYmd(ymd);
		} catch (Exception ignored) {
			// fall through
		}
		try {
			long sec = Long.parseLong(s);
			String ymd =
					Instant.ofEpochSecond(sec).atZone(ZoneId.systemDefault()).toLocalDate().toString();
			return agreementEpochFromYmd(ymd);
		} catch (Exception ignored) {
			return 0;
		}
	}

	@Activated(routeAlias = "reservation.get.everydaytime")
	@GetMapping(value = "/reservation/period", name = "获取每天预约时间段")
	public ResponseEntity<ApiResult<Object>> getEveryDayTimePeriod(HttpServletRequest request) {
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

		reservationRoutePermissionPort.assertReservationGetEveryDayTimePeriodAllowed(user);
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		Map<String, String> queryMap = toQueryStringMap(request);
		ReservationEveryDayTimePeriodParamValidator.EveryDayTimePeriodQuery q =
				reservationEveryDayTimePeriodParamValidator.validate(queryMap);
		String shopIdRaw = q.shopIdRaw();
		String dateDayRaw = q.dateDayRaw();

		int dayEpochSeconds = agreementEpochForGetList(dateDayRaw);
		Optional<ReservationEveryDayTimePeriodPayload> opt =
				reservationEveryDayTimePeriodService.buildResponse(companyId, shopIdRaw, dateDayRaw, dayEpochSeconds);
		if (opt.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(List.of()));
		}
		return ResponseEntity.ok(ApiResult.ok(opt.get()));
	}
}
