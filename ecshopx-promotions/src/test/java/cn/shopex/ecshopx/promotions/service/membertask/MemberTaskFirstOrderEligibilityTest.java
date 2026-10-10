package cn.shopex.ecshopx.promotions.service.membertask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.promotions.mapper.MemberTaskCompletionMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberTaskFirstOrderEligibilityTest {

	@Mock
	private MemberTaskCompletionMapper memberTaskCompletionMapper;

	private MemberTaskFirstOrderCompletionPolicy policy;

	@BeforeEach
	void setUp() {
		policy = new MemberTaskFirstOrderCompletionPolicy(memberTaskCompletionMapper);
	}

	@Test
	void noCompletion_allowsInsert() {
		when(memberTaskCompletionMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
		assertThat(policy.hasActiveFirstOrderCompletion(10L, 7L)).isFalse();
	}

	@Test
	void voidedOnly_allowsInsert() {
		when(memberTaskCompletionMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
		assertThat(policy.hasActiveFirstOrderCompletion(10L, 7L)).isFalse();
	}

	@Test
	void activeCompletion_rejectsSecondOrder() {
		when(memberTaskCompletionMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);
		assertThat(policy.hasActiveFirstOrderCompletion(10L, 7L)).isTrue();
	}
}
