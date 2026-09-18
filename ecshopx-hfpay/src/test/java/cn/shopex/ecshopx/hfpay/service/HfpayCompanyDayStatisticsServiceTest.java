package cn.shopex.ecshopx.hfpay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.hfpay.domain.HfpayCompanyStatisticsDay;
import cn.shopex.ecshopx.hfpay.domain.HfpayLedgerConfig;
import cn.shopex.ecshopx.hfpay.mapper.HfpayCompanyStatisticsDayMapper;
import cn.shopex.ecshopx.hfpay.mapper.HfpayLedgerConfigMapper;
import cn.shopex.ecshopx.hfpay.service.statistics.HfpayStatisticsOrderMetricsService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
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
class HfpayCompanyDayStatisticsServiceTest {

	private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter BASIC = DateTimeFormatter.BASIC_ISO_DATE;

	@Mock
	private HfpayLedgerConfigMapper ledgerConfigMapper;

	@Mock
	private HfpayCompanyStatisticsDayMapper companyStatisticsDayMapper;

	@Mock
	private HfpayStatisticsOrderMetricsService orderMetricsService;

	private HfpayCompanyDayStatisticsService service;

	@BeforeEach
	void rebind() {
		service = new HfpayCompanyDayStatisticsService(ledgerConfigMapper, companyStatisticsDayMapper, orderMetricsService, ZONE.getId());
	}

	private static HfpayLedgerConfig row(long companyId) {
		HfpayLedgerConfig c = new HfpayLedgerConfig();
		c.setCompanyId(companyId);
		return c;
	}

	@DisplayName("analysis §3 步骤 1,2,3,4: count&lt;1 早退，无 INSERT")
	@Test
	void earlyExit_noInsertsWhenCountZero() {
		when(ledgerConfigMapper.selectCount(any())).thenReturn(0L);
		assertThat(service.scheduleCompanyDailyStatistics()).isZero();
		verify(companyStatisticsDayMapper, never()).insert(isA(HfpayCompanyStatisticsDay.class));
	}

	@DisplayName("analysis §3 1–5,7-1,7-2,8-1,8-2,8-3,8-4-1,8-4-3,8-4-4,8-4-5,8-4-6,8-5,9（8-4-2 见 Nested 8-4-2）：单页 count&lt;=500")
	@Test
	void singlePage_runsInserts() {
		String ymd = LocalDate.now(ZONE).minusDays(1).format(BASIC);
		long endUnix =
				LocalDate.now(ZONE).minusDays(1).atTime(23, 59, 59).atZone(ZONE).toEpochSecond();
		int dateEpoch = (int) LocalDate.now(ZONE).minusDays(1).atStartOfDay(ZONE).toEpochSecond();
		when(ledgerConfigMapper.selectCount(any())).thenReturn(1L);
		when(ledgerConfigMapper.selectList(any())).thenReturn(List.of(row(1L)));
		when(orderMetricsService.sumProfitShareCapitalForPlatformFenWhereHfOrderDateLteYmd(1L, ymd))
				.thenReturn(30L);
		when(orderMetricsService.companyCumulativeSplitFeeIncomeFen0(1L, endUnix)).thenReturn(1000L);
		when(orderMetricsService.companyCumulativeSplitFeeRefundFen0(1L, endUnix)).thenReturn(100L);
		assertThat(service.scheduleCompanyDailyStatistics()).isEqualTo(1);
		ArgumentCaptor<HfpayCompanyStatisticsDay> cap = ArgumentCaptor.forClass(HfpayCompanyStatisticsDay.class);
		verify(companyStatisticsDayMapper).insert(cap.capture());
		assertThat(cap.getValue().getType()).isEqualTo(2);
		assertThat(cap.getValue().getDate()).isEqualTo(dateEpoch);
		assertThat(cap.getValue().getCompanyId()).isEqualTo(1);
		assertThat(cap.getValue().getIncome()).isEqualTo(1000);
		assertThat(cap.getValue().getRefund()).isEqualTo(100);
		assertThat(cap.getValue().getSettlementFunds()).isEqualTo(30);
		assertThat(cap.getValue().getUnsettledFunds()).isEqualTo(1000 - 100 - 30);
		assertThat(cap.getValue().getDisburse()).isZero();
		assertThat(cap.getValue().getWithdrawal()).isZero();
		assertThat(cap.getValue().getCreatedAt()).isNotNull();
		assertThat(cap.getValue().getUpdatedAt()).isNotNull();
	}

