package cn.shopex.ecshopx.youshu.dispatch;

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
import cn.shopex.ecshopx.youshu.service.YoushuNormalOrderPaySuccessSrDataSyncService;
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
class NormalOrderPaySuccessYoushuDispatchListenerDispatchConsumerRuntimeTest {

	@Mock
	private YoushuNormalOrderPaySuccessSrDataSyncService youshuNormalOrderPaySuccessSrDataSyncService;

	private DispatchConsumerRuntime runtime;

	@BeforeEach
	void setUp() {
		NormalOrderPaySuccessYoushuDispatchListener listener =
				new NormalOrderPaySuccessYoushuDispatchListener(youshuNormalOrderPaySuccessSrDataSyncService);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_PAY_SUCCESS,
				OrdersDispatchEventNames.LISTENER_YOUSHU_ORDERS_NORMAL_ORDER_PAY_SUCCESS,
				ListenerDispatchOptions.async("default", null),
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
	@DisplayName("EVENT_NORMAL_ORDER_PAY_SUCCESS: youshu listener — DispatchConsumerRuntime consume")
	void consume_event_message_invokes_sync_service_once() {
		long companyId = 7L;
		long orderId = 99L;
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("order_id", orderId);
		payload.put("pay_type", "alipay");

		DispatchMessage message =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						OrdersDispatchEventNames.EVENT_NORMAL_ORDER_PAY_SUCCESS,
						payload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T12:00:00Z"),
						"trace-normal-order-pay-success-youshu",
						OrdersDispatchEventNames.LISTENER_YOUSHU_ORDERS_NORMAL_ORDER_PAY_SUCCESS);

		runtime.consume(message, 1);

		verify(youshuNormalOrderPaySuccessSrDataSyncService, times(1))
				.syncOrderAfterNormalPaySuccess(eq(companyId), eq(orderId));
	}
}
