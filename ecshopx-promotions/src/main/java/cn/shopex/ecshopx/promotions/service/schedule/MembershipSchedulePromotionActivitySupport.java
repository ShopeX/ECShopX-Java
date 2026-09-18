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

package cn.shopex.ecshopx.promotions.service.schedule;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 三种 Schedule 类活动（生日 / 周年 / 会员日）的日历触发判断与会员过滤规格解析，单入口避免分支散落。
 */
@Component
public class MembershipSchedulePromotionActivitySupport {

	/** 与 PHP 业务时区及 plan 约定一致 */
	public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

	private final PromotionScheduleMemberListPort promotionScheduleMemberListPort;
	private final Clock clock;

	public MembershipSchedulePromotionActivitySupport(
			PromotionScheduleMemberListPort promotionScheduleMemberListPort,
			@Autowired(required = false) Clock clock) {
		this.promotionScheduleMemberListPort = promotionScheduleMemberListPort;
		this.clock = clock != null ? clock : Clock.system(BUSINESS_ZONE);
	}

	public long countMembers(String activityType, Map<String, Object> activityInfoRow, long triggerTimeEpochSeconds) {
		MemberFilterSpec spec = resolveMemberFilter(activityType, activityInfoRow, triggerTimeEpochSeconds);
		long companyId = longVal(activityInfoRow.get("company_id"));
		return promotionScheduleMemberListPort.countMembers(companyId, spec);
	}

	public List<Map<String, Object>> getMembers(
			String activityType,
			Map<String, Object> activityInfoRow,
			long triggerTimeEpochSeconds,
			int pageSize,
			int page) {
		MemberFilterSpec spec = resolveMemberFilter(activityType, activityInfoRow, triggerTimeEpochSeconds);
		long companyId = longVal(activityInfoRow.get("company_id"));
		return promotionScheduleMemberListPort.getMembers(companyId, spec, pageSize, page);
	}

