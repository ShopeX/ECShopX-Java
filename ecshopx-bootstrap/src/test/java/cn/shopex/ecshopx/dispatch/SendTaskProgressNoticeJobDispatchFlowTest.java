package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.WorkWechatDispatchJobNames;
import cn.shopex.ecshopx.salesperson.dispatch.SendTaskProgressNoticeJobHandler;
import cn.shopex.ecshopx.salesperson.service.SalespersonTaskWorkWechatNoticeService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SendTaskProgressNoticeJobDispatchFlowTest {

	@Test
	@DisplayName("Share success path dispatches async to slow queue and consumer invokes work wechat notice")
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesSendTaskProgressNotice() {
		SalespersonTaskWorkWechatNoticeService noticeService = mock(SalespersonTaskWorkWechatNoticeService.class);
		SendTaskProgressNoticeJobHandler handler = new SendTaskProgressNoticeJobHandler(noticeService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(WorkWechatDispatchJobNames.SEND_TASK_PROGRESS_NOTICE_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new SyncDispatchDriver(registry),
						Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 11L;
		long taskId = 501L;
		long salespersonId = 9001L;
		String username = "昵称";
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("task_id", taskId);
		payload.put("salesperson_id", salespersonId);
		payload.put("username", username);
		facade.dispatchJob(
				WorkWechatDispatchJobNames.SEND_TASK_PROGRESS_NOTICE_JOB,
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
		assertEquals(WorkWechatDispatchJobNames.SEND_TASK_PROGRESS_NOTICE_JOB, msg.messageName());
		assertNull(msg.listenerName());
		assertEquals(companyId, msg.payload().get("company_id"));
		assertEquals(taskId, msg.payload().get("task_id"));
		assertEquals(salespersonId, msg.payload().get("salesperson_id"));
		assertEquals(username, msg.payload().get("username"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(noticeService).sendTaskProgressNotice(eq(companyId), eq(taskId), eq(salespersonId), eq(username));
	}
}
