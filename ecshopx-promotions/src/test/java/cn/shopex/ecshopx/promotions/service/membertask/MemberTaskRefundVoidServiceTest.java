package cn.shopex.ecshopx.promotions.service.membertask;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskCompletion;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskOrderSnapshot;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskRewardGrant;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskCompletionMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskRewardGrantMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
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
	private MemberTaskCompletionMapper memberTaskCompletionMapper;

	@Mock
	private MemberTaskRewardGrantMapper memberTaskRewardGrantMapper;

	private MemberTaskRefundVoidService service;

	@BeforeEach
	void setUp() {
		service =
				new MemberTaskRefundVoidService(
						memberTaskOrderReadPort, memberTaskCompletionMapper, memberTaskRewardGrantMapper);
	}

	@Test
	void pendingGrant_voidsCompletion() {
		when(memberTaskOrderReadPort.get(1L, 9L))
				.thenReturn(new MemberTaskOrderSnapshot(1L, 9L, 7L, 10L, "PAYED", 0L, 100L, 0L));
		MemberTaskCompletion c = new MemberTaskCompletion();
		c.setCompletionId(3L);
		when(memberTaskCompletionMapper.selectOne(any(QueryWrapper.class))).thenReturn(c);
		MemberTaskRewardGrant g = new MemberTaskRewardGrant();
		g.setGrantId(5L);
		g.setStatus("pending");
		when(memberTaskRewardGrantMapper.selectOne(any(QueryWrapper.class))).thenReturn(g);
		when(memberTaskCompletionMapper.update(eq(null), any(UpdateWrapper.class))).thenReturn(1);

		service.apply(Map.of("company_id", 1L, "order_id", 9L, "refund_success_time", "2026-10-10 10:00:00"));

		verify(memberTaskCompletionMapper).update(eq(null), any(UpdateWrapper.class));
		verify(memberTaskRewardGrantMapper).update(eq(null), any(UpdateWrapper.class));
	}

	@Test
	void alreadyGranted_doesNotVoid() {
		when(memberTaskOrderReadPort.get(1L, 9L))
				.thenReturn(new MemberTaskOrderSnapshot(1L, 9L, 7L, 10L, "DONE", 100L, 100L, 0L));
		MemberTaskCompletion c = new MemberTaskCompletion();
		c.setCompletionId(3L);
		when(memberTaskCompletionMapper.selectOne(any(QueryWrapper.class))).thenReturn(c);
		MemberTaskRewardGrant g = new MemberTaskRewardGrant();
		g.setGrantId(5L);
		g.setStatus("granted");
		when(memberTaskRewardGrantMapper.selectOne(any(QueryWrapper.class))).thenReturn(g);

		service.apply(Map.of("company_id", 1L, "order_id", 9L));

		verify(memberTaskCompletionMapper, never()).update(eq(null), any(UpdateWrapper.class));
	}
}
