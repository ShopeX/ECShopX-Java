package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.thirdparty.dispatch.OrderDeliveryDmCrmOnNormalOrderDeliveryDispatchListener;
import cn.shopex.ecshopx.thirdparty.dispatch.OrderDeliveryPushMarketingCenterOnNormalOrderDeliveryDispatchListener;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.OrderDeliveryDmCrmOnNormalOrderDeliveryProcessor;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.OrderDeliveryPushMarketingCenterOnNormalOrderDeliveryProcessor;
import cn.shopex.ecshopx.youshu.dispatch.NormalOrderDeliveryYoushuDispatchListener;
import cn.shopex.ecshopx.youshu.service.YoushuNormalOrderDeliverySrDataSyncService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NormalOrderDeliveryOrderDeliveryPushDmCrmDispatchFlowTest {

	@Test
	void publishEvent_enqueuesDmCrmListener_andConsumeInvokesProcessorHandle() {
		OrderDeliveryDmCrmOnNormalOrderDeliveryProcessor processor =
				mock(OrderDeliveryDmCrmOnNormalOrderDeliveryProcessor.class);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_DELIVERY,
				OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_DELIVERY_PUSH_DM_CRM_ON_NORMAL_ORDER_DELIVERY,
				ListenerDispatchOptions.async("default", null),
				new OrderDeliveryDmCrmOnNormalOrderDeliveryDispatchListener(processor));

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 7L);
		payload.put("order_id", 55L);

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_DELIVERY,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.EVENT, msg.messageType());
		assertEquals(
				OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_DELIVERY_PUSH_DM_CRM_ON_NORMAL_ORDER_DELIVERY,
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
	}

	@Test
	void fanOut_threeListeners_orderPreserved_youshu_then_marketing_then_dmcrm() {
		YoushuNormalOrderDeliverySrDataSyncService youshuSvc = mock(YoushuNormalOrderDeliverySrDataSyncService.class);
		OrderDeliveryPushMarketingCenterOnNormalOrderDeliveryProcessor marketingProcessor =
				mock(OrderDeliveryPushMarketingCenterOnNormalOrderDeliveryProcessor.class);
		OrderDeliveryDmCrmOnNormalOrderDeliveryProcessor dmCrmProcessor =
				mock(OrderDeliveryDmCrmOnNormalOrderDeliveryProcessor.class);

		NormalOrderDeliveryYoushuDispatchListener youshuListener = new NormalOrderDeliveryYoushuDispatchListener(youshuSvc);
		OrderDeliveryPushMarketingCenterOnNormalOrderDeliveryDispatchListener marketingListener =
				new OrderDeliveryPushMarketingCenterOnNormalOrderDeliveryDispatchListener(marketingProcessor);
		OrderDeliveryDmCrmOnNormalOrderDeliveryDispatchListener dmCrmListener =
				new OrderDeliveryDmCrmOnNormalOrderDeliveryDispatchListener(dmCrmProcessor);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_DELIVERY,
				OrdersDispatchEventNames.LISTENER_YOUSHU_ORDERS_NORMAL_ORDER_DELIVERY,
				ListenerDispatchOptions.async("default", null),
				youshuListener);
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_DELIVERY,
				OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_DELIVERY_PUSH_MARKETING_CENTER_ON_NORMAL_ORDER_DELIVERY,
				ListenerDispatchOptions.async("default", null),
				marketingListener);
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_DELIVERY,
				OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_DELIVERY_PUSH_DM_CRM_ON_NORMAL_ORDER_DELIVERY,
				ListenerDispatchOptions.async("default", null),
				dmCrmListener);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);
		payload.put("order_id", 2L);

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_DELIVERY,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(3, captured.size());
		assertEquals(OrdersDispatchEventNames.LISTENER_YOUSHU_ORDERS_NORMAL_ORDER_DELIVERY, captured.get(0).listenerName());
		assertEquals(
				OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_DELIVERY_PUSH_MARKETING_CENTER_ON_NORMAL_ORDER_DELIVERY,
				captured.get(1).listenerName());
		assertEquals(
				OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_DELIVERY_PUSH_DM_CRM_ON_NORMAL_ORDER_DELIVERY,
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

		verify(youshuSvc).syncOrderAfterNormalDelivery(1L, 2L);
		verify(marketingProcessor).handle(any());
		verify(dmCrmProcessor).handle(any());
	}
}
