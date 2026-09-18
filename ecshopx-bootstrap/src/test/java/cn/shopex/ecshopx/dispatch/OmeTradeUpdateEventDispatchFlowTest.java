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

class OmeTradeUpdateEventDispatchFlowTest {

	@Test
	void publishEvent_async_enqueuesOmeTradeUpdateListener_thenDispatchConsumerRuntimeConsumeInvokesListener() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		AtomicInteger calls = new AtomicInteger();
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_OME_TRADE_UPDATE,
				"listener:systemlink.trade_update_send_ome",
				ListenerDispatchOptions.asyncDefaults(),
				payload -> calls.incrementAndGet());

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 11L);
		payload.put("order_id", "9001");
		payload.put("order_class", "normal_groups");
		payload.put("user_id", 42L);

		facade.publishEvent(
				SystemLinkDispatchEventNames.EVENT_OME_TRADE_UPDATE,
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
		assertEquals(SystemLinkDispatchEventNames.EVENT_OME_TRADE_UPDATE, msg.messageName());
		assertEquals("listener:systemlink.trade_update_send_ome", msg.listenerName());
		assertEquals("default", msg.queue());

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

	@Test
	void publishEvent_async_payload_contract_company_id_order_id_order_class_user_id_matches_entities_map() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		AtomicInteger calls = new AtomicInteger();
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_OME_TRADE_UPDATE,
				"listener:systemlink.trade_update_send_ome",
				ListenerDispatchOptions.asyncDefaults(),
				payload -> {
					calls.incrementAndGet();
					assertEquals(7L, payload.get("company_id"));
					assertEquals("501", payload.get("order_id"));
					assertEquals("normal_groups", payload.get("order_class"));
					assertEquals(99L, payload.get("user_id"));
				});

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 7L);
		payload.put("order_id", "501");
		payload.put("order_class", "normal_groups");
		payload.put("user_id", 99L);

		facade.publishEvent(
				SystemLinkDispatchEventNames.EVENT_OME_TRADE_UPDATE,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		Map<String, Object> p = msg.payload();
		assertEquals(7L, p.get("company_id"));
		assertEquals("501", p.get("order_id"));
		assertEquals("normal_groups", p.get("order_class"));
		assertEquals(99L, p.get("user_id"));

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
