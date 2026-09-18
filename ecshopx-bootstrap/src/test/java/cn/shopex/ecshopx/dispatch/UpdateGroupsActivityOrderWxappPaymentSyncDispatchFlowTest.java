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

/**
 * Wxapp-shaped {@code EVENT_TRADE_FINISH} payloads with registry topology aligned to production for {@code
 * listener:orders.listeners.UpdateGroupsActivityOrder}: {@link InMemoryDispatchRegistry} uses {@link
 * ListenerDispatchOptions#syncDefaults()} (not async Redis). Contrasts {@link UpdateItemSalesWxappPaymentSyncDispatchFlowTest}:
 * groups listener gates on {@code trade_source_type=normal_groups}; {@code pay_type=point} does not skip when that gate
 * passes.
 *
 * @see UpdateGroupsActivityOrderEventSyncDispatchFlowTest
 * @see UpdateItemSalesWxappPaymentSyncDispatchFlowTest
 */
@DisplayName("EVENT_TRADE_FINISH: Wxapp-style UpdateGroupsActivityOrder sync dispatch slice")
class UpdateGroupsActivityOrderWxappPaymentSyncDispatchFlowTest {

	private static final String LISTENER_NAME = "listener:orders.listeners.UpdateGroupsActivityOrder";

	private static DispatchFacade buildFacade(UpdateGroupsActivityOrderDispatchListener listener) {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.syncDefaults(),
				listener);
		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		return new DispatchFacade(core, new DispatchFanOutPlanner(registry));
	}

	@Test
	@DisplayName(
			"wxapp keys + normal_groups: handleTradeFinishRow once (contrasts ItemSales: groups gates on trade_source_type)")
	void publishEvent_sync_wxappShape_normalGroups_invokesBusOnce() {
		UpdateGroupsActivityOrderBusService bus = mock(UpdateGroupsActivityOrderBusService.class);
		DispatchFacade facade = buildFacade(new UpdateGroupsActivityOrderDispatchListener(bus));

		long epochSec = 1704067200L;
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("pay_type", "wxpay");
		payload.put("pay_fee", 0);
		payload.put("company_id", 9L);
		payload.put("order_id", 2001L);
		payload.put("user_id", 5L);
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
												&& Objects.equals(9L, toLong(m.get("company_id")))
												&& Objects.equals(2001L, toLong(m.get("order_id")))
												&& Objects.equals(5L, toLong(m.get("user_id")))
												&& "normal_groups"
														.equalsIgnoreCase(
																String.valueOf(m.get("trade_source_type"))
																		.trim())));
	}

	@Test
	@DisplayName(
			"wxapp shape but trade_source_type=normal: groups listener skips (contrasts ItemSales: point still runs)")
	void publishEvent_sync_wxappShape_normalTradeSource_skipsBus() {
		UpdateGroupsActivityOrderBusService bus = mock(UpdateGroupsActivityOrderBusService.class);
		DispatchFacade facade = buildFacade(new UpdateGroupsActivityOrderDispatchListener(bus));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("pay_type", "wxpay");
		payload.put("pay_fee", 0);
		payload.put("company_id", 9L);
		payload.put("order_id", 2002L);
		payload.put("user_id", 6L);
		payload.put("time_start", "1704067200");
		payload.put("trade_source_type", "normal");
		payload.put("trade_state", "SUCCESS");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(bus, never()).handleTradeFinishRow(any());
	}

	@Test
	@DisplayName("point pay + normal_groups: still invokes bus (pay_type does not gate groups listener)")
	void publishEvent_sync_pointPay_normalGroups_invokesBusOnce() {
		UpdateGroupsActivityOrderBusService bus = mock(UpdateGroupsActivityOrderBusService.class);
		DispatchFacade facade = buildFacade(new UpdateGroupsActivityOrderDispatchListener(bus));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("pay_type", "point");
		payload.put("pay_fee", 50);
		payload.put("company_id", 9L);
		payload.put("order_id", 2003L);
		payload.put("user_id", 8L);
		payload.put("time_start", "1704067200");
		payload.put("trade_source_type", "normal_groups");
		payload.put("trade_state", "SUCCESS");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(bus, times(1))
				.handleTradeFinishRow(
						argThat(
								m ->
										m != null
												&& "point".equals(String.valueOf(m.get("pay_type")))
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
