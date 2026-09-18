package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.KaquanDispatchEventNames;
import cn.shopex.ecshopx.config.CouponDeleteEventDispatchPublisherImpl;
import cn.shopex.ecshopx.youshu.dispatch.YoushuCouponDeleteDispatchListener;
import cn.shopex.ecshopx.youshu.service.YoushuCouponDeleteSrDataSyncService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CouponDeleteEvent287AsyncBusDispatchFlowTest {

	@Test
	void publishCouponDelete_event287_async_enqueuesOneListenerTask_andConsumerRuns() {
		YoushuCouponDeleteSrDataSyncService youshuSvc = mock(YoushuCouponDeleteSrDataSyncService.class);
		YoushuCouponDeleteDispatchListener youshuListener = new YoushuCouponDeleteDispatchListener(youshuSvc);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				KaquanDispatchEventNames.EVENT_COUPON_DELETE,
				KaquanDispatchEventNames.LISTENER_YOUSHU_BUNDLE_COUPON,
				ListenerDispatchOptions.async("default", null),
				youshuListener);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		CouponDeleteEventDispatchPublisherImpl publisher = new CouponDeleteEventDispatchPublisherImpl(facade);

		long cardId = 55L;
		long companyId = 100L;
		publisher.publish(cardId, companyId);

		assertEquals(1, captured.size());
		assertEquals(KaquanDispatchEventNames.EVENT_COUPON_DELETE, captured.get(0).messageName());
		assertEquals(KaquanDispatchEventNames.LISTENER_YOUSHU_BUNDLE_COUPON, captured.get(0).listenerName());
		assertEquals("default", captured.get(0).queue());
		assertNotNull(captured.get(0).occurredAt());
		assertFalse(captured.get(0).traceId().isBlank());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						mock(DispatchRetryDecider.class),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(captured.get(0), 1);

		verify(youshuSvc).syncAfterCouponDelete(eq(cardId), eq(companyId));
	}
}
