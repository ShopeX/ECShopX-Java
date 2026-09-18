package cn.shopex.ecshopx.orders.dispatch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundService;
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
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderPassRefundService;
import cn.shopex.ecshopx.orders.service.admin.OrdersConfirmCancelRefundStatuses;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("EVENT_NORMAL_ORDER_CANCEL: auto pass listener — DispatchConsumerRuntime consume (slow queue)")
class OrdersNormalOrderCancelAutoPassDispatchListenerSlowQueueDispatchAsyncConsumeTest {

	private static final String LISTENER_NAME = "listener:orders.normal_order_cancel_auto_pass";

	@Mock
	private AdminOrderPassRefundService adminOrderPassRefundService;

	@Mock
	private AftersalesRefundService aftersalesRefundService;

	@Mock
	private OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	private DispatchConsumerRuntime runtime;

	@BeforeEach
	void setUp() {
		OrdersNormalOrderCancelAutoPassDispatchListener listener =
				new OrdersNormalOrderCancelAutoPassDispatchListener(
						adminOrderPassRefundService,
						aftersalesRefundService,
						orderValiditySettingRedisReadService,
						normalOrdersMapper);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_CANCEL,
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
	void consume_async_shaped_message_invokes_pass_refund_when_gates_open() {
		long companyId = 7L;
		long orderId = 99L;

		Map<String, Object> platform = new HashMap<>();
		platform.put("auto_aftersales", Boolean.TRUE);
		when(orderValiditySettingRedisReadService.readPlatformSetting(companyId)).thenReturn(platform);

		NormalOrders order = new NormalOrders();
		order.setCompanyId(companyId);
		order.setOrderId(orderId);
		order.setOrderStatus("PAYED");
		order.setCancelStatus("WAIT_PROCESS");
		order.setOrderType("normal");
		order.setSupplierId(0);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);

		AftersalesRefund refund = new AftersalesRefund();
		refund.setRefundStatus("READY");
		refund.setSupplierId(0L);
		when(
						aftersalesRefundService.findSingleForConfirmCancel(
								eq(companyId),
								eq(orderId),
								eq(0L),
								isNull(),
								eq(OrdersConfirmCancelRefundStatuses.REFUND_STATUSES_FOR_ABSTRACT_CANCEL_LOOKUP)))
				.thenReturn(refund);

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("order_id", orderId);
		payload.put("source", "admin_normal_order_full_cancel");

		Instant occurredAt = Instant.parse("2026-05-10T12:10:00Z");
		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						OrdersDispatchEventNames.EVENT_NORMAL_ORDER_CANCEL,
						payload,
						"slow",
						null,
						RetryPolicy.platformDefault(),
						occurredAt,
						"trace-normal-order-cancel-auto-pass-async-consume",
						LISTENER_NAME);

		runtime.consume(msg, 1);

		verify(adminOrderPassRefundService, times(1)).passRefund(anyMap(), eq(refund), anyMap());
	}
}
