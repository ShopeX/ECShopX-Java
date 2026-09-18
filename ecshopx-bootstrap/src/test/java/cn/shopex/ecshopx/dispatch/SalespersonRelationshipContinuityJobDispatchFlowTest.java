package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import cn.shopex.ecshopx.common.dispatch.SalespersonDispatchJobNames;
import cn.shopex.ecshopx.salesperson.dispatch.SalespersonRelationshipContinuityJobHandler;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.MarketingCenterWxappActionEventPort;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SalespersonRelationshipContinuityJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesWxappActionEvent_withPayloadPassthrough() {
		MarketingCenterWxappActionEventPort port = mock(MarketingCenterWxappActionEventPort.class);
		SalespersonRelationshipContinuityJobHandler handler = new SalespersonRelationshipContinuityJobHandler(port);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(SalespersonDispatchJobNames.SALESPERSON_RELATIONSHIP_CONTINUITY_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new SyncDispatchDriver(registry),
						Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("event_type", "page_view");
		payload.put("event_id", "ev-1");
		payload.put("user_type", "member");
		payload.put("user_id", "u-9");
		payload.put("company_id", 77L);
		payload.put("path", "/pages/index");
		facade.dispatchJob(
				SalespersonDispatchJobNames.SALESPERSON_RELATIONSHIP_CONTINUITY_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals("slow", msg.queue());
		assertNull(msg.delay());
		assertEquals(SalespersonDispatchJobNames.SALESPERSON_RELATIONSHIP_CONTINUITY_JOB, msg.messageName());
		assertNull(msg.listenerName());
		assertEquals("page_view", msg.payload().get("event_type"));
		assertEquals("ev-1", msg.payload().get("event_id"));
		assertEquals("member", msg.payload().get("user_type"));
		assertEquals("u-9", msg.payload().get("user_id"));
		assertEquals(77L, ((Number) msg.payload().get("company_id")).longValue());
		assertEquals("/pages/index", msg.payload().get("path"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(port)
				.sendEvent(
						eq(77L),
						argThat(
								m ->
										m.get("event_type").equals("page_view")
												&& m.get("event_id").equals("ev-1")
												&& m.get("user_type").equals("member")
												&& m.get("user_id").equals("u-9")
												&& ((Number) m.get("company_id")).longValue() == 77L
												&& "/pages/index".equals(m.get("path"))));
	}

	@Test
	void dispatchJob_async_consumerSkipsSendEvent_whenCompanyIdMissingOrNonPositive() {
		assertConsumerSkipsWhenCompanyIdAbsent();
		assertConsumerSkipsWhenCompanyIdZero();
	}

	private void assertConsumerSkipsWhenCompanyIdAbsent() {
		MarketingCenterWxappActionEventPort port = mock(MarketingCenterWxappActionEventPort.class);
		SalespersonRelationshipContinuityJobHandler handler = new SalespersonRelationshipContinuityJobHandler(port);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(SalespersonDispatchJobNames.SALESPERSON_RELATIONSHIP_CONTINUITY_JOB, handler);
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new SyncDispatchDriver(registry),
						Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("event_type", "page_view");
		payload.put("event_id", "ev-1");
		payload.put("user_type", "member");
		payload.put("user_id", "u-9");
		facade.dispatchJob(
				SalespersonDispatchJobNames.SALESPERSON_RELATIONSHIP_CONTINUITY_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));
		DispatchMessage msg = captured.get(0);
		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);
		verifyNoInteractions(port);
	}

	private void assertConsumerSkipsWhenCompanyIdZero() {
		MarketingCenterWxappActionEventPort port = mock(MarketingCenterWxappActionEventPort.class);
		SalespersonRelationshipContinuityJobHandler handler = new SalespersonRelationshipContinuityJobHandler(port);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(SalespersonDispatchJobNames.SALESPERSON_RELATIONSHIP_CONTINUITY_JOB, handler);
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new SyncDispatchDriver(registry),
						Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("event_type", "page_view");
		payload.put("event_id", "ev-1");
		payload.put("user_type", "member");
		payload.put("user_id", "u-9");
		payload.put("company_id", 0L);
		facade.dispatchJob(
				SalespersonDispatchJobNames.SALESPERSON_RELATIONSHIP_CONTINUITY_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));
		DispatchMessage msg = captured.get(0);
		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);
		verifyNoInteractions(port);
	}
}
