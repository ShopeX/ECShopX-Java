package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.WorkWechatDispatchJobNames;
import cn.shopex.ecshopx.workwechat.dispatch.SendDeliveryWaitDeliveryNoticeJobHandler;
import cn.shopex.ecshopx.workwechat.dispatch.SendDeliveryWaitDeliveryNoticeWorker;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SendDeliveryWaitDeliveryNoticeJobDispatchFlowTest {

	@Test
	@DisplayName(
			"Trade finish work wechat: delivery wait notice dispatches async to slow queue and consumer invokes worker")
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesWorker() {
		SendDeliveryWaitDeliveryNoticeWorker workerMock = mock(SendDeliveryWaitDeliveryNoticeWorker.class);
		SendDeliveryWaitDeliveryNoticeJobHandler handler = new SendDeliveryWaitDeliveryNoticeJobHandler(workerMock);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(WorkWechatDispatchJobNames.SEND_DELIVERY_WAIT_DELIVERY_NOTICE_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new SyncDispatchDriver(registry),
						java.util.Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		String companyId = "10";
		String orderId = "202505069876543";
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("order_id", orderId);
		facade.dispatchJob(
				WorkWechatDispatchJobNames.SEND_DELIVERY_WAIT_DELIVERY_NOTICE_JOB,
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
		assertEquals(WorkWechatDispatchJobNames.SEND_DELIVERY_WAIT_DELIVERY_NOTICE_JOB, msg.messageName());
		assertEquals(companyId, String.valueOf(msg.payload().get("company_id")));
		assertEquals(orderId, String.valueOf(msg.payload().get("order_id")));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(workerMock).execute(eq(companyId), eq(orderId));
	}
}
