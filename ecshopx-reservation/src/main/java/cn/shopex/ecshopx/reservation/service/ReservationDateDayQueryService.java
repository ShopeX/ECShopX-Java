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

import cn.shopex.ecshopx.reservation.domain.ReservationSetting;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ReservationDateDayQueryService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

	private static final Map<String, String> WEEK_DAY_ZH = Map.ofEntries(
			Map.entry("Mon", "周一"),
			Map.entry("Tue", "周二"),
			Map.entry("Wed", "周三"),
			Map.entry("Thu", "周四"),
			Map.entry("Fri", "周五"),
			Map.entry("Sat", "周六"),
			Map.entry("Sun", "周日"));

	private static final DateTimeFormatter WEEK_EN =
			DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);

	private static final DateTimeFormatter DATE_DAY =
			DateTimeFormatter.ofPattern("MM月dd日");

	private static final DateTimeFormatter DATE_YMD =
			DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private final ReservationSettingQueryService reservationSettingQueryService;

	public ReservationDateDayQueryService(ReservationSettingQueryService reservationSettingQueryService) {
		this.reservationSettingQueryService = reservationSettingQueryService;
	}

	public List<Map<String, Object>> buildDateDayRows(long companyId, String endDateQueryRaw) {
		Optional<ReservationSetting> opt = reservationSettingQueryService.findByCompanyId(companyId);
		if (opt.isEmpty()) {
			return Collections.emptyList();
		}
		ReservationSetting row = opt.get();
		Integer maxLimitDayObj = row.getMaxLimitDay();
		double maxLimitDay = maxLimitDayObj == null ? 0.0 : maxLimitDayObj.doubleValue();

		long oneDay = 86400L;
		long nowDate = Instant.now().getEpochSecond();

		if (StringUtils.hasText(endDateQueryRaw)) {
			String trimmed = endDateQueryRaw.trim();
			try {
				long endEpoch = Long.parseLong(trimmed);
				if (endEpoch > nowDate) {
					double endDay = (endEpoch - nowDate) / (double) oneDay;
					maxLimitDay = Math.min(maxLimitDay, endDay);
				}
			} catch (NumberFormatException ignored) {
				// unparsable endDate: no cap applied
			}
		}

		List<Long> stamps = new ArrayList<>();
		for (int i = 0; (double) i < maxLimitDay; i++) {
			stamps.add(nowDate + (long) i * oneDay);
		}

		List<Map<String, Object>> result = new ArrayList<>(stamps.size());
		for (Long value : stamps) {
			ZonedDateTime zdt = Instant.ofEpochSecond(value).atZone(SHANGHAI);
			String weekAbbr = zdt.format(WEEK_EN);
			String dateWeek = WEEK_DAY_ZH.getOrDefault(weekAbbr, weekAbbr);

			LinkedHashMap<String, Object> line = new LinkedHashMap<>(4);
			line.put("date_week", dateWeek);
			line.put("date_day", zdt.format(DATE_DAY));
			line.put("timestamp", value.longValue());
			line.put("date", zdt.format(DATE_YMD));
			result.add(line);
		}
		return result;
	}
}
