package cn.shopex.ecshopx.salesperson.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.cron.CompanysRecordStatisticsRedisPort;
import cn.shopex.ecshopx.common.cron.SalespersonStatisticsCronLogPort;
import cn.shopex.ecshopx.salesperson.domain.SalespersonStatistics;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.SalespersonStatisticsMapper;
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
class SalespersonShoppingGuideStatisticsRunnerImplTest {

	@Mock
	private CompanysRecordStatisticsRedisPort recordStatsRedis;

	@Mock
	private ShopSalespersonMapper shopSalespersonMapper;

	@Mock
	private SalespersonStatisticsMapper salespersonStatisticsMapper;

	@Mock
	private SalespersonStatisticsCronLogPort cronLog;

	@InjectMocks
	private SalespersonShoppingGuideStatisticsRunnerImpl runner;

	@Test
	@DisplayName("§3.4-3 无导购行")
	void empty() {
		when(shopSalespersonMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
		int r = runner.runShoppingGuideBlock(20200101);
		assertThat(r).isEqualTo(0);
	}

	@Test
	@DisplayName("§3.4-4-C 单导购 try 抛错不冒泡")
	void swallow() {
		ShopSalesperson sp = new ShopSalesperson();
		sp.setCompanyId(1L);
		sp.setSalespersonId(2L);
		when(shopSalespersonMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sp));
		when(recordStatsRedis.scard(anyString())).thenThrow(new RuntimeException("r"));
		int r = runner.runShoppingGuideBlock(20200101);
		assertThat(r).isEqualTo(1);
		verify(cronLog, times(1)).debugStart(1L, 2L);
		verify(cronLog, times(1)).debugError(eq(1L), eq(2L), any(RuntimeException.class));
		verify(cronLog, times(1)).debugEnd(1L, 2L);
	}

	@Test
	@DisplayName("§3.5-3 §3.6-3 newAddMember 经 SalespersonStatisticsMapper 落 companys_salesperson_statistics")
	void newAddMemberUsesSalespersonMapper() {
		ShopSalesperson sp = new ShopSalesperson();
		sp.setCompanyId(1L);
		sp.setSalespersonId(2L);
		when(shopSalespersonMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sp));
		when(recordStatsRedis.scard(anyString()))
				.thenAnswer(
						invocation -> {
							String k = invocation.getArgument(0);
							return "Member:Salesperson:2:Company:1:20200101".equals(k) ? 4L : 0L;
						});
		when(recordStatsRedis.hgetall(
						"OrderPaySalespersonStatistics:normal:1:SalespersonId:2:20200101"))
				.thenReturn(Map.of());
		when(salespersonStatisticsMapper.selectOne(any(Wrapper.class))).thenReturn(null);
		int r = runner.runShoppingGuideBlock(20200101);
		assertThat(r).isEqualTo(1 + 3);
		ArgumentCaptor<SalespersonStatistics> cap = ArgumentCaptor.forClass(SalespersonStatistics.class);
		verify(salespersonStatisticsMapper, times(3)).insert(cap.capture());
		assertThat(cap.getAllValues())
				.anyMatch(
						row ->
								"newAddMember".equals(row.getStatisticTitle())
										&& "member".equals(row.getStatisticType())
										&& row.getDataValue() == 4
										&& row.getSalespersonId() == 2L);
	}

	@Test
	@DisplayName("§3.5-5/6 缺省 Hash 按 0 落库新行")
	void missingHashKeys() {
		ShopSalesperson sp = new ShopSalesperson();
		sp.setCompanyId(1L);
		sp.setSalespersonId(2L);
		when(shopSalespersonMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sp));
		when(recordStatsRedis.scard(anyString())).thenReturn(0L);
		when(recordStatsRedis.hgetall(
						"OrderPaySalespersonStatistics:normal:1:SalespersonId:2:20200101"))
				.thenReturn(Map.of());
		when(salespersonStatisticsMapper.selectOne(any(Wrapper.class))).thenReturn(null);
		int r = runner.runShoppingGuideBlock(20200101);
		assertThat(r).isEqualTo(1 + 2);
		ArgumentCaptor<SalespersonStatistics> cap = ArgumentCaptor.forClass(SalespersonStatistics.class);
		verify(salespersonStatisticsMapper, times(2)).insert(cap.capture());
		assertThat(cap.getAllValues()).hasSize(2);
	}

	@Test
	@DisplayName("§3.4-4-A/D debug 起止被调用")
	void debugTraces() {
		ShopSalesperson sp = new ShopSalesperson();
		sp.setCompanyId(1L);
		sp.setSalespersonId(2L);
		when(shopSalespersonMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sp));
		when(recordStatsRedis.scard(anyString())).thenReturn(0L);
		when(recordStatsRedis.hgetall(
						"OrderPaySalespersonStatistics:normal:1:SalespersonId:2:20200101"))
				.thenReturn(Map.of("2_salesperson_orderPayFee", "10", "2_salesperson_orderPayNum", "3"));
		when(salespersonStatisticsMapper.selectOne(any(Wrapper.class))).thenReturn(null);
		runner.runShoppingGuideBlock(20200101);
		verify(cronLog, times(1)).debugStart(1L, 2L);
		verify(cronLog, never()).debugError(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong(), any());
		verify(cronLog, times(1)).debugEnd(1L, 2L);
	}
}
