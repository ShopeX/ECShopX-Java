package cn.shopex.ecshopx.promotions.service.membertask;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.promotions.domain.membertask.MemberTask;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskCompletion;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskGrantTrigger;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskOrderSnapshot;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskCompletionMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskRewardGrantMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class MemberTaskPaySuccessServiceTest {

	@Mock
	private MemberTaskOrderReadPort memberTaskOrderReadPort;

	@Mock
	private MemberTaskMapper memberTaskMapper;

	@Mock
	private MemberTaskCompletionMapper memberTaskCompletionMapper;

	@Mock
	private MemberTaskRewardGrantMapper memberTaskRewardGrantMapper;

	@Mock
	private MemberTaskFirstOrderCompletionPolicy firstOrderCompletionPolicy;

	@Mock
	private MemberTaskRewardGrantService memberTaskRewardGrantService;

	@Mock
	private MemberTaskTotalSpendService memberTaskTotalSpendService;

	@Mock
	private MemberTaskShareOrderService memberTaskShareOrderService;

	private MemberTaskPaySuccessService service;

	@BeforeEach
	void setUp() {
		service =
				new MemberTaskPaySuccessService(
						memberTaskOrderReadPort,
						memberTaskMapper,
						memberTaskCompletionMapper,
						memberTaskRewardGrantMapper,
						firstOrderCompletionPolicy,
						memberTaskRewardGrantService,
						memberTaskTotalSpendService,
						memberTaskShareOrderService,
						new ObjectMapper());
	}

	@Test
	void needReceiptTrue_writesPending_doesNotImmediateGrant() {
		long now = System.currentTimeMillis() / 1000L;
		stubHappyPath(now, true, "PAYED", 0L);
		when(firstOrderCompletionPolicy.hasActiveFirstOrderCompletion(10L, 7L)).thenReturn(false);
		when(memberTaskCompletionMapper.insert(any(MemberTaskCompletion.class)))
				.thenAnswer(
						inv -> {
							inv.getArgument(0, MemberTaskCompletion.class).setCompletionId(3L);
							return 1;
						});

		service.apply(Map.of("company_id", 1L, "order_id", 9L));

		verify(memberTaskCompletionMapper).insert(any(MemberTaskCompletion.class));
		verify(memberTaskRewardGrantMapper).insert(any(cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskRewardGrant.class));
		verify(memberTaskRewardGrantService, never())
				.grantPendingImmediate(anyLong(), anyLong());
	}

	@Test
	void needReceiptFalse_callsImmediateGrant() {
		long now = System.currentTimeMillis() / 1000L;
		stubHappyPath(now, false, "PAYED", 0L);
		when(firstOrderCompletionPolicy.hasActiveFirstOrderCompletion(10L, 7L)).thenReturn(false);
		when(memberTaskCompletionMapper.insert(any(MemberTaskCompletion.class)))
				.thenAnswer(
						inv -> {
							inv.getArgument(0, MemberTaskCompletion.class).setCompletionId(3L);
							return 1;
						});

		service.apply(Map.of("company_id", 1L, "order_id", 9L));

		verify(memberTaskRewardGrantService).grantPendingImmediate(1L, 9L);
	}

	@Test
	void alreadyDoneH3_skipsInsert() {
		long now = System.currentTimeMillis() / 1000L;
		stubHappyPath(now, true, "PAYED", 0L);
		when(firstOrderCompletionPolicy.hasActiveFirstOrderCompletion(10L, 7L)).thenReturn(true);

		service.apply(Map.of("company_id", 1L, "order_id", 9L));

		verify(memberTaskCompletionMapper, never()).insert(any(MemberTaskCompletion.class));
	}

	@Test
	void duplicatePay_doesNotInsertGrant() {
		long now = System.currentTimeMillis() / 1000L;
		stubHappyPath(now, true, "PAYED", 0L);
		when(firstOrderCompletionPolicy.hasActiveFirstOrderCompletion(10L, 7L)).thenReturn(false);
		when(memberTaskCompletionMapper.insert(any(MemberTaskCompletion.class)))
				.thenThrow(new DuplicateKeyException("uk_mtc_task_order"));

		service.apply(Map.of("company_id", 1L, "order_id", 9L));

		verify(memberTaskRewardGrantMapper, never())
				.insert(any(cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskRewardGrant.class));
	}

	@Test
	void singleSpend_needReceiptFalse_immediateGrant() {
		long now = System.currentTimeMillis() / 1000L;
		when(memberTaskOrderReadPort.get(1L, 9L))
				.thenReturn(new MemberTaskOrderSnapshot(1L, 9L, 7L, 10L, "PAYED", 0L, 500L, 0L));
		MemberTask task = new MemberTask();
		task.setTaskId(10L);
		task.setCompanyId(1L);
		task.setTypeDetail("single_spend");
		task.setFrequencyType("once");
		task.setPublished(1);
		task.setOffShelf(0);
		task.setBeginTime((int) (now - 10));
		task.setEndTime((int) (now + 3600));
		task.setRuleConfig("{\"need_receipt\":false,\"amount_fen\":100,\"goods_scope\":\"unlimited\"}");
		task.setRewardConfig("{\"points\":10}");
		when(memberTaskMapper.selectOne(any(QueryWrapper.class))).thenReturn(task);
		when(firstOrderCompletionPolicy.hasActiveFirstOrderCompletion(10L, 7L)).thenReturn(false);
		when(memberTaskCompletionMapper.insert(any(MemberTaskCompletion.class)))
				.thenAnswer(
						inv -> {
							inv.getArgument(0, MemberTaskCompletion.class).setCompletionId(3L);
							return 1;
						});

		service.apply(Map.of("company_id", 1L, "order_id", 9L));

		verify(memberTaskRewardGrantService).grantPendingImmediate(1L, 9L);
	}

	void selfPickupAlreadyDone_callsGrantPendingIfOrderDone() {
		long now = System.currentTimeMillis() / 1000L;
		stubHappyPath(now, true, "DONE", now);
		when(firstOrderCompletionPolicy.hasActiveFirstOrderCompletion(10L, 7L)).thenReturn(false);
		when(memberTaskCompletionMapper.insert(any(MemberTaskCompletion.class)))
				.thenAnswer(
						inv -> {
							inv.getArgument(0, MemberTaskCompletion.class).setCompletionId(3L);
							return 1;
						});

		service.apply(Map.of("company_id", 1L, "order_id", 9L));

		verify(memberTaskRewardGrantService)
				.grantPendingIfOrderDone(1L, 9L, MemberTaskGrantTrigger.PAY_SUCCESS_AFTER_DONE);
	}

	private void stubHappyPath(long now, boolean needReceipt, String status, long endTime) {
		when(memberTaskOrderReadPort.get(1L, 9L))
				.thenReturn(new MemberTaskOrderSnapshot(1L, 9L, 7L, 10L, status, endTime, 500L, 0L));
		MemberTask task = new MemberTask();
		task.setTaskId(10L);
		task.setCompanyId(1L);
		task.setTypeDetail("first_order");
		task.setFrequencyType("once");
		task.setPublished(1);
		task.setOffShelf(0);
		task.setBeginTime((int) (now - 10));
		task.setEndTime((int) (now + 3600));
		task.setRuleConfig("{\"need_receipt\":" + needReceipt + ",\"amount_fen\":0}");
		task.setRewardConfig("{\"points\":10}");
		when(memberTaskMapper.selectOne(any(QueryWrapper.class))).thenReturn(task);
	}
}
