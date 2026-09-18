package cn.shopex.ecshopx.datacube.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.datacube.domain.CompanyData;
import cn.shopex.ecshopx.datacube.mapper.CompanyDataMapper;
import cn.shopex.ecshopx.datacube.mapper.CompanyDataStatisticsMapper;
import cn.shopex.ecshopx.datacube.service.companydata.CompanyDataStatisticAsyncExecutor;
import cn.shopex.ecshopx.datacube.service.companydata.StatisticJobEnqueuePort;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class CompanyDataServiceTest {

	private static final ZoneId SH = ZoneId.of("Asia/Shanghai");

	@Mock
	private CompanysMapper companysMapper;

	@Mock
	private CompanyDataMapper companyDataMapper;

	@Mock
	private CompanyDataStatisticsMapper companyDataStatisticsMapper;

	@Mock
	private StatisticJobEnqueuePort statisticJobEnqueuePort;

	/**
	 * Constructor dependency of {@link CompanyDataStatisticAsyncExecutor}; unused when {@code sync-inline=false}.
	 */
	@Mock
	private CompanyDataService asyncExecutorLazyCompanyDataService;

	private CompanyDataStatisticAsyncExecutor companyDataStatisticAsyncExecutor;

	@Mock
	private ActivitiesMapper activitiesMapper;

	private Clock clock;
	private CompanyDataService companyDataService;

	private ListAppender<ILoggingEvent> listAppender;
	private Logger serviceLogger;

	@BeforeEach
	void setUp() {
		clock = Clock.fixed(Instant.parse("2024-06-15T01:00:00+08:00"), SH);
		companyDataStatisticAsyncExecutor = new CompanyDataStatisticAsyncExecutor(
				asyncExecutorLazyCompanyDataService, statisticJobEnqueuePort, false);
		companyDataService = new CompanyDataService(
				companysMapper,
				companyDataMapper,
				companyDataStatisticsMapper,
				companyDataStatisticAsyncExecutor,
				activitiesMapper,
				clock);
		listAppender = new ListAppender<>();
		listAppender.start();
		listAppender.list.clear();
		serviceLogger = (Logger) LoggerFactory.getLogger(CompanyDataService.class);
		serviceLogger.addAppender(listAppender);
	}

	@AfterEach
	void tear() {
		serviceLogger.detachAppender(listAppender);
		listAppender.stop();
	}

	@Nested
	class ScheduleInitStatisticA {

		@Test
		void step1_countDateIsYesterday() {
			when(companysMapper.selectList(any())).thenReturn(List.of());
			companyDataService.scheduleInitStatistic();
			verify(statisticJobEnqueuePort, never())
					.enqueue(anyLong(), any(LocalDate.class), nullable(String.class), anyLong());
		}

		@Test
		void step2_logMessage() {
			when(companysMapper.selectList(any())).thenReturn(List.of());
			companyDataService.scheduleInitStatistic();
			assertThat(listAppender.list).anyMatch(
					e -> e.getMessage() != null
							&& e.getMessage().contains("执行统计商城数据初始化脚本"));
		}

		@Test
		void step3_loadCompanies() {
			when(companysMapper.selectList(any())).thenReturn(List.of());
			companyDataService.scheduleInitStatistic();
			verify(companysMapper, times(1)).selectList(any());
		}

		@Test
		void step3_noCompany_noLoopBody() {
			when(companysMapper.selectList(any())).thenReturn(List.of());
			companyDataService.scheduleInitStatistic();
			verify(companyDataMapper, never()).selectCount(any());
			verify(companyDataMapper, never()).insert(any(CompanyData.class));
			verify(statisticJobEnqueuePort, never())
					.enqueue(anyLong(), any(LocalDate.class), nullable(String.class), anyLong());
		}

		@Test
		void step4_twoCompanies_runsAsyncTwice() {
			Companys a = new Companys();
			a.setCompanyId(1L);
			Companys b = new Companys();
			b.setCompanyId(2L);
			when(companysMapper.selectList(any())).thenReturn(List.of(a, b));
			when(companyDataMapper.selectCount(any())).thenReturn(1L);
			companyDataService.scheduleInitStatistic();
			verify(statisticJobEnqueuePort, times(1))
					.enqueue(eq(1L), eq(LocalDate.of(2024, 6, 14)), isNull(), eq(0L));
			verify(statisticJobEnqueuePort, times(1))
					.enqueue(eq(2L), eq(LocalDate.of(2024, 6, 14)), isNull(), eq(0L));
		}

		@Test
		void a41_selectCountWithNullOrderClass() {
			Companys a = new Companys();
			a.setCompanyId(3L);
			when(companysMapper.selectList(any())).thenReturn(List.of(a));
			when(companyDataMapper.selectCount(any())).thenReturn(0L);
			companyDataService.scheduleInitStatistic();
			verify(companyDataMapper, times(1)).selectCount(any());
		}

		@Test
		void a42_insertsWhenZero() {
			Companys a = new Companys();
			a.setCompanyId(4L);
			when(companysMapper.selectList(any())).thenReturn(List.of(a));
			when(companyDataMapper.selectCount(any())).thenReturn(0L);
			companyDataService.scheduleInitStatistic();
			ArgumentCaptor<CompanyData> cap = ArgumentCaptor.forClass(CompanyData.class);
			verify(companyDataMapper, times(1)).insert(cap.capture());
			assertThat(cap.getValue().getOrderClass()).isNull();
			assertThat(cap.getValue().getActId()).isEqualTo(0L);
		}

		@Test
		void a42_noInsertWhenRowExists() {
			Companys a = new Companys();
			a.setCompanyId(5L);
			when(companysMapper.selectList(any())).thenReturn(List.of(a));
			when(companyDataMapper.selectCount(any())).thenReturn(2L);
			companyDataService.scheduleInitStatistic();
			verify(companyDataMapper, never()).insert(any(CompanyData.class));
		}

		@Test
		void a43_asyncOncePerCompany_withOrWithoutInsert() {
			Companys a = new Companys();
			a.setCompanyId(6L);
			Companys b = new Companys();
			b.setCompanyId(7L);
			when(companysMapper.selectList(any())).thenReturn(List.of(a, b));
			when(companyDataMapper.selectCount(any())).thenReturn(0L).thenReturn(2L);
			companyDataService.scheduleInitStatistic();
			verify(statisticJobEnqueuePort, times(2))
					.enqueue(anyLong(), eq(LocalDate.of(2024, 6, 14)), isNull(), eq(0L));
		}

		@Test
		void step5_completesWithoutError() {
			when(companysMapper.selectList(any())).thenReturn(List.of());
			companyDataService.scheduleInitStatistic();
			verifyNoMoreInteractions(companyDataStatisticsMapper);
		}
	}

	@Nested
	class ScheduleInitEmployeePurchaseStatisticE {

		private final long expectedStart =
				LocalDate.of(2024, 6, 14).atStartOfDay(SH).toEpochSecond();

		@Test
		void s_ep1_emptyActivities_noCompanys_noInitLog() {
			when(activitiesMapper.selectActiveIdsForCubeStatistic(expectedStart)).thenReturn(List.of());
			companyDataService.scheduleInitEmployeePurchaseStatistic();
			verify(activitiesMapper, times(1)).selectActiveIdsForCubeStatistic(expectedStart);
			verify(companysMapper, never()).selectList(any());
			assertThat(listAppender.list)
					.noneMatch(
							e -> e.getMessage() != null
									&& e.getMessage().contains("执行统计商城数据初始化脚本"));
		}

		@Test
		void s_ep1_countDateOverride_passesToActivityQuery() {
			LocalDate d = LocalDate.of(2024, 1, 5);
			long s = d.atStartOfDay(SH).toEpochSecond();
			when(activitiesMapper.selectActiveIdsForCubeStatistic(s)).thenReturn(List.of());
			companyDataService.scheduleInitEmployeePurchaseStatistic(d);
			verify(activitiesMapper).selectActiveIdsForCubeStatistic(s);
			verify(companysMapper, never()).selectList(any());
		}

		@Test
		void s_ep2_oneActivity_asyncUsesEmployeePurchaseAndActId() {
			when(activitiesMapper.selectActiveIdsForCubeStatistic(expectedStart))
					.thenReturn(List.of(100L));
			Companys a = new Companys();
			a.setCompanyId(1L);
			when(companysMapper.selectList(any())).thenReturn(List.of(a));
			when(companyDataMapper.selectCount(any())).thenReturn(1L);
			companyDataService.scheduleInitEmployeePurchaseStatistic();
			verify(statisticJobEnqueuePort, times(1))
					.enqueue(
							eq(1L), eq(LocalDate.of(2024, 6, 14)), eq("employee_purchase"), eq(100L));
		}

		@Test
		void s_ep3_twoActivities_runsInitPerActivity() {
			when(activitiesMapper.selectActiveIdsForCubeStatistic(expectedStart))
					.thenReturn(List.of(10L, 20L));
			when(companysMapper.selectList(any())).thenReturn(List.of());
			companyDataService.scheduleInitEmployeePurchaseStatistic();
			verify(companysMapper, times(2)).selectList(any());
			long initScriptLogs =
					listAppender.list.stream()
							.filter(
									e -> e.getMessage() != null
											&& e.getMessage()
													.contains("执行统计商城数据初始化脚本"))
							.count();
			assertThat(initScriptLogs).isEqualTo(2);
		}

		@Test
		void s_ep4_orderClassNonEmpty_skipsMemberAggregation() {
			LocalDate d = LocalDate.of(2024, 3, 10);
			long start = d.atStartOfDay(SH).toEpochSecond();
			long end = d.atTime(23, 59, 59).atZone(SH).toEpochSecond();
			when(companyDataStatisticsMapper.countAftersales(1L, start, end, "employee_purchase", 2L))
					.thenReturn(0L);
			when(companyDataStatisticsMapper.sumRefundedFee(1L, start, end, "employee_purchase", 2L))
					.thenReturn(0L);
			when(companyDataStatisticsMapper.sumAmountPayed(1L, start, end, "employee_purchase", 2L))
					.thenReturn(0L);
			when(companyDataStatisticsMapper.countOrders(1L, start, end, "employee_purchase", 2L))
					.thenReturn(0L);
			when(companyDataStatisticsMapper.countTradesSuccessWindow(1L, start, end, "employee_purchase", 2L))
					.thenReturn(0L);
			when(companyDataStatisticsMapper.sumGmv(1L, start, end, "employee_purchase", 2L))
					.thenReturn(0L);
			companyDataService.runStatistics(1L, d, "employee_purchase", 2L);
			verify(companyDataStatisticsMapper, never())
					.countNewMembers(anyLong(), anyLong(), anyLong());
		}
	}

	@Nested
	class RunStatisticsB {

		private static final LocalDate D = LocalDate.of(2024, 3, 10);
		private final long start = D.atStartOfDay(SH).toEpochSecond();
		private final long end = D.atTime(23, 59, 59).atZone(SH).toEpochSecond();

		@Test
		void b1_rejects_zeroCompany() {
			assertThrows(IllegalArgumentException.class, () -> companyDataService.runStatistics(0L, D, null, 0L));
		}

		@Test
		void b1_rejects_nullDate() {
			assertThrows(IllegalArgumentException.class, () -> companyDataService.runStatistics(1L, null, null, 0L));
		}

		@Test
		void b1_timeWindowPassedToAggregations() {
			when(companyDataStatisticsMapper.countNewMembers(1L, start, end)).thenReturn(0L);
			when(companyDataStatisticsMapper.countAftersales(1L, start, end, null, 0L)).thenReturn(0L);
			when(companyDataStatisticsMapper.sumRefundedFee(1L, start, end, null, 0L)).thenReturn(0L);
			when(companyDataStatisticsMapper.sumAmountPayed(1L, start, end, null, 0L)).thenReturn(0L);
			when(companyDataStatisticsMapper.countOrders(1L, start, end, null, 0L)).thenReturn(0L);
			when(companyDataStatisticsMapper.countTradesSuccessWindow(1L, start, end, null, 0L)).thenReturn(0L);
			when(companyDataStatisticsMapper.sumGmv(1L, start, end, null, 0L)).thenReturn(0L);
			companyDataService.runStatistics(1L, D, null, 0L);
			verify(companyDataStatisticsMapper, times(1)).countNewMembers(1L, start, end);
		}

		@Test
		void b2_memberCountWhenNoOrderClass() {
			stubAllZeroExceptMembers();
			when(companyDataStatisticsMapper.countNewMembers(1L, start, end)).thenReturn(3L);
			companyDataService.runStatistics(1L, D, null, 0L);
			verify(companyDataStatisticsMapper, times(1)).countNewMembers(1L, start, end);
		}

		@Test
		void b2_skipMembersWhenOrderClass() {
			when(companyDataStatisticsMapper.countAftersales(1L, start, end, "employee_purchase", 2L))
					.thenReturn(0L);
			when(companyDataStatisticsMapper.sumRefundedFee(1L, start, end, "employee_purchase", 2L))
					.thenReturn(0L);
			when(companyDataStatisticsMapper.sumAmountPayed(1L, start, end, "employee_purchase", 2L))
					.thenReturn(0L);
			when(companyDataStatisticsMapper.countOrders(1L, start, end, "employee_purchase", 2L))
					.thenReturn(0L);
			when(companyDataStatisticsMapper.countTradesSuccessWindow(1L, start, end, "employee_purchase", 2L))
					.thenReturn(0L);
			when(companyDataStatisticsMapper.sumGmv(1L, start, end, "employee_purchase", 2L))
					.thenReturn(0L);
			companyDataService.runStatistics(1L, D, "employee_purchase", 2L);
			verify(companyDataStatisticsMapper, never())
					.countNewMembers(anyLong(), anyLong(), anyLong());
		}

		@Test
		void b3_aftersales() {
			when(companyDataStatisticsMapper.countNewMembers(1L, start, end)).thenReturn(0L);
			when(companyDataStatisticsMapper.countAftersales(1L, start, end, null, 0L)).thenReturn(4L);
			when(companyDataStatisticsMapper.sumRefundedFee(1L, start, end, null, 0L)).thenReturn(0L);
			when(companyDataStatisticsMapper.sumAmountPayed(1L, start, end, null, 0L)).thenReturn(0L);
			when(companyDataStatisticsMapper.countOrders(1L, start, end, null, 0L)).thenReturn(0L);
			when(companyDataStatisticsMapper.countTradesSuccessWindow(1L, start, end, null, 0L)).thenReturn(0L);
			when(companyDataStatisticsMapper.sumGmv(1L, start, end, null, 0L)).thenReturn(0L);
			companyDataService.runStatistics(1L, D, null, 0L);
			verify(companyDataStatisticsMapper, times(1)).countAftersales(1L, start, end, null, 0L);
		}

		@Test
		void b4_refunded() {
			stubDefaultBranch();
			when(companyDataStatisticsMapper.sumRefundedFee(1L, start, end, null, 0L)).thenReturn(11L);
			companyDataService.runStatistics(1L, D, null, 0L);
			verify(companyDataStatisticsMapper, times(1)).sumRefundedFee(1L, start, end, null, 0L);
		}

		@Test
		void b5_amount_payed() {
			stubDefaultBranch();
			when(companyDataStatisticsMapper.sumAmountPayed(1L, start, end, null, 0L)).thenReturn(99L);
			companyDataService.runStatistics(1L, D, null, 0L);
			verify(companyDataStatisticsMapper, times(1)).sumAmountPayed(1L, start, end, null, 0L);
		}

		@Test
		void b6_orderCount() {
			stubDefaultBranch();
			when(companyDataStatisticsMapper.countOrders(1L, start, end, null, 0L)).thenReturn(7L);
			companyDataService.runStatistics(1L, D, null, 0L);
			verify(companyDataStatisticsMapper, times(1)).countOrders(1L, start, end, null, 0L);
		}

		@Test
		void b7_order_payed() {
			stubDefaultBranch();
			when(companyDataStatisticsMapper.countTradesSuccessWindow(1L, start, end, null, 0L))
					.thenReturn(8L);
			companyDataService.runStatistics(1L, D, null, 0L);
			verify(companyDataStatisticsMapper, times(1))
					.countTradesSuccessWindow(eq(1L), eq(start), eq(end), isNull(), eq(0L));
		}

		@Test
		void b8_gmv() {
			stubDefaultBranch();
			when(companyDataStatisticsMapper.sumGmv(1L, start, end, null, 0L)).thenReturn(500L);
			companyDataService.runStatistics(1L, D, null, 0L);
			verify(companyDataStatisticsMapper, times(1)).sumGmv(1L, start, end, null, 0L);
		}

		@Test
		void b9_updateSevenColumns() {
			stubDefaultBranch();
			when(companyDataStatisticsMapper.sumGmv(1L, start, end, null, 0L)).thenReturn(3L);
			companyDataService.runStatistics(1L, D, null, 0L);
			verify(companyDataMapper, times(1)).update(isNull(), any(Wrapper.class));
		}

		@Test
		void b10_endLog() {
			stubDefaultBranch();
			companyDataService.runStatistics(1L, D, null, 0L);
			assertThat(listAppender.list)
					.anyMatch(
							e -> e.getMessage() != null
									&& e.getMessage().contains("统计商城数据结束"));
		}

		private void stubDefaultBranch() {
			when(companyDataStatisticsMapper.countNewMembers(1L, start, end)).thenReturn(0L);
			when(companyDataStatisticsMapper.countAftersales(1L, start, end, null, 0L)).thenReturn(0L);
			when(companyDataStatisticsMapper.sumRefundedFee(1L, start, end, null, 0L)).thenReturn(0L);
			when(companyDataStatisticsMapper.sumAmountPayed(1L, start, end, null, 0L)).thenReturn(0L);
			when(companyDataStatisticsMapper.countOrders(1L, start, end, null, 0L)).thenReturn(0L);
			when(companyDataStatisticsMapper.countTradesSuccessWindow(1L, start, end, null, 0L))
					.thenReturn(0L);
			when(companyDataStatisticsMapper.sumGmv(1L, start, end, null, 0L)).thenReturn(0L);
		}

		private void stubAllZeroExceptMembers() {
			when(companyDataStatisticsMapper.countAftersales(1L, start, end, null, 0L)).thenReturn(0L);
			when(companyDataStatisticsMapper.sumRefundedFee(1L, start, end, null, 0L)).thenReturn(0L);
			when(companyDataStatisticsMapper.sumAmountPayed(1L, start, end, null, 0L)).thenReturn(0L);
			when(companyDataStatisticsMapper.countOrders(1L, start, end, null, 0L)).thenReturn(0L);
			when(companyDataStatisticsMapper.countTradesSuccessWindow(1L, start, end, null, 0L))
					.thenReturn(0L);
			when(companyDataStatisticsMapper.sumGmv(1L, start, end, null, 0L)).thenReturn(0L);
		}
	}
}
