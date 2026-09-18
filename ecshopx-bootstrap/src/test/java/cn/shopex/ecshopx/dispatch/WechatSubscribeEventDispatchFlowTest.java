package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.common.dispatch.WechatDispatchEventNames;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WechatSubscribeEventDispatchFlowTest {

	@Test
	void publishEvent_sync_runsListenerInline() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		DispatchListener listener = mock(DispatchListener.class);
		registry.registerEventListener(
				WechatDispatchEventNames.EVENT_WECHAT_SUBSCRIBE,
				"listener:members.wechat_subscribe",
				ListenerDispatchOptions.asyncDefaults(),
				listener);

		List<DispatchMessage> redisCaptured = new ArrayList<>();
		SyncDispatchDriver syncDriver = new SyncDispatchDriver(registry);
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, syncDriver, Map.of(DispatchDriverType.REDIS, redisCaptured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new HashMap<>();
		payload.put("openId", "o");
		payload.put("authorizerAppId", "wx");
		payload.put("company_id", 1L);
		payload.put("event", "subscribe");

		facade.publishEvent(
				WechatDispatchEventNames.EVENT_WECHAT_SUBSCRIBE, payload, DispatchOptions.eventDefaults());

		assertTrue(redisCaptured.isEmpty());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass((Class) Map.class);
		verify(listener).onEvent(cap.capture());
		assertEquals("o", cap.getValue().get("openId"));
	}

	@Test
	void publishEvent_sync_withUnsubscribePayload_runsListenerInline() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		DispatchListener listener = mock(DispatchListener.class);
		registry.registerEventListener(
				WechatDispatchEventNames.EVENT_WECHAT_SUBSCRIBE,
				"listener:members.wechat_subscribe",
				ListenerDispatchOptions.asyncDefaults(),
				listener);

		List<DispatchMessage> redisCaptured = new ArrayList<>();
		SyncDispatchDriver syncDriver = new SyncDispatchDriver(registry);
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, syncDriver, Map.of(DispatchDriverType.REDIS, redisCaptured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new HashMap<>();
		payload.put("openId", "ou");
		payload.put("authorizerAppId", "wx");
		payload.put("company_id", 2L);
		payload.put("event", "unsubscribe");

		facade.publishEvent(
				WechatDispatchEventNames.EVENT_WECHAT_SUBSCRIBE, payload, DispatchOptions.eventDefaults());

		assertTrue(redisCaptured.isEmpty());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass((Class) Map.class);
		verify(listener).onEvent(cap.capture());
		assertEquals("ou", cap.getValue().get("openId"));
		assertEquals("unsubscribe", cap.getValue().get("event"));
	}
}
