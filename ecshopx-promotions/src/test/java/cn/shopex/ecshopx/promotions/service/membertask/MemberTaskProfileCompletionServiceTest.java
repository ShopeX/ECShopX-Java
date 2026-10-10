package cn.shopex.ecshopx.promotions.service.membertask;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.promotions.domain.membertask.MemberTask;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskCompletion;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskRewardGrant;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskCompletionMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskRewardGrantMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberTaskProfileCompletionServiceTest {

	@Mock
	private MemberTaskProfileReadPort memberTaskProfileReadPort;

	@Mock
	private MemberTaskMapper memberTaskMapper;

	@Mock
	private MemberTaskCompletionMapper memberTaskCompletionMapper;

	@Mock
	private MemberTaskRewardGrantMapper memberTaskRewardGrantMapper;

	@Mock
	private MemberTaskRewardGrantService memberTaskRewardGrantService;

	private MemberTaskProfileCompletionService service;

	@BeforeEach
	void setUp() {
		service =
				new MemberTaskProfileCompletionService(
						memberTaskProfileReadPort,
						memberTaskMapper,
						memberTaskCompletionMapper,
						memberTaskRewardGrantMapper,
						memberTaskRewardGrantService);
	}

	@Test
	void incomplete_skipsCompletion() {
		when(memberTaskProfileReadPort.loadOpenMemberInfoFields(1L))
				.thenReturn(new LinkedHashMap<>(Map.of("username", Map.of("is_required", 1, "is_default", 1))));
		when(memberTaskProfileReadPort.loadMemberInfo(1L, 7L)).thenReturn(Map.of("username", ""));

		service.apply(Map.of("company_id", 1L, "user_id", 7L));

		verify(memberTaskMapper, never()).selectList(any());
		verify(memberTaskCompletionMapper, never()).insert(any(MemberTaskCompletion.class));
	}

	@Test
	void complete_writesCompletionAndImmediateGrant() {
		long now = System.currentTimeMillis() / 1000L;
		when(memberTaskProfileReadPort.loadOpenMemberInfoFields(1L))
				.thenReturn(new LinkedHashMap<>(Map.of("username", Map.of("is_required", 1, "is_default", 1))));
		when(memberTaskProfileReadPort.loadMemberInfo(1L, 7L)).thenReturn(Map.of("username", "王五"));

		MemberTask task = new MemberTask();
		task.setTaskId(20L);
		task.setCompanyId(1L);
		task.setTypeDetail("profile");
		task.setPublished(1);
		task.setOffShelf(0);
		task.setBeginTime((int) (now - 10));
		task.setEndTime((int) (now + 1000));
		task.setRewardConfig("{\"points\":10}");
		when(memberTaskMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(task));
		when(memberTaskCompletionMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
		when(memberTaskCompletionMapper.insert(any(MemberTaskCompletion.class)))
				.thenAnswer(
						inv -> {
							inv.getArgument(0, MemberTaskCompletion.class).setCompletionId(55L);
							return 1;
						});

		service.apply(Map.of("company_id", 1L, "user_id", 7L));

		verify(memberTaskCompletionMapper).insert(any(MemberTaskCompletion.class));
		verify(memberTaskRewardGrantMapper).insert(any(MemberTaskRewardGrant.class));
		verify(memberTaskRewardGrantService).grantPendingImmediateForCompletion(eq(1L), eq(7L), eq(55L));
	}

	@Test
	void alreadyCompleted_skipsInsert() {
		long now = System.currentTimeMillis() / 1000L;
		when(memberTaskProfileReadPort.loadOpenMemberInfoFields(1L))
				.thenReturn(new LinkedHashMap<>(Map.of("username", Map.of("is_required", 1, "is_default", 1))));
		when(memberTaskProfileReadPort.loadMemberInfo(1L, 7L)).thenReturn(Map.of("username", "王五"));
		MemberTask task = new MemberTask();
		task.setTaskId(20L);
		task.setCompanyId(1L);
		task.setPublished(1);
		task.setOffShelf(0);
		task.setBeginTime((int) (now - 10));
		task.setEndTime((int) (now + 1000));
		when(memberTaskMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(task));
		when(memberTaskCompletionMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

		service.apply(Map.of("company_id", 1L, "user_id", 7L));

		verify(memberTaskCompletionMapper, never()).insert(any(MemberTaskCompletion.class));
		verify(memberTaskRewardGrantService, never())
				.grantPendingImmediateForCompletion(anyLong(), anyLong(), anyLong());
	}
}
