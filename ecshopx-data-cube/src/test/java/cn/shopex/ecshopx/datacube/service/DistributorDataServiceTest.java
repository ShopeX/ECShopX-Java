package cn.shopex.ecshopx.datacube.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import cn.shopex.ecshopx.datacube.domain.DistributorData;
import cn.shopex.ecshopx.datacube.mapper.DistributorDataMapper;
import cn.shopex.ecshopx.datacube.mapper.DistributorDataStatisticsMapper;
import cn.shopex.ecshopx.datacube.service.distributordata.DistributorDataJobEnqueuePort;
import cn.shopex.ecshopx.datacube.service.distributordata.DistributorDataStatisticAsyncExecutor;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.members.mapper.ShopRelMemberMapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class DistributorDataServiceTest {

	private static final ZoneId SH = ZoneId.of("Asia/Shanghai");

	@Mock
	private CompanysMapper companysMapper;

	@Mock
	private DistributorMapper distributorMapper;

	@Mock
	private DistributorDataMapper distributorDataMapper;

	@Mock
	private DistributorDataStatisticsMapper distributorDataStatisticsMapper;

	@Mock
	private ShopRelMemberMapper shopRelMemberMapper;

	@Mock
	private DistributorDataJobEnqueuePort distributorDataJobEnqueuePort;

	/**
	 * Constructor dependency of {@link DistributorDataStatisticAsyncExecutor}; unused when {@code sync-inline=false}.
	 */
	@Mock
	private DistributorDataService asyncExecutorLazyDistributorDataService;

	private DistributorDataStatisticAsyncExecutor distributorDataStatisticAsyncExecutor;

	private Clock clock;
	private DistributorDataService distributorDataService;

	private ListAppender<ILoggingEvent> listAppender;
	private Logger serviceLogger;

	@BeforeEach
	void setUp() {
		clock = Clock.fixed(Instant.parse("2024-06-15T01:00:00+08:00"), SH);
		distributorDataStatisticAsyncExecutor = new DistributorDataStatisticAsyncExecutor(
				asyncExecutorLazyDistributorDataService, distributorDataJobEnqueuePort, false);
		distributorDataService = new DistributorDataService(
				companysMapper,
				distributorMapper,
				distributorDataMapper,
				distributorDataStatisticsMapper,
				shopRelMemberMapper,
				distributorDataStatisticAsyncExecutor,
				clock);
		listAppender = new ListAppender<>();
		listAppender.start();
		listAppender.list.clear();
		serviceLogger = (Logger) LoggerFactory.getLogger(DistributorDataService.class);
		serviceLogger.addAppender(listAppender);
	}

	@AfterEach
	void tear() {
		serviceLogger.detachAppender(listAppender);
		listAppender.stop();
	}

	@Nested
	class ScheduleInitA {

		private static final LocalDate YESTERDAY = LocalDate.of(2024, 6, 14);

		@Test
		@DisplayName("analysis §3: A-1, A-2, A-5（无公司，不进入 A-4）")
		void a_noCompanies_zeroJobs() {
			when(companysMapper.selectList(any())).thenReturn(List.of());
			assertThat(distributorDataService.scheduleInitStatistic().jobsEnqueued()).isEqualTo(0);
			verify(distributorMapper, never()).selectList(any());
		}

		@Test
		@DisplayName("analysis §3: A-1（初始化脚本日志）")
		void a1_logMessage() {
			when(companysMapper.selectList(any())).thenReturn(List.of());
			distributorDataService.scheduleInitStatistic();
			assertThat(listAppender.list)
					.anyMatch(
							e -> e.getMessage() != null
									&& e.getMessage().contains("执行统计商城门店数据初始化脚本"));
		}

		@Test
		@DisplayName("analysis §3: A-2（CompanysMapper 全量）")
		void a2_loadCompanys() {
			when(companysMapper.selectList(any())).thenReturn(List.of());
			distributorDataService.scheduleInitStatistic();
			verify(companysMapper, times(1)).selectList(any());
		}

		@Test
		@DisplayName(
				"analysis §3: A-1, A-3, A-4.1, A-4.2, A-4.3.1, A-4.3.2, A-4.3.5, A-5（仅总店占位 insert+async）")
		void a4_onlyHead_oneInsert_oneAsync() {
			Companys c = new Companys();
			c.setCompanyId(9L);
			when(companysMapper.selectList(any())).thenReturn(List.of(c));
			when(distributorMapper.selectList(any())).thenReturn(List.of());
			when(distributorDataMapper.selectCount(any())).thenReturn(0L);
			assertThat(distributorDataService.scheduleInitStatistic().jobsEnqueued()).isEqualTo(1);
			ArgumentCaptor<DistributorData> ins = ArgumentCaptor.forClass(DistributorData.class);
			verify(distributorDataMapper, times(1)).insert(ins.capture());
			assertThat(ins.getValue().getDistributorId()).isEqualTo(0L);
			assertThat(ins.getValue().getMerchantId()).isEqualTo(0L);
			assertThat(ins.getValue().getCompanyId()).isEqualTo(9L);
			assertThat(ins.getValue().getCountDate()).isEqualTo(YESTERDAY);
			verify(distributorDataJobEnqueuePort, times(1))
					.enqueue(9L, 0L, 0L, YESTERDAY);
		}

		@Test
		@DisplayName("analysis §3: A-4.3.1, A-4.3.3-else, A-4.3.5（占位已存在仍 async）")
		void a43_placeholderExists_noInsert_stillAsync() {
			Companys c = new Companys();
			c.setCompanyId(9L);
			when(companysMapper.selectList(any())).thenReturn(List.of(c));
			when(distributorMapper.selectList(any())).thenReturn(List.of());
			when(distributorDataMapper.selectCount(any())).thenReturn(2L);
			distributorDataService.scheduleInitStatistic();
			verify(distributorDataMapper, never()).insert(any(DistributorData.class));
			verify(distributorDataJobEnqueuePort, times(1))
					.enqueue(9L, 0L, 0L, YESTERDAY);
		}

		@Test
		@DisplayName("analysis §3: A-4.1, A-4.2, A-4.3.2, A-4.3.5（门店+总店 insert+async）")
		void a33_insertAndAsync() {
			Companys c = new Companys();
			c.setCompanyId(2L);
			Distributor d = new Distributor();
			d.setDistributorId(5L);
			d.setCompanyId(2L);
			d.setMerchantId(7L);
			when(companysMapper.selectList(any())).thenReturn(List.of(c));
			when(distributorMapper.selectList(any())).thenReturn(List.of(d));
			when(distributorDataMapper.selectCount(any())).thenReturn(0L);
			distributorDataService.scheduleInitStatistic();
			verify(distributorDataMapper, times(2)).insert(any(DistributorData.class));
			verify(distributorDataJobEnqueuePort, times(1))
					.enqueue(2L, 0L, 0L, YESTERDAY);
			verify(distributorDataJobEnqueuePort, times(1))
					.enqueue(2L, 5L, 7L, YESTERDAY);
		}

		@Test
		@DisplayName(
				"analysis §3: A-4.2, A-4.3.1, A-4.3.3-else, A-4.3.5, A-5（多门店+总店，混合占位）")
		void a_multiDistributors_plusHead() {
			Companys c = new Companys();
			c.setCompanyId(1L);
			Distributor a = new Distributor();
			a.setDistributorId(10L);
			a.setCompanyId(1L);
			a.setMerchantId(1L);
			Distributor b = new Distributor();
			b.setDistributorId(20L);
			b.setCompanyId(1L);
			b.setMerchantId(2L);
			when(companysMapper.selectList(any())).thenReturn(List.of(c));
			when(distributorMapper.selectList(any())).thenReturn(List.of(a, b));
			when(distributorDataMapper.selectCount(any())).thenReturn(1L);
			assertThat(distributorDataService.scheduleInitStatistic().jobsEnqueued()).isEqualTo(3);
			verify(distributorDataJobEnqueuePort, times(1))
					.enqueue(1L, 0L, 0L, YESTERDAY);
			verify(distributorDataJobEnqueuePort, times(1))
					.enqueue(1L, 10L, 1L, YESTERDAY);
			verify(distributorDataJobEnqueuePort, times(1))
					.enqueue(1L, 20L, 2L, YESTERDAY);
		}
	}

	@Nested
	class RunStatisticsB {

		private static final LocalDate D = LocalDate.of(2024, 3, 10);
		private final long start = D.atStartOfDay(SH).toEpochSecond();
		private final long end = D.atTime(23, 59, 59).atZone(SH).toEpochSecond();

		@Test
		@DisplayName("analysis §3: B-1, B-3（company_id=0 拒斥）")
		void b3_rejects_zeroCompany() {
			assertThrows(
					IllegalArgumentException.class, () -> distributorDataService.runStatistics(0L, 1L, 1L, D));
		}

		@Test
		@DisplayName(
				"analysis §3: B-1, B-4, B-6, B-7, B-8, B-9, B-10, B-11, B-12, B-13, B-14, B-15, B-16（总店"
						+ " distributorId=0 全路径）")
		void b4_distributorZero_allowed() {
			stubAllZero();
			distributorDataService.runStatistics(1L, 0L, 0L, D);
			verify(shopRelMemberMapper, never()).selectCount(any());
		}

		@Test
		@DisplayName("analysis §3: B-1, B-4, B-6（distributorId 任意合法 long 不抛错）")
		void b4_distributorArbitrary_doesNotThrow() {
			stubAllZero();
			distributorDataService.runStatistics(1L, 0L, 99L, D);
		}

		@Test
		@DisplayName("analysis §3: B-1, B-2, B-5（countDate null 拒斥）")
		void b5_rejects_nullDate() {
			assertThrows(
					IllegalArgumentException.class, () -> distributorDataService.runStatistics(1L, 1L, 1L, null));
		}

		@Test
		@DisplayName("analysis §3: B-1, B-2, B-6, B-7, B-8（门店 ShopRelMember + aftersales）")
		void b7_memberWhenDistributorPositive() {
			when(shopRelMemberMapper.selectCount(any())).thenReturn(3L);
			stubAllZero();
			distributorDataService.runStatistics(1L, 2L, 1L, D);
			verify(shopRelMemberMapper, times(1)).selectCount(any());
			verify(distributorDataStatisticsMapper, times(1)).countAftersales(1L, 2L, start, end);
		}

		@Test
		@DisplayName("analysis §3: B-7（总店 distributorId=0 跳过 ShopRelMember）")
		void b7_memberWhenDistributorHead_skipsShopQuery() {
			stubAllZero();
			distributorDataService.runStatistics(1L, 0L, 0L, D);
			verify(shopRelMemberMapper, never()).selectCount(any());
		}

		@Test
		@DisplayName(
				"analysis §3: B-7, B-8, B-9, B-10, B-11, B-12, B-13（十路聚合一次跑齐）")
		void b7_allAggregationsInvoked() {
			stubAllZero();
			when(shopRelMemberMapper.selectCount(any())).thenReturn(0L);
			distributorDataService.runStatistics(1L, 2L, 1L, D);
			verify(distributorDataStatisticsMapper, times(1)).countAftersales(1L, 2L, start, end);
			verify(distributorDataStatisticsMapper, times(1)).sumRefundedFee(1L, 2L, start, end);
			verify(distributorDataStatisticsMapper, times(1)).sumAmountPayed(1L, 2L, start, end);
			verify(distributorDataStatisticsMapper, times(1)).sumAmountPointPayed(1L, 2L, start, end);
			verify(distributorDataStatisticsMapper, times(1)).countOrders(1L, 2L, start, end);
			verify(distributorDataStatisticsMapper, times(1)).countOrderPoint(1L, 2L, start, end);
			verify(distributorDataStatisticsMapper, times(1)).countTradesSuccessWindow(1L, 2L, start, end);
			verify(distributorDataStatisticsMapper, times(1))
					.countTradesPointPayedWindow(1L, 2L, start, end);
			verify(distributorDataStatisticsMapper, times(1)).sumGmv(1L, 2L, start, end);
			verify(distributorDataStatisticsMapper, times(1)).sumGmvPoint(1L, 2L, start, end);
		}

		@Test
		@DisplayName("analysis §3: B-14, B-15（UpdateWrapper 四元组与统计列）")
		void b15_updateHasMemberAndMetrics_whereQuad() {
			stubAllZero();
			when(shopRelMemberMapper.selectCount(any())).thenReturn(0L);
			distributorDataService.runStatistics(1L, 2L, 88L, D);
			@SuppressWarnings("unchecked")
			ArgumentCaptor<UpdateWrapper<DistributorData>> cap = ArgumentCaptor.forClass(UpdateWrapper.class);
			verify(distributorDataMapper, times(1)).update(isNull(), cap.capture());
			String sqlSet = cap.getValue().getSqlSet();
			assertThat(sqlSet).contains("member_count");
			assertThat(sqlSet).contains("aftersales_count");
		}

		@Test
		@DisplayName("analysis §3: B-2（统计开始日志）")
		void b2_startLog() {
			stubAllZero();
			distributorDataService.runStatistics(1L, 2L, 1L, D);
			assertThat(listAppender.list)
					.anyMatch(
							e -> e.getMessage() != null
									&& e.getMessage().contains("统计商城加经销商数据开始")
									&& e.getMessage().contains("company_id:1")
									&& e.getMessage().contains("distributor_id:2"));
		}

		@Test
		@DisplayName("analysis §3: B-16（统计结束日志）")
		void b16_endLog() {
			stubAllZero();
			distributorDataService.runStatistics(1L, 2L, 1L, D);
			assertThat(listAppender.list)
					.anyMatch(e -> e.getMessage() != null && e.getMessage().contains("统计商城加经销商数据结束"));
		}

		@Test
		@DisplayName("analysis §3: A-4.3.2, B-15（同 distributor 不同 merchant 两次 update）")
		void updateZeroRows_sameDistributorNewMerchant_stillFiresUpdate() {
			stubAllZero();
			when(shopRelMemberMapper.selectCount(any())).thenReturn(0L);
			distributorDataService.runStatistics(10L, 5L, 100L, D);
			distributorDataService.runStatistics(10L, 5L, 200L, D);
			verify(distributorDataMapper, times(2)).update(isNull(), any());
		}

		private void stubAllZero() {
			when(distributorDataStatisticsMapper.countAftersales(anyLong(), anyLong(), anyLong(), anyLong()))
					.thenReturn(0L);
			when(distributorDataStatisticsMapper.sumRefundedFee(anyLong(), anyLong(), anyLong(), anyLong()))
					.thenReturn(0L);
			when(distributorDataStatisticsMapper.sumAmountPayed(anyLong(), anyLong(), anyLong(), anyLong()))
					.thenReturn(0L);
			when(distributorDataStatisticsMapper.sumAmountPointPayed(anyLong(), anyLong(), anyLong(), anyLong()))
					.thenReturn(0L);
			when(distributorDataStatisticsMapper.countOrders(anyLong(), anyLong(), anyLong(), anyLong()))
					.thenReturn(0L);
			when(distributorDataStatisticsMapper.countOrderPoint(anyLong(), anyLong(), anyLong(), anyLong()))
					.thenReturn(0L);
			when(distributorDataStatisticsMapper.countTradesSuccessWindow(
							anyLong(), anyLong(), anyLong(), anyLong()))
					.thenReturn(0L);
			when(distributorDataStatisticsMapper.countTradesPointPayedWindow(
							anyLong(), anyLong(), anyLong(), anyLong()))
					.thenReturn(0L);
			when(distributorDataStatisticsMapper.sumGmv(anyLong(), anyLong(), anyLong(), anyLong()))
					.thenReturn(0L);
			when(distributorDataStatisticsMapper.sumGmvPoint(anyLong(), anyLong(), anyLong(), anyLong()))
					.thenReturn(0L);
		}
	}

	@Nested
	class IsolationI {

		@Test
		@DisplayName("analysis §3: A-1, A-2, A-5（scheduleInit 不调用 DistributorDataStatisticsMapper）")
		void noStatisticsMapperInScheduleInitOnly() {
			when(companysMapper.selectList(any())).thenReturn(List.of());
			distributorDataService.scheduleInitStatistic();
			verifyNoMoreInteractions(distributorDataStatisticsMapper);
		}
	}
}
