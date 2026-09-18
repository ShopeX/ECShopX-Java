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

package cn.shopex.ecshopx.reservation.service;

import cn.shopex.ecshopx.reservation.domain.ReservationEveryDayTimePeriodPayload;
import cn.shopex.ecshopx.reservation.domain.ReservationSetting;
import cn.shopex.ecshopx.reservation.port.ReservationShopDetailPort;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ReservationEveryDayTimePeriodService {

	private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

	private final ReservationSettingQueryService reservationSettingQueryService;
	private final ReservationShopDetailPort reservationShopDetailPort;

	public ReservationEveryDayTimePeriodService(
			ReservationSettingQueryService reservationSettingQueryService,
			ReservationShopDetailPort reservationShopDetailPort) {
		this.reservationSettingQueryService = reservationSettingQueryService;
		this.reservationShopDetailPort = reservationShopDetailPort;
	}

	public Optional<ReservationEveryDayTimePeriodPayload> buildResponse(
			long companyId, String shopIdRaw, String dateDayRaw, int dayEpochSeconds) {
		String sid = shopIdRaw == null ? "" : shopIdRaw.trim();
		if (!StringUtils.hasText(sid) || "undefined".equalsIgnoreCase(sid)) {
			return Optional.empty();
		}
		long shopId;
		try {
			shopId = Long.parseLong(sid);
		} catch (NumberFormatException e) {
			return Optional.empty();
		}

		Optional<ReservationSetting> settingOpt = reservationSettingQueryService.findByCompanyId(companyId);
		if (settingOpt.isEmpty()) {
			return Optional.empty();
		}
		ReservationSetting setting = settingOpt.get();
		Integer intervalMin = setting.getTimeInterval();
		if (intervalMin == null || intervalMin <= 0) {
			return Optional.empty();
		}

		Optional<Map<String, Object>> shopOpt =
				reservationShopDetailPort.findShopForEveryDayTimePeriod(shopId, companyId);
		if (shopOpt.isEmpty()) {
			return Optional.empty();
		}
		Map<String, Object> store = shopOpt.get();
		Object cidObj = store.get("company_id");
		if (companyId > 0) {
			if (cidObj == null) {
				return Optional.empty();
			}
			long storeCompany = toLong(cidObj);
			if (storeCompany != companyId) {
				return Optional.empty();
			}
		}

		String hourRaw = Objects.toString(store.get("hour"), "");
		if (!StringUtils.hasText(hourRaw.trim())) {
			return Optional.empty();
		}
		String openWindow = hourRaw.trim();
		int dash = openWindow.indexOf('-');
		if (dash < 0) {
			return Optional.empty();
		}
		String beginTime = openWindow.substring(0, dash).trim();
		String endTime = openWindow.substring(dash + 1).trim();
		if (!StringUtils.hasText(beginTime) || !StringUtils.hasText(endTime)) {
			return Optional.empty();
		}

		List<Map<String, String>> slices =
				computeTimePeriodForDay(dayEpochSeconds, beginTime, endTime, intervalMin);
		if (slices.isEmpty()) {
			return Optional.empty();
		}

		String resourceName = setting.getResourceName() != null ? setting.getResourceName() : "";
		List<Map<String, String>> tableTitle = new ArrayList<>();
		Map<String, String> nameRow = new LinkedHashMap<>();
		nameRow.put("begin", resourceName);
		tableTitle.add(nameRow);
		for (Map<String, String> slot : slices) {
			Map<String, String> row = new LinkedHashMap<>();
			row.put("begin", slot.get("begin"));
			row.put("end", slot.get("end"));
			tableTitle.add(row);
		}

		String maxLimitDay =
				setting.getMaxLimitDay() != null ? String.valueOf(setting.getMaxLimitDay()) : "";
		String minLimitHour =
				setting.getMinLimitHour() != null ? String.valueOf(setting.getMinLimitHour()) : "";

		return Optional.of(new ReservationEveryDayTimePeriodPayload(tableTitle, maxLimitDay, minLimitHour));
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}

	/**
	 * 在系统默认时区下，将 {@code dayEpochSeconds} 对应的日历日与 {@code beginTime}、{@code endTime}（{@code HH:mm}）解析为当日营业起止时刻；
	 * 自营业开始按 {@code timeIntervalMinutes} 分钟为步长连续切片，每段为包含 {@code begin}、{@code end} 的映射，值为该日内的 {@code HH:mm} 字符串（内部先用 Unix 秒推进再格式化）。
	 * 时间解析失败，或营业结束不晚于开始时，返回空列表。
	 */
	private static List<Map<String, String>> computeTimePeriodForDay(
			long dayEpochSeconds, String beginTime, String endTime, int timeIntervalMinutes) {
		ZoneId z = ZoneId.systemDefault();
		LocalDate day =
				Instant.ofEpochSecond(dayEpochSeconds).atZone(z).toLocalDate();
		LocalTime beginLt;
		LocalTime endLt;
		try {
			beginLt = LocalTime.parse(beginTime.trim(), HM);
			endLt = LocalTime.parse(endTime.trim(), HM);
		} catch (Exception e) {
			return List.of();
		}
		long beginEpoch = day.atTime(beginLt).atZone(z).toEpochSecond();
		long endEpoch = day.atTime(endLt).atZone(z).toEpochSecond();
		if (beginEpoch >= endEpoch) {
			return List.of();
		}
		long intervalSec = timeIntervalMinutes * 60L;
		List<Map<String, String>> result = new ArrayList<>();
		long cur = beginEpoch;
		while (true) {
			long next = cur + intervalSec;
			Map<String, String> row = new LinkedHashMap<>();
			row.put("begin", HM.format(Instant.ofEpochSecond(cur).atZone(z).toLocalDateTime().toLocalTime()));
			row.put("end", HM.format(Instant.ofEpochSecond(next).atZone(z).toLocalDateTime().toLocalTime()));
			result.add(row);
			cur = next;
			if (cur >= endEpoch) {
				break;
			}
		}
		return result;
	}
}
