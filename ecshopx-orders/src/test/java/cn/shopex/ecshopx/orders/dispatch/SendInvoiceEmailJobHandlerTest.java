package cn.shopex.ecshopx.orders.dispatch;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.orders.service.invoice.SendInvoiceEmailJobService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SendInvoiceEmailJobHandlerTest {

	@Test
	void handle_missingEmail_skipsService() {
		SendInvoiceEmailJobService svc = Mockito.mock(SendInvoiceEmailJobService.class);
		SendInvoiceEmailJobHandler handler = new SendInvoiceEmailJobHandler(svc);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("invoice_file_url", "https://x");
		payload.put("company_id", 1L);
		handler.handle(payload);

		verify(svc, never()).execute(anyString(), anyString(), anyLong(), nullable(String.class));
	}

	@Test
	void handle_missingInvoiceFileUrl_skipsService() {
		SendInvoiceEmailJobService svc = Mockito.mock(SendInvoiceEmailJobService.class);
		SendInvoiceEmailJobHandler handler = new SendInvoiceEmailJobHandler(svc);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("email", "a@b.com");
		payload.put("company_id", 1L);
		handler.handle(payload);

		verify(svc, never()).execute(anyString(), anyString(), anyLong(), nullable(String.class));
	}

	@Test
	void handle_invalidCompanyId_skipsService() {
		SendInvoiceEmailJobService svc = Mockito.mock(SendInvoiceEmailJobService.class);
		SendInvoiceEmailJobHandler handler = new SendInvoiceEmailJobHandler(svc);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("email", "a@b.com");
		payload.put("invoice_file_url", "https://x");
		payload.put("company_id", "0");
		handler.handle(payload);

		verify(svc, never()).execute(anyString(), anyString(), anyLong(), nullable(String.class));
	}

	@Test
	void handle_validPayload_delegatesToService() {
		SendInvoiceEmailJobService svc = Mockito.mock(SendInvoiceEmailJobService.class);
		SendInvoiceEmailJobHandler handler = new SendInvoiceEmailJobHandler(svc);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("email", "a@b.com");
		payload.put("invoice_file_url", "https://inv.example/f");
		payload.put("company_id", 9L);
		handler.handle(payload);

		verify(svc).execute(eq("a@b.com"), eq("https://inv.example/f"), eq(9L), isNull());
	}
}
