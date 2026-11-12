package cn.shopex.ecshopx.promotions.service.membertask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTask;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class MemberTaskDetailServiceTest {

	@Mock
	private MemberTaskMapper memberTaskMapper;

	@Mock
	private MessageSource messageSource;

	@Mock
	private MemberTaskRewardsDisplayService memberTaskRewardsDisplayService;

	@Mock
	private MemberTaskRuleConfigDisplayService memberTaskRuleConfigDisplayService;

	private MemberTaskDetailService service;

	@BeforeEach
	void setUp() {
		service =
				new MemberTaskDetailService(
						memberTaskMapper,
						messageSource,
						new ObjectMapper(),
						memberTaskRewardsDisplayService,
						memberTaskRuleConfigDisplayService);
	}

	@Test
	void detail_returnsSaveShapeForEdit() {
		MemberTask row = new MemberTask();
		row.setTaskId(10L);
		row.setCompanyId(1L);
		row.setName("新人首单");
		row.setTypeCategory("newcomer");
		row.setTypeDetail("first_order");
		row.setBeginTime(1_760_000_000);
		row.setEndTime(Integer.MAX_VALUE);
		row.setPublished(0);
		row.setOffShelf(0);
		row.setFrequencyType("once");
		row.setAudienceMode("unlimited");
		row.setTaskRule("规则说明");
		row.setRewardConfig("{\"points\":100}");
		row.setRuleConfig(
				"{\"goods_scope\":\"category\",\"category_ids\":[1],\"distributor_ids\":[2],\"need_receipt\":true}");
		row.setConfigVersion(2L);
		when(memberTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(row);
		when(memberTaskRewardsDisplayService.buildRewards(1L, "{\"points\":100}"))
				.thenReturn(List.of(Map.of("kind", "points", "points", 100L)));
		Map<String, Object> enrichedRule = new java.util.LinkedHashMap<>();
		enrichedRule.put("goods_scope", "category");
		enrichedRule.put("category_ids", List.of(1L));
		enrichedRule.put("distributor_ids", List.of(2L));
		enrichedRule.put("need_receipt", true);
		enrichedRule.put("category_list", List.of(Map.of("category_id", 1L, "category_name", "类目A")));
		enrichedRule.put("distributor_list", List.of(Map.of("distributor_id", 2L, "name", "店铺B")));
		when(memberTaskRuleConfigDisplayService.enrichForAdminDetail(eq(1L), any()))
				.thenReturn(enrichedRule);

		Map<String, Object> data = service.detail(1L, 10L, Locale.SIMPLIFIED_CHINESE);

		assertThat(data.get("task_id")).isEqualTo(10L);
		assertThat(data.get("config_version")).isEqualTo(2L);
		assertThat(data.get("display_status")).isEqualTo("draft");
		assertThat(data.get("name")).isEqualTo("新人首单");
		assertThat(data.get("frequency_type")).isEqualTo("once");
		assertThat(data.get("task_rule")).isEqualTo("规则说明");
		@SuppressWarnings("unchecked")
		Map<String, Object> reward = (Map<String, Object>) data.get("reward_config");
		assertThat(reward.get("points")).isEqualTo(100);
		@SuppressWarnings("unchecked")
		Map<String, Object> rule = (Map<String, Object>) data.get("rule_config");
		assertThat(rule.get("goods_scope")).isEqualTo("category");
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> categoryList = (List<Map<String, Object>>) rule.get("category_list");
		assertThat(categoryList).hasSize(1);
		assertThat(categoryList.get(0)).containsEntry("category_name", "类目A");
	}

	@Test
	void detail_notFound() {
		when(memberTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
		when(messageSource.getMessage(
						"promotions.member_task.task_not_found", null, "任务不存在", Locale.SIMPLIFIED_CHINESE))
				.thenReturn("任务不存在");

		assertThatThrownBy(() -> service.detail(1L, 99L, Locale.SIMPLIFIED_CHINESE))
				.isInstanceOf(ResourceException.class)
				.hasMessage("任务不存在");
	}
}
