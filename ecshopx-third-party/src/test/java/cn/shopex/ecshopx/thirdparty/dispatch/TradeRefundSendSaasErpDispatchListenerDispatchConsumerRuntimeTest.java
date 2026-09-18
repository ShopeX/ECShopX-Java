package cn.shopex.ecshopx.thirdparty.dispatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import cn.shopex.ecshopx.thirdparty.service.saaserp.SaasErpStoreTradeRefundAddPort;
import cn.shopex.ecshopx.thirdparty.service.saaserp.TradeRefundSendSaasErpBusService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class TradeRefundSendSaasErpDispatchListenerDispatchConsumerRuntimeTest {

	private static final String LISTENER_NAME = "listener:thirdparty.listeners.TradeRefundSendSaasErp";

	@Mock
	private TradeRefundSendSaasErpBusService tradeRefundSendSaasErpBusService;

	private DispatchConsumerRuntime runtimeWithMockBus;

	@BeforeEach
	void setUp() {
		reset(tradeRefundSendSaasErpBusService);
		TradeRefundSendSaasErpDispatchListener listener =
				new TradeRefundSendSaasErpDispatchListener(tradeRefundSendSaasErpBusService);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_REFUND,
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
	void consume_adminFullCancelTradeRefundPayload_invokesHandleTradeRefundEntitiesOnce() {
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("cancel_id", "55");
		payload.put("order_id", "200");
		payload.put("company_id", "1");
		payload.put("shop_id", 1L);
		payload.put("user_id", "100");
		payload.put("distributor_id", "0");
		payload.put("order_type", "normal");
		payload.put("total_fee", "10000");
		payload.put("progress", 0);
		payload.put("cancel_from", "shop");
		payload.put("cancel_reason", "协商一致");
		payload.put("refund_status", "READY");
		payload.put("refund_bn", 9_001_002_003L);
		payload.put("action", "cancel_order");

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						SystemLinkDispatchEventNames.EVENT_TRADE_REFUND,
						payload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T14:00:00Z"),
						"trace-admin-full-cancel-trade-refund",
						LISTENER_NAME);

		runtimeWithMockBus.consume(msg, 1);

		ArgumentCaptor<Map<String, Object>> captor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		verify(tradeRefundSendSaasErpBusService, times(1)).handleTradeRefundEntities(captor.capture());
		assertThat(captor.getValue().get("refund_bn")).isEqualTo(9_001_002_003L);
		assertThat(captor.getValue().get("action")).isEqualTo("cancel_order");
	}

	@Test
	void consume_partialAftersalesRefundPayload_invokesHandleTradeRefundEntitiesOnce() {
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("refund_bn", 8_888_888L);
		payload.put("aftersales_bn", 51_020L);
		payload.put("order_id", 5103L);
		payload.put("trade_id", "tid-x");
		payload.put("company_id", 9103L);
		payload.put("supplier_id", 0L);
		payload.put("user_id", 201L);
		payload.put("shop_id", 3L);
		payload.put("distributor_id", 0L);
		payload.put("refund_type", 0);
		payload.put("refund_channel", "original");
		payload.put("refund_status", "READY");
		payload.put("refund_fee", 500);
		payload.put("refund_point", 0);
		payload.put("return_freight", 1);
		payload.put("freight", 0);
		payload.put("freight_type", "cash");
		payload.put("pay_type", "wxpay");
		payload.put("currency", "CNY");
		payload.put("cur_fee_type", "CNY");
		payload.put("cur_fee_rate", 1.0);
		payload.put("cur_fee_symbol", "￥");
		payload.put("cur_pay_fee", "500");
		payload.put("merchant_id", 0L);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						SystemLinkDispatchEventNames.EVENT_TRADE_REFUND,
						payload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T14:30:00Z"),
						"trace-partial-aftersales-trade-refund",
						LISTENER_NAME);

		runtimeWithMockBus.consume(msg, 1);

		ArgumentCaptor<Map<String, Object>> captor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		verify(tradeRefundSendSaasErpBusService, times(1)).handleTradeRefundEntities(captor.capture());
		Map<String, Object> passed = captor.getValue();
		assertThat(passed.get("aftersales_bn")).isEqualTo(51_020L);
		assertThat(passed.get("refund_bn")).isEqualTo(8_888_888L);
		assertThat(passed.get("company_id")).isEqualTo(9103L);
		assertThat(passed.get("order_id")).isEqualTo(5103L);
		assertThat(passed.get("user_id")).isEqualTo(201L);
		assertThat(passed.get("refund_status")).isEqualTo("READY");
	}

	@Test
	@DisplayName(
			"Consume trade_refund message shaped like quantity-based apply only-refund post-commit refund row (merged head, fan-out to SaaS ERP listener)")
	void consume_shopApplyByNumRefundRowShapedPayload_invokesHandleTradeRefundEntitiesWithRefundChannelAndRefundType() {
		long refundBn = 7_654_321L;
		long aftersalesBn = 88_001L;
		long orderId = 9_001L;
		long companyId = 42_001L;
		String tradeId = "trade-shop-apply-row";
		String refundChannel = "original";
		int refundType = 0;
		int refundFee = 1_200;
		String refundStatus = "READY";

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("order_id", orderId);
		payload.put("aftersales_bn", aftersalesBn);
		payload.put("trade_id", tradeId);
		payload.put("shop_id", 5L);
		payload.put("distributor_id", 0L);
		payload.put("user_id", 3_001L);
		payload.put("refund_type", refundType);
		payload.put("refund_channel", refundChannel);
		payload.put("refund_fee", refundFee);
		payload.put("refund_point", 0);
		payload.put("refund_status", refundStatus);
		payload.put("pay_type", "wxpay");
		payload.put("currency", "CNY");
		payload.put("cur_fee_type", "CNY");
		payload.put("cur_fee_rate", 1.0);
		payload.put("cur_fee_symbol", "￥");
		payload.put("cur_pay_fee", "1200");
		payload.put("supplier_id", 0L);
		payload.put("merchant_id", 0L);
		payload.put("refund_bn", refundBn);
		payload.put("return_freight", 0);
		payload.put("freight", 0);
		payload.put("freight_type", "cash");
		payload.put("aftersales_type", "ONLY_REFUND");

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						SystemLinkDispatchEventNames.EVENT_TRADE_REFUND,
						payload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T15:00:00Z"),
						"trace-shop-apply-refund-row",
						LISTENER_NAME);

		runtimeWithMockBus.consume(msg, 1);

		ArgumentCaptor<Map<String, Object>> captor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		verify(tradeRefundSendSaasErpBusService, times(1)).handleTradeRefundEntities(captor.capture());
		Map<String, Object> passed = captor.getValue();

		assertThat(passed.get("refund_bn")).isEqualTo(refundBn);
		assertThat(passed.get("refund_status")).isEqualTo(refundStatus);
		assertThat(passed.get("refund_type")).isEqualTo(refundType);
		assertThat(passed.get("refund_channel")).isEqualTo(refundChannel);
		assertThat(passed.get("refund_fee")).isEqualTo(refundFee);
		assertThat(passed.get("trade_id")).isEqualTo(tradeId);
		assertThat(passed.get("aftersales_bn")).isEqualTo(aftersalesBn);
		assertThat(passed.get("order_id")).isEqualTo(orderId);
		assertThat(passed.get("company_id")).isEqualTo(companyId);
		assertThat(passed.get("aftersales_type")).isEqualTo("ONLY_REFUND");
		assertThat(passed.get("refund_channel"))
				.as("refund_channel matches shopApplyByNumHandle derivation (original or offline)")
				.isIn("original", "offline");
	}

	/**
	 * Admin {@code POST /api/v1/aftersales/apply} ONLY_REFUND: post-commit publish merges aftersales head
	 * ({@code aftersales_type}, etc.) into the refund-row map for {@code EVENT_TRADE_REFUND}.
	 */
	@Test
	void consume_adminAftersalesApplyMergedRefundAndHeadPayload_invokesHandleTradeRefundEntitiesWithAftersalesType() {
		long refundBn = 88_001L;
		long aftersalesBn = 20_260_510_000_000_1L;
		long orderId = 5_001L;
		long companyId = 9_001L;

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("user_id", 3_001L);
		payload.put("aftersales_bn", aftersalesBn);
		payload.put("order_id", orderId);
		payload.put("trade_id", "TR-ADMIN-PROBE");
		payload.put("shop_id", 11L);
		payload.put("distributor_id", 22L);
		payload.put("refund_type", 0);
		payload.put("refund_channel", "original");
		payload.put("refund_fee", 100);
		payload.put("refund_point", 0);
		payload.put("return_freight", 0);
		payload.put("pay_type", "online");
		payload.put("currency", "CNY");
		payload.put("cur_fee_type", "CNY");
		payload.put("cur_fee_rate", 1.0);
		payload.put("cur_fee_symbol", "¥");
		payload.put("cur_pay_fee", "100");
		payload.put("return_point", 0);
		payload.put("merchant_id", 0L);
		payload.put("refund_status", "READY");
		payload.put("supplier_id", 1L);
		payload.put("freight_type", "cash");
		payload.put("freight", 0);
		payload.put("refund_bn", refundBn);
		payload.put("aftersales_type", "ONLY_REFUND");

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						SystemLinkDispatchEventNames.EVENT_TRADE_REFUND,
						payload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T15:30:00Z"),
						"trace-admin-aftersales-apply-trade-refund-head",
						LISTENER_NAME);

		runtimeWithMockBus.consume(msg, 1);

		ArgumentCaptor<Map<String, Object>> captor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		verify(tradeRefundSendSaasErpBusService, times(1)).handleTradeRefundEntities(captor.capture());
		Map<String, Object> passed = captor.getValue();
		assertThat(passed.get("aftersales_type")).isEqualTo("ONLY_REFUND");
		assertThat(passed.get("refund_bn")).isEqualTo(refundBn);
		assertThat(passed.get("aftersales_bn")).isEqualTo(aftersalesBn);
		assertThat(passed.get("company_id")).isEqualTo(companyId);
		assertThat(passed.get("order_id")).isEqualTo(orderId);
	}

	/**
	 * Wxapp quantity apply, ONLY_REFUND, post-merge refund map: combines partial-aftersales-style
	 * freight keys ({@code return_freight}, {@code freight}) with head-merged {@code aftersales_type}
	 * (same bag shape as admin-apply merge) for the ThirdParty {@code TradeRefundSendSaasErp} fan-out
	 * child message.
	 */
	@Test
	@DisplayName(
			"ThirdParty TradeRefundSendSaasErp fan-out: wxapp ONLY_REFUND merged payload carries partial-freight row plus aftersales_type for consume")
	void consume_wxappOnlyRefund_mergedPartialFreightRowAndAftersalesHead_deliversPayloadToBusService() {
		long refundBn = 9_012_345_678L;
		long aftersalesBn = 60_060L;
		long orderId = 7_007L;
		long companyId = 8_008L;
		int refundFee = 2_400;

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("order_id", orderId);
		payload.put("aftersales_bn", aftersalesBn);
		payload.put("trade_id", "trade-wxapp-partial-freight");
		payload.put("shop_id", 4L);
		payload.put("distributor_id", 0L);
		payload.put("user_id", 501L);
		payload.put("refund_type", 0);
		payload.put("refund_channel", "original");
		payload.put("refund_fee", refundFee);
		payload.put("refund_point", 5);
		payload.put("refund_status", "READY");
		payload.put("pay_type", "wxpay");
		payload.put("currency", "CNY");
		payload.put("cur_fee_type", "CNY");
		payload.put("cur_fee_rate", 1.0);
		payload.put("cur_fee_symbol", "￥");
		payload.put("cur_pay_fee", "2400");
		payload.put("supplier_id", 0L);
		payload.put("merchant_id", 0L);
		payload.put("refund_bn", refundBn);
		payload.put("return_point", 0);
		payload.put("return_freight", 1);
		payload.put("freight", 80);
		payload.put("freight_type", "cash");
		payload.put("aftersales_type", "ONLY_REFUND");

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						SystemLinkDispatchEventNames.EVENT_TRADE_REFUND,
						payload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T16:00:00Z"),
						"trace-wxapp-only-refund-partial-freight-merged-head",
						LISTENER_NAME);

		assertThat(msg.messageName()).isEqualTo(SystemLinkDispatchEventNames.EVENT_TRADE_REFUND);
		assertThat(msg.queue()).isEqualTo("default");
		assertThat(msg.listenerName()).isEqualTo(LISTENER_NAME);

		runtimeWithMockBus.consume(msg, 1);

		ArgumentCaptor<Map<String, Object>> captor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		verify(tradeRefundSendSaasErpBusService, times(1)).handleTradeRefundEntities(captor.capture());
		Map<String, Object> passed = captor.getValue();

		assertThat(passed.get("aftersales_type")).isEqualTo("ONLY_REFUND");
		assertThat(passed.get("return_freight")).isEqualTo(1);
		assertThat(passed.get("freight")).isEqualTo(80);
		assertThat(passed.get("freight_type")).isEqualTo("cash");
		assertThat(passed.get("return_point")).isEqualTo(0);
		assertThat(passed.get("refund_bn")).isEqualTo(refundBn);
		assertThat(passed.get("aftersales_bn")).isEqualTo(aftersalesBn);
		assertThat(passed.get("refund_fee")).isEqualTo(refundFee);
		assertThat(passed.get("refund_point")).isEqualTo(5);
		assertThat(passed.get("refund_channel")).isEqualTo("original");
	}

	@Test
	void consume_whenPayloadMinimal_invokesBusServiceOnce() {
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 10L);
		payload.put("order_id", 99L);
		payload.put("refund_bn", 1001L);
		payload.put("refund_status", "READY");

		Instant occurredAt = Instant.parse("2026-05-10T12:00:00Z");
		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						SystemLinkDispatchEventNames.EVENT_TRADE_REFUND,
						payload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						occurredAt,
						"trace-trade-refund-saas-erp",
						LISTENER_NAME);

		runtimeWithMockBus.consume(msg, 1);

		verify(tradeRefundSendSaasErpBusService, times(1)).handleTradeRefundEntities(payload);
	}

	@Test
	void consume_whenCompanyIdMissing_skipsRefundAddPort() {
		SaasErpStoreTradeRefundAddPort port = mock(SaasErpStoreTradeRefundAddPort.class);
		TradeRefundSendSaasErpBusService realBus = new TradeRefundSendSaasErpBusService(port);
		TradeRefundSendSaasErpDispatchListener listener = new TradeRefundSendSaasErpDispatchListener(realBus);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_REFUND,
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
						SystemLinkDispatchEventNames.EVENT_TRADE_REFUND,
						payload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T12:00:00Z"),
						"trace-missing-company-refund",
						LISTENER_NAME);

		runtime.consume(msg, 1);

		verify(port, never()).callStoreTradeRefundAdd(any());
	}

	@Test
	void consume_whenRefundBnMissing_skipsRefundAddPort() {
		SaasErpStoreTradeRefundAddPort port = mock(SaasErpStoreTradeRefundAddPort.class);
		TradeRefundSendSaasErpBusService realBus = new TradeRefundSendSaasErpBusService(port);
		TradeRefundSendSaasErpDispatchListener listener = new TradeRefundSendSaasErpDispatchListener(realBus);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_REFUND,
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
						SystemLinkDispatchEventNames.EVENT_TRADE_REFUND,
						payload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T12:30:00Z"),
						"trace-missing-refund-bn",
						LISTENER_NAME);

		runtime.consume(msg, 1);

		verify(port, never()).callStoreTradeRefundAdd(any());
	}

	@Test
	void consume_whenOrderIdNonPositive_skipsRefundAddPort() {
		SaasErpStoreTradeRefundAddPort port = mock(SaasErpStoreTradeRefundAddPort.class);
		TradeRefundSendSaasErpBusService realBus = new TradeRefundSendSaasErpBusService(port);
		TradeRefundSendSaasErpDispatchListener listener = new TradeRefundSendSaasErpDispatchListener(realBus);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_REFUND,
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
		payload.put("order_id", 0L);
		payload.put("refund_bn", 9L);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						SystemLinkDispatchEventNames.EVENT_TRADE_REFUND,
						payload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T13:00:00Z"),
						"trace-missing-order-id-refund",
						LISTENER_NAME);

		runtime.consume(msg, 1);

		verify(port, never()).callStoreTradeRefundAdd(any());
	}
}
