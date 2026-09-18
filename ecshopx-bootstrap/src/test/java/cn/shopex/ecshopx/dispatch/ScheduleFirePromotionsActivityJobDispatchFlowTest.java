package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.PromotionsDispatchJobNames;
import cn.shopex.ecshopx.promotions.dispatch.ScheduleFirePromotionsActivityJobHandler;
import cn.shopex.ecshopx.promotions.schedule.ScheduleFirePromotionActivityConsumer;
import cn.shopex.ecshopx.promotions.schedule.ScheduleFirePromotionActivityMessage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ScheduleFirePromotionsActivityJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnDefaultQueue_andConsumerInvokesScheduleFireLogic() {
		ScheduleFirePromotionActivityConsumer consumer = mock(ScheduleFirePromotionActivityConsumer.class);
		ScheduleFirePromotionsActivityJobHandler handler = new ScheduleFirePromotionsActivityJobHandler(consumer);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(PromotionsDispatchJobNames.SCHEDULE_FIRE_PROMOTIONS_ACTIVITY, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> activityInfo = new LinkedHashMap<>();
		activityInfo.put("activity_id", 200L);
		activityInfo.put("company_id", 10L);
		activityInfo.put("name", "summer");

		facade.dispatchJob(
				PromotionsDispatchJobNames.SCHEDULE_FIRE_PROMOTIONS_ACTIVITY,
				Map.of(
						"activity_type",
						"seckill",
						"activity_info",
						activityInfo,
						"trigger_time",
						1_700_000_000L,
						"page_size",
						100,
						"page",
						2),
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"default",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("default", msg.queue());
		assertEquals(PromotionsDispatchJobNames.SCHEDULE_FIRE_PROMOTIONS_ACTIVITY, msg.messageName());

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(consumer)
				.handle(
						argThat(
								m ->
										"seckill".equals(m.activityType())
												&& m.triggerTime() == 1_700_000_000L
												&& m.pageSize() == 100
												&& m.page() == 2
												&& Long.valueOf(200L).equals(toLong(m.activityInfo().get("activity_id")))
												&& Long.valueOf(10L).equals(toLong(m.activityInfo().get("company_id")))
												&& "summer".equals(m.activityInfo().get("name"))));
	}

	private static Long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(o));
	}

	@Test
	void handler_propagatesConsumerBehavior_consumeAcks() {
		ScheduleFirePromotionActivityConsumer consumer = mock(ScheduleFirePromotionActivityConsumer.class);
		doThrow(new RuntimeException("downstream")).when(consumer).handle(any(ScheduleFirePromotionActivityMessage.class));
		ScheduleFirePromotionsActivityJobHandler handler = new ScheduleFirePromotionsActivityJobHandler(consumer);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(PromotionsDispatchJobNames.SCHEDULE_FIRE_PROMOTIONS_ACTIVITY, handler);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						PromotionsDispatchJobNames.SCHEDULE_FIRE_PROMOTIONS_ACTIVITY,
						Map.of(
								"activity_type",
								"x",
								"activity_info",
								Map.of("k", "v"),
								"trigger_time",
								1L,
								"page_size",
								50,
								"page",
								1),
						"default",
						null,
						RetryPolicy.platformDefault(),
						Instant.now(),
						"trace-test",
						null);

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(consumer)
				.handle(
						argThat(
								m ->
										"x".equals(m.activityType())
												&& m.triggerTime() == 1L
												&& m.pageSize() == 50
												&& m.page() == 1
												&& "v".equals(m.activityInfo().get("k"))));
	}
}
