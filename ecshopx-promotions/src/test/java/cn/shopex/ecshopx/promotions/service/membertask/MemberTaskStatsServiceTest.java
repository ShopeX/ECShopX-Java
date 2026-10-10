package cn.shopex.ecshopx.promotions.service.membertask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.promotions.domain.membertask.MemberTask;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskStatsMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class MemberTaskStatsServiceTest {

	@Mock
	private MemberTaskMapper memberTaskMapper;

	@Mock
	private MemberTaskStatsMapper memberTaskStatsMapper;

	@Mock
	private MessageSource messageSource;

	private MemberTaskStatsService service;

	@BeforeEach
	void setUp() {
		service = new MemberTaskStatsService(memberTaskMapper, memberTaskStatsMapper, messageSource);
	}

	@Test
	void stats_returnsCounts() {
		when(memberTaskMapper.selectOne(any(QueryWrapper.class))).thenReturn(new MemberTask());
		when(memberTaskStatsMapper.countCompletions(1L, 10L)).thenReturn(5L);
		when(memberTaskStatsMapper.countCompletionUsers(1L, 10L)).thenReturn(3L);
		when(memberTaskStatsMapper.countGrantsByStatus(1L, 10L))
				.thenReturn(List.of(Map.of("status", "pending", "cnt", 2L)));

		var data = service.stats(1L, 10L, Locale.SIMPLIFIED_CHINESE);

		assertThat(data.get("completion_count")).isEqualTo(5L);
		assertThat(data.get("completion_user_count")).isEqualTo(3L);
		@SuppressWarnings("unchecked")
		Map<String, Long> grants = (Map<String, Long>) data.get("grant_status_counts");
		assertThat(grants.get("pending")).isEqualTo(2L);
	}
}
