package cn.shopex.ecshopx.orders.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.orders.service.consumption.ConsumptionOrderJobService;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class ConsumptionOrderJobHandlerTest {

	@Test
	@DisplayName("handle forwards the job payload map to ConsumptionOrderJobService#execute unchanged")
	void handle_delegatesToService_withPayload() {
		ConsumptionOrderJobService svc = Mockito.mock(ConsumptionOrderJobService.class);
		ConsumptionOrderJobHandler handler = new ConsumptionOrderJobHandler(svc);

		Map<String, Object> payload = Map.of("orderType", "normal", "pageSize", "100");
		handler.handle(payload);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
		verify(svc).execute(cap.capture());
		assertEquals("normal", cap.getValue().get("orderType"));
		assertEquals("100", cap.getValue().get("pageSize"));
	}

	@Test
	@DisplayName("handle(null) delegates with an empty map so the service always receives a non-null payload")
	void handle_nullPayload_treatedAsEmptyMap() {
		ConsumptionOrderJobService svc = Mockito.mock(ConsumptionOrderJobService.class);
		ConsumptionOrderJobHandler handler = new ConsumptionOrderJobHandler(svc);

		handler.handle(null);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
		verify(svc).execute(cap.capture());
		assertEquals(Map.of(), cap.getValue());
	}
}
