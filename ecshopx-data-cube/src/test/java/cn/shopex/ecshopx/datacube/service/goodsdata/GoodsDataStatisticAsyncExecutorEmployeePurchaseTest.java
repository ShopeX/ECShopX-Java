package cn.shopex.ecshopx.datacube.service.goodsdata;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.datacube.service.GoodsDataService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoodsDataStatisticAsyncExecutorEmployeePurchaseTest {

	@Mock
	private GoodsDataService goodsDataService;

	@Mock
	private GoodsStatisticJobEnqueuePort goodsStatisticJobEnqueuePort;

	@Test
	@DisplayName("S-EPG-3/9, B-6: sync-inline=true 同线程序 runStatistics(batch,d,employee_purchase,actId), 非 goods-daily sync")
	void employeePurchaseSyncInline_callsRunStatisticsOnSameBean() {
		var executor = new GoodsDataStatisticAsyncExecutor(goodsDataService, goodsStatisticJobEnqueuePort, false, true);
		LocalDate d = LocalDate.of(2024, 3, 1);
		var batch = new GoodsStatisticsBatch(List.of(new GoodsDailyStatLineKey(1L, 2L)));
		executor.runEmployeePurchaseBatchAsync(batch, d, 99L);
		verify(goodsDataService).runStatistics(batch, d, "employee_purchase", 99L);
		verify(goodsStatisticJobEnqueuePort, never()).enqueueEmployeePurchase(any(), any(), anyLong());
	}

	@Test
	@DisplayName("S-EPG-9, B-6: sync-inline=false 时 enqueueEmployeePurchase, 不直调 runStatistics 于本 bean")
	void employeePurchaseAsync_ignoresGoodsDailySyncInline() {
		var executor = new GoodsDataStatisticAsyncExecutor(goodsDataService, goodsStatisticJobEnqueuePort, true, false);
		LocalDate d = LocalDate.of(2024, 3, 2);
		var batch = new GoodsStatisticsBatch(List.of(new GoodsDailyStatLineKey(3L, 4L)));
		executor.runEmployeePurchaseBatchAsync(batch, d, 42L);
		verify(goodsStatisticJobEnqueuePort).enqueueEmployeePurchase(eq(batch), eq(d), eq(42L));
		verify(goodsDataService, never()).runStatistics(any(), any(), any(), anyLong());
	}
}
