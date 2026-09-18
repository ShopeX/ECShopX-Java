package cn.shopex.ecshopx.promotions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.cron.CompanysRecordStatisticsRedisPort;
import cn.shopex.ecshopx.common.cron.SalespersonStatisticsCronLogKind;
import cn.shopex.ecshopx.common.cron.SalespersonStatisticsCronLogPort;
import cn.shopex.ecshopx.promotions.domain.SalespersonActiveArticleStatistics;
import cn.shopex.ecshopx.promotions.mapper.SalespersonActiveArticleStatisticsMapper;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SalespersonActiveArticleRecordRunnerImplTest {

	@Mock
	private CompanysRecordStatisticsRedisPort recordStatsRedis;

	@Mock
	private ShopSalespersonMapper shopSalespersonMapper;

	@Mock
	private SalespersonActiveArticleStatisticsMapper activeArticleStatisticsMapper;

	@Mock
	private SalespersonStatisticsCronLogPort cronLog;

	@InjectMocks
	private SalespersonActiveArticleRecordRunnerImpl runner;

	@Test
	@DisplayName("§3.3-3 无导购行")
	void empty() {
		when(shopSalespersonMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
		int r = runner.runActiveArticleBlock(20200101);
		assertThat(r).isEqualTo(0);
	}

	@Test
	@DisplayName("§3.3-4-C try 内异常不冒泡")
	void swallow() {
		ShopSalesperson sp = new ShopSalesperson();
		sp.setCompanyId(1L);
		sp.setSalespersonId(2L);
		when(shopSalespersonMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sp));
		when(recordStatsRedis.hgetall(
						"ActiveArticleForwardTimes:Company:1:Salesperson:2"))
				.thenThrow(new RuntimeException("r"));
		int r = runner.runActiveArticleBlock(20200101);
		assertThat(r).isEqualTo(1);
		verify(cronLog, times(1))
				.debugStart(SalespersonStatisticsCronLogKind.ACTIVE_ARTICLE_FORWARD, 1L, 2L);
		verify(cronLog, times(1))
				.debugError(
						eq(SalespersonStatisticsCronLogKind.ACTIVE_ARTICLE_FORWARD),
						eq(1L),
						eq(2L),
						any(RuntimeException.class));
		verify(cronLog, times(1))
				.debugEnd(SalespersonStatisticsCronLogKind.ACTIVE_ARTICLE_FORWARD, 1L, 2L);
	}

	@Test
	@DisplayName("§3.4～§3.5 Redis 多 field 求和并 INSERT")
	void sumAndInsert() {
		ShopSalesperson sp = new ShopSalesperson();
		sp.setCompanyId(1L);
		sp.setSalespersonId(2L);
		when(shopSalespersonMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sp));
		when(recordStatsRedis.hgetall("ActiveArticleForwardTimes:Company:1:Salesperson:2"))
				.thenReturn(Map.of("a", "3", "b", "7"));
		when(activeArticleStatisticsMapper.selectOne(any(Wrapper.class))).thenReturn(null);
		int r = runner.runActiveArticleBlock(20200101);
		assertThat(r).isEqualTo(1 + 1);
		ArgumentCaptor<SalespersonActiveArticleStatistics> cap =
				ArgumentCaptor.forClass(SalespersonActiveArticleStatistics.class);
		verify(activeArticleStatisticsMapper, times(1)).insert(cap.capture());
		assertThat(cap.getValue().getDataValue()).isEqualTo(10);
		assertThat(cap.getValue().getAddDate()).isEqualTo(20200101);
	}

	@Test
	@DisplayName("§3.4-2～3 §3.5-5 Hash 空仍 INSERT data_value=0")
	void emptyHashInsertsZero() {
		ShopSalesperson sp = new ShopSalesperson();
		sp.setCompanyId(1L);
		sp.setSalespersonId(2L);
		when(shopSalespersonMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sp));
		when(recordStatsRedis.hgetall("ActiveArticleForwardTimes:Company:1:Salesperson:2"))
				.thenReturn(Map.of());
		when(activeArticleStatisticsMapper.selectOne(any(Wrapper.class))).thenReturn(null);
		int r = runner.runActiveArticleBlock(20200101);
		assertThat(r).isEqualTo(1 + 1);
		ArgumentCaptor<SalespersonActiveArticleStatistics> cap =
				ArgumentCaptor.forClass(SalespersonActiveArticleStatistics.class);
		verify(activeArticleStatisticsMapper, times(1)).insert(cap.capture());
		assertThat(cap.getValue().getDataValue()).isEqualTo(0);
	}

	@Test
	@DisplayName("§3.5-4 已存在行不 INSERT")
	void existingSkips() {
		ShopSalesperson sp = new ShopSalesperson();
		sp.setCompanyId(1L);
		sp.setSalespersonId(2L);
		when(shopSalespersonMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sp));
		when(recordStatsRedis.hgetall("ActiveArticleForwardTimes:Company:1:Salesperson:2"))
				.thenReturn(Map.of("x", "9"));
		SalespersonActiveArticleStatistics existing = new SalespersonActiveArticleStatistics();
		existing.setId(1L);
		existing.setDataValue(1);
		when(activeArticleStatisticsMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
		int r = runner.runActiveArticleBlock(20200101);
		assertThat(r).isEqualTo(1 + 0);
		verify(activeArticleStatisticsMapper, never()).insert(any(SalespersonActiveArticleStatistics.class));
	}

	@Test
	@DisplayName("§3.3-4-A/D 活动转发 debug 起止")
	void debugTraces() {
		ShopSalesperson sp = new ShopSalesperson();
		sp.setCompanyId(1L);
		sp.setSalespersonId(2L);
		when(shopSalespersonMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sp));
		when(recordStatsRedis.hgetall("ActiveArticleForwardTimes:Company:1:Salesperson:2"))
				.thenReturn(Map.of());
		when(activeArticleStatisticsMapper.selectOne(any(Wrapper.class))).thenReturn(null);
		runner.runActiveArticleBlock(20200101);
		verify(cronLog, times(1))
				.debugStart(SalespersonStatisticsCronLogKind.ACTIVE_ARTICLE_FORWARD, 1L, 2L);
		verify(cronLog, never())
				.debugError(
						org.mockito.ArgumentMatchers.any(),
						org.mockito.ArgumentMatchers.anyLong(),
						org.mockito.ArgumentMatchers.anyLong(),
						org.mockito.ArgumentMatchers.any());
		verify(cronLog, times(1))
				.debugEnd(SalespersonStatisticsCronLogKind.ACTIVE_ARTICLE_FORWARD, 1L, 2L);
	}
}
