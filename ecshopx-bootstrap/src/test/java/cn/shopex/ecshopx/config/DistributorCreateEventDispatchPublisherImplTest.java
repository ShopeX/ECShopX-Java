package cn.shopex.ecshopx.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.DistributionDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchCore;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchFanOutPlanner;
import cn.shopex.ecshopx.dispatch.DispatchMessage;
import cn.shopex.ecshopx.dispatch.DispatchMessageType;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchRegistry;
import cn.shopex.ecshopx.dispatch.SyncDispatchDriver;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DistributorCreateEventDispatchPublisherImplTest {

	@Test
	void publish_invokesDispatchFacadeWithEventNameAndEventDefaults() {
		DispatchFacade facade = mock(DispatchFacade.class);
		DistributorCreateEventDispatchPublisherImpl publisher = new DistributorCreateEventDispatchPublisherImpl(facade);

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("company_id", 9001L);
		entities.put("distributor_id", 55L);

		publisher.publish(entities);

		verify(facade, times(1))
				.publishEvent(
						eq(DistributionDispatchEventNames.EVENT_DISTRIBUTOR_CREATE),
						argThat(payload -> {
							Object ent = payload.get("entities");
							if (!(ent instanceof Map<?, ?> em)) {
								return false;
							}
							return 9001L == ((Number) em.get("company_id")).longValue()
									&& 55L == ((Number) em.get("distributor_id")).longValue();
						}),
						eq(DispatchOptions.eventDefaults()));
	}

	@Test
	void publish_nullEntities_wrapsEmptyEntitiesMap() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> asyncCaptured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, asyncCaptured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		DistributorCreateEventDispatchPublisherImpl publisher = new DistributorCreateEventDispatchPublisherImpl(facade);
		publisher.publish(null);

		List<DispatchMessage> published = facade.publishedMessages();
		assertEquals(1, published.size());
		DispatchMessage dispatched = published.get(0);
		assertEquals(DispatchMessageType.EVENT, dispatched.messageType());
		assertEquals(DispatchMode.SYNC, dispatched.dispatchMode());
		assertEquals(DispatchDriverType.SYNC, dispatched.driverType());
		assertEquals(DistributionDispatchEventNames.EVENT_DISTRIBUTOR_CREATE, dispatched.messageName());
		Object ent = dispatched.payload().get("entities");
		assertTrue(ent instanceof Map<?, ?>);
		assertTrue(((Map<?, ?>) ent).isEmpty());
		assertTrue(asyncCaptured.isEmpty());
	}
}
