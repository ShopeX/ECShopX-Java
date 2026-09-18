package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchJobNames;
import cn.shopex.ecshopx.orders.dispatch.RefundByOrderUpdateOrderStatusJobHandler;
import cn.shopex.ecshopx.orders.service.refund.RefundByOrderUpdateOrderStatusJobService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RefundByOrderUpdateOrderStatusJobDispatchFlowTest {

	@Test
	@DisplayName("dispatchJob async default queue 5s delay enqueues and consumer invokes job service")
	void dispatchJob_asyncDefaultQueue5sDelay_enqueuesAndConsumerInvokesExecutorService() {
		RefundByOrderUpdateOrderStatusJobService jobService = Mockito.mock(RefundByOrderUpdateOrderStatusJobService.class);
		RefundByOrderUpdateOrderStatusJobHandler handler = new RefundByOrderUpdateOrderStatusJobHandler(jobService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(OrdersDispatchJobNames.REFUND_BY_ORDER_UPDATE_ORDER_STATUS_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new SyncDispatchDriver(registry),
						java.util.Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("order_id", 8001L);
		payload.put("company_id", 9L);
		payload.put("order_type", "normal_groups");
		facade.dispatchJob(
				OrdersDispatchJobNames.REFUND_BY_ORDER_UPDATE_ORDER_STATUS_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"default",
						Duration.ofSeconds(5),
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("default", msg.queue());
		assertEquals(Duration.ofSeconds(5), msg.delay());
		assertEquals(OrdersDispatchJobNames.REFUND_BY_ORDER_UPDATE_ORDER_STATUS_JOB, msg.messageName());
		assertEquals(8001L, msg.payload().get("order_id"));
		assertEquals(9L, msg.payload().get("company_id"));
		assertEquals("normal_groups", msg.payload().get("order_type"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						Mockito.mock(FailedJobRecorder.class),
						Mockito.mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(jobService).execute(eq(8001L), eq(9L), eq("normal_groups"));
	}
}
