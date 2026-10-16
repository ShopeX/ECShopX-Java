package cn.shopex.ecshopx.promotions.service.membersignin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninActivity;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninDailyPoints;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninRecord;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninDailyPointsMapper;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninRecordMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MemberSignInCalendarServiceTest {

	@Mock private MemberSigninRecordMapper recordMapper;
	@Mock private MemberSigninActivityMapper activityMapper;
	@Mock private MemberSigninDailyPointsMapper dailyPointsMapper;
	@Mock private MemberSignInActivitySupport activitySupport;
	@Mock private cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninZoneResolver zoneResolver;
	@Mock private MessageSource messageSource;
	@Mock private MemberSignInReminderPushService reminderPushService;

	private MemberSignInCalendarService service;

	@BeforeEach
	void setUp() {
		when(messageSource.getMessage(anyString(), nullable(Object[].class), anyString(), any(Locale.class)))
				.thenAnswer(inv -> inv.getArgument(2));
		when(activitySupport.resolveDailyPointsForDate(anyLong(), any())).thenReturn(0);
		service =
				new MemberSignInCalendarService(
						recordMapper,
						activityMapper,
						dailyPointsMapper,
						activitySupport,
						zoneResolver,
						messageSource,
						reminderPushService);
	}

	@Test
	void getCalendar_marksFutureDaysUnsignedAndTodaySigned() {
		LocalDate today = LocalDate.of(2026, 9, 15);
		MemberSigninRecord todayRecord = new MemberSigninRecord();
		todayRecord.setSignDate(today);
		todayRecord.setActivitySeq(3);
		when(recordMapper.selectList(any(Wrapper.class))).thenReturn(List.of(todayRecord));
		when(activityMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

		Map<String, Object> result =
				service.getCalendar(1L, 2L, "2026-09-14", today, Locale.SIMPLIFIED_CHINESE);

		assertThat(result.get("signed_today")).isEqualTo(true);
		assertThat(result.get("week_start")).isEqualTo("2026-09-14");
		assertThat(result.get("week_end")).isEqualTo("2026-09-20");
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> days = (List<Map<String, Object>>) result.get("days");
		assertThat(days).hasSize(7);
		assertThat(days.get(1))
				.containsEntry("date", "2026-09-15")
				.containsEntry("signed", true)
				.containsEntry("activity_seq", 3)
				.containsEntry("display", "已签");
		assertThat(days.get(1).get("daily_points")).isNull();
		assertThat(days.get(2))
				.containsEntry("date", "2026-09-16")
				.containsEntry("signed", false)
				.containsEntry("daily_points", null)
				.containsEntry("display", null);
		assertThat(result.get("rule_desc")).isEqualTo("");
	}

	@Test
	void getCalendar_futureDayShowsConfiguredDailyPoints() {
		LocalDate today = LocalDate.of(2026, 9, 15);
		when(recordMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
		when(activityMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
		when(activitySupport.resolveDailyPointsForDate(1L, LocalDate.of(2026, 9, 16))).thenReturn(5);

		Map<String, Object> result =
				service.getCalendar(1L, 2L, "2026-09-14", today, Locale.SIMPLIFIED_CHINESE);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> days = (List<Map<String, Object>>) result.get("days");
		assertThat(days.get(2))
				.containsEntry("date", "2026-09-16")
				.containsEntry("signed", false)
				.containsEntry("daily_points", 5)
				.containsEntry("display", "+5");
	}

	@Test
	void getCalendar_returnsSavedRuleDesc() {
		LocalDate today = LocalDate.of(2026, 9, 15);
		MemberSigninDailyPoints config = new MemberSigninDailyPoints();
		config.setRuleDesc("<p>签到规则</p>");
		when(recordMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
		when(activityMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
		when(dailyPointsMapper.selectById(1L)).thenReturn(config);

		Map<String, Object> result =
				service.getCalendar(1L, 2L, "2026-09-14", today, Locale.SIMPLIFIED_CHINESE);

		assertThat(result.get("rule_desc")).isEqualTo("<p>签到规则</p>");
	}

	@Test
	void getCalendar_exposesCurrentInProgressActivity() {
		LocalDate today = LocalDate.of(2026, 9, 15);
		long nowSec = java.time.Instant.now().getEpochSecond();
		MemberSigninActivity activity = new MemberSigninActivity();
		activity.setId(99L);
		activity.setBeginTime(nowSec - 3600);
		activity.setEndTime(nowSec + 3600);
		when(recordMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
		when(activityMapper.selectList(any(Wrapper.class))).thenReturn(List.of(activity));

		Map<String, Object> result =
				service.getCalendar(1L, 2L, null, today, Locale.SIMPLIFIED_CHINESE);

		assertThat(result.get("current_activity_id")).isEqualTo(99L);
	}
}
