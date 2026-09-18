package cn.shopex.ecshopx.orders.dispatch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.orders.service.invoice.InvoiceQueryJobService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class InvoiceQueryJobHandlerTest {

	@Test
	void handle_nullPayload_noop() {
		InvoiceQueryJobService svc = Mockito.mock(InvoiceQueryJobService.class);
		InvoiceQueryJobHandler handler = new InvoiceQueryJobHandler(svc);

		handler.handle(null);

		verify(svc, never()).execute(any());
	}

	@Test
	void handle_delegatesToService_whenAllKeysPresent() {
		InvoiceQueryJobService svc = Mockito.mock(InvoiceQueryJobService.class);
		InvoiceQueryJobHandler handler = new InvoiceQueryJobHandler(svc);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("invoice_id", 42L);
		payload.put("company_id", 1L);
		payload.put("order_id", "O1");
		payload.put("invoice_apply_bn", "BN");

		handler.handle(payload);

		verify(svc).execute(payload);
	}

	@Test
	void handle_missingInvoiceId_noServiceCall() {
		InvoiceQueryJobService svc = Mockito.mock(InvoiceQueryJobService.class);
		InvoiceQueryJobHandler handler = new InvoiceQueryJobHandler(svc);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);
		payload.put("order_id", "O1");
		payload.put("invoice_apply_bn", "BN");

		handler.handle(payload);

		verify(svc, never()).execute(any());
	}

	@Test
	void handle_missingCompanyId_noServiceCall() {
		InvoiceQueryJobService svc = Mockito.mock(InvoiceQueryJobService.class);
		InvoiceQueryJobHandler handler = new InvoiceQueryJobHandler(svc);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("invoice_id", 1L);
		payload.put("order_id", "O1");
		payload.put("invoice_apply_bn", "BN");

		handler.handle(payload);

		verify(svc, never()).execute(any());
	}

	@Test
	void handle_missingOrderId_noServiceCall() {
		InvoiceQueryJobService svc = Mockito.mock(InvoiceQueryJobService.class);
		InvoiceQueryJobHandler handler = new InvoiceQueryJobHandler(svc);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("invoice_id", 1L);
		payload.put("company_id", 1L);
		payload.put("invoice_apply_bn", "BN");

		handler.handle(payload);

		verify(svc, never()).execute(any());
	}
}
