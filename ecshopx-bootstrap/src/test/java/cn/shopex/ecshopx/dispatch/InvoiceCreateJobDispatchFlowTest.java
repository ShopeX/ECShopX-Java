package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchJobNames;
import cn.shopex.ecshopx.orders.dispatch.InvoiceCreateJobHandler;
import cn.shopex.ecshopx.orders.service.invoice.InvoiceCreateJobService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class InvoiceCreateJobDispatchFlowTest {

	@Test
	@DisplayName("InvoiceCreate: dispatch async to slow queue and consumer invokes job service")
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesService() {
		InvoiceCreateJobService jobService = Mockito.mock(InvoiceCreateJobService.class);
		InvoiceCreateJobHandler handler = new InvoiceCreateJobHandler(jobService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(OrdersDispatchJobNames.INVOICE_CREATE_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new SyncDispatchDriver(registry),
						java.util.Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("invoice_id", 7L);
		payload.put("company_id", 2L);
		payload.put("order_id", "ORD-1");
		payload.put("invoice_type", "enterprise");
		payload.put("invoice_type_code", "02");
		payload.put("company_title", "T");
		payload.put("company_tax_number", "N");
		payload.put("company_address", "A");
		payload.put("company_telephone", "T");
		payload.put("bank_name", "B");
		payload.put("bank_account", "1");
		payload.put("email", "e@e.com");
		payload.put("mobile", "138");

		facade.dispatchJob(
				OrdersDispatchJobNames.INVOICE_CREATE_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("slow", msg.queue());
		assertNull(msg.delay());
		assertEquals(OrdersDispatchJobNames.INVOICE_CREATE_JOB, msg.messageName());
		assertNull(msg.listenerName());
		assertEquals(7L, msg.payload().get("invoice_id"));
		assertEquals(2L, msg.payload().get("company_id"));
		assertEquals("ORD-1", msg.payload().get("order_id"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						Mockito.mock(FailedJobRecorder.class),
						Mockito.mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
		verify(jobService).execute(cap.capture());
		assertEquals(payload, cap.getValue());
	}
}
