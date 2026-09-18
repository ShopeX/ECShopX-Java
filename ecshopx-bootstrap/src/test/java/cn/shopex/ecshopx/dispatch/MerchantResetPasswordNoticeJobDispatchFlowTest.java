package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.MerchantDispatchJobNames;
import cn.shopex.ecshopx.promotions.dispatch.MerchantResetPasswordNoticeJobHandler;
import cn.shopex.ecshopx.promotions.service.SmsSendTestService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MerchantResetPasswordNoticeJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSmsQueue_andConsumerInvokesJobHandler() {
		SmsSendTestService sms = mock(SmsSendTestService.class);
		MerchantResetPasswordNoticeJobHandler handler = new MerchantResetPasswordNoticeJobHandler(sms);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(MerchantDispatchJobNames.MERCHANT_RESET_PASSWORD_NOTICE, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		facade.dispatchJob(
				MerchantDispatchJobNames.MERCHANT_RESET_PASSWORD_NOTICE,
				Map.of("company_id", 1L, "mobile", "13800138000", "password", "123456"),
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"sms",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("sms", msg.queue());
		assertEquals(MerchantDispatchJobNames.MERCHANT_RESET_PASSWORD_NOTICE, msg.messageName());
		assertEquals(1L, msg.payload().get("company_id"));
		assertEquals("13800138000", msg.payload().get("mobile"));
		assertEquals("123456", msg.payload().get("password"));

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(sms)
				.sendTemplatedNoticeSms(
						eq(1L),
						eq("13800138000"),
						eq("merchant_reset_password_notice"),
						argThat(m -> m != null && "123456".equals(m.get("password"))));
	}

	@Test
	void handler_logsSmsFailureWithoutRethrowing_soConsumeAcks() {
		SmsSendTestService sms = mock(SmsSendTestService.class);
		doThrow(new RuntimeException("gateway down"))
				.when(sms)
				.sendTemplatedNoticeSms(anyLong(), anyString(), anyString(), any());
		MerchantResetPasswordNoticeJobHandler handler = new MerchantResetPasswordNoticeJobHandler(sms);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(MerchantDispatchJobNames.MERCHANT_RESET_PASSWORD_NOTICE, handler);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						MerchantDispatchJobNames.MERCHANT_RESET_PASSWORD_NOTICE,
						Map.of("company_id", 9L, "mobile", "13900000000", "password", "654321"),
						"sms",
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

		verify(sms).sendTemplatedNoticeSms(anyLong(), anyString(), anyString(), any());
	}
}
