package cn.shopex.ecshopx.thirdparty.dispatch;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmTradeFinishOrchestratorService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ThirdPartyDmCrmTradeFinishDispatchListenerTest {

	@Mock private DmCrmTradeFinishOrchestratorService orchestrator;

	@Test
	void onEvent_delegatesToOrchestrator() {
		ThirdPartyDmCrmTradeFinishDispatchListener listener =
				new ThirdPartyDmCrmTradeFinishDispatchListener(orchestrator);
		Map<String, Object> payload = Map.of("company_id", 1L, "order_id", 2L);
		listener.onEvent(payload);
		verify(orchestrator).handleTradeFinish(any());
	}
}
