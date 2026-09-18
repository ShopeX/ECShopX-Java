package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchJobNames;
import cn.shopex.ecshopx.orders.dispatch.InvoicePushOmsJobHandler;
import cn.shopex.ecshopx.orders.service.invoice.InvoicePushOmsJobService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class InvoicePushOmsJobDispatchFlowTest {

	@Test
	@DisplayName("InvoicePushOms: dispatch async default queue and consumer invokes job service")
	void dispatchJob_asyncDefaultQueue_enqueuesAndConsumerInvokesJobService() {
		InvoicePushOmsJobService jobService = Mockito.mock(InvoicePushOmsJobService.class);
		InvoicePushOmsJobHandler handler = new InvoicePushOmsJobHandler(jobService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(OrdersDispatchJobNames.INVOICE_PUSH_OMS_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new SyncDispatchDriver(registry),
						java.util.Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		long invoiceId = 501L;
		long companyId = 9L;
		payload.put("invoice_id", invoiceId);
		payload.put("company_id", companyId);
		facade.dispatchJob(
				OrdersDispatchJobNames.INVOICE_PUSH_OMS_JOB,
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
		assertEquals(OrdersDispatchJobNames.INVOICE_PUSH_OMS_JOB, msg.messageName());
		assertEquals(invoiceId, msg.payload().get("invoice_id"));
		assertEquals(companyId, msg.payload().get("company_id"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						Mockito.mock(FailedJobRecorder.class),
						Mockito.mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(jobService).execute(eq(invoiceId), eq(companyId));
	}
}
