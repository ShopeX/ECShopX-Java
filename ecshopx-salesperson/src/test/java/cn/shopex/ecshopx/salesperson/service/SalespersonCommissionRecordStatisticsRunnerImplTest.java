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
class SalespersonCommissionRecordStatisticsRunnerImplTest {

	@Mock
	private CompanysRecordStatisticsRedisPort recordStatsRedis;

	@Mock
	private ShopSalespersonMapper shopSalespersonMapper;

	@Mock
	private SalespersonStatisticsMapper salespersonStatisticsMapper;

	@Mock
	private SalespersonStatisticsCronLogPort cronLog;

	@InjectMocks
	private SalespersonCommissionRecordStatisticsRunnerImpl runner;

	@Test
	@DisplayName("§5.2 无导购行：仅块级起止，processed=0")
	void empty() {
		when(shopSalespersonMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
		int r = runner.runCommissionBlock(20200101);
		assertThat(r).isEqualTo(0);
		verify(cronLog, times(1))
				.debugStart(SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_COMMISSION, 0L, 0L);
		verify(cronLog, times(1))
				.debugEnd(SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_COMMISSION, 0L, 0L);
	}

	@Test
	@DisplayName("§6.2–6.3 Redis get 有值时写入 newGuestDivided，键与 PHP 一致")
	void getInserts() {
		ShopSalesperson sp = new ShopSalesperson();
		sp.setCompanyId(1L);
		sp.setSalespersonId(2L);
		when(shopSalespersonMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sp));
		when(recordStatsRedis.get("Member:Salesperson:Commission:2:Company:1:20200101"))
				.thenReturn("9");
		when(salespersonStatisticsMapper.selectOne(any(Wrapper.class))).thenReturn(null);
		int r = runner.runCommissionBlock(20200101);
		assertThat(r).isEqualTo(1 + 1);
		ArgumentCaptor<SalespersonStatistics> cap = ArgumentCaptor.forClass(SalespersonStatistics.class);
		verify(salespersonStatisticsMapper, times(1)).insert(cap.capture());
		assertThat(cap.getValue().getStatisticTitle()).isEqualTo("newGuestDivided");
		assertThat(cap.getValue().getStatisticType()).isEqualTo("member");
		assertThat(cap.getValue().getDataValue()).isEqualTo(9);
	}

	@Test
	@DisplayName("§6.2 GET 缺键为 0 仍可 INSERT 一行")
	void missingKey() {
		ShopSalesperson sp = new ShopSalesperson();
		sp.setCompanyId(1L);
		sp.setSalespersonId(2L);
		when(shopSalespersonMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sp));
		when(recordStatsRedis.get("Member:Salesperson:Commission:2:Company:1:20200101"))
				.thenReturn(null);
		when(salespersonStatisticsMapper.selectOne(any(Wrapper.class))).thenReturn(null);
		int r = runner.runCommissionBlock(20200101);
		assertThat(r).isEqualTo(1 + 1);
		ArgumentCaptor<SalespersonStatistics> cap = ArgumentCaptor.forClass(SalespersonStatistics.class);
		verify(salespersonStatisticsMapper, times(1)).insert(cap.capture());
		assertThat(cap.getValue().getDataValue()).isEqualTo(0);
	}

	@Test
	@DisplayName("§4.7 已存在不重复 INSERT")
	void existingNoInsert() {
		ShopSalesperson sp = new ShopSalesperson();
		sp.setCompanyId(1L);
		sp.setSalespersonId(2L);
		when(shopSalespersonMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sp));
		when(recordStatsRedis.get(any())).thenReturn("1");
		SalespersonStatistics existing = new SalespersonStatistics();
		existing.setId(9L);
		when(salespersonStatisticsMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
		int r = runner.runCommissionBlock(20200101);
		assertThat(r).isEqualTo(1);
		verify(salespersonStatisticsMapper, never()).insert(any(SalespersonStatistics.class));
	}

	@Test
	@DisplayName("§5.4 recordSalespersonCommissionStatistics 抛错不冒泡")
	void swallow() {
		ShopSalesperson sp = new ShopSalesperson();
		sp.setCompanyId(1L);
		sp.setSalespersonId(2L);
		when(shopSalespersonMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sp));
		when(recordStatsRedis.get(any())).thenThrow(new RuntimeException("r"));
		int r = runner.runCommissionBlock(20200101);
		assertThat(r).isEqualTo(1);
		verify(cronLog, times(1))
				.debugError(
						eq(SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_COMMISSION),
						eq(1L),
						eq(2L),
						any(RuntimeException.class));
	}

	@Test
	@DisplayName("DEBUG：块起止与逐行 kind=SHOPPING_GUIDE_COMMISSION")
	void logKinds() {
		ShopSalesperson sp = new ShopSalesperson();
		sp.setCompanyId(1L);
		sp.setSalespersonId(2L);
		when(shopSalespersonMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sp));
		when(recordStatsRedis.get(any())).thenReturn("0");
		when(salespersonStatisticsMapper.selectOne(any(Wrapper.class))).thenReturn(null);
		runner.runCommissionBlock(20200101);
		verify(cronLog, times(1))
				.debugStart(SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_COMMISSION, 0L, 0L);
		verify(cronLog, times(1))
				.debugStart(SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_COMMISSION, 1L, 2L);
	}

	@Test
	@DisplayName("§6.3 分润键与 block 的 Ymd 入参一致；已存在行则 insert=0")
	void dateUsesBlockParam() {
		when(recordStatsRedis.get("Member:Salesperson:Commission:2:Company:1:20200101"))
				.thenReturn("1");
		when(salespersonStatisticsMapper.selectOne(any(Wrapper.class)))
				.thenReturn(new SalespersonStatistics());
		int r = runner.recordSalespersonCommissionStatistics(1L, 2L, 20200101);
		assertThat(r).isEqualTo(0);
		verify(recordStatsRedis, times(1)).get("Member:Salesperson:Commission:2:Company:1:20200101");
	}

	@Test
	@DisplayName("plan §5 / analysis §6.1：date 为 null 时 Redis get 键中日段为昨日 Ymd（Asia/Shanghai）")
	void nullDateRedisKeyUsesYesterdayYmd() {
		String yesterday = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
				.toLocalDate()
				.minusDays(1)
				.format(DateTimeFormatter.BASIC_ISO_DATE);
		String expectedKey = "Member:Salesperson:Commission:2:Company:1:" + yesterday;
		when(recordStatsRedis.get(expectedKey)).thenReturn("0");
		when(salespersonStatisticsMapper.selectOne(any(Wrapper.class)))
				.thenReturn(new SalespersonStatistics());
		runner.recordSalespersonCommissionStatistics(1L, 2L, null);
		verify(recordStatsRedis, times(1)).get(expectedKey);
	}
}
