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

package cn.shopex.ecshopx.reservation.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.web.WxappMemberAuthAttributes;
import cn.shopex.ecshopx.reservation.service.ReservationCreateService;
import cn.shopex.ecshopx.reservation.service.ReservationDateDayQueryService;
import cn.shopex.ecshopx.reservation.service.ReservationWxappGetRecordCountService;
import cn.shopex.ecshopx.reservation.service.ReservationWxappRecordListQueryService;
import cn.shopex.ecshopx.reservation.service.ReservationWxappTimelistQueryService;
import cn.shopex.ecshopx.reservation.service.ReservationWxappLimitCheckService;
import cn.shopex.ecshopx.reservation.service.WxappCanReservationRightsParamValidator;
import cn.shopex.ecshopx.reservation.service.WxappCanReservationRightsQueryService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400)
@RestController("reservationWxappFrontV1")
@RequestMapping("/api/v1/wxapp")
public class ReservationController {

	private final ReservationCreateService reservationCreateService;

	private final ReservationWxappLimitCheckService reservationWxappLimitCheckService;

	private final WxappCanReservationRightsParamValidator wxappCanReservationRightsParamValidator;

	private final WxappCanReservationRightsQueryService wxappCanReservationRightsQueryService;

	private final ReservationDateDayQueryService reservationDateDayQueryService;

	private final ReservationWxappGetRecordCountService reservationWxappGetRecordCountService;

	private final ReservationWxappRecordListQueryService reservationWxappRecordListQueryService;

	private final ReservationWxappTimelistQueryService reservationWxappTimelistQueryService;

	public ReservationController(
			ReservationCreateService reservationCreateService,
			ReservationWxappLimitCheckService reservationWxappLimitCheckService,
			WxappCanReservationRightsParamValidator wxappCanReservationRightsParamValidator,
			WxappCanReservationRightsQueryService wxappCanReservationRightsQueryService,
			ReservationDateDayQueryService reservationDateDayQueryService,
			ReservationWxappGetRecordCountService reservationWxappGetRecordCountService,
			ReservationWxappRecordListQueryService reservationWxappRecordListQueryService,
			ReservationWxappTimelistQueryService reservationWxappTimelistQueryService) {
		this.reservationCreateService = reservationCreateService;
		this.reservationWxappLimitCheckService = reservationWxappLimitCheckService;
		this.wxappCanReservationRightsParamValidator = wxappCanReservationRightsParamValidator;
		this.wxappCanReservationRightsQueryService = wxappCanReservationRightsQueryService;
		this.reservationDateDayQueryService = reservationDateDayQueryService;
		this.reservationWxappGetRecordCountService = reservationWxappGetRecordCountService;
		this.reservationWxappRecordListQueryService = reservationWxappRecordListQueryService;
		this.reservationWxappTimelistQueryService = reservationWxappTimelistQueryService;
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400)
	@PostMapping(value = "/reservation", name = "用户提交预约数据")
	public ResponseEntity<ApiResult<Map<String, Object>>> createReservation(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}

		Map<String, Object> input = mergeInputLikeFlexibleResolver(request, body);

		if (isBlank(input.get("shopId"))) {
			throw new ResourceException("门店必选");
		}
		if (isBlank(input.get("beginTime"))) {
			throw new ResourceException("时段必选");
		}

		String dateDayRaw = Objects.toString(input.get("dateDay"), "");
		String normalizedYmd = normalizeDateDayYmd(dateDayRaw);

