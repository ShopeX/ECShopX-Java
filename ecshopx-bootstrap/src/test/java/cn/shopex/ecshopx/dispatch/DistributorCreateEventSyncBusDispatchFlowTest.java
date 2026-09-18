package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.common.dispatch.DistributionDispatchEventNames;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class DistributorCreateEventSyncBusDispatchFlowTest {

	@Test
	void publishDistributorCreate_sync_deliversEntitiesToShopCreateSendOmeInOrder() {
		DispatchListener listener = mock(DispatchListener.class);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		registry.registerEventListener(
				DistributionDispatchEventNames.EVENT_DISTRIBUTOR_CREATE,
				DistributionDispatchEventNames.LISTENER_SHOP_CREATE_SEND_OME,
				ListenerDispatchOptions.syncDefaults(),
				listener);

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("company_id", 42L);
		entities.put("distributor_id", 1001L);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		facade.publishEvent(
				DistributionDispatchEventNames.EVENT_DISTRIBUTOR_CREATE, payload, DispatchOptions.eventDefaults());

		List<DispatchMessage> published = facade.publishedMessages();
		assertEquals(2, published.size());
		for (DispatchMessage msg : published) {
			assertEquals(DistributionDispatchEventNames.EVENT_DISTRIBUTOR_CREATE, msg.messageName());
			assertEquals(
					DistributionDispatchEventNames.LISTENER_SHOP_CREATE_SEND_OME, msg.listenerName());
			Map<?, ?> ent = (Map<?, ?>) msg.payload().get("entities");
			assertEquals(42L, ((Number) ent.get("company_id")).longValue());
			assertEquals(1001L, ((Number) ent.get("distributor_id")).longValue());
		}

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);

		InOrder order = inOrder(listener);
		order.verify(listener).onEvent(payloadCaptor.capture());
		Map<String, Object> listenerPayload = payloadCaptor.getValue();
		Map<?, ?> sent = (Map<?, ?>) listenerPayload.get("entities");
		assertEquals(42L, ((Number) sent.get("company_id")).longValue());
		assertEquals(1001L, ((Number) sent.get("distributor_id")).longValue());

		assertTrue(captured.isEmpty());
	}
}
