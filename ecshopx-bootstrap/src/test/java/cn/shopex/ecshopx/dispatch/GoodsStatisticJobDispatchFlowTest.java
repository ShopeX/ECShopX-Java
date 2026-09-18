package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.DatacubeDispatchJobNames;
import cn.shopex.ecshopx.datacube.dispatch.GoodsStatisticJobHandler;
import cn.shopex.ecshopx.datacube.service.GoodsDataService;
import cn.shopex.ecshopx.datacube.service.goodsdata.GoodsDailyStatLineKey;
import cn.shopex.ecshopx.datacube.service.goodsdata.GoodsStatisticsBatch;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GoodsStatisticJobDispatchFlowTest {

	@Test
	@DisplayName("dispatch enqueues slow queue and consumer invokes default-dimension runStatistics")
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesDefaultRunStatistics() {
		GoodsDataService goodsDataService = mock(GoodsDataService.class);
		GoodsStatisticJobHandler handler = new GoodsStatisticJobHandler(goodsDataService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(DatacubeDispatchJobNames.GOODS_STATISTIC_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> line = new LinkedHashMap<>();
		line.put("order_id", 10L);
		line.put("line_id", 20L);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("count_date", "2025-03-01");
		payload.put("lines", List.of(line));

		facade.dispatchJob(
				DatacubeDispatchJobNames.GOODS_STATISTIC_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals("slow", msg.queue());
		assertNull(msg.delay());
		assertEquals(DatacubeDispatchJobNames.GOODS_STATISTIC_JOB, msg.messageName());
		assertNull(msg.listenerName());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		ArgumentCaptor<GoodsStatisticsBatch> batchCap = ArgumentCaptor.forClass(GoodsStatisticsBatch.class);
		ArgumentCaptor<LocalDate> dateCap = ArgumentCaptor.forClass(LocalDate.class);
		verify(goodsDataService).runStatistics(batchCap.capture(), dateCap.capture());
		assertEquals(LocalDate.of(2025, 3, 1), dateCap.getValue());
		assertEquals(List.of(new GoodsDailyStatLineKey(10L, 20L)), batchCap.getValue().lines());
	}

	@Test
	@DisplayName("dispatch consumer invokes employee_purchase runStatistics when payload carries act_id and order_class")
	void dispatchJob_consumerInvokesEmployeePurchaseOverload() {
		GoodsDataService goodsDataService = mock(GoodsDataService.class);
		GoodsStatisticJobHandler handler = new GoodsStatisticJobHandler(goodsDataService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(DatacubeDispatchJobNames.GOODS_STATISTIC_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> line = new LinkedHashMap<>();
		line.put("order_id", 100L);
		line.put("line_id", 200L);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("count_date", "2024-11-05");
		payload.put("lines", List.of(line));
		payload.put("order_class", "employee_purchase");
		payload.put("act_id", 77L);

		facade.dispatchJob(
				DatacubeDispatchJobNames.GOODS_STATISTIC_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		DispatchMessage msg = captured.get(0);
		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		ArgumentCaptor<GoodsStatisticsBatch> batchCap = ArgumentCaptor.forClass(GoodsStatisticsBatch.class);
		ArgumentCaptor<LocalDate> dateCap = ArgumentCaptor.forClass(LocalDate.class);
		verify(goodsDataService)
				.runStatistics(batchCap.capture(), dateCap.capture(), eq("employee_purchase"), eq(77L));
		assertEquals(LocalDate.of(2024, 11, 5), dateCap.getValue());
		assertEquals(List.of(new GoodsDailyStatLineKey(100L, 200L)), batchCap.getValue().lines());
	}
}
