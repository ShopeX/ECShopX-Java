package cn.shopex.ecshopx.promotions.service.membertask;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.promotions.domain.membertask.MemberTask;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberTaskInviteRegisterCompletionServiceTest {

	@Mock
	private MemberTaskInProgressTasks memberTaskInProgressTasks;

	@Mock
	private cn.shopex.ecshopx.promotions.mapper.MemberTaskCompletionMapper memberTaskCompletionMapper;

	@Mock
	private MemberTaskFirstOrderCompletionPolicy memberTaskOnceCompletionPolicy;

	@Mock
	private MemberTaskImmediateCompletionService memberTaskImmediateCompletionService;

	private MemberTaskInviteRegisterCompletionService service;

	@BeforeEach
	void setUp() {
		service =
				new MemberTaskInviteRegisterCompletionService(
						memberTaskInProgressTasks,
						memberTaskCompletionMapper,
						memberTaskOnceCompletionPolicy,
						memberTaskImmediateCompletionService);
	}

	@Test
	void apply_happyPath() {
		MemberTask task = new MemberTask();
		task.setTaskId(10L);
		task.setCompanyId(1L);
		task.setFrequencyType("once");
		when(memberTaskInProgressTasks.loadInProgressTask(eq(1L), eq(10L), eq("invite_register"), any(Long.class)))
				.thenReturn(task);
		when(memberTaskOnceCompletionPolicy.hasActiveFirstOrderCompletion(10L, 55L)).thenReturn(false);
		when(memberTaskCompletionMapper.selectCount(any())).thenReturn(0L);

		service.apply(
				Map.of(
						"company_id",
						1L,
						"user_id",
						7L,
						"inviter_id",
						55L,
						"member_task_id",
						10L));

		verify(memberTaskImmediateCompletionService)
				.tryComplete(eq(task), eq(55L), eq(0L), eq(7L), anyInt());
	}

	@Test
	void apply_noInviter_skips() {
		service.apply(Map.of("company_id", 1L, "user_id", 7L, "member_task_id", 10L));
		verify(memberTaskImmediateCompletionService, never())
				.tryComplete(any(), anyLong(), anyLong(), anyLong(), anyInt());
	}
}
