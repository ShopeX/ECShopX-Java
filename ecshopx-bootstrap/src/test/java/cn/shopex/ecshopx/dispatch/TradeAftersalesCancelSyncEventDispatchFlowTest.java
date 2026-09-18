package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.common.dispatch.SystemLinkDispatchEventNames;
import cn.shopex.ecshopx.thirdparty.dispatch.ThirdPartyTradeAftersalesCancelPushMarketingCenterDispatchListener;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.TradeAftersalesCancelPushMarketingCenterProcessor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Trade aftersales cancel SYNC event: Bus listener invokes marketing center cancel processor")
class TradeAftersalesCancelSyncEventDispatchFlowTest {

	private static final String LISTENER_MKT =
			"listener:thirdparty.trade_aftersales_cancel_push_marketing_center";

	@Test
	void publishEvent_sync_tradeAftersalesCancelPayload_invokesCancelProcessorHandle() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		TradeAftersalesCancelPushMarketingCenterProcessor marketingProcessor =
				mock(TradeAftersalesCancelPushMarketingCenterProcessor.class);
		ThirdPartyTradeAftersalesCancelPushMarketingCenterDispatchListener marketingListener =
				new ThirdPartyTradeAftersalesCancelPushMarketingCenterDispatchListener(marketingProcessor);

		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_AFTERSALES_CANCEL,
				LISTENER_MKT,
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
				SystemLinkDispatchEventNames.EVENT_TRADE_AFTERSALES_CANCEL, payload, DispatchOptions.eventDefaults());

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
	void publishEvent_sync_invokesOmeListenerThenMarketingListenerInRegistrationOrder() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<String> order = new ArrayList<>();

		DispatchListener omeCancel = payload -> order.add("OME_CANCEL");
		DispatchListener marketingCancel = payload -> order.add("MKT_CANCEL");

		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_AFTERSALES_CANCEL,
				"listener:systemlink.trade_aftersale_cancel_send_ome",
				ListenerDispatchOptions.asyncDefaults(),
				omeCancel);
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_AFTERSALES_CANCEL,
				LISTENER_MKT,
				ListenerDispatchOptions.asyncDefaults(),
				marketingCancel);

		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 42L);
		payload.put("order_id", 9001L);
		payload.put("aftersales_bn", 2026050712345678L);

		facade.publishEvent(
				SystemLinkDispatchEventNames.EVENT_TRADE_AFTERSALES_CANCEL, payload, DispatchOptions.eventDefaults());

		assertEquals(List.of("OME_CANCEL", "MKT_CANCEL"), order);
	}

	@Test
	void publishEvent_sync_invokesSingleCancelListenerInRegistrationOrder() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<String> order = new ArrayList<>();

		DispatchListener marketingStub = payload -> order.add("MKT_CANCEL");

		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_AFTERSALES_CANCEL,
				LISTENER_MKT,
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
				SystemLinkDispatchEventNames.EVENT_TRADE_AFTERSALES_CANCEL, payload, DispatchOptions.eventDefaults());

		assertEquals(List.of("MKT_CANCEL"), order);
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
