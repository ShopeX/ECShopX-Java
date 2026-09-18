package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.PopularizeBundleDispatchJobNames;
import cn.shopex.ecshopx.popularize.dispatch.UpgradePromoterGradeJobDispatchPublisher;
import cn.shopex.ecshopx.popularize.dispatch.UpgradePromoterGradeJobHandler;
import cn.shopex.ecshopx.popularize.service.UpgradePromoterGradeJobRunService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class UpgradePromoterGradeJobDispatchFlowTest {

	@Test
	@DisplayName("dispatch job 211 async enqueues on default queue and consumer invokes handler")
	void dispatchJob_async_enqueuesOnDefaultQueue_andConsumerInvokesHandler() {
		UpgradePromoterGradeJobRunService runService = Mockito.mock(UpgradePromoterGradeJobRunService.class);
		UpgradePromoterGradeJobHandler handler = new UpgradePromoterGradeJobHandler(runService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(PopularizeBundleDispatchJobNames.UPGRADE_PROMOTER_GRADE, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = samplePayload();
		facade.dispatchJob(
				PopularizeBundleDispatchJobNames.UPGRADE_PROMOTER_GRADE,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertNull(msg.queue());
		assertNull(msg.delay());
		assertEquals(PopularizeBundleDispatchJobNames.UPGRADE_PROMOTER_GRADE, msg.messageName());
		assertNull(msg.listenerName());
		assertNotNull(msg.occurredAt());
		assertNotNull(msg.traceId());
		assertTrue(msg.traceId().length() > 0);
		assertNotNull(msg.retryPolicy());
		Map<String, Object> pl = msg.payload();
		assertEquals(2, pl.size());
		assertEquals(100L, asLong(pl.get("company_id")));
		assertEquals(200L, asLong(pl.get("user_id")));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(runService).run(100L, 200L);
	}

	@Test
	void dispatchJob_publishPayloadMatchesUpgradePromoterGradeEnvelope() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		UpgradePromoterGradeJobDispatchPublisher publisher = new UpgradePromoterGradeJobDispatchPublisher(dispatchFacade);

		publisher.enqueueUpgradePromoterGrade(100L, 200L);

		verify(dispatchFacade)
				.dispatchJob(
						eq(PopularizeBundleDispatchJobNames.UPGRADE_PROMOTER_GRADE),
						Mockito.argThat(
								m ->
										m != null
												&& 100L == asLong(m.get("company_id"))
												&& 200L == asLong(m.get("user_id"))),
						Mockito.argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.ASYNC
												&& opts.driverOverride() == DispatchDriverType.REDIS
												&& opts.queue() == null
												&& opts.delay() == null
												&& opts.retryPolicy() != null));
	}

	private static Map<String, Object> samplePayload() {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 100L);
		payload.put("user_id", 200L);
		return payload;
	}

	private static long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}
}
