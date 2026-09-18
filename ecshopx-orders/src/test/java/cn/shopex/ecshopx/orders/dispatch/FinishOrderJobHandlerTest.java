package cn.shopex.ecshopx.orders.dispatch;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.orders.service.finish.FinishOrderJobService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class FinishOrderJobHandlerTest {

	@Test
	void handle_delegatesToService_withEmptyPayload() {
		FinishOrderJobService svc = Mockito.mock(FinishOrderJobService.class);
		FinishOrderJobHandler handler = new FinishOrderJobHandler(svc);

		handler.handle(Map.of());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
		verify(svc).execute(cap.capture());
		assertTrue(cap.getValue().isEmpty());
	}

	@Test
	void handle_nullPayload_treatedAsEmptyMap() {
		FinishOrderJobService svc = Mockito.mock(FinishOrderJobService.class);
		FinishOrderJobHandler handler = new FinishOrderJobHandler(svc);

		handler.handle(null);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
		verify(svc).execute(cap.capture());
		assertTrue(cap.getValue().isEmpty());
	}
}
