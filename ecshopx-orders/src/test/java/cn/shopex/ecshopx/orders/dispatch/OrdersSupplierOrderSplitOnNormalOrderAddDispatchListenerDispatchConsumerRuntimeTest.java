package cn.shopex.ecshopx.orders.dispatch;

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
import cn.shopex.ecshopx.supplier.service.SupplierOrderSplitOnNormalOrderAddService;
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
class OrdersSupplierOrderSplitOnNormalOrderAddDispatchListenerDispatchConsumerRuntimeTest {

	private static final String LISTENER_NAME = "listener:orders.supplier_order_split_on_normal_order_add";

	@Mock
	private SupplierOrderSplitOnNormalOrderAddService supplierOrderSplitOnNormalOrderAddService;

	private DispatchConsumerRuntime runtime;

	@BeforeEach
	void setUp() {
		OrdersSupplierOrderSplitOnNormalOrderAddDispatchListener listener =
				new OrdersSupplierOrderSplitOnNormalOrderAddDispatchListener(supplierOrderSplitOnNormalOrderAddService);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_ADD,
				LISTENER_NAME,
				ListenerDispatchOptions.syncDefaults(),
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
	@DisplayName("EVENT_NORMAL_ORDER_ADD: supplier split listener — DispatchConsumerRuntime consume")
	void consume_event_message_invokes_split_service_once() {
		long companyId = 7L;
		long orderId = 99L;
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("order_id", orderId);
		payload.put("pay_type", "wxpay");

		DispatchMessage message =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.SYNC,
						DispatchDriverType.SYNC,
						OrdersDispatchEventNames.EVENT_NORMAL_ORDER_ADD,
						payload,
						null,
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T12:00:00Z"),
						"trace-normal-order-add-supplier-split",
						LISTENER_NAME);

		runtime.consume(message, 1);

		verify(supplierOrderSplitOnNormalOrderAddService, times(1)).split(eq(companyId), eq(orderId));
	}
}
