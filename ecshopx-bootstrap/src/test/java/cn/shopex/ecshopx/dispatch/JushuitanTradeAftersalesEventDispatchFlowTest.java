package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.jushuitan.JushuitanTradeAftersalesBusPayloadBuilder;
import cn.shopex.ecshopx.common.dispatch.SystemLinkDispatchEventNames;
import cn.shopex.ecshopx.systemlink.dispatch.SystemLinkJushuitanTradeAftersalesDispatchListener;
import cn.shopex.ecshopx.systemlink.service.jushuitan.JushuitanTradeAftersalesNotificationProcessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class JushuitanTradeAftersalesEventDispatchFlowTest {

	@Test
	void publishEvent_sync_fanOutRegistersListener_andInvokesRunnable() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		AtomicInteger calls = new AtomicInteger();
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES,
				"listener:systemlink.trade_aftersales_send_jushuitan",
				ListenerDispatchOptions.asyncDefaults(),
				payload -> calls.incrementAndGet());

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 11L);
		payload.put("order_id", 9001L);
		payload.put("aftersales_bn", 202501011234567L);

		facade.publishEvent(
				SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES,
				payload,
				DispatchOptions.eventDefaults());

		assertEquals(0, captured.size());
		assertEquals(1, calls.get());
		DispatchMessage last = facade.lastPublishedMessage();
		assertNotNull(last);
		assertEquals(DispatchMode.SYNC, last.dispatchMode());
		assertEquals(DispatchMessageType.EVENT, last.messageType());
		assertEquals(SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES, last.messageName());
	}

	@Test
	void publishEvent_async_fanOut_enqueuesDefaultQueue_andConsumerInvokesListener() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		AtomicInteger calls = new AtomicInteger();
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES,
				"listener:systemlink.trade_aftersales_send_jushuitan",
				ListenerDispatchOptions.asyncDefaults(),
				payload -> calls.incrementAndGet());

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 11L);
		payload.put("order_id", 9001L);
		payload.put("aftersales_bn", 2026050611111111L);
		payload.put("distributor_id", 5L);

		facade.publishEvent(
				SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.EVENT, msg.messageType());
		assertEquals(SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES, msg.messageName());
		assertEquals("listener:systemlink.trade_aftersales_send_jushuitan", msg.listenerName());
		assertEquals("default", msg.queue());
		assertEquals(11L, msg.payload().get("company_id"));
		assertEquals(9001L, msg.payload().get("order_id"));
		assertEquals(2026050611111111L, ((Number) msg.payload().get("aftersales_bn")).longValue());
		assertEquals(5L, msg.payload().get("distributor_id"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);
		assertEquals(1, calls.get());
	}

	@Test
	void publishEvent_async_enqueue_dispatchConsumerRuntime_consume_invokesSystemLinkJushuitanTradeAftersalesDispatchListener_andProcessor() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		JushuitanTradeAftersalesNotificationProcessor mockedProcessor =
				mock(JushuitanTradeAftersalesNotificationProcessor.class);
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES,
				"listener:systemlink.trade_aftersales_send_jushuitan",
				ListenerDispatchOptions.asyncDefaults(),
				new SystemLinkJushuitanTradeAftersalesDispatchListener(mockedProcessor));

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 11L);
		payload.put("order_id", 9001L);
		payload.put("aftersales_bn", 2026050612121212L);
		payload.put("distributor_id", 5L);

		facade.publishEvent(
				SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.EVENT, msg.messageType());
		assertEquals(SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES, msg.messageName());
		assertEquals("listener:systemlink.trade_aftersales_send_jushuitan", msg.listenerName());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(mockedProcessor, times(1)).handle(payloadCaptor.capture());
		assertEquals(11L, payloadCaptor.getValue().get("company_id"));
	}

	@Test
	void publishEvent_async_adminReviewLikePayload_dispatchConsumerRuntime_consume_invokesSystemLinkJushuitanTradeAftersalesDispatchListener_andProcessor() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		JushuitanTradeAftersalesNotificationProcessor mockedProcessor =
				mock(JushuitanTradeAftersalesNotificationProcessor.class);
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES,
				"listener:systemlink.trade_aftersales_send_jushuitan",
				ListenerDispatchOptions.asyncDefaults(),
				new SystemLinkJushuitanTradeAftersalesDispatchListener(mockedProcessor));

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		long aftersalesBn = 2026050619191919L;
		long companyId = 11L;
		long orderId = 9001L;
		long distributorId = 5L;
		payload.put("aftersales_bn", aftersalesBn);
		payload.put("company_id", companyId);
		payload.put("order_id", orderId);
		payload.put("distributor_id", distributorId);
		payload.put("shop_id", 1L);
		payload.put("supplier_id", 0);
		payload.put("user_id", 20L);
		payload.put("aftersales_type", "ONLY_REFUND");
		payload.put("aftersales_status", 1);
		payload.put("progress", 9);
		payload.put("reason", "");
		payload.put("description", "");
		payload.put("evidence_pic", "");
		payload.put("salesman_id", 0L);
		payload.put("contact", "");
		payload.put("mobile", "");
		payload.put("merchant_id", 0L);
		payload.put("self_delivery_operator_id", 0L);
		payload.put("is_partial_cancel", false);
		payload.put("return_type", "logistics");
		payload.put("freight", 0);
		payload.put("freight_type", "cash");
		payload.put("return_distributor_id", 0L);
		payload.put("aftersales_address", "");

		facade.publishEvent(
				SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.EVENT, msg.messageType());
		assertEquals(SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES, msg.messageName());
		assertEquals("listener:systemlink.trade_aftersales_send_jushuitan", msg.listenerName());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(mockedProcessor, times(1)).handle(payloadCaptor.capture());
		Map<String, Object> passed = payloadCaptor.getValue();
		assertEquals(companyId, ((Number) passed.get("company_id")).longValue());
		assertEquals(orderId, ((Number) passed.get("order_id")).longValue());
		assertEquals(aftersalesBn, ((Number) passed.get("aftersales_bn")).longValue());
		assertEquals(distributorId, ((Number) passed.get("distributor_id")).longValue());
	}

	@Test
	@DisplayName(
			"Rejected review Bus payload async consume covers shared Shop reject snapshot semantics (same listener chain as approve-like paths)")
	void publishEvent_async_adminReviewRejectedPayload_dispatchConsumerRuntime_consume_invokesSystemLinkJushuitanTradeAftersalesDispatchListener_andProcessor() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		JushuitanTradeAftersalesNotificationProcessor mockedProcessor =
				mock(JushuitanTradeAftersalesNotificationProcessor.class);
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES,
				"listener:systemlink.trade_aftersales_send_jushuitan",
				ListenerDispatchOptions.asyncDefaults(),
				new SystemLinkJushuitanTradeAftersalesDispatchListener(mockedProcessor));

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		long aftersalesBn = 2026050620202020L;
		long companyId = 11L;
		long orderId = 9001L;
		long distributorId = 5L;
		payload.put("aftersales_bn", aftersalesBn);
		payload.put("company_id", companyId);
		payload.put("order_id", orderId);
		payload.put("distributor_id", distributorId);
		payload.put("shop_id", 1L);
		payload.put("supplier_id", 0);
		payload.put("user_id", 20L);
		payload.put("aftersales_type", "ONLY_REFUND");
		payload.put("aftersales_status", 3);
		payload.put("progress", 3);
		payload.put("reason", "");
		payload.put("description", "");
		payload.put("evidence_pic", "");
		payload.put("salesman_id", 0L);
		payload.put("contact", "");
		payload.put("mobile", "");
		payload.put("merchant_id", 0L);
		payload.put("self_delivery_operator_id", 0L);
		payload.put("is_partial_cancel", false);
		payload.put("return_type", "logistics");
		payload.put("freight", 0);
		payload.put("freight_type", "cash");
		payload.put("return_distributor_id", 0L);
		payload.put("aftersales_address", "");

		facade.publishEvent(
				SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.EVENT, msg.messageType());
		assertEquals(SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES, msg.messageName());
		assertEquals("listener:systemlink.trade_aftersales_send_jushuitan", msg.listenerName());
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
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(mockedProcessor, times(1)).handle(payloadCaptor.capture());
		Map<String, Object> passed = payloadCaptor.getValue();
		assertEquals(companyId, ((Number) passed.get("company_id")).longValue());
		assertEquals(orderId, ((Number) passed.get("order_id")).longValue());
		assertEquals(aftersalesBn, ((Number) passed.get("aftersales_bn")).longValue());
		assertEquals(distributorId, ((Number) passed.get("distributor_id")).longValue());
		assertEquals(3, ((Number) passed.get("aftersales_status")).intValue());
		assertEquals(3, ((Number) passed.get("progress")).intValue());
	}

	@Test
	@DisplayName(
			"Refund check reject (Shop confirmRefund) Bus payload async consume invokes SystemLink listener and processor — same listener/processor chain as Admin refund-check path")
	void publishEvent_async_refundCheckRejectedPayload_dispatchConsumerRuntime_consume_invokesSystemLinkJushuitanTradeAftersalesDispatchListener_andProcessor() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		JushuitanTradeAftersalesNotificationProcessor mockedProcessor =
				mock(JushuitanTradeAftersalesNotificationProcessor.class);
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES,
				"listener:systemlink.trade_aftersales_send_jushuitan",
				ListenerDispatchOptions.asyncDefaults(),
				new SystemLinkJushuitanTradeAftersalesDispatchListener(mockedProcessor));

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long aftersalesBn = 2026050621212121L;
		long companyId = 11L;
		long orderId = 9001L;
		long distributorId = 5L;
		Aftersales rejectSnapshot = new Aftersales();
		rejectSnapshot.setAftersalesBn(aftersalesBn);
		rejectSnapshot.setCompanyId(companyId);
		rejectSnapshot.setOrderId(orderId);
		rejectSnapshot.setDistributorId(distributorId);
		rejectSnapshot.setShopId(1L);
		rejectSnapshot.setSupplierId(0);
		rejectSnapshot.setUserId(20L);
		rejectSnapshot.setAftersalesType("ONLY_REFUND");
		rejectSnapshot.setAftersalesStatus(3);
		rejectSnapshot.setProgress(3);
		rejectSnapshot.setReason("");
		rejectSnapshot.setDescription("");
		rejectSnapshot.setEvidencePic("");
		rejectSnapshot.setSalesmanId(0L);
		rejectSnapshot.setContact("");
		rejectSnapshot.setMobile("");
		rejectSnapshot.setMerchantId(0L);
		rejectSnapshot.setSelfDeliveryOperatorId(0L);
		rejectSnapshot.setIsPartialCancel(false);
		rejectSnapshot.setReturnType("logistics");
		rejectSnapshot.setFreight(0);
		rejectSnapshot.setFreightType("cash");
		rejectSnapshot.setReturnDistributorId(0L);
		rejectSnapshot.setAftersalesAddress("");

		Map<String, Object> payload =
				new JushuitanTradeAftersalesBusPayloadBuilder().build(rejectSnapshot, new LinkedHashMap<>());

		facade.publishEvent(
				SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.EVENT, msg.messageType());
		assertEquals(SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES, msg.messageName());
		assertEquals("listener:systemlink.trade_aftersales_send_jushuitan", msg.listenerName());
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
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(mockedProcessor, times(1)).handle(payloadCaptor.capture());
		Map<String, Object> passed = payloadCaptor.getValue();
		assertEquals(companyId, ((Number) passed.get("company_id")).longValue());
		assertEquals(orderId, ((Number) passed.get("order_id")).longValue());
		assertEquals(aftersalesBn, ((Number) passed.get("aftersales_bn")).longValue());
		assertEquals(distributorId, ((Number) passed.get("distributor_id")).longValue());
		assertEquals(3, ((Number) passed.get("aftersales_status")).intValue());
		assertEquals(3, ((Number) passed.get("progress")).intValue());
	}

	@Test
	@DisplayName(
			"Shop/buyer refund-check success: Bus payload parity with Admin path — async consume invokes SystemLink listener and processor")
	void publishEvent_async_refundCheckSuccessPayload_dispatchConsumerRuntime_consume_invokesSystemLinkJushuitanTradeAftersalesDispatchListener_andProcessor() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		JushuitanTradeAftersalesNotificationProcessor mockedProcessor =
				mock(JushuitanTradeAftersalesNotificationProcessor.class);
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES,
				"listener:systemlink.trade_aftersales_send_jushuitan",
				ListenerDispatchOptions.asyncDefaults(),
				new SystemLinkJushuitanTradeAftersalesDispatchListener(mockedProcessor));

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long aftersalesBn = 2026050623232323L;
		long companyId = 11L;
		long orderId = 9001L;
		long distributorId = 5L;
		Aftersales successSnapshot = new Aftersales();
		successSnapshot.setAftersalesBn(aftersalesBn);
		successSnapshot.setCompanyId(companyId);
		successSnapshot.setOrderId(orderId);
		successSnapshot.setDistributorId(distributorId);
		successSnapshot.setShopId(1L);
		successSnapshot.setSupplierId(0);
		successSnapshot.setUserId(20L);
		successSnapshot.setAftersalesType("ONLY_REFUND");
		successSnapshot.setAftersalesStatus(2);
		successSnapshot.setProgress(4);
		successSnapshot.setReason("");
		successSnapshot.setDescription("");
		successSnapshot.setEvidencePic("");
		successSnapshot.setSalesmanId(0L);
		successSnapshot.setContact("");
		successSnapshot.setMobile("");
		successSnapshot.setMerchantId(0L);
		successSnapshot.setSelfDeliveryOperatorId(0L);
		successSnapshot.setIsPartialCancel(false);
		successSnapshot.setReturnType("logistics");
		successSnapshot.setFreight(0);
		successSnapshot.setFreightType("cash");
		successSnapshot.setReturnDistributorId(0L);
		successSnapshot.setAftersalesAddress("");

		Map<String, Object> payload =
				new JushuitanTradeAftersalesBusPayloadBuilder().build(successSnapshot, new LinkedHashMap<>());

		facade.publishEvent(
				SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.EVENT, msg.messageType());
		assertEquals(SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES, msg.messageName());
		assertEquals("listener:systemlink.trade_aftersales_send_jushuitan", msg.listenerName());
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
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(mockedProcessor, times(1)).handle(payloadCaptor.capture());
		Map<String, Object> passed = payloadCaptor.getValue();
		assertEquals(companyId, ((Number) passed.get("company_id")).longValue());
		assertEquals(orderId, ((Number) passed.get("order_id")).longValue());
		assertEquals(aftersalesBn, ((Number) passed.get("aftersales_bn")).longValue());
		assertEquals(distributorId, ((Number) passed.get("distributor_id")).longValue());
		assertEquals(2, ((Number) passed.get("aftersales_status")).intValue());
		assertEquals(4, ((Number) passed.get("progress")).intValue());
	}

	@Test
	void publishEvent_async_wxappSendbackPayload_progress2_dispatchConsumerRuntime_consume_invokesSystemLinkJushuitanTradeAftersalesDispatchListener_andProcessor() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		JushuitanTradeAftersalesNotificationProcessor mockedProcessor =
				mock(JushuitanTradeAftersalesNotificationProcessor.class);
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES,
				"listener:systemlink.trade_aftersales_send_jushuitan",
				ListenerDispatchOptions.asyncDefaults(),
				new SystemLinkJushuitanTradeAftersalesDispatchListener(mockedProcessor));

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long aftersalesBn = 2026050624242424L;
		long companyId = 11L;
		long orderId = 9001L;
		long distributorId = 5L;
		long userId = 20L;
		Aftersales wxappSendbackSnapshot = new Aftersales();
		wxappSendbackSnapshot.setAftersalesBn(aftersalesBn);
		wxappSendbackSnapshot.setCompanyId(companyId);
		wxappSendbackSnapshot.setOrderId(orderId);
		wxappSendbackSnapshot.setDistributorId(distributorId);
		wxappSendbackSnapshot.setShopId(1L);
		wxappSendbackSnapshot.setSupplierId(0);
		wxappSendbackSnapshot.setUserId(userId);
		wxappSendbackSnapshot.setAftersalesType("RETURN_GOODS");
		wxappSendbackSnapshot.setAftersalesStatus(1);
		wxappSendbackSnapshot.setProgress(2);
		wxappSendbackSnapshot.setReason("");
		wxappSendbackSnapshot.setDescription("");
		wxappSendbackSnapshot.setEvidencePic("");
		wxappSendbackSnapshot.setSalesmanId(0L);
		wxappSendbackSnapshot.setContact("");
		wxappSendbackSnapshot.setMobile("");
		wxappSendbackSnapshot.setMerchantId(0L);
		wxappSendbackSnapshot.setSelfDeliveryOperatorId(0L);
		wxappSendbackSnapshot.setIsPartialCancel(false);
		wxappSendbackSnapshot.setReturnType("logistics");
		wxappSendbackSnapshot.setFreight(0);
		wxappSendbackSnapshot.setFreightType("cash");
		wxappSendbackSnapshot.setReturnDistributorId(0L);
		wxappSendbackSnapshot.setAftersalesAddress("");

		Map<String, Object> payload =
				new JushuitanTradeAftersalesBusPayloadBuilder().build(wxappSendbackSnapshot, new LinkedHashMap<>());

		facade.publishEvent(
				SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.EVENT, msg.messageType());
		assertEquals(SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES, msg.messageName());
		assertEquals("listener:systemlink.trade_aftersales_send_jushuitan", msg.listenerName());
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
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(mockedProcessor, times(1)).handle(payloadCaptor.capture());
		Map<String, Object> passed = payloadCaptor.getValue();
		assertEquals(companyId, ((Number) passed.get("company_id")).longValue());
		assertEquals(orderId, ((Number) passed.get("order_id")).longValue());
		assertEquals(aftersalesBn, ((Number) passed.get("aftersales_bn")).longValue());
		assertEquals(distributorId, ((Number) passed.get("distributor_id")).longValue());
		assertEquals(2, ((Number) passed.get("progress")).intValue());
	}

	@Test
	void publishEvent_async_shopSendbackPayload_progress2_dispatchConsumerRuntime_consume_invokesSystemLinkJushuitanTradeAftersalesDispatchListener_andProcessor() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		JushuitanTradeAftersalesNotificationProcessor mockedProcessor =
				mock(JushuitanTradeAftersalesNotificationProcessor.class);
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES,
				"listener:systemlink.trade_aftersales_send_jushuitan",
				ListenerDispatchOptions.asyncDefaults(),
				new SystemLinkJushuitanTradeAftersalesDispatchListener(mockedProcessor));

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long aftersalesBn = 2026050616161616L;
		long companyId = 11L;
		long orderId = 9001L;
		long distributorId = 5L;
		long userId = 20L;
		Aftersales shopSendbackSnapshot = new Aftersales();
		shopSendbackSnapshot.setAftersalesBn(aftersalesBn);
		shopSendbackSnapshot.setCompanyId(companyId);
		shopSendbackSnapshot.setOrderId(orderId);
		shopSendbackSnapshot.setDistributorId(distributorId);
		shopSendbackSnapshot.setShopId(1L);
		shopSendbackSnapshot.setSupplierId(0);
		shopSendbackSnapshot.setUserId(userId);
		shopSendbackSnapshot.setAftersalesType("RETURN_GOODS");
		shopSendbackSnapshot.setAftersalesStatus(1);
		shopSendbackSnapshot.setProgress(2);
		shopSendbackSnapshot.setReason("");
		shopSendbackSnapshot.setDescription("");
		shopSendbackSnapshot.setEvidencePic("");
		shopSendbackSnapshot.setSalesmanId(0L);
		shopSendbackSnapshot.setContact("");
		shopSendbackSnapshot.setMobile("");
		shopSendbackSnapshot.setMerchantId(0L);
		shopSendbackSnapshot.setSelfDeliveryOperatorId(0L);
		shopSendbackSnapshot.setIsPartialCancel(false);
		shopSendbackSnapshot.setReturnType("logistics");
		shopSendbackSnapshot.setFreight(0);
		shopSendbackSnapshot.setFreightType("cash");
		shopSendbackSnapshot.setReturnDistributorId(0L);
		shopSendbackSnapshot.setAftersalesAddress("");

		Map<String, Object> payload =
				new JushuitanTradeAftersalesBusPayloadBuilder().build(shopSendbackSnapshot, new LinkedHashMap<>());

		facade.publishEvent(
				SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.EVENT, msg.messageType());
		assertEquals(SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES, msg.messageName());
		assertEquals("listener:systemlink.trade_aftersales_send_jushuitan", msg.listenerName());
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
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(mockedProcessor, times(1)).handle(payloadCaptor.capture());
		Map<String, Object> passed = payloadCaptor.getValue();
		assertEquals(companyId, ((Number) passed.get("company_id")).longValue());
		assertEquals(orderId, ((Number) passed.get("order_id")).longValue());
		assertEquals(aftersalesBn, ((Number) passed.get("aftersales_bn")).longValue());
		assertEquals(distributorId, ((Number) passed.get("distributor_id")).longValue());
		assertEquals(2, ((Number) passed.get("progress")).intValue());
	}

	@Test
	@DisplayName(
			"close-aftersales cancelled async REDIS consume; schedule batch auto-close shares same bus payload as wxapp close")
	void publishEvent_async_closeAftersalesCancelledPayload_dispatchConsumerRuntime_consume_invokesSystemLinkJushuitanTradeAftersalesDispatchListener_andProcessor() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		JushuitanTradeAftersalesNotificationProcessor mockedProcessor =
				mock(JushuitanTradeAftersalesNotificationProcessor.class);
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES,
				"listener:systemlink.trade_aftersales_send_jushuitan",
				ListenerDispatchOptions.asyncDefaults(),
				new SystemLinkJushuitanTradeAftersalesDispatchListener(mockedProcessor));

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long aftersalesBn = 2026050625252525L;
		long companyId = 11L;
		long orderId = 9001L;
		long distributorId = 5L;
		long userId = 20L;
		Aftersales cancelledSnapshot = new Aftersales();
		cancelledSnapshot.setAftersalesBn(aftersalesBn);
		cancelledSnapshot.setCompanyId(companyId);
		cancelledSnapshot.setOrderId(orderId);
		cancelledSnapshot.setDistributorId(distributorId);
		cancelledSnapshot.setShopId(1L);
		cancelledSnapshot.setSupplierId(0);
		cancelledSnapshot.setUserId(userId);
		cancelledSnapshot.setAftersalesType("ONLY_REFUND");
		cancelledSnapshot.setAftersalesStatus(4);
		cancelledSnapshot.setProgress(7);
		cancelledSnapshot.setReason("");
		cancelledSnapshot.setDescription("");
		cancelledSnapshot.setEvidencePic("");
		cancelledSnapshot.setSalesmanId(0L);
		cancelledSnapshot.setContact("");
		cancelledSnapshot.setMobile("");
		cancelledSnapshot.setMerchantId(0L);
		cancelledSnapshot.setSelfDeliveryOperatorId(0L);
		cancelledSnapshot.setIsPartialCancel(false);
		cancelledSnapshot.setReturnType("logistics");
		cancelledSnapshot.setFreight(0);
		cancelledSnapshot.setFreightType("cash");
		cancelledSnapshot.setReturnDistributorId(0L);
		cancelledSnapshot.setAftersalesAddress("");

		Map<String, Object> payload =
				new JushuitanTradeAftersalesBusPayloadBuilder().build(cancelledSnapshot, new LinkedHashMap<>());

		facade.publishEvent(
				SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.EVENT, msg.messageType());
		assertEquals(SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES, msg.messageName());
		assertEquals("listener:systemlink.trade_aftersales_send_jushuitan", msg.listenerName());
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
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(mockedProcessor, times(1)).handle(payloadCaptor.capture());
		Map<String, Object> passed = payloadCaptor.getValue();
		assertEquals(companyId, ((Number) passed.get("company_id")).longValue());
		assertEquals(orderId, ((Number) passed.get("order_id")).longValue());
		assertEquals(aftersalesBn, ((Number) passed.get("aftersales_bn")).longValue());
		assertEquals(distributorId, ((Number) passed.get("distributor_id")).longValue());
		assertEquals(4, ((Number) passed.get("aftersales_status")).intValue());
		assertEquals(7, ((Number) passed.get("progress")).intValue());
	}
}
