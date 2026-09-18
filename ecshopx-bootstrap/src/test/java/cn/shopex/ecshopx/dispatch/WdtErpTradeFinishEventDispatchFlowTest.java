package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import cn.shopex.ecshopx.common.dispatch.SystemLinkDispatchEventNames;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class WdtErpTradeFinishEventDispatchFlowTest {

	@Test
	void publishEvent_async_enqueuesSystemLinkWdtErpListener_andConsumerRunsListener() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		AtomicInteger calls = new AtomicInteger();
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_WDT_ERP_TRADE_FINISH,
				"listener:systemlink.trade_finish_send_wdt_erp",
				ListenerDispatchOptions.asyncDefaults(),
				payload -> calls.incrementAndGet());

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 11L);
		payload.put("order_id", 9001L);
		payload.put("distributor_id", 0L);
		payload.put("trade_source_type", "normal");
		payload.put("user_id", 42L);

		facade.publishEvent(
				SystemLinkDispatchEventNames.EVENT_WDT_ERP_TRADE_FINISH,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.EVENT, msg.messageType());
		assertEquals(SystemLinkDispatchEventNames.EVENT_WDT_ERP_TRADE_FINISH, msg.messageName());
		assertEquals("listener:systemlink.trade_finish_send_wdt_erp", msg.listenerName());
		assertEquals("default", msg.queue());
		assertEquals(11L, msg.payload().get("company_id"));
		assertEquals(9001L, msg.payload().get("order_id"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);
		assertEquals(1, calls.get());
	}
}
