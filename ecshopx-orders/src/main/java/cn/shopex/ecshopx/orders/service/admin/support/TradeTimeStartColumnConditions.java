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

package cn.shopex.ecshopx.orders.service.admin.support;

import cn.shopex.ecshopx.orders.domain.Trade;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.springframework.util.StringUtils;

/**
 * Builds {@code time_start} column predicates so date-only bounds ({@code yyyy-MM-dd}) match rows whose
 * stored value is a short numeric string (legacy unix seconds) as well as ISO-like datetime strings.
 */
public final class TradeTimeStartColumnConditions {

	private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

	private TradeTimeStartColumnConditions() {}

	public static void apply(LambdaQueryWrapper<Trade> w, String timeStartBegin, String timeStartEnd) {
		if (!StringUtils.hasText(timeStartBegin)) {
			return;
		}
		String tb = timeStartBegin.trim();
		String te = StringUtils.hasText(timeStartEnd) ? timeStartEnd.trim() : null;
		if (isIsoDateOnly(tb) && (te == null || isIsoDateOnly(te))) {
			long beginEpoch;
			try {
				beginEpoch = LocalDate.parse(tb, ISO_DATE).atStartOfDay(ZONE).toEpochSecond();
			} catch (DateTimeParseException e) {
				applyLexicographic(w, tb, te);
				return;
			}
			Long endEpochInclusive = null;
			if (te != null) {
				try {
					endEpochInclusive =
							LocalDate.parse(te, ISO_DATE).atTime(23, 59, 59).atZone(ZONE).toEpochSecond();
				} catch (DateTimeParseException e) {
					applyLexicographic(w, tb, te);
					return;
				}
			}
			final long b = beginEpoch;
			final Long ein = endEpochInclusive;
			w.and(sub -> {
				sub.nested(n -> {
					n.apply("TRIM(time_start) REGEXP {0}", "^[0-9]+$")
							.apply("CAST(TRIM(time_start) AS UNSIGNED) >= {0}", b);
					if (ein != null) {
						n.apply("CAST(TRIM(time_start) AS UNSIGNED) <= {0}", ein);
					}
				});
				sub.or()
						.nested(n -> {
							n.apply("TRIM(time_start) NOT REGEXP {0}", "^[0-9]+$");
							n.ge(Trade::getTimeStart, tb);
							if (te != null) {
								n.le(Trade::getTimeStart, te);
							}
						});
			});
			return;
		}
		applyLexicographic(w, tb, te);
	}

	private static void applyLexicographic(
			LambdaQueryWrapper<Trade> w, String timeStartBegin, String timeStartEnd) {
		w.ge(Trade::getTimeStart, timeStartBegin);
		if (StringUtils.hasText(timeStartEnd)) {
			w.le(Trade::getTimeStart, timeStartEnd);
		}
	}

	private static boolean isIsoDateOnly(String s) {
		return s.length() == 10 && s.charAt(4) == '-' && s.charAt(7) == '-';
	}
}
