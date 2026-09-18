package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import cn.shopex.ecshopx.common.dispatch.MembersDispatchEventNames;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SyncWechatFansEventDispatchFlowTest {

	private static final String LISTENER_SYNC_WECHAT_FANS = "listener:members.sync_wechat_fans";

	@Test
	void publishEvent_async_enqueuesSlowQueueListener_andConsumerRunsProcessor() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		AtomicInteger calls = new AtomicInteger();
		registry.registerEventListener(
				MembersDispatchEventNames.EVENT_SYNC_WECHAT_FANS,
				LISTENER_SYNC_WECHAT_FANS,
				ListenerDispatchOptions.async("slow", null),
				payload -> calls.incrementAndGet());

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		List<String> openIds = List.of("o1", "o2");
		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 42L);
		payload.put("authorizer_appid", "wx-appid");
		payload.put("count", 150);
		payload.put("open_ids", openIds);

		facade.publishEvent(
				MembersDispatchEventNames.EVENT_SYNC_WECHAT_FANS,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.EVENT, msg.messageType());
		assertEquals(LISTENER_SYNC_WECHAT_FANS, msg.listenerName());
		assertEquals("slow", msg.queue());
		assertEquals(MembersDispatchEventNames.EVENT_SYNC_WECHAT_FANS, msg.messageName());
		assertEquals(42L, msg.payload().get("company_id"));
		assertEquals("wx-appid", msg.payload().get("authorizer_appid"));
		assertEquals(150, ((Number) msg.payload().get("count")).intValue());
		assertEquals(openIds, msg.payload().get("open_ids"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);
		assertEquals(1, calls.get());
	}
}
