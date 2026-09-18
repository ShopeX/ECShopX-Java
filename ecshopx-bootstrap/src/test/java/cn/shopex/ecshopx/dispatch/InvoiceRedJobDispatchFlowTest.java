package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchJobNames;
import cn.shopex.ecshopx.orders.dispatch.OrdersInvoiceRedJobHandler;
import cn.shopex.ecshopx.orders.service.OrdersInvoiceRedJobService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InvoiceRedJobDispatchFlowTest {

	@Test
	@DisplayName(
			"Shared aftersales admin review post-commit invoice-red path: async dispatch targets invoice queue and consumer invokes job service")
	void dispatchJob_async_enqueuesOnInvoiceQueue_andConsumerInvokesJobService() {
		OrdersInvoiceRedJobService jobService = mock(OrdersInvoiceRedJobService.class);
		OrdersInvoiceRedJobHandler handler = new OrdersInvoiceRedJobHandler(jobService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(OrdersDispatchJobNames.INVOICE_RED_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new SyncDispatchDriver(registry),
						java.util.Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 2L);
		payload.put("order_id", 100L);
		payload.put("aftersales_bn", "9001");
		facade.dispatchJob(
				OrdersDispatchJobNames.INVOICE_RED_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"invoice",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("invoice", msg.queue());
		assertNull(msg.delay());
		assertEquals(OrdersDispatchJobNames.INVOICE_RED_JOB, msg.messageName());
		assertEquals(2L, msg.payload().get("company_id"));
		assertEquals(100L, msg.payload().get("order_id"));
		assertEquals("9001", msg.payload().get("aftersales_bn"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(jobService).execute(eq(msg.payload()));
	}
}
