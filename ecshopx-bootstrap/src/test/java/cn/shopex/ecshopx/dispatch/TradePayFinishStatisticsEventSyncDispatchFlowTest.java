package cn.shopex.ecshopx.dispatch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.orders.dispatch.TradePayFinishStatisticsDispatchListener;
import cn.shopex.ecshopx.orders.service.statistics.TradePayFinishStatisticsBusService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TradePayFinishStatisticsEventSyncDispatchFlowTest {

	private static final String LISTENER_NAME = "listener:orders.listeners.TradePayFinishStatistics";

	@Test
	void publishEvent_whenTradeStateNotSuccess_skipsRecordPayFinishStatistics() {
		TradePayFinishStatisticsBusService bus = mock(TradePayFinishStatisticsBusService.class);
		TradePayFinishStatisticsDispatchListener listener = new TradePayFinishStatisticsDispatchListener(bus);

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
		payload.put("total_fee", 100);

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(bus, never()).recordPayFinishStatistics(anyMap());
	}

	@Test
	void publishEvent_whenTradeSourceTypeNotAllowed_skipsRecordPayFinishStatistics() {
		TradePayFinishStatisticsBusService bus = mock(TradePayFinishStatisticsBusService.class);
		TradePayFinishStatisticsDispatchListener listener = new TradePayFinishStatisticsDispatchListener(bus);

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
		payload.put("trade_source_type", "unknown_campaign_type");
		payload.put("trade_state", "SUCCESS");
		payload.put("total_fee", 100);

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(bus, never()).recordPayFinishStatistics(anyMap());
	}

	@Test
	void publishEvent_whenNormalSuccess_invokesRecordPayFinishStatisticsOnce() {
		TradePayFinishStatisticsBusService bus = mock(TradePayFinishStatisticsBusService.class);
		when(bus.isEligibleTradeSourceType(any())).thenReturn(true);
		TradePayFinishStatisticsDispatchListener listener = new TradePayFinishStatisticsDispatchListener(bus);

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
		payload.put("total_fee", 100);
		payload.put("user_id", "3");
		payload.put("distributor_id", "0");
		payload.put("merchant_id", 0L);

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(bus, times(1))
				.recordPayFinishStatistics(
						argThat(
								m ->
										m != null
												&& "normal".equals(String.valueOf(m.get("trade_source_type")))
												&& "SUCCESS".equals(String.valueOf(m.get("trade_state")))
												&& ((Number) m.get("company_id")).longValue() == 9L
												&& ((Number) m.get("order_id")).longValue() == 501L));
	}
}
