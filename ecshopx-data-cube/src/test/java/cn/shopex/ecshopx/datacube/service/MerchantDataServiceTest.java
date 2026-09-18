package cn.shopex.ecshopx.datacube.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.shopex.ecshopx.datacube.domain.MerchantData;
import cn.shopex.ecshopx.datacube.mapper.MerchantDataMapper;
import cn.shopex.ecshopx.datacube.mapper.MerchantDataStatisticsMapper;
import cn.shopex.ecshopx.datacube.service.merchantdata.MerchantDataStatisticAsyncExecutor;
import cn.shopex.ecshopx.datacube.service.merchantdata.MerchantStatisticJobEnqueuePort;
import cn.shopex.ecshopx.merchant.domain.Merchant;
import cn.shopex.ecshopx.merchant.mapper.MerchantMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
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
class MerchantDataServiceTest {

	private static final ZoneId SH = ZoneId.of("Asia/Shanghai");

	@Mock
	private MerchantMapper merchantMapper;

	@Mock
	private MerchantDataMapper merchantDataMapper;

	@Mock
	private MerchantDataStatisticsMapper merchantDataStatisticsMapper;

	@Mock
	private MerchantStatisticJobEnqueuePort merchantStatisticJobEnqueuePort;

	/**
	 * Constructor dependency of {@link MerchantDataStatisticAsyncExecutor}; unused when {@code sync-inline=false}.
	 */
	@Mock
	private MerchantDataService asyncExecutorLazyMerchantDataService;

	private MerchantDataStatisticAsyncExecutor merchantDataStatisticAsyncExecutor;

	private Clock clock;
	private MerchantDataService merchantDataService;

	private ListAppender<ILoggingEvent> listAppender;
	private Logger serviceLogger;

	@BeforeEach
	void setUp() {
		clock = Clock.fixed(Instant.parse("2024-06-15T01:00:00+08:00"), SH);
		merchantDataStatisticAsyncExecutor = new MerchantDataStatisticAsyncExecutor(
				asyncExecutorLazyMerchantDataService, merchantStatisticJobEnqueuePort, false);
		merchantDataService = new MerchantDataService(
				merchantMapper,
				merchantDataMapper,
				merchantDataStatisticsMapper,
				merchantDataStatisticAsyncExecutor,
				clock);
		listAppender = new ListAppender<>();
		listAppender.start();
		listAppender.list.clear();
		serviceLogger = (Logger) LoggerFactory.getLogger(MerchantDataService.class);
		serviceLogger.addAppender(listAppender);
	}

	@AfterEach
	void tear() {
		serviceLogger.detachAppender(listAppender);
		listAppender.stop();
	}

	@Nested
	class ScheduleInitA {

		@Test
		void a_noMerchants_noInsert_noEnqueue() {
			when(merchantMapper.selectList(any())).thenReturn(List.of());
			merchantDataService.scheduleInitStatistic();
			verify(merchantDataMapper, never()).selectCount(any());
			verify(merchantDataMapper, never()).insert(any(MerchantData.class));
			verify(merchantStatisticJobEnqueuePort, never()).enqueue(anyLong(), anyLong(), any());
		}

		@Test
		void a1_logMessage() {
			when(merchantMapper.selectList(any())).thenReturn(List.of());
			merchantDataService.scheduleInitStatistic();
			assertThat(listAppender.list)
					.anyMatch(
							e -> e.getMessage() != null
									&& e.getMessage().contains("执行统计商城数据初始化脚本"));
		}

		@Test
		void a2_loadMerchants() {
			when(merchantMapper.selectList(any())).thenReturn(List.of());
			merchantDataService.scheduleInitStatistic();
			verify(merchantMapper, times(1)).selectList(any());
		}

		@Test
		void a35_placeholderExists_noInsert_enqueueOnce() {
			Merchant m = new Merchant();
			m.setId(5L);
			m.setCompanyId(9L);
			when(merchantMapper.selectList(any())).thenReturn(List.of(m));
			when(merchantDataMapper.selectCount(any())).thenReturn(2L);
			merchantDataService.scheduleInitStatistic();
			verify(merchantDataMapper, never()).insert(any(MerchantData.class));
			verify(merchantStatisticJobEnqueuePort, times(1))
					.enqueue(9L, 5L, LocalDate.of(2024, 6, 14));
		}

		@Test
		void a33_insertAndAsync() {
			Merchant m = new Merchant();
			m.setId(1L);
			m.setCompanyId(2L);
			when(merchantMapper.selectList(any())).thenReturn(List.of(m));
			when(merchantDataMapper.selectCount(any())).thenReturn(0L);
			merchantDataService.scheduleInitStatistic();
			ArgumentCaptor<MerchantData> ins = ArgumentCaptor.forClass(MerchantData.class);
			verify(merchantDataMapper, times(1)).insert(ins.capture());
			assertThat(ins.getValue().getMerchantId()).isEqualTo(1L);
			assertThat(ins.getValue().getCompanyId()).isEqualTo(2L);
			assertThat(ins.getValue().getCountDate()).isEqualTo(LocalDate.of(2024, 6, 14));
			verify(merchantStatisticJobEnqueuePort, times(1))
					.enqueue(2L, 1L, LocalDate.of(2024, 6, 14));
		}

