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

class WxShopsUpdateEventDispatchFlowTest {

	@Test
	void publishEvent_sync_runsListenerInline() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		DispatchListener listener = mock(DispatchListener.class);
		registry.registerEventListener(
				WechatDispatchEventNames.EVENT_WX_SHOPS_UPDATE,
				"listener:companys.wx_shops_update",
				ListenerDispatchOptions.asyncDefaults(),
				listener);

		List<DispatchMessage> redisCaptured = new ArrayList<>();
		SyncDispatchDriver syncDriver = new SyncDispatchDriver(registry);
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, syncDriver, Map.of(DispatchDriverType.REDIS, redisCaptured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new HashMap<>();
		payload.put("audit_id", "u1");
		payload.put("status", "2");
		payload.put("reason", "done");

		facade.publishEvent(
				WechatDispatchEventNames.EVENT_WX_SHOPS_UPDATE, payload, DispatchOptions.eventDefaults());

		assertTrue(redisCaptured.isEmpty());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass((Class) Map.class);
		verify(listener).onEvent(cap.capture());
		assertEquals("u1", cap.getValue().get("audit_id"));
		assertEquals("done", cap.getValue().get("reason"));
	}
}
