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
import cn.shopex.ecshopx.promotions.mapper.MemberTaskMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskRewardGrantMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberTaskRefundVoidServiceTest {

	@Mock
	private MemberTaskOrderReadPort memberTaskOrderReadPort;

	@Mock
	private MemberTaskMapper memberTaskMapper;

	@Mock
	private MemberTaskCompletionMapper memberTaskCompletionMapper;

	@Mock
	private MemberTaskRewardGrantMapper memberTaskRewardGrantMapper;

	@Mock
	private MemberTaskTotalSpendService memberTaskTotalSpendService;

	private MemberTaskRefundVoidService service;

	@BeforeEach
	void setUp() {
		service =
				new MemberTaskRefundVoidService(
						memberTaskOrderReadPort,
						memberTaskMapper,
						memberTaskCompletionMapper,
						memberTaskRewardGrantMapper,
						memberTaskTotalSpendService,
						new ObjectMapper());
	}

	@Test
	void pendingGrant_voidsCompletion() {
		stubTask("{\"amount_fen\":100,\"need_receipt\":true,\"goods_scope\":\"unlimited\"}");
		when(memberTaskOrderReadPort.get(1L, 9L))
				.thenReturn(new MemberTaskOrderSnapshot(1L, 9L, 7L, 10L, "PAYED", 0L, 100L, 0L, 0L));
		MemberTaskCompletion c = new MemberTaskCompletion();
		c.setCompletionId(3L);
		when(memberTaskCompletionMapper.selectOne(any(QueryWrapper.class))).thenReturn(c);
		MemberTaskRewardGrant g = new MemberTaskRewardGrant();
		g.setGrantId(5L);
		g.setStatus("pending");
		when(memberTaskRewardGrantMapper.selectOne(any(QueryWrapper.class))).thenReturn(g);
		when(memberTaskCompletionMapper.update(eq(null), any(UpdateWrapper.class))).thenReturn(1);

		service.apply(
				Map.of(
						"company_id",
						1L,
						"order_id",
						9L,
						"refund_success_time",
						"2026-10-10 10:00:00",
						"refunded_fee",
						100L));

		verify(memberTaskCompletionMapper).update(eq(null), any(UpdateWrapper.class));
		verify(memberTaskRewardGrantMapper).update(eq(null), any(UpdateWrapper.class));
	}

	@Test
	void partialRefund_stillMeetsAmount_doesNotVoid() {
		stubTask("{\"amount_fen\":100,\"need_receipt\":true,\"goods_scope\":\"unlimited\"}");
		when(memberTaskOrderReadPort.get(1L, 9L))
				.thenReturn(new MemberTaskOrderSnapshot(1L, 9L, 7L, 10L, "PAYED", 0L, 200L, 0L, 0L));

		service.apply(
				Map.of(
						"company_id",
						1L,
						"order_id",
						9L,
						"refund_success_time",
						"2026-10-10 10:00:00",
						"refunded_fee",
						50L));

		verify(memberTaskCompletionMapper, never()).selectOne(any(QueryWrapper.class));
	}

	@Test
	void alreadyGranted_doesNotVoid() {
		stubTask("{\"amount_fen\":50,\"need_receipt\":true,\"goods_scope\":\"unlimited\"}");
		when(memberTaskOrderReadPort.get(1L, 9L))
				.thenReturn(new MemberTaskOrderSnapshot(1L, 9L, 7L, 10L, "DONE", 100L, 100L, 0L, 0L));
		MemberTaskCompletion c = new MemberTaskCompletion();
		c.setCompletionId(3L);
		when(memberTaskCompletionMapper.selectOne(any(QueryWrapper.class))).thenReturn(c);
		MemberTaskRewardGrant g = new MemberTaskRewardGrant();
		g.setGrantId(5L);
		g.setStatus("granted");
		when(memberTaskRewardGrantMapper.selectOne(any(QueryWrapper.class))).thenReturn(g);

		service.apply(
				Map.of(
						"company_id",
						1L,
						"order_id",
						9L,
						"refund_success_time",
						"2026-10-10 10:00:00",
						"refunded_fee",
						60L));

		verify(memberTaskCompletionMapper, never()).update(eq(null), any(UpdateWrapper.class));
	}

	private void stubTask(String ruleConfig) {
		MemberTask task = new MemberTask();
		task.setTaskId(10L);
		task.setRuleConfig(ruleConfig);
		when(memberTaskMapper.selectOne(any(QueryWrapper.class))).thenReturn(task);
	}
}
