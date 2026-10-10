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

package cn.shopex.ecshopx.promotions.domain.membertask;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;

/** 当前周期键（SSOT §3.5.1 · Asia/Shanghai）。 */
public final class MemberTaskCycleKey {

	private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZONE);
	private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM").withZone(ZONE);

	private MemberTaskCycleKey() {}

	public static String current(MemberTask task, long nowEpochSec) {
		if (task == null || !"cycle".equals(task.getFrequencyType())) {
			return "once";
		}
		String period = task.getFrequencyPeriod();
		Instant instant = Instant.ofEpochSecond(nowEpochSec);
		if ("weekly".equals(period)) {
			var local = instant.atZone(ZONE).toLocalDate();
			int week = local.get(WeekFields.ISO.weekOfWeekBasedYear());
			int year = local.get(WeekFields.ISO.weekBasedYear());
			return String.format("%04d-W%02d", year, week);
		}
		if ("monthly".equals(period)) {
			return MONTH.format(instant);
		}
		return DAY.format(instant);
	}
}
