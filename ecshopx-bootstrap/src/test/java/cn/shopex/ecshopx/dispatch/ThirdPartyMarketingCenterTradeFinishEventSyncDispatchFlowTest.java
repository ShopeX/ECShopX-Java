package cn.shopex.ecshopx.dispatch;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.thirdparty.dispatch.ThirdPartyMarketingCenterTradeFinishDispatchListener;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.TradeFinishPushMarketingCenterProcessor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EVENT_TRADE_FINISH: marketing center basics.order.pay listener sync dispatch")
class ThirdPartyMarketingCenterTradeFinishEventSyncDispatchFlowTest {

	private static final String LISTENER_MC =
			"listener:thirdparty.marketing_center.trade_finish_basics_order_pay";

	@Test
	void publishEvent_sync_invokesTradeFinishMarketingCenterProcessorOnce() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		TradeFinishPushMarketingCenterProcessor processor = mock(TradeFinishPushMarketingCenterProcessor.class);
		ThirdPartyMarketingCenterTradeFinishDispatchListener listener =
				new ThirdPartyMarketingCenterTradeFinishDispatchListener(processor);

		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_MC,
				ListenerDispatchOptions.asyncDefaults(),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 91L);
		payload.put("order_id", 77001L);
		payload.put("trade_id", "T-001");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(processor, times(1))
				.handle(
						argThat(
								m ->
										Objects.equals(91L, toLong(m.get("company_id")))
												&& Objects.equals(77001L, toLong(m.get("order_id")))
												&& "T-001".equals(String.valueOf(m.get("trade_id")))));
	}

	private static long toLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v));
	}
}
