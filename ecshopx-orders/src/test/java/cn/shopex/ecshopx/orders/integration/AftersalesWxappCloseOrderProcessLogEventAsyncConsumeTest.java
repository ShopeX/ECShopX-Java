package cn.shopex.ecshopx.orders.integration;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
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
import cn.shopex.ecshopx.orders.dispatch.OrderProcessLogDispatchListener;
import cn.shopex.ecshopx.orders.service.orderlog.OrderProcessLogBusService;
import java.time.Instant;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link OrdersDispatchEventNames#EVENT_ORDER_PROCESS_LOG} shaped as wxapp consumer aftersales close: drives
 * {@link DispatchConsumerRuntime#consume} to reach {@link OrderProcessLogBusService}.
 */
@DisplayName("EVENT_ORDER_PROCESS_LOG: wxapp aftersales close async consume (slow)")
class AftersalesWxappCloseOrderProcessLogEventAsyncConsumeTest {

	private static final String LISTENER_NAME = "listener:orders.listeners.OrderProcess.OrderProcessLog";

	@Test
	void consume_asyncSlowWxappCloseShapedMessage_invokesOrderProcessLogBusServicePersistEntitiesMapOnce() {
		OrderProcessLogBusService bus = mock(OrderProcessLogBusService.class);
		OrderProcessLogDispatchListener listener = new OrderProcessLogDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG,
				LISTENER_NAME,
				ListenerDispatchOptions.async("slow", null),
				listener);

		long aftersalesBn = 2026050912222222L;
		long orderId = 5102L;
		long companyId = 9102L;
		long userId = 88001L;

		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("aftersales_bn", aftersalesBn);
		params.put("company_id", companyId);
		params.put("user_id", userId);
		params.put("memo", "消费者主动关闭售后");

		LinkedHashMap<String, Object> expectedPayload = new LinkedHashMap<>();
		expectedPayload.put("order_id", orderId);
		expectedPayload.put("company_id", companyId);
		expectedPayload.put("operator_type", "user");
		expectedPayload.put("operator_id", userId);
		expectedPayload.put("remarks", "订单售后");
		expectedPayload.put("detail", "售后单号：" + aftersalesBn + "，消费者主动关闭售后");
		expectedPayload.put("params", params);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG,
						expectedPayload,
						"slow",
						null,
						RetryPolicy.platformDefault(),
						Instant.now(),
						"trace-entry-01-wxapp-close-opl",
						LISTENER_NAME);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(bus, times(1)).persistEntitiesMap(eq(expectedPayload));
	}
}
