package cn.shopex.ecshopx.salesperson.service;

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
import cn.shopex.ecshopx.salesperson.domain.SalespersonStatistics;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.SalespersonStatisticsMapper;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SalespersonPopularizeRecordStatisticsRunnerImplTest {

	@Mock
	private CompanysRecordStatisticsRedisPort recordStatsRedis;

	@Mock
	private ShopSalespersonMapper shopSalespersonMapper;

	@Mock
	private SalespersonStatisticsMapper salespersonStatisticsMapper;

	@Mock
	private SalespersonStatisticsCronLogPort cronLog;

	@InjectMocks
	private SalespersonPopularizeRecordStatisticsRunnerImpl runner;

	@Test
	@DisplayName("§5.1·§5.2 无导购行：块级起止，processed=0")
	void empty() {
		when(shopSalespersonMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
		int r = runner.runPopularizeBlock(20200101);
		assertThat(r).isEqualTo(0);
		verify(cronLog, times(1))
				.debugStart(SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_POPULARIZE, 0L, 0L);
		verify(cronLog, times(1))
				.debugEnd(SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_POPULARIZE, 0L, 0L);
	}

	@Test
	@DisplayName("§6.2·§6.4·§6.5·§6.6：GET 有值写入 salesCommission/member；§6.3 键与约定一致")
	void getInserts() {
		ShopSalesperson sp = new ShopSalesperson();
		sp.setCompanyId(1L);
		sp.setSalespersonId(2L);
		when(shopSalespersonMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sp));
		when(recordStatsRedis.get("Member:Salesperson:Popularize:2:Company:1:20200101"))
				.thenReturn("9");
		when(salespersonStatisticsMapper.selectOne(any(Wrapper.class))).thenReturn(null);
		int r = runner.runPopularizeBlock(20200101);
		assertThat(r).isEqualTo(1 + 1);
		ArgumentCaptor<SalespersonStatistics> cap = ArgumentCaptor.forClass(SalespersonStatistics.class);
		verify(salespersonStatisticsMapper, times(1)).insert(cap.capture());
		assertThat(cap.getValue().getStatisticTitle()).isEqualTo("salesCommission");
		assertThat(cap.getValue().getStatisticType()).isEqualTo("member");
		assertThat(cap.getValue().getDataValue()).isEqualTo(9);
	}

	@Test
	@DisplayName("§6.2 GET 缺键为 0 仍 INSERT 一行")
	void missingKey() {
		ShopSalesperson sp = new ShopSalesperson();
		sp.setCompanyId(1L);
		sp.setSalespersonId(2L);
		when(shopSalespersonMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sp));
		when(recordStatsRedis.get("Member:Salesperson:Popularize:2:Company:1:20200101"))
				.thenReturn(null);
		when(salespersonStatisticsMapper.selectOne(any(Wrapper.class))).thenReturn(null);
		int r = runner.runPopularizeBlock(20200101);
		assertThat(r).isEqualTo(1 + 1);
		ArgumentCaptor<SalespersonStatistics> cap = ArgumentCaptor.forClass(SalespersonStatistics.class);
		verify(salespersonStatisticsMapper, times(1)).insert(cap.capture());
		assertThat(cap.getValue().getDataValue()).isEqualTo(0);
	}

	@Test
	@DisplayName("§7.1–§7.4 已存在不重复 INSERT")
	void existingNoInsert() {
		ShopSalesperson sp = new ShopSalesperson();
		sp.setCompanyId(1L);
		sp.setSalespersonId(2L);
		when(shopSalespersonMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sp));
		when(recordStatsRedis.get(any())).thenReturn("1");
		SalespersonStatistics existing = new SalespersonStatistics();
		existing.setId(9L);
		when(salespersonStatisticsMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
		int r = runner.runPopularizeBlock(20200101);
		assertThat(r).isEqualTo(1);
		verify(salespersonStatisticsMapper, never()).insert(any(SalespersonStatistics.class));
	}

	@Test
	@DisplayName("§5.4 recordSalespersonPopularizeStatistics 抛错不冒泡")
	void swallow() {
		ShopSalesperson sp = new ShopSalesperson();
		sp.setCompanyId(1L);
		sp.setSalespersonId(2L);
		when(shopSalespersonMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sp));
		when(recordStatsRedis.get(any())).thenThrow(new RuntimeException("r"));
		int r = runner.runPopularizeBlock(20200101);
		assertThat(r).isEqualTo(1);
		verify(cronLog, times(1))
				.debugError(
						eq(SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_POPULARIZE),
						eq(1L),
						eq(2L),
						any(RuntimeException.class));
	}

	@Test
	@DisplayName("§5.3·§5.5 DEBUG 块与逐行 kind=SHOPPING_GUIDE_POPULARIZE")
	void logKinds() {
		ShopSalesperson sp = new ShopSalesperson();
		sp.setCompanyId(1L);
		sp.setSalespersonId(2L);
		when(shopSalespersonMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sp));
		when(recordStatsRedis.get(any())).thenReturn("0");
		when(salespersonStatisticsMapper.selectOne(any(Wrapper.class))).thenReturn(null);
		runner.runPopularizeBlock(20200101);
		verify(cronLog, times(1))
				.debugStart(SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_POPULARIZE, 0L, 0L);
		verify(cronLog, times(1))
				.debugStart(SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_POPULARIZE, 1L, 2L);
	}

	@Test
	@DisplayName("§5.6：return 导购行数 + inserts；与块出口对位")
	void section56Return() {
		ShopSalesperson sp = new ShopSalesperson();
		sp.setCompanyId(1L);
		sp.setSalespersonId(2L);
		when(shopSalespersonMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sp));
		when(recordStatsRedis.get(any())).thenReturn("3");
		when(salespersonStatisticsMapper.selectOne(any(Wrapper.class))).thenReturn(null);
		int r = runner.runPopularizeBlock(20200101);
		assertThat(r).isEqualTo(2);
	}

	@Test
	@DisplayName("§6.1：date 为 null 时 GET 键中日段为昨日 Ymd（Asia/Shanghai）")
	void nullDateRedisKeyUsesYesterdayYmd() {
		String yesterday = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
				.toLocalDate()
				.minusDays(1)
				.format(DateTimeFormatter.BASIC_ISO_DATE);
		String expectedKey = "Member:Salesperson:Popularize:2:Company:1:" + yesterday;
		when(recordStatsRedis.get(expectedKey)).thenReturn("0");
		when(salespersonStatisticsMapper.selectOne(any(Wrapper.class)))
				.thenReturn(new SalespersonStatistics());
		runner.recordSalespersonPopularizeStatistics(1L, 2L, null);
		verify(recordStatsRedis, times(1)).get(expectedKey);
	}
}
