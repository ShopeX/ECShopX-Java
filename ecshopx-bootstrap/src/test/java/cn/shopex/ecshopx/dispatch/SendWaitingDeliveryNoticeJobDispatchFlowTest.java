package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.WorkWechatDispatchJobNames;
import cn.shopex.ecshopx.workwechat.dispatch.SendWaitingDeliveryNoticeJobHandler;
import cn.shopex.ecshopx.workwechat.dispatch.SendWaitingDeliveryNoticeWorker;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SendWaitingDeliveryNoticeJobDispatchFlowTest {

	@Test
	@DisplayName(
			"Trade finish wxa waiting delivery: job 114 dispatches async to slow queue and consumer invokes worker")
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesWorker() {
		SendWaitingDeliveryNoticeWorker workerMock = mock(SendWaitingDeliveryNoticeWorker.class);
		SendWaitingDeliveryNoticeJobHandler handler = new SendWaitingDeliveryNoticeJobHandler(workerMock);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(WorkWechatDispatchJobNames.SEND_WAITING_DELIVERY_NOTICE_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new SyncDispatchDriver(registry),
						java.util.Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		String companyId = "10";
		String orderId = "202505069876543";
		String distributorId = "55";
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("order_id", orderId);
		payload.put("distributor_id", distributorId);
		facade.dispatchJob(
				WorkWechatDispatchJobNames.SEND_WAITING_DELIVERY_NOTICE_JOB,
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
		assertEquals(WorkWechatDispatchJobNames.SEND_WAITING_DELIVERY_NOTICE_JOB, msg.messageName());
		assertEquals(companyId, String.valueOf(msg.payload().get("company_id")));
		assertEquals(orderId, String.valueOf(msg.payload().get("order_id")));
		assertEquals(distributorId, String.valueOf(msg.payload().get("distributor_id")));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(workerMock).execute(eq(companyId), eq(orderId), eq(distributorId));
	}
}
