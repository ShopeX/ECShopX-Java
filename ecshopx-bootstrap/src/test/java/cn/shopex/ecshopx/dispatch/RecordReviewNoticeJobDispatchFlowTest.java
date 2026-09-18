package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.SelfserviceDispatchJobNames;
import cn.shopex.ecshopx.selfservice.dispatch.RecordReviewNoticeJobHandler;
import cn.shopex.ecshopx.selfservice.service.RegistrationRecordReviewNotifyService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RecordReviewNoticeJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSmsQueue_andConsumerInvokesNotifyService() {
		RegistrationRecordReviewNotifyService notifyMock = mock(RegistrationRecordReviewNotifyService.class);
		RecordReviewNoticeJobHandler handler = new RecordReviewNoticeJobHandler(notifyMock);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(SelfserviceDispatchJobNames.RECORD_REVIEW_NOTICE, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		facade.dispatchJob(
				SelfserviceDispatchJobNames.RECORD_REVIEW_NOTICE,
				Map.of("company_id", 1L, "record_id", 99L),
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"sms",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("sms", msg.queue());
		assertEquals(SelfserviceDispatchJobNames.RECORD_REVIEW_NOTICE, msg.messageName());
		assertEquals(1L, msg.payload().get("company_id"));
		assertEquals(99L, msg.payload().get("record_id"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(notifyMock).sendMassage(eq(1L), eq(99L));
	}
}
