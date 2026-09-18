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

@DisplayName("EVENT_ORDER_PROCESS_LOG: admin offline do_check refuse async consume (slow)")
class OfflinePaymentDoCheckRefuseOrderProcessLogEventAsyncConsumeTest {

	private static final String LISTENER_NAME = "listener:orders.listeners.OrderProcess.OrderProcessLog";

	@Test
	void consume_asyncSlowAdminOfflineRefuseShapedMessage_invokesOrderProcessLogBusServicePersistEntitiesMapOnce() {
		OrderProcessLogBusService bus = mock(OrderProcessLogBusService.class);
		OrderProcessLogDispatchListener listener = new OrderProcessLogDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG,
				LISTENER_NAME,
				ListenerDispatchOptions.async("slow", null),
				listener);

		long companyId = 88L;
		long orderIdNum = 42L;
		long operatorId = 1L;
		long offlinePaymentId = 10L;
		String refuseRemark = "审核拒绝说明";
		String operatorName = "op";

		Map<String, Object> innerParams = new LinkedHashMap<>();
		innerParams.put("check_status", 2);
		innerParams.put("offline_payment_id", offlinePaymentId);
		innerParams.put("order_id", orderIdNum);
		innerParams.put("remark", refuseRemark);
		innerParams.put("operator_name", operatorName);

		LinkedHashMap<String, Object> expectedPayload = new LinkedHashMap<>();
		expectedPayload.put("order_id", orderIdNum);
		expectedPayload.put("company_id", companyId);
		expectedPayload.put("operator_type", "admin");
		expectedPayload.put("operator_id", operatorId);
		expectedPayload.put("remarks", "线下转账审核拒绝");
		expectedPayload.put("detail", "审核拒绝");
		expectedPayload.put("params", innerParams);
		expectedPayload.put("is_show", true);

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
						"trace-offline-docheck-refuse-opl-async-consume",
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
