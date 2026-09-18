package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class ItemCreateEventSyncBusDispatchFlowTest {

	@Test
	void publishItemCreate_sync_invokesCreateItemSuccessPromotionsInOrder() {
		DispatchListener primary = mock(DispatchListener.class);
		DispatchListener secondary = mock(DispatchListener.class);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		registry.registerEventListener(
				GoodsDispatchEventNames.EVENT_ITEM_CREATE,
				"listener:promotions.CreateItemSuccessPromotions",
				ListenerDispatchOptions.syncDefaults(),
				primary);
		registry.registerEventListener(
				GoodsDispatchEventNames.EVENT_ITEM_CREATE,
				"listener:promotions.ItemCreateSecondaryStub",
				ListenerDispatchOptions.syncDefaults(),
				secondary);

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("company_id", 42L);
		entities.put("item_main_cat_id", 9L);
		entities.put("brand_id", 3);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);
		payload.put("item_ids", List.of(100L, 101L));

		facade.publishEvent(GoodsDispatchEventNames.EVENT_ITEM_CREATE, payload, DispatchOptions.eventDefaults());

		List<DispatchMessage> published = facade.publishedMessages();
		assertEquals(1, published.size());
		DispatchMessage root = published.get(0);
		assertEquals(GoodsDispatchEventNames.EVENT_ITEM_CREATE, root.messageName());
		assertEquals(42L, ((Map<?, ?>) root.payload().get("entities")).get("company_id"));
		assertEquals(List.of(100L, 101L), root.payload().get("item_ids"));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);

		InOrder order = inOrder(primary, secondary);
		order.verify(primary).onEvent(payloadCaptor.capture());
		Map<String, Object> listenerPayload = payloadCaptor.getValue();
		assertEquals(42L, ((Map<?, ?>) listenerPayload.get("entities")).get("company_id"));
		assertEquals(List.of(100L, 101L), listenerPayload.get("item_ids"));

		order.verify(secondary)
				.onEvent(argThat(p -> {
					Object companyId = ((Map<?, ?>) p.get("entities")).get("company_id");
					return 42L == ((Number) companyId).longValue()
							&& List.of(100L, 101L).equals(p.get("item_ids"));
				}));
		assertTrue(captured.isEmpty());
	}
}
