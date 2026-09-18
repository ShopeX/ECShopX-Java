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

import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 按指定公历年，在系统默认时区下生成 52 个自然周（周一至周日）的每日 0 点起止纪元秒；
 * 周界与既有「1 月 1 日起算、向前对齐到周一」的算法一致。
 */
@Service
public class ReservationDateWeeksCalendarService {

	private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

	/**
	 * 与 {@link #getWeeksYearBoundaries} 使用相同的空值、非法年与回退规则，返回实际参与周界计算的公历年。
	 */
	public int resolveEffectiveCalendarYear(String effectiveYear) {
		ZoneId zone = ZoneId.systemDefault();
		return resolveYearJanuaryFirst(effectiveYear, zone).getYear();
	}

	public Map<Integer, WeekRange> getWeeksYearBoundaries(String effectiveYear) {
		ZoneId zone = ZoneId.systemDefault();
		LocalDate jan1 = resolveYearJanuaryFirst(effectiveYear, zone);
		long startEpoch = jan1.atStartOfDay(zone).toEpochSecond();
		int isoDow = jan1.getDayOfWeek().getValue();
		// ISO 星期：周一=1 … 周日=7；换算为「周日记为 0、周一=1 … 周六=6」的星期索引以回退到当周周一 0 点
		if (isoDow != 1) {
			int w = (isoDow == 7) ? 0 : isoDow;
			startEpoch = startEpoch - (w - 1L) * 86400L;
		}
		LocalDate mondayFirstWeek = Instant.ofEpochSecond(startEpoch).atZone(zone).toLocalDate();

		LinkedHashMap<Integer, WeekRange> weekArray = new LinkedHashMap<>();
		for (int i = 1; i <= 52; i++) {
			int j = i - 1;
			LocalDate weekStart = mondayFirstWeek.plusWeeks(j);
			long begin = weekStart.atStartOfDay(zone).toEpochSecond();
			LocalDate weekEndDay = weekStart.plusDays(6);
			long end = weekEndDay.atStartOfDay(zone).toEpochSecond();
			weekArray.put(i, new WeekRange(begin, end));
		}
		return weekArray;
	}

	private static LocalDate resolveYearJanuaryFirst(String effectiveYear, ZoneId zone) {
		String y = effectiveYear;
		if (y == null || y.isEmpty()) {
			y = String.valueOf(Year.now(zone).getValue());
		}
		String yearStartStr = y + "-01-01";
		try {
			return LocalDate.parse(yearStartStr, ISO_DATE);
		} catch (DateTimeParseException ex) {
			return LocalDate.now(zone).withMonth(1).withDayOfMonth(1);
		}
	}
}
