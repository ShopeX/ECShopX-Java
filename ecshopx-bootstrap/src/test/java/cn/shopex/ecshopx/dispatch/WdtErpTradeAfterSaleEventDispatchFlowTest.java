package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.SystemLinkDispatchEventNames;
import cn.shopex.ecshopx.systemlink.dispatch.SystemLinkWdtErpTradeAfterSaleDispatchListener;
import cn.shopex.ecshopx.systemlink.service.wdterp.WdtErpTradeAfterSaleNotificationProcessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * 内存注册表与同步 driver 组装的轻量 harness：验证旺店通售后事件经 {@link DispatchFacade#publishEvent} 异步入队后，
 * 由 {@link DispatchConsumerRuntime#consume} 驱动出队并分派至已注册的售后 listener，直至通知处理器被调用。
 * 与「退款审核同意」后在业务侧触发的 Bus 投递在出队与监听链路上形成闭环验证（无外部中间件进程）。
 */
@DisplayName("旺店通 ERP 售后 Dispatch：异步入队与出队执行（与共用售后审核拒绝路径衔接）")
class WdtErpTradeAfterSaleEventDispatchFlowTest {

	@Test
	void publishEvent_async_enqueuesOnDefaultQueue_thenDispatchConsumerRuntimeConsumeInvokesListener() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		AtomicInteger calls = new AtomicInteger();
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_WDT_ERP_TRADE_AFTERSALE,
				"listener:systemlink.trade_after_sale_send_wdt_erp",
				ListenerDispatchOptions.async("default", null),
				payload -> calls.incrementAndGet());

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 11L);
		payload.put("order_id", 9001L);
		payload.put("aftersales_bn", 2026050611111111L);
		payload.put("distributor_id", 5L);

		facade.publishEvent(
				SystemLinkDispatchEventNames.EVENT_WDT_ERP_TRADE_AFTERSALE,
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
		assertEquals(SystemLinkDispatchEventNames.EVENT_WDT_ERP_TRADE_AFTERSALE, msg.messageName());
		assertEquals("listener:systemlink.trade_after_sale_send_wdt_erp", msg.listenerName());
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
	void publishEvent_async_payload_contains_company_id_distributor_id_aftersales_bn_order_id() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		WdtErpTradeAfterSaleNotificationProcessor mockedProcessor =
				mock(WdtErpTradeAfterSaleNotificationProcessor.class);
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_WDT_ERP_TRADE_AFTERSALE,
				"listener:systemlink.trade_after_sale_send_wdt_erp",
				ListenerDispatchOptions.async("default", null),
				new SystemLinkWdtErpTradeAfterSaleDispatchListener(mockedProcessor));

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 22L);
		payload.put("order_id", 8002L);
		payload.put("aftersales_bn", 2026050622222222L);
		payload.put("distributor_id", 7L);

		facade.publishEvent(
				SystemLinkDispatchEventNames.EVENT_WDT_ERP_TRADE_AFTERSALE,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertNotNull(msg.payload().get("company_id"));
		assertNotNull(msg.payload().get("distributor_id"));
		assertNotNull(msg.payload().get("aftersales_bn"));
		assertNotNull(msg.payload().get("order_id"));
		assertEquals(22L, ((Number) msg.payload().get("company_id")).longValue());
		assertEquals(8002L, ((Number) msg.payload().get("order_id")).longValue());
		assertEquals(2026050622222222L, ((Number) msg.payload().get("aftersales_bn")).longValue());
		assertEquals(7L, ((Number) msg.payload().get("distributor_id")).longValue());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(mockedProcessor, times(1)).handle(payloadCaptor.capture());
		Map<String, Object> passed = payloadCaptor.getValue();
		assertEquals(22L, ((Number) passed.get("company_id")).longValue());
		assertEquals(8002L, ((Number) passed.get("order_id")).longValue());
		assertEquals(2026050622222222L, ((Number) passed.get("aftersales_bn")).longValue());
		assertEquals(7L, ((Number) passed.get("distributor_id")).longValue());
	}
}
