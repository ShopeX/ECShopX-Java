package cn.shopex.ecshopx.promotions.service.membertask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskCompletion;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskGrantAttemptResult;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskGrantTrigger;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskOrderSnapshot;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskRewardGrant;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskCompletionMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskRewardGrantMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberTaskRewardGrantServiceTest {

	@Mock
	private MemberTaskOrderReadPort memberTaskOrderReadPort;

	@Mock
	private MemberTaskCompletionMapper memberTaskCompletionMapper;

	@Mock
	private MemberTaskRewardGrantMapper memberTaskRewardGrantMapper;

	@Mock
	private MemberTaskPointGrantPort memberTaskPointGrantPort;

	@Mock
	private MemberTaskCouponGrantPort memberTaskCouponGrantPort;

	private MemberTaskRewardGrantService service;

	@BeforeEach
	void setUp() {
		service =
				new MemberTaskRewardGrantService(
						memberTaskOrderReadPort,
						memberTaskCompletionMapper,
						memberTaskRewardGrantMapper,
						memberTaskPointGrantPort,
						memberTaskCouponGrantPort,
						new ObjectMapper());
	}

	@Test
	void payedOrder_neverCallsPort() {
		when(memberTaskOrderReadPort.get(1L, 9L))
				.thenReturn(new MemberTaskOrderSnapshot(1L, 9L, 7L, 10L, "PAYED", 0L, 100L, 0L, 0L));

		assertThat(service.grantPendingIfOrderDone(1L, 9L, MemberTaskGrantTrigger.CONFIRM_RECEIPT))
				.isEqualTo(MemberTaskGrantAttemptResult.SKIPPED_GATE);
		verify(memberTaskPointGrantPort, never()).grantPoints(anyLong(), anyLong(), anyInt(), anyLong());
	}

	@Test
	void doneOrder_pendingGrant_callsPortOnce() {
		when(memberTaskOrderReadPort.get(1L, 9L))
				.thenReturn(new MemberTaskOrderSnapshot(1L, 9L, 7L, 10L, "DONE", 100L, 100L, 0L, 0L));
		MemberTaskCompletion c = new MemberTaskCompletion();
		c.setCompletionId(3L);
		when(memberTaskCompletionMapper.selectOne(any(QueryWrapper.class))).thenReturn(c);
		MemberTaskRewardGrant g = new MemberTaskRewardGrant();
		g.setGrantId(5L);
		g.setCompletionId(3L);
		g.setTaskId(10L);
		g.setStatus("pending");
		g.setRewardSnapshot("{\"points\":80,\"coupon_id\":null}");
		when(memberTaskRewardGrantMapper.selectOne(any(QueryWrapper.class))).thenReturn(g);
		when(memberTaskRewardGrantMapper.update(eq(null), any(UpdateWrapper.class))).thenReturn(1);

		assertThat(service.grantPendingIfOrderDone(1L, 9L, MemberTaskGrantTrigger.CONFIRM_RECEIPT))
				.isEqualTo(MemberTaskGrantAttemptResult.GRANTED);
		verify(memberTaskPointGrantPort).grantPoints(1L, 7L, 80, 9L);
	}

	@Test
	void secondCall_alreadyGranted_portNever() {
		when(memberTaskOrderReadPort.get(1L, 9L))
				.thenReturn(new MemberTaskOrderSnapshot(1L, 9L, 7L, 10L, "DONE", 100L, 100L, 0L, 0L));
		MemberTaskCompletion c = new MemberTaskCompletion();
		c.setCompletionId(3L);
		when(memberTaskCompletionMapper.selectOne(any(QueryWrapper.class))).thenReturn(c);
		MemberTaskRewardGrant g = new MemberTaskRewardGrant();
		g.setGrantId(5L);
		g.setStatus("granted");
		when(memberTaskRewardGrantMapper.selectOne(any(QueryWrapper.class))).thenReturn(g);

		assertThat(service.grantPendingIfOrderDone(1L, 9L, MemberTaskGrantTrigger.CONFIRM_RECEIPT))
				.isEqualTo(MemberTaskGrantAttemptResult.ALREADY_GRANTED);
		verify(memberTaskPointGrantPort, never()).grantPoints(anyLong(), anyLong(), anyInt(), anyLong());
	}

	@Test
	void shareOrderGrant_usesGrantUserIdNotBuyer() {
		when(memberTaskOrderReadPort.get(1L, 9L))
				.thenReturn(new MemberTaskOrderSnapshot(1L, 9L, 7L, 10L, "DONE", 100L, 100L, 0L, 0L));
		MemberTaskCompletion c = new MemberTaskCompletion();
		c.setCompletionId(3L);
		when(memberTaskCompletionMapper.selectOne(any(QueryWrapper.class))).thenReturn(c);
		MemberTaskRewardGrant g = new MemberTaskRewardGrant();
		g.setGrantId(5L);
		g.setCompletionId(3L);
		g.setTaskId(10L);
		g.setUserId(55L);
		g.setStatus("pending");
		g.setRewardSnapshot("{\"points\":80}");
		when(memberTaskRewardGrantMapper.selectOne(any(QueryWrapper.class))).thenReturn(g);
		when(memberTaskRewardGrantMapper.update(eq(null), any(UpdateWrapper.class))).thenReturn(1);

		service.grantPendingIfOrderDone(1L, 9L, MemberTaskGrantTrigger.CONFIRM_RECEIPT);

		verify(memberTaskPointGrantPort).grantPoints(1L, 55L, 80, 9L);
	}

	@Test
	void portThrows_marksFailed() {
		when(memberTaskOrderReadPort.get(1L, 9L))
				.thenReturn(new MemberTaskOrderSnapshot(1L, 9L, 7L, 10L, "DONE", 100L, 100L, 0L, 0L));
		MemberTaskCompletion c = new MemberTaskCompletion();
		c.setCompletionId(3L);
		when(memberTaskCompletionMapper.selectOne(any(QueryWrapper.class))).thenReturn(c);
		MemberTaskRewardGrant g = new MemberTaskRewardGrant();
		g.setGrantId(5L);
		g.setCompletionId(3L);
		g.setTaskId(10L);
		g.setStatus("pending");
		g.setRewardSnapshot("{\"points\":80}");
		when(memberTaskRewardGrantMapper.selectOne(any(QueryWrapper.class))).thenReturn(g);
		when(memberTaskRewardGrantMapper.update(eq(null), any(UpdateWrapper.class))).thenReturn(1);
		org.mockito.Mockito.doThrow(new RuntimeException("积分不足"))
				.when(memberTaskPointGrantPort)
				.grantPoints(anyLong(), anyLong(), anyInt(), anyLong());

		assertThat(service.grantPendingIfOrderDone(1L, 9L, MemberTaskGrantTrigger.CONFIRM_RECEIPT))
				.isEqualTo(MemberTaskGrantAttemptResult.FAILED);
	}

	@Test
	void adminRetry_failedGrant_callsPortOnce() {
		MemberTaskCompletion completion = new MemberTaskCompletion();
		completion.setCompanyId(1L);
		completion.setUserId(7L);
		completion.setOrderId(9L);
		MemberTaskRewardGrant g = new MemberTaskRewardGrant();
		g.setGrantId(5L);
		g.setCompanyId(1L);
		g.setTaskId(10L);
		g.setUserId(7L);
		g.setStatus("failed");
		g.setRetryCount(0);
		g.setRewardSnapshot("{\"points\":50}");
		when(memberTaskOrderReadPort.get(1L, 9L))
				.thenReturn(new MemberTaskOrderSnapshot(1L, 9L, 7L, 10L, "DONE", 100L, 100L, 0L, 0L));
		when(memberTaskRewardGrantMapper.update(eq(null), any(UpdateWrapper.class)))
				.thenReturn(1)
				.thenReturn(1);

		assertThat(service.retryAdminFailedGrant(g, completion))
				.isEqualTo(MemberTaskGrantAttemptResult.GRANTED);
		verify(memberTaskPointGrantPort).grantPoints(1L, 7L, 50, 9L);
	}
}
