package cn.shopex.ecshopx.orders.service.front.wxapp;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName(
		"EVENT_ORDER_PROCESS_LOG: wxapp confirm delivery packaged — DispatchConsumerRuntime consume (slow, system/0)")
class WxappConfirmDeliveryPackagOrderProcessLogDispatchAsyncConsumeTest {

	private static final String LISTENER_NAME = "listener:orders.listeners.OrderProcess.OrderProcessLog";

	@Mock
	private OrderProcessLogBusService orderProcessLogBusService;

	private DispatchConsumerRuntime runtime;

	@BeforeEach
	void setUp() {
		OrderProcessLogDispatchListener listener =
				new OrderProcessLogDispatchListener(orderProcessLogBusService);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG,
				LISTENER_NAME,
				ListenerDispatchOptions.async("slow", null),
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
	void consume_wxappPackagedConfirmShapedAsyncMessage_invokesPersistEntitiesMapOnce() {
		long companyId = 100L;
		long orderId = 200L;
		long operatorId = 0L;

		LinkedHashMap<String, Object> innerParams = new LinkedHashMap<>();
		innerParams.put("order_id", orderId);
		innerParams.put("company_id", companyId);
		innerParams.put("operator_id", operatorId);
		innerParams.put("operator_type", "system");
		innerParams.put("self_delivery_status", "PACKAGED");

		LinkedHashMap<String, Object> expectedPayload = new LinkedHashMap<>();
		expectedPayload.put("order_id", orderId);
		expectedPayload.put("company_id", companyId);
		expectedPayload.put("operator_type", "system");
		expectedPayload.put("operator_id", operatorId);
		expectedPayload.put("is_show", Boolean.TRUE);
		expectedPayload.put("remarks", "商家已打包");
		expectedPayload.put("detail", "订单号：200，已打包");
		expectedPayload.put("params", new LinkedHashMap<>(innerParams));

		Instant occurredAt = Instant.parse("2026-05-10T08:00:00Z");
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
						"trace-wxapp-confirm-delivery-packaged-opl-async-consume",
						LISTENER_NAME);

		runtime.consume(msg, 1);

		verify(orderProcessLogBusService, times(1)).persistEntitiesMap(eq(expectedPayload));
	}
}
