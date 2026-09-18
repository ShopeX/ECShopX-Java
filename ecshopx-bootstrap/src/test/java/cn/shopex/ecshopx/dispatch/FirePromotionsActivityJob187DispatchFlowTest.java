package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.PromotionsDispatchJobNames;
import cn.shopex.ecshopx.promotions.dispatch.FirePromotionsActivityJobHandler;
import cn.shopex.ecshopx.promotions.service.PromotionActivityFireService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FirePromotionsActivityJob187DispatchFlowTest {

	@Test
	void dispatchJob187_async_enqueuesDefaultQueue_andConsumerInvokesFireServiceWithMemberUpgrade() {
		PromotionActivityFireService fireService = mock(PromotionActivityFireService.class);
		FirePromotionsActivityJobHandler handler = new FirePromotionsActivityJobHandler(fireService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(PromotionsDispatchJobNames.FIRE_PROMOTIONS_ACTIVITY_JOB_187, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> memberInfo = new LinkedHashMap<>();
		memberInfo.put("grade_id", 10L);
		memberInfo.put("user_id", 50L);
		memberInfo.put("mobile", "13800138000");
		memberInfo.put("grade_name", "Gold");

		facade.dispatchJob(
				PromotionsDispatchJobNames.FIRE_PROMOTIONS_ACTIVITY_JOB_187,
				Map.of(
						"company_id",
						1L,
						"member_info",
						memberInfo,
						"activity_type",
						"member_upgrade"),
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"default",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("default", msg.queue());
		assertEquals(PromotionsDispatchJobNames.FIRE_PROMOTIONS_ACTIVITY_JOB_187, msg.messageName());

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(fireService)
				.fire(
						eq(1L),
						argThat(
								m ->
										Long.valueOf(10L).equals(toLong(m.get("grade_id")))
												&& Long.valueOf(50L).equals(toLong(m.get("user_id")))
												&& "13800138000".equals(m.get("mobile"))
												&& "Gold".equals(m.get("grade_name"))),
						eq("member_upgrade"));
	}

	@Test
	void handler187_logsFireFailureWithoutRethrowing_soConsumeAcks() {
		PromotionActivityFireService fireService = mock(PromotionActivityFireService.class);
		doThrow(new RuntimeException("downstream"))
				.when(fireService)
				.fire(anyLong(), anyMap(), anyString());
		FirePromotionsActivityJobHandler handler = new FirePromotionsActivityJobHandler(fireService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(PromotionsDispatchJobNames.FIRE_PROMOTIONS_ACTIVITY_JOB_187, handler);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						PromotionsDispatchJobNames.FIRE_PROMOTIONS_ACTIVITY_JOB_187,
						Map.of(
								"company_id",
								9L,
								"member_info",
								Map.of("user_id", 1L, "grade_id", 2L, "mobile", "", "grade_name", "S"),
								"activity_type",
								"member_upgrade"),
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

		verify(fireService).fire(anyLong(), anyMap(), anyString());
	}

	private static Long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(o));
	}
}
