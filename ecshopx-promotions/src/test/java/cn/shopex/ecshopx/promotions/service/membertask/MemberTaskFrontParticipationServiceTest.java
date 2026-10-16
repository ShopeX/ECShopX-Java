package cn.shopex.ecshopx.promotions.service.membertask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.promotions.mapper.MemberTaskCompletionListMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberTaskFrontParticipationServiceTest {

	@Mock
	private MemberTaskCompletionListMapper memberTaskCompletionListMapper;

	@Mock
	private MemberTaskRewardsDisplayService memberTaskRewardsDisplayService;

	private MemberTaskFrontParticipationService service;

	@BeforeEach
	void setUp() {
		service =
				new MemberTaskFrontParticipationService(
						memberTaskCompletionListMapper, memberTaskRewardsDisplayService);
	}

	@Test
	void list_returnsPagedItem() {
		when(memberTaskCompletionListMapper.countUserParticipations(1L, 9L)).thenReturn(1L);
		when(memberTaskCompletionListMapper.selectUserParticipationsPage(eq(1L), eq(9L), eq(0L), eq(20)))
				.thenReturn(
						List.of(
								Map.of(
										"completion_id",
										11L,
										"task_id",
										100L,
										"task_name",
										"签到",
										"type_category",
										"behavior",
										"type_detail",
										"sign_in",
										"completed_at",
										1_700_000_000,
										"grant_status",
										"granted",
										"reward_config",
										"{\"points\":10}")));
		when(memberTaskRewardsDisplayService.buildRewards(1L, "{\"points\":10}"))
				.thenReturn(List.of(Map.of("kind", "points", "points", 10L)));

		var data = service.list(1L, 9L, 1, 20);

		assertThat(data.get("total_count")).isEqualTo(1L);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("list");
		assertThat(list.get(0).get("grant_status_text")).isEqualTo("奖励已发放");
		assertThat(list.get(0).get("user_task_status")).isEqualTo("rewarded");
	}
}
