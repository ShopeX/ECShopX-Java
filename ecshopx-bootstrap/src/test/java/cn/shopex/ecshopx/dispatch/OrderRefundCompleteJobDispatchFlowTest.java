package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.aftersales.dispatch.AftersalesOrderRefundCompleteJobHandler;
import cn.shopex.ecshopx.aftersales.service.AftersalesOrderRefundCompleteJobService;
import cn.shopex.ecshopx.common.dispatch.AftersalesDispatchJobNames;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderRefundCompleteJobDispatchFlowTest {

	@Test
	@DisplayName("Order refund complete job async dispatch targets slow queue and invokes job service on consume")
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesJobService() {
		AftersalesOrderRefundCompleteJobService jobService = mock(AftersalesOrderRefundCompleteJobService.class);
		AftersalesOrderRefundCompleteJobHandler handler = new AftersalesOrderRefundCompleteJobHandler(jobService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AftersalesDispatchJobNames.ORDER_REFUND_COMPLETE_JOB, handler);

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
		facade.dispatchJob(
				AftersalesDispatchJobNames.ORDER_REFUND_COMPLETE_JOB,
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
		assertEquals(AftersalesDispatchJobNames.ORDER_REFUND_COMPLETE_JOB, msg.messageName());
		assertEquals(2L, msg.payload().get("company_id"));
		assertEquals(100L, msg.payload().get("order_id"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(jobService).execute(eq(2L), eq(100L));
	}
}
