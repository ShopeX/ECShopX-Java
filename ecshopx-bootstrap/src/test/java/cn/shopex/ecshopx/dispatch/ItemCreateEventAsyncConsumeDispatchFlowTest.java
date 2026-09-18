package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import cn.shopex.ecshopx.common.dispatch.GoodsDispatchEventNames;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ItemCreateEventAsyncConsumeDispatchFlowTest {

	@Test
	void publishItemCreate_async_enqueuesListenerTask_andConsumerRunsListener() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		AtomicInteger calls = new AtomicInteger();
		registry.registerEventListener(
				GoodsDispatchEventNames.EVENT_ITEM_CREATE,
				"listener:promotions.CreateItemSuccessPromotions",
				ListenerDispatchOptions.asyncDefaults(),
				payload -> calls.incrementAndGet());

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("company_id", 7L);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);
		payload.put("item_ids", List.of(55L));

		facade.publishEvent(
				GoodsDispatchEventNames.EVENT_ITEM_CREATE,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("listener:promotions.CreateItemSuccessPromotions", msg.listenerName());
		assertEquals("default", msg.queue());
		assertEquals(7L, ((Map<?, ?>) msg.payload().get("entities")).get("company_id"));

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);
		assertEquals(1, calls.get());
	}
}
