package cn.shopex.ecshopx.promotions.service.membersignin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ConflictException;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountReceiveCardService;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninActivity;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninDisplayStatus;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninActivityTierMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.util.LinkedHashMap;
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
class MemberSignInActivityServiceTest {

	@Mock private MemberSigninActivityMapper activityMapper;
	@Mock private MemberSigninActivityTierMapper tierMapper;
	@Mock private DiscountCardsMapper discountCardsMapper;
	@Mock private UserDiscountReceiveCardService userDiscountReceiveCardService;
	@Mock private MessageSource messageSource;

	private MemberSignInActivityService service;

	@BeforeEach
	void setUp() {
		when(messageSource.getMessage(anyString(), nullable(Object[].class), anyString(), any(Locale.class)))
				.thenAnswer(inv -> inv.getArgument(2));
		service =
				new MemberSignInActivityService(
						activityMapper,
						tierMapper,
						discountCardsMapper,
						userDiscountReceiveCardService,
						messageSource);
	}

	@Test
	void stopActivity_onlyUpdatesEndTime() {
		long now = Instant.now().getEpochSecond();
		MemberSigninActivity row = new MemberSigninActivity();
		row.setId(10L);
		row.setCompanyId(1L);
		row.setName("活动A");
		row.setBeginTime(now - 3600);
		row.setEndTime(now + 3600);
		row.setConfigVersion(3L);
		when(activityMapper.selectOne(any(Wrapper.class))).thenReturn(row);
		when(activityMapper.update(any(), any())).thenReturn(1);

		Map<String, Object> result = service.stopActivity(1L, 10L, Locale.SIMPLIFIED_CHINESE);

		assertThat(result.get("display_status")).isEqualTo(MemberSigninDisplayStatus.ENDED);
		verify(activityMapper).update(any(), any());
	}

	@Test
	void updateActivity_rejectsConfigVersionMismatch() {
		long now = Instant.now().getEpochSecond();
		MemberSigninActivity row = new MemberSigninActivity();
		row.setId(10L);
		row.setCompanyId(1L);
		row.setBeginTime(now + 1000);
		row.setEndTime(now + 2000);
		row.setConfigVersion(2L);
		when(activityMapper.selectOne(any(Wrapper.class))).thenReturn(row);

		Map<String, Object> input = new LinkedHashMap<>();
		input.put("name", "活动B");
		input.put("begin_time", now + 1000);
		input.put("end_time", now + 2000);
		input.put("config_version", 1);
		input.put("tiers", List.of(Map.of("tier_count", 1, "points_reward", 10)));

		assertThatThrownBy(() -> service.updateActivity(1L, 10L, input, Locale.SIMPLIFIED_CHINESE))
				.isInstanceOf(ConflictException.class);
	}

	@Test
	void listActivities_returnsPagedSummary() {
		long now = Instant.now().getEpochSecond();
		MemberSigninActivity row = new MemberSigninActivity();
		row.setId(1L);
		row.setName("A");
		row.setBeginTime(now - 10);
		row.setEndTime(now + 10);
		row.setConfigVersion(1L);
		row.setCreated((int) now);
		Page<MemberSigninActivity> page = new Page<>(1, 20);
		page.setRecords(List.of(row));
		page.setTotal(1);
		when(activityMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);

		Map<String, Object> result = service.listActivities(1L, null, "1", "20");

		assertThat(result.get("total_count")).isEqualTo(1L);
		assertThat((List<?>) result.get("list")).hasSize(1);
	}
}
