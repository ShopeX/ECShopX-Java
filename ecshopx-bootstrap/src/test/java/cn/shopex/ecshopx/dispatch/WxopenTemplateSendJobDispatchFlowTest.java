package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.PromotionsDispatchJobNames;
import cn.shopex.ecshopx.promotions.dispatch.WxopenTemplateSendJobHandler;
import cn.shopex.ecshopx.promotions.service.WxaTemplateMsgActivityRemindSendService;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WxopenTemplateSendJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesSendService() {
		WxaTemplateMsgActivityRemindSendService sendService = mock(WxaTemplateMsgActivityRemindSendService.class);
		WxopenTemplateSendJobHandler handler = new WxopenTemplateSendJobHandler(sendService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(PromotionsDispatchJobNames.WXOPEN_TEMPLATE_SEND, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> innerData = new LinkedHashMap<>();
		innerData.put("activity_name", "Spring Fair");
		innerData.put("activity_start_time", "2026-05-07 10:00:00");
		innerData.put("activity_end_time", "2026-05-07 18:00:00");
		innerData.put("activity_address", "Hall A");

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("scenes_name", "registrationActivityNotice");
		payload.put("company_id", 42L);
		payload.put("appid", "wx-app-test");
		payload.put("openid", "o-open-test");
		payload.put("page_query_str", "record_id=99");
		payload.put("data", innerData);
		payload.put("is_force_fire", true);

		facade.dispatchJob(
				PromotionsDispatchJobNames.WXOPEN_TEMPLATE_SEND,
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
		assertEquals(PromotionsDispatchJobNames.WXOPEN_TEMPLATE_SEND, msg.messageName());

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(sendService)
				.send(
						argThat(m -> {
							if (!"registrationActivityNotice".equals(m.get("scenes_name"))) {
								return false;
							}
							if (!Long.valueOf(42L).equals(toLong(m.get("company_id")))) {
								return false;
							}
							if (!"wx-app-test".equals(m.get("appid"))) {
								return false;
							}
							if (!"o-open-test".equals(m.get("openid"))) {
								return false;
							}
							if (!"record_id=99".equals(m.get("page_query_str"))) {
								return false;
							}
							Object dataObj = m.get("data");
							if (!(dataObj instanceof Map<?, ?> dm)) {
								return false;
							}
							return "Spring Fair".equals(dm.get("activity_name"))
									&& "2026-05-07 10:00:00".equals(dm.get("activity_start_time"))
									&& "2026-05-07 18:00:00".equals(dm.get("activity_end_time"))
									&& "Hall A".equals(dm.get("activity_address"));
						}),
						eq(true));
	}

	@Test
	void dispatchJob_delayed_enqueuesSlowQueueWithDelay_andConsumerInvokesSendServiceWithForceFire() {
		WxaTemplateMsgActivityRemindSendService sendService = mock(WxaTemplateMsgActivityRemindSendService.class);
		WxopenTemplateSendJobHandler handler = new WxopenTemplateSendJobHandler(sendService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(PromotionsDispatchJobNames.WXOPEN_TEMPLATE_SEND, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Duration delay = Duration.ofSeconds(120);
		Map<String, Object> innerData = new LinkedHashMap<>();
		innerData.put("order_id", "9001");
		innerData.put("refund_fee", "12元");
		innerData.put("remarks", "您的售后已审核成功，请填写回寄物流！");

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("scenes_name", "aftersalesSuccess");
		payload.put("company_id", 42L);
		payload.put("appid", "wx-app-test");
		payload.put("openid", "o-open-test");
		payload.put("data", innerData);
		payload.put("is_force_fire", true);

		facade.dispatchJob(
				PromotionsDispatchJobNames.WXOPEN_TEMPLATE_SEND,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						delay,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("slow", msg.queue());
		assertEquals(PromotionsDispatchJobNames.WXOPEN_TEMPLATE_SEND, msg.messageName());
		assertEquals(delay, msg.delay());

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(sendService)
				.send(
						argThat(m -> {
							if (!"aftersalesSuccess".equals(m.get("scenes_name"))) {
								return false;
							}
							if (!Long.valueOf(42L).equals(toLong(m.get("company_id")))) {
								return false;
							}
							Object dataObj = m.get("data");
							if (!(dataObj instanceof Map<?, ?> dm)) {
								return false;
							}
							return "9001".equals(dm.get("order_id"))
									&& "12元".equals(dm.get("refund_fee"))
									&& "您的售后已审核成功，请填写回寄物流！".equals(dm.get("remarks"));
						}),
						eq(true));
	}

	@Test
	void handler_whenSendServiceThrows_logsAndDoesNotPropagate() {
		WxaTemplateMsgActivityRemindSendService sendService = mock(WxaTemplateMsgActivityRemindSendService.class);
		doThrow(new RuntimeException("downstream"))
				.when(sendService)
				.send(org.mockito.ArgumentMatchers.anyMap(), org.mockito.ArgumentMatchers.anyBoolean());
		WxopenTemplateSendJobHandler handler = new WxopenTemplateSendJobHandler(sendService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(PromotionsDispatchJobNames.WXOPEN_TEMPLATE_SEND, handler);

		Map<String, Object> innerData =
				Map.of(
						"activity_name",
						"x",
						"activity_start_time",
						"y",
						"activity_end_time",
						"z",
						"activity_address",
						"w");
		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						PromotionsDispatchJobNames.WXOPEN_TEMPLATE_SEND,
						Map.of(
								"company_id",
								1L,
								"scenes_name",
								"registrationActivityNotice",
								"appid",
								"a",
								"openid",
								"b",
								"page_query_str",
								"record_id=1",
								"data",
								innerData,
								"is_force_fire",
								true),
						"slow",
						null,
						RetryPolicy.platformDefault(),
						Instant.now(),
						"trace-wxopen",
						null);

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(sendService).send(org.mockito.ArgumentMatchers.anyMap(), eq(true));
	}

	@Test
	void dispatchJob_paymentSucc_delayed_enqueuesWithDelay_andHandlerUsesForceFire() {
		WxaTemplateMsgActivityRemindSendService sendService = mock(WxaTemplateMsgActivityRemindSendService.class);
		WxopenTemplateSendJobHandler handler = new WxopenTemplateSendJobHandler(sendService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(PromotionsDispatchJobNames.WXOPEN_TEMPLATE_SEND, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Duration delay = Duration.ofMinutes(3);
		Map<String, Object> innerData = new LinkedHashMap<>();
		innerData.put("pay_money", "12.50");
		innerData.put("pay_date", "2026-05-08 10:00:00");
		innerData.put("item_name", "Gift");
		innerData.put("shop_name", "Demo");
		innerData.put("order_id", "9001");
		innerData.put("trade_id", "tr-501");
		innerData.put("receipt_type", "物流配送");
		innerData.put("pay_type", "微信支付");

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("scenes_name", "paymentSucc");
		payload.put("company_id", 42L);
		payload.put("appid", "wx-app-test");
		payload.put("openid", "o-open-test");
		payload.put("data", innerData);
		payload.put("is_force_fire", true);

		facade.dispatchJob(
				PromotionsDispatchJobNames.WXOPEN_TEMPLATE_SEND,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						delay,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("slow", msg.queue());
		assertEquals(PromotionsDispatchJobNames.WXOPEN_TEMPLATE_SEND, msg.messageName());
		assertEquals(delay, msg.delay());

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(sendService)
				.send(
						argThat(m -> {
							if (!"paymentSucc".equals(m.get("scenes_name"))) {
								return false;
							}
							if (!Long.valueOf(42L).equals(toLong(m.get("company_id")))) {
								return false;
							}
							Object dataObj = m.get("data");
							if (!(dataObj instanceof Map<?, ?> dm)) {
								return false;
							}
							return "12.50".equals(dm.get("pay_money"))
									&& "2026-05-08 10:00:00".equals(dm.get("pay_date"))
									&& "Gift".equals(dm.get("item_name"))
									&& "Demo".equals(dm.get("shop_name"))
									&& "9001".equals(dm.get("order_id"))
									&& "tr-501".equals(dm.get("trade_id"))
									&& "物流配送".equals(dm.get("receipt_type"))
									&& "微信支付".equals(dm.get("pay_type"));
						}),
						eq(true));
	}

	private static Long toLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw));
	}
}
