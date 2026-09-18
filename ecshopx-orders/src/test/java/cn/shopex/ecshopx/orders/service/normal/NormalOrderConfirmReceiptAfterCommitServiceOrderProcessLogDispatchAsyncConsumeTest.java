package cn.shopex.ecshopx.orders.service.normal;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("EVENT_ORDER_PROCESS_LOG: confirmReceipt — DispatchConsumerRuntime consume (slow)")
class NormalOrderConfirmReceiptAfterCommitServiceOrderProcessLogDispatchAsyncConsumeTest {

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
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
	}

	@Test
	void consume_confirmReceiptAdminOrderCompleteShapedAsyncMessage_invokesPersistEntitiesMapOnce() {
		long companyId = 100L;
		long orderIdNum = 200L;
		long operatorId = 42L;

		LinkedHashMap<String, Object> expectedPayload = new LinkedHashMap<>();
		expectedPayload.put("order_id", orderIdNum);
		expectedPayload.put("company_id", companyId);
		expectedPayload.put("operator_type", "admin");
		expectedPayload.put("operator_id", operatorId);
		expectedPayload.put("remarks", "订单完成");
		expectedPayload.put("detail", "订单号：" + orderIdNum + "，订单完成");
		expectedPayload.put("params", Map.of());

		Instant occurredAt = Instant.parse("2026-05-10T12:00:00Z");
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
						"trace-confirm-receipt-admin-opl-async-consume",
						LISTENER_NAME);

		runtime.consume(msg, 1);

		verify(orderProcessLogBusService, times(1)).persistEntitiesMap(eq(expectedPayload));
	}

	/**
	 * Covers OrderRefundCompleteJob-driven confirm-receipt path: user-side order process log fan-out
	 * shape (operator_type=user, detail prefix 订单单号：).
	 */
	@Test
	void consume_confirmReceiptWxappUserOrderCompleteShapedAsyncMessage_invokesPersistEntitiesMapOnce() {
		long companyId = 100L;
		long orderIdNum = 200L;
		long operatorId = 99L;

		LinkedHashMap<String, Object> expectedPayload = new LinkedHashMap<>();
		expectedPayload.put("order_id", orderIdNum);
		expectedPayload.put("company_id", companyId);
		expectedPayload.put("operator_type", "user");
		expectedPayload.put("operator_id", operatorId);
		expectedPayload.put("remarks", "订单完成");
		expectedPayload.put("detail", "订单单号：" + orderIdNum + "，订单完成");
		expectedPayload.put("params", Map.of());

		Instant occurredAt = Instant.parse("2026-05-10T12:00:00Z");
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
						"trace-confirm-receipt-wxapp-user-opl-async-consume",
						LISTENER_NAME);

		runtime.consume(msg, 1);

		verify(orderProcessLogBusService, times(1)).persistEntitiesMap(eq(expectedPayload));
	}
}
