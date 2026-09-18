package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.aliyunsms.dispatch.QuerySmsSignJobHandler;
import cn.shopex.ecshopx.aliyunsms.service.SignService;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsDispatchJobNames;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class QuerySmsSignJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSmsQueue_andConsumerInvokesSignServiceApplyPath() {
		SignService signServiceMock = mock(SignService.class);
		QuerySmsSignJobHandler handler = new QuerySmsSignJobHandler(signServiceMock);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AliyunsmsDispatchJobNames.QUERY_SMS_SIGN_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 7L);
		payload.put("sign_name", "StoreSign");

		facade.dispatchJob(
				AliyunsmsDispatchJobNames.QUERY_SMS_SIGN_JOB,
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
		assertEquals(AliyunsmsDispatchJobNames.QUERY_SMS_SIGN_JOB, msg.messageName());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(signServiceMock).applyPendingSignAuditFromCloud(eq(7L), eq("StoreSign"));
	}

	@Test
	void dispatchJob_async_payloadMatchesQuerySmsSignPublisherEnvelope() {
		SignService signServiceMock = mock(SignService.class);
		QuerySmsSignJobHandler handler = new QuerySmsSignJobHandler(signServiceMock);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AliyunsmsDispatchJobNames.QUERY_SMS_SIGN_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		// Same envelope as AliyunsmsQuerySmsSignJobDispatchPublisherImpl#publish
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 99L);
		payload.put("sign_name", "AlignedSignName");

		facade.dispatchJob(
				AliyunsmsDispatchJobNames.QUERY_SMS_SIGN_JOB,
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
		assertEquals(AliyunsmsDispatchJobNames.QUERY_SMS_SIGN_JOB, msg.messageName());
		Map<String, Object> p = msg.payload();
		assertEquals(99L, p.get("company_id"));
		assertEquals("AlignedSignName", p.get("sign_name"));
		assertEquals(2, p.size());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(signServiceMock).applyPendingSignAuditFromCloud(eq(99L), eq("AlignedSignName"));
	}

	@Test
	void runtimeConsume_handCraftedQuerySmsSignJob_invokesApplyPath() {
		SignService signServiceMock = mock(SignService.class);
		QuerySmsSignJobHandler handler = new QuerySmsSignJobHandler(signServiceMock);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AliyunsmsDispatchJobNames.QUERY_SMS_SIGN_JOB, handler);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 7L);
		payload.put("sign_name", "StoreSign");

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						AliyunsmsDispatchJobNames.QUERY_SMS_SIGN_JOB,
						payload,
						"sms",
						null,
						RetryPolicy.platformDefault(),
						Instant.now(),
						"trace-query-sign-consumer",
						null);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(signServiceMock).applyPendingSignAuditFromCloud(eq(7L), eq("StoreSign"));
	}
}
