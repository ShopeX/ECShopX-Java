package cn.shopex.ecshopx.promotions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.promotions.domain.LuckyDrawActivity;
import cn.shopex.ecshopx.promotions.mapper.LuckyDrawActivityMapper;
import cn.shopex.ecshopx.promotions.service.multilang.LuckyDrawActivityOutsideMultiLangReadService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TurntableActivityListServiceListOrderTest {

	@Mock
	private LuckyDrawActivityMapper luckyDrawActivityMapper;
	@Mock
	private LuckyDrawActivityOutsideMultiLangReadService luckyDrawActivityOutsideMultiLangReadService;

	private TurntableActivityListService service;

	@BeforeAll
	static void initTableInfo() {
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(new MybatisConfiguration(), ""), LuckyDrawActivity.class);
	}

	@BeforeEach
	void setUp() {
		service = new TurntableActivityListService(luckyDrawActivityMapper, luckyDrawActivityOutsideMultiLangReadService);
	}

	@Test
	void getDrawActivityList_defaultOrder_sortsByLifecycleThenCreatedDesc() {
		when(luckyDrawActivityMapper.selectCount(any())).thenReturn(0L);
		when(luckyDrawActivityMapper.selectPage(any(Page.class), any())).thenAnswer(inv -> inv.getArgument(0));

		service.getDrawActivityList(1L, "1", "10", null, null, null, "zh-CN");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<LambdaQueryWrapper<LuckyDrawActivity>> cap = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(luckyDrawActivityMapper).selectPage(any(), cap.capture());
		String sql = cap.getValue().getCustomSqlSegment();
		assertThat(sql).contains("CASE WHEN begin_time").contains("created DESC");
	}

	@Test
	void getDrawActivityList_statusFilter_orderByCreatedOnly() {
		when(luckyDrawActivityMapper.selectCount(any())).thenReturn(0L);
		when(luckyDrawActivityMapper.selectPage(any(Page.class), any())).thenAnswer(inv -> inv.getArgument(0));

		service.getDrawActivityList(1L, "1", "10", null, null, "online", "zh-CN");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<LambdaQueryWrapper<LuckyDrawActivity>> cap = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(luckyDrawActivityMapper).selectPage(any(), cap.capture());
		String sql = cap.getValue().getCustomSqlSegment();
		assertThat(sql).doesNotContain("CASE WHEN begin_time");
		assertThat(sql).containsIgnoringCase("created");
	}
}