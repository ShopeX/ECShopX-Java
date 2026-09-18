package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchJobNames;
import cn.shopex.ecshopx.orders.dispatch.ConsumptionOrderJobHandler;
import cn.shopex.ecshopx.orders.service.consumption.ConsumptionOrderJobService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class ConsumptionOrderJobDispatchFlowTest {

	@Test
	@DisplayName("Consumption order job: dispatch async to slow queue and consumer invokes job service")
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesConsumptionJobService() {
		ConsumptionOrderJobService jobService = Mockito.mock(ConsumptionOrderJobService.class);
		ConsumptionOrderJobHandler handler = new ConsumptionOrderJobHandler(jobService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(OrdersDispatchJobNames.CONSUMPTION_ORDER_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new SyncDispatchDriver(registry),
						java.util.Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = Map.of("orderType", "normal", "pageSize", "100");

		facade.dispatchJob(
				OrdersDispatchJobNames.CONSUMPTION_ORDER_JOB,
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
		assertEquals(OrdersDispatchJobNames.CONSUMPTION_ORDER_JOB, msg.messageName());
		assertNull(msg.listenerName());
		assertEquals("normal", msg.payload().get("orderType"));
		assertEquals("100", msg.payload().get("pageSize"));

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
		Map<String, Object> arg = cap.getValue();
		assertEquals("normal", arg.get("orderType"));
		assertEquals("100", arg.get("pageSize"));
	}
}
