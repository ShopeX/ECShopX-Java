package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aliyunsms.dispatch.AddSmsTemplateJobHandler;
import cn.shopex.ecshopx.aliyunsms.integration.AliyunsmsCreateSmsTemplateClient;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsDispatchJobNames;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AddSmsTemplateJobDispatchFlowTest {

	@Test
	void dispatchJob_sync_executesHandlerAndCreateSmsTemplateClient() {
		AliyunsmsCreateSmsTemplateClient clientMock = mock(AliyunsmsCreateSmsTemplateClient.class);
		when(clientMock.createSmsTemplate(
						eq(7L), eq(1), eq("T1"), eq("r"), eq("body"), eq(3), eq("SignA")))
				.thenReturn("SMS_123456");
		AddSmsTemplateJobHandler handler = new AddSmsTemplateJobHandler(clientMock);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AliyunsmsDispatchJobNames.ADD_SMS_TEMPLATE_JOB, handler);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 7L);
		payload.put("template_type", 1);
		payload.put("template_name", "T1");
		payload.put("remark", "r");
		payload.put("template_content", "body");
		payload.put("scene_id", 3);
		payload.put("related_sign_name", "SignA");

		facade.dispatchJob(
				AliyunsmsDispatchJobNames.ADD_SMS_TEMPLATE_JOB,
				payload,
				new DispatchOptions(DispatchMode.SYNC, null, null, null, RetryPolicy.platformDefault()));

		verify(clientMock).createSmsTemplate(eq(7L), eq(1), eq("T1"), eq("r"), eq("body"), eq(3), eq("SignA"));

		DispatchMessage last = facade.lastPublishedMessage();
		assertEquals(DispatchMode.SYNC, last.dispatchMode());
		assertEquals(DispatchDriverType.SYNC, last.driverType());
		assertEquals(AliyunsmsDispatchJobNames.ADD_SMS_TEMPLATE_JOB, last.messageName());
		assertNull(last.queue());
	}

	@Test
	void runtimeConsume_handCraftedAddSmsTemplateJob_invokesCreateSmsTemplateClient() {
		AliyunsmsCreateSmsTemplateClient clientMock = mock(AliyunsmsCreateSmsTemplateClient.class);
		AddSmsTemplateJobHandler handler = new AddSmsTemplateJobHandler(clientMock);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AliyunsmsDispatchJobNames.ADD_SMS_TEMPLATE_JOB, handler);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 9L);
		payload.put("template_type", 0);
		payload.put("template_name", "RuntimeT");
		payload.put("remark", "x");
		payload.put("template_content", "c");
		payload.put("scene_id", 2);
		payload.put("related_sign_name", "SignB");

		when(clientMock.createSmsTemplate(
						eq(9L), eq(0), eq("RuntimeT"), eq("x"), eq("c"), eq(2), eq("SignB")))
				.thenReturn("SMS_RUNTIME");

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.SYNC,
						DispatchDriverType.SYNC,
						AliyunsmsDispatchJobNames.ADD_SMS_TEMPLATE_JOB,
						payload,
						null,
						null,
						RetryPolicy.platformDefault(),
						Instant.now(),
						"trace-add-sms-template-runtime",
						null);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(clientMock).createSmsTemplate(eq(9L), eq(0), eq("RuntimeT"), eq("x"), eq("c"), eq(2), eq("SignB"));
	}
}
