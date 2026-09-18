package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.orders.dispatch.OrdersTradeFinishWorkWechatDispatchListener;
import cn.shopex.ecshopx.orders.service.workwechat.TradeFinishWorkWechatNotifyService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TradeFinishWorkWechatNotifyEventSyncDispatchFlowTest {

	private static final String LISTENER_NAME = "listener:orders.listeners.TradeFinishWorkWechatNotify";

	@Test
	void publishEvent_whenPayloadHasCompanyAndOrder_invokesTradeFinishWorkWechatNotifyServiceOnce() {
		TradeFinishWorkWechatNotifyService service = mock(TradeFinishWorkWechatNotifyService.class);
		OrdersTradeFinishWorkWechatDispatchListener listener =
				new OrdersTradeFinishWorkWechatDispatchListener(service);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.syncDefaults(),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", "9");
		payload.put("order_id", "501");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(service, times(1)).dispatchTradeFinishWorkWechatDeliveryWaitJobs(same(payload));

		DispatchMessage last = facade.lastPublishedMessage();
		assertNotNull(last);
		assertEquals(DispatchMessageType.EVENT, last.messageType());
		assertEquals(DispatchDriverType.SYNC, last.driverType());
		assertEquals(LISTENER_NAME, last.listenerName());
	}

	@Test
	void publishEvent_whenTradeRowMissingCompanyId_skipsService() {
		TradeFinishWorkWechatNotifyService service = mock(TradeFinishWorkWechatNotifyService.class);
		OrdersTradeFinishWorkWechatDispatchListener listener =
				new OrdersTradeFinishWorkWechatDispatchListener(service);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.syncDefaults(),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("order_id", "501");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verifyNoInteractions(service);
	}
}
