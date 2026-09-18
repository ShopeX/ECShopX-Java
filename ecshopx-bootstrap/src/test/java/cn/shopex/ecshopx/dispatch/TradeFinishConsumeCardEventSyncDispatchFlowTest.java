package cn.shopex.ecshopx.dispatch;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.kaquan.dispatch.TradeFinishConsumeCardDispatchListener;
import cn.shopex.ecshopx.kaquan.service.discount.TradeFinishConsumeCardBusService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EVENT_TRADE_FINISH: trade-finish consume-card listener sync dispatch")
class TradeFinishConsumeCardEventSyncDispatchFlowTest {

	private static final String LISTENER_NAME = "listener:kaquan.trade_finish_consume_card_pay_bill";

	@Test
	void publishEvent_sync_skipsBusServiceWhenPayTypeIsPoint() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		TradeFinishConsumeCardBusService busService = mock(TradeFinishConsumeCardBusService.class);
		TradeFinishConsumeCardDispatchListener listener = new TradeFinishConsumeCardDispatchListener(busService);

		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.asyncDefaults(),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 91L);
		payload.put("user_id", 1L);
		payload.put("order_id", "O-1");
		payload.put("pay_type", "point");
		payload.put("discount_info", "[{\"coupon_code\":\"Z\"}]");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verifyNoInteractions(busService);
	}

	@Test
	void publishEvent_sync_skipsBusServiceWhenPayTypeIsDeposit() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		TradeFinishConsumeCardBusService busService = mock(TradeFinishConsumeCardBusService.class);
		TradeFinishConsumeCardDispatchListener listener = new TradeFinishConsumeCardDispatchListener(busService);

		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.asyncDefaults(),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 91L);
		payload.put("user_id", 1L);
		payload.put("order_id", "O-1");
		payload.put("pay_type", "Deposit");
		payload.put("discount_info", "[{\"coupon_code\":\"Z\"}]");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verifyNoInteractions(busService);
	}

	@Test
	void publishEvent_sync_invokesBusServiceWithPayloadWhenPayTypeIsWxpayAndDiscountInfoPresent() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		TradeFinishConsumeCardBusService busService = mock(TradeFinishConsumeCardBusService.class);
		TradeFinishConsumeCardDispatchListener listener = new TradeFinishConsumeCardDispatchListener(busService);

		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.asyncDefaults(),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 91L);
		payload.put("user_id", 1L);
		payload.put("order_id", "O-1");
		payload.put("pay_type", "wxpay");
		payload.put("discount_info", "[{\"coupon_code\":\"CARD99\"}]");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(busService, times(1))
				.onTradeFinishTradeRow(
						argThat(
								m ->
										m != null
												&& Objects.equals(91L, toLong(m.get("company_id")))
												&& Objects.equals(1L, toLong(m.get("user_id")))
												&& "O-1".equals(String.valueOf(m.get("order_id")))
												&& "wxpay"
														.equalsIgnoreCase(
																String.valueOf(m.get("pay_type")).trim())
												&& "[{\"coupon_code\":\"CARD99\"}]"
														.equals(String.valueOf(m.get("discount_info")))));
	}

	private static long toLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v));
	}
}
