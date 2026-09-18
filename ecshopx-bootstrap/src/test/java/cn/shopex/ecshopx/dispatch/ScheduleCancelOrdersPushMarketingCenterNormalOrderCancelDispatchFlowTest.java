package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.thirdparty.dispatch.ScheduleCancelOrdersPushMarketingCenterDispatchListener;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.ScheduleCancelOrdersPushMarketingCenterProcessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ScheduleCancelOrdersPushMarketingCenterNormalOrderCancelDispatchFlowTest {

	@Test
	void publishEvent_withMarketingListener_invokesBasicsOrderProccessForCronSource() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_CANCEL,
				"listener:orders.normal_order_cancel_auto_pass",
				ListenerDispatchOptions.asyncDefaults(),
				payload -> {});

		ScheduleCancelOrdersPushMarketingCenterProcessor processor =
				mock(ScheduleCancelOrdersPushMarketingCenterProcessor.class);
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_CANCEL,
				"listener:thirdparty.schedule_cancel_orders_push_marketing_center",
				ListenerDispatchOptions.asyncDefaults(),
				new ScheduleCancelOrdersPushMarketingCenterDispatchListener(processor));

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 11L);
		payload.put("order_id", 9001L);
		payload.put("source", ScheduleCancelOrdersPushMarketingCenterProcessor.CRON_SCHEDULE_CANCEL_SOURCE);

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_CANCEL, payload, DispatchOptions.eventDefaults());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(processor).handle(captor.capture());
		Map<String, Object> passed = captor.getValue();
		assertEquals(11L, passed.get("company_id"));
		assertEquals(9001L, passed.get("order_id"));
		assertEquals(ScheduleCancelOrdersPushMarketingCenterProcessor.CRON_SCHEDULE_CANCEL_SOURCE, passed.get("source"));
	}

	@Test
	void publishEvent_fanOutOrder_autoPassBeforeMarketing() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<String> callOrder = new ArrayList<>();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_CANCEL,
				"listener:orders.normal_order_cancel_auto_pass",
				ListenerDispatchOptions.asyncDefaults(),
				payload -> callOrder.add("auto_pass"));

		ScheduleCancelOrdersPushMarketingCenterProcessor processor =
				mock(ScheduleCancelOrdersPushMarketingCenterProcessor.class);
		doAnswer(invocation -> {
					callOrder.add("marketing");
					return null;
				})
				.when(processor)
				.handle(any());

		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_CANCEL,
				"listener:thirdparty.schedule_cancel_orders_push_marketing_center",
				ListenerDispatchOptions.asyncDefaults(),
				new ScheduleCancelOrdersPushMarketingCenterDispatchListener(processor));

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 1L);
		payload.put("order_id", 2L);
		payload.put("source", ScheduleCancelOrdersPushMarketingCenterProcessor.CRON_SCHEDULE_CANCEL_SOURCE);

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_CANCEL, payload, DispatchOptions.eventDefaults());

		assertEquals(List.of("auto_pass", "marketing"), callOrder);
	}

	@Test
	void publishEvent_fanOutOrder_autoPassMarketingThenYoushuCancel() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<String> callOrder = new ArrayList<>();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_CANCEL,
				"listener:orders.normal_order_cancel_auto_pass",
				ListenerDispatchOptions.asyncDefaults(),
				payload -> callOrder.add("auto_pass"));

		ScheduleCancelOrdersPushMarketingCenterProcessor processor =
				mock(ScheduleCancelOrdersPushMarketingCenterProcessor.class);
		doAnswer(invocation -> {
					callOrder.add("marketing");
					return null;
				})
				.when(processor)
				.handle(any());

		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_CANCEL,
				"listener:thirdparty.schedule_cancel_orders_push_marketing_center",
				ListenerDispatchOptions.asyncDefaults(),
				new ScheduleCancelOrdersPushMarketingCenterDispatchListener(processor));

		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_CANCEL,
				OrdersDispatchEventNames.LISTENER_YOUSHU_ORDERS_NORMAL_ORDER_CANCEL,
				ListenerDispatchOptions.async("default", null),
				payload -> callOrder.add("youshu_cancel"));

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 1L);
		payload.put("order_id", 2L);
		payload.put("source", ScheduleCancelOrdersPushMarketingCenterProcessor.CRON_SCHEDULE_CANCEL_SOURCE);

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_CANCEL, payload, DispatchOptions.eventDefaults());

		assertEquals(List.of("auto_pass", "marketing", "youshu_cancel"), callOrder);
	}
}
