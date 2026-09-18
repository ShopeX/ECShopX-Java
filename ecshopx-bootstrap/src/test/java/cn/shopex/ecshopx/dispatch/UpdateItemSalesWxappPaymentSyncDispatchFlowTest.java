package cn.shopex.ecshopx.dispatch;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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

/**
 * Wxapp-shaped {@code EVENT_TRADE_FINISH} payloads with the same registry topology as production: {@link
 * InMemoryDispatchRegistry} registers {@code listener:orders.listeners.UpdateItemSalesListener} with {@link
 * ListenerDispatchOptions#async(String, java.time.Duration)} queue {@code default}. Unlike {@link
 * WxappPaymentTradeFinishSmsNotifySyncDispatchFlowTest}, {@code pay_type=point} does not early-exit sales — {@link
 * UpdateItemSalesBusService} keys only on {@code company_id} / {@code order_id}.
 *
 * @see WxappPaymentTradeFinishSmsNotifySyncDispatchFlowTest
 */
@DisplayName("EVENT_TRADE_FINISH: Wxapp-style UpdateItemSales sync dispatch slice")
class UpdateItemSalesWxappPaymentSyncDispatchFlowTest {

	private static final String LISTENER_NAME = "listener:orders.listeners.UpdateItemSalesListener";

	private static DispatchFacade buildFacade(UpdateItemSalesDispatchListener listener) {
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
	@DisplayName("wxpay zero fee + numeric order_id: incrementSalesForNormalOrder once (UpdateItemSales gate keys)")
	void publishEvent_sync_wxpayZeroFee_withOrderId_incrementsSalesOnce() {
		TradeFinishItemSalesIncrementPort salesPort = mock(TradeFinishItemSalesIncrementPort.class);
		UpdateItemSalesBusService bus = new UpdateItemSalesBusService(salesPort);
		DispatchFacade facade = buildFacade(new UpdateItemSalesDispatchListener(bus));

		long epochSec = 1704067200L;
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("pay_type", "wxpay");
		payload.put("pay_fee", 0);
		payload.put("company_id", 9L);
		payload.put("order_id", 2001L);
		payload.put("time_start", String.valueOf(epochSec));

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(salesPort, times(1)).incrementSalesForNormalOrder(eq(9L), eq(2001L));
	}

	@Test
	@DisplayName("point pay + numeric order_id: still increments (contrasts SMS listener early exit)")
	void publishEvent_sync_pointPay_withOrderId_incrementsSalesOnce() {
		TradeFinishItemSalesIncrementPort salesPort = mock(TradeFinishItemSalesIncrementPort.class);
		UpdateItemSalesBusService bus = new UpdateItemSalesBusService(salesPort);
		DispatchFacade facade = buildFacade(new UpdateItemSalesDispatchListener(bus));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("pay_type", "point");
		payload.put("pay_fee", 50);
		payload.put("company_id", 9L);
		payload.put("order_id", 2002L);
		payload.put("time_start", "1704067200");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(salesPort, times(1)).incrementSalesForNormalOrder(eq(9L), eq(2002L));
	}
}
