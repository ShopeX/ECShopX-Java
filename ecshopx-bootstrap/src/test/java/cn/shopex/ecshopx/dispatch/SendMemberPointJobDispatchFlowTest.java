package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.PointBundleDispatchJobNames;
import cn.shopex.ecshopx.point.dispatch.SendMemberPointJobHandler;
import cn.shopex.ecshopx.point.service.PointMemberService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SendMemberPointJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesScheduleSendMemberPointBatch() {
		PointMemberService pointMemberService = mock(PointMemberService.class);
		SendMemberPointJobHandler handler = new SendMemberPointJobHandler(pointMemberService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(PointBundleDispatchJobNames.SEND_MEMBER_POINT_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		facade.dispatchJob(
				PointBundleDispatchJobNames.SEND_MEMBER_POINT_JOB,
				new LinkedHashMap<>(),
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
		assertEquals(PointBundleDispatchJobNames.SEND_MEMBER_POINT_JOB, msg.messageName());
		assertTrue(msg.payload().isEmpty());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(pointMemberService, times(1)).scheduleSendMemberPoint();
	}

	@Test
	void dispatchJob_publishPayloadIsEmptyMap() {
		PointMemberService pointMemberService = mock(PointMemberService.class);
		SendMemberPointJobHandler handler = new SendMemberPointJobHandler(pointMemberService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(PointBundleDispatchJobNames.SEND_MEMBER_POINT_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		facade.dispatchJob(
				PointBundleDispatchJobNames.SEND_MEMBER_POINT_JOB,
				new LinkedHashMap<>(),
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		DispatchMessage msg = captured.get(0);
		assertTrue(msg.payload().isEmpty());
		assertNull(msg.listenerName());
	}
}
