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

@DisplayName("EVENT_ORDER_PROCESS_LOG: RefundJob/adapay async consume (slow)")
class AftersalesRefundJobAdapayOrderProcessLogEventAsyncConsumeTest {

	private static final String LISTENER_NAME = "listener:orders.listeners.OrderProcess.OrderProcessLog";

	@Test
	void consume_asyncSlowAdapayRefundSuccessShapedMessage_invokesOrderProcessLogBusServicePersistEntitiesMapOnce() {
		OrderProcessLogBusService bus = mock(OrderProcessLogBusService.class);
		OrderProcessLogDispatchListener listener = new OrderProcessLogDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG,
				LISTENER_NAME,
				ListenerDispatchOptions.async("slow", null),
				listener);

		long companyId = 9102L;
		long orderId = 5102L;

		LinkedHashMap<String, Object> expectedPayload = new LinkedHashMap<>();
		expectedPayload.put("order_id", orderId);
		expectedPayload.put("company_id", companyId);
		expectedPayload.put("operator_type", "system");
		expectedPayload.put("remarks", "订单退款");
		expectedPayload.put("detail", "订单号：" + orderId + "，订单退款成功（adapay支付渠道）");

		Instant occurredAt = Instant.now();
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
						"trace-entry-01-qc-refundjob-adapay-opl",
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

	@Test
	void consume_asyncSlowAdapayRefundFailShapedMessage_invokesOrderProcessLogBusServicePersistEntitiesMapOnce() {
		OrderProcessLogBusService bus = mock(OrderProcessLogBusService.class);
		OrderProcessLogDispatchListener listener = new OrderProcessLogDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG,
				LISTENER_NAME,
				ListenerDispatchOptions.async("slow", null),
				listener);

		long companyId = 9103L;
		long orderId = 5103L;
		String err = "AdaPay refund declined";

		LinkedHashMap<String, Object> expectedPayload = new LinkedHashMap<>();
		expectedPayload.put("order_id", orderId);
		expectedPayload.put("company_id", companyId);
		expectedPayload.put("operator_type", "system");
		expectedPayload.put("remarks", "订单退款");
		expectedPayload.put(
				"detail", "订单号：" + orderId + "，订单退款失败（adapay支付渠道），失败原因：" + err);

		Instant occurredAt = Instant.now();
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
						"trace-entry-01-qc-refundjob-adapay-opl-fail",
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
