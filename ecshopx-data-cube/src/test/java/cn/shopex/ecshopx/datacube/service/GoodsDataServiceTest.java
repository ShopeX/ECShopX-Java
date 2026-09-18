package cn.shopex.ecshopx.datacube.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import cn.shopex.ecshopx.datacube.service.goodsdata.ScheduleGoodsDailyInitResult;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class GoodsDataServiceTest {

	private static final ZoneId SH = ZoneId.of("Asia/Shanghai");

	private static final LocalDate YESTERDAY = LocalDate.of(2024, 6, 10);

	@Mock
	private GoodsDailyStatisticsQueryMapper goodsDailyStatisticsQueryMapper;

	@Mock
	private GoodsStatisticJobEnqueuePort goodsStatisticJobEnqueuePort;

	/**
	 * Constructor dependency of {@link GoodsDataStatisticAsyncExecutor}; unused when {@code sync-inline=false}.
	 */
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
	void scheduleInitStatistic_noRows_doesNotDispatch_batchesZero() {
		when(goodsDailyStatisticsQueryMapper.countKernelJoinWindow(anyLong(), anyLong(), any()))
				.thenReturn(0L);
		ScheduleGoodsDailyInitResult r = goodsDataService.scheduleInitStatistic();
		assertThat(r.jobsEnqueued()).isZero();
		verify(goodsStatisticJobEnqueuePort, never()).enqueue(any(), any());
		verify(goodsDailyStatisticsQueryMapper, never())
				.selectKernelJoinPage(anyLong(), anyLong(), any(), anyInt(), anyInt());
	}

	@Test
	void scheduleInitStatistic_onePage_dispatchesOnce() {
		List<GoodsDailyStatLineKey> keys = List.of(new GoodsDailyStatLineKey(10L, 20L));
		when(goodsDailyStatisticsQueryMapper.countKernelJoinWindow(anyLong(), anyLong(), any()))
				.thenReturn(2L);
		when(goodsDailyStatisticsQueryMapper.selectKernelJoinPage(anyLong(), anyLong(), any(), eq(0), eq(100)))
				.thenReturn(keys);
		ScheduleGoodsDailyInitResult r = goodsDataService.scheduleInitStatistic();
		assertThat(r.jobsEnqueued()).isEqualTo(1);
		ArgumentCaptor<GoodsStatisticsBatch> cap = ArgumentCaptor.forClass(GoodsStatisticsBatch.class);
		ArgumentCaptor<LocalDate> dateCap = ArgumentCaptor.forClass(LocalDate.class);
		verify(goodsStatisticJobEnqueuePort, times(1)).enqueue(cap.capture(), dateCap.capture());
		assertThat(dateCap.getValue()).isEqualTo(YESTERDAY);
		assertThat(cap.getValue().lines()).isEqualTo(keys);
	}

	@Test
	void scheduleInitStatistic_multiPage_dispatchesThreeTimes() {
		when(goodsDailyStatisticsQueryMapper.countKernelJoinWindow(anyLong(), anyLong(), any()))
				.thenReturn(250L);
		List<GoodsDailyStatLineKey> p1 = pageKeys(0, 100);
		List<GoodsDailyStatLineKey> p2 = pageKeys(100, 100);
		List<GoodsDailyStatLineKey> p3 = pageKeys(200, 50);
		when(goodsDailyStatisticsQueryMapper.selectKernelJoinPage(anyLong(), anyLong(), any(), eq(0), eq(100)))
				.thenReturn(p1);
		when(goodsDailyStatisticsQueryMapper.selectKernelJoinPage(anyLong(), anyLong(), any(), eq(100), eq(100)))
				.thenReturn(p2);
		when(goodsDailyStatisticsQueryMapper.selectKernelJoinPage(anyLong(), anyLong(), any(), eq(200), eq(100)))
				.thenReturn(p3);
		ScheduleGoodsDailyInitResult r = goodsDataService.scheduleInitStatistic();
		assertThat(r.jobsEnqueued()).isEqualTo(3);
		verify(goodsStatisticJobEnqueuePort, times(3)).enqueue(any(), eq(YESTERDAY));
	}

	@Test
	void runStatistics_emptyOrderIds_throws() {
		assertThrows(
				IllegalArgumentException.class,
				() -> goodsDataService.runStatistics(new GoodsStatisticsBatch(List.of()), LocalDate.now()));
	}

	@Test
	void runStatistics_nullDate_throws() {
		assertThrows(
				IllegalArgumentException.class,
				() ->
						goodsDataService.runStatistics(
								new GoodsStatisticsBatch(List.of(new GoodsDailyStatLineKey(1L, 1L))), null));
	}

	@Test
	void runStatistics_insertWhenNoAggregateRow() {
		LocalDate d = LocalDate.of(2024, 6, 10);
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
		goodsDataService.runStatistics(
				new GoodsStatisticsBatch(List.of(new GoodsDailyStatLineKey(100L, 200L))), d);
		verify(goodsDataMapper, times(1)).insert(ArgumentMatchers.<GoodsData>any());
		verify(goodsDataMapper, never()).update(isNull(), any());
	}

	@Test
	void runStatistics_updateAccumulates() {
		LocalDate d = LocalDate.of(2024, 6, 10);
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
		goodsDataService.runStatistics(
				new GoodsStatisticsBatch(List.of(new GoodsDailyStatLineKey(100L, 200L))), d);
		verify(goodsDataMapper, never()).insert(ArgumentMatchers.<GoodsData>any());
		verify(goodsDataMapper, times(1)).update(isNull(), any());
	}

	@Test
	void runStatistics_resolvesMerchantFromDistributor() {
		LocalDate d = LocalDate.of(2024, 6, 10);
		NormalOrdersItems line = new NormalOrdersItems();
		line.setOrderId(100L);
		line.setId(200L);
		line.setCompanyId(1L);
		line.setItemId(2L);
		line.setNum(1);
		line.setItemFee(1);
		line.setTotalFee(1);
		line.setDistributorId(55L);
		Map<String, Object> dist = new LinkedHashMap<>();
		dist.put("merchant_id", 999L);
		when(normalOrdersItemsMapper.selectOne(any(QueryWrapper.class))).thenReturn(line);
		when(distributorMapper.selectDistributorRowDynamic(any())).thenReturn(dist);
		when(goodsDataMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
		ArgumentCaptor<GoodsData> ins = ArgumentCaptor.forClass(GoodsData.class);
		goodsDataService.runStatistics(
				new GoodsStatisticsBatch(List.of(new GoodsDailyStatLineKey(100L, 200L))), d);
		verify(goodsDataMapper, times(1)).insert(ins.capture());
		assertThat(ins.getValue().getMerchantId()).isEqualTo(999L);
	}

	@Test
	void scheduleInitStatistic_asyncExecutorContract_perNonEmptyPage() {
		when(goodsDailyStatisticsQueryMapper.countKernelJoinWindow(anyLong(), anyLong(), any()))
				.thenReturn(100L);
		when(goodsDailyStatisticsQueryMapper.selectKernelJoinPage(anyLong(), anyLong(), any(), eq(0), eq(100)))
				.thenReturn(List.of(new GoodsDailyStatLineKey(1L, 1L)));
		goodsDataService.scheduleInitStatistic();
		verify(goodsStatisticJobEnqueuePort, times(1)).enqueue(any(), eq(YESTERDAY));
	}

	private static List<GoodsDailyStatLineKey> pageKeys(int base, int n) {
		List<GoodsDailyStatLineKey> list = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			list.add(new GoodsDailyStatLineKey((long) (base + i), (long) (base + i)));
		}
		return list;
	}
}
