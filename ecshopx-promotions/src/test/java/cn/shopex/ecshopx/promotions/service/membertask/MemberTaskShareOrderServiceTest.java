package cn.shopex.ecshopx.promotions.service.membertask;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.promotions.domain.membertask.MemberTask;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskCompletion;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskOrderSnapshot;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskRewardGrant;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskCompletionMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskRewardGrantMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MemberTaskShareOrderServiceTest {

	@Mock
	private MemberTaskInviterReadPort memberTaskInviterReadPort;

	@Mock
	private MemberTaskCompletionMapper memberTaskCompletionMapper;

	@Mock
	private MemberTaskRewardGrantMapper memberTaskRewardGrantMapper;

	@Mock
	private MemberTaskFirstOrderCompletionPolicy memberTaskOnceCompletionPolicy;

	@Mock
	private MemberTaskRewardGrantService memberTaskRewardGrantService;

	private MemberTaskShareOrderService service;

	@BeforeEach
	void setUp() {
		service =
				new MemberTaskShareOrderService(
						memberTaskInviterReadPort,
						memberTaskCompletionMapper,
						memberTaskRewardGrantMapper,
						memberTaskOnceCompletionPolicy,
						memberTaskRewardGrantService);
	}

	@Test
	void noInviter_skips() {
		MemberTask task = task();
		MemberTaskOrderSnapshot order = new MemberTaskOrderSnapshot(1L, 9L, 7L, 10L, "PAYED", 0L, 100L, 0L);
		when(memberTaskInviterReadPort.findInviterUserId(1L, 7L)).thenReturn(0L);

		service.onPaySuccess(task, order, rule(false, 0L), 100);

		verify(memberTaskCompletionMapper, never()).insert(any(MemberTaskCompletion.class));
	}

	@Test
	void happyPath_completionForSharer() {
		MemberTask task = task();
		MemberTaskOrderSnapshot order = new MemberTaskOrderSnapshot(1L, 9L, 7L, 10L, "PAYED", 0L, 100L, 0L);
		when(memberTaskInviterReadPort.findInviterUserId(1L, 7L)).thenReturn(55L);
		when(memberTaskOnceCompletionPolicy.hasActiveFirstOrderCompletion(10L, 55L)).thenReturn(false);
		when(memberTaskCompletionMapper.insert(any(MemberTaskCompletion.class)))
				.thenAnswer(
						inv -> {
							inv.getArgument(0, MemberTaskCompletion.class).setCompletionId(3L);
							return 1;
						});

		service.onPaySuccess(task, order, rule(false, 0L), 100);

		ArgumentCaptor<MemberTaskCompletion> cap = ArgumentCaptor.forClass(MemberTaskCompletion.class);
		verify(memberTaskCompletionMapper).insert(cap.capture());
		assertThat(cap.getValue().getUserId()).isEqualTo(55L);
		assertThat(cap.getValue().getInviteeUserId()).isEqualTo(7L);
		verify(memberTaskRewardGrantService).grantPendingImmediate(1L, 9L);
	}

	@Test
	void needReceiptTrue_doesNotImmediateGrant() {
		MemberTask task = task();
		MemberTaskOrderSnapshot order = new MemberTaskOrderSnapshot(1L, 9L, 7L, 10L, "PAYED", 0L, 100L, 0L);
		when(memberTaskInviterReadPort.findInviterUserId(1L, 7L)).thenReturn(55L);
		when(memberTaskOnceCompletionPolicy.hasActiveFirstOrderCompletion(10L, 55L)).thenReturn(false);
		when(memberTaskCompletionMapper.insert(any(MemberTaskCompletion.class)))
				.thenAnswer(
						inv -> {
							inv.getArgument(0, MemberTaskCompletion.class).setCompletionId(3L);
							return 1;
						});
		when(memberTaskRewardGrantMapper.insert(any(MemberTaskRewardGrant.class))).thenReturn(1);

		service.onPaySuccess(task, order, rule(true, 0L), 100);

		verify(memberTaskRewardGrantService, never()).grantPendingImmediate(eq(1L), eq(9L));
	}

	private static MemberTask task() {
		MemberTask task = new MemberTask();
		task.setTaskId(10L);
		task.setCompanyId(1L);
		task.setTypeDetail("share_order");
		task.setFrequencyType("once");
		task.setRewardConfig("{\"points\":10}");
		return task;
	}

	private static MemberTaskPurchaseRule rule(boolean needReceipt, long amountFen) {
		return new MemberTaskPurchaseRule(needReceipt, amountFen);
	}
}
