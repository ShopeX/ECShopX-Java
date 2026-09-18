package cn.shopex.ecshopx.dispatch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.orders.dispatch.UpdateGroupsActivityOrderDispatchListener;
import cn.shopex.ecshopx.orders.service.groups.UpdateGroupsActivityOrderBusService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EVENT_TRADE_FINISH: UpdateGroupsActivityOrder listener sync dispatch")
class UpdateGroupsActivityOrderEventSyncDispatchFlowTest {

	private static final String LISTENER_NAME = "listener:orders.listeners.UpdateGroupsActivityOrder";

	@Test
	void publishEvent_whenTradeSourceTypeNotNormalGroups_skipsBusService() {
		UpdateGroupsActivityOrderBusService bus = mock(UpdateGroupsActivityOrderBusService.class);
		UpdateGroupsActivityOrderDispatchListener listener = new UpdateGroupsActivityOrderDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.syncDefaults(),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 9L);
		payload.put("order_id", 501L);
		payload.put("user_id", 3L);
		payload.put("trade_source_type", "normal");
		payload.put("trade_state", "SUCCESS");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(bus, never()).handleTradeFinishRow(any());
	}

	@Test
	void publishEvent_whenNormalGroups_invokesHandleTradeFinishRow() {
		UpdateGroupsActivityOrderBusService bus = mock(UpdateGroupsActivityOrderBusService.class);
		UpdateGroupsActivityOrderDispatchListener listener = new UpdateGroupsActivityOrderDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.syncDefaults(),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 91L);
		payload.put("order_id", 601L);
		payload.put("user_id", 7L);
		payload.put("trade_source_type", "normal_groups");
		payload.put("trade_state", "SUCCESS");
		payload.put("pay_type", "alipay");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(bus, times(1))
				.handleTradeFinishRow(
						argThat(
								m ->
										m != null
												&& Objects.equals(91L, toLong(m.get("company_id")))
												&& Objects.equals(601L, toLong(m.get("order_id")))
												&& Objects.equals(7L, toLong(m.get("user_id")))
												&& "normal_groups"
														.equalsIgnoreCase(
																String.valueOf(m.get("trade_source_type"))
																		.trim())));
	}

	@Test
	@DisplayName(
			"offline_pay + positive pay_fee + normal_groups: handleTradeFinishRow once — mirror event:117 Wxapp payload shape in offline context")
	void publishEvent_sync_offlinePay_positivePayFee_normalGroups_wxappFieldLayout_invokesBusOnce() {
		UpdateGroupsActivityOrderBusService bus = mock(UpdateGroupsActivityOrderBusService.class);
		UpdateGroupsActivityOrderDispatchListener listener = new UpdateGroupsActivityOrderDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.syncDefaults(),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long epochSec = 1704067200L;
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("pay_type", "offline_pay");
		payload.put("pay_fee", 10_000);
		payload.put("company_id", 91L);
		payload.put("order_id", 2601L);
		payload.put("user_id", 7L);
		payload.put("time_start", String.valueOf(epochSec));
		payload.put("trade_source_type", "normal_groups");
		payload.put("trade_state", "SUCCESS");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(bus, times(1))
				.handleTradeFinishRow(
						argThat(
								m ->
										m != null
												&& "offline_pay".equals(String.valueOf(m.get("pay_type")))
												&& Objects.equals(10_000L, toLong(m.get("pay_fee")))
												&& Objects.equals(91L, toLong(m.get("company_id")))
												&& Objects.equals(2601L, toLong(m.get("order_id")))
												&& Objects.equals(7L, toLong(m.get("user_id")))
												&& "normal_groups"
														.equalsIgnoreCase(
																String.valueOf(m.get("trade_source_type"))
																		.trim())));
	}

	private static long toLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v));
	}
}
