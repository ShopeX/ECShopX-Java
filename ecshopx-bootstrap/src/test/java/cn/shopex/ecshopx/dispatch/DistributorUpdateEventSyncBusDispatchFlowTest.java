package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.common.dispatch.DistributionDispatchEventNames;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class DistributorUpdateEventSyncBusDispatchFlowTest {

	/**
	 * Shape gate for shop batch merged row → {@code entities} payload (same fan-out as production).
	 *
	 * <p>Production data path (types only; this test drives {@link DispatchFacade} directly with a
	 * handcrafted payload equivalent to {@link
	 * cn.shopex.ecshopx.distribution.service.DistributorRowMaps#toApiRow} output after {@link
	 * cn.shopex.ecshopx.distribution.service.DistributorUpdateService#performUpdateAndEvents(Map,
	 * long, String)} updates):
	 *
	 * <ol>
	 *   <li>{@link cn.shopex.ecshopx.distribution.service.DistributorUpdateService#performUpdateAndEvents(
	 *       Map, long, String)}
	 *   <li>{@link cn.shopex.ecshopx.common.dispatch.DistributorUpdateEventDispatchPublisher#publish(Map)}
	 *   <li>{@link cn.shopex.ecshopx.config.DistributorUpdateEventDispatchPublisherImpl#publish(Map)}
	 *   <li>{@link cn.shopex.ecshopx.dispatch.DispatchFacade#publishEvent(String, Map, DispatchOptions)}
	 *   <li>{@link cn.shopex.ecshopx.dispatch.SyncDispatchDriver} (via {@link
	 *       cn.shopex.ecshopx.dispatch.DispatchCore#asyncReady})
	 *   <li>{@link cn.shopex.ecshopx.common.dispatch.DispatchListener#onEvent(Map)} — e.g. {@link
	 *       cn.shopex.ecshopx.thirdparty.dispatch.ShopUpdateSendOmeDispatchListener}
	 * </ol>
	 */
	@Test
	void publishDistributorUpdate_MergedRowShapeShopBatchAlias_deliversEntitiesToListener() {
		DispatchListener listener = mock(DispatchListener.class);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		registry.registerEventListener(
				DistributionDispatchEventNames.EVENT_DISTRIBUTOR_UPDATE,
				DistributionDispatchEventNames.LISTENER_SHOP_UPDATE_SEND_OME,
				ListenerDispatchOptions.syncDefaults(),
				listener);

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("company_id", 502L);
		entities.put("distributor_id", 4004L);
		entities.put("shop_id", 99002L);
		entities.put("is_distributor", false);
		entities.put("name", "分店招牌");
		entities.put("address", "滨江路1号");
		entities.put("mobile", "13900001111");
		entities.put("is_valid", "true");
		entities.put("province", "江苏省");
		entities.put("city", "南京市");
		entities.put("area", "鼓楼区");
		entities.put("hour", "10:00 - 19:00");
		entities.put("logo", "/s/logo.png");
		entities.put("banner", "/s/banner.png");
		entities.put("auto_sync_goods", true);
		entities.put("is_audit_goods", false);
		entities.put("contact", "李四");
		entities.put("is_ziti", false);
		entities.put("lng", "118.796877");
		entities.put("lat", "32.060255");
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		facade.publishEvent(
				DistributionDispatchEventNames.EVENT_DISTRIBUTOR_UPDATE, payload, DispatchOptions.eventDefaults());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		InOrder order = inOrder(listener);
		order.verify(listener).onEvent(payloadCaptor.capture());
		Map<String, Object> listenerPayload = payloadCaptor.getValue();
		Map<?, ?> sent = (Map<?, ?>) listenerPayload.get("entities");
		assertEquals(502L, ((Number) sent.get("company_id")).longValue());
		assertEquals(4004L, ((Number) sent.get("distributor_id")).longValue());
		assertEquals(false, sent.get("is_distributor"));
		assertEquals("分店招牌", sent.get("name"));
		assertEquals("滨江路1号", sent.get("address"));
		assertEquals("10:00 - 19:00", sent.get("hour"));
		assertEquals("江苏省", sent.get("province"));
		assertEquals("南京市", sent.get("city"));
		assertEquals("鼓楼区", sent.get("area"));

		assertTrue(captured.isEmpty());
	}

	@Test
	void publishDistributorUpdate_sync_deliversEntitiesToShopUpdateSendOmeInOrder() {
		DispatchListener listener = mock(DispatchListener.class);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		registry.registerEventListener(
				DistributionDispatchEventNames.EVENT_DISTRIBUTOR_UPDATE,
				DistributionDispatchEventNames.LISTENER_SHOP_UPDATE_SEND_OME,
				ListenerDispatchOptions.syncDefaults(),
				listener);

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("company_id", 42L);
		entities.put("distributor_id", 1001L);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		facade.publishEvent(
				DistributionDispatchEventNames.EVENT_DISTRIBUTOR_UPDATE, payload, DispatchOptions.eventDefaults());

		List<DispatchMessage> published = facade.publishedMessages();
		assertEquals(2, published.size());
		for (DispatchMessage msg : published) {
			assertEquals(DistributionDispatchEventNames.EVENT_DISTRIBUTOR_UPDATE, msg.messageName());
			assertEquals(DistributionDispatchEventNames.LISTENER_SHOP_UPDATE_SEND_OME, msg.listenerName());
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

	@Test
	void publishDistributorUpdate_sync_twice_samePayload_invokesListenerTwice() {
		DispatchListener listener = mock(DispatchListener.class);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		registry.registerEventListener(
				DistributionDispatchEventNames.EVENT_DISTRIBUTOR_UPDATE,
				DistributionDispatchEventNames.LISTENER_SHOP_UPDATE_SEND_OME,
				ListenerDispatchOptions.syncDefaults(),
				listener);

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("company_id", 99L);
		entities.put("distributor_id", 5005L);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		facade.publishEvent(
				DistributionDispatchEventNames.EVENT_DISTRIBUTOR_UPDATE, payload, DispatchOptions.eventDefaults());
		facade.publishEvent(
				DistributionDispatchEventNames.EVENT_DISTRIBUTOR_UPDATE, payload, DispatchOptions.eventDefaults());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		InOrder order = inOrder(listener);
		order.verify(listener, times(2)).onEvent(payloadCaptor.capture());
		assertEquals(2, payloadCaptor.getAllValues().size());
		for (Map<String, Object> p : payloadCaptor.getAllValues()) {
			Map<?, ?> ent = (Map<?, ?>) p.get("entities");
			assertEquals(99L, ((Number) ent.get("company_id")).longValue());
			assertEquals(5005L, ((Number) ent.get("distributor_id")).longValue());
		}

		assertTrue(captured.isEmpty());
	}
}
