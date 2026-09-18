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

class CreateMemberSuccessEventDispatchFlowTest {

	@Test
	void publishEvent_async_enqueuesYoushuMemberListener_andConsumerRunsListener() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		AtomicInteger calls = new AtomicInteger();
		registry.registerEventListener(
				MembersDispatchEventNames.EVENT_CREATE_MEMBER_SUCCESS,
				"listener:youshu.member_create_success",
				ListenerDispatchOptions.asyncDefaults(),
				payload -> calls.incrementAndGet());

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 7L);
		payload.put("user_id", 99L);
		payload.put("mobile", "13800138000");
		payload.put("openid", "open-id");
		payload.put("wxa_appid", "");
		payload.put("inviter_id", 0L);
		payload.put("distributor_id", 0L);
		payload.put("source_id", 0L);
		payload.put("monitor_id", 0L);
		payload.put("salesperson_id", 0L);
		payload.put("if_register_promotion", true);

		facade.publishEvent(
				MembersDispatchEventNames.EVENT_CREATE_MEMBER_SUCCESS,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("listener:youshu.member_create_success", msg.listenerName());
		assertEquals("default", msg.queue());
		assertEquals(7L, msg.payload().get("company_id"));
		assertEquals(99L, msg.payload().get("user_id"));

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

	@Test
	void publishEvent_async_enqueuesYoushuAndPromotionsListeners_fanOutOrderAndConsumeBoth() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		AtomicInteger youshuCalls = new AtomicInteger();
		AtomicInteger promoCalls = new AtomicInteger();
		registry.registerEventListener(
				MembersDispatchEventNames.EVENT_CREATE_MEMBER_SUCCESS,
				"listener:youshu.member_create_success",
				ListenerDispatchOptions.asyncDefaults(),
				payload -> youshuCalls.incrementAndGet());
		registry.registerEventListener(
				MembersDispatchEventNames.EVENT_CREATE_MEMBER_SUCCESS,
				"listener:promotions.create_member_success",
				ListenerDispatchOptions.asyncDefaults(),
				payload -> promoCalls.incrementAndGet());

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 7L);
		payload.put("user_id", 99L);
		payload.put("mobile", "13800138000");
		payload.put("openid", "open-id");
		payload.put("wxa_appid", "");
		payload.put("inviter_id", 0L);
		payload.put("distributor_id", 0L);
		payload.put("source_id", 0L);
		payload.put("monitor_id", 0L);
		payload.put("salesperson_id", 0L);
		payload.put("if_register_promotion", true);

		facade.publishEvent(
				MembersDispatchEventNames.EVENT_CREATE_MEMBER_SUCCESS,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(2, captured.size());
		assertEquals("listener:youshu.member_create_success", captured.get(0).listenerName());
		assertEquals("default", captured.get(0).queue());
		assertEquals("listener:promotions.create_member_success", captured.get(1).listenerName());
		assertEquals("default", captured.get(1).queue());
		assertEquals(7L, captured.get(0).payload().get("company_id"));
		assertEquals(7L, captured.get(1).payload().get("company_id"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(captured.get(0), 1);
		runtime.consume(captured.get(1), 1);
		assertEquals(1, youshuCalls.get());
		assertEquals(1, promoCalls.get());
	}

	@Test
	void publishEvent_async_enqueuesYoushuPromotionsAndSendMembercardListeners_fanOutOrderAndConsumeAll() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		AtomicInteger youshuCalls = new AtomicInteger();
		AtomicInteger promoCalls = new AtomicInteger();
		AtomicInteger membercardCalls = new AtomicInteger();
		registry.registerEventListener(
				MembersDispatchEventNames.EVENT_CREATE_MEMBER_SUCCESS,
				"listener:youshu.member_create_success",
				ListenerDispatchOptions.asyncDefaults(),
				payload -> youshuCalls.incrementAndGet());
		registry.registerEventListener(
				MembersDispatchEventNames.EVENT_CREATE_MEMBER_SUCCESS,
				"listener:promotions.create_member_success",
				ListenerDispatchOptions.asyncDefaults(),
				payload -> promoCalls.incrementAndGet());
		registry.registerEventListener(
				MembersDispatchEventNames.EVENT_CREATE_MEMBER_SUCCESS,
				"listener:promotions.create_member_success_send_membercard",
				ListenerDispatchOptions.asyncDefaults(),
				payload -> membercardCalls.incrementAndGet());

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 7L);
		payload.put("user_id", 99L);
		payload.put("mobile", "13800138000");
		payload.put("openid", "open-id");
		payload.put("wxa_appid", "");
		payload.put("inviter_id", 0L);
		payload.put("distributor_id", 0L);
		payload.put("source_id", 0L);
		payload.put("monitor_id", 0L);
		payload.put("salesperson_id", 0L);
		payload.put("if_register_promotion", true);

		facade.publishEvent(
				MembersDispatchEventNames.EVENT_CREATE_MEMBER_SUCCESS,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(3, captured.size());
		assertEquals("listener:youshu.member_create_success", captured.get(0).listenerName());
		assertEquals("default", captured.get(0).queue());
		assertEquals("listener:promotions.create_member_success", captured.get(1).listenerName());
		assertEquals("default", captured.get(1).queue());
		assertEquals("listener:promotions.create_member_success_send_membercard", captured.get(2).listenerName());
		assertEquals("default", captured.get(2).queue());
		assertEquals(7L, captured.get(0).payload().get("company_id"));
		assertEquals(7L, captured.get(1).payload().get("company_id"));
		assertEquals(7L, captured.get(2).payload().get("company_id"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(captured.get(0), 1);
		runtime.consume(captured.get(1), 1);
		runtime.consume(captured.get(2), 1);
		assertEquals(1, youshuCalls.get());
		assertEquals(1, promoCalls.get());
		assertEquals(1, membercardCalls.get());
	}

	@Test
	void publishEvent_async_enqueuesYoushuPromotionsRegisterPointDatacubeNoticeAndSendMembercardListeners_fanOutOrderAndConsumeAll() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		AtomicInteger youshuCalls = new AtomicInteger();
		AtomicInteger promoCalls = new AtomicInteger();
		AtomicInteger registerPointCalls = new AtomicInteger();
		AtomicInteger datacubeCalls = new AtomicInteger();
		AtomicInteger noticeCalls = new AtomicInteger();
		AtomicInteger membercardCalls = new AtomicInteger();
		registry.registerEventListener(
				MembersDispatchEventNames.EVENT_CREATE_MEMBER_SUCCESS,
				"listener:youshu.member_create_success",
				ListenerDispatchOptions.asyncDefaults(),
				payload -> youshuCalls.incrementAndGet());
		registry.registerEventListener(
				MembersDispatchEventNames.EVENT_CREATE_MEMBER_SUCCESS,
				"listener:promotions.create_member_success",
				ListenerDispatchOptions.asyncDefaults(),
				payload -> promoCalls.incrementAndGet());
		registry.registerEventListener(
				MembersDispatchEventNames.EVENT_CREATE_MEMBER_SUCCESS,
				MembersDispatchEventNames.LISTENER_MEMBERS_REGISTER_POINT,
				ListenerDispatchOptions.asyncDefaults(),
				payload -> registerPointCalls.incrementAndGet());
		registry.registerEventListener(
				MembersDispatchEventNames.EVENT_CREATE_MEMBER_SUCCESS,
				MembersDispatchEventNames.LISTENER_DATACUBE_REGISTER_NUM_STATS,
				ListenerDispatchOptions.async("slow", null),
				payload -> datacubeCalls.incrementAndGet());
		registry.registerEventListener(
				MembersDispatchEventNames.EVENT_CREATE_MEMBER_SUCCESS,
				MembersDispatchEventNames.LISTENER_MEMBERS_CREATE_MEMBER_SUCCESS_NOTICE,
				ListenerDispatchOptions.async("slow", null),
				payload -> noticeCalls.incrementAndGet());
		registry.registerEventListener(
				MembersDispatchEventNames.EVENT_CREATE_MEMBER_SUCCESS,
				"listener:promotions.create_member_success_send_membercard",
				ListenerDispatchOptions.asyncDefaults(),
				payload -> membercardCalls.incrementAndGet());

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 7L);
		payload.put("user_id", 99L);
		payload.put("mobile", "13800138000");
		payload.put("openid", "open-id");
		payload.put("wxa_appid", "");
		payload.put("inviter_id", 0L);
		payload.put("distributor_id", 0L);
		payload.put("source_id", 0L);
		payload.put("monitor_id", 0L);
		payload.put("salesperson_id", 0L);
		payload.put("if_register_promotion", true);

		facade.publishEvent(
				MembersDispatchEventNames.EVENT_CREATE_MEMBER_SUCCESS,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(6, captured.size());
		assertEquals("listener:youshu.member_create_success", captured.get(0).listenerName());
		assertEquals("default", captured.get(0).queue());
		assertEquals("listener:promotions.create_member_success", captured.get(1).listenerName());
		assertEquals("default", captured.get(1).queue());
		assertEquals(MembersDispatchEventNames.LISTENER_MEMBERS_REGISTER_POINT, captured.get(2).listenerName());
		assertEquals("default", captured.get(2).queue());
		assertEquals(
				MembersDispatchEventNames.LISTENER_DATACUBE_REGISTER_NUM_STATS,
				captured.get(3).listenerName());
		assertEquals("slow", captured.get(3).queue());
		assertEquals(
				MembersDispatchEventNames.LISTENER_MEMBERS_CREATE_MEMBER_SUCCESS_NOTICE,
				captured.get(4).listenerName());
		assertEquals("slow", captured.get(4).queue());
		assertEquals("listener:promotions.create_member_success_send_membercard", captured.get(5).listenerName());
		assertEquals("default", captured.get(5).queue());
		assertEquals(7L, captured.get(0).payload().get("company_id"));
		assertEquals(7L, captured.get(1).payload().get("company_id"));
		assertEquals(7L, captured.get(2).payload().get("company_id"));
		assertEquals(7L, captured.get(3).payload().get("company_id"));
		assertEquals(7L, captured.get(4).payload().get("company_id"));
		assertEquals(7L, captured.get(5).payload().get("company_id"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(captured.get(0), 1);
		runtime.consume(captured.get(1), 1);
		runtime.consume(captured.get(2), 1);
		runtime.consume(captured.get(3), 1);
		runtime.consume(captured.get(4), 1);
		runtime.consume(captured.get(5), 1);
		assertEquals(1, youshuCalls.get());
		assertEquals(1, promoCalls.get());
		assertEquals(1, registerPointCalls.get());
		assertEquals(1, datacubeCalls.get());
		assertEquals(1, noticeCalls.get());
		assertEquals(1, membercardCalls.get());
	}
}
