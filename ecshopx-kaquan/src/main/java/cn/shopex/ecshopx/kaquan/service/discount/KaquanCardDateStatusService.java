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

package cn.shopex.ecshopx.kaquan.service.discount;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import org.springframework.util.StringUtils;

/**
 * 与活动日期状态常量一致：0 未知、1 未开始、2 进行中、3 已结束。
 */
public final class KaquanCardDateStatusService {

	public static final int UNKNOWN = 0;
	public static final int COMING_SOON = 1;
	public static final int ON_GOING = 2;
	public static final int FINISHED = 3;

	private KaquanCardDateStatusService() {}

	public static int getDateStatus(String beginDate, String endDate) {
		if (beginDate == null || endDate == null) {
			return UNKNOWN;
		}
		String b = beginDate.trim();
		String e = endDate.trim();
		if (!StringUtils.hasText(b) || !StringUtils.hasText(e) || "0".equals(b) || "0".equals(e)) {
			return UNKNOWN;
		}
		Instant beginInstant;
		Instant endInstant;
		try {
			beginInstant = parseToInstant(b);
			endInstant = parseToInstant(e);
		} catch (DateTimeParseException | NumberFormatException | ArithmeticException ex) {
			return UNKNOWN;
		}
		Instant now = Instant.now();
		if (endInstant.isBefore(now)) {
			return FINISHED;
		}
		if (beginInstant.isAfter(now)) {
			return COMING_SOON;
		}
		return ON_GOING;
	}

	private static Instant parseToInstant(String raw) {
		if (StringUtils.hasText(raw) && raw.chars().allMatch(Character::isDigit)) {
			long sec = Long.parseLong(raw);
			return Instant.ofEpochSecond(sec);
		}
		try {
			LocalDateTime ldt = LocalDateTime.parse(raw);
			return ldt.atZone(ZoneId.systemDefault()).toInstant();
		} catch (DateTimeParseException ignored) {
		}
		try {
			LocalDate ld = LocalDate.parse(raw);
			return ld.atStartOfDay(ZoneId.systemDefault()).toInstant();
		} catch (DateTimeParseException ex) {
			throw ex;
		}
	}
}
