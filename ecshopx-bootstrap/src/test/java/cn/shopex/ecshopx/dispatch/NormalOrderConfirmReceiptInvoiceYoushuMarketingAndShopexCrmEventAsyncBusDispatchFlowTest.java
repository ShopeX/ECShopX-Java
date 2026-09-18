package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.orders.dispatch.NormalOrderConfirmReceiptInvoiceDispatchListener;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.service.normal.OrderInvoiceEndTimeOnOrderFinishService;
import cn.shopex.ecshopx.thirdparty.dispatch.NormalOrderConfirmReceiptShopexCrmSyncDispatchListener;
import cn.shopex.ecshopx.thirdparty.dispatch.OrderConfirmReceiptPushMarketingCenterOnNormalOrderConfirmReceiptDispatchListener;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.OrderConfirmReceiptPushMarketingCenterOnNormalOrderConfirmReceiptProcessor;
import cn.shopex.ecshopx.thirdparty.service.shopexcrm.NormalOrderConfirmReceiptShopexCrmSyncExecutionService;
import cn.shopex.ecshopx.thirdparty.service.shopexcrm.ShopexCrmSyncSingleOrderPort;
import cn.shopex.ecshopx.youshu.dispatch.NormalOrderConfirmReceiptYoushuDispatchListener;
import cn.shopex.ecshopx.youshu.service.YoushuNormalOrderConfirmReceiptSrDataSyncService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

class NormalOrderConfirmReceiptInvoiceYoushuMarketingAndShopexCrmEventAsyncBusDispatchFlowTest {

	private static final String INVOICE_LISTENER_NAME = "listener:orders.listeners.OrderFinishInvoice";

	@Test
	void publishNormalOrderConfirmReceipt_async_enqueuesFourListenerTasks_andConsumersRunAll() {
		NormalOrdersMapper normalOrdersMapper = mock(NormalOrdersMapper.class);
		OrderInvoiceEndTimeOnOrderFinishService invoiceService = mock(OrderInvoiceEndTimeOnOrderFinishService.class);
		YoushuNormalOrderConfirmReceiptSrDataSyncService youshuSvc = mock(YoushuNormalOrderConfirmReceiptSrDataSyncService.class);
		OrderConfirmReceiptPushMarketingCenterOnNormalOrderConfirmReceiptProcessor marketingProcessor =
				mock(OrderConfirmReceiptPushMarketingCenterOnNormalOrderConfirmReceiptProcessor.class);
		ShopexCrmSyncSingleOrderPort shopexCrmPort = mock(ShopexCrmSyncSingleOrderPort.class);
		JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

		long companyId = 100L;
		long orderId = 200L;
		when(jdbcTemplate.queryForObject(
						eq("SELECT pay_status FROM orders_normal_orders WHERE company_id = ? AND order_id = ? LIMIT 1"),
						eq(String.class),
						eq(companyId),
						eq(orderId)))
				.thenReturn("PAYED");
		int endSec = 1_710_000_000;
		int closeSec = 86_400;
		NormalOrders fresh = new NormalOrders();
		fresh.setCompanyId(companyId);
		fresh.setOrderId(orderId);
		fresh.setOrderStatus("DONE");
		fresh.setEndTime((long) endSec);
		fresh.setOrderAutoCloseAftersalesTime(closeSec);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(fresh));

