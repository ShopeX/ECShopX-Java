package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.SystemLinkDispatchEventNames;
import cn.shopex.ecshopx.config.TradeAftersalesCancelAsyncFanOutDispatchPublisher;
import cn.shopex.ecshopx.systemlink.dispatch.SystemLinkTradeAftersaleCancelSendOmeDispatchListener;
import cn.shopex.ecshopx.systemlink.service.ome.TradeAftersaleCancelSendOmeNotificationProcessor;
import cn.shopex.ecshopx.thirdparty.dispatch.ThirdPartyTradeAftersalesCancelPushMarketingCenterDispatchListener;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.TradeAftersalesCancelPushMarketingCenterProcessor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Trade aftersales cancel OME: {@link TradeAftersalesCancelAsyncFanOutDispatchPublisher} (ASYNC+REDIS parent envelope);
 * production registers SendOme cancel then Marketing Center; harness consumes only the SendOme cancel child message.
 */
@DisplayName("Trade aftersales cancel OME: async fan-out enqueue and DispatchConsumerRuntime consume")
class TradeAftersalesCancelSendOmeEventDispatchFlowTest {

	private static final String LISTENER_OME = "listener:systemlink.trade_aftersale_cancel_send_ome";
	private static final String LISTENER_MKT =
			"listener:thirdparty.trade_aftersales_cancel_push_marketing_center";

	@Test
	void asyncFanOutPublisher_enqueuesOmeOnDefaultQueue_thenConsumeInvokesCancelProcessor() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();

		TradeAftersaleCancelSendOmeNotificationProcessor cancelOmeProcessor =
				mock(TradeAftersaleCancelSendOmeNotificationProcessor.class);
		TradeAftersalesCancelPushMarketingCenterProcessor marketingProcessor =
				mock(TradeAftersalesCancelPushMarketingCenterProcessor.class);

		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_AFTERSALES_CANCEL,
				LISTENER_OME,
				ListenerDispatchOptions.asyncDefaults(),
				new SystemLinkTradeAftersaleCancelSendOmeDispatchListener(cancelOmeProcessor));
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_AFTERSALES_CANCEL,
				LISTENER_MKT,
				ListenerDispatchOptions.asyncDefaults(),
				new ThirdPartyTradeAftersalesCancelPushMarketingCenterDispatchListener(marketingProcessor));

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 31L);
		payload.put("order_id", 7003L);
		payload.put("aftersales_bn", 2026050733333333L);

		new TradeAftersalesCancelAsyncFanOutDispatchPublisher(facade).publish(payload);

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
		assertEquals(SystemLinkDispatchEventNames.EVENT_TRADE_AFTERSALES_CANCEL, omeMsg.messageName());
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
		verify(cancelOmeProcessor, times(1)).handle(captor.capture());
		Map<String, Object> passed = captor.getValue();
		assertNotNull(passed.get("company_id"));
		assertNotNull(passed.get("order_id"));
		assertEquals(31L, ((Number) passed.get("company_id")).longValue());
		assertEquals(7003L, ((Number) passed.get("order_id")).longValue());

		verify(marketingProcessor, times(0)).handle(any());
	}
}
