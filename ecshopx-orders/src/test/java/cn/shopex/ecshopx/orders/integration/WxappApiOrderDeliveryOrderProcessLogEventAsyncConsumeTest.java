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
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 使用与 wxapp 发货形态一致的 {@code DispatchMessage}，经 {@link DispatchConsumerRuntime#consume} 驱动
 * {@link OrderProcessLogDispatchListener}；与
 * {@link cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDeliveryCoreServiceWxappApiOrderDeliveryOrderProcessLogDispatchPublishProbeTest}
 * 互补覆盖投递与出队侧。
 */
@DisplayName("EVENT_ORDER_PROCESS_LOG: wxapp Api Order#delivery async consume (slow)")
class WxappApiOrderDeliveryOrderProcessLogEventAsyncConsumeTest {

	private static final String LISTENER_NAME = "listener:orders.listeners.OrderProcess.OrderProcessLog";

	@Test
	void consume_asyncSlowWxappDeliveryShapedMessage_invokesOrderProcessLogBusServicePersistEntitiesMapOnce() {
		OrderProcessLogBusService bus = mock(OrderProcessLogBusService.class);
		OrderProcessLogDispatchListener listener = new OrderProcessLogDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG,
				LISTENER_NAME,
				ListenerDispatchOptions.async("slow", null),
				listener);

		long companyId = 100L;
		long orderId = 200L;

		Map<String, Object> innerParams = new LinkedHashMap<>();
		innerParams.put("company_id", companyId);
		innerParams.put("order_id", orderId);
		innerParams.put("supplier_id", 0);
		innerParams.put("delivery_type", "batch");
		innerParams.put("delivery_corp", "SF");
		innerParams.put("delivery_code", "SFTRACK001");
		innerParams.put("operator_type", "user");
		innerParams.put("operator_id", 55L);
		innerParams.put("user_id", 55L);

		LinkedHashMap<String, Object> expectedPayload = new LinkedHashMap<>();
		expectedPayload.put("order_id", orderId);
		expectedPayload.put("company_id", companyId);
		expectedPayload.put("supplier_id", 0);
		expectedPayload.put("operator_type", "user");
		expectedPayload.put("operator_id", 55L);
		expectedPayload.put("is_show", true);
		expectedPayload.put("remarks", "订单发货");
		expectedPayload.put("detail", "订单号：" + orderId + "，订单发货");
		expectedPayload.put("delivery_remark", "");
		expectedPayload.put("pics", null);
		expectedPayload.put("params", innerParams);

		Instant occurredAt = Instant.parse("2026-05-09T12:00:00Z");
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
						occurredAt,
						"trace-wxapp-api-order-delivery-opl-async-consume",
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
