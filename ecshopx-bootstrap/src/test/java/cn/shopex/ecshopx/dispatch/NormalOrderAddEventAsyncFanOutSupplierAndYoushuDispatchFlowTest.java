package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.orders.dispatch.OrdersSupplierOrderSplitOnNormalOrderAddDispatchListener;
import cn.shopex.ecshopx.supplier.service.SupplierOrderSplitOnNormalOrderAddService;
import cn.shopex.ecshopx.youshu.dispatch.NormalOrderAddYoushuDispatchListener;
import cn.shopex.ecshopx.youshu.service.YoushuNormalOrderAddSrDataSyncService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NormalOrderAddEventAsyncFanOutSupplierAndYoushuDispatchFlowTest {

	private static final String LISTENER_SUPPLIER = "listener:orders.supplier_order_split_on_normal_order_add";

	@Test
	void publishNormalOrderAdd_async_enqueuesTwoListenerTasks_andConsumerRunsBoth() {
		SupplierOrderSplitOnNormalOrderAddService supplierSvc = mock(SupplierOrderSplitOnNormalOrderAddService.class);
		YoushuNormalOrderAddSrDataSyncService youshuSvc = mock(YoushuNormalOrderAddSrDataSyncService.class);

		OrdersSupplierOrderSplitOnNormalOrderAddDispatchListener supplierListener =
				new OrdersSupplierOrderSplitOnNormalOrderAddDispatchListener(supplierSvc);
		NormalOrderAddYoushuDispatchListener youshuListener = new NormalOrderAddYoushuDispatchListener(youshuSvc);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_ADD,
				LISTENER_SUPPLIER,
				ListenerDispatchOptions.async("default", null),
				supplierListener);
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_ADD,
				OrdersDispatchEventNames.LISTENER_YOUSHU_ORDERS_NORMAL_ORDER_ADD,
				ListenerDispatchOptions.async("default", null),
				youshuListener);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 7L);
		payload.put("order_id", 55L);
		payload.put("pay_type", "wxpay");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_ADD,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(2, captured.size());
		assertEquals(LISTENER_SUPPLIER, captured.get(0).listenerName());
		assertEquals(OrdersDispatchEventNames.LISTENER_YOUSHU_ORDERS_NORMAL_ORDER_ADD, captured.get(1).listenerName());
		assertEquals("default", captured.get(0).queue());
		assertEquals("default", captured.get(1).queue());
		assertEquals(DispatchMode.ASYNC, captured.get(0).dispatchMode());
		assertEquals(DispatchDriverType.REDIS, captured.get(0).driverType());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(captured.get(0), 1);
		runtime.consume(captured.get(1), 1);

		verify(supplierSvc).split(7L, 55L);
		verify(youshuSvc).syncOrderAfterNormalAdd(7L, 55L);
	}
}