	@DisplayName("analysis §3 步骤 6-1,6-2,6-3,6-4 与 §8: count=1000 时 $page&lt;pageNum 仅一批")
	@Test
	void whenCountThousand_selectListOnlyOnce_sameMissAsPhpStrictLessThan() {
		when(ledgerConfigMapper.selectCount(any())).thenReturn(1000L);
		when(ledgerConfigMapper.selectList(any()))
				.thenReturn(Collections.nCopies(500, row(1L)))
				.thenReturn(Collections.emptyList());
		stubDefaultMetrics(1L);
		assertThat(service.scheduleCompanyDailyStatistics()).isEqualTo(500);
		verify(ledgerConfigMapper, times(1)).selectList(any());
	}

	@DisplayName("analysis §3 步骤 6-1,6-2,6-3,6-4 与 §8: 501 条时仅第一页 500 行")
	@Test
	void whenCount501_singleBatchLikePhp() {
		when(ledgerConfigMapper.selectCount(any())).thenReturn(501L);
		when(ledgerConfigMapper.selectList(any())).thenReturn(Collections.nCopies(500, row(1L)));
		stubDefaultMetrics(1L);
		assertThat(service.scheduleCompanyDailyStatistics()).isEqualTo(500);
		verify(ledgerConfigMapper, times(1)).selectList(any());
		verify(companyStatisticsDayMapper, times(500)).insert(isA(HfpayCompanyStatisticsDay.class));
	}

	@DisplayName("analysis §3 8-2: 某页 selectList 空则该批 0 行写库")
	@Test
	void emptyListBatch_noInserts() {
		when(ledgerConfigMapper.selectCount(any())).thenReturn(1L);
		when(ledgerConfigMapper.selectList(any())).thenReturn(Collections.emptyList());
		assertThat(service.scheduleCompanyDailyStatistics()).isZero();
		verify(companyStatisticsDayMapper, never()).insert(isA(HfpayCompanyStatisticsDay.class));
	}

	@DisplayName("analysis §8: company_id 为 null 的行跳过，不计入 processed")
	@Test
	void nullCompanyId_skipped() {
		HfpayLedgerConfig noCompany = new HfpayLedgerConfig();
		noCompany.setCompanyId(null);
		when(ledgerConfigMapper.selectCount(any())).thenReturn(1L);
		when(ledgerConfigMapper.selectList(any())).thenReturn(List.of(noCompany));
		assertThat(service.scheduleCompanyDailyStatistics()).isZero();
		verify(companyStatisticsDayMapper, never()).insert(isA(HfpayCompanyStatisticsDay.class));
	}

	@Nested
	class PlatformLteAndSplitFee0 {

		@DisplayName("plan §2 8-4-2: 已结算须 hf_order_date &lt;= 昨日 Ymd + platform，禁止误用 gte 路径")
		@Test
		void settlementUsesPlatformLteYmd() {
			String ymd = LocalDate.now(ZONE).minusDays(1).format(BASIC);
			long endUnix =
					LocalDate.now(ZONE).minusDays(1).atTime(23, 59, 59).atZone(ZONE).toEpochSecond();
			when(ledgerConfigMapper.selectCount(any())).thenReturn(1L);
			when(ledgerConfigMapper.selectList(any())).thenReturn(List.of(row(9L)));
			when(orderMetricsService.sumProfitShareCapitalForPlatformFenWhereHfOrderDateLteYmd(9L, ymd))
					.thenReturn(1L);
			when(orderMetricsService.companyCumulativeSplitFeeIncomeFen0(9L, endUnix)).thenReturn(2L);
			when(orderMetricsService.companyCumulativeSplitFeeRefundFen0(9L, endUnix)).thenReturn(0L);
			assertThat(service.scheduleCompanyDailyStatistics()).isEqualTo(1);
			verify(orderMetricsService, atLeastOnce())
					.sumProfitShareCapitalForPlatformFenWhereHfOrderDateLteYmd(eq(9L), eq(ymd));
			verify(orderMetricsService, never()).sumProfitShareCapitalFen(9L, ymd);
			verify(orderMetricsService, never())
					.sumProfitShareCapitalForDistributorFenWhereHfOrderDateLteYmd(
							anyLong(), anyLong(), anyString());
		}
	}

	private void stubDefaultMetrics(long companyId) {
		String ymd = LocalDate.now(ZONE).minusDays(1).format(BASIC);
		long endUnix =
				LocalDate.now(ZONE).minusDays(1).atTime(23, 59, 59).atZone(ZONE).toEpochSecond();
		when(orderMetricsService.sumProfitShareCapitalForPlatformFenWhereHfOrderDateLteYmd(companyId, ymd))
				.thenReturn(0L);
		when(orderMetricsService.companyCumulativeSplitFeeIncomeFen0(companyId, endUnix)).thenReturn(0L);
		when(orderMetricsService.companyCumulativeSplitFeeRefundFen0(companyId, endUnix)).thenReturn(0L);
	}
}
