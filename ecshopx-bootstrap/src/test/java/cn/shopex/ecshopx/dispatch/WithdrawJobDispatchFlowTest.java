package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.bspay.dispatch.WithdrawJobHandler;
import cn.shopex.ecshopx.bspay.service.WithdrawApplyService;
import cn.shopex.ecshopx.common.dispatch.BsPayBundleDispatchJobNames;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WithdrawJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnDefaultQueue_andConsumerInvokesExecuteHuifuWithdraw() {
		WithdrawApplyService withdrawApplyService = mock(WithdrawApplyService.class);
		WithdrawJobHandler handler = new WithdrawJobHandler(withdrawApplyService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(BsPayBundleDispatchJobNames.WITHDRAW_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("apply_id", 123L);
		facade.dispatchJob(
				BsPayBundleDispatchJobNames.WITHDRAW_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"default",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals("default", msg.queue());
		assertNull(msg.delay());
		assertEquals(BsPayBundleDispatchJobNames.WITHDRAW_JOB, msg.messageName());
		assertEquals(123L, ((Number) msg.payload().get("apply_id")).longValue());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(withdrawApplyService, times(1)).executeHuifuWithdraw(123L);
	}

	@Test
	void dispatchJob_payloadContainsApplyIdKey() {
		WithdrawApplyService withdrawApplyService = mock(WithdrawApplyService.class);
		WithdrawJobHandler handler = new WithdrawJobHandler(withdrawApplyService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(BsPayBundleDispatchJobNames.WITHDRAW_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("apply_id", 456L);
		facade.dispatchJob(
				BsPayBundleDispatchJobNames.WITHDRAW_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"default",
						null,
						RetryPolicy.platformDefault()));

		DispatchMessage msg = captured.get(0);
		assertTrue(msg.payload().containsKey("apply_id"));
		assertTrue(msg.payload().get("apply_id") instanceof Number);
		assertEquals(456L, ((Number) msg.payload().get("apply_id")).longValue());
	}
}
