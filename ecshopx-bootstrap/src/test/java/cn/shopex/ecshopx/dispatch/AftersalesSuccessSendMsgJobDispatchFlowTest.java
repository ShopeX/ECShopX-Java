package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.dispatch.AftersalesSuccessSendMsgJobHandler;
import cn.shopex.ecshopx.aftersales.service.AftersalesSuccessSendMsgJobService;
import cn.shopex.ecshopx.common.dispatch.AftersalesDispatchJobNames;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AftersalesSuccessSendMsgJobDispatchFlowTest {

	@Test
	@DisplayName("Aftersales success send-msg job async dispatch targets slow queue and invokes job service on consume")
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesJobService() {
		AftersalesSuccessSendMsgJobService jobService = mock(AftersalesSuccessSendMsgJobService.class);
		AftersalesSuccessSendMsgJobHandler handler = new AftersalesSuccessSendMsgJobHandler(jobService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AftersalesDispatchJobNames.AFTERSALES_SUCCESS_SEND_MSG_JOB, handler);

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
		payload.put("aftersales_bn", 55L);
		facade.dispatchJob(
				AftersalesDispatchJobNames.AFTERSALES_SUCCESS_SEND_MSG_JOB,
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
		assertEquals(AftersalesDispatchJobNames.AFTERSALES_SUCCESS_SEND_MSG_JOB, msg.messageName());
		assertEquals(2L, msg.payload().get("company_id"));
		assertEquals(100L, msg.payload().get("order_id"));
		assertEquals(55L, msg.payload().get("aftersales_bn"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(jobService).execute(eq(2L), eq(100L), eq(55L));
	}

	@Test
	@DisplayName("Consumer completes without propagating when job service throws and retries are disabled")
	void consume_whenJobServiceThrows_andRetriesDisabled_recordsWithoutPropagating() {
		AftersalesSuccessSendMsgJobService jobService = mock(AftersalesSuccessSendMsgJobService.class);
		doThrow(new RuntimeException("wx-downstream"))
				.when(jobService)
				.execute(anyLong(), anyLong(), anyLong());

		AftersalesSuccessSendMsgJobHandler handler = new AftersalesSuccessSendMsgJobHandler(jobService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AftersalesDispatchJobNames.AFTERSALES_SUCCESS_SEND_MSG_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new SyncDispatchDriver(registry),
						Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 2L);
		payload.put("order_id", 100L);
		payload.put("aftersales_bn", 55L);
		facade.dispatchJob(
				AftersalesDispatchJobNames.AFTERSALES_SUCCESS_SEND_MSG_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		DispatchMessage msg = captured.get(0);

		DispatchRetryDecider decider = mock(DispatchRetryDecider.class);
		when(decider.shouldRetry(any(), anyInt())).thenReturn(false);
		FailedJobRecorder failedJobRecorder = mock(FailedJobRecorder.class);
		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						decider,
						failedJobRecorder,
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());

		assertDoesNotThrow(() -> runtime.consume(msg, 1));
		verify(failedJobRecorder).recordFailure(eq(msg), eq(1), eq(msg.traceId()), any(RuntimeException.class));
	}
}
