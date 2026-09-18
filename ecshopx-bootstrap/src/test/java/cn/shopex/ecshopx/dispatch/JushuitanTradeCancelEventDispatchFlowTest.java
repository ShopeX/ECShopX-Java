package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import cn.shopex.ecshopx.common.dispatch.SystemLinkDispatchEventNames;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class JushuitanTradeCancelEventDispatchFlowTest {

	@Test
	void publishEvent_sync_invokesSystemLinkTradeCancelListener_immediately() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		AtomicInteger calls = new AtomicInteger();
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_CANCEL,
				"listener:systemlink.trade_cancel_send_jushuitan",
				ListenerDispatchOptions.asyncDefaults(),
				payload -> calls.incrementAndGet());

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 11L);
		payload.put("order_id", 9001L);
		payload.put("action", "cancel_order");

		facade.publishEvent(
				SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_CANCEL,
				payload,
				DispatchOptions.eventDefaults());

		assertEquals(0, captured.size());
		assertEquals(1, calls.get());
		DispatchMessage last = facade.lastPublishedMessage();
		assertNotNull(last);
		assertEquals(DispatchMode.SYNC, last.dispatchMode());
		assertEquals(DispatchMessageType.EVENT, last.messageType());
		assertEquals(SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_CANCEL, last.messageName());
	}

	@Test
	void publishEvent_sync_invokesListener_when_action_is_pass_refund() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		AtomicInteger calls = new AtomicInteger();
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_CANCEL,
				"listener:systemlink.trade_cancel_send_jushuitan",
				ListenerDispatchOptions.asyncDefaults(),
				payload -> calls.incrementAndGet());

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 11L);
		payload.put("order_id", 9001L);
		payload.put("action", "pass_refund");

		facade.publishEvent(
				SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_CANCEL,
				payload,
				DispatchOptions.eventDefaults());

		assertEquals(0, captured.size());
		assertEquals(1, calls.get());
		DispatchMessage last = facade.lastPublishedMessage();
		assertNotNull(last);
		assertEquals(DispatchMode.SYNC, last.dispatchMode());
		assertEquals(DispatchMessageType.EVENT, last.messageType());
		assertEquals(SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_CANCEL, last.messageName());
	}
}
