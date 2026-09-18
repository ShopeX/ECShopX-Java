package cn.shopex.ecshopx.dispatch;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.port.goods.TradeFinishItemSalesIncrementPort;
import cn.shopex.ecshopx.orders.dispatch.UpdateItemSalesDispatchListener;
import cn.shopex.ecshopx.orders.service.sales.UpdateItemSalesBusService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EVENT_TRADE_FINISH: UpdateItemSales listener sync dispatch")
class UpdateItemSalesEventSyncDispatchFlowTest {

	private static final String LISTENER_NAME = "listener:orders.listeners.UpdateItemSalesListener";

	@Test
	void publishEvent_whenOrderIdBlank_skipsBusService() {
		TradeFinishItemSalesIncrementPort salesPort = mock(TradeFinishItemSalesIncrementPort.class);
		UpdateItemSalesBusService bus = new UpdateItemSalesBusService(salesPort);
		UpdateItemSalesDispatchListener listener = new UpdateItemSalesDispatchListener(bus);

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

		verify(salesPort, never()).incrementSalesForNormalOrder(anyLong(), anyLong());
	}

	@Test
	void publishEvent_whenOrderIdPresent_invokesUpdateItemSalesBusService() {
		TradeFinishItemSalesIncrementPort salesPort = mock(TradeFinishItemSalesIncrementPort.class);
		UpdateItemSalesBusService bus = new UpdateItemSalesBusService(salesPort);
		UpdateItemSalesDispatchListener listener = new UpdateItemSalesDispatchListener(bus);

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

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(salesPort, times(1)).incrementSalesForNormalOrder(9L, 501L);
	}

	@Test
	@DisplayName(
			"offline_pay with positive pay_fee (fen, e.g. 10000) still invokes TradeFinishItemSalesIncrementPort — mirror offline do_check trade row shape")
	void publishEvent_whenOfflinePayPositivePayFee_invokesIncrementSalesForNormalOrder() {
		TradeFinishItemSalesIncrementPort salesPort = mock(TradeFinishItemSalesIncrementPort.class);
		UpdateItemSalesBusService bus = new UpdateItemSalesBusService(salesPort);
		UpdateItemSalesDispatchListener listener = new UpdateItemSalesDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.async("default", null),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 88L);
		payload.put("order_id", 42L);
		payload.put("trade_source_type", "normal");
		payload.put("pay_type", "offline_pay");
		payload.put("pay_fee", 10_000);
		payload.put("time_start", "1704067200");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(salesPort, times(1)).incrementSalesForNormalOrder(88L, 42L);
	}
}
