package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.WechatDispatchJobNames;
import cn.shopex.ecshopx.common.wechat.WechatOfflinePaidTemplateIds;
import cn.shopex.ecshopx.wechat.dispatch.SendTemplateMessageJobHandler;
import cn.shopex.ecshopx.wechat.service.WechatAuthQueryService;
import cn.shopex.ecshopx.wechat.service.WoaMpTemplateMessageClient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

class SendTemplateMessageJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesTemplateWorker() {
		WechatAuthQueryService auth = mock(WechatAuthQueryService.class);
		org.mockito.Mockito.when(auth.getAuthorizerAppidForWoaQuery(anyString())).thenReturn("wx-auth-app");
		WoaMpTemplateMessageClient client = mock(WoaMpTemplateMessageClient.class);
		SendTemplateMessageJobHandler handler = new SendTemplateMessageJobHandler(auth, client);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(WechatDispatchJobNames.SEND_TEMPLATE_MESSAGE_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> msgData = new LinkedHashMap<>();
		msgData.put("amount6", "12.34");
		msgData.put("time5", "2026-01-02 03:04:05");

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);
		payload.put("template_id", WechatOfflinePaidTemplateIds.SUPPLIER_OFFLINE_PAID_CONFIRM);
		payload.put("touser", "o-test-openid");
		payload.put("msg_data", msgData);

		facade.dispatchJob(
				WechatDispatchJobNames.SEND_TEMPLATE_MESSAGE_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("slow", msg.queue());
		assertEquals(WechatDispatchJobNames.SEND_TEMPLATE_MESSAGE_JOB, msg.messageName());

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(client)
				.sendTemplateMessage(
						eq("wx-auth-app"),
						eq(WechatOfflinePaidTemplateIds.SUPPLIER_OFFLINE_PAID_CONFIRM),
						eq("o-test-openid"),
						ArgumentMatchers.<Map<String, Object>>argThat(m -> "12.34".equals(m.get("amount6"))));
	}

	@Test
	void handler_logsFailureWithoutRethrowing_soConsumeAcks() {
		WechatAuthQueryService auth = mock(WechatAuthQueryService.class);
		org.mockito.Mockito.when(auth.getAuthorizerAppidForWoaQuery(anyString())).thenReturn("wx-auth-app");
		WoaMpTemplateMessageClient client = mock(WoaMpTemplateMessageClient.class);
		doThrow(new RuntimeException("downstream"))
				.when(client)
				.sendTemplateMessage(anyString(), anyString(), anyString(), any());
		SendTemplateMessageJobHandler handler = new SendTemplateMessageJobHandler(auth, client);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(WechatDispatchJobNames.SEND_TEMPLATE_MESSAGE_JOB, handler);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						WechatDispatchJobNames.SEND_TEMPLATE_MESSAGE_JOB,
						Map.of(
								"company_id",
								9L,
								"template_id",
								WechatOfflinePaidTemplateIds.SUPPLIER_OFFLINE_PAID_CONFIRM,
								"touser",
								"o-x",
								"msg_data",
								Map.of("amount6", "1.00")),
						"slow",
						null,
						RetryPolicy.platformDefault(),
						Instant.now(),
						"trace-test",
						null);

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(client).sendTemplateMessage(eq("wx-auth-app"), anyString(), eq("o-x"), any());
	}

	@Test
	void dispatchJob_whenAuthorizerAppIdMissing_handlerSilentlyCompletesWithoutWechatCall() {
		WechatAuthQueryService auth = mock(WechatAuthQueryService.class);
		org.mockito.Mockito.when(auth.getAuthorizerAppidForWoaQuery(anyString())).thenReturn(null);
		WoaMpTemplateMessageClient client = mock(WoaMpTemplateMessageClient.class);
		SendTemplateMessageJobHandler handler = new SendTemplateMessageJobHandler(auth, client);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(WechatDispatchJobNames.SEND_TEMPLATE_MESSAGE_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 2L);
		payload.put("template_id", WechatOfflinePaidTemplateIds.SUPPLIER_OFFLINE_PAID_CONFIRM);
		payload.put("touser", "o-no-appid");
		payload.put("msg_data", Map.of("amount6", "0.01"));

		facade.dispatchJob(
				WechatDispatchJobNames.SEND_TEMPLATE_MESSAGE_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		DispatchMessage msg = captured.get(0);
		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(client, never()).sendTemplateMessage(any(), any(), any(), any());
	}
}