		NormalOrderConfirmReceiptInvoiceDispatchListener invoiceListener =
				new NormalOrderConfirmReceiptInvoiceDispatchListener(normalOrdersMapper, invoiceService);
		NormalOrderConfirmReceiptYoushuDispatchListener youshuListener =
				new NormalOrderConfirmReceiptYoushuDispatchListener(youshuSvc);
		OrderConfirmReceiptPushMarketingCenterOnNormalOrderConfirmReceiptDispatchListener marketingListener =
				new OrderConfirmReceiptPushMarketingCenterOnNormalOrderConfirmReceiptDispatchListener(marketingProcessor);
		NormalOrderConfirmReceiptShopexCrmSyncExecutionService shopexExecution =
				new NormalOrderConfirmReceiptShopexCrmSyncExecutionService(shopexCrmPort, jdbcTemplate, "on");
		NormalOrderConfirmReceiptShopexCrmSyncDispatchListener shopexListener =
				new NormalOrderConfirmReceiptShopexCrmSyncDispatchListener(shopexExecution);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_CONFIRM_RECEIPT,
				INVOICE_LISTENER_NAME,
				ListenerDispatchOptions.syncDefaults(),
				invoiceListener);
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_CONFIRM_RECEIPT,
				OrdersDispatchEventNames.LISTENER_YOUSHU_ORDERS_NORMAL_ORDER_CONFIRM_RECEIPT,
				ListenerDispatchOptions.async("default", null),
				youshuListener);
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_CONFIRM_RECEIPT,
				OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_CONFIRM_RECEIPT_PUSH_MARKETING_CENTER_ON_NORMAL_ORDER_CONFIRM_RECEIPT,
				ListenerDispatchOptions.async("default", null),
				marketingListener);
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_CONFIRM_RECEIPT,
				OrdersDispatchEventNames.LISTENER_THIRDPARTY_SHOPEX_CRM_SYNC_CONFIRM_RECEIPT_ORDER_ON_NORMAL_ORDER_CONFIRM_RECEIPT,
				ListenerDispatchOptions.syncDefaults(),
				shopexListener);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("order_id", orderId);

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_CONFIRM_RECEIPT,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(4, captured.size());
		assertEquals(INVOICE_LISTENER_NAME, captured.get(0).listenerName());
		assertEquals(OrdersDispatchEventNames.LISTENER_YOUSHU_ORDERS_NORMAL_ORDER_CONFIRM_RECEIPT, captured.get(1).listenerName());
		assertEquals(
				OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_CONFIRM_RECEIPT_PUSH_MARKETING_CENTER_ON_NORMAL_ORDER_CONFIRM_RECEIPT,
				captured.get(2).listenerName());
		assertEquals(
				OrdersDispatchEventNames.LISTENER_THIRDPARTY_SHOPEX_CRM_SYNC_CONFIRM_RECEIPT_ORDER_ON_NORMAL_ORDER_CONFIRM_RECEIPT,
				captured.get(3).listenerName());

		DispatchMessage youshuMsg = captured.get(1);
		DispatchMessage marketingMsg = captured.get(2);
		DispatchMessage shopexMsg = captured.get(3);
		assertSame(youshuMsg.occurredAt(), marketingMsg.occurredAt());
		assertSame(youshuMsg.occurredAt(), shopexMsg.occurredAt());
		assertEquals(youshuMsg.traceId(), marketingMsg.traceId());
		assertEquals(youshuMsg.traceId(), shopexMsg.traceId());
		assertEquals(youshuMsg.retryPolicy(), marketingMsg.retryPolicy());
		assertEquals(youshuMsg.retryPolicy(), shopexMsg.retryPolicy());
		assertEquals(youshuMsg.driverType(), marketingMsg.driverType());
		assertEquals(youshuMsg.driverType(), shopexMsg.driverType());
		assertEquals(DispatchMessageType.EVENT, marketingMsg.messageType());
		assertEquals(DispatchMessageType.EVENT, youshuMsg.messageType());
		assertEquals(DispatchMessageType.EVENT, shopexMsg.messageType());

		for (DispatchMessage m : captured) {
			assertEquals("default", m.queue());
			assertEquals(DispatchMode.ASYNC, m.dispatchMode());
			assertEquals(DispatchDriverType.REDIS, m.driverType());
		}

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(captured.get(0), 1);
		runtime.consume(captured.get(1), 1);
		runtime.consume(captured.get(2), 1);
		runtime.consume(captured.get(3), 1);

		verify(invoiceService).updateInvoiceEndTime(eq(companyId), eq(orderId), eq(endSec), eq(closeSec));
		verify(youshuSvc).syncOrderAfterNormalOrderConfirmReceipt(eq(companyId), eq(orderId));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> marketingCaptor = ArgumentCaptor.forClass(Map.class);
		verify(marketingProcessor).handle(marketingCaptor.capture());
		Map<String, Object> marketingPayload = marketingCaptor.getValue();
		assertEquals(companyId, marketingPayload.get("company_id"));
		assertEquals(orderId, marketingPayload.get("order_id"));

		verify(shopexCrmPort).syncSingleOrder(eq(companyId), eq(orderId));
	}
}
