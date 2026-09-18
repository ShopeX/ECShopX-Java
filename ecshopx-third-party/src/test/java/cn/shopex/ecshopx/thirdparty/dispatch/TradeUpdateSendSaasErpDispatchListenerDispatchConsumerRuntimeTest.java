package cn.shopex.ecshopx.thirdparty.dispatch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import cn.shopex.ecshopx.common.saaserp.TradeUpdateGroupMemberOrdersPort;
import cn.shopex.ecshopx.thirdparty.service.saaserp.SaasErpStoreTradeAddPort;
import cn.shopex.ecshopx.thirdparty.service.saaserp.TradeUpdateSendSaasErpBusService;
import java.time.Instant;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TradeUpdateSendSaasErpDispatchListenerDispatchConsumerRuntimeTest {

	private static final String LISTENER_NAME = "listener:thirdparty.listeners.TradeUpdateSendSaasErp";

	@Mock
	private TradeUpdateSendSaasErpBusService tradeUpdateSendSaasErpBusService;

	private DispatchConsumerRuntime runtimeWithMockBus;

	@BeforeEach
	void setUp() {
		TradeUpdateSendSaasErpDispatchListener listener =
				new TradeUpdateSendSaasErpDispatchListener(tradeUpdateSendSaasErpBusService);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				ThirdPartyDispatchEventNames.EVENT_TRADE_UPDATE,
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
	void consume_whenPayloadComplete_invokesBusServiceOnce() {
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 10L);
		payload.put("order_id", "99");
		payload.put("user_id", 3L);
		payload.put("order_class", "normal_groups");

		Instant occurredAt = Instant.parse("2026-05-10T12:00:00Z");
		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						ThirdPartyDispatchEventNames.EVENT_TRADE_UPDATE,
						payload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						occurredAt,
						"trace-trade-update-saas-erp",
						LISTENER_NAME);

		runtimeWithMockBus.consume(msg, 1);

		verify(tradeUpdateSendSaasErpBusService, times(1)).handleTradeUpdateEntities(payload);
	}

	@Test
	void consume_whenZitiWriteoffOrderInfoPayload_invokesBusServiceOnce() {
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 12L);
		payload.put("order_id", "70001");
		payload.put("user_id", 501L);
		payload.put("order_class", "normal");
		payload.put("order_status", "DONE");
		payload.put("ziti_status", "DONE");

		Instant occurredAt = Instant.parse("2026-05-10T14:00:00Z");
		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						ThirdPartyDispatchEventNames.EVENT_TRADE_UPDATE,
						payload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						occurredAt,
						"trace-ziti-writeoff-trade-update",
						LISTENER_NAME);

		runtimeWithMockBus.consume(msg, 1);

		verify(tradeUpdateSendSaasErpBusService, times(1)).handleTradeUpdateEntities(payload);
	}

	/**
	 * Mirrors {@code AdminOrderPassRefundService#passRefund} → {@code ThirdPartyTradeUpdateDispatchPublisher} payload
	 * for admin API confirm-cancel agree (event-208 entry-01): normal order_class with CANCEL status after association
	 * update.
	 */
	@Test
	void consume_whenPassRefundCancelAssociationPayload_invokesBusServiceOnce() {
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);
		payload.put("order_id", "200");
		payload.put("user_id", 10L);
		payload.put("order_class", "normal");
		payload.put("order_status", "CANCEL");
		payload.put("trade_id", "9001");

		Instant occurredAt = Instant.parse("2026-05-10T16:00:00Z");
		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						ThirdPartyDispatchEventNames.EVENT_TRADE_UPDATE,
						payload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						occurredAt,
						"trace-pass-refund-cancel-assoc",
						LISTENER_NAME);

		runtimeWithMockBus.consume(msg, 1);

		verify(tradeUpdateSendSaasErpBusService, times(1)).handleTradeUpdateEntities(payload);
	}

	@Test
	void consume_whenNormalGroupsCancelAssociationPayload_invokesBusServiceOnce() {
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 10L);
		payload.put("order_id", "101");
		payload.put("user_id", 3L);
		payload.put("order_class", "normal_groups");
		payload.put("order_status", "CANCEL");

		Instant occurredAt = Instant.parse("2026-05-10T15:00:00Z");
		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						ThirdPartyDispatchEventNames.EVENT_TRADE_UPDATE,
						payload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						occurredAt,
						"trace-normal-groups-cancel-assoc",
						LISTENER_NAME);

		runtimeWithMockBus.consume(msg, 1);

		verify(tradeUpdateSendSaasErpBusService, times(1)).handleTradeUpdateEntities(payload);
	}

	@Test
	void consume_whenCompanyIdMissing_skipsBusService() {
		SaasErpStoreTradeAddPort port = mock(SaasErpStoreTradeAddPort.class);
		TradeUpdateGroupMemberOrdersPort groupPort = mock(TradeUpdateGroupMemberOrdersPort.class);
		TradeUpdateSendSaasErpBusService realBus = new TradeUpdateSendSaasErpBusService(groupPort, port);
		TradeUpdateSendSaasErpDispatchListener listener = new TradeUpdateSendSaasErpDispatchListener(realBus);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				ThirdPartyDispatchEventNames.EVENT_TRADE_UPDATE,
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
		payload.put("order_id", "1");
		payload.put("order_class", "normal_groups");

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						ThirdPartyDispatchEventNames.EVENT_TRADE_UPDATE,
						payload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T12:00:00Z"),
						"trace-missing-company",
						LISTENER_NAME);

		runtime.consume(msg, 1);

		verify(port, never()).callStoreTradeAdd(any());
		verify(groupPort, never()).listPaidTeamOrderRowsForLeader(anyLong(), anyString(), anyLong());
	}
}
