package cn.shopex.ecshopx.orders.dispatch;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.orders.service.invoice.InvoicePushOmsJobService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class InvoicePushOmsJobHandlerTest {

	@Test
	void handle_nullOrInvalidPayload_skipsService() {
		InvoicePushOmsJobService svc = Mockito.mock(InvoicePushOmsJobService.class);
		InvoicePushOmsJobHandler handler = new InvoicePushOmsJobHandler(svc);

		handler.handle(null);

		Map<String, Object> empty = new LinkedHashMap<>();
		handler.handle(empty);

		Map<String, Object> badInvoice = new LinkedHashMap<>();
		badInvoice.put("invoice_id", 0L);
		badInvoice.put("company_id", 1L);
		handler.handle(badInvoice);

		Map<String, Object> badCompany = new LinkedHashMap<>();
		badCompany.put("invoice_id", 1L);
		badCompany.put("company_id", "0");
		handler.handle(badCompany);

		verify(svc, never()).execute(anyLong(), anyLong());
	}

	@Test
	void handle_validPayload_delegatesToService() {
		InvoicePushOmsJobService svc = Mockito.mock(InvoicePushOmsJobService.class);
		InvoicePushOmsJobHandler handler = new InvoicePushOmsJobHandler(svc);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("invoice_id", "9002");
		payload.put("company_id", 3L);
		handler.handle(payload);

		verify(svc).execute(9002L, 3L);
	}
}
