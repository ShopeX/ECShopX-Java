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
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
@ExtendWith(MockitoExtension.class)
class MemberTaskTotalSpendServiceTest {

	@Mock
	private MemberTaskUserProgressService memberTaskUserProgressService;

	@Mock
	private MemberTaskCompletionMapper memberTaskCompletionMapper;

	@Mock
	private MemberTaskRewardGrantMapper memberTaskRewardGrantMapper;

	@Mock
	private MemberTaskFirstOrderCompletionPolicy memberTaskOnceCompletionPolicy;

	@Mock
	private MemberTaskRewardGrantService memberTaskRewardGrantService;

	private MemberTaskTotalSpendService service;

	@BeforeEach
	void setUp() {
		service =
				new MemberTaskTotalSpendService(
						memberTaskUserProgressService,
						memberTaskCompletionMapper,
						memberTaskRewardGrantMapper,
						memberTaskOnceCompletionPolicy,
						memberTaskRewardGrantService);
	}

	@Test
	void paySuccess_belowThreshold_onlyProgress() {
		MemberTask task = task();
		MemberTaskOrderSnapshot order = new MemberTaskOrderSnapshot(1L, 9L, 7L, 10L, "PAYED", 0L, 50L, 0L);
		MemberTaskPurchaseRule rule = new MemberTaskPurchaseRule(false, 100L);
		when(memberTaskCompletionMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
		when(memberTaskOnceCompletionPolicy.hasActiveFirstOrderCompletion(10L, 7L)).thenReturn(false);
		when(memberTaskUserProgressService.addAmountFen(1L, 10L, 7L, 50L, 100)).thenReturn(50L);

		service.onPaySuccess(task, order, rule, 100);

		verify(memberTaskCompletionMapper, never()).insert(any(MemberTaskCompletion.class));
	}

	@Test
	void paySuccess_reachesThreshold_writesCompletion() {
		MemberTask task = task();
		MemberTaskOrderSnapshot order = new MemberTaskOrderSnapshot(1L, 9L, 7L, 10L, "PAYED", 0L, 60L, 0L);
		MemberTaskPurchaseRule rule = new MemberTaskPurchaseRule(false, 100L);
		when(memberTaskCompletionMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
		when(memberTaskOnceCompletionPolicy.hasActiveFirstOrderCompletion(10L, 7L)).thenReturn(false);
		when(memberTaskUserProgressService.addAmountFen(1L, 10L, 7L, 60L, 100)).thenReturn(110L);
		when(memberTaskCompletionMapper.insert(any(MemberTaskCompletion.class)))
				.thenAnswer(
						inv -> {
							inv.getArgument(0, MemberTaskCompletion.class).setCompletionId(3L);
							return 1;
						});

		service.onPaySuccess(task, order, rule, 100);

		verify(memberTaskCompletionMapper).insert(any(MemberTaskCompletion.class));
		verify(memberTaskRewardGrantMapper).insert(any(MemberTaskRewardGrant.class));
		verify(memberTaskRewardGrantService).grantPendingImmediate(1L, 9L);
	}

	@Test
	void refund_dropsBelowThreshold_voidsPending() {
		MemberTask task = task();
		MemberTaskOrderSnapshot order = new MemberTaskOrderSnapshot(1L, 9L, 7L, 10L, "PAYED", 0L, 100L, 0L);
		MemberTaskPurchaseRule rule = new MemberTaskPurchaseRule(false, 100L);
		when(memberTaskUserProgressService.deductAmountFen(10L, 7L, 30L, 100)).thenReturn(70L);
		MemberTaskCompletion c = new MemberTaskCompletion();
		c.setCompletionId(3L);
		when(memberTaskCompletionMapper.selectOne(any(QueryWrapper.class))).thenReturn(c);
		MemberTaskRewardGrant g = new MemberTaskRewardGrant();
		g.setGrantId(5L);
		g.setStatus("pending");
		when(memberTaskRewardGrantMapper.selectOne(any(QueryWrapper.class))).thenReturn(g);
		when(memberTaskCompletionMapper.update(eq(null), any())).thenReturn(1);
		when(memberTaskRewardGrantMapper.update(eq(null), any())).thenReturn(1);

		service.onRefund(task, order, rule, 30L, 100);

		verify(memberTaskCompletionMapper).update(eq(null), any());
	}

	private static MemberTask task() {
		MemberTask task = new MemberTask();
		task.setTaskId(10L);
		task.setCompanyId(1L);
		task.setTypeDetail("total_spend");
		task.setFrequencyType("once");
		task.setRewardConfig("{\"points\":10}");
		return task;
	}
}
