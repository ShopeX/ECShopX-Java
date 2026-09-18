package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.common.dispatch.ReservationDispatchEventNames;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class ReservationFinishEventSyncBusDispatchFlowTest {

	@Test
	void publishReservationFinish_sync_deliversEntitiesToListenersInRegistrationOrder() {
		DispatchListener workShift = mock(DispatchListener.class);
		DispatchListener sendWxa = mock(DispatchListener.class);
		DispatchListener remind = mock(DispatchListener.class);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		registry.registerEventListener(
				ReservationDispatchEventNames.EVENT_RESERVATION_FINISH,
				ReservationDispatchEventNames.LISTENER_RESERVATION_FINISH_WORK_SHIFT_ADD,
				ListenerDispatchOptions.syncDefaults(),
				workShift);
		registry.registerEventListener(
				ReservationDispatchEventNames.EVENT_RESERVATION_FINISH,
				ReservationDispatchEventNames.LISTENER_RESERVATION_FINISH_SEND_WXA_TEMPLATE,
				ListenerDispatchOptions.syncDefaults(),
				sendWxa);
		registry.registerEventListener(
				ReservationDispatchEventNames.EVENT_RESERVATION_FINISH,
				ReservationDispatchEventNames.LISTENER_RESERVATION_REMIND_SEND_WXA_TEMPLATE,
				ListenerDispatchOptions.async("default", null),
				remind);

		Map<String, Object> post = new LinkedHashMap<>();
		post.put("company_id", 1L);
		post.put("shop_id", 2L);
		post.put("date_day", "2026-03-01");
		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("postdata", post);
		entities.put("result", Map.of("recordId", 99L));
		entities.put("setting_data", Map.of("reservationMode", 0));
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		facade.publishEvent(
				ReservationDispatchEventNames.EVENT_RESERVATION_FINISH, payload, DispatchOptions.eventDefaults());

		List<DispatchMessage> published = facade.publishedMessages();
		assertEquals(1, published.size());
		assertEquals(ReservationDispatchEventNames.EVENT_RESERVATION_FINISH, published.get(0).messageName());
		assertTrue(captured.isEmpty());

		InOrder order = inOrder(workShift, sendWxa, remind);
		order.verify(workShift).onEvent(payload);
		order.verify(sendWxa).onEvent(payload);
		order.verify(remind).onEvent(payload);
	}
}
