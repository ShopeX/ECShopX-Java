package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class NormalOrderCancelDispatchFlowTest {

	private static void assertMessageMatchesPlanTable(DispatchMessage msg) {
		assertNotNull(msg);
		assertEquals(OrdersDispatchEventNames.EVENT_NORMAL_ORDER_CANCEL, msg.messageName());
		assertEquals(DispatchMessageType.EVENT, msg.messageType());
		assertEquals(DispatchMode.SYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.SYNC, msg.driverType());
		assertEquals(null, msg.queue());
		assertEquals(null, msg.delay());
		assertEquals("listener:orders.normal_order_cancel_auto_pass", msg.listenerName());
	}

	@Test
	void publishEvent_sync_invokesAutoPassListener_immediately() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		AtomicInteger calls = new AtomicInteger();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_CANCEL,
				"listener:orders.normal_order_cancel_auto_pass",
				ListenerDispatchOptions.asyncDefaults(),
				payload -> {
					calls.incrementAndGet();
					assertEquals(11L, payload.get("company_id"));
					assertEquals(9001L, payload.get("order_id"));
					assertEquals("admin_normal_order_full_cancel", payload.get("source"));
				});

		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 11L);
		payload.put("order_id", 9001L);
		payload.put("source", "admin_normal_order_full_cancel");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_CANCEL, payload, DispatchOptions.eventDefaults());

		assertEquals(1, calls.get());
		List<DispatchMessage> all = facade.publishedMessages();
		assertEquals(2, all.size());
		assertMessageMatchesPlanTable(all.get(0));
		assertMessageMatchesPlanTable(all.get(1));
		for (DispatchMessage msg : all) {
			assertMessageMatchesPlanTable(msg);
		}
		DispatchMessage last = facade.lastPublishedMessage();
		assertMessageMatchesPlanTable(last);
	}
}
