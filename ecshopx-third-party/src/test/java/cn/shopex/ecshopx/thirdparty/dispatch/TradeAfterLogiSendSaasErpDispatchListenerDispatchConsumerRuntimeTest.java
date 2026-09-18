package cn.shopex.ecshopx.thirdparty.dispatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.SystemLinkDispatchEventNames;
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
import cn.shopex.ecshopx.thirdparty.service.saaserp.TradeAfterLogiSendSaasErpBusService;
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
class TradeAfterLogiSendSaasErpDispatchListenerDispatchConsumerRuntimeTest {

	private static final String LISTENER_NAME = "listener:thirdparty.listeners.TradeAfterLogiSendSaasErp";

	@Mock
	private TradeAfterLogiSendSaasErpBusService tradeAfterLogiSendSaasErpBusService;

	private DispatchConsumerRuntime runtimeWithMockBus;

	@BeforeEach
	void setUp() {
		reset(tradeAfterLogiSendSaasErpBusService);
		TradeAfterLogiSendSaasErpDispatchListener listener =
				new TradeAfterLogiSendSaasErpDispatchListener(tradeAfterLogiSendSaasErpBusService);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_AFTERSALES_LOGI,
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
	void consume_shapedPayload_invokesHandleTradeAftersalesLogisticsUpdateOnce() {
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 9103L);
		payload.put("order_id", 5103L);
		payload.put("aftersales_bn", 51_020L);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						SystemLinkDispatchEventNames.EVENT_TRADE_AFTERSALES_LOGI,
						payload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T14:00:00Z"),
						"trace-after-logi-saas-erp",
						LISTENER_NAME);

		runtimeWithMockBus.consume(msg, 1);

		ArgumentCaptor<Map<String, Object>> captor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		verify(tradeAfterLogiSendSaasErpBusService, times(1)).handleTradeAftersalesLogisticsUpdate(captor.capture());
		Map<String, Object> received = captor.getValue();
		assertThat(received.get("company_id")).isEqualTo(9103L);
		assertThat(received.get("order_id")).isEqualTo(5103L);
		assertThat(received.get("aftersales_bn")).isEqualTo(51_020L);
	}
}
