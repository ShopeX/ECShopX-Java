package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class WxOrderShippingEventDispatchFlowTest {

	@Test
	void publishEvent_async_enqueuesOrdersWxOrderShippingListener_andConsumerRunsListener() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		AtomicInteger calls = new AtomicInteger();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_WX_ORDER_SHIPPING,
				"listener:orders.wx_order_shipping",
				ListenerDispatchOptions.asyncDefaults(),
				payload -> calls.incrementAndGet());

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 11L);
		payload.put("order_id", 9001L);
		payload.put("trade_id", "tr-ziti-1");
		payload.put("user_id", 42L);
		payload.put("receipt_type", "ziti");
		payload.put("delivery_type", "batch");
		payload.put("is_all_delivered", Boolean.TRUE);
		payload.put("delivery_corp", "");
		payload.put("delivery_code", "");
		payload.put("delivery_items", List.of());

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_WX_ORDER_SHIPPING,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("listener:orders.wx_order_shipping", msg.listenerName());
		assertEquals("default", msg.queue());
		assertEquals(11L, msg.payload().get("company_id"));
		assertEquals(9001L, msg.payload().get("order_id"));
		assertEquals("ziti", msg.payload().get("receipt_type"));

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
	void publishEvent_async_enqueuesListener_withLogisticsDeliveryPayload() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		AtomicInteger calls = new AtomicInteger();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_WX_ORDER_SHIPPING,
				"listener:orders.wx_order_shipping",
				ListenerDispatchOptions.asyncDefaults(),
				payload -> calls.incrementAndGet());

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 11L);
		payload.put("order_id", 9002L);
		payload.put("trade_id", "tr-logistics-1");
		payload.put("user_id", 43L);
		payload.put("receipt_type", "logistics");
		payload.put("delivery_type", "sep");
		payload.put("is_all_delivered", Boolean.FALSE);
		payload.put("delivery_corp", "SF");
		payload.put("delivery_code", "SF999");
		payload.put(
				"delivery_items",
				List.of(Map.of("item_name", "SKU-A", "num", 1)));

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_WX_ORDER_SHIPPING,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("listener:orders.wx_order_shipping", msg.listenerName());
		assertEquals("default", msg.queue());
		assertEquals("logistics", msg.payload().get("receipt_type"));
		assertEquals("sep", msg.payload().get("delivery_type"));
		assertEquals(Boolean.FALSE, msg.payload().get("is_all_delivered"));
		assertEquals("SF", msg.payload().get("delivery_corp"));
		assertEquals("SF999", msg.payload().get("delivery_code"));
		assertEquals(11L, msg.payload().get("company_id"));

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
