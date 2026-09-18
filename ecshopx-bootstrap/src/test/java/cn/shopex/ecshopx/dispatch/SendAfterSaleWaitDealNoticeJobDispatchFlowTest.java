package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.AftersalesDispatchJobNames;
import cn.shopex.ecshopx.workwechat.dispatch.SendAfterSaleWaitDealNoticeJobHandler;
import cn.shopex.ecshopx.workwechat.dispatch.SendAfterSaleWaitDealNoticeWorker;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SendAfterSaleWaitDealNoticeJobDispatchFlowTest {

	@Test
	@DisplayName(
			"Front apply create-by-num: post-commit wait-deal notice dispatches async to slow queue and consumer invokes delegate")
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesDelegate() {
		SendAfterSaleWaitDealNoticeWorker delegateMock = mock(SendAfterSaleWaitDealNoticeWorker.class);
		SendAfterSaleWaitDealNoticeJobHandler handler = new SendAfterSaleWaitDealNoticeJobHandler(delegateMock);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AftersalesDispatchJobNames.SEND_AFTER_SALE_WAIT_DEAL_NOTICE, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new SyncDispatchDriver(registry),
						java.util.Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 10L);
		payload.put("aftersales_bn", 202505069876543L);
		facade.dispatchJob(
				AftersalesDispatchJobNames.SEND_AFTER_SALE_WAIT_DEAL_NOTICE,
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
		assertEquals(AftersalesDispatchJobNames.SEND_AFTER_SALE_WAIT_DEAL_NOTICE, msg.messageName());
		assertEquals(10L, msg.payload().get("company_id"));
		assertEquals(202505069876543L, msg.payload().get("aftersales_bn"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(delegateMock).execute(eq(10L), eq(202505069876543L));
	}

	@Test
	@DisplayName(
			"Create-by-num post-commit scope: partial-cancel path dispatches wait-deal notice async and consumer invokes delegate")
	void partialCancelOrder_aftersaleWaitDealNotice_dispatchJob_thenConsume_verifiesWorkerMock() {
		SendAfterSaleWaitDealNoticeWorker workerMock = mock(SendAfterSaleWaitDealNoticeWorker.class);
		SendAfterSaleWaitDealNoticeJobHandler handler = new SendAfterSaleWaitDealNoticeJobHandler(workerMock);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AftersalesDispatchJobNames.SEND_AFTER_SALE_WAIT_DEAL_NOTICE, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new SyncDispatchDriver(registry),
						java.util.Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 55L;
		long aftersalesBn = 202605061112233L;
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("aftersales_bn", aftersalesBn);
		facade.dispatchJob(
				AftersalesDispatchJobNames.SEND_AFTER_SALE_WAIT_DEAL_NOTICE,
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
		assertEquals(AftersalesDispatchJobNames.SEND_AFTER_SALE_WAIT_DEAL_NOTICE, msg.messageName());
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
