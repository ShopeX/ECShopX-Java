package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.SelfserviceDispatchJobNames;
import cn.shopex.ecshopx.selfservice.dispatch.SendWxRemindJobHandler;
import cn.shopex.ecshopx.selfservice.integration.SendRegistrationWxRemindJobHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SendWxRemindJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesRegistrationHandler() {
		SendRegistrationWxRemindJobHandler delegateMock = mock(SendRegistrationWxRemindJobHandler.class);
		SendWxRemindJobHandler handler = new SendWxRemindJobHandler(delegateMock);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(SelfserviceDispatchJobNames.SEND_WX_REMIND, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		facade.dispatchJob(
				SelfserviceDispatchJobNames.SEND_WX_REMIND,
				Map.of("activity_id", 40L),
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("slow", msg.queue());
		assertEquals(SelfserviceDispatchJobNames.SEND_WX_REMIND, msg.messageName());
		assertEquals(40L, msg.payload().get("activity_id"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(delegateMock)
				.handle(argThat(m -> m != null && m.getActivityId() == 40L));
	}
}
