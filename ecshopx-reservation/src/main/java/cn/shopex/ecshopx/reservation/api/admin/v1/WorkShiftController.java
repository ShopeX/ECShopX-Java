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
import cn.shopex.ecshopx.reservation.service.ReservationDateWeeksCalendarService;
import cn.shopex.ecshopx.reservation.service.WeekRange;
import cn.shopex.ecshopx.reservation.service.WorkShiftCreateService;
import cn.shopex.ecshopx.reservation.service.WorkShiftDeleteService;
import cn.shopex.ecshopx.reservation.service.WorkShiftListService;
import cn.shopex.ecshopx.reservation.service.WorkShiftUpdateService;
import cn.shopex.ecshopx.reservation.util.StringBooleanParity;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RestController("reservationWorkShiftAdminV1")
@RequestMapping("/api/v1")
public class WorkShiftController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final ReservationRoutePermissionPort reservationRoutePermissionPort;
	private final ShopOperatorCompanyActivation shopOperatorCompanyActivation;
	private final WorkShiftCreateService workShiftCreateService;
	private final WorkShiftUpdateService workShiftUpdateService;
	private final WorkShiftDeleteService workShiftDeleteService;
	private final WorkShiftListService workShiftListService;
	private final ReservationDateWeeksCalendarService reservationDateWeeksCalendarService;
	private final ShopOperatorLogsWrite shopOperatorLogsWrite;
	private final ObjectMapper objectMapper;

	public WorkShiftController(
			ReservationRoutePermissionPort reservationRoutePermissionPort,
			ShopOperatorCompanyActivation shopOperatorCompanyActivation,
			WorkShiftCreateService workShiftCreateService,
			WorkShiftUpdateService workShiftUpdateService,
			WorkShiftDeleteService workShiftDeleteService,
			WorkShiftListService workShiftListService,
			ReservationDateWeeksCalendarService reservationDateWeeksCalendarService,
			ShopOperatorLogsWrite shopOperatorLogsWrite,
			ObjectMapper objectMapper) {
		this.reservationRoutePermissionPort = reservationRoutePermissionPort;
		this.shopOperatorCompanyActivation = shopOperatorCompanyActivation;
		this.workShiftCreateService = workShiftCreateService;
		this.workShiftUpdateService = workShiftUpdateService;
		this.workShiftDeleteService = workShiftDeleteService;
		this.workShiftListService = workShiftListService;
		this.reservationDateWeeksCalendarService = reservationDateWeeksCalendarService;
		this.shopOperatorLogsWrite = shopOperatorLogsWrite;
		this.objectMapper = objectMapper;
	}

	@Activated(routeAlias = "work.shift.create")
	@PostMapping(value = "/workshift", name = "新增排班")
	public ResponseEntity<ApiResult<Map<String, Object>>> createWorkShift(
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
		validateCreateWorkShiftInput(input);

		long shopId = parseRequiredLongField(input.get("shopId"), "shopId");
		String shiftTypeId =
				input.get("shiftTypeId") != null ? input.get("shiftTypeId").toString().trim() : "";
		long resourceLevelId = parseRequiredLongField(input.get("resourceLevelId"), "resourceLevelId");

		String dateDayRaw = input.get("dateDay").toString().trim();
		long workDateStartOfDayEpoch;
		try {
			LocalDate d = LocalDate.parse(dateDayRaw, DateTimeFormatter.ISO_LOCAL_DATE);
			workDateStartOfDayEpoch = d.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
		} catch (DateTimeParseException ex) {
			throw new BadRequestException("dateDay 日期格式无效，需为 yyyy-MM-dd");
		}

		long nowEpochSec = Instant.now().getEpochSecond();
		if (workDateStartOfDayEpoch <= nowEpochSec) {
			throw new ResourceException("只能对今天之后做排班");
		}

		Map<String, Object> statusPayload =
				workShiftCreateService.createWorkShift(
						companyId, shopId, shiftTypeId, workDateStartOfDayEpoch, resourceLevelId);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/workshift");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(input));
		} catch (Exception e) {
			logCtx.put("params", input.toString());
		}
		logCtx.put("operator_name", "新增排班");
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

	private static void validateCreateWorkShiftInput(Map<String, Object> input) {
		List<String> parts = new ArrayList<>();
		if (isBlank(input.get("shopId"))) {
			parts.add("shopId 必填");
		}
		if (isBlank(input.get("shiftTypeId"))) {
			parts.add("shiftTypeId 必填");
		}
		if (isBlank(input.get("dateDay"))) {
			parts.add("dateDay 必填");
		}
		if (isBlank(input.get("resourceLevelId"))) {
			parts.add("resourceLevelId 必填");
		}
		if (!parts.isEmpty()) {
			throw new BadRequestException(String.join("\uFF0C", parts));
		}
	}

	private static void validateUpdateWorkShiftInput(Map<String, Object> input) {
		List<String> parts = new ArrayList<>();
		if (isBlank(input.get("shopId"))) {
			parts.add("shopId 必填");
		}
		if (isBlank(input.get("shiftTypeId"))) {
			parts.add("shiftTypeId 必填");
		}
		if (isBlank(input.get("dateDay"))) {
			parts.add("dateDay 必填");
		}
		if (isBlank(input.get("resourceLevelId"))) {
			parts.add("resourceLevelId 必填");
		}
		if (isBlank(input.get("id"))) {
			parts.add("id 必填");
		}
		if (!parts.isEmpty()) {
			throw new BadRequestException(String.join("\uFF0C", parts));
		}
	}

	private static long parseRequiredLongField(Object raw, String fieldName) {
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException ex) {
			throw new BadRequestException(fieldName + " 格式无效");
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

	private static Long parseOptionalWorkShiftIdForDelete(Object idRaw) {
		if (isBlank(idRaw)) {
			return null;
		}
		try {
			return Long.parseLong(String.valueOf(idRaw).trim());
		} catch (NumberFormatException ex) {
			return null;
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

	@Activated(routeAlias = "work.shift.delete")
	@DeleteMapping(value = "/workshift", name = "删除排班")
	public ResponseEntity<?> deleteWorkShift(
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

		reservationRoutePermissionPort.assertWorkShiftDeleteAllowed(user);
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		Map<String, Object> input = mergeInputLikeFlexibleResolver(request, body);
		if (!input.containsKey("id")) {
			throw new BadRequestException("缺少必填字段: id");
		}
		Object idRaw = input.get("id");
		Long workShiftIdOrNull = parseOptionalWorkShiftIdForDelete(idRaw);

		boolean deleted = workShiftDeleteService.deleteWorkShift(companyId, workShiftIdOrNull);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/workshift");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(input));
		} catch (Exception e) {
			logCtx.put("params", input.toString());
		}
		logCtx.put("operator_name", "删除排班");
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

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", deleted)));
	}

	@Activated(routeAlias = "work.shift.update")
	@PatchMapping(value = "/workshift", name = "编辑排班")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateWorkShift(
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

		reservationRoutePermissionPort.assertWorkShiftUpdateAllowed(user);
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		Map<String, Object> input = mergeInputLikeFlexibleResolver(request, body);
		validateUpdateWorkShiftInput(input);

		long shopId = parseRequiredLongField(input.get("shopId"), "shopId");
		long workShiftId = parseRequiredLongField(input.get("id"), "id");
		String shiftTypeId =
				input.get("shiftTypeId") != null ? input.get("shiftTypeId").toString().trim() : "";
		long resourceLevelId = parseRequiredLongField(input.get("resourceLevelId"), "resourceLevelId");

		String dateDayRaw = input.get("dateDay").toString().trim();
		long workDateStartOfDayEpoch;
		try {
			LocalDate d = LocalDate.parse(dateDayRaw, DateTimeFormatter.ISO_LOCAL_DATE);
			workDateStartOfDayEpoch = d.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
		} catch (DateTimeParseException ex) {
			throw new BadRequestException("dateDay 日期格式无效，需为 yyyy-MM-dd");
		}

		long nowEpochSec = Instant.now().getEpochSecond();
		if (workDateStartOfDayEpoch <= nowEpochSec) {
			throw new ResourceException("历史排班不可编辑");
		}

		Map<String, Object> statusPayload =
				workShiftUpdateService.updateWorkShift(
						companyId,
						shopId,
						shiftTypeId,
						workDateStartOfDayEpoch,
						resourceLevelId,
						workShiftId);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/workshift");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(input));
		} catch (Exception e) {
			logCtx.put("params", input.toString());
		}
		logCtx.put("operator_name", "编辑排班");
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

	@Activated(routeAlias = "work.shift.getlist")
	@GetMapping(value = "/workshift", name = "排班列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getListWorkShift(
			HttpServletRequest request,
			@RequestParam(value = "shopId", required = false) String shopId,
			@RequestParam(value = "dateData", required = false) String dateData) {
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

		reservationRoutePermissionPort.assertWorkShiftGetListAllowed(user);
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		validateGetListWorkShiftQuery(shopId, dateData);
		long[] range = parseDateDataToEpochRange(dateData);
		Map<String, Object> payload =
				workShiftListService.getListWorkShift(companyId, shopId.trim(), range[0], range[1]);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	private static void validateGetListWorkShiftQuery(String shopId, String dateData) {
		List<String> chunks = new ArrayList<>();
		if (!StringUtils.hasText(shopId == null ? "" : shopId.trim())) {
			chunks.add("店铺id必填");
		}
		if (!StringUtils.hasText(dateData == null ? "" : dateData.trim())) {
			chunks.add("周期时间必填");
		}
		if (chunks.isEmpty()) {
			return;
		}
		StringBuilder errmsg = new StringBuilder();
		for (String c : chunks) {
			errmsg.append(c).append("，");
		}
		throw new ResourceException(errmsg.toString());
	}

	private static long[] parseDateDataToEpochRange(String dateData) {
		String t = dateData.trim();
		String[] parts = t.split("-", -1);
		if (parts.length < 2) {
			throw new ResourceException("周期时间格式无效");
		}
		String beginStr = parts[0].trim();
		String endStr = parts[1].trim();
		try {
			long begin = Long.parseLong(beginStr);
			long end = Long.parseLong(endStr);
			return new long[] {begin, end};
		} catch (NumberFormatException e) {
			throw new ResourceException("周期时间格式无效");
		}
	}

	@Activated(routeAlias = "work.shift.getweekday")
	@GetMapping(value = "/getweekday", name = "获取每年的周日期")
	public ResponseEntity<ApiResult<Map<String, Object>>> getEveryYearWeeks(
			HttpServletRequest request,
			@RequestParam(name = "year", required = false) String year) {
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

		reservationRoutePermissionPort.assertWorkShiftGetWeekdayAllowed(user);
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		ZoneId zone = ZoneId.systemDefault();
		String effectiveYearString =
				StringBooleanParity.isTruthyBool(year)
						? year
						: String.valueOf(Year.now(zone).getValue());

		Map<Integer, WeekRange> weekDay =
				reservationDateWeeksCalendarService.getWeeksYearBoundaries(effectiveYearString);

		int resolvedYear =
				reservationDateWeeksCalendarService.resolveEffectiveCalendarYear(effectiveYearString);
		LocalDate today = LocalDate.now(zone);
		String nowComposite =
				resolvedYear
						+ "-"
						+ String.format("%02d", today.getMonthValue())
						+ "-"
						+ String.format("%02d", today.getDayOfMonth());
		long nowtime;
		try {
			nowtime =
					LocalDate.parse(nowComposite, DateTimeFormatter.ISO_LOCAL_DATE)
							.atStartOfDay(zone)
							.toEpochSecond();
		} catch (DateTimeParseException ex) {
			nowtime = Long.MIN_VALUE;
		}

		DateTimeFormatter monthDayCn = DateTimeFormatter.ofPattern("MM月dd日", Locale.CHINA);
		List<Map<String, Object>> list = new ArrayList<>();
		Map<String, Object> weekMapOrNull = null;
		int i = 0;
		for (Map.Entry<Integer, WeekRange> en : weekDay.entrySet()) {
			int weekKey = en.getKey();
			WeekRange wr = en.getValue();
			long b = wr.beginEpochSecond();
			long e = wr.endEpochSecond();
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("value", weekKey);
			row.put("label", b + "-" + e);
			LocalDate bd = Instant.ofEpochSecond(b).atZone(zone).toLocalDate();
			LocalDate ed = Instant.ofEpochSecond(e).atZone(zone).toLocalDate();
			row.put("name", bd.format(monthDayCn) + "-" + ed.format(monthDayCn));
			list.add(row);
			if (b <= nowtime && nowtime <= e) {
				Map<String, Object> w = new LinkedHashMap<>();
				w.put("label", b + "-" + e);
				w.put("value", i);
				w.put("name", bd.format(monthDayCn) + "-" + ed.format(monthDayCn));
				weekMapOrNull = w;
			}
			i++;
		}

		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("list", list);
		data.put("week", weekMapOrNull);
		return ResponseEntity.ok(ApiResult.ok(data));
	}
}
