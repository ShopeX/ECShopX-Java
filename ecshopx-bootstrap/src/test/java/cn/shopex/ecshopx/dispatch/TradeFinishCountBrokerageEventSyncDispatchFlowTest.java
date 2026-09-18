package cn.shopex.ecshopx.dispatch;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.orders.dispatch.TradeFinishCountBrokerageDispatchListener;
import cn.shopex.ecshopx.orders.service.brokerage.TradeFinishCountBrokerageBusService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EVENT_TRADE_FINISH: TradeFinishCountBrokerage listener async dispatch")
class TradeFinishCountBrokerageEventSyncDispatchFlowTest {

	private static final String LISTENER_NAME = "listener:orders.listeners.TradeFinishCountBrokerage";

	@Test
	void publishEvent_whenOrderIdBlank_skipsBusService() {
		TradeFinishCountBrokerageBusService bus = mock(TradeFinishCountBrokerageBusService.class);
		TradeFinishCountBrokerageDispatchListener listener = new TradeFinishCountBrokerageDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.async("default", null),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 9L);
		payload.put("order_id", "");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(bus, never()).handleTradeFinishRow(anyMap());
	}

	@Test
	void publishEvent_whenPayTypePoint_skipsBrokerageLogic() {
		TradeFinishCountBrokerageBusService bus = mock(TradeFinishCountBrokerageBusService.class);
		TradeFinishCountBrokerageDispatchListener listener = new TradeFinishCountBrokerageDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.async("default", null),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 9L);
		payload.put("order_id", 501L);
		payload.put("pay_type", "point");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(bus, never()).handleTradeFinishRow(anyMap());
	}

	@Test
	void publishEvent_whenAlipayLikePayload_invokesHandleTradeFinishRow() {
		TradeFinishCountBrokerageBusService bus = mock(TradeFinishCountBrokerageBusService.class);
		TradeFinishCountBrokerageDispatchListener listener = new TradeFinishCountBrokerageDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.async("default", null),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 9L);
		payload.put("order_id", 501L);
		payload.put("pay_type", "alipay");
		payload.put("trade_state", "SUCCESS");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(bus, times(1)).handleTradeFinishRow(anyMap());
	}

	@Test
	@DisplayName("offline_pay + positive pay_fee + SUCCESS: TradeFinishCountBrokerage handleTradeFinishRow once")
	void publishEvent_whenOfflinePayPositivePayFee_invokesHandleTradeFinishRowOnce() {
		TradeFinishCountBrokerageBusService bus = mock(TradeFinishCountBrokerageBusService.class);
		TradeFinishCountBrokerageDispatchListener listener = new TradeFinishCountBrokerageDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.async("default", null),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long epochSec = 1704067200L;
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 9L);
		payload.put("order_id", 501L);
		payload.put("pay_type", "offline_pay");
		payload.put("trade_state", "SUCCESS");
		payload.put("pay_fee", 10_000);
		payload.put("time_start", String.valueOf(epochSec));

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(bus, times(1))
				.handleTradeFinishRow(
						argThat(
								m -> "offline_pay".equals(String.valueOf(m.get("pay_type")))
										&& m.get("pay_fee") != null
										&& ((Number) m.get("pay_fee")).longValue() == 10_000L));
	}
}
