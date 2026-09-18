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
 * {@link OrdersDispatchEventNames#EVENT_ORDER_PROCESS_LOG} as admin aftersales apply (slow async): drives
 * {@link DispatchConsumerRuntime#consume} to {@link OrderProcessLogBusService#persistEntitiesMap}.
 */
@DisplayName("EVENT_ORDER_PROCESS_LOG: admin aftersales apply async consume (slow)")
class AftersalesAdminApplyOrderProcessLogEventAsyncConsumeTest {

	private static final String LISTENER_NAME = "listener:orders.listeners.OrderProcess.OrderProcessLog";

	@Test
	void consume_asyncSlowAdminApplyShapedMessage_invokesOrderProcessLogBusServicePersistEntitiesMapOnce() {
		OrderProcessLogBusService bus = mock(OrderProcessLogBusService.class);
		OrderProcessLogDispatchListener listener = new OrderProcessLogDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG,
				LISTENER_NAME,
				ListenerDispatchOptions.async("slow", null),
				listener);

		long orderId = 5001L;
		long companyId = 9001L;
		long aftersalesBn = 2026050915000001L;
		long operatorId = 700L;

		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("order_id", orderId);
		params.put("company_id", companyId);
		params.put("reason", "probe reason");
		params.put("operator_type", "admin");
		params.put("operator_id", operatorId);

		LinkedHashMap<String, Object> expectedPayload = new LinkedHashMap<>();
		expectedPayload.put("order_id", orderId);
		expectedPayload.put("company_id", companyId);
		expectedPayload.put("supplier_id", 1L);
		expectedPayload.put("operator_type", "admin");
		expectedPayload.put("operator_id", operatorId);
		expectedPayload.put("remarks", "订单售后");
		expectedPayload.put("detail", "售后单号：" + aftersalesBn + " 后台申请售后，申请原因：probe reason");
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
						"trace-entry-01-api-aftersales-apply-opl-151",
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
