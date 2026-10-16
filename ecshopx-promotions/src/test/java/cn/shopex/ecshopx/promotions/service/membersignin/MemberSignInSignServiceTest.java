package cn.shopex.ecshopx.promotions.service.membersignin;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ConflictException;
import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninRecord;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninZoneResolver;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninActivityProgressMapper;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninRecordMapper;
import cn.shopex.ecshopx.promotions.service.membersignin.MemberSignInRewardService.GrantOutcome;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.time.ZoneId;
import java.util.Locale;
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
class MemberSignInSignServiceTest {

	@Mock private MemberSigninRecordMapper recordMapper;
	@Mock private MemberSigninActivityProgressMapper progressMapper;
	@Mock private MemberSignInActivitySupport activitySupport;
	@Mock private PointMemberAddPointService pointMemberAddPointService;
	@Mock private MemberSignInRewardService rewardService;
	@Mock private MessageSource messageSource;
	@Mock private MemberSigninZoneResolver zoneResolver;

	private MemberSignInSignService service;

	@BeforeEach
	void setUp() {
		when(messageSource.getMessage(anyString(), nullable(Object[].class), anyString(), any(Locale.class)))
				.thenAnswer(inv -> inv.getArgument(2));
		when(zoneResolver.resolveZone(anyLong())).thenReturn(ZoneId.of("Asia/Shanghai"));
		service =
				new MemberSignInSignService(
						recordMapper,
						progressMapper,
						activitySupport,
						pointMemberAddPointService,
						rewardService,
						zoneResolver,
						messageSource);
	}

	@Test
	void sign_rejectsDuplicateSameDay() {
		when(recordMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

		assertThatThrownBy(() -> service.sign(1L, 2L, Locale.SIMPLIFIED_CHINESE))
				.isInstanceOf(ConflictException.class);
		verify(recordMapper, never()).insert(any(MemberSigninRecord.class));
	}

	@Test
	void sign_insertsRecordWhenNotSigned() {
		when(recordMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
		when(activitySupport.resolveDailyPointsForDate(anyLong(), any())).thenReturn(0);
		when(activitySupport.findInProgressActivity(1L)).thenReturn(null);

		service.sign(1L, 2L, Locale.SIMPLIFIED_CHINESE);

		verify(recordMapper).insert(any(MemberSigninRecord.class));
		verify(rewardService, never()).grantEligibleTiers(anyLong(), anyLong(), any(), anyInt(), anyLong());
		verify(rewardService, never())
				.recordDailyGrant(anyLong(), anyLong(), any(), anyInt(), anyLong());
	}
}
