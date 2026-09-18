package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.GoodsDispatchEventNames;
import cn.shopex.ecshopx.youshu.dispatch.ItemAddYoushuDispatchListener;
import cn.shopex.ecshopx.youshu.service.YoushuItemAddSrDataSyncService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ItemAddYoushuEventAsyncBusDispatchFlowTest {

	@Test
	void publishItemAdd_async_enqueuesYoushuListenerTask_andConsumerRunsListener() {
		YoushuItemAddSrDataSyncService syncService = mock(YoushuItemAddSrDataSyncService.class);
		ItemAddYoushuDispatchListener listener = new ItemAddYoushuDispatchListener(syncService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				GoodsDispatchEventNames.EVENT_ITEM_ADD,
				"listener:youshu.items_item_add",
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
				GoodsDispatchEventNames.EVENT_ITEM_ADD,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("listener:youshu.items_item_add", msg.listenerName());
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
		verify(syncService).syncItemsSku(7L, 55L);
	}
}
