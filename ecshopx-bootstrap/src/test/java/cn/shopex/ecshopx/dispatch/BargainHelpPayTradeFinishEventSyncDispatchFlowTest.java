package cn.shopex.ecshopx.dispatch;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.promotions.dispatch.BargainHelpPayTradeFinishDispatchListener;
import cn.shopex.ecshopx.promotions.service.bargain.BargainHelpPayTradeFinishActivityService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EVENT_TRADE_FINISH: bargain help-pay listener sync dispatch")
class BargainHelpPayTradeFinishEventSyncDispatchFlowTest {

	private static final String LISTENER_NAME = "listener:orders.trade_finish_help_pay_bargain_plus_one";

	@Test
	void publishEvent_sync_skipsActivityWhenTradeSourceTypeIsNotBargain() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		BargainHelpPayTradeFinishActivityService activityService =
				mock(BargainHelpPayTradeFinishActivityService.class);
		BargainHelpPayTradeFinishDispatchListener listener =
				new BargainHelpPayTradeFinishDispatchListener(activityService);

		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.asyncDefaults(),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 91L);
		payload.put("order_id", 77001L);
		payload.put("trade_id", "T-001");
		payload.put("trade_source_type", "normal");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verifyNoInteractions(activityService);
	}

	@Test
	void publishEvent_sync_invokesActivityServiceOnceForBargainTradeSource() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		BargainHelpPayTradeFinishActivityService activityService =
				mock(BargainHelpPayTradeFinishActivityService.class);
		BargainHelpPayTradeFinishDispatchListener listener =
				new BargainHelpPayTradeFinishDispatchListener(activityService);

		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.asyncDefaults(),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 91L);
		payload.put("order_id", 77001L);
		payload.put("trade_id", "T-001");
		payload.put("trade_source_type", "bargain");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(activityService, times(1))
				.onTradeFinishTradeRow(
						argThat(
								m ->
										m != null
												&& Objects.equals(91L, toLong(m.get("company_id")))
												&& Objects.equals(77001L, toLong(m.get("order_id")))
												&& "T-001".equals(String.valueOf(m.get("trade_id")))
												&& "bargain"
														.equalsIgnoreCase(
																String.valueOf(m.get("trade_source_type")).trim())));
	}

	private static long toLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v));
	}
}
