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

package cn.shopex.ecshopx.common.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析多种格式的日期/时间表达式为 epoch seconds。
 */
public final class DateExpressionParser {

    private static final Pattern RELATIVE_OFFSET = Pattern.compile(
        "([+-]?\\d+)\\s*(sec(?:ond)?s?|min(?:ute)?s?|hours?|days?|weeks?|months?|years?)",
        Pattern.CASE_INSENSITIVE
    );
    private static final DateTimeFormatter[] FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
    };

    private DateExpressionParser() {}

    public static Long parseToEpochSecond(Object raw, ZoneId zone) {
        if (raw == null) return null;
        if (raw instanceof Number n) return n.longValue();
        String s = raw.toString().trim();
        if (s.isEmpty()) return null;
        if (s.startsWith("@")) {
            try { return Long.parseLong(s.substring(1)); }
            catch (NumberFormatException e) { return null; }
        }
        Long kw = resolveKeywordOrRelative(s, zone);
        if (kw != null) return kw;
        for (DateTimeFormatter fmt : FORMATTERS) {
            try {
                if (fmt.toString().contains("HourOfDay")) {
                    LocalDateTime ldt = LocalDateTime.parse(s, fmt);
                    return ldt.atZone(zone).toEpochSecond();
                } else {
                    LocalDate ld = LocalDate.parse(s, fmt);
                    return ld.atStartOfDay(zone).toEpochSecond();
                }
            } catch (DateTimeParseException ignored) {}
        }
        Long iso = parseIsoOffsetOrInstant(s);
        if (iso != null) return iso;
        try { return Long.parseLong(s); }
        catch (NumberFormatException e) { return null; }
    }

    private static Long parseIsoOffsetOrInstant(String s) {
        try {
            return Instant.parse(s).getEpochSecond();
        } catch (DateTimeParseException ignored) {}
        try {
            return OffsetDateTime.parse(s).toEpochSecond();
        } catch (DateTimeParseException ignored) {}
        try {
            return ZonedDateTime.parse(s).toEpochSecond();
        } catch (DateTimeParseException ignored) {}
        return null;
    }

    static Long resolveKeywordOrRelative(String s, ZoneId zone) {
        ZonedDateTime now = ZonedDateTime.now(zone);
        return switch (s.toLowerCase()) {
            case "now" -> now.toEpochSecond();
            case "today", "midnight" -> now.toLocalDate().atStartOfDay(zone).toEpochSecond();
            case "tomorrow" -> now.toLocalDate().plusDays(1).atStartOfDay(zone).toEpochSecond();
            case "yesterday" -> now.toLocalDate().minusDays(1).atStartOfDay(zone).toEpochSecond();
            default -> {
                Matcher m = RELATIVE_OFFSET.matcher(s);
                if (!m.find()) yield null;
                long amount = Long.parseLong(m.group(1));
                String unit = m.group(2).toLowerCase();
                ZonedDateTime result = switch (unit.replaceAll("s$", "")) {
                    case "sec", "second" -> now.plusSeconds(amount);
                    case "min", "minute" -> now.plusMinutes(amount);
                    case "hour" -> now.plusHours(amount);
                    case "day" -> now.plusDays(amount);
                    case "week" -> now.plusWeeks(amount);
                    case "month" -> now.plusMonths(amount);
                    case "year" -> now.plusYears(amount);
                    default -> now;
                };
                yield result.toEpochSecond();
            }
        };
    }
}
