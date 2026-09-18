package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.adapay.dispatch.DrawCashJobHandler;
import cn.shopex.ecshopx.adapay.service.AdapayDrawCashWithdrawService;
import cn.shopex.ecshopx.common.dispatch.AdaPayDispatchJobNames;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DrawCashJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesRunScheduledAutoDrawCash() {
		AdapayDrawCashWithdrawService withdrawService = mock(AdapayDrawCashWithdrawService.class);
		DrawCashJobHandler handler = new DrawCashJobHandler(withdrawService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AdaPayDispatchJobNames.DRAW_CASH_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 999L);
		payload.put("member_id", 7L);
		payload.put("settle_account_id", "sa-1");

		facade.dispatchJob(
				AdaPayDispatchJobNames.DRAW_CASH_JOB,
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
		assertEquals(AdaPayDispatchJobNames.DRAW_CASH_JOB, msg.messageName());
		assertTrue(msg.messageName().startsWith("job:109:"));
		assertEquals(999L, msg.payload().get("company_id"));
		assertEquals(7L, msg.payload().get("member_id"));
		assertEquals("sa-1", msg.payload().get("settle_account_id"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(withdrawService, times(1)).runScheduledAutoDrawCash(999L, 7L, "sa-1");
	}

	@Test
	void dispatchJob_payloadMainMerchantBranchUsesZeroAndEmptySettleId() {
		AdapayDrawCashWithdrawService withdrawService = mock(AdapayDrawCashWithdrawService.class);
		DrawCashJobHandler handler = new DrawCashJobHandler(withdrawService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AdaPayDispatchJobNames.DRAW_CASH_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 100L);
		payload.put("member_id", 0L);
		payload.put("settle_account_id", "");

		facade.dispatchJob(
				AdaPayDispatchJobNames.DRAW_CASH_JOB,
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
		assertEquals(AdaPayDispatchJobNames.DRAW_CASH_JOB, msg.messageName());
		assertTrue(msg.messageName().startsWith("job:109:"));
		assertEquals(100L, msg.payload().get("company_id"));
		assertEquals(0L, msg.payload().get("member_id"));
		assertEquals("", msg.payload().get("settle_account_id"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(withdrawService, times(1)).runScheduledAutoDrawCash(100L, 0L, "");
	}
}
