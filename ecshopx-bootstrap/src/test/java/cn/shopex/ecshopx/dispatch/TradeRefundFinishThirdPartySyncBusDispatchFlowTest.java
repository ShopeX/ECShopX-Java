package cn.shopex.ecshopx.dispatch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.ThirdPartyDispatchEventNames;
import cn.shopex.ecshopx.thirdparty.dispatch.ThirdPartyTradeRefundFinishDmCrmDispatchListener;
import cn.shopex.ecshopx.thirdparty.dispatch.ThirdPartyTradeRefundPushMarketingCenterDispatchListener;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.TradeRefundFinishDmCrmProcessor;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.TradeRefundPushMarketingCenterProcessor;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

@DisplayName("TradeRefundFinishEvent: SYNC Bus dispatch (third-party) mirrors PHP listener order")
class TradeRefundFinishThirdPartySyncBusDispatchFlowTest {

	@Test
	void publishEvent_sync_invokesMarketingCenterThenDmCrmInPhpRegistrationOrder() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		TradeRefundPushMarketingCenterProcessor processor = mock(TradeRefundPushMarketingCenterProcessor.class);
		ThirdPartyTradeRefundPushMarketingCenterDispatchListener marketingListener =
				spy(new ThirdPartyTradeRefundPushMarketingCenterDispatchListener(processor));
		TradeRefundFinishDmCrmProcessor dmCrmProcessor = mock(TradeRefundFinishDmCrmProcessor.class);
		ThirdPartyTradeRefundFinishDmCrmDispatchListener dmCrm =
				spy(new ThirdPartyTradeRefundFinishDmCrmDispatchListener(dmCrmProcessor));

		registry.registerEventListener(
				ThirdPartyDispatchEventNames.EVENT_TRADE_REFUND_FINISH,
				"listener:thirdparty.trade_refund_finish_push_marketing_center",
				ListenerDispatchOptions.syncDefaults(),
				marketingListener);
		registry.registerEventListener(
				ThirdPartyDispatchEventNames.EVENT_TRADE_REFUND_FINISH,
				"listener:thirdparty.trade_refund_finish_dm_crm",
				ListenerDispatchOptions.syncDefaults(),
				dmCrm);

		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), java.util.Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 42L);
		payload.put("order_id", 9001L);
		payload.put("aftersales_bn", 2026050712345678L);

		facade.publishEvent(
				ThirdPartyDispatchEventNames.EVENT_TRADE_REFUND_FINISH,
				payload,
				DispatchOptions.eventDefaults());

		InOrder inOrder = inOrder(marketingListener, dmCrm);
		inOrder.verify(marketingListener).onEvent(any());
		inOrder.verify(dmCrm).onEvent(any());
	}

	@Test
	void publishEvent_sync_payloadReachesMarketingProcessorWithExpectedKeys() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		TradeRefundPushMarketingCenterProcessor processor = mock(TradeRefundPushMarketingCenterProcessor.class);
		ThirdPartyTradeRefundPushMarketingCenterDispatchListener marketingListener =
				new ThirdPartyTradeRefundPushMarketingCenterDispatchListener(processor);
		TradeRefundFinishDmCrmProcessor dmCrmProcessor = mock(TradeRefundFinishDmCrmProcessor.class);
		ThirdPartyTradeRefundFinishDmCrmDispatchListener dmCrm =
				new ThirdPartyTradeRefundFinishDmCrmDispatchListener(dmCrmProcessor);

		registry.registerEventListener(
				ThirdPartyDispatchEventNames.EVENT_TRADE_REFUND_FINISH,
				"listener:thirdparty.trade_refund_finish_push_marketing_center",
				ListenerDispatchOptions.syncDefaults(),
				marketingListener);
		registry.registerEventListener(
				ThirdPartyDispatchEventNames.EVENT_TRADE_REFUND_FINISH,
				"listener:thirdparty.trade_refund_finish_dm_crm",
				ListenerDispatchOptions.syncDefaults(),
				dmCrm);

		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), java.util.Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);
		payload.put("order_id", 200L);
		payload.put("refund_bn", 2202505071234567890L);
		payload.put("trade_id", "TR9k");
		payload.put("aftersales_bn", 2026050712345678L);
		payload.put("shop_id", 11L);
		payload.put("distributor_id", 0L);
		payload.put("refund_fee", 199);
		payload.put("refunded_fee", 199);
		payload.put("refund_success_time", "2026-05-11 12:00:00");

		facade.publishEvent(
				ThirdPartyDispatchEventNames.EVENT_TRADE_REFUND_FINISH,
				payload,
				DispatchOptions.eventDefaults());

		verify(processor)
				.handle(
						argThat(
								map ->
										Long.valueOf(1L).equals(map.get("company_id"))
												&& Long.valueOf(200L).equals(map.get("order_id"))
												&& Long.valueOf(2026050712345678L)
														.equals(map.get("aftersales_bn"))
												&& map.containsKey("refund_bn")
												&& map.containsKey("trade_id")
												&& map.containsKey("refund_fee")
												&& map.containsKey("refunded_fee")
												&& map.containsKey("refund_success_time")));
	}

	@Test
	void publishEvent_sync_payloadForwardedToDmCrmProcessorAfterMarketing() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		TradeRefundPushMarketingCenterProcessor marketingProcessor = mock(TradeRefundPushMarketingCenterProcessor.class);
		ThirdPartyTradeRefundPushMarketingCenterDispatchListener marketingListener =
				new ThirdPartyTradeRefundPushMarketingCenterDispatchListener(marketingProcessor);
		TradeRefundFinishDmCrmProcessor dmCrmProcessor = mock(TradeRefundFinishDmCrmProcessor.class);
		ThirdPartyTradeRefundFinishDmCrmDispatchListener dmCrm =
				new ThirdPartyTradeRefundFinishDmCrmDispatchListener(dmCrmProcessor);

		registry.registerEventListener(
				ThirdPartyDispatchEventNames.EVENT_TRADE_REFUND_FINISH,
				"listener:thirdparty.trade_refund_finish_push_marketing_center",
				ListenerDispatchOptions.syncDefaults(),
				marketingListener);
		registry.registerEventListener(
				ThirdPartyDispatchEventNames.EVENT_TRADE_REFUND_FINISH,
				"listener:thirdparty.trade_refund_finish_dm_crm",
				ListenerDispatchOptions.syncDefaults(),
				dmCrm);

		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), java.util.Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 9L);
		payload.put("order_id", 800L);
		payload.put("aftersales_bn", 0L);

		facade.publishEvent(
				ThirdPartyDispatchEventNames.EVENT_TRADE_REFUND_FINISH,
				payload,
				DispatchOptions.eventDefaults());

		InOrder inOrder = inOrder(marketingProcessor, dmCrmProcessor);
		inOrder.verify(marketingProcessor).handle(argThat(m -> Long.valueOf(9L).equals(m.get("company_id"))));
		inOrder.verify(dmCrmProcessor).handle(argThat(m -> Long.valueOf(800L).equals(m.get("order_id"))));
	}
}
