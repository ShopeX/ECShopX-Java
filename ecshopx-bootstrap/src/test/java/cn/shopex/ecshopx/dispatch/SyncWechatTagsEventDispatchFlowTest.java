package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import cn.shopex.ecshopx.common.dispatch.MembersDispatchEventNames;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SyncWechatTagsEventDispatchFlowTest {

	private static final String LISTENER_SYNC_WECHAT_TAGS = "listener:members.sync_wechat_tags";

	@Test
	@DisplayName(
			"publishEvent (SYNC) invokes SyncWechatTags listener immediately and does not enqueue Redis")
	void publishEvent_sync_invokesSyncWechatTagsListener_immediately_andDoesNotEnqueueRedis() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		AtomicInteger calls = new AtomicInteger();
		registry.registerEventListener(
				MembersDispatchEventNames.EVENT_SYNC_WECHAT_TAGS,
				LISTENER_SYNC_WECHAT_TAGS,
				ListenerDispatchOptions.asyncDefaults(),
				payload -> calls.incrementAndGet());

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 42L);
		payload.put("authorizer_appid", "wx-appid");

		facade.publishEvent(
				MembersDispatchEventNames.EVENT_SYNC_WECHAT_TAGS,
				payload,
				DispatchOptions.eventDefaults());

		assertEquals(0, captured.size());
		assertEquals(1, calls.get());
		DispatchMessage last = facade.lastPublishedMessage();
		assertNotNull(last);
		assertEquals(DispatchMode.SYNC, last.dispatchMode());
		assertEquals(DispatchMessageType.EVENT, last.messageType());
		assertEquals(MembersDispatchEventNames.EVENT_SYNC_WECHAT_TAGS, last.messageName());
		assertEquals(LISTENER_SYNC_WECHAT_TAGS, last.listenerName());
		@SuppressWarnings("unchecked")
		Map<String, Object> lastPayload = (Map<String, Object>) last.payload();
		assertEquals(42L, lastPayload.get("company_id"));
		assertEquals("wx-appid", lastPayload.get("authorizer_appid"));
	}
}
