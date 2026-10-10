package cn.shopex.ecshopx.promotions.service.membertask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTask;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class MemberTaskCheckoutServiceTest {

	@Mock
	private MemberTaskMapper memberTaskMapper;

	@Mock
	private MemberTaskFirstOrderCompletionPolicy firstOrderCompletionPolicy;

	@Mock
	private MessageSource messageSource;

	private MemberTaskCheckoutService service;

	@BeforeEach
	void setUp() {
		service = new MemberTaskCheckoutService(memberTaskMapper, firstOrderCompletionPolicy, messageSource);
	}

	@Test
	void missingTaskId_writesZero() {
		NormalOrderCreateParams p = new NormalOrderCreateParams();
		p.getOrderData().put("company_id", 1L);
		service.applyBeforePersist(p);
		assertThat(p.getOrderData().get("member_task_id")).isEqualTo(0L);
	}

	@Test
	void unknownTask_throws() {
		when(memberTaskMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
		when(messageSource.getMessage(
						eq("promotions.member_task.checkout_task_invalid"),
						eq(null),
						eq("当前任务不可用于下单"),
						any(Locale.class)))
				.thenReturn("当前任务不可用于下单");
		NormalOrderCreateParams p = new NormalOrderCreateParams();
		p.getOrderData().put("company_id", 1L);
		p.getOrderData().put("user_id", 7L);
		p.getParams().put("member_task_id", 10L);
		assertThatThrownBy(() -> service.applyBeforePersist(p))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("当前任务不可用于下单");
	}

	@Test
	void inProgressFirstOrder_writesTaskId() {
		long now = System.currentTimeMillis() / 1000L;
		MemberTask task = new MemberTask();
		task.setTaskId(10L);
		task.setTypeDetail("first_order");
		task.setFrequencyType("once");
		task.setPublished(1);
		task.setOffShelf(0);
		task.setBeginTime((int) (now - 10));
		task.setEndTime((int) (now + 1000));
		when(memberTaskMapper.selectOne(any(QueryWrapper.class))).thenReturn(task);
		when(firstOrderCompletionPolicy.hasActiveFirstOrderCompletion(10L, 7L)).thenReturn(false);
		NormalOrderCreateParams p = new NormalOrderCreateParams();
		p.getOrderData().put("company_id", 1L);
		p.getOrderData().put("user_id", 7L);
		p.getParams().put("member_task_id", 10L);
		service.applyBeforePersist(p);
		assertThat(p.getOrderData().get("member_task_id")).isEqualTo(10L);
	}
}
