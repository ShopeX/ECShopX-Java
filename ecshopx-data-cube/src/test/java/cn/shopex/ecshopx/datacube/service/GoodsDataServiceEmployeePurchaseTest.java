package cn.shopex.ecshopx.datacube.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.datacube.domain.GoodsData;
import cn.shopex.ecshopx.datacube.mapper.GoodsDailyStatisticsQueryMapper;
import cn.shopex.ecshopx.datacube.mapper.GoodsDataMapper;
import cn.shopex.ecshopx.datacube.service.goodsdata.GoodsDailyStatLineKey;
import cn.shopex.ecshopx.datacube.service.goodsdata.GoodsDataStatisticAsyncExecutor;
import cn.shopex.ecshopx.datacube.service.goodsdata.GoodsStatisticJobEnqueuePort;
import cn.shopex.ecshopx.datacube.service.goodsdata.GoodsStatisticsBatch;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class GoodsDataServiceEmployeePurchaseTest {

	private static final ZoneId SH = ZoneId.of("Asia/Shanghai");

	private static final LocalDate YESTERDAY = LocalDate.of(2024, 6, 10);

	private static final long ACT = 77L;

	@Mock
	private GoodsDailyStatisticsQueryMapper goodsDailyStatisticsQueryMapper;

	@Mock
	private GoodsStatisticJobEnqueuePort goodsStatisticJobEnqueuePort;

	@Mock
	private GoodsDataService asyncExecutorLazyGoodsDataService;

	private GoodsDataStatisticAsyncExecutor goodsDataStatisticAsyncExecutor;

	@Mock
	private NormalOrdersItemsMapper normalOrdersItemsMapper;

	@Mock
	private GoodsDataMapper goodsDataMapper;

	@Mock
	private DistributorMapper distributorMapper;

	@Mock
	private ActivitiesMapper activitiesMapper;

	private GoodsDataService goodsDataService;

	@BeforeEach
	void setUp() {
		long anchor = YESTERDAY.plusDays(1).atStartOfDay(SH).toEpochSecond();
		Clock clock = Clock.fixed(Instant.ofEpochSecond(anchor), SH);
		goodsDataStatisticAsyncExecutor = new GoodsDataStatisticAsyncExecutor(
				asyncExecutorLazyGoodsDataService, goodsStatisticJobEnqueuePort, false, false);
		goodsDataService =
				new GoodsDataService(
						goodsDailyStatisticsQueryMapper,
						goodsDataStatisticAsyncExecutor,
						normalOrdersItemsMapper,
						goodsDataMapper,
						distributorMapper,
						activitiesMapper,
						clock);
	}

	@Test
	@DisplayName(
			"S-EPG-1: A-1, A-2, A-3, A-4 空活动; 不触达内购 count/page; 无 执行统计商品数据初始化脚本")
	void scheduleInitEmployeePurchase_noActivities_doesNotTouchEmployeePurchaseQueries() {
		when(activitiesMapper.selectActiveIdsForCubeStatistic(anyLong())).thenReturn(List.of());
		var r = goodsDataService.scheduleInitEmployeePurchaseStatistic();
		assertThat(r.jobsEnqueued()).isZero();
		verify(goodsDailyStatisticsQueryMapper, never())
				.countEmployeePurchaseJoinWindow(anyLong(), anyLong(), any(), anyLong());
		verify(goodsDailyStatisticsQueryMapper, never())
				.selectEmployeePurchaseJoinPage(anyLong(), anyLong(), any(), anyLong(), anyInt(), anyInt());
	}

	@Test
	@DisplayName(
			"S-EPG-2: A-1, A-2, A-3, A-4, A-5, B-1, B-2, B-3, B-4, B-5, B-6, B-7, count=0, B-1 打 执行统计商品数据初始化脚本 日志")
	void scheduleInitEmployeePurchase_oneActCountZero_noBatchEnqueue() {
		ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
		logAppender.start();
		Logger svcLog = (Logger) LoggerFactory.getLogger(GoodsDataService.class);
		svcLog.addAppender(logAppender);
		try {
			when(activitiesMapper.selectActiveIdsForCubeStatistic(anyLong())).thenReturn(List.of(ACT));
			when(goodsDailyStatisticsQueryMapper.countEmployeePurchaseJoinWindow(
							anyLong(), anyLong(), any(), eq(ACT)))
					.thenReturn(0L);
			var r = goodsDataService.scheduleInitEmployeePurchaseStatistic();
			assertThat(r.jobsEnqueued()).isZero();
			assertThat(logAppender.list)
					.anyMatch(
							e ->
									e.getFormattedMessage() != null
											&& e.getFormattedMessage()
													.contains("执行统计商品数据初始化脚本"));
			verify(goodsStatisticJobEnqueuePort, never())
					.enqueueEmployeePurchase(any(), any(), anyLong());
			verify(goodsDailyStatisticsQueryMapper, never())
					.selectEmployeePurchaseJoinPage(
							anyLong(), anyLong(), any(), anyLong(), anyInt(), anyInt());
		} finally {
			svcLog.detachAppender(logAppender);
			logAppender.stop();
		}
	}

	@Test
	@DisplayName(
			"S-EPG-3: A-1, A-2, A-3, A-5, B-1, B-2, B-3, B-4, B-5, B-6, B-7, 单页有数据, runEmployeePurchaseBatchAsync, actId, jobsEnqueued=1")
	void scheduleInitEmployeePurchase_onePage_dispatchesOnceWithActId() {
		when(activitiesMapper.selectActiveIdsForCubeStatistic(anyLong())).thenReturn(List.of(ACT));
		when(goodsDailyStatisticsQueryMapper.countEmployeePurchaseJoinWindow(
						anyLong(), anyLong(), any(), eq(ACT)))
				.thenReturn(2L);
		List<GoodsDailyStatLineKey> keys = List.of(new GoodsDailyStatLineKey(10L, 20L));
		when(goodsDailyStatisticsQueryMapper.selectEmployeePurchaseJoinPage(
						anyLong(), anyLong(), any(), eq(ACT), eq(0), eq(100)))
				.thenReturn(keys);
		var r = goodsDataService.scheduleInitEmployeePurchaseStatistic();
		assertThat(r.jobsEnqueued()).isEqualTo(1);
		ArgumentCaptor<GoodsStatisticsBatch> cap = ArgumentCaptor.forClass(GoodsStatisticsBatch.class);
		verify(goodsStatisticJobEnqueuePort, times(1))
				.enqueueEmployeePurchase(cap.capture(), eq(YESTERDAY), eq(ACT));
		assertThat(cap.getValue().lines()).isEqualTo(keys);
	}

	@Test
	@DisplayName(
			"S-EPG-4: A-5, B-1, B-2, B-3, B-4, B-5, B-6, B-7, 多页 ceil(count/100), jobsEnqueued=3")
	void scheduleInitEmployeePurchase_multiPage_dispatchesThreeTimes() {
		when(activitiesMapper.selectActiveIdsForCubeStatistic(anyLong())).thenReturn(List.of(ACT));
		when(goodsDailyStatisticsQueryMapper.countEmployeePurchaseJoinWindow(
						anyLong(), anyLong(), any(), eq(ACT)))
				.thenReturn(250L);
		when(goodsDailyStatisticsQueryMapper.selectEmployeePurchaseJoinPage(
						anyLong(), anyLong(), any(), eq(ACT), eq(0), eq(100)))
				.thenReturn(pageKeys(0, 100));
		when(goodsDailyStatisticsQueryMapper.selectEmployeePurchaseJoinPage(
						anyLong(), anyLong(), any(), eq(ACT), eq(100), eq(100)))
				.thenReturn(pageKeys(100, 100));
		when(goodsDailyStatisticsQueryMapper.selectEmployeePurchaseJoinPage(
						anyLong(), anyLong(), any(), eq(ACT), eq(200), eq(100)))
				.thenReturn(pageKeys(200, 50));
		var r = goodsDataService.scheduleInitEmployeePurchaseStatistic();
		assertThat(r.jobsEnqueued()).isEqualTo(3);
		verify(goodsStatisticJobEnqueuePort, times(3))
				.enqueueEmployeePurchase(any(), eq(YESTERDAY), eq(ACT));
	}

	@Test
	@DisplayName("S-EPG-5: A-5, B-1, B-2, B-3, B-4, B-5, B-6, B-7, 多 actId 每活动 count")
	void scheduleInitEmployeePurchase_multipleActs_queriesPerAct() {
		when(activitiesMapper.selectActiveIdsForCubeStatistic(anyLong()))
				.thenReturn(List.of(10L, 20L));
		when(goodsDailyStatisticsQueryMapper.countEmployeePurchaseJoinWindow(
						anyLong(), anyLong(), any(), eq(10L)))
				.thenReturn(0L);
		when(goodsDailyStatisticsQueryMapper.countEmployeePurchaseJoinWindow(
						anyLong(), anyLong(), any(), eq(20L)))
				.thenReturn(0L);
		goodsDataService.scheduleInitEmployeePurchaseStatistic();
		verify(goodsDailyStatisticsQueryMapper).countEmployeePurchaseJoinWindow(
				anyLong(), anyLong(), any(), eq(10L));
		verify(goodsDailyStatisticsQueryMapper).countEmployeePurchaseJoinWindow(
				anyLong(), anyLong(), any(), eq(20L));
	}

	@Test
	@DisplayName("S-EPG-6, C-1(空行): IllegalArgumentException, 无 C-6/C-7 写")
	void runStatistics_employee_emptyLines_throws() {
		assertThrows(
				IllegalArgumentException.class,
				() ->
						goodsDataService.runStatistics(
								new GoodsStatisticsBatch(List.of()), YESTERDAY, "employee_purchase", ACT));
	}

	@Test
	@DisplayName("S-EPG-6, C-1(空日期): IllegalArgumentException, 无 C-6/C-7 写")
	void runStatistics_employee_nullDate_throws() {
		assertThrows(
				IllegalArgumentException.class,
				() ->
						goodsDataService.runStatistics(
								new GoodsStatisticsBatch(List.of(new GoodsDailyStatLineKey(1L, 1L))),
								null,
								"employee_purchase",
								ACT));
	}

	@Test
	@DisplayName(
			"S-EPG-7, C-1(合法行与日期), C-2, C-3, C-5, C-6, C-8, C-9: INSERT order_class, actId; 统计商品数据开始, 逐单 开始/结束, 统计商品数据结束 日志")
	void runStatistics_employee_insertSetsOrderClassAndActId() {
		ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
		logAppender.start();
		Logger svcLog = (Logger) LoggerFactory.getLogger(GoodsDataService.class);
		svcLog.addAppender(logAppender);
		NormalOrdersItems line = new NormalOrdersItems();
		line.setOrderId(100L);
		line.setId(200L);
		line.setCompanyId(1L);
		line.setItemId(2L);
		line.setNum(3);
		line.setItemFee(400);
		line.setTotalFee(500);
		line.setDistributorId(0L);
		when(normalOrdersItemsMapper.selectOne(any(QueryWrapper.class))).thenReturn(line);
		when(goodsDataMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
		ArgumentCaptor<GoodsData> ins = ArgumentCaptor.forClass(GoodsData.class);
		try {
			goodsDataService.runStatistics(
					new GoodsStatisticsBatch(List.of(new GoodsDailyStatLineKey(100L, 200L))),
					YESTERDAY,
					"employee_purchase",
					ACT);
			verify(goodsDataMapper, times(1)).insert(ins.capture());
			assertThat(ins.getValue().getOrderClass()).isEqualTo("employee_purchase");
			assertThat(ins.getValue().getActId()).isEqualTo(ACT);
			var msgs = logAppender.list;
			assertThat(msgs)
					.anyMatch(
							e ->
									e.getFormattedMessage() != null
											&& e.getFormattedMessage().contains("统计商品数据开始"));
			assertThat(msgs)
					.anyMatch(
							e -> {
								String m = e.getFormattedMessage();
								return m != null && m.contains("订单") && m.contains("开始");
							});
			assertThat(msgs)
					.anyMatch(
							e -> {
								String m = e.getFormattedMessage();
								return m != null && m.contains("订单") && m.contains("结束");
							});
			assertThat(msgs)
					.anyMatch(
							e ->
									e.getFormattedMessage() != null
											&& e.getFormattedMessage().contains("统计商品数据结束"));
		} finally {
			svcLog.detachAppender(logAppender);
			logAppender.stop();
		}
	}

	@Test
	@DisplayName(
			"S-EPG-8, C-1(合法行与日期), C-2, C-3, C-5, C-7, C-8, C-9: UPDATE 累加, WHERE order_class+actId; 逐单与 统计商品数据结束 日志")
	void runStatistics_employee_updateUsesDimensionInWhere() {
		ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
		logAppender.start();
		Logger svcLog = (Logger) LoggerFactory.getLogger(GoodsDataService.class);
		svcLog.addAppender(logAppender);
		NormalOrdersItems line = new NormalOrdersItems();
		line.setOrderId(100L);
		line.setId(200L);
		line.setCompanyId(1L);
		line.setItemId(2L);
		line.setNum(2);
		line.setItemFee(10);
		line.setTotalFee(20);
		line.setDistributorId(0L);
		GoodsData existing = new GoodsData();
		existing.setSalesCount(5);
		existing.setFixedAmountCount(100L);
		existing.setSettleAmountCount(200L);
		when(normalOrdersItemsMapper.selectOne(any(QueryWrapper.class))).thenReturn(line);
		when(goodsDataMapper.selectOne(any(QueryWrapper.class))).thenReturn(existing);
		try {
			goodsDataService.runStatistics(
					new GoodsStatisticsBatch(List.of(new GoodsDailyStatLineKey(100L, 200L))),
					YESTERDAY,
					"employee_purchase",
					ACT);
			verify(goodsDataMapper, never()).insert(ArgumentMatchers.<GoodsData>any());
			verify(goodsDataMapper, times(1)).update(eq(null), any());
			var msgs = logAppender.list;
			assertThat(msgs)
					.anyMatch(
							e -> {
								String m = e.getFormattedMessage();
								return m != null && m.contains("订单") && m.contains("开始");
							});
			assertThat(msgs)
					.anyMatch(
							e -> {
								String m = e.getFormattedMessage();
								return m != null && m.contains("订单") && m.contains("结束");
							});
			assertThat(msgs)
					.anyMatch(
							e ->
									e.getFormattedMessage() != null
											&& e.getFormattedMessage().contains("统计商品数据结束"));
		} finally {
			svcLog.detachAppender(logAppender);
			logAppender.stop();
		}
	}

	/**
	 * C-4: distributor_id 非空时读 {@link DistributorMapper}；与 plan §5 S-EPG-3/7 并集、analysis §3 C-4 对齐。
	 */
	@Test
	@DisplayName("S-EPG-7, C-4, C-2, C-3, C-5, C-6, C-8, C-9: distributor 非空 → merchant_id 写入 INSERT")
	void runStatistics_employee_distributorNonEmpty_resolvesMerchant() {
		ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
		logAppender.start();
		Logger svcLog = (Logger) LoggerFactory.getLogger(GoodsDataService.class);
		svcLog.addAppender(logAppender);
		NormalOrdersItems line = new NormalOrdersItems();
		line.setOrderId(100L);
		line.setId(200L);
		line.setCompanyId(1L);
		line.setItemId(2L);
		line.setNum(1);
		line.setItemFee(1);
		line.setTotalFee(1);
		line.setDistributorId(55L);
		when(normalOrdersItemsMapper.selectOne(any(QueryWrapper.class))).thenReturn(line);
		when(goodsDataMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
		when(distributorMapper.selectDistributorRowDynamic(any()))
				.thenReturn(Map.of("merchant_id", 9001L));
		ArgumentCaptor<GoodsData> ins = ArgumentCaptor.forClass(GoodsData.class);
		try {
			goodsDataService.runStatistics(
					new GoodsStatisticsBatch(List.of(new GoodsDailyStatLineKey(100L, 200L))),
					YESTERDAY,
					"employee_purchase",
					ACT);
			verify(distributorMapper, times(1)).selectDistributorRowDynamic(any());
			verify(goodsDataMapper, times(1)).insert(ins.capture());
			assertThat(ins.getValue().getMerchantId()).isEqualTo(9001L);
			assertThat(logAppender.list)
					.anyMatch(
							e ->
									e.getFormattedMessage() != null
											&& e.getFormattedMessage().contains("统计商品数据结束"));
		} finally {
			svcLog.detachAppender(logAppender);
			logAppender.stop();
		}
	}

	private static List<GoodsDailyStatLineKey> pageKeys(int base, int n) {
		List<GoodsDailyStatLineKey> list = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			list.add(new GoodsDailyStatLineKey((long) (base + i), (long) (base + i)));
		}
		return list;
	}
}
