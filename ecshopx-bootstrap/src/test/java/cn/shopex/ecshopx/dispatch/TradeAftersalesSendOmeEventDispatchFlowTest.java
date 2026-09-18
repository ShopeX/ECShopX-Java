package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.SystemLinkDispatchEventNames;
import cn.shopex.ecshopx.config.TradeAftersalesAsyncFanOutDispatchPublisher;
import cn.shopex.ecshopx.systemlink.dispatch.SystemLinkTradeAftersalesSendOmeDispatchListener;
import cn.shopex.ecshopx.systemlink.service.ome.TradeAftersalesSendOmeNotificationProcessor;
import cn.shopex.ecshopx.thirdparty.dispatch.ThirdPartyTradeAftersalesPushMarketingCenterDispatchListener;
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
 * Trade aftersales OME: {@link TradeAftersalesAsyncFanOutDispatchPublisher} (ASYNC+REDIS parent envelope);
 * production registers SendOme then Marketing Center; harness consumes only the SendOme child message.
 */
@DisplayName("Trade aftersales OME: async fan-out enqueue and DispatchConsumerRuntime consume")
class TradeAftersalesSendOmeEventDispatchFlowTest {

	private static final String LISTENER_OME = "listener:systemlink.trade_aftersales_send_ome";
	private static final String LISTENER_MC = "listener:thirdparty.trade_aftersales_push_marketing_center";

	@Test
	void asyncFanOutPublisher_enqueuesOmeOnDefaultQueue_thenConsumeInvokesOmeProcessor() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();

		TradeAftersalesSendOmeNotificationProcessor omeProcessor = mock(TradeAftersalesSendOmeNotificationProcessor.class);
		TradeRefundPushMarketingCenterProcessor mcProcessor = mock(TradeRefundPushMarketingCenterProcessor.class);

		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_AFTERSALES,
				LISTENER_OME,
				ListenerDispatchOptions.asyncDefaults(),
				new SystemLinkTradeAftersalesSendOmeDispatchListener(omeProcessor));
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_AFTERSALES,
				LISTENER_MC,
				ListenerDispatchOptions.asyncDefaults(),
				new ThirdPartyTradeAftersalesPushMarketingCenterDispatchListener(mcProcessor));

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 31L);
		payload.put("order_id", 7003L);
		payload.put("aftersales_bn", 2026050733333333L);

		new TradeAftersalesAsyncFanOutDispatchPublisher(facade).publish(payload);

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
		assertEquals(SystemLinkDispatchEventNames.EVENT_TRADE_AFTERSALES, omeMsg.messageName());
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
}
