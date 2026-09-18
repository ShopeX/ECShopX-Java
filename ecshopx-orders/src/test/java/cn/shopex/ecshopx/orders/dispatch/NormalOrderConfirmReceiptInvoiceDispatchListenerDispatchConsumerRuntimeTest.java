package cn.shopex.ecshopx.orders.dispatch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import cn.shopex.ecshopx.orders.service.normal.OrderInvoiceEndTimeOnOrderFinishService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NormalOrderConfirmReceiptInvoiceDispatchListenerDispatchConsumerRuntimeTest {

	private static final String LISTENER_NAME = "listener:orders.listeners.OrderFinishInvoice";

	@Mock
	private OrderInvoiceEndTimeOnOrderFinishService orderInvoiceEndTimeOnOrderFinishService;

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	private DispatchConsumerRuntime runtime;

	@BeforeEach
	void setUp() {
		NormalOrderConfirmReceiptInvoiceDispatchListener listener =
				new NormalOrderConfirmReceiptInvoiceDispatchListener(
						normalOrdersMapper, orderInvoiceEndTimeOnOrderFinishService);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_CONFIRM_RECEIPT,
				LISTENER_NAME,
				ListenerDispatchOptions.syncDefaults(),
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
	void consume_whenOrderDone_invokesUpdateInvoiceEndTimeOnce() {
		long companyId = 100L;
		long orderId = 200L;
		int endSec = 1_710_000_000;
		int closeSec = 86_400;
		NormalOrders fresh = new NormalOrders();
		fresh.setCompanyId(companyId);
		fresh.setOrderId(orderId);
		fresh.setOrderStatus("DONE");
		fresh.setEndTime((long) endSec);
		fresh.setOrderAutoCloseAftersalesTime(closeSec);

		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(fresh));

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("order_id", orderId);

		Instant occurredAt = Instant.parse("2026-05-10T12:00:00Z");
		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						OrdersDispatchEventNames.EVENT_NORMAL_ORDER_CONFIRM_RECEIPT,
						payload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						occurredAt,
						"trace-normal-order-confirm-receipt-invoice",
						LISTENER_NAME);

		runtime.consume(msg, 1);

		verify(orderInvoiceEndTimeOnOrderFinishService, times(1))
				.updateInvoiceEndTime(eq(companyId), eq(orderId), eq(endSec), eq(closeSec));
	}

	@Test
	void consume_whenOrderMissing_skipsUpdateInvoiceEndTime() {
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of());

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);
		payload.put("order_id", 2L);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						OrdersDispatchEventNames.EVENT_NORMAL_ORDER_CONFIRM_RECEIPT,
						payload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T12:00:00Z"),
						"trace-missing-order",
						LISTENER_NAME);

		runtime.consume(msg, 1);

		verify(orderInvoiceEndTimeOnOrderFinishService, never())
				.updateInvoiceEndTime(anyLong(), anyLong(), anyInt(), anyInt());
	}

	@Test
	void consume_whenOrderStatusNotDone_skipsUpdateInvoiceEndTime() {
		NormalOrders row = new NormalOrders();
		row.setCompanyId(7L);
		row.setOrderId(55L);
		row.setOrderStatus("WAIT_BUYER_CONFIRM");
		row.setEndTime(1L);
		row.setOrderAutoCloseAftersalesTime(2);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(row));

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 7L);
		payload.put("order_id", 55L);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						OrdersDispatchEventNames.EVENT_NORMAL_ORDER_CONFIRM_RECEIPT,
						payload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T12:00:00Z"),
						"trace-not-done",
						LISTENER_NAME);

		runtime.consume(msg, 1);

		verify(orderInvoiceEndTimeOnOrderFinishService, never())
				.updateInvoiceEndTime(anyLong(), anyLong(), anyInt(), anyInt());
	}
}
