package cn.shopex.ecshopx.dispatch;

import static org.mockito.ArgumentMatchers.anyMap;
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

/**
 * Wxapp-shaped {@code EVENT_TRADE_FINISH} rows with the same registry topology as production: {@link
 * InMemoryDispatchRegistry} registers {@code listener:orders.listeners.TradeFinishCountBrokerage} with {@link
 * ListenerDispatchOptions#async(String, java.time.Duration)} queue {@code default}. Unlike {@link
 * UpdateItemSalesWxappPaymentSyncDispatchFlowTest}, {@code pay_type=point} short-circuits in {@link
 * TradeFinishCountBrokerageDispatchListener} — {@link TradeFinishCountBrokerageBusService} is never invoked.
 *
 * @see TradeFinishCountBrokerageEventSyncDispatchFlowTest
 */
@DisplayName("EVENT_TRADE_FINISH: Wxapp-style TradeFinishCountBrokerage sync dispatch slice")
class TradeFinishCountBrokerageWxappPaymentSyncDispatchFlowTest {

	private static final String LISTENER_NAME = "listener:orders.listeners.TradeFinishCountBrokerage";

	private static DispatchFacade buildFacade(TradeFinishCountBrokerageDispatchListener listener) {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.async("default", null),
				listener);
		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		return new DispatchFacade(core, new DispatchFanOutPlanner(registry));
	}

	@Test
	@DisplayName("wxpay zero fee + numeric order_id: handleTradeFinishRow once (TradeFinishCountBrokerage gate keys)")
	void publishEvent_sync_wxpayZeroFee_withOrderId_invokesBrokerageBusOnce() {
		TradeFinishCountBrokerageBusService bus = mock(TradeFinishCountBrokerageBusService.class);
		DispatchFacade facade = buildFacade(new TradeFinishCountBrokerageDispatchListener(bus));

		long epochSec = 1704067200L;
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("pay_type", "wxpay");
		payload.put("pay_fee", 0);
		payload.put("company_id", 9L);
		payload.put("order_id", 2001L);
		payload.put("time_start", String.valueOf(epochSec));

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(bus, times(1)).handleTradeFinishRow(anyMap());
	}

	@Test
	@DisplayName("point pay + numeric order_id: never invokes brokerage (contrasts UpdateItemSales still running)")
	void publishEvent_sync_pointPay_withOrderId_skipsBrokerageBus() {
		TradeFinishCountBrokerageBusService bus = mock(TradeFinishCountBrokerageBusService.class);
		DispatchFacade facade = buildFacade(new TradeFinishCountBrokerageDispatchListener(bus));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("pay_type", "point");
		payload.put("pay_fee", 50);
		payload.put("company_id", 9L);
		payload.put("order_id", 2002L);
		payload.put("time_start", "1704067200");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(bus, never()).handleTradeFinishRow(anyMap());
	}
}
