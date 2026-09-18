package cn.shopex.ecshopx.thirdparty.dispatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import cn.shopex.ecshopx.thirdparty.service.saaserp.SaasErpCompanySaasBindingReadPort;
import cn.shopex.ecshopx.thirdparty.service.saaserp.SaasErpStoreTradeRefundCancelPort;
import cn.shopex.ecshopx.thirdparty.service.saaserp.TradeRefundCancelSendSaasErpBusService;
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
class TradeRefundCancelSendSaasErpDispatchListenerDispatchConsumerRuntimeTest {

	private static final String LISTENER_NAME = "listener:thirdparty.listeners.TradeRefundCancelSendSaasErp";

	@Mock
	private TradeRefundCancelSendSaasErpBusService tradeRefundCancelSendSaasErpBusService;

	private DispatchConsumerRuntime runtimeWithMockBus;

	@BeforeEach
	void setUp() {
		reset(tradeRefundCancelSendSaasErpBusService);
		TradeRefundCancelSendSaasErpDispatchListener listener =
				new TradeRefundCancelSendSaasErpDispatchListener(tradeRefundCancelSendSaasErpBusService);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				ThirdPartyDispatchEventNames.EVENT_TRADE_REFUND_CANCEL_SAAS_ERP,
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
	void consume_rejectConfirmCancelRefundRowPayload_invokesHandleTradeRefundCancelEntitiesOnce() {
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);
		payload.put("order_id", 200L);
		payload.put("refund_bn", 800L);
		payload.put("aftersales_bn", 50_020L);
		payload.put("user_id", 100L);
		payload.put("refund_status", "REFUSE");

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						ThirdPartyDispatchEventNames.EVENT_TRADE_REFUND_CANCEL_SAAS_ERP,
						payload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T14:00:00Z"),
						"trace-trade-refund-cancel-saas-erp",
						LISTENER_NAME);

		runtimeWithMockBus.consume(msg, 1);

		ArgumentCaptor<Map<String, Object>> captor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		verify(tradeRefundCancelSendSaasErpBusService, times(1)).handleTradeRefundCancelEntities(captor.capture());
		Map<String, Object> passed = captor.getValue();
		assertThat(passed.get("company_id")).isEqualTo(1L);
		assertThat(passed.get("order_id")).isEqualTo(200L);
		assertThat(passed.get("refund_bn")).isEqualTo(800L);
		assertThat(passed.get("refund_status")).isEqualTo("REFUSE");
	}

	@Test
	void consume_whenMissingRefundBn_neverInvokesCancelPort() {
		SaasErpStoreTradeRefundCancelPort port = mock(SaasErpStoreTradeRefundCancelPort.class);
		SaasErpCompanySaasBindingReadPort binding = mock(SaasErpCompanySaasBindingReadPort.class);
		TradeRefundCancelSendSaasErpBusService realBus =
				new TradeRefundCancelSendSaasErpBusService(port, binding);
		TradeRefundCancelSendSaasErpDispatchListener listener = new TradeRefundCancelSendSaasErpDispatchListener(realBus);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				ThirdPartyDispatchEventNames.EVENT_TRADE_REFUND_CANCEL_SAAS_ERP,
				LISTENER_NAME,
				ListenerDispatchOptions.async("default", null),
				listener);
		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 10L);
		payload.put("order_id", 1L);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						ThirdPartyDispatchEventNames.EVENT_TRADE_REFUND_CANCEL_SAAS_ERP,
						payload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T12:30:00Z"),
						"trace-missing-refund-bn-cancel",
						LISTENER_NAME);

		runtime.consume(msg, 1);

		verify(port, never()).callStoreTradeRefundCancel(any());
	}

	@Test
	void consume_whenCompanyIdMissing_neverInvokesCancelPort() {
		SaasErpStoreTradeRefundCancelPort port = mock(SaasErpStoreTradeRefundCancelPort.class);
		SaasErpCompanySaasBindingReadPort binding = mock(SaasErpCompanySaasBindingReadPort.class);
		TradeRefundCancelSendSaasErpBusService realBus =
				new TradeRefundCancelSendSaasErpBusService(port, binding);
		TradeRefundCancelSendSaasErpDispatchListener listener = new TradeRefundCancelSendSaasErpDispatchListener(realBus);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				ThirdPartyDispatchEventNames.EVENT_TRADE_REFUND_CANCEL_SAAS_ERP,
				LISTENER_NAME,
				ListenerDispatchOptions.async("default", null),
				listener);
		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("order_id", 1L);
		payload.put("refund_bn", 9L);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						ThirdPartyDispatchEventNames.EVENT_TRADE_REFUND_CANCEL_SAAS_ERP,
						payload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T12:00:00Z"),
						"trace-missing-company-refund-cancel",
						LISTENER_NAME);

		runtime.consume(msg, 1);

		verify(port, never()).callStoreTradeRefundCancel(any());
		verify(binding, never()).isSaasErpOutboundEnabled(anyLong());
	}
}
