package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.KaquanDispatchJobNames;
import cn.shopex.ecshopx.config.BatchReceiveMemberCardDispatchPublisherImpl;
import cn.shopex.ecshopx.kaquan.dispatch.BatchReceiveMemberCardJobHandler;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeBatchActiveDelayService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BatchReceiveMemberCardJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesRunExpiredJob() {
		VipGradeBatchActiveDelayService delayService = mock(VipGradeBatchActiveDelayService.class);
		BatchReceiveMemberCardJobHandler handler = new BatchReceiveMemberCardJobHandler(delayService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(KaquanDispatchJobNames.BATCH_RECEIVE_MEMBER_CARD, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);
		payload.put("vip_grade_id", 2L);
		payload.put("day", 7);
		payload.put("add_day", 7);
		payload.put("vip_type", "vip");
		payload.put("filter", "expired");

		facade.dispatchJob(
				KaquanDispatchJobNames.BATCH_RECEIVE_MEMBER_CARD,
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
		assertEquals(KaquanDispatchJobNames.BATCH_RECEIVE_MEMBER_CARD, msg.messageName());

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(delayService).runExpiredJobInOneTransaction(eq(1L), eq(2L), eq(7), eq("vip"));
	}

	@Test
	void handler_logsFailureWithoutRethrowing_soConsumeAcks() {
		VipGradeBatchActiveDelayService delayService = mock(VipGradeBatchActiveDelayService.class);
		doThrow(new RuntimeException("downstream"))
				.when(delayService)
				.runExpiredJobInOneTransaction(anyLong(), anyLong(), anyInt(), eq("vip"));
		BatchReceiveMemberCardJobHandler handler = new BatchReceiveMemberCardJobHandler(delayService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(KaquanDispatchJobNames.BATCH_RECEIVE_MEMBER_CARD, handler);

		Map<String, Object> handlerFailPayload = new LinkedHashMap<>();
		handlerFailPayload.put("company_id", 9L);
		handlerFailPayload.put("vip_grade_id", 3L);
		handlerFailPayload.put("day", 5);
		handlerFailPayload.put("add_day", 5);
		handlerFailPayload.put("vip_type", "vip");
		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						KaquanDispatchJobNames.BATCH_RECEIVE_MEMBER_CARD,
						handlerFailPayload,
						"slow",
						null,
						RetryPolicy.platformDefault(),
						Instant.now(),
						"trace-test",
						null);

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(delayService).runExpiredJobInOneTransaction(eq(9L), eq(3L), eq(5), eq("vip"));
	}

	@Test
	void dispatchJob_publishPayloadMatchesExportDataExpiredEnvelope() {
		VipGradeBatchActiveDelayService delayService = mock(VipGradeBatchActiveDelayService.class);
		BatchReceiveMemberCardJobHandler handler = new BatchReceiveMemberCardJobHandler(delayService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(KaquanDispatchJobNames.BATCH_RECEIVE_MEMBER_CARD, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		BatchReceiveMemberCardDispatchPublisherImpl publisher = new BatchReceiveMemberCardDispatchPublisherImpl(facade);
		publisher.publish(101L, 202L, 14, "vip");

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("slow", msg.queue());
		assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals(KaquanDispatchJobNames.BATCH_RECEIVE_MEMBER_CARD, msg.messageName());

		Map<String, Object> p = msg.payload();
		assertEquals(101L, p.get("company_id"));
		assertEquals(202L, p.get("vip_grade_id"));
		assertEquals(14, p.get("day"));
		assertEquals(14, p.get("add_day"));
		assertEquals("vip", p.get("vip_type"));
		assertEquals("expired", p.get("filter"));

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(delayService).runExpiredJobInOneTransaction(eq(101L), eq(202L), eq(14), eq("vip"));
	}
}
