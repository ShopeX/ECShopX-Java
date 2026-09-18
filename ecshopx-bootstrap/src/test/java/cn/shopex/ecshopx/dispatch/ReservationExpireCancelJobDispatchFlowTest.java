package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.ReservationDispatchJobNames;
import cn.shopex.ecshopx.reservation.dispatch.ReservationExpireCancelJobHandler;
import cn.shopex.ecshopx.reservation.service.ReservationAutoExpireCancelService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReservationExpireCancelJobDispatchFlowTest {

	@Test
	void dispatchJob_job71_delayed_enqueuesDefaultQueueWithDelay_andHandlerInvokesExpireCancelService() {
		ReservationAutoExpireCancelService expireCancelService = mock(ReservationAutoExpireCancelService.class);
		ReservationExpireCancelJobHandler handler = new ReservationExpireCancelJobHandler(expireCancelService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(ReservationDispatchJobNames.RESERVATION_EXPIRE_CANCEL_DELAYED, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long epochSec = 1_714_521_600L;
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);
		payload.put("record_id", 100L);
		payload.put("to_shop_time", epochSec);

		Duration delay = Duration.ofSeconds(3600);
		facade.dispatchJob(
				ReservationDispatchJobNames.RESERVATION_EXPIRE_CANCEL_DELAYED,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"default",
						delay,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("default", msg.queue());
		assertEquals(delay, msg.delay());
		assertEquals(ReservationDispatchJobNames.RESERVATION_EXPIRE_CANCEL_DELAYED, msg.messageName());

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(expireCancelService).applyCancelOrNotToShopIfSuccess(eq(1L), eq(100L));
	}

	@Test
	void dispatchJob_job71_zeroDelay_stillEnqueuesAsync_andHandlerRuns() {
		ReservationAutoExpireCancelService expireCancelService = mock(ReservationAutoExpireCancelService.class);
		ReservationExpireCancelJobHandler handler = new ReservationExpireCancelJobHandler(expireCancelService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(ReservationDispatchJobNames.RESERVATION_EXPIRE_CANCEL_DELAYED, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 9L);
		payload.put("record_id", 88L);
		payload.put("to_shop_time", 1_714_521_600);

		facade.dispatchJob(
				ReservationDispatchJobNames.RESERVATION_EXPIRE_CANCEL_DELAYED,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"default",
						Duration.ZERO,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(Duration.ZERO, msg.delay());

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(expireCancelService).applyCancelOrNotToShopIfSuccess(eq(9L), eq(88L));
	}
}
