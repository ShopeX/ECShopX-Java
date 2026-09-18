package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.aliyunsms.dispatch.QuerySmsTemplateJobHandler;
import cn.shopex.ecshopx.aliyunsms.service.TemplateService;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsDispatchJobNames;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class QuerySmsTemplateJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSmsQueue_andConsumerInvokesTemplateServiceApplyPath() {
		TemplateService templateServiceMock = mock(TemplateService.class);
		QuerySmsTemplateJobHandler handler = new QuerySmsTemplateJobHandler(templateServiceMock);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AliyunsmsDispatchJobNames.QUERY_SMS_TEMPLATE_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 7L);
		payload.put("template_code", "TCODE");

		facade.dispatchJob(
				AliyunsmsDispatchJobNames.QUERY_SMS_TEMPLATE_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"sms",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("sms", msg.queue());
		assertEquals(AliyunsmsDispatchJobNames.QUERY_SMS_TEMPLATE_JOB, msg.messageName());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(templateServiceMock).applyPendingTemplateAuditFromCloud(eq(7L), eq("TCODE"));
	}

	@Test
	void dispatchJob_async_payloadMatchesQuerySmsTemplatePublisherEnvelope() {
		TemplateService templateServiceMock = mock(TemplateService.class);
		QuerySmsTemplateJobHandler handler = new QuerySmsTemplateJobHandler(templateServiceMock);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AliyunsmsDispatchJobNames.QUERY_SMS_TEMPLATE_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		// Same envelope as AliyunsmsQuerySmsTemplateJobDispatchPublisherImpl#publish
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 99L);
		payload.put("template_code", "AlignedTemplate");

		facade.dispatchJob(
				AliyunsmsDispatchJobNames.QUERY_SMS_TEMPLATE_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"sms",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("sms", msg.queue());
		assertEquals(AliyunsmsDispatchJobNames.QUERY_SMS_TEMPLATE_JOB, msg.messageName());
		Map<String, Object> p = msg.payload();
		assertEquals(99L, p.get("company_id"));
		assertEquals("AlignedTemplate", p.get("template_code"));
		assertEquals(2, p.size());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(templateServiceMock).applyPendingTemplateAuditFromCloud(eq(99L), eq("AlignedTemplate"));
	}

	@Test
	void runtimeConsume_handCraftedQuerySmsTemplateJob_invokesApplyPath() {
		TemplateService templateServiceMock = mock(TemplateService.class);
		QuerySmsTemplateJobHandler handler = new QuerySmsTemplateJobHandler(templateServiceMock);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AliyunsmsDispatchJobNames.QUERY_SMS_TEMPLATE_JOB, handler);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 7L);
		payload.put("template_code", "TCODE");

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						AliyunsmsDispatchJobNames.QUERY_SMS_TEMPLATE_JOB,
						payload,
						"sms",
						null,
						RetryPolicy.platformDefault(),
						Instant.now(),
						"trace-query-template-consumer",
						null);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(templateServiceMock).applyPendingTemplateAuditFromCloud(eq(7L), eq("TCODE"));
	}
}
