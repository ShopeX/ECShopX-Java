package cn.shopex.ecshopx.dispatch;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.orders.dispatch.OrderProcessLogDispatchListener;
import cn.shopex.ecshopx.orders.service.orderlog.OrderProcessLogBusService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EVENT_ORDER_PROCESS_LOG: OrderProcessLogListener async slow dispatch")
class SupplierOrderPaidConfirmOrderProcessLogPublishTest {

	private static final String LISTENER_NAME = "listener:orders.listeners.OrderProcess.OrderProcessLog";

	@Test
	void publishEvent_sync_invokesOrderProcessLogBusWithSupplierPayload() {
		OrderProcessLogBusService bus = mock(OrderProcessLogBusService.class);
		OrderProcessLogDispatchListener listener = new OrderProcessLogDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG,
				LISTENER_NAME,
				ListenerDispatchOptions.async("slow", null),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long orderId = 91001L;
		long companyId = 92002L;
		long supplierId = 93003L;
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("order_id", orderId);
		payload.put("company_id", companyId);
		payload.put("operator_type", "supplier");
		payload.put("operator_id", supplierId);
		payload.put("remarks", "确认收款");
		payload.put("detail", "订单号：" + orderId + " 确认收款");
		payload.put("params", Map.of());

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG, payload, DispatchOptions.eventDefaults());

