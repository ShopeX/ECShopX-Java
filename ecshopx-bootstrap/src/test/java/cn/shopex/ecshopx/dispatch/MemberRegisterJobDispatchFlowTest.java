package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.ShuyunBundleDispatchJobNames;
import cn.shopex.ecshopx.members.dispatch.MemberRegisterJobDispatchPublisher;
import cn.shopex.ecshopx.members.dispatch.MemberRegisterJobHandler;
import cn.shopex.ecshopx.members.service.h5.auth.ShuyunLoginBridgeService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MemberRegisterJobDispatchFlowTest {

	@Test
	@DisplayName("dispatch enqueues default queue without delay and consumer invokes ShuyunLoginBridgeService#shuyunMemberSilent")
	void dispatchJob_async_enqueuesOnDefaultQueue_andConsumerInvokesShuyunMemberSilent() {
		ShuyunLoginBridgeService bridgeService = Mockito.mock(ShuyunLoginBridgeService.class);
		Mockito.when(bridgeService.shuyunMemberSilent(Mockito.any(), Mockito.any()))
				.thenReturn(Optional.of(Map.of("user_id", 1L)));

		MemberRegisterJobHandler handler = new MemberRegisterJobHandler(bridgeService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(ShuyunBundleDispatchJobNames.MEMBER_REGISTER_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = sampleMemberRegisterPayload();

		facade.dispatchJob(
				ShuyunBundleDispatchJobNames.MEMBER_REGISTER_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"default",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals("default", msg.queue());
		assertNull(msg.delay());
		assertEquals(ShuyunBundleDispatchJobNames.MEMBER_REGISTER_JOB, msg.messageName());
		assertNull(msg.listenerName());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(bridgeService)
				.shuyunMemberSilent(
						argThat(
								in -> in != null
										&& "wxa1".equals(in.get("appid"))
										&& Integer.valueOf(1).equals(in.get("source_id"))
										&& Integer.valueOf(2).equals(in.get("monitor_id"))
										&& Integer.valueOf(3).equals(in.get("inviter_id"))
										&& "src".equals(in.get("source_from"))
										&& "sy-app".equals(in.get("shuyunappid"))),
						argThat(
								br -> br != null
										&& 10L == asLong(br.get("company_id"))
										&& "oid".equals(br.get("open_id"))
										&& "uid".equals(br.get("unionid"))));
	}

	@Test
	void dispatchJob_publishPayloadMatchesMemberRegisterEnvelope() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		MemberRegisterJobDispatchPublisher publisher = new MemberRegisterJobDispatchPublisher(dispatchFacade);

		Map<String, Object> payload = sampleMemberRegisterPayload();
		publisher.enqueueMemberRegister(payload);

		verify(dispatchFacade)
				.dispatchJob(
						eq(ShuyunBundleDispatchJobNames.MEMBER_REGISTER_JOB),
						argThat(
								m ->
										m != null
												&& 10L == asLong(m.get("company_id"))
												&& 20L == asLong(m.get("user_id"))
												&& "13800000000".equals(m.get("mobile"))
												&& "uid".equals(m.get("unionid"))
												&& "oid".equals(m.get("open_id"))
												&& "wxa1".equals(m.get("appid"))
												&& Integer.valueOf(1).equals(m.get("source_id"))
												&& Integer.valueOf(2).equals(m.get("monitor_id"))
												&& Integer.valueOf(3).equals(m.get("inviter_id"))
												&& "src".equals(m.get("source_from"))
												&& "sy-app".equals(m.get("shuyunappid"))),
						argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.ASYNC
												&& opts.driverOverride() == DispatchDriverType.REDIS
												&& "default".equals(opts.queue())
												&& opts.delay() == null
												&& opts.retryPolicy() != null));
	}

	private static Map<String, Object> sampleMemberRegisterPayload() {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 10L);
		payload.put("user_id", 20L);
		payload.put("mobile", "13800000000");
		payload.put("unionid", "uid");
		payload.put("open_id", "oid");
		payload.put("appid", "wxa1");
		payload.put("source_id", 1);
		payload.put("monitor_id", 2);
		payload.put("inviter_id", 3);
		payload.put("source_from", "src");
		payload.put("shuyunappid", "sy-app");
		return payload;
	}

	private static long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}
}
