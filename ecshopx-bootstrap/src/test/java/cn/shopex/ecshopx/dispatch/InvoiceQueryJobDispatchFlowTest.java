package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchJobNames;
import cn.shopex.ecshopx.orders.dispatch.InvoiceQueryJobHandler;
import cn.shopex.ecshopx.orders.service.invoice.InvoiceQueryJobService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class InvoiceQueryJobDispatchFlowTest {

	@Test
	@DisplayName("Blue invoice query: dispatch async to slow queue and consumer invokes job service")
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesService() {
		InvoiceQueryJobService jobService = Mockito.mock(InvoiceQueryJobService.class);
		InvoiceQueryJobHandler handler = new InvoiceQueryJobHandler(jobService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(OrdersDispatchJobNames.INVOICE_QUERY_JOB, handler);

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
		payload.put("invoice_apply_bn", "FPQ-1");

		facade.dispatchJob(
				OrdersDispatchJobNames.INVOICE_QUERY_JOB,
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
		assertEquals(OrdersDispatchJobNames.INVOICE_QUERY_JOB, msg.messageName());
		assertNull(msg.listenerName());
		assertEquals(7L, msg.payload().get("invoice_id"));
		assertEquals(2L, msg.payload().get("company_id"));
		assertEquals("ORD-1", msg.payload().get("order_id"));
		assertEquals("FPQ-1", msg.payload().get("invoice_apply_bn"));

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
