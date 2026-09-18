package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.aliyunsms.integration.members.AdminMemberMassSmsFanOutDispatchPortImpl;
import cn.shopex.ecshopx.aliyunsms.integration.members.GroupSendSmsJobDispatchPublisher;
import cn.shopex.ecshopx.aliyunsms.integration.members.GroupSendSmsJobHandler;
import cn.shopex.ecshopx.common.dispatch.MembersBundleDispatchJobNames;
import cn.shopex.ecshopx.common.members.admin.MemberBatchFanOutSmsOutboundPort;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName(
		"Group-send SMS job dispatch flow: batch async chunk paths share the same dispatchJob bus boundary as inline sends")
class GroupSendSmsJobDispatchFlowTest {

	@Test
	@DisplayName(
			"dispatch enqueues sms queue without delay and consumer invokes MemberBatchFanOutSmsOutboundPort per mobile")
	void dispatchJob_async_enqueuesOnSmsQueue_andConsumerInvokesFanOutPortPerMobile() {
		MemberBatchFanOutSmsOutboundPort fanOut = Mockito.mock(MemberBatchFanOutSmsOutboundPort.class);
		GroupSendSmsJobHandler handler = new GroupSendSmsJobHandler(fanOut);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(MembersBundleDispatchJobNames.GROUP_SEND_SMS_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = sampleGroupSendSmsPayload();

		facade.dispatchJob(
				MembersBundleDispatchJobNames.GROUP_SEND_SMS_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"sms",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals("sms", msg.queue());
		assertNull(msg.delay());
		assertEquals(MembersBundleDispatchJobNames.GROUP_SEND_SMS_JOB, msg.messageName());
		assertNull(msg.listenerName());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(fanOut).sendOne(100L, "13800138000", "hello body");
		verify(fanOut).sendOne(100L, "13900139000", "hello body");
	}

	@Test
	@DisplayName("publisher forwards group-send payload and async REDIS sms DispatchOptions to DispatchFacade")
	void dispatchJob_publishPayloadMatchesGroupSendSmsEnvelope() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		GroupSendSmsJobDispatchPublisher publisher = new GroupSendSmsJobDispatchPublisher(dispatchFacade);

		Map<String, Object> payload = sampleGroupSendSmsPayload();
		publisher.enqueueGroupSendSms(payload);

		verify(dispatchFacade)
				.dispatchJob(
						eq(MembersBundleDispatchJobNames.GROUP_SEND_SMS_JOB),
						argThat(
								m ->
										m != null
												&& 100L == asLong(m.get("company_id"))
												&& m.get("send_to_phones") instanceof List<?> phones
												&& phones.size() == 2
												&& "13800138000".equals(phones.get(0))
												&& "13900139000".equals(phones.get(1))
												&& "hello body".equals(m.get("sms_content"))
												&& "管理员".equals(m.get("operator"))
												&& "jwt-sender".equals(m.get("sender"))
												&& 7L == asLong(m.get("distributor_id"))),
						argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.ASYNC
												&& opts.driverOverride() == DispatchDriverType.REDIS
												&& "sms".equals(opts.queue())
												&& opts.delay() == null
												&& opts.retryPolicy() != null));
	}

	@Test
	@DisplayName("admin mass SMS fan-out port uses same job envelope; consumer fans out per mobile")
	void adminFanOutPort_enqueueThenConsumerInvokesFanOutPerMobile() {
		MemberBatchFanOutSmsOutboundPort fanOut = Mockito.mock(MemberBatchFanOutSmsOutboundPort.class);
		GroupSendSmsJobHandler handler = new GroupSendSmsJobHandler(fanOut);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(MembersBundleDispatchJobNames.GROUP_SEND_SMS_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));
		GroupSendSmsJobDispatchPublisher publisher = new GroupSendSmsJobDispatchPublisher(facade);
		AdminMemberMassSmsFanOutDispatchPortImpl port = new AdminMemberMassSmsFanOutDispatchPortImpl(publisher);

		port.dispatchFanOutAfterPersist(200L, List.of("13011112222", "13022223333"), "batch body");

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("sms", msg.queue());
		assertEquals(MembersBundleDispatchJobNames.GROUP_SEND_SMS_JOB, msg.messageName());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(fanOut).sendOne(200L, "13011112222", "batch body");
		verify(fanOut).sendOne(200L, "13022223333", "batch body");
	}

	private static Map<String, Object> sampleGroupSendSmsPayload() {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 100L);
		payload.put("send_to_phones", List.of("13800138000", "13900139000"));
		payload.put("sms_content", "hello body");
		payload.put("operator", "管理员");
		payload.put("sender", "jwt-sender");
		payload.put("distributor_id", 7L);
		return payload;
	}

	private static long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}
}
