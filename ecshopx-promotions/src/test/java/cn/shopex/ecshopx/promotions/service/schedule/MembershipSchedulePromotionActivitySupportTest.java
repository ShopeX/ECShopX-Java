package cn.shopex.ecshopx.promotions.service.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MembershipSchedulePromotionActivitySupportTest {

	private MembershipSchedulePromotionActivitySupport newSupport(Clock clock) {
		return new MembershipSchedulePromotionActivitySupport(mock(PromotionScheduleMemberListPort.class), clock);
	}

	@Test
	@DisplayName("analysis §3 附节：MemberBirthday birthday_month 且当月 1 日触发")
	void isTrigger_birthdayMonth_firstDayOfMonth() {
		ZonedDateTime z = LocalDate.of(2024, 6, 1).atStartOfDay(MembershipSchedulePromotionActivitySupport.BUSINESS_ZONE);
		Clock clock = Clock.fixed(z.toInstant(), MembershipSchedulePromotionActivitySupport.BUSINESS_ZONE);
		var s = newSupport(clock);
		Map<String, Object> row = rowBirthday("birthday_month");
		assertThat(s.isTrigger("member_birthday", row)).isTrue();
	}

	@Test
	@DisplayName("analysis §3 附节：MemberBirthday birthday_month 非 1 日不触发")
	void isTrigger_birthdayMonth_notFirstDay() {
		ZonedDateTime z = LocalDate.of(2024, 6, 2).atStartOfDay(MembershipSchedulePromotionActivitySupport.BUSINESS_ZONE);
		Clock clock = Clock.fixed(z.toInstant(), MembershipSchedulePromotionActivitySupport.BUSINESS_ZONE);
		var s = newSupport(clock);
		Map<String, Object> row = rowBirthday("birthday_month");
		assertThat(s.isTrigger("member_birthday", row)).isFalse();
	}

	@Test
	@DisplayName("analysis §3 附节：MemberBirthday birthday_week 仅周日触发")
	void isTrigger_birthdayWeek_sunday() {
		// 2024-06-02 为周日 (Asia/Shanghai)
		ZonedDateTime z = LocalDate.of(2024, 6, 2).atStartOfDay(MembershipSchedulePromotionActivitySupport.BUSINESS_ZONE);
		Clock clock = Clock.fixed(z.toInstant(), MembershipSchedulePromotionActivitySupport.BUSINESS_ZONE);
		var s = newSupport(clock);
		Map<String, Object> row = rowBirthday("birthday_week");
		assertThat(s.isTrigger("member_birthday", row)).isTrue();
	}

	@Test
	@DisplayName("plan §5 附节：MemberBirthday birthday_week 非周日不触发")
	void isTrigger_birthdayWeek_notSunday() {
		ZonedDateTime z = LocalDate.of(2024, 6, 3).atStartOfDay(MembershipSchedulePromotionActivitySupport.BUSINESS_ZONE);
		Clock clock = Clock.fixed(z.toInstant(), MembershipSchedulePromotionActivitySupport.BUSINESS_ZONE);
		var s = newSupport(clock);
		Map<String, Object> row = rowBirthday("birthday_week");
		assertThat(s.isTrigger("member_birthday", row)).isFalse();
	}

	@Test
	@DisplayName("plan §5 附节：MemberBirthday birthday_day 恒真")
	void isTrigger_birthdayDay_alwaysTrue() {
		ZonedDateTime z = LocalDate.of(2024, 6, 15).atStartOfDay(MembershipSchedulePromotionActivitySupport.BUSINESS_ZONE);
		Clock clock = Clock.fixed(z.toInstant(), MembershipSchedulePromotionActivitySupport.BUSINESS_ZONE);
		var s = newSupport(clock);
		Map<String, Object> row = rowBirthday("birthday_day");
		assertThat(s.isTrigger("member_birthday", row)).isTrue();
	}

	@Test
	@DisplayName("plan §5 附节：MemberAnniversary anniversary_day 恒真")
	void isTrigger_anniversaryDay_alwaysTrue() {
		ZonedDateTime z = LocalDate.of(2024, 3, 20).atStartOfDay(MembershipSchedulePromotionActivitySupport.BUSINESS_ZONE);
		Clock clock = Clock.fixed(z.toInstant(), MembershipSchedulePromotionActivitySupport.BUSINESS_ZONE);
		var s = newSupport(clock);
		Map<String, Object> row = rowAnniversary("anniversary_day");
		assertThat(s.isTrigger("member_anniversary", row)).isTrue();
	}

	@Test
	@DisplayName("plan §5 附节：MemberAnniversary anniversary_month 仅每月 1 日触发")
	void isTrigger_anniversaryMonth_firstDayOnly() {
		ZonedDateTime z1 = LocalDate.of(2024, 7, 1).atStartOfDay(MembershipSchedulePromotionActivitySupport.BUSINESS_ZONE);
		assertThat(newSupport(Clock.fixed(z1.toInstant(), MembershipSchedulePromotionActivitySupport.BUSINESS_ZONE))
						.isTrigger("member_anniversary", rowAnniversary("anniversary_month")))
				.isTrue();
		ZonedDateTime z2 = LocalDate.of(2024, 7, 2).atStartOfDay(MembershipSchedulePromotionActivitySupport.BUSINESS_ZONE);
		assertThat(newSupport(Clock.fixed(z2.toInstant(), MembershipSchedulePromotionActivitySupport.BUSINESS_ZONE))
						.isTrigger("member_anniversary", rowAnniversary("anniversary_month")))
				.isFalse();
	}

	@Test
	@DisplayName("plan §5 附节：MemberAnniversary anniversary_week 仅周日触发")
	void isTrigger_anniversaryWeek_sundayNotMonday() {
		ZonedDateTime sun = LocalDate.of(2024, 6, 9).atStartOfDay(MembershipSchedulePromotionActivitySupport.BUSINESS_ZONE);
		assertThat(newSupport(Clock.fixed(sun.toInstant(), MembershipSchedulePromotionActivitySupport.BUSINESS_ZONE))
						.isTrigger("member_anniversary", rowAnniversary("anniversary_week")))
				.isTrue();
		ZonedDateTime mon = LocalDate.of(2024, 6, 10).atStartOfDay(MembershipSchedulePromotionActivitySupport.BUSINESS_ZONE);
		assertThat(newSupport(Clock.fixed(mon.toInstant(), MembershipSchedulePromotionActivitySupport.BUSINESS_ZONE))
						.isTrigger("member_anniversary", rowAnniversary("anniversary_week")))
				.isFalse();
	}

	@Test
	@DisplayName("plan §5 附节：MemberDay every_year 月日匹配")
	void isTrigger_memberDay_everyYear_match() {
		ZonedDateTime z = LocalDate.of(2024, 8, 8).atStartOfDay(MembershipSchedulePromotionActivitySupport.BUSINESS_ZONE);
		Clock clock = Clock.fixed(z.toInstant(), MembershipSchedulePromotionActivitySupport.BUSINESS_ZONE);
		var s = newSupport(clock);
		Map<String, Object> row = rowMemberDay("every_year", 8, 8);
		assertThat(s.isTrigger("member_day", row)).isTrue();
	}

	@Test
	@DisplayName("plan §5 附节：MemberDay every_year 月日不匹配不触发")
	void isTrigger_memberDay_everyYear_noMatch() {
		ZonedDateTime z = LocalDate.of(2024, 8, 8).atStartOfDay(MembershipSchedulePromotionActivitySupport.BUSINESS_ZONE);
		Clock clock = Clock.fixed(z.toInstant(), MembershipSchedulePromotionActivitySupport.BUSINESS_ZONE);
		var s = newSupport(clock);
		Map<String, Object> row = rowMemberDay("every_year", 8, 9);
		assertThat(s.isTrigger("member_day", row)).isFalse();
	}

	@Test
	@DisplayName("plan §5 附节：MemberDay every_month 仅日匹配")
	void isTrigger_memberDay_everyMonth() {
		ZonedDateTime z = LocalDate.of(2024, 5, 18).atStartOfDay(MembershipSchedulePromotionActivitySupport.BUSINESS_ZONE);
		Clock clock = Clock.fixed(z.toInstant(), MembershipSchedulePromotionActivitySupport.BUSINESS_ZONE);
		var s = newSupport(clock);
		Map<String, Object> row = rowMemberDayEveryMonth(18);
		assertThat(s.isTrigger("member_day", row)).isTrue();
	}

	@Test
	@DisplayName("plan §5 附节：MemberDay every_month 日不匹配不触发")
	void isTrigger_memberDay_everyMonth_noMatch() {
		ZonedDateTime z = LocalDate.of(2024, 5, 18).atStartOfDay(MembershipSchedulePromotionActivitySupport.BUSINESS_ZONE);
		Clock clock = Clock.fixed(z.toInstant(), MembershipSchedulePromotionActivitySupport.BUSINESS_ZONE);
		var s = newSupport(clock);
		Map<String, Object> row = rowMemberDayEveryMonth(19);
		assertThat(s.isTrigger("member_day", row)).isFalse();
	}

	@Test
	@DisplayName("MemberDay every_week 与 date('N') 对齐")
	void isTrigger_memberDay_everyWeek() {
		// 2024-06-03 周一 N=1
		ZonedDateTime z = LocalDate.of(2024, 6, 3).atStartOfDay(MembershipSchedulePromotionActivitySupport.BUSINESS_ZONE);
		Clock clock = Clock.fixed(z.toInstant(), MembershipSchedulePromotionActivitySupport.BUSINESS_ZONE);
		var s = newSupport(clock);
		Map<String, Object> tc = new LinkedHashMap<>();
		Map<String, Object> tt = new LinkedHashMap<>();
		tt.put("type", "every_week");
		tt.put("week", 1);
		tc.put("trigger_time", tt);
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("trigger_condition", tc);
		assertThat(s.isTrigger("member_day", row)).isTrue();
	}

	private static Map<String, Object> rowBirthday(String triggerTime) {
		Map<String, Object> tc = new LinkedHashMap<>();
		tc.put("trigger_time", triggerTime);
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("trigger_condition", tc);
		return row;
	}

	private static Map<String, Object> rowAnniversary(String triggerTime) {
		Map<String, Object> tc = new LinkedHashMap<>();
		tc.put("trigger_time", triggerTime);
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("trigger_condition", tc);
		return row;
	}

	private static Map<String, Object> rowMemberDay(String type, int month, int day) {
		Map<String, Object> tt = new LinkedHashMap<>();
		tt.put("type", type);
		tt.put("month", month);
		tt.put("day", day);
		Map<String, Object> tc = new LinkedHashMap<>();
		tc.put("trigger_time", tt);
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("trigger_condition", tc);
		return row;
	}

	private static Map<String, Object> rowMemberDayEveryMonth(int day) {
		Map<String, Object> tt = new LinkedHashMap<>();
		tt.put("type", "every_month");
		tt.put("day", day);
		Map<String, Object> tc = new LinkedHashMap<>();
		tc.put("trigger_time", tt);
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("trigger_condition", tc);
		return row;
	}
}
