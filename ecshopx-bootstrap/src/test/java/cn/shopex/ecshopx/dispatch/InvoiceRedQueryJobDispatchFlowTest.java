package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchJobNames;
import cn.shopex.ecshopx.orders.dispatch.InvoiceRedQueryJobHandler;
import cn.shopex.ecshopx.orders.service.invoice.InvoiceRedQueryJobService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class InvoiceRedQueryJobDispatchFlowTest {

	@Test
	@DisplayName("InvoiceRedQuery: dispatch async to slow queue and consumer invokes job service")
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesService() {
		InvoiceRedQueryJobService jobService = Mockito.mock(InvoiceRedQueryJobService.class);
		InvoiceRedQueryJobHandler handler = new InvoiceRedQueryJobHandler(jobService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(OrdersDispatchJobNames.INVOICE_RED_QUERY_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new SyncDispatchDriver(registry),
						java.util.Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 2L);
		payload.put("order_id", "ORD-1");
		payload.put("id", 7L);
		payload.put("red_confirm_serial_no", "RS-1");
		payload.put("entry_identity", "0");
		payload.put("type", "red");

		facade.dispatchJob(
				OrdersDispatchJobNames.INVOICE_RED_QUERY_JOB,
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
		assertEquals(OrdersDispatchJobNames.INVOICE_RED_QUERY_JOB, msg.messageName());
		assertNull(msg.listenerName());
		assertEquals(7L, msg.payload().get("id"));
		assertEquals(2L, msg.payload().get("company_id"));
		assertEquals("ORD-1", msg.payload().get("order_id"));
		assertEquals("RS-1", msg.payload().get("red_confirm_serial_no"));

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
