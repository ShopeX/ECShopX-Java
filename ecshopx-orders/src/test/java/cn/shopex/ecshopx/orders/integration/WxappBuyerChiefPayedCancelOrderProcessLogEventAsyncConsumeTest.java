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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName(
		"EVENT_ORDER_PROCESS_LOG: wxapp buyer|chief PAYED cancel — DispatchConsumerRuntime consume (slow, payed-cancel payload)")
class WxappBuyerChiefPayedCancelOrderProcessLogEventAsyncConsumeTest {

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
	void consume_wxappBuyerPayedCancelShapedAsyncMessage_invokesOrderProcessLogBusServicePersistEntitiesMapOnce() {
		LinkedHashMap<String, Object> innerParams = new LinkedHashMap<>();
		innerParams.put("order_id", "200");
		innerParams.put("company_id", 1L);
		innerParams.put("cancel_from", "buyer");
		innerParams.put("cancel_reason", 1);
		innerParams.put("user_id", 100L);
		innerParams.put("mobile", "13800000000");

		LinkedHashMap<String, Object> expectedPayload = new LinkedHashMap<>();
		expectedPayload.put("order_id", 200L);
		expectedPayload.put("company_id", 1L);
		expectedPayload.put("supplier_id", 0L);
		expectedPayload.put("operator_type", "user");
		expectedPayload.put("operator_id", 100L);
		expectedPayload.put("remarks", "订单取消");
		expectedPayload.put("detail", "订单号：200，用户申请取消订单，需要进行退款操作");
		expectedPayload.put("is_show", Boolean.FALSE);
		expectedPayload.put("params", innerParams);

		Instant occurredAt = Instant.parse("2026-05-10T12:10:00Z");
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
						"trace-wxapp-buyer-payed-cancel-opl-async-consume",
						LISTENER_NAME);

		runtime.consume(msg, 1);

		verify(orderProcessLogBusService, times(1)).persistEntitiesMap(eq(expectedPayload));
	}

	@Test
	void consume_wxappChiefPayedCancelShapedAsyncMessage_invokesOrderProcessLogBusServicePersistEntitiesMapOnce() {
		LinkedHashMap<String, Object> innerParams = new LinkedHashMap<>();
		innerParams.put("order_id", "200");
		innerParams.put("company_id", 1L);
		innerParams.put("cancel_from", "chief");
		innerParams.put("cancel_reason", 1);
		innerParams.put("user_id", 100L);
		innerParams.put("chief_id", 777L);
		innerParams.put("mobile", "13800000000");

		LinkedHashMap<String, Object> expectedPayload = new LinkedHashMap<>();
		expectedPayload.put("order_id", 200L);
		expectedPayload.put("company_id", 1L);
		expectedPayload.put("supplier_id", 0L);
		expectedPayload.put("operator_type", "chief");
		expectedPayload.put("operator_id", 777L);
		expectedPayload.put("remarks", "订单取消");
		expectedPayload.put("detail", "订单号：200，团长取消订单");
		expectedPayload.put("is_show", Boolean.FALSE);
		expectedPayload.put("params", innerParams);

		Instant occurredAt = Instant.parse("2026-05-10T12:11:00Z");
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
						"trace-wxapp-chief-payed-cancel-opl-async-consume",
						LISTENER_NAME);

		runtime.consume(msg, 1);

		verify(orderProcessLogBusService, times(1)).persistEntitiesMap(eq(expectedPayload));
	}
}
