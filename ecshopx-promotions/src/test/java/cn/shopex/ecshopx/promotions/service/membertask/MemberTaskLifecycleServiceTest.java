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
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskDisplayStatus;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class MemberTaskLifecycleServiceTest {

	@Mock
	private MemberTaskMapper memberTaskMapper;

	@Mock
	private MessageSource messageSource;

	private MemberTaskLifecycleService service;

	@BeforeEach
	void setUp() {
		service = new MemberTaskLifecycleService(memberTaskMapper, messageSource);
	}

	@Test
	void publish_ok_pendingStart() {
		long now = 1_000L;
		MemberTask row = draft((int) (now + 500), (int) (now + 2000));
		when(memberTaskMapper.selectOne(any(QueryWrapper.class))).thenReturn(row);
		when(memberTaskMapper.update(any(), any(UpdateWrapper.class))).thenReturn(1);

		Map<String, Object> out = service.publish(1L, 10L, now, Locale.SIMPLIFIED_CHINESE);

		assertThat(out.get("published")).isEqualTo(1);
		assertThat(out.get("display_status")).isEqualTo(MemberTaskDisplayStatus.PENDING_START);
		verify(memberTaskMapper).update(any(), any(UpdateWrapper.class));
	}

	@Test
	void publish_rejectsNonDraft_en() {
		long now = 1_000L;
		MemberTask row = draft((int) (now + 500), (int) (now + 2000));
		row.setPublished(1);
		when(memberTaskMapper.selectOne(any(QueryWrapper.class))).thenReturn(row);
		when(messageSource.getMessage(
						eq("promotions.member_task.publish_not_draft"),
						eq(null),
						eq("仅待发布状态可发布"),
						eq(Locale.US)))
				.thenReturn("Only draft tasks can be published");

		assertThatThrownBy(() -> service.publish(1L, 10L, now, Locale.US))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Only draft tasks can be published");
	}

	@Test
	void publish_rejectsExpired() {
		long now = 2_000L;
		MemberTask row = draft(100, 500);
		when(memberTaskMapper.selectOne(any(QueryWrapper.class))).thenReturn(row);
		when(messageSource.getMessage(
						eq("promotions.member_task.publish_expired"),
						eq(null),
						eq("任务已过期，无法发布"),
						eq(Locale.SIMPLIFIED_CHINESE)))
				.thenReturn("任务已过期，无法发布");

		assertThatThrownBy(() -> service.publish(1L, 10L, now, Locale.SIMPLIFIED_CHINESE))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("任务已过期，无法发布");
	}

	@Test
	void offShelf_ok() {
		long now = 1_500L;
		MemberTask row = draft(1_000, 3_000);
		row.setPublished(1);
		when(memberTaskMapper.selectOne(any(QueryWrapper.class))).thenReturn(row);
		when(memberTaskMapper.update(any(), any(UpdateWrapper.class))).thenReturn(1);

		Map<String, Object> out = service.offShelf(1L, 10L, now, Locale.SIMPLIFIED_CHINESE);

		assertThat(out.get("off_shelf")).isEqualTo(1);
		assertThat(out.get("display_status")).isEqualTo(MemberTaskDisplayStatus.EXPIRED);
	}

	@Test
	void offShelf_rejectsDraft() {
		long now = 1_000L;
		MemberTask row = draft((int) (now + 100), (int) (now + 2000));
		when(memberTaskMapper.selectOne(any(QueryWrapper.class))).thenReturn(row);
		when(messageSource.getMessage(
						eq("promotions.member_task.off_shelf_not_allowed"),
						eq(null),
						eq("当前状态不可下架"),
						eq(Locale.SIMPLIFIED_CHINESE)))
				.thenReturn("当前状态不可下架");

		assertThatThrownBy(() -> service.offShelf(1L, 10L, now, Locale.SIMPLIFIED_CHINESE))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("当前状态不可下架");
	}

	@Test
	void publish_notFound() {
		when(memberTaskMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
		when(messageSource.getMessage(
						eq("promotions.member_task.task_not_found"),
						eq(null),
						eq("任务不存在"),
						eq(Locale.SIMPLIFIED_CHINESE)))
				.thenReturn("任务不存在");

		assertThatThrownBy(() -> service.publish(1L, 99L, 1_000L, Locale.SIMPLIFIED_CHINESE))
				.isInstanceOf(ResourceException.class)
				.hasMessage("任务不存在");
	}

	private static MemberTask draft(int begin, int end) {
		MemberTask t = new MemberTask();
		t.setTaskId(10L);
		t.setCompanyId(1L);
		t.setName("t");
		t.setBeginTime(begin);
		t.setEndTime(end);
		t.setPublished(0);
		t.setOffShelf(0);
		return t;
	}
}