	/**
	 * 是否到了「可发放」的日历日（与 analysis §3 附节一致）。
	 */
	public boolean isTrigger(String activityType, Map<String, Object> activityInfoRow) {
		Object tcOb = activityInfoRow.get("trigger_condition");
		if (!(tcOb instanceof Map<?, ?> raw)) {
			return false;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> triggerCondition = (Map<String, Object>) raw;
		ZonedDateTime now = ZonedDateTime.now(clock);
		return switch (n(activityType)) {
			case "member_birthday" -> isTriggerBirthday(triggerCondition, now);
			case "member_anniversary" -> isTriggerAnniversary(triggerCondition, now);
			case "member_day" -> isTriggerMemberDay(triggerCondition, now);
			default -> false;
		};
	}

	/**
	 * 与 PHP {@code Member*::countMembers} / {@code getMembers} 使用的 filter 含义对齐。
	 */
	public MemberFilterSpec resolveMemberFilter(
			String activityType, Map<String, Object> activityInfoRow, long triggerTimeEpochSeconds) {
		Object tcOb = activityInfoRow.get("trigger_condition");
		if (!(tcOb instanceof Map<?, ?> raw)) {
			return MemberFilterSpec.all();
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> triggerCondition = (Map<String, Object>) raw;
		ZonedDateTime t =
				ZonedDateTime.ofInstant(java.time.Instant.ofEpochSecond(triggerTimeEpochSeconds), BUSINESS_ZONE);
		return switch (n(activityType)) {
			case "member_birthday" -> filterBirthday(triggerCondition, t);
			case "member_anniversary" -> filterAnniversary(triggerCondition, t);
			case "member_day" -> MemberFilterSpec.all();
			default -> MemberFilterSpec.all();
		};
	}

	/** 活动侧展示用来源串，与历史 PHP 活动对象一致。 */
	public String getSourceFromStr(String activityType) {
		return switch (n(activityType)) {
			case "member_birthday" -> "会员生日送";
			case "member_anniversary" -> "会员周年送";
			case "member_day" -> "会员日送";
			default -> "营销活动";
		};
	}

	public String getSmsTmplName(String activityType) {
		return n(activityType);
	}

	private static boolean isTriggerBirthday(Map<String, Object> triggerCondition, ZonedDateTime now) {
		String triggerTime = str(triggerCondition.get("trigger_time"));
		if (!StringUtils.hasText(triggerTime)) {
			return false;
		}
		int day = now.getDayOfMonth();
		if ("birthday_day".equals(triggerTime)) {
			return true;
		}
		if ("birthday_month".equals(triggerTime) && day == 1) {
			return true;
		}
		if ("birthday_week".equals(triggerTime)) {
			return now.getDayOfWeek() == DayOfWeek.SUNDAY;
		}
		return false;
	}

	private static boolean isTriggerAnniversary(Map<String, Object> triggerCondition, ZonedDateTime now) {
		String triggerTime = str(triggerCondition.get("trigger_time"));
		if (!StringUtils.hasText(triggerTime)) {
			return false;
		}
		int day = now.getDayOfMonth();
		if ("anniversary_day".equals(triggerTime)) {
			return true;
		}
		if ("anniversary_month".equals(triggerTime) && day == 1) {
			return true;
		}
		if ("anniversary_week".equals(triggerTime)) {
			return now.getDayOfWeek() == DayOfWeek.SUNDAY;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private static boolean isTriggerMemberDay(Map<String, Object> triggerCondition, ZonedDateTime now) {
		Object tt = triggerCondition.get("trigger_time");
		if (!(tt instanceof Map<?, ?> m)) {
			return false;
		}
		String type = str(m.get("type"));
		if (!StringUtils.hasText(type)) {
			return false;
		}
		if ("every_year".equals(type)) {
			int month = intVal(m.get("month"));
			int day = intVal(m.get("day"));
			return month == now.getMonthValue() && day == now.getDayOfMonth();
		}
		if ("every_month".equals(type)) {
			int day = intVal(m.get("day"));
			return day == now.getDayOfMonth();
		}
		if ("every_week".equals(type)) {
			int week = intVal(m.get("week"));
			return week == now.getDayOfWeek().getValue();
		}
		return false;
	}

	private static MemberFilterSpec filterBirthday(Map<String, Object> triggerCondition, ZonedDateTime t) {
		String triggerTime = str(triggerCondition.get("trigger_time"));
		int month = t.getMonthValue();
		int d = t.getDayOfMonth();
		if ("birthday_month".equals(triggerTime)) {
			return new MemberFilterSpec(MemberFilterSpecKind.BIRTHDAY_MONTH, month, 0, 0);
		}
		if ("birthday_week".equals(triggerTime)) {
			return new MemberFilterSpec(MemberFilterSpecKind.BIRTHDAY_WEEK, month, d, d + 6);
		}
		if ("birthday_day".equals(triggerTime)) {
			return new MemberFilterSpec(MemberFilterSpecKind.BIRTHDAY_DAY, month, d, d);
		}
		return MemberFilterSpec.all();
	}

	private static MemberFilterSpec filterAnniversary(Map<String, Object> triggerCondition, ZonedDateTime t) {
		String triggerTime = str(triggerCondition.get("trigger_time"));
		int month = t.getMonthValue();
		int d = t.getDayOfMonth();
		if ("anniversary_month".equals(triggerTime)) {
			return new MemberFilterSpec(MemberFilterSpecKind.ANNIVERSARY_MONTH, month, 0, 0);
		}
		if ("anniversary_week".equals(triggerTime)) {
			return new MemberFilterSpec(MemberFilterSpecKind.ANNIVERSARY_WEEK, month, d, d + 6);
		}
		if ("anniversary_day".equals(triggerTime)) {
			return new MemberFilterSpec(MemberFilterSpecKind.ANNIVERSARY_DAY, month, d, d);
		}
		return MemberFilterSpec.all();
	}

	private static String n(String activityType) {
		return activityType == null ? "" : activityType.trim();
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}

	private static int intVal(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}
}
