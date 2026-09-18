package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import cn.shopex.ecshopx.common.dispatch.GoodsDispatchEventNames;
import cn.shopex.ecshopx.thirdparty.dispatch.ItemAddPushMarketingCenterDispatchListener;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.ItemAddPushMarketingCenterProcessor;
import cn.shopex.ecshopx.youshu.dispatch.ItemAddYoushuDispatchListener;
import cn.shopex.ecshopx.youshu.service.YoushuItemAddSrDataSyncService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class ItemAddEventMarketingCenterAndYoushuFanOutDispatchFlowTest {

	@Test
	void publishEvent_itemAdd_asyncRedis_enqueuesMarketingCenterThenYoushu_andConsumerInvokesBothInRegistrationOrder() {
		ItemAddPushMarketingCenterProcessor mcProcessor = mock(ItemAddPushMarketingCenterProcessor.class);
		ItemAddPushMarketingCenterDispatchListener mcListener = new ItemAddPushMarketingCenterDispatchListener(mcProcessor);

		YoushuItemAddSrDataSyncService syncService = mock(YoushuItemAddSrDataSyncService.class);
		ItemAddYoushuDispatchListener youshuListener = new ItemAddYoushuDispatchListener(syncService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				GoodsDispatchEventNames.EVENT_ITEM_ADD,
				GoodsDispatchEventNames.LISTENER_THIRDPARTY_ITEM_ADD_PUSH_MARKETING_CENTER,
				ListenerDispatchOptions.async("default", null),
				mcListener);
		registry.registerEventListener(
				GoodsDispatchEventNames.EVENT_ITEM_ADD,
				"listener:youshu.items_item_add",
				ListenerDispatchOptions.async("default", null),
				youshuListener);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("item_id", 55L);
		payload.put("company_id", 7L);

		facade.publishEvent(
				GoodsDispatchEventNames.EVENT_ITEM_ADD,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(2, captured.size());
		assertEquals(
				GoodsDispatchEventNames.LISTENER_THIRDPARTY_ITEM_ADD_PUSH_MARKETING_CENTER,
				captured.get(0).listenerName());
		assertEquals("listener:youshu.items_item_add", captured.get(1).listenerName());
		assertEquals("default", captured.get(0).queue());
		assertEquals("default", captured.get(1).queue());
		assertEquals(55L, captured.get(0).payload().get("item_id"));
		assertEquals(7L, captured.get(0).payload().get("company_id"));

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(captured.get(0), 1);
		runtime.consume(captured.get(1), 1);

		InOrder order = inOrder(mcProcessor, syncService);
		order.verify(mcProcessor).handle(argThat(ItemAddEventMarketingCenterAndYoushuFanOutDispatchFlowTest::matchesItemAddPayload));
		order.verify(syncService).syncItemsSku(7L, 55L);
	}

	private static boolean matchesItemAddPayload(Map<String, Object> m) {
		if (m == null) {
			return false;
		}
		return ((Number) m.get("item_id")).longValue() == 55L && ((Number) m.get("company_id")).longValue() == 7L;
	}
}
