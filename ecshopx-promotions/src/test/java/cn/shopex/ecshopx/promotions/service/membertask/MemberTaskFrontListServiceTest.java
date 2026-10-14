package cn.shopex.ecshopx.promotions.service.membertask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.promotions.domain.membertask.MemberTask;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskUserTaskStatus;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskCompletionMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskRewardGrantMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberTaskFrontListServiceTest {

	@Mock
	private MemberTaskMapper memberTaskMapper;

	@Mock
	private MemberTaskCompletionMapper memberTaskCompletionMapper;

	@Mock
	private MemberTaskRewardGrantMapper memberTaskRewardGrantMapper;

	@Mock
	private MemberTaskMemberTagReadPort memberTaskMemberTagReadPort;

	private MemberTaskFrontListService service;

	@BeforeEach
	void setUp() {
		service =
				new MemberTaskFrontListService(
						memberTaskMapper,
						memberTaskCompletionMapper,
						memberTaskRewardGrantMapper,
						memberTaskMemberTagReadPort,
						new ObjectMapper());
	}

	@Test
	void list_inProgressTask_returnsTodoItem() {
		when(memberTaskMemberTagReadPort.userTagIds(1L, 7L)).thenReturn(Set.of());
		MemberTask task = new MemberTask();
		task.setTaskId(100L);
		task.setName("首单");
		task.setTypeCategory("newcomer");
		task.setTypeDetail("first_order");
		task.setPublished(1);
		task.setOffShelf(0);
		task.setBeginTime(1);
		task.setEndTime(Integer.MAX_VALUE);
		task.setFrequencyType("once");
		task.setAudienceMode("unlimited");
		task.setRewardSummary("100积分");
		task.setFrequencyType("once");
		task.setTaskRule("完成首单即得奖励");
		when(memberTaskMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(task));
		when(memberTaskCompletionMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

		var data = service.list(1L, 7L, Locale.SIMPLIFIED_CHINESE);

		assertThat(data.get("total_count")).isEqualTo(1);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("list");
		assertThat(list.get(0).get("user_task_status")).isEqualTo(MemberTaskUserTaskStatus.TODO);
		assertThat(list.get(0).get("frequency_text")).isEqualTo("任务周期内仅可完成一次");
		assertThat(list.get(0).get("task_rule")).isEqualTo("完成首单即得奖励");
		assertThat(list.get(0).get("audience_badge_text")).isEqualTo("限新会员");
	}
}
