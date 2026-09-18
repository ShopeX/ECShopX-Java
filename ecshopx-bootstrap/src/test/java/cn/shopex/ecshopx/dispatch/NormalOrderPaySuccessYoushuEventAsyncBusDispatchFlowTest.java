package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.youshu.dispatch.NormalOrderPaySuccessYoushuDispatchListener;
import cn.shopex.ecshopx.youshu.service.YoushuNormalOrderPaySuccessSrDataSyncService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NormalOrderPaySuccessYoushuEventAsyncBusDispatchFlowTest {

	@Test
	void publishNormalOrderPaySuccess_async_enqueuesOneListenerTask_andConsumerRunsYoushu() {
		YoushuNormalOrderPaySuccessSrDataSyncService youshuSvc = mock(YoushuNormalOrderPaySuccessSrDataSyncService.class);
		NormalOrderPaySuccessYoushuDispatchListener youshuListener =
				new NormalOrderPaySuccessYoushuDispatchListener(youshuSvc);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_PAY_SUCCESS,
				OrdersDispatchEventNames.LISTENER_YOUSHU_ORDERS_NORMAL_ORDER_PAY_SUCCESS,
				ListenerDispatchOptions.async("default", null),
				youshuListener);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 7L);
		payload.put("order_id", 55L);
		payload.put("pay_type", "alipay");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_PAY_SUCCESS,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		assertEquals(OrdersDispatchEventNames.EVENT_NORMAL_ORDER_PAY_SUCCESS, captured.get(0).messageName());
		assertEquals(OrdersDispatchEventNames.LISTENER_YOUSHU_ORDERS_NORMAL_ORDER_PAY_SUCCESS, captured.get(0).listenerName());
		assertEquals("default", captured.get(0).queue());
		assertEquals(DispatchMode.ASYNC, captured.get(0).dispatchMode());
		assertEquals(DispatchDriverType.REDIS, captured.get(0).driverType());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(captured.get(0), 1);

		verify(youshuSvc).syncOrderAfterNormalPaySuccess(7L, 55L);
	}
}
