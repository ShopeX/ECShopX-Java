package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.common.dispatch.SystemLinkDispatchEventNames;
import cn.shopex.ecshopx.thirdparty.dispatch.ThirdPartyTradeAftersalesPushMarketingCenterDispatchListener;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.TradeRefundPushMarketingCenterProcessor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Trade aftersales SYNC event: Bus listener invokes marketing center processor")
class TradeAftersalesSyncEventDispatchFlowTest {

	private static final String LISTENER_OME = "listener:systemlink.trade_aftersales_send_ome";

	@Test
	void publishEvent_sync_tradeAftersalesPayload_invokesMarketingProcessorHandle() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		TradeRefundPushMarketingCenterProcessor marketingProcessor = mock(TradeRefundPushMarketingCenterProcessor.class);
		ThirdPartyTradeAftersalesPushMarketingCenterDispatchListener marketingListener =
				new ThirdPartyTradeAftersalesPushMarketingCenterDispatchListener(marketingProcessor);

		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_AFTERSALES,
				"listener:thirdparty.trade_aftersales_push_marketing_center",
				ListenerDispatchOptions.asyncDefaults(),
				marketingListener);

		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 42L);
		payload.put("order_id", 9001L);
		payload.put("aftersales_bn", 2026050712345678L);

		facade.publishEvent(
				SystemLinkDispatchEventNames.EVENT_TRADE_AFTERSALES, payload, DispatchOptions.eventDefaults());

		verify(marketingProcessor)
				.handle(
						argThat(
								map ->
										Long.valueOf(42L).equals(asLong(map.get("company_id")))
												&& Long.valueOf(9001L).equals(asLong(map.get("order_id")))
												&& Long.valueOf(2026050712345678L)
														.equals(asLong(map.get("aftersales_bn")))));
	}

	@Test
	void publishEvent_sync_invokesSendOmeStubThenMarketingListenerInRegistrationOrder() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<String> order = new ArrayList<>();

		DispatchListener omeStub = payload -> order.add("OME");
		DispatchListener marketingStub = payload -> order.add("MKT");

		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_AFTERSALES,
				LISTENER_OME,
				ListenerDispatchOptions.asyncDefaults(),
				omeStub);
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_AFTERSALES,
				"listener:thirdparty.trade_aftersales_push_marketing_center",
				ListenerDispatchOptions.asyncDefaults(),
				marketingStub);

		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 42L);
		payload.put("order_id", 9001L);
		payload.put("aftersales_bn", 2026050712345678L);

		facade.publishEvent(
				SystemLinkDispatchEventNames.EVENT_TRADE_AFTERSALES, payload, DispatchOptions.eventDefaults());

		assertEquals(List.of("OME", "MKT"), order);
	}

	private static Long asLong(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(o));
	}
}
