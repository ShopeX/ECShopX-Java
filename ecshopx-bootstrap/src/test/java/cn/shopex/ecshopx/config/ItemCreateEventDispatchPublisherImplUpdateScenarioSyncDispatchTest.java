package cn.shopex.ecshopx.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.common.dispatch.GoodsDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchCore;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchFanOutPlanner;
import cn.shopex.ecshopx.dispatch.DispatchMessage;
import cn.shopex.ecshopx.dispatch.DispatchMessageType;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchRegistry;
import cn.shopex.ecshopx.dispatch.ListenerDispatchOptions;
import cn.shopex.ecshopx.dispatch.SyncDispatchDriver;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ItemCreateEventDispatchPublisherImplUpdateScenarioSyncDispatchTest {

	@Test
	void publish_updateShapedEntitiesAndItemIds_syncDispatchesWithSyncDriverAndInvokesListener() {
		DispatchListener listenerMock = mock(DispatchListener.class);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> asyncCaptured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, asyncCaptured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		registry.registerEventListener(
				GoodsDispatchEventNames.EVENT_ITEM_CREATE,
				"listener:promotions.CreateItemSuccessPromotions",
				ListenerDispatchOptions.syncDefaults(),
				listenerMock);

		ItemCreateEventDispatchPublisherImpl publisher = new ItemCreateEventDispatchPublisherImpl(facade);

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("company_id", 7001L);
		entities.put("item_id", 55L);
		entities.put("item_main_cat_id", 9L);
		entities.put("brand_id", 3);
		List<Long> itemIds = List.of(200L, 201L);

		publisher.publish(entities, itemIds);

		List<DispatchMessage> published = facade.publishedMessages();
		assertEquals(2, published.size());
		DispatchMessage dispatched = published.get(1);
		assertEquals(DispatchMessageType.EVENT, dispatched.messageType());
		assertEquals(DispatchMode.SYNC, dispatched.dispatchMode());
		assertEquals(DispatchDriverType.SYNC, dispatched.driverType());
		assertEquals(GoodsDispatchEventNames.EVENT_ITEM_CREATE, dispatched.messageName());
		assertEquals("listener:promotions.CreateItemSuccessPromotions", dispatched.listenerName());

		verify(listenerMock, times(1))
				.onEvent(
						argThat(payload -> {
							Object ent = payload.get("entities");
							if (!(ent instanceof Map<?, ?> em)) {
								return false;
							}
							if (((Number) em.get("company_id")).longValue() != 7001L) {
								return false;
							}
							if (((Number) em.get("item_id")).longValue() != 55L) {
								return false;
							}
							Object ids = payload.get("item_ids");
							return ids instanceof List<?> list
									&& list.size() == 2
									&& 200L == ((Number) list.get(0)).longValue()
									&& 201L == ((Number) list.get(1)).longValue();
						}));

		assertTrue(asyncCaptured.isEmpty());
	}
}
