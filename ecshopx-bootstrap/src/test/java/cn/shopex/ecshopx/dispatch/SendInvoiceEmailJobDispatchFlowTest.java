package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchJobNames;
import cn.shopex.ecshopx.orders.dispatch.SendInvoiceEmailJobHandler;
import cn.shopex.ecshopx.orders.service.invoice.SendInvoiceEmailJobService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SendInvoiceEmailJobDispatchFlowTest {

	@Test
	@DisplayName("SendInvoiceEmail: dispatch async default queue and consumer invokes job service")
	void dispatchJob_asyncDefaultQueue_enqueuesAndConsumerInvokesJobService() {
		SendInvoiceEmailJobService jobService = Mockito.mock(SendInvoiceEmailJobService.class);
		SendInvoiceEmailJobHandler handler = new SendInvoiceEmailJobHandler(jobService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(OrdersDispatchJobNames.SEND_INVOICE_EMAIL_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new SyncDispatchDriver(registry),
						java.util.Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		String email = "a@b.com";
		String url = "https://invoice.example/file";
		long companyId = 9L;
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("email", email);
		payload.put("invoice_file_url", url);
		payload.put("company_id", companyId);
		facade.dispatchJob(
				OrdersDispatchJobNames.SEND_INVOICE_EMAIL_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"default",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals("default", msg.queue());
		assertNull(msg.delay());
		assertNull(msg.listenerName());
		assertEquals(OrdersDispatchJobNames.SEND_INVOICE_EMAIL_JOB, msg.messageName());
		assertEquals(email, msg.payload().get("email"));
		assertEquals(url, msg.payload().get("invoice_file_url"));
		assertEquals(companyId, msg.payload().get("company_id"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						Mockito.mock(FailedJobRecorder.class),
						Mockito.mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(jobService).execute(eq(email), eq(url), eq(companyId), isNull());
	}
}
