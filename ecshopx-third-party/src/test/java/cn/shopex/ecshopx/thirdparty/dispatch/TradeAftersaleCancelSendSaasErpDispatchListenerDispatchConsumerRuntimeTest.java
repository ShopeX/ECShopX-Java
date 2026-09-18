package cn.shopex.ecshopx.thirdparty.dispatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.ThirdPartyDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchConsumerRuntime;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchMessage;
import cn.shopex.ecshopx.dispatch.DispatchMessageType;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchRetryDecider;
import cn.shopex.ecshopx.dispatch.DispatchStructuredLogger;
import cn.shopex.ecshopx.dispatch.FailedJobRecorder;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchConsumerStateRecorder;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchRegistry;
import cn.shopex.ecshopx.dispatch.ListenerDispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import cn.shopex.ecshopx.thirdparty.service.saaserp.TradeAftersaleCancelSendSaasErpBusService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class TradeAftersaleCancelSendSaasErpDispatchListenerDispatchConsumerRuntimeTest {

	private static final String LISTENER_NAME = "listener:thirdparty.listeners.TradeAftersaleCancelSendSaasErp";

	@Mock
	private TradeAftersaleCancelSendSaasErpBusService tradeAftersaleCancelSendSaasErpBusService;

	private DispatchConsumerRuntime runtimeWithMockBus;

	@BeforeEach
	void setUp() {
		reset(tradeAftersaleCancelSendSaasErpBusService);
		TradeAftersaleCancelSendSaasErpDispatchListener listener =
				new TradeAftersaleCancelSendSaasErpDispatchListener(tradeAftersaleCancelSendSaasErpBusService);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				ThirdPartyDispatchEventNames.EVENT_TRADE_AFTERSALES_CANCEL_SAAS_ERP,
				LISTENER_NAME,
				ListenerDispatchOptions.async("default", null),
				listener);
		runtimeWithMockBus =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
	}

	@Test
	void consume_afterRejectShapedPayload_invokesHandleTradeAftersalesCancelEntitiesOnce() {
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 9103L);
		payload.put("order_id", 5103L);
		payload.put("aftersales_bn", 51_020L);
		payload.put("aftersales_type", "REFUND_GOODS");

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						ThirdPartyDispatchEventNames.EVENT_TRADE_AFTERSALES_CANCEL_SAAS_ERP,
						payload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T14:00:00Z"),
						"trace-aftersales-saas-erp-cancel",
						LISTENER_NAME);

		runtimeWithMockBus.consume(msg, 1);

		ArgumentCaptor<Map<String, Object>> captor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		verify(tradeAftersaleCancelSendSaasErpBusService, times(1)).handleTradeAftersalesCancelEntities(captor.capture());
		Map<String, Object> received = captor.getValue();
		assertThat(received.get("company_id")).isEqualTo(9103L);
		assertThat(received.get("order_id")).isEqualTo(5103L);
		assertThat(received.get("aftersales_bn")).isEqualTo(51_020L);
		assertThat(received.get("aftersales_type")).isEqualTo("REFUND_GOODS");
	}
}
