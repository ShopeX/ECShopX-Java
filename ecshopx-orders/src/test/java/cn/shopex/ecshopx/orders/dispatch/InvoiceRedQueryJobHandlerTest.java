package cn.shopex.ecshopx.orders.dispatch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.orders.service.invoice.InvoiceRedQueryJobService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class InvoiceRedQueryJobHandlerTest {

	@Test
	void handle_nullPayload_noop() {
		InvoiceRedQueryJobService svc = Mockito.mock(InvoiceRedQueryJobService.class);
		InvoiceRedQueryJobHandler handler = new InvoiceRedQueryJobHandler(svc);

		handler.handle(null);

		verify(svc, never()).execute(any());
	}

	@Test
	void handle_delegatesToService_whenAllKeysPresent() {
		InvoiceRedQueryJobService svc = Mockito.mock(InvoiceRedQueryJobService.class);
		InvoiceRedQueryJobHandler handler = new InvoiceRedQueryJobHandler(svc);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("id", 42L);
		payload.put("company_id", 1L);
		payload.put("order_id", "O1");
		payload.put("red_confirm_serial_no", "RS");

		handler.handle(payload);

		verify(svc).execute(payload);
	}

	@Test
	void handle_missingId_noServiceCall() {
		InvoiceRedQueryJobService svc = Mockito.mock(InvoiceRedQueryJobService.class);
		InvoiceRedQueryJobHandler handler = new InvoiceRedQueryJobHandler(svc);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);
		payload.put("order_id", "O1");
		payload.put("red_confirm_serial_no", "RS");

		handler.handle(payload);

		verify(svc, never()).execute(any());
	}

	@Test
	void handle_missingRedConfirmSerial_noServiceCall() {
		InvoiceRedQueryJobService svc = Mockito.mock(InvoiceRedQueryJobService.class);
		InvoiceRedQueryJobHandler handler = new InvoiceRedQueryJobHandler(svc);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("id", 1L);
		payload.put("company_id", 1L);
		payload.put("order_id", "O1");

		handler.handle(payload);

		verify(svc, never()).execute(any());
	}
}
