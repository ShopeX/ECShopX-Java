package cn.shopex.ecshopx.youshu.dispatch;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.KaquanDispatchEventNames;
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
import cn.shopex.ecshopx.dispatch.ListenerDispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import cn.shopex.ecshopx.youshu.service.YoushuCouponAddSrDataSyncService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouponAddYoushuDispatchListenerDispatchConsumerRuntimeTest {

	@Mock
	private YoushuCouponAddSrDataSyncService youshuCouponAddSrDataSyncService;

	private DispatchConsumerRuntime runtime;

	@BeforeEach
	void setUp() {
		YoushuCouponAddDispatchListener listener = new YoushuCouponAddDispatchListener(youshuCouponAddSrDataSyncService);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		var options = ListenerDispatchOptions.async("default", null);
		registry.registerEventListener(
				KaquanDispatchEventNames.EVENT_COUPON_ADD,
				KaquanDispatchEventNames.LISTENER_YOUSHU_BUNDLE_COUPON,
				options,
				listener);
		registry.registerEventListener(
				KaquanDispatchEventNames.EVENT_COUPON_ADD_NEW_GIFT,
				KaquanDispatchEventNames.LISTENER_YOUSHU_BUNDLE_COUPON,
				options,
				listener);
		runtime =
				new DispatchConsumerRuntime(
						registry,
						mock(DispatchRetryDecider.class),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
	}

	@Test
	@DisplayName("EVENT_COUPON_ADD: youshu Coupon listener — DispatchConsumerRuntime consume")
	void consume_event_message_invokes_sync_service_once() {
		long cardId = 12L;
		long companyId = 34L;
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("card_id", cardId);
		payload.put("company_id", companyId);

		DispatchMessage message =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						KaquanDispatchEventNames.EVENT_COUPON_ADD,
						payload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T12:00:00Z"),
						"trace-coupon-add-youshu",
						KaquanDispatchEventNames.LISTENER_YOUSHU_BUNDLE_COUPON);

		runtime.consume(message, 1);

		verify(youshuCouponAddSrDataSyncService, times(1)).syncAfterCouponAdd(eq(cardId), eq(companyId));
	}

	@Test
	@DisplayName("EVENT_COUPON_ADD_NEW_GIFT: same Youshu Coupon listener consumes event:284 fan-out message")
	void consume_event284_message_invokes_sync_service_once() {
		long cardId = 77L;
		long companyId = 88L;
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("card_id", cardId);
		payload.put("company_id", companyId);

		DispatchMessage message =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						KaquanDispatchEventNames.EVENT_COUPON_ADD_NEW_GIFT,
						payload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T12:00:00Z"),
						"trace-coupon-add-youshu-284",
						KaquanDispatchEventNames.LISTENER_YOUSHU_BUNDLE_COUPON);

		runtime.consume(message, 1);

		verify(youshuCouponAddSrDataSyncService, times(1)).syncAfterCouponAdd(eq(cardId), eq(companyId));
	}
}
