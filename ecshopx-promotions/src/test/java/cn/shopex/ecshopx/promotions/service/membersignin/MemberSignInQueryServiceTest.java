package cn.shopex.ecshopx.promotions.service.membersignin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninZoneResolver;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninActivityProgressMapper;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninActivityTierMapper;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninRecordMapper;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninRewardGrantMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class MemberSignInQueryServiceTest {

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
	}

	@Test
	void getActivityProgress_returnsNullActivityWhenNone() {
		Map<String, Object> result = service.getActivityProgress(1L, 2L);
		assertThat(result.get("activity")).isNull();
		assertThat(result.get("progress_count")).isEqualTo(0);
	}
}
