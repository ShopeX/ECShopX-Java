package cn.shopex.ecshopx.dispatch;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.systemlink.dispatch.TradeFinishSendOmeDispatchListener;
import cn.shopex.ecshopx.systemlink.service.ome.TradeFinishSendOmeBusService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EVENT_TRADE_FINISH: TradeFinishSendOme listener async dispatch")
class TradeFinishSendOmeEventSyncDispatchFlowTest {

	private static final String LISTENER_NAME = "listener:systemlink.trade_finish_send_ome";

	@Test
	void publishEvent_whenOmeDisabledOrPayloadIncomplete_skipsBusService() {
		TradeFinishSendOmeBusService bus = mock(TradeFinishSendOmeBusService.class);
		TradeFinishSendOmeDispatchListener listener = new TradeFinishSendOmeDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.async("default", null),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("order_id", 100L);

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(bus, never()).handleTradeFinishRow(anyMap());
	}

	@Test
	void publishEvent_whenNormalTradeRow_invokesHandleTradeFinishRowOnce() {
		TradeFinishSendOmeBusService bus = mock(TradeFinishSendOmeBusService.class);
		TradeFinishSendOmeDispatchListener listener = new TradeFinishSendOmeDispatchListener(bus);

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
		payload.put("order_id", 100L);
		payload.put("trade_source_type", "normal");
		payload.put("user_id", 1L);

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(bus, times(1))
				.handleTradeFinishRow(
						argThat(
								m ->
										m != null
												&& Objects.equals(9L, longish(m.get("company_id")))
												&& Objects.equals(100L, longish(m.get("order_id")))
												&& "normal".equals(String.valueOf(m.get("trade_source_type")))
												&& Objects.equals(1L, longish(m.get("user_id")))));
	}

	private static Long longish(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}
}
