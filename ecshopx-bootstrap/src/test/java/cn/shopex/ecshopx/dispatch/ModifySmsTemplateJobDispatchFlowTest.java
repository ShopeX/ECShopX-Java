package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import cn.shopex.ecshopx.aliyunsms.dispatch.ModifySmsTemplateJobHandler;
import cn.shopex.ecshopx.aliyunsms.integration.AliyunsmsUpdateSmsTemplateClient;
import cn.shopex.ecshopx.aliyunsms.mapper.TemplateMapper;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsDispatchJobNames;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class ModifySmsTemplateJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSmsQueue_andConsumerInvokesUpdateTemplateClientThenStatusWriteback() {
		AliyunsmsUpdateSmsTemplateClient clientMock = mock(AliyunsmsUpdateSmsTemplateClient.class);
		TemplateMapper templateMapperMock = mock(TemplateMapper.class);

		ModifySmsTemplateJobHandler handler = new ModifySmsTemplateJobHandler(clientMock, templateMapperMock);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AliyunsmsDispatchJobNames.MODIFY_SMS_TEMPLATE_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 7L);
		payload.put("id", 501L);
		payload.put("status", 0);
		payload.put("template_name", "TName");
		payload.put("template_type", 1);
		payload.put("remark", "rk");
		payload.put("template_content", "body");
		payload.put("scene_id", 3);
		payload.put("related_sign_name", "SName");
		payload.put("template_code", "TCODE");

		facade.dispatchJob(
				AliyunsmsDispatchJobNames.MODIFY_SMS_TEMPLATE_JOB,
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
		assertEquals(AliyunsmsDispatchJobNames.MODIFY_SMS_TEMPLATE_JOB, msg.messageName());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		InOrder order = inOrder(clientMock, templateMapperMock);
		order.verify(clientMock)
				.updateSmsTemplate(eq(7L), eq(1), eq("TName"), eq("rk"), eq("body"), eq(3), eq("SName"), eq("TCODE"));
		order.verify(templateMapperMock).update(isNull(), any());
	}

	@Test
	void dispatchJob_async_payloadMatchesModifyTemplateEnvelope_includingStatusZero() {
		AliyunsmsUpdateSmsTemplateClient clientMock = mock(AliyunsmsUpdateSmsTemplateClient.class);
		TemplateMapper templateMapperMock = mock(TemplateMapper.class);

		ModifySmsTemplateJobHandler handler = new ModifySmsTemplateJobHandler(clientMock, templateMapperMock);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AliyunsmsDispatchJobNames.MODIFY_SMS_TEMPLATE_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		// Mirrors AliyunsmsModifySmsTemplateJobDispatchPublisherImpl#publish payload and matching DispatchOptions.
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 99L);
		payload.put("id", 1001L);
		payload.put("status", Integer.valueOf(0));
		payload.put("template_name", "AlignedTpl");
		payload.put("template_type", 2);
		payload.put("remark", "note");
		payload.put("template_content", "hello ${code}");
		payload.put("scene_id", 4);
		payload.put("related_sign_name", "SignX");
		payload.put("template_code", "SMS_001");

		facade.dispatchJob(
				AliyunsmsDispatchJobNames.MODIFY_SMS_TEMPLATE_JOB,
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
		assertEquals(AliyunsmsDispatchJobNames.MODIFY_SMS_TEMPLATE_JOB, msg.messageName());
		Map<String, Object> p = msg.payload();
		assertEquals(99L, p.get("company_id"));
		assertEquals(1001L, p.get("id"));
		assertInstanceOf(Number.class, p.get("status"));
		assertEquals(0, assertNumber(p.get("status")).intValue());
		assertEquals("AlignedTpl", p.get("template_name"));
		assertEquals(2, assertNumber(p.get("template_type")).intValue());
		assertEquals("note", p.get("remark"));
		assertEquals("hello ${code}", p.get("template_content"));
		assertEquals(4, assertNumber(p.get("scene_id")).intValue());
		assertEquals("SignX", p.get("related_sign_name"));
		assertEquals("SMS_001", p.get("template_code"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		InOrder order = inOrder(clientMock, templateMapperMock);
		order.verify(clientMock)
				.updateSmsTemplate(eq(99L), eq(2), eq("AlignedTpl"), eq("note"), eq("hello ${code}"), eq(4), eq("SignX"), eq("SMS_001"));
		order.verify(templateMapperMock).update(isNull(), any());
	}

	private static Number assertNumber(Object v) {
		assertInstanceOf(Number.class, v);
		return (Number) v;
	}

	@Test
	void runtimeConsume_handCraftedModifySmsTemplateJob_invokesHandlerWithoutDispatchFacade() {
		AliyunsmsUpdateSmsTemplateClient clientMock = mock(AliyunsmsUpdateSmsTemplateClient.class);
		TemplateMapper templateMapperMock = mock(TemplateMapper.class);

		ModifySmsTemplateJobHandler handler = new ModifySmsTemplateJobHandler(clientMock, templateMapperMock);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AliyunsmsDispatchJobNames.MODIFY_SMS_TEMPLATE_JOB, handler);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 7L);
		payload.put("id", 501L);
		payload.put("status", Integer.valueOf(0));
		payload.put("template_name", "TName");
		payload.put("template_type", 1);
		payload.put("remark", "rk");
		payload.put("template_content", "body");
		payload.put("scene_id", 3);
		payload.put("related_sign_name", "SName");
		payload.put("template_code", "TCODE");

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						AliyunsmsDispatchJobNames.MODIFY_SMS_TEMPLATE_JOB,
						payload,
						"sms",
						null,
						RetryPolicy.platformDefault(),
						Instant.now(),
						"trace-modify-sms-template-consumer",
						null);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		InOrder order = inOrder(clientMock, templateMapperMock);
		order.verify(clientMock)
				.updateSmsTemplate(eq(7L), eq(1), eq("TName"), eq("rk"), eq("body"), eq(3), eq("SName"), eq("TCODE"));
		order.verify(templateMapperMock).update(isNull(), any());
	}
}
