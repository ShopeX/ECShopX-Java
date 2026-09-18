package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.KaquanDispatchEventNames;
import cn.shopex.ecshopx.youshu.dispatch.YoushuCouponAddDispatchListener;
import cn.shopex.ecshopx.youshu.service.YoushuCouponAddSrDataSyncService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CouponAddDmTemplateAsyncBusDispatchFlowTest {

	@Test
	@DisplayName("CouponAdd fan-out: Redis-enqueued listener task consumed by DispatchConsumerRuntime invokes youshu sync")
	void publishCouponAdd_fromFanOut_enqueuesListenerTask_andRuntimeConsumeInvokesYoushuSync() {
		YoushuCouponAddSrDataSyncService youshuSvc = mock(YoushuCouponAddSrDataSyncService.class);
		YoushuCouponAddDispatchListener youshuListener = new YoushuCouponAddDispatchListener(youshuSvc);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				KaquanDispatchEventNames.EVENT_COUPON_ADD,
				KaquanDispatchEventNames.LISTENER_YOUSHU_BUNDLE_COUPON,
				ListenerDispatchOptions.async("default", null),
				youshuListener);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long cardId = 56L;
		long companyId = 101L;
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("card_id", cardId);
		payload.put("company_id", companyId);

		facade.publishEvent(
				KaquanDispatchEventNames.EVENT_COUPON_ADD,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		assertEquals(KaquanDispatchEventNames.LISTENER_YOUSHU_BUNDLE_COUPON, captured.get(0).listenerName());
		assertEquals("default", captured.get(0).queue());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						mock(DispatchRetryDecider.class),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(captured.get(0), 1);

		verify(youshuSvc).syncAfterCouponAdd(eq(cardId), eq(companyId));
	}
}
