package cn.shopex.ecshopx.salesperson.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.cron.SalespersonGiveCouponsCountPort;
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
class SalespersonGiveCouponsRecordStatisticsRunnerImplTest {

	@Mock
	private ShopSalespersonMapper shopSalespersonMapper;

	@Mock
	private SalespersonStatisticsMapper salespersonStatisticsMapper;

	@Mock
	private SalespersonStatisticsCronLogPort cronLog;

	@Mock
	private SalespersonGiveCouponsCountPort giveCouponsCountPort;

	@InjectMocks
	private SalespersonGiveCouponsRecordStatisticsRunnerImpl runner;

	@Test
	@DisplayName("§5.1·§5.2 无导购行：与 PHP 一致不进入块，无 SHOPPING_GUIDE_GIVE_COUPONS debug，processed=0")
	void empty() {
		when(shopSalespersonMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
		int r = runner.runGiveCouponsBlock(20200101);
		assertThat(r).isEqualTo(0);
		verify(cronLog, never())
				.debugStart(
						eq(SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_GIVE_COUPONS), anyLong(), anyLong());
		verify(cronLog, never())
				.debugEnd(
						eq(SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_GIVE_COUPONS), anyLong(), anyLong());
	}

	@Test
	@DisplayName("§6.2 COUNT 有值写入 salespersonGiveCoupons/member")
	void countAndInsert() {
		ShopSalesperson sp = new ShopSalesperson();
		sp.setCompanyId(1L);
		sp.setSalespersonId(2L);
		when(shopSalespersonMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sp));
		when(giveCouponsCountPort.countSuccessRowsForDate(1L, 2L, 20200101)).thenReturn(5);
		when(salespersonStatisticsMapper.selectOne(any(Wrapper.class))).thenReturn(null);
		int r = runner.runGiveCouponsBlock(20200101);
		assertThat(r).isEqualTo(1 + 1);
		ArgumentCaptor<SalespersonStatistics> cap = ArgumentCaptor.forClass(SalespersonStatistics.class);
		verify(salespersonStatisticsMapper, times(1)).insert(cap.capture());
		assertThat(cap.getValue().getStatisticTitle()).isEqualTo("salespersonGiveCoupons");
		assertThat(cap.getValue().getStatisticType()).isEqualTo("member");
		assertThat(cap.getValue().getDataValue()).isEqualTo(5);
	}

	@Test
	@DisplayName("§7.1–§7.4 已存在不重复 INSERT")
	void existingNoInsert() {
		ShopSalesperson sp = new ShopSalesperson();
		sp.setCompanyId(1L);
		sp.setSalespersonId(2L);
		when(shopSalespersonMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sp));
		when(giveCouponsCountPort.countSuccessRowsForDate(1L, 2L, 20200101)).thenReturn(3);
		SalespersonStatistics existing = new SalespersonStatistics();
		existing.setId(1L);
		when(salespersonStatisticsMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
		int r = runner.runGiveCouponsBlock(20200101);
		assertThat(r).isEqualTo(1);
		verify(salespersonStatisticsMapper, never()).insert(any(SalespersonStatistics.class));
	}

	@Test
	@DisplayName("§5.3 内层异常吞掉、不冒泡段二外")
	void swallow() {
		ShopSalesperson sp = new ShopSalesperson();
		sp.setCompanyId(1L);
		sp.setSalespersonId(2L);
		when(shopSalespersonMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sp));
		when(giveCouponsCountPort.countSuccessRowsForDate(1L, 2L, 20200101))
				.thenThrow(new RuntimeException("r"));
		int r = runner.runGiveCouponsBlock(20200101);
		assertThat(r).isEqualTo(1);
		verify(cronLog, times(1))
				.debugError(
						eq(SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_GIVE_COUPONS),
						eq(1L),
						eq(2L),
						any(RuntimeException.class));
	}

	@Test
	@DisplayName("§5.3 kind=SHOPPING_GUIDE_GIVE_COUPONS 含逐行起止与块尾")
	void logKinds() {
		ShopSalesperson sp = new ShopSalesperson();
		sp.setCompanyId(1L);
		sp.setSalespersonId(2L);
		when(shopSalespersonMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sp));
		when(giveCouponsCountPort.countSuccessRowsForDate(1L, 2L, 20200101)).thenReturn(0);
		when(salespersonStatisticsMapper.selectOne(any(Wrapper.class))).thenReturn(null);
		runner.runGiveCouponsBlock(20200101);
		verify(cronLog, times(1))
				.debugStart(SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_GIVE_COUPONS, 0L, 0L);
		verify(cronLog, times(1))
				.debugStart(SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_GIVE_COUPONS, 1L, 2L);
		verify(cronLog, times(1))
				.debugEnd(SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_GIVE_COUPONS, 1L, 2L);
		verify(cronLog, times(1))
				.debugEnd(SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_GIVE_COUPONS, 0L, 0L);
	}

	@Test
	@DisplayName("§6.1：date 为 null 时 COUNT 用昨日 Ymd")
	void nullDateUsesYesterdayYmd() {
		String yesterday = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
				.toLocalDate()
				.minusDays(1)
				.format(DateTimeFormatter.BASIC_ISO_DATE);
		int y = Integer.parseInt(yesterday);
		when(giveCouponsCountPort.countSuccessRowsForDate(1L, 2L, y)).thenReturn(0);
		when(salespersonStatisticsMapper.selectOne(any(Wrapper.class)))
				.thenReturn(new SalespersonStatistics());
		runner.recordSalespersonGiveCouponsStatistics(1L, 2L, null);
		verify(giveCouponsCountPort, times(1)).countSuccessRowsForDate(1L, 2L, y);
	}
}
