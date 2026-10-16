package cn.shopex.ecshopx.promotions.service.membersignin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninZoneResolver;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninActivityProgressMapper;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninActivityTierMapper;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninRecordMapper;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninRewardGrantMapper;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninRecord;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneId;
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
class MemberSignInQueryServiceRecordsTest {

	@Mock private MemberSignInActivitySupport activitySupport;
	@Mock private MemberSigninActivityProgressMapper progressMapper;
	@Mock private MemberSigninActivityTierMapper tierMapper;
	@Mock private MemberSigninRewardGrantMapper grantMapper;
	@Mock private MemberSigninRecordMapper recordMapper;
	@Mock private MemberSigninActivityMapper activityMapper;
	@Mock private MemberSignInRewardService rewardService;
	@Mock private MessageSource messageSource;
	@Mock private MemberSigninZoneResolver zoneResolver;

	private MemberSignInQueryService service;

	@BeforeEach
	void setUp() {
		when(messageSource.getMessage(anyString(), nullable(Object[].class), anyString(), any(Locale.class)))
				.thenAnswer(inv -> inv.getArgument(2));
		service =
				new MemberSignInQueryService(
						activitySupport,
						zoneResolver,
						progressMapper,
						tierMapper,
						grantMapper,
						recordMapper,
						activityMapper,
						rewardService,
						messageSource,
						new ObjectMapper());
		when(activitySupport.findInProgressActivity(anyLong())).thenReturn(null);
		when(zoneResolver.resolveZone(anyLong())).thenReturn(ZoneId.of("Asia/Shanghai"));
		when(activitySupport.resolveDailyPointsForDate(anyLong(), any())).thenReturn(0);
	}

	@Test
	void getMonthlyRecords_rejectsInvalidMonth() {
		assertThatThrownBy(
						() ->
								service.getMonthlyRecords(
										1L, 2L, "2026-13", Locale.SIMPLIFIED_CHINESE))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	void getMonthlyRecords_returnsFullMonthGrid() {
		MemberSigninRecord signed = new MemberSigninRecord();
		signed.setSignDate(LocalDate.of(2026, 9, 1));
		signed.setDailyPoints(0);
		when(recordMapper.selectList(any(Wrapper.class))).thenReturn(List.of(signed));

		Map<String, Object> result =
				service.getMonthlyRecords(1L, 2L, "2026-09", Locale.SIMPLIFIED_CHINESE);

		assertThat(result.get("month")).isEqualTo("2026-09");
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> days = (List<Map<String, Object>>) result.get("days");
		assertThat(days).hasSize(30);
		assertThat(days.get(0))
				.containsEntry("date", "2026-09-01")
				.containsEntry("signed", true)
				.containsEntry("display", "已签");
		assertThat(days.get(0).get("daily_points")).isNull();
	}
}
