package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aliyunsms.dispatch.ModifySmsSignJobHandler;
import cn.shopex.ecshopx.aliyunsms.integration.AliyunsmsUpdateSmsSignClient;
import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsAddSmsSignImageSupport;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsDispatchJobNames;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class ModifySmsSignJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSmsQueue_andConsumerInvokesImageSupportThenUpdateClient() {
		AliyunsmsUpdateSmsSignClient clientMock = mock(AliyunsmsUpdateSmsSignClient.class);
		AliyunsmsAddSmsSignImageSupport imageMock = mock(AliyunsmsAddSmsSignImageSupport.class);
		Map<String, Object> enriched = new LinkedHashMap<>();
		enriched.put("sign_file", Map.of("fileContents", "QQ==", "fileSuffix", "png"));
		when(imageMock.enrichModifyParams(anyLong(), anyMap())).thenReturn(enriched);

		ModifySmsSignJobHandler handler = new ModifySmsSignJobHandler(imageMock, clientMock);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AliyunsmsDispatchJobNames.MODIFY_SMS_SIGN_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 7L);
		payload.put("sign_name", "StoreSign");
		payload.put("sign_source", 1);
		payload.put("remark", "r");
		payload.put("third_party", true);
		payload.put("qualification_id", "42");
		payload.put("sign_file", "/data/x.png");
		payload.put("delegate_file", null);
		payload.put("status", 0);

		facade.dispatchJob(
				AliyunsmsDispatchJobNames.MODIFY_SMS_SIGN_JOB,
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
		assertEquals(AliyunsmsDispatchJobNames.MODIFY_SMS_SIGN_JOB, msg.messageName());
		assertEquals(7L, msg.payload().get("company_id"));
		assertEquals(0, assertNumber(msg.payload().get("status")).intValue());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		InOrder order = inOrder(imageMock, clientMock);
		order.verify(imageMock).enrichModifyParams(eq(7L), anyMap());
		order.verify(clientMock).updateSmsSign(eq(7L), eq("StoreSign"), eq(1), eq("r"), eq(true), eq("42"), eq(enriched));
	}

	@Test
	void dispatchJob_async_payloadMatchesModifySignEnvelope_includingStatusZero() {
		AliyunsmsUpdateSmsSignClient clientMock = mock(AliyunsmsUpdateSmsSignClient.class);
		AliyunsmsAddSmsSignImageSupport imageMock = mock(AliyunsmsAddSmsSignImageSupport.class);
		Map<String, Object> enriched = new LinkedHashMap<>();
		enriched.put("sign_file", Map.of("fileContents", "QQ==", "fileSuffix", "png"));
		when(imageMock.enrichModifyParams(anyLong(), anyMap())).thenReturn(enriched);

		ModifySmsSignJobHandler handler = new ModifySmsSignJobHandler(imageMock, clientMock);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AliyunsmsDispatchJobNames.MODIFY_SMS_SIGN_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		// Mirrors AliyunsmsModifySmsSignJobDispatchPublisherImpl#publish payload and matching DispatchOptions.
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 99L);
		payload.put("sign_name", "AlignedSignName");
		payload.put("sign_source", 2);
		payload.put("remark", "note");
		payload.put("third_party", false);
		payload.put("qualification_id", "qid");
		payload.put("sign_file", "/tmp/a.png");
		payload.put("delegate_file", null);
		payload.put("status", Integer.valueOf(0));

		facade.dispatchJob(
				AliyunsmsDispatchJobNames.MODIFY_SMS_SIGN_JOB,
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
		assertEquals(AliyunsmsDispatchJobNames.MODIFY_SMS_SIGN_JOB, msg.messageName());
		Map<String, Object> p = msg.payload();
		assertEquals(99L, p.get("company_id"));
		assertEquals("AlignedSignName", p.get("sign_name"));
		assertEquals(2, assertNumber(p.get("sign_source")).intValue());
		assertEquals("note", p.get("remark"));
		assertEquals(false, p.get("third_party"));
		assertEquals("qid", p.get("qualification_id"));
		assertEquals("/tmp/a.png", p.get("sign_file"));
		assertEquals(null, p.get("delegate_file"));
		assertInstanceOf(Number.class, p.get("status"));
		assertEquals(0, assertNumber(p.get("status")).intValue());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		InOrder order = inOrder(imageMock, clientMock);
		order.verify(imageMock).enrichModifyParams(eq(99L), anyMap());
		order.verify(clientMock)
				.updateSmsSign(eq(99L), eq("AlignedSignName"), eq(2), eq("note"), eq(false), eq("qid"), eq(enriched));
	}

	private static Number assertNumber(Object v) {
		assertInstanceOf(Number.class, v);
		return (Number) v;
	}

	@Test
	void runtimeConsume_handCraftedModifySmsSignJob_invokesHandlerWithoutDispatchFacade() {
		AliyunsmsUpdateSmsSignClient clientMock = mock(AliyunsmsUpdateSmsSignClient.class);
		AliyunsmsAddSmsSignImageSupport imageMock = mock(AliyunsmsAddSmsSignImageSupport.class);
		Map<String, Object> enriched = new LinkedHashMap<>();
		enriched.put("sign_file", Map.of("fileContents", "QQ==", "fileSuffix", "png"));
		when(imageMock.enrichModifyParams(anyLong(), anyMap())).thenReturn(enriched);

		ModifySmsSignJobHandler handler = new ModifySmsSignJobHandler(imageMock, clientMock);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AliyunsmsDispatchJobNames.MODIFY_SMS_SIGN_JOB, handler);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 7L);
		payload.put("sign_name", "StoreSign");
		payload.put("sign_source", 1);
		payload.put("remark", "r");
		payload.put("third_party", true);
		payload.put("qualification_id", "42");
		payload.put("sign_file", "/data/x.png");
		payload.put("delegate_file", null);
		payload.put("status", Integer.valueOf(0));

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						AliyunsmsDispatchJobNames.MODIFY_SMS_SIGN_JOB,
						payload,
						"sms",
						null,
						RetryPolicy.platformDefault(),
						Instant.now(),
						"trace-entry-02-consumer",
						null);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		InOrder order = inOrder(imageMock, clientMock);
		order.verify(imageMock).enrichModifyParams(eq(7L), anyMap());
		order.verify(clientMock).updateSmsSign(eq(7L), eq("StoreSign"), eq(1), eq("r"), eq(true), eq("42"), eq(enriched));
	}
}
