package cn.shopex.ecshopx.promotions.service.membertask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTask;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskCompletion;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskGrantAttemptResult;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskRewardGrant;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskCompletionMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskRewardGrantMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class MemberTaskGrantRetryServiceTest {

	@Mock
	private MemberTaskMapper memberTaskMapper;

	@Mock
	private MemberTaskRewardGrantMapper memberTaskRewardGrantMapper;

	@Mock
	private MemberTaskCompletionMapper memberTaskCompletionMapper;

	@Mock
	private MemberTaskRewardGrantService memberTaskRewardGrantService;

	@Mock
	private MessageSource messageSource;

	private MemberTaskGrantRetryService service;

	@BeforeEach
	void setUp() {
		service =
				new MemberTaskGrantRetryService(
						memberTaskMapper,
						memberTaskRewardGrantMapper,
						memberTaskCompletionMapper,
						memberTaskRewardGrantService,
						messageSource);
	}

	@Test
	void retry_success_returnsGrantSnapshot() {
		Locale locale = Locale.SIMPLIFIED_CHINESE;
		MemberTask task = new MemberTask();
		when(memberTaskMapper.selectOne(any(QueryWrapper.class))).thenReturn(task);
		MemberTaskRewardGrant grant = new MemberTaskRewardGrant();
		grant.setGrantId(5L);
		grant.setCompletionId(3L);
		grant.setStatus("failed");
		grant.setRetryCount(1);
		when(memberTaskRewardGrantMapper.selectOne(any(QueryWrapper.class))).thenReturn(grant);
		MemberTaskCompletion completion = new MemberTaskCompletion();
		completion.setVoided(0);
		when(memberTaskCompletionMapper.selectById(3L)).thenReturn(completion);
		when(memberTaskRewardGrantService.retryAdminFailedGrant(grant, completion))
				.thenReturn(MemberTaskGrantAttemptResult.GRANTED);
		MemberTaskRewardGrant fresh = new MemberTaskRewardGrant();
		fresh.setStatus("granted");
		fresh.setRetryCount(2);
		when(memberTaskRewardGrantMapper.selectById(5L)).thenReturn(fresh);

		var result = service.retry(1L, 10L, 5L, locale);

		assertThat(result.get("status")).isEqualTo("granted");
		assertThat(result.get("retry_count")).isEqualTo(2);
		assertThat(result.get("attempt_result")).isEqualTo("granted");
		verify(memberTaskRewardGrantService).retryAdminFailedGrant(grant, completion);
	}

	@Test
	void retry_notFailed_throws422() {
		when(memberTaskMapper.selectOne(any(QueryWrapper.class))).thenReturn(new MemberTask());
		MemberTaskRewardGrant grant = new MemberTaskRewardGrant();
		grant.setCompletionId(3L);
		grant.setStatus("granted");
		when(memberTaskRewardGrantMapper.selectOne(any(QueryWrapper.class))).thenReturn(grant);
		when(memberTaskCompletionMapper.selectById(3L)).thenReturn(new MemberTaskCompletion());
		when(messageSource.getMessage(
						eq("promotions.member_task.grant_retry_not_allowed"),
						eq(null),
						eq("当前奖励不可补发"),
						eq(Locale.US)))
				.thenReturn("This reward cannot be retried");

		assertThatThrownBy(() -> service.retry(1L, 10L, 5L, Locale.US))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("This reward cannot be retried");
	}

	@Test
	void retry_grantMissing_throws404() {
		when(memberTaskMapper.selectOne(any(QueryWrapper.class))).thenReturn(new MemberTask());
		when(memberTaskRewardGrantMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
		when(messageSource.getMessage(
						eq("promotions.member_task.grant_not_found"),
						eq(null),
						eq("发奖记录不存在"),
						any(Locale.class)))
				.thenReturn("发奖记录不存在");

		assertThatThrownBy(() -> service.retry(1L, 10L, 99L, Locale.SIMPLIFIED_CHINESE))
				.isInstanceOf(ResourceException.class);
	}
}
