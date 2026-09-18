package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.common.dispatch.GoodsDispatchEventNames;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class ItemDeleteEventAsyncFanOutDispatchFlowTest {

	@Test
	void publishEvent_asyncRedis_enqueuesTwoListenerTasks_andConsumerInvokesBothInRegistrationOrder() {
		DispatchListener spyMarketing = mock(DispatchListener.class);
		DispatchListener spyYoushu = mock(DispatchListener.class);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				GoodsDispatchEventNames.EVENT_ITEM_DELETE,
				"listener:thirdparty.item_delete_push_marketing_center",
				ListenerDispatchOptions.syncDefaults(),
				spyMarketing);
		registry.registerEventListener(
				GoodsDispatchEventNames.EVENT_ITEM_DELETE,
				GoodsDispatchEventNames.LISTENER_YOUSHU_ITEMS_ITEM_DELETE,
				ListenerDispatchOptions.async("default", null),
				spyYoushu);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> itemInfo = new LinkedHashMap<>();
		itemInfo.put("item_id", 901L);
		itemInfo.put("company_id", 11L);
		itemInfo.put("item_bn", "bn-del");

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("item_id", 901L);
		payload.put("company_id", 11L);
		payload.put("del_ids", new ArrayList<>(List.of(901L, 902L)));
		payload.put("item_info", itemInfo);

		facade.publishEvent(
				GoodsDispatchEventNames.EVENT_ITEM_DELETE,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(2, captured.size());
		assertEquals("listener:thirdparty.item_delete_push_marketing_center", captured.get(0).listenerName());
		assertEquals(GoodsDispatchEventNames.LISTENER_YOUSHU_ITEMS_ITEM_DELETE, captured.get(1).listenerName());

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(captured.get(0), 1);
		runtime.consume(captured.get(1), 1);

		InOrder order = inOrder(spyMarketing, spyYoushu);
		order.verify(spyMarketing).onEvent(argThat(m -> matchesPayloadShape(m)));
		order.verify(spyYoushu).onEvent(argThat(m -> matchesPayloadShape(m)));
	}

	private static boolean matchesPayloadShape(Map<String, Object> m) {
		if (m == null) {
			return false;
		}
		if (((Number) m.get("item_id")).longValue() != 901L) {
			return false;
		}
		if (((Number) m.get("company_id")).longValue() != 11L) {
			return false;
		}
		Object del = m.get("del_ids");
		if (!(del instanceof List<?> list) || list.size() != 2) {
			return false;
		}
		if (((Number) list.get(0)).longValue() != 901L || ((Number) list.get(1)).longValue() != 902L) {
			return false;
		}
		Object info = m.get("item_info");
		if (!(info instanceof Map<?, ?> im)) {
			return false;
		}
		return "bn-del".equals(String.valueOf(im.get("item_bn")));
	}
}