		Map<String, Object> postData = buildWxappPostData(auth, input, normalizedYmd);
		reservationWxappLimitCheckService.checkLimit(postData);
		reservationCreateService.createReservation(postData);

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	private Map<String, Object> buildWxappPostData(
			Map<String, Object> auth, Map<String, Object> input, String ymd) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", toLong(auth.get("company_id")));
		m.put("shop_id", toLong(input.get("shopId")));
		m.put("shop_name", Objects.toString(input.get("shopName"), ""));
		if (input.containsKey("resourceLevelId")
				&& input.get("resourceLevelId") != null
				&& !isBlank(input.get("resourceLevelId"))) {
			m.put("resource_level_id", toLong(input.get("resourceLevelId")));
		}
		if (input.containsKey("resourceLevelName")) {
			m.put("resource_level_name", Objects.toString(input.get("resourceLevelName"), ""));
		}
		m.put("label_id", input.get("labelId"));
		if (input.containsKey("labelName")) {
			m.put("label_name", Objects.toString(input.get("labelName"), ""));
		}
		m.put("rights_id", input.get("rightsId"));
		if (input.containsKey("rightsName")) {
			m.put("rights_name", Objects.toString(input.get("rightsName"), ""));
		}
		m.put("date_day", ymd);
		m.put("begin_time", Objects.toString(input.get("beginTime"), ""));
		if (input.containsKey("endTime") && input.get("endTime") != null) {
			String et = Objects.toString(input.get("endTime"), "");
			if (StringUtils.hasText(et)) {
				m.put("end_time", et);
			}
		}
		m.put("num", 1);
		m.put("status", "success");
		m.put(
				"user_name",
				firstNonBlankString(auth.get("username"), auth.get("nickname"), "会员"));
		m.put("mobile", Objects.toString(auth.get("mobile"), ""));
		m.put("sex", auth.containsKey("sex") ? toIntObject(auth.get("sex")) : 0);
		putUserIdLikeAdmin(m, auth.get("user_id"));
		m.put("open_id", Objects.toString(auth.get("open_id"), ""));
		m.put("wxapp_appid", Objects.toString(auth.get("wxapp_appid"), ""));
		return m;
	}

	private static void putUserIdLikeAdmin(Map<String, Object> m, Object userIdFromAuth) {
		if (userIdFromAuth == null) {
			return;
		}
		String us = userIdFromAuth.toString().trim();
		if (us.isEmpty()) {
			m.put("user_id", "");
			return;
		}
		try {
			m.put("user_id", Long.parseLong(us));
		} catch (NumberFormatException e) {
			m.put("user_id", us);
		}
	}

	private static String firstNonBlankString(Object a, Object b, String fallback) {
		if (a != null && StringUtils.hasText(a.toString().trim())) {
			return a.toString().trim();
		}
		if (b != null && StringUtils.hasText(b.toString().trim())) {
			return b.toString().trim();
		}
		return fallback;
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

	@GetMapping(value = "/reservation/dateDay", name = "获取可预约的具体日期")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getReservationDate(
			HttpServletRequest request,
			@RequestParam(value = "endDate", required = false) String endDate) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		if (isBlank(auth.get("company_id"))) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = toLong(auth.get("company_id"));
		List<Map<String, Object>> rows = reservationDateDayQueryService.buildDateDayRows(companyId, endDate);
		return ResponseEntity.ok(ApiResult.ok(rows));
	}

	@GetMapping(value = "/reservation/recordlist", name = "获取预约记录列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getRecordList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String page,
			@RequestParam(value = "pageSize", required = false) String pageSize) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		if (isBlank(auth.get("company_id"))) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> result = reservationWxappRecordListQueryService.query(auth, page, pageSize);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@GetMapping(value = "/reservation/timelist", name = "获取可预约的时段")
	public ResponseEntity<ApiResult<Object>> getTimelist(
			HttpServletRequest request,
			@RequestParam(value = "shopId", required = false) String shopId,
			@RequestParam(value = "dateDay", required = false) String dateDay,
			@RequestParam(value = "labelId", required = false) String labelId,
			@RequestParam(value = "levelId", required = false) String levelId) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		if (isBlank(auth.get("company_id"))) {
			throw new UnauthorizedException("未登录");
		}
		if (!isShopIdAndDateDayTruthyForTimelist(shopId, dateDay)) {
			return ResponseEntity.ok(ApiResult.ok(List.of()));
		}
		long companyId = toLong(auth.get("company_id"));
		String levelIdOrEmpty = levelId != null ? levelId : "";
		Object data =
				reservationWxappTimelistQueryService.buildResponse(
						companyId, shopId, dateDay, labelId, levelIdOrEmpty);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	/** Both query params present, non-blank after trim, neither {@code "0"}, {@code shopId} not {@code undefined}. */
	private static boolean isShopIdAndDateDayTruthyForTimelist(String shopId, String dateDay) {
		if (shopId == null || dateDay == null) {
			return false;
		}
		String s = shopId.trim();
		String d = dateDay.trim();
		if (!StringUtils.hasText(s) || !StringUtils.hasText(d)) {
			return false;
		}
		if ("0".equals(s) || "0".equals(d)) {
			return false;
		}
		if ("undefined".equalsIgnoreCase(s)) {
			return false;
		}
		return true;
	}

	@GetMapping(value = "/reservation/getCount", name = "获取指定项目的预约量")
	public ResponseEntity<ApiResult<Map<String, Object>>> getRecordCount(
			HttpServletRequest request,
			@RequestParam(value = "rights_id", required = false) String rightsId) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		if (isBlank(auth.get("company_id"))) {
			throw new UnauthorizedException("未登录");
		}
		long totalCount = reservationWxappGetRecordCountService.count(auth, rightsId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("total_count", totalCount)));
	}

	@GetMapping(value = "/can/reservation/rights", name = "获取可用预约项目")
	public ResponseEntity<ApiResult<Map<String, Object>>> getCanReservationRights(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String page,
			@RequestParam(value = "pageSize", required = false) String pageSize) {
		WxappCanReservationRightsParamValidator.ValidatedPageParams p =
				wxappCanReservationRightsParamValidator.validate(page, pageSize);
		Map<String, Object> result = wxappCanReservationRightsQueryService.query(request, p.page(), p.pageSize());
		return ResponseEntity.ok(ApiResult.ok(result));
	}
}
