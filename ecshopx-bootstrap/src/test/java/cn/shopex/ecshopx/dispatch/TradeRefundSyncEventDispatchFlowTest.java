package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.common.dispatch.SystemLinkDispatchEventNames;
import cn.shopex.ecshopx.thirdparty.dispatch.ThirdPartyTradeRefundPushMarketingCenterDispatchListener;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.TradeRefundPushMarketingCenterProcessor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Trade refund SYNC event: listeners fire in registry order inside the same thread")
class TradeRefundSyncEventDispatchFlowTest {

	@Test
	void publishEvent_sync_invokesOmeListenerThenMarketingListenerInRegistrationOrder() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<String> order = new ArrayList<>();

		DispatchListener ome = payload -> order.add("OME");
		DispatchListener marketing = payload -> order.add("MKT");

		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_REFUND,
				"listener:systemlink.trade_refund_send_ome",
				ListenerDispatchOptions.asyncDefaults(),
				ome);
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_REFUND,
				"listener:thirdparty.trade_refund_push_marketing_center",
				ListenerDispatchOptions.asyncDefaults(),
				marketing);

		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 42L);
		payload.put("order_id", 9001L);
		payload.put("aftersales_bn", 2026050712345678L);

		facade.publishEvent(
				SystemLinkDispatchEventNames.EVENT_TRADE_REFUND, payload, DispatchOptions.eventDefaults());

		assertEquals(List.of("OME", "MKT"), order);
		Object company = payload.get("company_id");
		Object oid = payload.get("order_id");
		Object asn = payload.get("aftersales_bn");
		assertNotNull(company);
		assertNotNull(oid);
		assertNotNull(asn);
	}

	/**
	 * Exercises primary SYNC {@code DispatchOptions.eventDefaults()} (same shape as legacy {@code TradeRefundDispatchPublisherImpl}),
	 * not {@code TradeRefundAsyncFanOutDispatchPublisher}; admin cancel OME async parity is covered by {@code
	 * TradeRefundSendOmeEventDispatchFlowTest#asyncFanOutPublisher_adminFullCancelPayload_enqueuesOmeThenConsumeInvokesOmeProcessorOnce}.
	 */
	@Test
	void publishEvent_sync_adminFullCancelPayloadShape_invokesMarketingCenterProcessorHandle() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<String> omeOrder = new ArrayList<>();

		DispatchListener omeStub = payload -> omeOrder.add("OME");
		TradeRefundPushMarketingCenterProcessor marketingProcessor = mock(TradeRefundPushMarketingCenterProcessor.class);
		DispatchListener marketingListener =
				new ThirdPartyTradeRefundPushMarketingCenterDispatchListener(marketingProcessor);

		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_REFUND,
				"listener:systemlink.trade_refund_send_ome",
				ListenerDispatchOptions.asyncDefaults(),
				omeStub);
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_REFUND,
				"listener:thirdparty.trade_refund_push_marketing_center",
				ListenerDispatchOptions.asyncDefaults(),
				marketingListener);

		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", "1");
		payload.put("order_id", "200");
		payload.put("cancel_id", "55");
		payload.put("cancel_from", "shop");
		payload.put("action", "cancel_order");
		payload.put("user_id", "100");
		payload.put("shop_id", 1L);
		payload.put("supplier_id", 0L);

		facade.publishEvent(
				SystemLinkDispatchEventNames.EVENT_TRADE_REFUND, payload, DispatchOptions.eventDefaults());

		assertEquals(List.of("OME"), omeOrder);

		verify(marketingProcessor)
				.handle(
						argThat(
								map ->
										"cancel_order".equals(map.get("action"))
												&& "shop".equals(String.valueOf(map.get("cancel_from")))
												&& "200".equals(String.valueOf(map.get("order_id")))
												&& "1".equals(String.valueOf(map.get("company_id")))
												&& "55".equals(String.valueOf(map.get("cancel_id")))
												&& !map.containsKey("aftersales_bn")));
	}

	@Test
	void publishEvent_sync_wxappPendingFullCancelPayloadShape_invokesMarketingCenterProcessorHandle() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<String> omeOrder = new ArrayList<>();

		DispatchListener omeStub = payload -> omeOrder.add("OME");
		TradeRefundPushMarketingCenterProcessor marketingProcessor = mock(TradeRefundPushMarketingCenterProcessor.class);
		DispatchListener marketingListener =
				new ThirdPartyTradeRefundPushMarketingCenterDispatchListener(marketingProcessor);

		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_REFUND,
				"listener:systemlink.trade_refund_send_ome",
				ListenerDispatchOptions.asyncDefaults(),
				omeStub);
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_REFUND,
				"listener:thirdparty.trade_refund_push_marketing_center",
				ListenerDispatchOptions.asyncDefaults(),
				marketingListener);

		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", "1");
		payload.put("order_id", "200");
		payload.put("cancel_id", "55");
		payload.put("cancel_from", "buyer");
		payload.put("action", "cancel_order");
		payload.put("user_id", "100");
		payload.put("shop_id", 1L);
		payload.put("supplier_id", 0L);

		facade.publishEvent(
				SystemLinkDispatchEventNames.EVENT_TRADE_REFUND, payload, DispatchOptions.eventDefaults());

		assertEquals(List.of("OME"), omeOrder);

		verify(marketingProcessor)
				.handle(
						argThat(
								map ->
										"cancel_order".equals(map.get("action"))
												&& "buyer".equals(String.valueOf(map.get("cancel_from")))
												&& "200".equals(String.valueOf(map.get("order_id")))
												&& "1".equals(String.valueOf(map.get("company_id")))
												&& "55".equals(String.valueOf(map.get("cancel_id")))
												&& !map.containsKey("aftersales_bn")));
	}

	@Test
	void listenerNameDuplicates_failAtRegistration() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		DispatchListener stub = payload -> {};
		String name = "listener:duplicate";
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_REFUND,
				name,
				ListenerDispatchOptions.asyncDefaults(),
				stub);
		assertThrows(
				IllegalStateException.class,
				() ->
						registry.registerEventListener(
								SystemLinkDispatchEventNames.EVENT_TRADE_REFUND,
								name,
								ListenerDispatchOptions.asyncDefaults(),
								stub));
	}

	@Test
	void publishEvent_sync_payloadFromPartialCancelMapperShape_stillInvokesOmeThenMarketingInOrder() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<String> order = new ArrayList<>();

		DispatchListener ome = payload -> order.add("OME");
		DispatchListener marketing = payload -> order.add("MKT");

		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_REFUND,
				"listener:systemlink.trade_refund_send_ome",
				ListenerDispatchOptions.asyncDefaults(),
				ome);
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_REFUND,
				"listener:thirdparty.trade_refund_push_marketing_center",
				ListenerDispatchOptions.asyncDefaults(),
				marketing);

		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("refund_bn", 2202505071234567890L);
		payload.put("aftersales_bn", 2026050712345678L);
		payload.put("order_id", 9001L);
		payload.put("trade_id", "TR9k");
		payload.put("company_id", 42L);
		payload.put("supplier_id", 0L);
		payload.put("user_id", 3001L);
		payload.put("shop_id", 11L);
		payload.put("distributor_id", 0L);
		payload.put("refund_type", 0);
		payload.put("refund_channel", "original");
		payload.put("refund_status", "READY");
		payload.put("refund_fee", 199);
		payload.put("refund_point", 0);
		payload.put("return_freight", 0);
		payload.put("freight", 0);
		payload.put("freight_type", "cash");
		payload.put("pay_type", "wxpay");
		payload.put("currency", "CNY");
		payload.put("cur_fee_type", "CNY");
		payload.put("cur_fee_rate", 1.0);
		payload.put("cur_fee_symbol", "¥");
		payload.put("cur_pay_fee", "199");
		payload.put("merchant_id", 0L);

		facade.publishEvent(
				SystemLinkDispatchEventNames.EVENT_TRADE_REFUND, payload, DispatchOptions.eventDefaults());

		assertEquals(List.of("OME", "MKT"), order);
	}
}
