package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.SystemLinkDispatchEventNames;
import cn.shopex.ecshopx.config.TradeRefundAsyncFanOutDispatchPublisher;
import cn.shopex.ecshopx.systemlink.dispatch.SystemLinkTradeRefundSendOmeDispatchListener;
import cn.shopex.ecshopx.systemlink.service.ome.TradeRefundSendOmeNotificationProcessor;
import cn.shopex.ecshopx.thirdparty.dispatch.ThirdPartyTradeRefundPushMarketingCenterDispatchListener;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.TradeRefundPushMarketingCenterProcessor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Partial-cancel path uses {@link TradeRefundAsyncFanOutDispatchPublisher} (ASYNC+REDIS parent envelope);
 * {@link DispatchFanOutPlanner} emits one ASYNC child per registered listener. Production registers OME
 * then Marketing Center; this harness mirrors both and consumes only the OME child message.
 */
@DisplayName("TradeRefund OME: async fan-out enqueue and DispatchConsumerRuntime consume")
class TradeRefundSendOmeEventDispatchFlowTest {

	private static final String LISTENER_OME = "listener:systemlink.trade_refund_send_ome";
	private static final String LISTENER_MC = "listener:thirdparty.trade_refund_push_marketing_center";

	@Test
	void asyncFanOutPublisher_enqueuesOmeOnDefaultQueue_thenConsumeInvokesOmeProcessor() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();

		TradeRefundSendOmeNotificationProcessor omeProcessor = mock(TradeRefundSendOmeNotificationProcessor.class);
		TradeRefundPushMarketingCenterProcessor mcProcessor = mock(TradeRefundPushMarketingCenterProcessor.class);

		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_REFUND,
				LISTENER_OME,
				ListenerDispatchOptions.asyncDefaults(),
				new SystemLinkTradeRefundSendOmeDispatchListener(omeProcessor));
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_REFUND,
				LISTENER_MC,
				ListenerDispatchOptions.asyncDefaults(),
				new ThirdPartyTradeRefundPushMarketingCenterDispatchListener(mcProcessor));

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 31L);
		payload.put("order_id", 7003L);
		payload.put("aftersales_bn", 2026050733333333L);
		payload.put("refund_bn", 2202505073333333333L);

		new TradeRefundAsyncFanOutDispatchPublisher(facade).publish(payload);

		assertEquals(2, captured.size());

		List<DispatchMessage> omeMessages =
				captured.stream()
						.filter(m -> LISTENER_OME.equals(m.listenerName()))
						.collect(Collectors.toList());
		assertEquals(1, omeMessages.size());
		DispatchMessage omeMsg = omeMessages.get(0);
		assertEquals(DispatchMessageType.EVENT, omeMsg.messageType());
		assertEquals(DispatchMode.ASYNC, omeMsg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, omeMsg.driverType());
		assertEquals(SystemLinkDispatchEventNames.EVENT_TRADE_REFUND, omeMsg.messageName());
		assertEquals("default", omeMsg.queue());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(omeMsg, 1);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(omeProcessor, times(1)).handle(captor.capture());
		Map<String, Object> passed = captor.getValue();
		assertNotNull(passed.get("company_id"));
		assertNotNull(passed.get("order_id"));
		assertEquals(31L, ((Number) passed.get("company_id")).longValue());
		assertEquals(7003L, ((Number) passed.get("order_id")).longValue());

		verify(mcProcessor, times(0)).handle(any());
	}

	@Test
	void asyncFanOutPublisher_adminFullCancelPayload_enqueuesOmeThenConsumeInvokesOmeProcessorOnce() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();

		TradeRefundSendOmeNotificationProcessor omeProcessor = mock(TradeRefundSendOmeNotificationProcessor.class);
		TradeRefundPushMarketingCenterProcessor mcProcessor = mock(TradeRefundPushMarketingCenterProcessor.class);

		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_REFUND,
				LISTENER_OME,
				ListenerDispatchOptions.asyncDefaults(),
				new SystemLinkTradeRefundSendOmeDispatchListener(omeProcessor));
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_REFUND,
				LISTENER_MC,
				ListenerDispatchOptions.asyncDefaults(),
				new ThirdPartyTradeRefundPushMarketingCenterDispatchListener(mcProcessor));

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		// Keys aligned with payed admin full-cancel / cancelToMap + action (AdminNormalOrderFullCancelService).
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", "1");
		payload.put("order_id", "200");
		payload.put("cancel_id", "55");
		payload.put("cancel_from", "shop");
		payload.put("action", "cancel_order");
		payload.put("user_id", "100");
		payload.put("shop_id", 1L);
		payload.put("supplier_id", 0L);

		new TradeRefundAsyncFanOutDispatchPublisher(facade).publish(payload);

		assertEquals(2, captured.size());

		List<DispatchMessage> omeMessages =
				captured.stream()
						.filter(m -> LISTENER_OME.equals(m.listenerName()))
						.collect(Collectors.toList());
		assertEquals(1, omeMessages.size());
		DispatchMessage omeMsg = omeMessages.get(0);
		assertEquals(DispatchMessageType.EVENT, omeMsg.messageType());
		assertEquals(DispatchMode.ASYNC, omeMsg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, omeMsg.driverType());
		assertEquals(SystemLinkDispatchEventNames.EVENT_TRADE_REFUND, omeMsg.messageName());
		assertEquals("default", omeMsg.queue());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(omeMsg, 1);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(omeProcessor, times(1)).handle(captor.capture());
		Map<String, Object> passed = captor.getValue();
		assertEquals("cancel_order", passed.get("action"));
		assertEquals("shop", String.valueOf(passed.get("cancel_from")));
		assertEquals("200", String.valueOf(passed.get("order_id")));
		assertEquals("1", String.valueOf(passed.get("company_id")));
		assertEquals("55", String.valueOf(passed.get("cancel_id")));
		assertNotNull(passed.get("user_id"));

		verify(mcProcessor, times(0)).handle(any());
	}
}
