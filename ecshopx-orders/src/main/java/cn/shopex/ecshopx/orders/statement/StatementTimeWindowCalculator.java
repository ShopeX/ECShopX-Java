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

package cn.shopex.ecshopx.orders.statement;

import java.time.ZoneId;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * 与 {@code GenerateStatementsJob} / {@code StatementsService} 中周期窗口判定一致的日期计算，时区 {@link
 * #SHANGHAI} 对齐常见 PHP 应用时区设置。
 */
public final class StatementTimeWindowCalculator {

	public static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

	private StatementTimeWindowCalculator() {}

	/** PHP: date('w')；周日=0。 */
	public static int phpW(long epochSec) {
		int d = zdt(epochSec).getDayOfWeek().getValue();
		return d == 7 ? 0 : d;
	}

	/** 调度段：endTime 判定（日/周/月），与 {@code doStatementsForDistributor} 中 switch 同构。 */
	public static long computeScheduleWindowEndForLastLine(long lastEnd, int n, String unit) {
		ZonedDateTime z = zdt(lastEnd);
		return switch (unit) {
			case "day" -> z.plusDays(n).toEpochSecond();
			case "week" -> {
				int w = phpW(lastEnd);
				if (z.plusDays(7 - w).toEpochSecond() == lastEnd) {
					yield z.plusDays(n * 7L + 7L - w).toEpochSecond();
				}
				yield z.plusDays(n * 7L - w).toEpochSecond();
			}
			case "month" -> {
				long monthEndOfCurrent =
						z.with(TemporalAdjusters.firstDayOfMonth())
										.plusMonths(1)
										.toLocalDate()
										.atStartOfDay(SHANGHAI)
										.toEpochSecond()
						- 1;
				if (monthEndOfCurrent == lastEnd) {
					yield z.with(TemporalAdjusters.firstDayOfMonth())
							.plusMonths(n + 1L)
							.toLocalDate()
							.atStartOfDay(SHANGHAI)
							.toEpochSecond()
							- 1;
				}
				yield z.with(TemporalAdjusters.firstDayOfMonth())
						.plusMonths(n)
						.toLocalDate()
						.atStartOfDay(SHANGHAI)
						.toEpochSecond()
						- 1;
			}
			default -> 0L;
		};
	}

	public static long jobFirstEndDay(long lastEnd, int n) {
		return zdt(lastEnd).plusDays(n).toEpochSecond();
	}

	public static long jobFirstEndWeek(long lastEnd, int n) {
		ZonedDateTime z = zdt(lastEnd);
		int w = phpW(lastEnd);
		if (z.plusDays(7 - w).toEpochSecond() == lastEnd) {
			return z.plusDays(n * 7L + 7L - w).toEpochSecond();
		}
		return z.plusDays(n * 7L - w).toEpochSecond();
	}

	public static long jobFirstEndMonth(long lastEnd, int n) {
		ZonedDateTime l = zdt(lastEnd);
		long monthEndNext =
				l.with(TemporalAdjusters.firstDayOfMonth())
						.plusMonths(1)
						.toLocalDate()
						.atStartOfDay(SHANGHAI)
						.toEpochSecond()
				- 1;
		if (monthEndNext == lastEnd) {
			return l.with(TemporalAdjusters.firstDayOfMonth())
					.plusMonths(n + 1L)
					.toLocalDate()
					.atStartOfDay(SHANGHAI)
					.toEpochSecond()
					- 1;
		}
		return l.with(TemporalAdjusters.firstDayOfMonth())
				.plusMonths(n)
				.toLocalDate()
				.atStartOfDay(SHANGHAI)
				.toEpochSecond()
				- 1;
	}

	public static long jobAdvanceEndDay(long endTime, int n) {
		return zdt(endTime).plusDays(n).toEpochSecond();
	}

	public static long jobAdvanceEndWeek(long endTime, int n) {
		return zdt(endTime).plusWeeks(n).toEpochSecond();
	}

	public static long jobAdvanceEndMonth(long endTime, int n) {
		return zdt(endTime)
						.with(TemporalAdjusters.firstDayOfMonth())
						.plusMonths(n + 1L)
						.toLocalDate()
						.atStartOfDay(SHANGHAI)
						.toEpochSecond()
				- 1;
	}

	/**
	 * 供应商侧 Redis 无游标时的 lastEnd 反推：day/week 为「本日 0 点」回溯；month 为 PHP 的 0 点 {@code
	 * Y-m-01} 起点回溯。
	 */
	public static long supplierInferredLastEndIfRedisMissing(StatementPeriodValue p, long nowSec) {
		ZonedDateTime now = zdt(nowSec);
		return switch (p.unit()) {
			case "day" -> {
				long st =
						now.toLocalDate()
								.minusDays(p.n())
								.atStartOfDay(SHANGHAI)
								.toEpochSecond();
				yield st;
			}
			case "week" -> {
				yield now.toLocalDate()
						.minusDays(7L * p.n())
						.atStartOfDay(SHANGHAI)
						.toEpochSecond();
			}
			case "month" -> {
				yield zdt(0L)
						.with(TemporalAdjusters.firstDayOfMonth())
						.minusMonths(p.n())
						.toLocalDate()
						.atStartOfDay(SHANGHAI)
						.toEpochSecond()
						- 1;
			}
			default -> 0L;
		};
	}

	private static ZonedDateTime zdt(long sec) {
		return ZonedDateTime.ofInstant(Instant.ofEpochSecond(sec), SHANGHAI);
	}
}
