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

class ItemTagEditEventSyncBusDispatchFlowTest {

	@Test
	void publishItemTagEdit_sync_deliversPayloadWithEntitiesCompanyIdToListener() {
		DispatchListener listener = mock(DispatchListener.class);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		registry.registerEventListener(
				GoodsDispatchEventNames.EVENT_ITEM_TAG_EDIT,
				"listener:promotions.ItemTagEditSuccessPromotions",
				ListenerDispatchOptions.syncDefaults(),
				listener);

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("company_id", 42L);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		facade.publishEvent(GoodsDispatchEventNames.EVENT_ITEM_TAG_EDIT, payload, DispatchOptions.eventDefaults());

		List<DispatchMessage> published = facade.publishedMessages();
		assertEquals(2, published.size());
		for (DispatchMessage msg : published) {
			assertEquals(GoodsDispatchEventNames.EVENT_ITEM_TAG_EDIT, msg.messageName());
			assertEquals(42L, ((Map<?, ?>) msg.payload().get("entities")).get("company_id"));
		}

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);

		InOrder order = inOrder(listener);
		order.verify(listener).onEvent(payloadCaptor.capture());
		Map<String, Object> listenerPayload = payloadCaptor.getValue();
		assertEquals(42L, ((Map<?, ?>) listenerPayload.get("entities")).get("company_id"));

		assertTrue(captured.isEmpty());
	}
}
