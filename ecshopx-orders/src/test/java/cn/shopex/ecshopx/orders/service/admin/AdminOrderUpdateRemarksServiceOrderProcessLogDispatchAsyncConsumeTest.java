package cn.shopex.ecshopx.orders.service.admin;

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
@DisplayName("EVENT_ORDER_PROCESS_LOG: admin updateRemarks — DispatchConsumerRuntime consume (slow)")
class AdminOrderUpdateRemarksServiceOrderProcessLogDispatchAsyncConsumeTest {

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
	void consume_updateRemarksNonDistributionShapedAsyncMessage_invokesPersistEntitiesMapOnce() {
		long companyId = 100L;
		long orderId = 200L;
		long operatorId = 42L;
		String pathRaw = "200";

		LinkedHashMap<String, Object> innerParams = new LinkedHashMap<>();
		innerParams.put("remark", "hello");
		innerParams.put("company_id", companyId);
		innerParams.put("operator_type", "admin");
		innerParams.put("operator_id", operatorId);

		LinkedHashMap<String, Object> expectedPayload = new LinkedHashMap<>();
		expectedPayload.put("order_id", orderId);
		expectedPayload.put("company_id", companyId);
		expectedPayload.put("operator_type", "admin");
		expectedPayload.put("operator_id", operatorId);
		expectedPayload.put("remarks", "订单备注");
		expectedPayload.put("detail", "订单号：" + pathRaw + "，订单备注修改");
		expectedPayload.put("params", Map.copyOf(new LinkedHashMap<>(innerParams)));

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
						"trace-admin-update-remarks-opl-async-consume-nondist",
						LISTENER_NAME);

		runtime.consume(msg, 1);

		verify(orderProcessLogBusService, times(1)).persistEntitiesMap(eq(expectedPayload));
	}

	@Test
	void consume_updateRemarksDistributionShapedAsyncMessage_invokesPersistEntitiesMapOnce() {
		long companyId = 100L;
		long orderId = 200L;
		long operatorId = 42L;
		String pathRaw = "200";

		LinkedHashMap<String, Object> innerParams = new LinkedHashMap<>();
		innerParams.put("remark", "");
		innerParams.put("is_distribution", Boolean.TRUE);
		innerParams.put("company_id", companyId);
		innerParams.put("operator_type", "admin");
		innerParams.put("operator_id", operatorId);

		LinkedHashMap<String, Object> expectedPayload = new LinkedHashMap<>();
		expectedPayload.put("order_id", orderId);
		expectedPayload.put("company_id", companyId);
		expectedPayload.put("operator_type", "admin");
		expectedPayload.put("operator_id", operatorId);
		expectedPayload.put("remarks", "订单备注");
		expectedPayload.put("detail", "订单号：" + pathRaw + "，订单商家备注修改");
		expectedPayload.put("params", Map.copyOf(new LinkedHashMap<>(innerParams)));

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
						"trace-admin-update-remarks-opl-async-consume-dist",
						LISTENER_NAME);

		runtime.consume(msg, 1);

		verify(orderProcessLogBusService, times(1)).persistEntitiesMap(eq(expectedPayload));
	}
}
