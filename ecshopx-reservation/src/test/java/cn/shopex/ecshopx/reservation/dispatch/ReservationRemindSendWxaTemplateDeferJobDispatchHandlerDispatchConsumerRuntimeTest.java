package cn.shopex.ecshopx.reservation.dispatch;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.ReservationDispatchJobNames;
import cn.shopex.ecshopx.dispatch.DispatchConsumerRuntime;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchMessage;
import cn.shopex.ecshopx.dispatch.DispatchMessageType;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchRetryDecider;
import cn.shopex.ecshopx.dispatch.DispatchStructuredLogger;
import cn.shopex.ecshopx.dispatch.FailedJobRecorder;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchConsumerStateRecorder;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchRegistry;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationRemindSendWxaTemplateDeferJobDispatchHandlerDispatchConsumerRuntimeTest {

	@Mock
	private ReservationRemindSendWxaTemplateDispatchService reservationRemindSendWxaTemplateDispatchService;

	private DispatchConsumerRuntime runtime;

	@BeforeEach
	void setUp() {
		ReservationRemindSendWxaTemplateDeferJobDispatchHandler handler =
				new ReservationRemindSendWxaTemplateDeferJobDispatchHandler(
						reservationRemindSendWxaTemplateDispatchService);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(
				ReservationDispatchJobNames.JOB_RESERVATION_REMIND_SEND_WXA_TEMPLATE_DEFER, handler);
		runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
	}

	@Test
	void consume_delegatesToServiceOnce() {
		Map<String, Object> payload = new LinkedHashMap<>();
		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("postdata", Map.of("company_id", 1L, "user_id", 2L));
		entities.put("result", Map.of("recordId", 9L));
		payload.put("entities", entities);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						ReservationDispatchJobNames.JOB_RESERVATION_REMIND_SEND_WXA_TEMPLATE_DEFER,
						payload,
						"default",
						Duration.ofSeconds(60),
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T12:00:00Z"),
						"trace-reservation-remind-defer",
						null);

		runtime.consume(msg, 1);

		verify(reservationRemindSendWxaTemplateDispatchService, times(1)).handle(payload);
	}
}
