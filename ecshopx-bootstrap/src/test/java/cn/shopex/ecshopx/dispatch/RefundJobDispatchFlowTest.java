package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.aftersales.dispatch.AftersalesRefundJobHandler;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundJobService;
import cn.shopex.ecshopx.common.dispatch.AftersalesDispatchJobNames;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesRefundQueueMessage;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefundJobDispatchFlowTest {

	@Test
	@DisplayName("Refund job bus dispatch without delay matches first-per-order scheduling")
	void dispatchJob_withoutDelay_enqueuesAndHandlerDelegatesToJobService() {
		AftersalesRefundJobService jobService = mock(AftersalesRefundJobService.class);
		AftersalesRefundJobHandler handler = new AftersalesRefundJobHandler(jobService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AftersalesDispatchJobNames.REFUND_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new SyncDispatchDriver(registry),
						java.util.Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("refund_bn", 9L);
		payload.put("company_id", 2L);
		payload.put("order_id", 100L);
		facade.dispatchJob(
				AftersalesDispatchJobNames.REFUND_JOB,
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
		assertEquals(AftersalesDispatchJobNames.REFUND_JOB, msg.messageName());
		assertEquals(9L, msg.payload().get("refund_bn"));
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

		verify(jobService)
				.handle(argThat(m -> m != null && m.getRefundBn() == 9L && m.getCompanyId() == 2L && m.getOrderId() == 100L));
	}

	@Test
	void dispatchJob_withDelay_setsDispatchMessageDelayAndStillConsumes() {
		AftersalesRefundJobService jobService = mock(AftersalesRefundJobService.class);
		AftersalesRefundJobHandler handler = new AftersalesRefundJobHandler(jobService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AftersalesDispatchJobNames.REFUND_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new SyncDispatchDriver(registry),
						java.util.Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("refund_bn", 11L);
		payload.put("company_id", 3L);
		payload.put("order_id", 200L);
		facade.dispatchJob(
				AftersalesDispatchJobNames.REFUND_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						Duration.ofSeconds(61),
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(Duration.ofSeconds(61), msg.delay());
		assertEquals("slow", msg.queue());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(jobService).handle(eq(new AftersalesRefundQueueMessage(11L, 3L, 200L)));
	}
}