		@Test
		void a_mixedTwoMerchants() {
			Merchant a = new Merchant();
			a.setId(10L);
			a.setCompanyId(1L);
			Merchant b = new Merchant();
			b.setId(20L);
			b.setCompanyId(2L);
			when(merchantMapper.selectList(any())).thenReturn(List.of(a, b));
			when(merchantDataMapper.selectCount(any())).thenReturn(0L).thenReturn(3L);
			merchantDataService.scheduleInitStatistic();
			verify(merchantDataMapper, times(1)).insert(any(MerchantData.class));
			verify(merchantStatisticJobEnqueuePort, times(1))
					.enqueue(1L, 10L, LocalDate.of(2024, 6, 14));
			verify(merchantStatisticJobEnqueuePort, times(1))
					.enqueue(2L, 20L, LocalDate.of(2024, 6, 14));
		}
	}

	@Nested
	class RunStatisticsB {

		private static final LocalDate D = LocalDate.of(2024, 3, 10);
		private final long start = D.atStartOfDay(SH).toEpochSecond();
		private final long end = D.atTime(23, 59, 59).atZone(SH).toEpochSecond();

		@Test
		void b3_rejects_zeroCompany() {
			assertThrows(
					IllegalArgumentException.class, () -> merchantDataService.runStatistics(0L, 1L, D));
		}

		@Test
		void b4_rejects_zeroMerchant() {
			assertThrows(
					IllegalArgumentException.class, () -> merchantDataService.runStatistics(1L, 0L, D));
		}

		@Test
		void b5_rejects_nullDate() {
			assertThrows(
					IllegalArgumentException.class, () -> merchantDataService.runStatistics(1L, 1L, null));
		}

		@Test
		void b7_allAggregationsInvoked() {
			stubAllZero();
			merchantDataService.runStatistics(1L, 2L, D);
			verify(merchantDataStatisticsMapper, times(1)).countAftersales(1L, 2L, start, end);
			verify(merchantDataStatisticsMapper, times(1)).sumRefundedFee(1L, 2L, start, end);
			verify(merchantDataStatisticsMapper, times(1)).sumAmountPayed(1L, 2L, start, end);
			verify(merchantDataStatisticsMapper, times(1)).sumAmountPointPayed(1L, 2L, start, end);
			verify(merchantDataStatisticsMapper, times(1)).countOrders(1L, 2L, start, end);
			verify(merchantDataStatisticsMapper, times(1)).countOrderPoint(1L, 2L, start, end);
			verify(merchantDataStatisticsMapper, times(1)).countTradesSuccessWindow(1L, 2L, start, end);
			verify(merchantDataStatisticsMapper, times(1))
					.countTradesPointPayedWindow(1L, 2L, start, end);
			verify(merchantDataStatisticsMapper, times(1)).sumGmv(1L, 2L, start, end);
			verify(merchantDataStatisticsMapper, times(1)).sumGmvPoint(1L, 2L, start, end);
		}

		@Test
		void b9_updateOnce_noMemberCountColumn() {
			stubAllZero();
			merchantDataService.runStatistics(1L, 2L, D);
			@SuppressWarnings("unchecked")
			ArgumentCaptor<UpdateWrapper<MerchantData>> cap = ArgumentCaptor.forClass(UpdateWrapper.class);
			verify(merchantDataMapper, times(1)).update(isNull(), cap.capture());
			String sqlSet = cap.getValue().getSqlSet();
			assertThat(sqlSet).contains("aftersales_count");
			assertThat(sqlSet).doesNotContain("member_count");
		}

		@Test
		void b10_endLog() {
			stubAllZero();
			merchantDataService.runStatistics(1L, 2L, D);
			assertThat(listAppender.list)
					.anyMatch(
							e -> e.getMessage() != null
									&& e.getMessage().contains("统计商城数据结束"));
		}

		@Test
		void b2_startLog() {
			stubAllZero();
			merchantDataService.runStatistics(1L, 2L, D);
			assertThat(listAppender.list)
					.anyMatch(
							e -> e.getMessage() != null
									&& e.getMessage().contains("统计商城数据开始")
									&& e.getMessage().contains("company_id:1")
									&& e.getMessage().contains("2024-03-10"));
		}

		private void stubAllZero() {
			when(merchantDataStatisticsMapper.countAftersales(1L, 2L, start, end)).thenReturn(0L);
			when(merchantDataStatisticsMapper.sumRefundedFee(1L, 2L, start, end)).thenReturn(0L);
			when(merchantDataStatisticsMapper.sumAmountPayed(1L, 2L, start, end)).thenReturn(0L);
			when(merchantDataStatisticsMapper.sumAmountPointPayed(1L, 2L, start, end)).thenReturn(0L);
			when(merchantDataStatisticsMapper.countOrders(1L, 2L, start, end)).thenReturn(0L);
			when(merchantDataStatisticsMapper.countOrderPoint(1L, 2L, start, end)).thenReturn(0L);
			when(merchantDataStatisticsMapper.countTradesSuccessWindow(1L, 2L, start, end))
					.thenReturn(0L);
			when(merchantDataStatisticsMapper.countTradesPointPayedWindow(1L, 2L, start, end))
					.thenReturn(0L);
			when(merchantDataStatisticsMapper.sumGmv(1L, 2L, start, end)).thenReturn(0L);
			when(merchantDataStatisticsMapper.sumGmvPoint(1L, 2L, start, end)).thenReturn(0L);
		}
	}

	@Nested
	class IsolationI {

		@Test
		void noStatisticsMapperInScheduleInitOnly() {
			when(merchantMapper.selectList(any())).thenReturn(List.of());
			merchantDataService.scheduleInitStatistic();
			verifyNoMoreInteractions(merchantDataStatisticsMapper);
		}
	}
}
