package cn.shopex.ecshopx.orders.dispatch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.orders.service.invoice.InvoiceCreateJobService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class InvoiceCreateJobHandlerTest {

	@Test
	void handle_nullPayload_noop() {
		InvoiceCreateJobService svc = Mockito.mock(InvoiceCreateJobService.class);
		InvoiceCreateJobHandler handler = new InvoiceCreateJobHandler(svc);

		handler.handle(null);

		verify(svc, never()).execute(any());
	}

	@Test
	void handle_delegatesToService_whenInvoiceIdPresent() {
		InvoiceCreateJobService svc = Mockito.mock(InvoiceCreateJobService.class);
		InvoiceCreateJobHandler handler = new InvoiceCreateJobHandler(svc);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("invoice_id", 42L);
		payload.put("company_id", 1L);

		handler.handle(payload);

		verify(svc).execute(payload);
	}

	@Test
	void handle_missingInvoiceId_noServiceCall() {
		InvoiceCreateJobService svc = Mockito.mock(InvoiceCreateJobService.class);
		InvoiceCreateJobHandler handler = new InvoiceCreateJobHandler(svc);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);

		handler.handle(payload);

		verify(svc, never()).execute(any());
	}
}
