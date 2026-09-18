package cn.shopex.ecshopx.dispatch;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.orders.dispatch.TradeFinishFapiaoDispatchListener;
import cn.shopex.ecshopx.orders.service.invoice.TradeFinishFapiaoBusService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TradeFinishFapiaoEventSyncDispatchFlowTest {

	private static final String LISTENER_NAME = "listener:orders.listeners.TradeFinishFapiao";

	@Test
	void publishEvent_whenTradeStateNotSuccess_skipsBusService() {
		TradeFinishFapiaoBusService bus = mock(TradeFinishFapiaoBusService.class);
		TradeFinishFapiaoDispatchListener listener = new TradeFinishFapiaoDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.async("slow", null),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 9L);
		payload.put("order_id", 501L);
		payload.put("trade_source_type", "normal");
		payload.put("trade_state", "NOTPAY");
		payload.put("user_id", 3L);

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(bus, never()).handleTradeFinishRow(anyMap());
	}

	@Test
	void publishEvent_whenSuccess_invokesHandleTradeFinishRowOnce() {
		TradeFinishFapiaoBusService bus = mock(TradeFinishFapiaoBusService.class);
		TradeFinishFapiaoDispatchListener listener = new TradeFinishFapiaoDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.async("slow", null),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 9L);
		payload.put("order_id", 501L);
		payload.put("trade_source_type", "normal");
		payload.put("trade_state", "SUCCESS");
		payload.put("user_id", "3");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(bus, times(1))
				.handleTradeFinishRow(
						argThat(
								m -> {
									if (m == null) {
										return false;
									}
									Long uid = userIdFromPayload(m.get("user_id"));
									return "normal".equals(String.valueOf(m.get("trade_source_type")))
											&& "SUCCESS".equals(String.valueOf(m.get("trade_state")))
											&& ((Number) m.get("company_id")).longValue() == 9L
											&& ((Number) m.get("order_id")).longValue() == 501L
											&& uid != null
											&& uid == 3L;
								}));
	}

	private static Long userIdFromPayload(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
