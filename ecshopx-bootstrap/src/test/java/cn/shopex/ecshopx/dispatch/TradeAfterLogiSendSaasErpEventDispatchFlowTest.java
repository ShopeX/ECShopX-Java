package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.SystemLinkDispatchEventNames;
import cn.shopex.ecshopx.config.TradeAftersalesLogiAsyncDispatchPublisher;
import cn.shopex.ecshopx.thirdparty.dispatch.TradeAfterLogiSendSaasErpDispatchListener;
import cn.shopex.ecshopx.thirdparty.service.saaserp.TradeAfterLogiSendSaasErpBusService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("Trade aftersales logistics Saas ERP arm: ASYNC+REDIS enqueue and DispatchConsumerRuntime consume")
class TradeAfterLogiSendSaasErpEventDispatchFlowTest {

	private static final String LISTENER_SAAS_ERP = "listener:thirdparty.listeners.TradeAfterLogiSendSaasErp";

	@Test
	void asyncPublisher_enqueuesSaasErpLogiListenerOnDefaultQueue_thenConsumeInvokesBusService() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();

		TradeAfterLogiSendSaasErpBusService mockBusService = mock(TradeAfterLogiSendSaasErpBusService.class);

		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_AFTERSALES_LOGI,
				LISTENER_SAAS_ERP,
				ListenerDispatchOptions.async("default", null),
				new TradeAfterLogiSendSaasErpDispatchListener(mockBusService));

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 31L);
		payload.put("order_id", 7003L);
		payload.put("aftersales_bn", 2026050733333333L);

		new TradeAftersalesLogiAsyncDispatchPublisher(facade).publish(payload);

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.EVENT, msg.messageType());
		assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals(SystemLinkDispatchEventNames.EVENT_TRADE_AFTERSALES_LOGI, msg.messageName());
		assertEquals(LISTENER_SAAS_ERP, msg.listenerName());
		assertEquals("default", msg.queue());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(mockBusService, times(1)).handleTradeAftersalesLogisticsUpdate(captor.capture());
		Map<String, Object> passed = captor.getValue();
		assertNotNull(passed.get("company_id"));
		assertNotNull(passed.get("aftersales_bn"));
		assertEquals(31L, ((Number) passed.get("company_id")).longValue());
		assertEquals(2026050733333333L, ((Number) passed.get("aftersales_bn")).longValue());
	}
}
