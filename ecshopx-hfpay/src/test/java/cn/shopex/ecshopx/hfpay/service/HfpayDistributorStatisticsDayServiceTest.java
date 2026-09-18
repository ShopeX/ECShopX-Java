package cn.shopex.ecshopx.hfpay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.hfpay.domain.HfpayDistributorStatisticsDay;
import cn.shopex.ecshopx.hfpay.domain.HfpayDistributorTransactionStatistics;
import cn.shopex.ecshopx.hfpay.domain.HfpayEnterapply;
import cn.shopex.ecshopx.hfpay.mapper.HfpayDistributorStatisticsDayMapper;
import cn.shopex.ecshopx.hfpay.mapper.HfpayDistributorTransactionStatisticsMapper;
import cn.shopex.ecshopx.hfpay.mapper.HfpayEnterapplyMapper;
import cn.shopex.ecshopx.hfpay.mapper.HfpayTradeRecordMapper;
import cn.shopex.ecshopx.hfpay.service.statistics.HfpayStatisticsOrderMetricsService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HfpayDistributorStatisticsDayServiceTest {

	private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter BASIC = DateTimeFormatter.BASIC_ISO_DATE;

	@Mock
	private HfpayEnterapplyMapper enterapplyMapper;

	@Mock
	private HfpayDistributorStatisticsDayMapper statisticsDayMapper;

	@Mock
	private HfpayDistributorTransactionStatisticsMapper transactionStatisticsMapper;

	@Mock
	private HfpayTradeRecordMapper tradeRecordMapper;

	@Mock
	private HfpayStatisticsOrderMetricsService orderMetricsService;

	private HfpayDistributorStatisticsDayService service;

	@BeforeEach
	void rebind() {
		service =
				new HfpayDistributorStatisticsDayService(
						enterapplyMapper,
						statisticsDayMapper,
						transactionStatisticsMapper,
						tradeRecordMapper,
						orderMetricsService,
						ZONE.getId());
	}

	private HfpayEnterapply row(long companyId, long distId) {
		HfpayEnterapply e = new HfpayEnterapply();
		e.setCompanyId(companyId);
		e.setDistributorId(distId);
		return e;
	}

	@DisplayName("analysis §3 步骤 1,2,3,4,9: count=0 早退，无步骤 5/6/7/8（processed=0）")
	@Test
	void earlyExit_noInsertsWhenCountZero() {
		when(enterapplyMapper.selectCount(any())).thenReturn(0L);
		assertThat(service.statistics()).isZero();
		verify(statisticsDayMapper, never()).insert(isA(HfpayDistributorStatisticsDay.class));
		verify(transactionStatisticsMapper, never()).insert(isA(HfpayDistributorTransactionStatistics.class));
	}

	@DisplayName("analysis §3 步骤 1,2,3,4,6-1,6-2,7-1~7-8,8-1~8-4,9: 单页 count<=500")
	@Test
	void singlePage_runsDistributorAndTransactionInserts() {
		String ymd = LocalDate.now(ZONE).minusDays(1).format(BASIC);
		long endUnix =
				LocalDate.now(ZONE).minusDays(1).atTime(23, 59, 59).atZone(ZONE).toEpochSecond();
		long startUnix = LocalDate.now(ZONE).minusDays(1).atStartOfDay(ZONE).toEpochSecond();
		when(enterapplyMapper.selectCount(any())).thenReturn(1L);
		when(enterapplyMapper.selectList(any())).thenReturn(List.of(row(1L, 2L)));
		when(orderMetricsService.sumProfitShareCapitalForDistributorFenWhereHfOrderDateLteYmd(1L, 2L, ymd))
				.thenReturn(100L);
		when(orderMetricsService.distributorCumulativeNetIncomeFenAfterSplit(1L, 2L, endUnix))
				.thenReturn(1000L);
		when(orderMetricsService.distributorCumulativeNetRefundFenAfterSplit(1L, 2L, endUnix))
				.thenReturn(30L);
		when(tradeRecordMapper.sumOutcomeFenByWithdrawalFilter(1L, "2", endUnix))
				.thenReturn(5L);
		Map<String, Object> countMap = sampleCountMap();
		when(orderMetricsService.count(1L, 2L, startUnix, endUnix)).thenReturn(countMap);
		assertThat(service.statistics()).isEqualTo(1);
		verify(statisticsDayMapper, times(1)).insert(isA(HfpayDistributorStatisticsDay.class));
		verify(transactionStatisticsMapper, times(1))
				.insert(isA(HfpayDistributorTransactionStatistics.class));
		ArgumentCaptor<HfpayDistributorStatisticsDay> dayCap = ArgumentCaptor.forClass(HfpayDistributorStatisticsDay.class);
		verify(statisticsDayMapper).insert(dayCap.capture());
		assertThat(dayCap.getValue().getIncome()).isEqualTo(1000 - 30);
		assertThat(dayCap.getValue().getSettlementFunds()).isEqualTo(100);
		assertThat(dayCap.getValue().getUnsettledFunds()).isEqualTo(1000 - 30 - 100);
	}

	@DisplayName("analysis §3 步骤 1~4,5-1~5-4 与 §8: count=1000 时 $page<pageNum 仅一批")
	@Test
	void whenCountThousand_selectListOnlyOnce_sameMissAsPhpStrictLessThan() {
		when(enterapplyMapper.selectCount(any())).thenReturn(1000L);
		when(enterapplyMapper.selectList(any()))
				.thenReturn(Collections.nCopies(500, row(1L, 1L)))
				.thenReturn(Collections.emptyList());
		stubDefaultMetricsFor(1L, 1L);
		assertThat(service.statistics()).isEqualTo(500);
		verify(enterapplyMapper, times(1)).selectList(any());
		// 与 analysis §8: $page < $count/$pageSize，第二页不跑；与 PHP 同步漏跑
	}

	@DisplayName("analysis §3 步骤 5-1~5-4: $page < $pageNum 不执行末页 (501 条时仅跑第一页 500 行)")
	@Test
	void whenCount501_singleBatchLikePhp() {
		when(enterapplyMapper.selectCount(any())).thenReturn(501L);
		when(enterapplyMapper.selectList(any())).thenReturn(Collections.nCopies(500, row(1L, 1L)));
		stubDefaultMetricsFor(1L, 1L);
		assertThat(service.statistics()).isEqualTo(500);
		verify(enterapplyMapper, times(1)).selectList(any());
		verify(statisticsDayMapper, times(500)).insert(isA(HfpayDistributorStatisticsDay.class));
	}

	@DisplayName("analysis §3 步骤 7-5 与 §8: 提现聚合 null → 0（NPE 防护）")
	@Test
	void withdrawalSumNull_treatedAsZero() {
		String ymd = LocalDate.now(ZONE).minusDays(1).format(BASIC);
		long endUnix =
				LocalDate.now(ZONE).minusDays(1).atTime(23, 59, 59).atZone(ZONE).toEpochSecond();
		long startUnix = LocalDate.now(ZONE).minusDays(1).atStartOfDay(ZONE).toEpochSecond();
		when(enterapplyMapper.selectCount(any())).thenReturn(1L);
		when(enterapplyMapper.selectList(any())).thenReturn(List.of(row(1L, 2L)));
		when(orderMetricsService.sumProfitShareCapitalForDistributorFenWhereHfOrderDateLteYmd(1L, 2L, ymd))
				.thenReturn(0L);
		when(orderMetricsService.distributorCumulativeNetIncomeFenAfterSplit(1L, 2L, endUnix))
				.thenReturn(10L);
		when(orderMetricsService.distributorCumulativeNetRefundFenAfterSplit(1L, 2L, endUnix))
				.thenReturn(0L);
		when(tradeRecordMapper.sumOutcomeFenByWithdrawalFilter(1L, "2", endUnix))
				.thenReturn(null);
		when(orderMetricsService.count(1L, 2L, startUnix, endUnix))
				.thenReturn(sampleCountMap());
		assertDoesNotThrow(() -> assertThat(service.statistics()).isEqualTo(1));
		ArgumentCaptor<HfpayDistributorStatisticsDay> dayCap = ArgumentCaptor.forClass(HfpayDistributorStatisticsDay.class);
		verify(statisticsDayMapper).insert(dayCap.capture());
		assertThat(dayCap.getValue().getWithdrawal()).isZero();
	}

	@Nested
	class LteHfOrderDateAndCountWindow {
		@DisplayName("analysis §3 步骤 7-2: 已结算资金须 hf_order_date <= 昨日 Ymd，禁止误用 gte 方法")
		@Test
		void settlementUsesHfOrderDateLteYmdNotGte() {
			String ymd = LocalDate.now(ZONE).minusDays(1).format(BASIC);
			long endUnix =
					LocalDate.now(ZONE).minusDays(1).atTime(23, 59, 59).atZone(ZONE).toEpochSecond();
			long startUnix = LocalDate.now(ZONE).minusDays(1).atStartOfDay(ZONE).toEpochSecond();
			when(enterapplyMapper.selectCount(any())).thenReturn(1L);
			when(enterapplyMapper.selectList(any())).thenReturn(List.of(row(9L, 8L)));
			when(orderMetricsService.sumProfitShareCapitalForDistributorFenWhereHfOrderDateLteYmd(9L, 8L, ymd))
					.thenReturn(1L);
			when(orderMetricsService.distributorCumulativeNetIncomeFenAfterSplit(9L, 8L, endUnix))
					.thenReturn(2L);
			when(orderMetricsService.distributorCumulativeNetRefundFenAfterSplit(9L, 8L, endUnix))
					.thenReturn(0L);
			when(tradeRecordMapper.sumOutcomeFenByWithdrawalFilter(9L, "8", endUnix)).thenReturn(0L);
			when(orderMetricsService.count(9L, 8L, startUnix, endUnix)).thenReturn(sampleCountMap());
			assertThat(service.statistics()).isEqualTo(1);
			verify(orderMetricsService, atLeastOnce())
					.sumProfitShareCapitalForDistributorFenWhereHfOrderDateLteYmd(eq(9L), eq(8L), eq(ymd));
			verify(orderMetricsService, never())
					.sumProfitShareCapitalForDistributorFen(9L, 8L, ymd);
		}

		@DisplayName("analysis §3 步骤 8-1,8-2: count 窗口为昨日 00:00:00~23:59:59（业务时区）")
		@Test
		void countUsesYesterdayMidnightToEnd() {
			String ymd = LocalDate.now(ZONE).minusDays(1).format(BASIC);
			LocalDate d = LocalDate.parse(ymd, BASIC);
			long endUnix = d.atTime(23, 59, 59).atZone(ZONE).toEpochSecond();
			long startUnix = d.atStartOfDay(ZONE).toEpochSecond();
			when(enterapplyMapper.selectCount(any())).thenReturn(1L);
			when(enterapplyMapper.selectList(any())).thenReturn(List.of(row(3L, 4L)));
			stubDefaultMetricsFor(3L, 4L);
			assertThat(service.statistics()).isEqualTo(1);
			verify(orderMetricsService).count(eq(3L), eq(4L), eq(startUnix), eq(endUnix));
		}
	}

	private void stubDefaultMetricsFor(long companyId, long distId) {
		String ymd = LocalDate.now(ZONE).minusDays(1).format(BASIC);
		long endUnix =
				LocalDate.now(ZONE).minusDays(1).atTime(23, 59, 59).atZone(ZONE).toEpochSecond();
		long startUnix = LocalDate.now(ZONE).minusDays(1).atStartOfDay(ZONE).toEpochSecond();
		when(orderMetricsService.sumProfitShareCapitalForDistributorFenWhereHfOrderDateLteYmd(companyId, distId, ymd))
				.thenReturn(0L);
		when(orderMetricsService.distributorCumulativeNetIncomeFenAfterSplit(companyId, distId, endUnix))
				.thenReturn(0L);
		when(orderMetricsService.distributorCumulativeNetRefundFenAfterSplit(companyId, distId, endUnix))
				.thenReturn(0L);
		when(tradeRecordMapper.sumOutcomeFenByWithdrawalFilter(companyId, String.valueOf(distId), endUnix))
				.thenReturn(0L);
		when(orderMetricsService.count(companyId, distId, startUnix, endUnix))
				.thenReturn(sampleCountMap());
	}

	private static Map<String, Object> sampleCountMap() {
		Map<String, Object> c = new LinkedHashMap<>();
		c.put("order_count", 1L);
		c.put("order_total_fee", "1.00");
		c.put("order_refund_count", 0L);
		c.put("order_refund_total_fee", "0.00");
		c.put("order_refunding_count", 0L);
		c.put("order_refunding_total_fee", "0.00");
		c.put("order_profit_sharing_charge", "0.00");
		c.put("order_total_charge", "0.00");
		c.put("order_refund_total_charge", "0.00");
		c.put("order_un_profit_sharing_total_charge", "0.00");
		c.put("order_un_profit_sharing_refund_total_charge", "0.00");
		c.put("order_un_profit_sharing_charge", "0.00");
		return c;
	}
}
