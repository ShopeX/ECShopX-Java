package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.AftersalesDispatchJobNames;
import cn.shopex.ecshopx.workwechat.dispatch.SendAfterSaleCancelNoticeJobHandler;
import cn.shopex.ecshopx.workwechat.dispatch.SendAfterSaleCancelNoticeWorker;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Covers the same dispatch job stack used after scheduled auto-close ({@link cn.shopex.ecshopx.aftersales.cron.ScheduleDoneAftersalesHandler}) and HTTP close. */
class SendAfterSaleCancelNoticeJobDispatchFlowTest {

	@Test
	@DisplayName("Post-commit cancel notice dispatches async to slow queue and consumer invokes worker")
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesWorker() {
		SendAfterSaleCancelNoticeWorker workerMock = mock(SendAfterSaleCancelNoticeWorker.class);
		SendAfterSaleCancelNoticeJobHandler handler = new SendAfterSaleCancelNoticeJobHandler(workerMock);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AftersalesDispatchJobNames.SEND_AFTER_SALE_CANCEL_NOTICE_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new SyncDispatchDriver(registry),
						Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 10L;
		long aftersalesBn = 202505069876543L;
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("aftersales_bn", aftersalesBn);
		facade.dispatchJob(
				AftersalesDispatchJobNames.SEND_AFTER_SALE_CANCEL_NOTICE_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("slow", msg.queue());
		assertNull(msg.delay());
		assertEquals(AftersalesDispatchJobNames.SEND_AFTER_SALE_CANCEL_NOTICE_JOB, msg.messageName());
		assertNull(msg.listenerName());
		assertEquals(companyId, msg.payload().get("company_id"));
		assertEquals(aftersalesBn, msg.payload().get("aftersales_bn"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(workerMock).execute(eq(companyId), eq(aftersalesBn));
	}
}
