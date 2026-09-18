package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.GoodsDispatchEventNames;
import cn.shopex.ecshopx.youshu.dispatch.ItemDeleteYoushuDispatchListener;
import cn.shopex.ecshopx.youshu.service.YoushuItemDeleteSrDataSyncService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ItemDeleteYoushuEventAsyncBusDispatchFlowTest {

	@Test
	void publishItemDelete_async_enqueuesYoushuListenerTask_andConsumerRunsListener() {
		YoushuItemDeleteSrDataSyncService syncService = mock(YoushuItemDeleteSrDataSyncService.class);
		ItemDeleteYoushuDispatchListener listener = new ItemDeleteYoushuDispatchListener(syncService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				GoodsDispatchEventNames.EVENT_ITEM_DELETE,
				GoodsDispatchEventNames.LISTENER_YOUSHU_ITEMS_ITEM_DELETE,
				ListenerDispatchOptions.async("default", null),
				listener);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("item_id", 55L);
		payload.put("company_id", 7L);

		facade.publishEvent(
				GoodsDispatchEventNames.EVENT_ITEM_DELETE,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(GoodsDispatchEventNames.LISTENER_YOUSHU_ITEMS_ITEM_DELETE, msg.listenerName());
		assertEquals("default", msg.queue());
		assertEquals(55L, msg.payload().get("item_id"));
		assertEquals(7L, msg.payload().get("company_id"));

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);
		verify(syncService).syncItemsDelete(7L, 55L);
	}
}
