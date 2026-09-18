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
import cn.shopex.ecshopx.thirdparty.service.saaserp.TradeAftersalesSendSaasErpBusService;
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
class TradeAftersalesSendSaasErpDispatchListenerDispatchConsumerRuntimeTest {

	private static final String LISTENER_NAME = "listener:thirdparty.listeners.TradeAftersalesSendSaasErp";

	@Mock
	private TradeAftersalesSendSaasErpBusService tradeAftersalesSendSaasErpBusService;

	private DispatchConsumerRuntime runtimeWithMockBus;

	@BeforeEach
	void setUp() {
		reset(tradeAftersalesSendSaasErpBusService);
		TradeAftersalesSendSaasErpDispatchListener listener =
				new TradeAftersalesSendSaasErpDispatchListener(tradeAftersalesSendSaasErpBusService);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				ThirdPartyDispatchEventNames.EVENT_TRADE_AFTERSALES_SAAS_ERP,
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
	void consume_afterApplyShapedPayload_invokesHandleTradeAftersalesEntitiesOnce() {
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
						ThirdPartyDispatchEventNames.EVENT_TRADE_AFTERSALES_SAAS_ERP,
						payload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T14:00:00Z"),
						"trace-aftersales-saas-erp-apply",
						LISTENER_NAME);

		runtimeWithMockBus.consume(msg, 1);

		ArgumentCaptor<Map<String, Object>> captor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		verify(tradeAftersalesSendSaasErpBusService, times(1)).handleTradeAftersalesEntities(captor.capture());
		Map<String, Object> received = captor.getValue();
		assertThat(received.get("company_id")).isEqualTo(9103L);
		assertThat(received.get("order_id")).isEqualTo(5103L);
		assertThat(received.get("aftersales_bn")).isEqualTo(51_020L);
	}

	@Test
	void consume_afterReviewApproveShapedPayload_withAftersalesActionUpdate_invokesHandleTradeAftersalesEntitiesOnce() {
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 9104L);
		payload.put("order_id", 5104L);
		payload.put("aftersales_bn", 51_021L);
		payload.put("aftersales_type", "ONLY_REFUND");
		payload.put("aftersales_action", "update");
		payload.put("aftersales_status", 2);
		payload.put("progress", 4);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						ThirdPartyDispatchEventNames.EVENT_TRADE_AFTERSALES_SAAS_ERP,
						payload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T15:00:00Z"),
						"trace-refundcheck-agree-saas-erp-update",
						LISTENER_NAME);

		runtimeWithMockBus.consume(msg, 1);

		ArgumentCaptor<Map<String, Object>> captor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		verify(tradeAftersalesSendSaasErpBusService, times(1)).handleTradeAftersalesEntities(captor.capture());
		Map<String, Object> received = captor.getValue();
		assertThat(received.get("aftersales_action")).isEqualTo("update");
		assertThat(received.get("company_id")).isEqualTo(9104L);
		assertThat(received.get("order_id")).isEqualTo(5104L);
		assertThat(received.get("aftersales_bn")).isEqualTo(51_021L);
		assertThat(received.get("aftersales_type")).isEqualTo("ONLY_REFUND");
		assertThat(received.get("aftersales_status")).isEqualTo(2);
		assertThat(received.get("progress")).isEqualTo(4);
	}
}
