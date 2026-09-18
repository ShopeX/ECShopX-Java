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

import cn.shopex.ecshopx.reservation.domain.ReservationRecord;
import cn.shopex.ecshopx.reservation.domain.ReservationSetting;
import cn.shopex.ecshopx.reservation.mapper.ReservationRecordMapper;
import cn.shopex.ecshopx.reservation.port.ReservationShopDetailPort;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ReservationWxappTimelistQueryService {

	private static final List<String> RECORD_STATUSES =
			List.of("system", "success", "not_to_shop", "to_the_shop");

	private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter H_MM = DateTimeFormatter.ofPattern("H:mm");

	private static final DateTimeFormatter WEEKDAY_EN =
			DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH);

	private final ReservationSettingQueryService reservationSettingQueryService;
	private final ReservationShopDetailPort reservationShopDetailPort;
	private final WorkShiftListService workShiftListService;
	private final ResourceLevelListByMaterialService resourceLevelListByMaterialService;
	private final ReservationRecordMapper reservationRecordMapper;

	public ReservationWxappTimelistQueryService(
			ReservationSettingQueryService reservationSettingQueryService,
			ReservationShopDetailPort reservationShopDetailPort,
			WorkShiftListService workShiftListService,
			ResourceLevelListByMaterialService resourceLevelListByMaterialService,
			ReservationRecordMapper reservationRecordMapper) {
		this.reservationSettingQueryService = reservationSettingQueryService;
		this.reservationShopDetailPort = reservationShopDetailPort;
		this.workShiftListService = workShiftListService;
		this.resourceLevelListByMaterialService = resourceLevelListByMaterialService;
		this.reservationRecordMapper = reservationRecordMapper;
	}

	@Transactional(readOnly = true)
	public Object buildResponse(
			long companyId, String shopIdRaw, String dateDayRaw, String labelIdRaw, String levelIdRaw) {
		String shopTrim = shopIdRaw == null ? "" : shopIdRaw.trim();
		if (!StringUtils.hasText(shopTrim) || "undefined".equalsIgnoreCase(shopTrim)) {
			return Collections.emptyList();
		}
		long shopIdLong;
		try {
			shopIdLong = Long.parseLong(shopTrim);
		} catch (NumberFormatException e) {
			return Collections.emptyList();
		}

		Optional<ReservationSetting> settingOpt = reservationSettingQueryService.findByCompanyId(companyId);
		if (settingOpt.isEmpty()) {
			return Collections.emptyList();
		}
		ReservationSetting setting = settingOpt.get();
		Integer timeInterval = setting.getTimeInterval();
		if (timeInterval == null || timeInterval <= 0) {
			return Collections.emptyList();
		}

		Optional<Map<String, Object>> shopOpt =
				reservationShopDetailPort.findShopForWxappTimelist(shopIdLong, companyId);
		if (shopOpt.isEmpty()) {
			return Collections.emptyList();
		}
		Map<String, Object> storeData = shopOpt.get();
		Object hourObj = storeData.get("hour");
		String storeOpenTime = hourObj == null ? "" : hourObj.toString().trim();
		if (!StringUtils.hasText(storeOpenTime)) {
			return Collections.emptyList();
		}
		String[] hourParts = storeOpenTime.split("-", 2);
		if (hourParts.length != 2) {
			return Collections.emptyList();
		}
		String beginHm = hourParts[0].trim();
		String endHm = hourParts[1].trim();
		if (!StringUtils.hasText(beginHm) || !StringUtils.hasText(endHm)) {
			return Collections.emptyList();
		}

		OptionalLong dayEpochOpt = resolveDayEpochStart(dateDayRaw);
		if (dayEpochOpt.isEmpty()) {
			return Collections.emptyList();
		}
		long dayEpoch = dayEpochOpt.getAsLong();

		List<LinkedHashMap<String, Long>> unixSlices =
				computeUnixTimeSlices(dayEpoch, beginHm, endHm, timeInterval);

		if (isLabelAbsent(labelIdRaw)) {
			LinkedHashMap<String, Object> result = new LinkedHashMap<>();
			result.put("timeData", unixSlices);
			result.put("resourceName", setting.getResourceName() != null ? setting.getResourceName() : "");
			result.put(
					"maxLimitDay",
					setting.getMaxLimitDay() != null ? setting.getMaxLimitDay() : 0);
			result.put(
					"minLimitHour",
					setting.getMinLimitHour() != null ? setting.getMinLimitHour() : 0);
			return result;
		}

		ZoneId zone = ZoneId.systemDefault();
		String weekKey = weekKeyEnglish(dayEpoch, zone);
		Map<String, Object> workShiftRoot =
				workShiftListService.getListWorkShift(companyId, String.valueOf(shopIdLong), dayEpoch, dayEpoch);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> resourceLevelRows =
				(List<Map<String, Object>>) workShiftRoot.get("resourceLevel");
		if (resourceLevelRows == null) {
			resourceLevelRows = List.of();
		}

		Long levelFilter = parseOptionalLongFilter(levelIdRaw);
		LinkedHashMap<Long, LinkedHashMap<String, Object>> shiftByResourceLevelId = new LinkedHashMap<>();
		for (Map<String, Object> row : resourceLevelRows) {
			long resourceLevelId = toLong(row.get("resourceLevelId"));
			if (levelFilter != null && resourceLevelId != levelFilter) {
				continue;
			}
			Object wk = row.get(weekKey);
			if (!(wk instanceof Map<?, ?>)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> shift = (Map<String, Object>) wk;
			if (isRestShiftType(shift.get("typeId"))) {
				continue;
			}
			shiftByResourceLevelId.put(resourceLevelId, new LinkedHashMap<>(shift));
		}

		List<Map<String, Object>> resourceByMaterial =
				resourceLevelListByMaterialService.listByMaterial(companyId, labelIdRaw, shopIdLong);
		if (resourceByMaterial.isEmpty()) {
			return Collections.emptyList();
		}

		List<LinkedHashMap<String, Object>> availableArr = new ArrayList<>();
		for (Map<String, Object> resourceRow : resourceByMaterial) {
			long rid = toLong(resourceRow.get("resourceLevelId"));
			LinkedHashMap<String, Object> shiftDay = shiftByResourceLevelId.get(rid);
			if (shiftDay == null) {
				continue;
			}
			LinkedHashMap<String, Object> merged = new LinkedHashMap<>(shiftDay);
			merged.putAll(resourceRow);
			availableArr.add(merged);
		}
		if (availableArr.isEmpty()) {
			return Collections.emptyList();
		}

		Integer minLimitHour = setting.getMinLimitHour();
		long after = Instant.now().getEpochSecond() + (minLimitHour != null ? minLimitHour : 0) * 60L;

		List<Map<String, Object>> timeData = new ArrayList<>();
		for (LinkedHashMap<String, Long> slice : unixSlices) {
			long slotBegin = slice.get("begin");
			long slotEnd = slice.get("end");
			String beginTimeStr = formatHm(slotBegin, zone);
			String endTimeStr = formatHm(slotEnd, zone);
			LinkedHashMap<String, Object> slot = new LinkedHashMap<>();
			slot.put("begin_time", beginTimeStr);
			slot.put("end_time", endTimeStr);
			slot.put("status", slotBegin >= after ? 1 : 0);
			timeData.add(slot);
		}

		for (int i = 0; i < timeData.size(); i++) {
			Map<String, Object> slot = timeData.get(i);
			Object st = slot.get("status");
			int statusVal = st instanceof Number n ? n.intValue() : 0;
			if (statusVal != 1) {
				continue;
			}
			String slotBeginHm = Objects.toString(slot.get("begin_time"), "");
			LinkedHashMap<String, Object> levelMap = new LinkedHashMap<>();
			int covered = 0;
			for (Map<String, Object> val : availableArr) {
				String bt = Objects.toString(val.get("beginTime"), "");
				String et = Objects.toString(val.get("endTime"), "");
				if (bt.compareTo(slotBeginHm) <= 0 && et.compareTo(slotBeginHm) > 0) {
					String key = String.valueOf(val.get("resourceLevelId"));
					levelMap.put(key, val);
					covered = 1;
				}
			}
			slot.put("status", covered);
			if (covered == 1) {
				slot.put("level", levelMap);
			} else {
				slot.remove("level");
			}
		}

		List<ReservationRecord> records =
				reservationRecordMapper.selectList(
						Wrappers.<ReservationRecord>lambdaQuery()
								.eq(ReservationRecord::getCompanyId, companyId)
								.eq(ReservationRecord::getShopId, shopIdLong)
								.eq(ReservationRecord::getAgreementDate, (int) dayEpoch)
								.in(ReservationRecord::getStatus, RECORD_STATUSES));
		if (records != null && !records.isEmpty()) {
			for (Map<String, Object> slot : timeData) {
				if (slot.containsKey("level") && slot.get("level") instanceof Map<?, ?>) {
					@SuppressWarnings("unchecked")
					Map<String, Object> level =
							(Map<String, Object>) slot.get("level");
					int origStatus = ((Number) slot.get("status")).intValue();
					String slotBt = Objects.toString(slot.get("begin_time"), "");
					for (ReservationRecord rec : records) {
						if (rec.getBeginTime() == null || rec.getResourceLevelId() == null) {
							continue;
						}
						if (!rec.getBeginTime().trim().equals(slotBt)) {
							continue;
						}
						level.remove(String.valueOf(rec.getResourceLevelId()));
					}
					if (level.isEmpty() && origStatus == 1) {
						slot.put("status", 0);
					}
				} else {
					slot.put("status", 0);
				}
			}
		}

		return timeData;
	}

	private static boolean isLabelAbsent(String labelIdRaw) {
		if (labelIdRaw == null) {
			return true;
		}
		String t = labelIdRaw.trim();
		return t.isEmpty() || "0".equals(t);
	}

	private static Long parseOptionalLongFilter(String levelIdRaw) {
		if (levelIdRaw == null) {
			return null;
		}
		String s = levelIdRaw.trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static OptionalLong resolveDayEpochStart(String dateDayRaw) {
		if (dateDayRaw == null) {
			return OptionalLong.empty();
		}
		String s = dateDayRaw.trim();
		if (s.isEmpty()) {
			return OptionalLong.empty();
		}
		ZoneId z = ZoneId.systemDefault();
		try {
			LocalDate d = LocalDate.parse(s);
			return OptionalLong.of(d.atStartOfDay(z).toEpochSecond());
		} catch (DateTimeParseException ignored) {
			// fall through
		}
		try {
			long sec = Long.parseLong(s);
			LocalDate d = Instant.ofEpochSecond(sec).atZone(z).toLocalDate();
			return OptionalLong.of(d.atStartOfDay(z).toEpochSecond());
		} catch (NumberFormatException e) {
			return OptionalLong.empty();
		}
	}

	/** Fixed-step Unix-second slices from {@code beginHm} until current start reaches {@code endHm}. */
	private static List<LinkedHashMap<String, Long>> computeUnixTimeSlices(
			long dayEpochStart, String beginHm, String endHm, int intervalMinutes) {
		ZoneId z = ZoneId.systemDefault();
		LocalDate date = Instant.ofEpochSecond(dayEpochStart).atZone(z).toLocalDate();
		LocalTime bt = parseFlexibleHm(beginHm);
		LocalTime et = parseFlexibleHm(endHm);
		if (bt == null || et == null) {
			return List.of();
		}
		long begin = LocalDateTime.of(date, bt).atZone(z).toEpochSecond();
		long end = LocalDateTime.of(date, et).atZone(z).toEpochSecond();
		long intervalSec = intervalMinutes * 60L;
		List<LinkedHashMap<String, Long>> result = new ArrayList<>();
		long cur = begin;
		while (true) {
			long slotBegin = cur;
			long slotEnd = cur + intervalSec;
			LinkedHashMap<String, Long> data = new LinkedHashMap<>();
			data.put("begin", slotBegin);
			data.put("end", slotEnd);
			result.add(data);
			cur = slotEnd;
			if (cur >= end) {
				break;
			}
		}
		return result;
	}

	private static String formatHm(long epochSec, ZoneId z) {
		LocalTime t = Instant.ofEpochSecond(epochSec).atZone(z).toLocalTime();
		return HH_MM.format(t);
	}

	private static LocalTime parseFlexibleHm(String raw) {
		if (raw == null) {
			return null;
		}
		String s = raw.trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return LocalTime.parse(s, HH_MM);
		} catch (DateTimeParseException ignored) {
			// fall through
		}
		try {
			return LocalTime.parse(s, H_MM);
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	private static String weekKeyEnglish(long dayEpoch, ZoneId z) {
		LocalDate d = Instant.ofEpochSecond(dayEpoch).atZone(z).toLocalDate();
		return WEEKDAY_EN.format(d).toLowerCase(Locale.ENGLISH);
	}

	private static boolean isRestShiftType(Object typeId) {
		if (typeId == null) {
			return false;
		}
		if (typeId instanceof Number n) {
			return n.longValue() == -1L;
		}
		return "-1".equals(typeId.toString().trim());
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
}
