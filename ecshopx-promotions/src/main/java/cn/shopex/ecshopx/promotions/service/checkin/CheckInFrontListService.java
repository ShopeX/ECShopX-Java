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

package cn.shopex.ecshopx.promotions.service.checkin;

import cn.shopex.ecshopx.promotions.domain.CheckInLog;
import cn.shopex.ecshopx.promotions.mapper.CheckInLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CheckInFrontListService {

	private static final ZoneId CHECKIN_ZONE = ZoneId.of("Asia/Shanghai");

	private final CheckInLogMapper checkInLogMapper;

	public CheckInFrontListService(CheckInLogMapper checkInLogMapper) {
		this.checkInLogMapper = checkInLogMapper;
	}

	public Map<String, Object> getCheckInList(
			long companyId, long userId, String checkType, String startDate, String endDate) {
		String ct = !StringUtils.hasText(checkType) ? "month" : checkType.trim();

		LocalDate startLocal;
		LocalDate endLocal;
		if (ct.equalsIgnoreCase("month")) {
			if (!StringUtils.hasText(startDate)) {
				startLocal = YearMonth.now(CHECKIN_ZONE).atDay(1);
			} else {
				startLocal = parseQueryDateToLocalDateOrEpoch(startDate);
			}
			if (!StringUtils.hasText(endDate)) {
				endLocal = startLocal.plusMonths(1).minusDays(1);
			} else {
				endLocal = parseQueryDateToLocalDateOrEpoch(endDate);
			}
		} else if (ct.equalsIgnoreCase("week")) {
			if (!StringUtils.hasText(startDate)) {
				startLocal = LocalDate.now(CHECKIN_ZONE).with(WeekFields.of(Locale.CHINA).dayOfWeek(), 1L);
			} else {
				startLocal = parseQueryDateToLocalDateOrEpoch(startDate);
			}
			if (!StringUtils.hasText(endDate)) {
				endLocal = LocalDate.now(CHECKIN_ZONE).with(WeekFields.of(Locale.CHINA).dayOfWeek(), 7L);
			} else {
				endLocal = parseQueryDateToLocalDateOrEpoch(endDate);
			}
		} else {
			if (!StringUtils.hasText(startDate)) {
				startLocal = LocalDate.now(CHECKIN_ZONE);
			} else {
				startLocal = parseQueryDateToLocalDateOrEpoch(startDate);
			}
			if (!StringUtils.hasText(endDate)) {
				endLocal = startLocal;
			} else {
				endLocal = parseQueryDateToLocalDateOrEpoch(endDate);
			}
		}

		int startYmd = yyyyMmddInt(startLocal);
		int endYmd = yyyyMmddInt(endLocal);

		LambdaQueryWrapper<CheckInLog> w = new LambdaQueryWrapper<CheckInLog>()
				.eq(CheckInLog::getCompanyId, companyId)
				.eq(CheckInLog::getUserId, userId)
				.ge(CheckInLog::getCreateTime, startYmd)
				.le(CheckInLog::getCreateTime, endYmd);
		Long total = checkInLogMapper.selectCount(w);

		Page<CheckInLog> pg = new Page<>(1, 100, false);
		if (total != null && total > 0) {
			checkInLogMapper.selectPage(pg, w);
		}

		List<Map<String, Object>> list = new ArrayList<>();
		for (CheckInLog e : pg.getRecords()) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("id", e.getId());
			row.put("company_id", e.getCompanyId());
			row.put("user_id", e.getUserId());
			row.put("create_time", e.getCreateTime());
			row.put("tag", e.getTag());
			row.put("created", e.getCreated());
			list.add(row);
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", total == null ? 0 : total.intValue());
		result.put("list", list);
		return result;
	}

	private static LocalDate parseQueryDateToLocalDateOrEpoch(String raw) {
		String t = raw.trim();
		LocalDate resolved = null;
		if (t.matches("^\\d{8}$")) {
			try {
				int y = Integer.parseInt(t.substring(0, 4));
				int m = Integer.parseInt(t.substring(4, 6));
				int d = Integer.parseInt(t.substring(6, 8));
				resolved = LocalDate.of(y, m, d);
			} catch (NumberFormatException | DateTimeException e) {
				resolved = null;
			}
		}
		if (resolved == null && t.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
			try {
				resolved = LocalDate.parse(t, DateTimeFormatter.ISO_LOCAL_DATE);
			} catch (DateTimeParseException e) {
				resolved = null;
			}
		}
		if (resolved == null && t.matches("^\\d{10}$")) {
			try {
				long sec = Long.parseLong(t);
				resolved = LocalDate.ofInstant(Instant.ofEpochSecond(sec), CHECKIN_ZONE);
			} catch (DateTimeException | NumberFormatException e) {
				resolved = null;
			}
		}
		if (resolved == null) {
			resolved = LocalDate.ofInstant(Instant.EPOCH, CHECKIN_ZONE);
		}
		return resolved;
	}

	private static int yyyyMmddInt(LocalDate d) {
		return d.getYear() * 10_000 + d.getMonthValue() * 100 + d.getDayOfMonth();
	}
}
