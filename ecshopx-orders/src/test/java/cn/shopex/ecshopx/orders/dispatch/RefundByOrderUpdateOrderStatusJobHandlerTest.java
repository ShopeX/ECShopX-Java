package cn.shopex.ecshopx.orders.dispatch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.orders.service.refund.RefundByOrderUpdateOrderStatusJobService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RefundByOrderUpdateOrderStatusJobHandlerTest {

	@Test
	void whenPayloadMissingOrderId_skipsService() {
		RefundByOrderUpdateOrderStatusJobService svc = Mockito.mock(RefundByOrderUpdateOrderStatusJobService.class);
		RefundByOrderUpdateOrderStatusJobHandler handler = new RefundByOrderUpdateOrderStatusJobHandler(svc);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);
		payload.put("order_type", "normal_groups");
		handler.handle(payload);

		verify(svc, never()).execute(anyLong(), anyLong(), any());
	}

	@Test
	void whenValidPayload_invokesServiceWithParsedFields() {
		RefundByOrderUpdateOrderStatusJobService svc = Mockito.mock(RefundByOrderUpdateOrderStatusJobService.class);
		RefundByOrderUpdateOrderStatusJobHandler handler = new RefundByOrderUpdateOrderStatusJobHandler(svc);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("order_id", "9002");
		payload.put("company_id", 3L);
		payload.put("order_type", "normal_groups");
		handler.handle(payload);

		verify(svc).execute(eq(9002L), eq(3L), eq("normal_groups"));
	}
}
