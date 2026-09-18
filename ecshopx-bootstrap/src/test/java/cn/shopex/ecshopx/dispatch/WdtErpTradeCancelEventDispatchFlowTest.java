package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import cn.shopex.ecshopx.common.dispatch.SystemLinkDispatchEventNames;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WdtErpTradeCancelEventDispatchFlowTest {

	@Test
	@DisplayName(
			"DispatchFacade.publishEvent + DispatchOptions.eventDefaults (SYNC): EVENT_WDT_ERP_TRADE_CANCEL "
					+ "invokes registered listener:systemlink.trade_cancel_send_wdt_erp immediately; "
					+ "cancel_order then pass_refund payload shapes")
	void publishEvent_sync_invokesSystemLinkWdtErpTradeCancelListener_immediately() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		AtomicInteger calls = new AtomicInteger();
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_WDT_ERP_TRADE_CANCEL,
				"listener:systemlink.trade_cancel_send_wdt_erp",
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
				SystemLinkDispatchEventNames.EVENT_WDT_ERP_TRADE_CANCEL,
				payload,
				DispatchOptions.eventDefaults());

		assertEquals(0, captured.size());
		assertEquals(1, calls.get());
		DispatchMessage last = facade.lastPublishedMessage();
		assertNotNull(last);
		assertEquals(DispatchMode.SYNC, last.dispatchMode());
		assertEquals(DispatchMessageType.EVENT, last.messageType());
		assertEquals(SystemLinkDispatchEventNames.EVENT_WDT_ERP_TRADE_CANCEL, last.messageName());

		payload.put("action", "pass_refund");
		payload.put("distributor_id", 3L);
		payload.put("cancel_id", 88L);
		facade.publishEvent(
				SystemLinkDispatchEventNames.EVENT_WDT_ERP_TRADE_CANCEL,
				payload,
				DispatchOptions.eventDefaults());

		assertEquals(2, calls.get());
		DispatchMessage lastPassRefund = facade.lastPublishedMessage();
		assertNotNull(lastPassRefund);
		assertEquals(DispatchMode.SYNC, lastPassRefund.dispatchMode());
		assertEquals(DispatchMessageType.EVENT, lastPassRefund.messageType());
		assertEquals(SystemLinkDispatchEventNames.EVENT_WDT_ERP_TRADE_CANCEL, lastPassRefund.messageName());
		@SuppressWarnings("unchecked")
		Map<String, Object> lastPayload = (Map<String, Object>) lastPassRefund.payload();
		assertEquals("pass_refund", lastPayload.get("action"));
		assertEquals(3L, lastPayload.get("distributor_id"));
		assertEquals(88L, lastPayload.get("cancel_id"));
	}

	@Test
	@DisplayName(
			"SYNC publishEvent: reject-refund style payload (company_id, order_id, distributor_id) has no action; listener still runs")
	void publishEvent_sync_rejectRefundPayload_withoutAction() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		AtomicInteger calls = new AtomicInteger();
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_WDT_ERP_TRADE_CANCEL,
				"listener:systemlink.trade_cancel_send_wdt_erp",
				ListenerDispatchOptions.asyncDefaults(),
				payload -> calls.incrementAndGet());

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 7L);
		payload.put("order_id", 1200L);
		payload.put("distributor_id", 0L);

		facade.publishEvent(
				SystemLinkDispatchEventNames.EVENT_WDT_ERP_TRADE_CANCEL,
				payload,
				DispatchOptions.eventDefaults());

		assertEquals(0, captured.size());
		assertEquals(1, calls.get());
		DispatchMessage last = facade.lastPublishedMessage();
		assertNotNull(last);
		@SuppressWarnings("unchecked")
		Map<String, Object> lastPayload = (Map<String, Object>) last.payload();
		assertFalse(lastPayload.containsKey("action"));
		assertEquals(7L, lastPayload.get("company_id"));
		assertEquals(1200L, lastPayload.get("order_id"));
		assertEquals(0L, lastPayload.get("distributor_id"));
	}
}
