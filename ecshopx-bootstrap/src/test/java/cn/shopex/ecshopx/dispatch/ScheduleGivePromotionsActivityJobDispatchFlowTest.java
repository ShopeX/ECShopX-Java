package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.PromotionsDispatchJobNames;
import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountReceiveCardService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.promotions.dispatch.ScheduleGivePromotionsActivityJobHandler;
import cn.shopex.ecshopx.promotions.mapper.CouponGiveErrorLogMapper;
import cn.shopex.ecshopx.promotions.mapper.CouponGiveLogMapper;
import cn.shopex.ecshopx.promotions.service.give.PromotionActivityGiveAsyncService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("entry02-queue-consumer-parity")
class ScheduleGivePromotionsActivityJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andHandlerDelegatesToGiveService() {
		PromotionActivityGiveAsyncService real =
				new PromotionActivityGiveAsyncService(
						mock(CouponGiveLogMapper.class),
						mock(CouponGiveErrorLogMapper.class),
						mock(UserDiscountReceiveCardService.class),
						mock(MemberAccountService.class));
		PromotionActivityGiveAsyncService spySvc = spy(real);
		doNothing()
				.when(spySvc)
				.executeScheduleGive(
						anyLong(), anyLong(), anyString(), anyList(), anyList(), anyString(), anyLong());

		ScheduleGivePromotionsActivityJobHandler handler = new ScheduleGivePromotionsActivityJobHandler(spySvc);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(PromotionsDispatchJobNames.SCHEDULE_GIVE_PROMOTIONS_ACTIVITY, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long triggerTime = 1_700_000_000L;
		facade.dispatchJob(
				PromotionsDispatchJobNames.SCHEDULE_GIVE_PROMOTIONS_ACTIVITY,
				Map.of(
						"company_id",
						10L,
						"distributor_id",
						2L,
						"sender",
						"staff",
						"users",
						List.of(100L, 101L),
						"coupons",
						List.of(500L),
						"source_from",
						"admin",
						"trigger_time",
						triggerTime),
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("slow", msg.queue());
		assertEquals(PromotionsDispatchJobNames.SCHEDULE_GIVE_PROMOTIONS_ACTIVITY, msg.messageName());

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(spySvc)
				.executeScheduleGive(
						eq(10L),
						eq(2L),
						eq("staff"),
						argThat(u -> u.equals(List.of(100L, 101L))),
						argThat(c -> c.equals(List.of(500L))),
						eq("admin"),
						eq(triggerTime));
	}

	@Test
	void consume_whenDownstreamFails_propagatesForRetryDecider() {
		PromotionActivityGiveAsyncService mockSvc = mock(PromotionActivityGiveAsyncService.class);
		doThrow(new RuntimeException("downstream"))
				.when(mockSvc)
				.executeScheduleGive(
						anyLong(), anyLong(), anyString(), anyList(), anyList(), anyString(), anyLong());

		ScheduleGivePromotionsActivityJobHandler handler = new ScheduleGivePromotionsActivityJobHandler(mockSvc);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(PromotionsDispatchJobNames.SCHEDULE_GIVE_PROMOTIONS_ACTIVITY, handler);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						PromotionsDispatchJobNames.SCHEDULE_GIVE_PROMOTIONS_ACTIVITY,
						new LinkedHashMap<>(
								Map.of(
										"company_id",
										1L,
										"distributor_id",
										0L,
										"sender",
										"x",
										"users",
										List.of(9L),
										"coupons",
										List.of(8L),
										"source_from",
										"src",
										"trigger_time",
										42L)),
						"slow",
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
		assertThrows(RuntimeException.class, () -> runtime.consume(msg, 1));

		verify(mockSvc)
				.executeScheduleGive(
						eq(1L),
						eq(0L),
						eq("x"),
						argThat(u -> u.equals(List.of(9L))),
						argThat(c -> c.equals(List.of(8L))),
						eq("src"),
						eq(42L));
	}
}
