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

package cn.shopex.ecshopx.goods.service.hotword;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * 热词展示时间解析与展示（ECX-11390 F-003）。
 *
 * <p>存储使用 epoch 秒（绝对时间，不依赖服务器时区）；入参同时接受 epoch 秒与
 * {@code yyyy-MM-dd HH:mm:ss}/{@code yyyy-MM-dd HH:mm}/{@code yyyy-MM-dd} 本地时间串，
 * 本地时间按 {@link #ZONE} 解释。
 */
public final class SearchHotwordTime {

	/** 展示与本地时间解析使用的时区。 */
	public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

	private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static final List<String> DATE_CODES = List.of("validation.date");

	private SearchHotwordTime() {}

	/** 解析时间入参为 epoch 秒；空值返回 {@code null}，非法值抛 422。 */
	public static Integer parse(Object raw, String field) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number number) {
			return toEpochSecond(number.longValue(), field);
		}
		String text = raw.toString().trim();
		if (text.isEmpty()) {
			return null;
		}
		if (text.chars().allMatch(Character::isDigit)) {
			try {
				return toEpochSecond(Long.parseLong(text), field);
			} catch (NumberFormatException e) {
				throw invalid(field);
			}
		}
		String normalized = text.replace('T', ' ');
		if (normalized.length() == 10) {
			normalized = normalized + " 00:00:00";
		} else if (normalized.length() == 16) {
			normalized = normalized + ":00";
		}
		try {
			LocalDateTime local = LocalDateTime.parse(normalized, DISPLAY);
			return toEpochSecond(local.atZone(ZONE).toEpochSecond(), field);
		} catch (DateTimeParseException e) {
			throw invalid(field);
		}
	}

	/** epoch 秒 → {@code yyyy-MM-dd HH:mm:ss}（{@link #ZONE}）；空值返回 {@code null}。 */
	public static String format(Integer epochSecond) {
		if (epochSecond == null) {
			return null;
		}
		return DISPLAY.format(LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSecond.longValue()), ZONE));
	}

	private static Integer toEpochSecond(long value, String field) {
		if (value < 0L || value > Integer.MAX_VALUE) {
			throw invalid(field);
		}
		return Integer.valueOf((int) value);
	}

	private static BadRequestException invalid(String field) {
		return new BadRequestException(field + " 不是有效的时间", Map.of(field, DATE_CODES), 422);
	}
}