		verify(bus, times(1))
				.persistEntitiesMap(
						argThat(
								m ->
										m != null
												&& orderId == toLong(m.get("order_id"))
												&& companyId == toLong(m.get("company_id"))
												&& "supplier".equals(m.get("operator_type"))));
	}

	@Test
	void consume_asyncSlowMessage_invokesSameListenerPath() {
		OrderProcessLogBusService bus = mock(OrderProcessLogBusService.class);
		OrderProcessLogDispatchListener listener = new OrderProcessLogDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG,
				LISTENER_NAME,
				ListenerDispatchOptions.async("slow", null),
				listener);

		long orderId = 44001L;
		long companyId = 44002L;
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("order_id", orderId);
		payload.put("company_id", companyId);
		payload.put("operator_type", "supplier");
		payload.put("operator_id", 55L);
		payload.put("remarks", "确认收款");
		payload.put("detail", "订单号：" + orderId + " 确认收款");
		payload.put("params", Map.of());

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG,
						payload,
						"slow",
						null,
						RetryPolicy.platformDefault(),
						Instant.now(),
						"trace-order-process-log",
						LISTENER_NAME);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(bus, times(1)).persistEntitiesMap(eq(payload));
	}

	@Test
	void partialCancelPayload_drainsSlowQueueAndPersists() {
		OrderProcessLogBusService bus = mock(OrderProcessLogBusService.class);
		OrderProcessLogDispatchListener listener = new OrderProcessLogDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG,
				LISTENER_NAME,
				ListenerDispatchOptions.async("slow", null),
				listener);

		long orderId = 55001L;
		long companyId = 55002L;
		long userId = 55003L;
		long supplierId = 55004L;
		long aftersalesBn = 202605088881L;
		String reasonText = "不想要了";

		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("order_id", orderId);
		params.put("company_id", companyId);
		params.put("user_id", userId);
		params.put("supplier_id", supplierId);
		params.put("operator_type", "user");
		params.put("operator_id", userId);
		params.put("reason", reasonText);
		params.put("is_partial_cancel", true);
		params.put("aftersales_bn", aftersalesBn);
		params.put("detail", List.of(Map.of("id", 1L, "num", 1)));

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("order_id", orderId);
		payload.put("company_id", companyId);
		payload.put("supplier_id", supplierId);
		payload.put("operator_type", "user");
		payload.put("operator_id", userId);
		payload.put("remarks", "订单售后");
		payload.put(
				"detail",
				"售后单号：" + aftersalesBn + " 申请售后，申请原因：" + reasonText);
		payload.put("params", params);
		payload.put("is_show", false);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG,
						payload,
						"slow",
						null,
						RetryPolicy.platformDefault(),
						Instant.now(),
						"trace-order-process-log-partial-cancel",
						LISTENER_NAME);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(bus, times(1)).persistEntitiesMap(eq(payload));
	}

	@Test
	void autoApproveOnlyRefundPayload_drainsSlowQueueAndPersists() {
		OrderProcessLogBusService bus = mock(OrderProcessLogBusService.class);
		OrderProcessLogDispatchListener listener = new OrderProcessLogDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG,
				LISTENER_NAME,
				ListenerDispatchOptions.async("slow", null),
				listener);

		long orderId = 66001L;
		long companyId = 66002L;
		long aftersalesBn = 202605099992L;
		long operatorId = 2L;

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("order_id", orderId);
		payload.put("company_id", companyId);
		payload.put("operator_type", "admin");
		payload.put("operator_id", operatorId);
		payload.put("remarks", "订单售后");
		payload.put("detail", "售后单号：" + aftersalesBn + "，同意退款");

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG,
						payload,
						"slow",
						null,
						RetryPolicy.platformDefault(),
						Instant.now(),
						"trace-order-process-log-auto-approve-only-refund",
						LISTENER_NAME);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(bus, times(1)).persistEntitiesMap(eq(payload));
	}

	@Test
	void publishEvent_sync_invokesOrderProcessLogBusWithWaitReturnGoodsDetail() {
		OrderProcessLogBusService bus = mock(OrderProcessLogBusService.class);
		OrderProcessLogDispatchListener listener = new OrderProcessLogDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG,
				LISTENER_NAME,
				ListenerDispatchOptions.async("slow", null),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long orderId = 91051L;
		long companyId = 91052L;
		long aftersalesBn = 20260509999101L;
		long operatorId = 3L;
		String detailExpected =
				"售后单号：" + aftersalesBn + "，售后单审核通过，等待商品回寄";
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("order_id", orderId);
		payload.put("company_id", companyId);
		payload.put("operator_type", "admin");
		payload.put("operator_id", operatorId);
		payload.put("remarks", "订单售后");
		payload.put("detail", detailExpected);
		payload.put("params", Map.of());

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG, payload, DispatchOptions.eventDefaults());

		verify(bus, times(1))
				.persistEntitiesMap(
						argThat(
								m ->
										m != null
												&& detailExpected.equals(String.valueOf(m.get("detail")))
												&& orderId == toLong(m.get("order_id"))
												&& companyId == toLong(m.get("company_id"))
												&& "admin".equals(m.get("operator_type"))));
	}

	@Test
	void consume_asyncSlowMessage_waitReturnGoodsDetail_invokesSameListenerPath() {
		OrderProcessLogBusService bus = mock(OrderProcessLogBusService.class);
		OrderProcessLogDispatchListener listener = new OrderProcessLogDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG,
				LISTENER_NAME,
				ListenerDispatchOptions.async("slow", null),
				listener);

		long orderId = 77001L;
		long companyId = 77002L;
		long aftersalesBn = 20260509999202L;
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("order_id", orderId);
		payload.put("company_id", companyId);
		payload.put("operator_type", "admin");
		payload.put("operator_id", 4L);
		payload.put("remarks", "订单售后");
		payload.put("detail", "售后单号：" + aftersalesBn + "，售后单审核通过，等待商品回寄");

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG,
						payload,
						"slow",
						null,
						RetryPolicy.platformDefault(),
						Instant.now(),
						"trace-order-process-log-wait-return-goods",
						LISTENER_NAME);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(bus, times(1)).persistEntitiesMap(eq(payload));
	}

	@Test
	void publishEvent_sync_invokesOrderProcessLogBusWithRefundRejectDetail() {
		OrderProcessLogBusService bus = mock(OrderProcessLogBusService.class);
		OrderProcessLogDispatchListener listener = new OrderProcessLogDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG,
				LISTENER_NAME,
				ListenerDispatchOptions.async("slow", null),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long orderId = 88001L;
		long companyId = 88002L;
		long aftersalesBn = 20260508888333L;
		String memo = "不符合退换规则";
		String detailExpected =
				"售后单号：" + aftersalesBn + " 拒绝退款，拒绝退款原因：" + memo;

		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("aftersales_bn", aftersalesBn);
		params.put("check_refund", false);
		params.put("refunds_memo", memo);

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("order_id", orderId);
		payload.put("company_id", companyId);
		payload.put("operator_type", "user");
		payload.put("operator_id", 17L);
		payload.put("remarks", "订单售后");
		payload.put("detail", detailExpected);
		payload.put("params", params);

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG, payload, DispatchOptions.eventDefaults());

		verify(bus, times(1))
				.persistEntitiesMap(
						argThat(
								m ->
										m != null
												&& detailExpected.equals(String.valueOf(m.get("detail")))
												&& "user".equals(m.get("operator_type"))
												&& orderId == toLong(m.get("order_id"))
												&& companyId == toLong(m.get("company_id"))
												&& "订单售后".equals(m.get("remarks"))));
	}

	@Test
	void consume_asyncSlowMessage_refundRejectDetail_invokesSameListenerPath() {
		OrderProcessLogBusService bus = mock(OrderProcessLogBusService.class);
		OrderProcessLogDispatchListener listener = new OrderProcessLogDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG,
				LISTENER_NAME,
				ListenerDispatchOptions.async("slow", null),
				listener);

		long orderId = 88051L;
		long companyId = 88052L;
		long aftersalesBn = 20260508888444L;
		String memo = "用户超时未寄回";
		String detailExpected =
				"售后单号：" + aftersalesBn + " 拒绝退款，拒绝退款原因：" + memo;

		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", companyId);
		params.put("aftersales_bn", aftersalesBn);
		params.put("refunds_memo", memo);

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("order_id", orderId);
		payload.put("company_id", companyId);
		payload.put("operator_type", "user");
		payload.put("operator_id", 0L);
		payload.put("remarks", "订单售后");
		payload.put("detail", detailExpected);
		payload.put("params", params);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG,
						payload,
						"slow",
						null,
						RetryPolicy.platformDefault(),
						Instant.now(),
						"trace-order-process-log-refund-reject",
						LISTENER_NAME);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(bus, times(1)).persistEntitiesMap(eq(payload));
	}

	@Test
	void publishEvent_sync_invokesOrderProcessLogBusWithRefundAgreeDetail() {
		OrderProcessLogBusService bus = mock(OrderProcessLogBusService.class);
		OrderProcessLogDispatchListener listener = new OrderProcessLogDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG,
				LISTENER_NAME,
				ListenerDispatchOptions.async("slow", null),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long orderId = 89001L;
		long companyId = 89002L;
		long aftersalesBn = 20260508888555L;
		long operatorId = 19L;
		String detailExpected = "售后单号：" + aftersalesBn + "，售后单同意退款";

		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", companyId);
		params.put("aftersales_bn", aftersalesBn);
		params.put("check_refund", true);
		params.put("refund_fee", 100);
		params.put("refund_point", 0);
		params.put("operator_id", operatorId);

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("order_id", orderId);
		payload.put("company_id", companyId);
		payload.put("operator_type", "admin");
		payload.put("operator_id", operatorId);
		payload.put("remarks", "订单售后");
		payload.put("detail", detailExpected);
		payload.put("params", params);

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG, payload, DispatchOptions.eventDefaults());

		verify(bus, times(1))
				.persistEntitiesMap(
						argThat(
								m ->
										m != null
												&& detailExpected.equals(String.valueOf(m.get("detail")))
												&& "admin".equals(m.get("operator_type"))
												&& orderId == toLong(m.get("order_id"))
												&& companyId == toLong(m.get("company_id"))
												&& "订单售后".equals(m.get("remarks"))));
	}

	@Test
	void consume_asyncSlowMessage_refundAgreeDetail_invokesSameListenerPath() {
		OrderProcessLogBusService bus = mock(OrderProcessLogBusService.class);
		OrderProcessLogDispatchListener listener = new OrderProcessLogDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG,
				LISTENER_NAME,
				ListenerDispatchOptions.async("slow", null),
				listener);

		long orderId = 89051L;
		long companyId = 89052L;
		long aftersalesBn = 20260508888666L;
		long operatorId = 29L;
		String detailExpected = "售后单号：" + aftersalesBn + "，售后单同意退款";

		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", companyId);
		params.put("aftersales_bn", aftersalesBn);
		params.put("check_refund", true);
		params.put("refund_fee", 80);
		params.put("refund_point", 0);
		params.put("operator_id", operatorId);

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("order_id", orderId);
		payload.put("company_id", companyId);
		payload.put("operator_type", "admin");
		payload.put("operator_id", operatorId);
		payload.put("remarks", "订单售后");
		payload.put("detail", detailExpected);
		payload.put("params", params);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG,
						payload,
						"slow",
						null,
						RetryPolicy.platformDefault(),
						Instant.now(),
						"trace-order-process-log-refund-agree",
						LISTENER_NAME);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(bus, times(1)).persistEntitiesMap(eq(payload));
	}

	@Test
	void wxappSendbackPayload_consume_slowQueue_invokesPersistEntitiesMap() {
		OrderProcessLogBusService bus = mock(OrderProcessLogBusService.class);
		OrderProcessLogDispatchListener listener = new OrderProcessLogDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG,
				LISTENER_NAME,
				ListenerDispatchOptions.async("slow", null),
				listener);

		long orderId = 99501L;
		long companyId = 99502L;
		long userId = 99503L;
		long aftersalesBn = 20260510151515L;

		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("aftersales_bn", aftersalesBn);
		params.put("company_id", companyId);
		params.put("user_id", userId);
		params.put("corp_code", "SF");
		params.put("logi_no", "1234567890");

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("order_id", orderId);
		payload.put("company_id", companyId);
		payload.put("operator_type", "user");
		payload.put("operator_id", userId);
		payload.put("remarks", "订单售后");
		payload.put("detail", "售后单号：" + aftersalesBn + "，售后单寄回商品");
		payload.put("params", params);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG,
						payload,
						"slow",
						null,
						RetryPolicy.platformDefault(),
						Instant.now(),
						"trace-wxapp-sendback-order-process-log",
						LISTENER_NAME);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(bus, times(1)).persistEntitiesMap(eq(payload));
	}

	private static long toLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}
}
