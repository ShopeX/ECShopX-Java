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
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninRewardGrant;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninRewardGrantStatus;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninRewardRecordType;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninRewardGrantMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
class MemberSignInQueryServiceRewardsTest {

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
	void getRewards_returnsEmptyPage() {
		Page<?> empty = new Page<>(1, 20);
		empty.setRecords(List.of());
		empty.setTotal(0);
		when(grantMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(empty);

		Map<String, Object> result = service.getRewards(1L, 2L, null, "1", "20");

		assertThat((List<?>) result.get("list")).isEmpty();
		assertThat(result.get("total_count")).isEqualTo(0L);
	}

	@Test
	void getRewards_mapsGrantedRecordWithRewardItems() {
		MemberSigninRewardGrant grant = new MemberSigninRewardGrant();
		grant.setId(7L);
		grant.setRecordType(MemberSigninRewardRecordType.DAILY);
		grant.setTitle("签到");
		grant.setStatus(MemberSigninRewardGrantStatus.GRANTED);
		grant.setRewardSnapshot(
				MemberSignInRewardSnapshot.writeDailyPoints(new ObjectMapper(), 1));
		grant.setCreated(100);

		Page<MemberSigninRewardGrant> page = new Page<>(1, 20);
		page.setRecords(List.of(grant));
		page.setTotal(1);
		when(grantMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);

		Map<String, Object> result = service.getRewards(1L, 2L, null, "1", "20");

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("list");
		assertThat(list).hasSize(1);
		assertThat(list.get(0).get("record_type")).isEqualTo("daily");
		assertThat((List<?>) list.get(0).get("reward_items")).isNotEmpty();
	}
}
