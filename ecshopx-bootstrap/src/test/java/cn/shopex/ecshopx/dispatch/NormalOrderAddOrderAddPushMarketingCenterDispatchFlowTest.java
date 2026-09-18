package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.orders.dispatch.OrdersSupplierOrderSplitOnNormalOrderAddDispatchListener;
import cn.shopex.ecshopx.supplier.service.SupplierOrderSplitOnNormalOrderAddService;
import cn.shopex.ecshopx.thirdparty.dispatch.OrderAddPushMarketingCenterOnNormalOrderAddDispatchListener;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.OrderAddPushMarketingCenterOnNormalOrderAddProcessor;
import cn.shopex.ecshopx.youshu.dispatch.NormalOrderAddYoushuDispatchListener;
import cn.shopex.ecshopx.youshu.service.YoushuNormalOrderAddSrDataSyncService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NormalOrderAddOrderAddPushMarketingCenterDispatchFlowTest {

	private static final String LISTENER_SUPPLIER = "listener:orders.supplier_order_split_on_normal_order_add";

	@Test
	void publishEvent_enqueuesMarketingListener_andConsumeInvokesProcessorHandle() {
		OrderAddPushMarketingCenterOnNormalOrderAddProcessor processor =
				mock(OrderAddPushMarketingCenterOnNormalOrderAddProcessor.class);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_ADD,
				OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_ADD_PUSH_MARKETING_CENTER_ON_NORMAL_ORDER_ADD,
				ListenerDispatchOptions.async("default", null),
				new OrderAddPushMarketingCenterOnNormalOrderAddDispatchListener(processor));

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

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(
				OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_ADD_PUSH_MARKETING_CENTER_ON_NORMAL_ORDER_ADD,
				msg.listenerName());
		assertEquals("default", msg.queue());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(processor).handle(captor.capture());
		Map<String, Object> passed = captor.getValue();
		assertEquals(7L, passed.get("company_id"));
		assertEquals(55L, passed.get("order_id"));
		assertEquals("wxpay", passed.get("pay_type"));
	}

	@Test
	void fanOut_threeListeners_orderPreserved() {
		SupplierOrderSplitOnNormalOrderAddService supplierSvc = mock(SupplierOrderSplitOnNormalOrderAddService.class);
		YoushuNormalOrderAddSrDataSyncService youshuSvc = mock(YoushuNormalOrderAddSrDataSyncService.class);
		OrderAddPushMarketingCenterOnNormalOrderAddProcessor marketingProcessor =
				mock(OrderAddPushMarketingCenterOnNormalOrderAddProcessor.class);

		OrdersSupplierOrderSplitOnNormalOrderAddDispatchListener supplierListener =
				new OrdersSupplierOrderSplitOnNormalOrderAddDispatchListener(supplierSvc);
		NormalOrderAddYoushuDispatchListener youshuListener = new NormalOrderAddYoushuDispatchListener(youshuSvc);
		OrderAddPushMarketingCenterOnNormalOrderAddDispatchListener marketingListener =
				new OrderAddPushMarketingCenterOnNormalOrderAddDispatchListener(marketingProcessor);

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
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_ADD,
				OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_ADD_PUSH_MARKETING_CENTER_ON_NORMAL_ORDER_ADD,
				ListenerDispatchOptions.async("default", null),
				marketingListener);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);
		payload.put("order_id", 2L);
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

		assertEquals(3, captured.size());
		assertEquals(LISTENER_SUPPLIER, captured.get(0).listenerName());
		assertEquals(OrdersDispatchEventNames.LISTENER_YOUSHU_ORDERS_NORMAL_ORDER_ADD, captured.get(1).listenerName());
		assertEquals(
				OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_ADD_PUSH_MARKETING_CENTER_ON_NORMAL_ORDER_ADD,
				captured.get(2).listenerName());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		for (DispatchMessage m : captured) {
			runtime.consume(m, 1);
		}

		verify(supplierSvc).split(1L, 2L);
		verify(youshuSvc).syncOrderAfterNormalAdd(1L, 2L);
		verify(marketingProcessor).handle(any());
	}
}
