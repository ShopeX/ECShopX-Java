package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.aliyunsms.dispatch.AddSmsSignJobHandler;
import cn.shopex.ecshopx.aliyunsms.integration.AliyunsmsCreateSmsSignClient;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsDispatchJobNames;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AddSmsSignJobDispatchFlowTest {

	@Test
	void dispatchJob_sync_executesHandlerAndCreateSmsSignClient() {
		AliyunsmsCreateSmsSignClient clientMock = mock(AliyunsmsCreateSmsSignClient.class);
		AddSmsSignJobHandler handler = new AddSmsSignJobHandler(clientMock);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AliyunsmsDispatchJobNames.ADD_SMS_SIGN_JOB, handler);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 7L);
		payload.put("sign_name", "StoreSign");
		payload.put("sign_source", 1);
		payload.put("remark", "r");
		payload.put("third_party", true);
		payload.put("qualification_id", "42");

		facade.dispatchJob(
				AliyunsmsDispatchJobNames.ADD_SMS_SIGN_JOB,
				payload,
				new DispatchOptions(DispatchMode.SYNC, null, null, null, RetryPolicy.platformDefault()));

		verify(clientMock).createSmsSign(eq(7L), eq("StoreSign"), eq(1), eq("r"), eq(true), eq("42"));

		DispatchMessage last = facade.lastPublishedMessage();
		assertEquals(DispatchMode.SYNC, last.dispatchMode());
		assertEquals(DispatchDriverType.SYNC, last.driverType());
		assertEquals(AliyunsmsDispatchJobNames.ADD_SMS_SIGN_JOB, last.messageName());
		assertNull(last.queue());
	}

	@Test
	void runtimeConsume_handCraftedAddSmsSignJob_invokesCreateSmsSignClient() {
		AliyunsmsCreateSmsSignClient clientMock = mock(AliyunsmsCreateSmsSignClient.class);
		AddSmsSignJobHandler handler = new AddSmsSignJobHandler(clientMock);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AliyunsmsDispatchJobNames.ADD_SMS_SIGN_JOB, handler);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 9L);
		payload.put("sign_name", "RuntimeSign");
		payload.put("sign_source", 2);
		payload.put("remark", "x");
		payload.put("third_party", false);
		payload.put("qualification_id", "99");

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.SYNC,
						DispatchDriverType.SYNC,
						AliyunsmsDispatchJobNames.ADD_SMS_SIGN_JOB,
						payload,
						null,
						null,
						RetryPolicy.platformDefault(),
						Instant.now(),
						"trace-add-sms-sign-runtime",
						null);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(clientMock).createSmsSign(eq(9L), eq("RuntimeSign"), eq(2), eq("x"), eq(false), eq("99"));
	}
}
