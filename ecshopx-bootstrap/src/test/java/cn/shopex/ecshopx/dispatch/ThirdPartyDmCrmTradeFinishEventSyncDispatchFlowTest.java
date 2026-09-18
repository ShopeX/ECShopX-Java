package cn.shopex.ecshopx.dispatch;


import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.thirdparty.dispatch.ThirdPartyDmCrmTradeFinishDispatchListener;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmTradeFinishOrchestratorService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EVENT_TRADE_FINISH: DmCrm listener sync dispatch")
class ThirdPartyDmCrmTradeFinishEventSyncDispatchFlowTest {

	private static final String LISTENER_DM_CRM = "listener:thirdparty.dm_crm.trade_finish";

	@Test
	void publishEvent_sync_invokesDmCrmTradeFinishOrchestratorOnce() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		DmCrmTradeFinishOrchestratorService orchestrator = mock(DmCrmTradeFinishOrchestratorService.class);
		ThirdPartyDmCrmTradeFinishDispatchListener listener =
				new ThirdPartyDmCrmTradeFinishDispatchListener(orchestrator);

		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_DM_CRM,
				ListenerDispatchOptions.asyncDefaults(),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 91L);
		payload.put("order_id", 77001L);

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(orchestrator, times(1))
				.handleTradeFinish(
						argThat(
								m ->
										Objects.equals(91L, toLong(m.get("company_id")))
												&& Objects.equals(77001L, toLong(m.get("order_id")))));
	}

	private static long toLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v));
	}
}
