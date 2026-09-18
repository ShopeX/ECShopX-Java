package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.ReservationDispatchJobNames;
import cn.shopex.ecshopx.promotions.dispatch.ReservationSendSmsNoticeJobHandler;
import cn.shopex.ecshopx.promotions.service.SmsSendTestService;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReservationSendSmsNoticeJobDispatchFlowTest {

	private static final DateTimeFormatter DATE_TIME =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	@Test
	void dispatchJob_async_enqueuesOnSmsQueue_andConsumerInvokesSendTemplatedNoticeSms() {
		SmsSendTestService sms = mock(SmsSendTestService.class);
		ReservationSendSmsNoticeJobHandler handler = new ReservationSendSmsNoticeJobHandler(sms);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(ReservationDispatchJobNames.RESERVATION_SEND_SMS_NOTICE, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long epochSec = 1_714_521_600L;
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);
		payload.put("shop_id", 2L);
		payload.put("to_shop_time", (int) epochSec);
		payload.put("shop_name", "演示门店");
		payload.put("rights_name", "体验权益");
		payload.put("mobile", "13900001001");
		payload.put("record_id", 100L);
		payload.put("status", "success");
		payload.put("shop_address", "示例地址");
		payload.put("telephone", "021-12345678");
		payload.put("user_name", "张三");
		payload.put("setting_data", Map.of("reservationMode", 1));

		facade.dispatchJob(
				ReservationDispatchJobNames.RESERVATION_SEND_SMS_NOTICE,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"sms",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("sms", msg.queue());
		assertEquals(ReservationDispatchJobNames.RESERVATION_SEND_SMS_NOTICE, msg.messageName());
		assertEquals(1L, msg.payload().get("company_id"));
		assertEquals("演示门店", msg.payload().get("shop_name"));
		assertEquals("13900001001", msg.payload().get("mobile"));
		assertEquals(100L, msg.payload().get("record_id"));

		String expectedDate = DATE_TIME.format(Instant.ofEpochSecond(epochSec));

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(sms)
				.sendTemplatedNoticeSms(
						eq(1L),
						eq("13900001001"),
						eq("reservation_notice"),
						argThat(
								m ->
										m != null
												&& expectedDate.equals(m.get("date"))
												&& "演示门店".equals(m.get("shop_name"))
												&& "体验权益".equals(m.get("rights_name"))
												&& "示例地址".equals(m.get("shop_address"))
												&& "021-12345678".equals(m.get("telephone"))
												&& "张三".equals(m.get("user_name"))));
	}

	@Test
	void dispatchJob_job70_delayed_enqueuesSmsQueueWithDelay_andHandlerSendsGotoShopTemplate() {
		SmsSendTestService sms = mock(SmsSendTestService.class);
		ReservationSendSmsNoticeJobHandler handler = new ReservationSendSmsNoticeJobHandler(sms);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(ReservationDispatchJobNames.RESERVATION_SEND_SMS_NOTICE, handler);
		registry.registerJob(ReservationDispatchJobNames.RESERVATION_GOTO_SHOP_SMS_NOTICE_DELAYED, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long epochSec = 1_714_521_600L;
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);
		payload.put("shop_id", 2L);
		payload.put("to_shop_time", (int) epochSec);
		payload.put("shop_name", "演示门店");
		payload.put("rights_name", "体验权益");
		payload.put("mobile", "13900001001");
		payload.put("record_id", 100L);
		payload.put("status", "success");
		payload.put("shop_address", "示例地址");
		payload.put("telephone", "021-12345678");
		payload.put("user_name", "张三");
		payload.put("setting_data", Map.of("reservationMode", 1));
		payload.put("sms_template_name", "gotoShop_notice");

		Duration delay = Duration.ofSeconds(3600);
		facade.dispatchJob(
				ReservationDispatchJobNames.RESERVATION_GOTO_SHOP_SMS_NOTICE_DELAYED,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"sms",
						delay,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("sms", msg.queue());
		assertEquals(delay, msg.delay());
		assertEquals(ReservationDispatchJobNames.RESERVATION_GOTO_SHOP_SMS_NOTICE_DELAYED, msg.messageName());
		assertEquals("gotoShop_notice", msg.payload().get("sms_template_name"));

		String expectedDate = DATE_TIME.format(Instant.ofEpochSecond(epochSec));

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(sms)
				.sendTemplatedNoticeSms(
						eq(1L),
						eq("13900001001"),
						eq("gotoShop_notice"),
						argThat(
								m ->
										m != null
												&& expectedDate.equals(m.get("date"))
												&& "演示门店".equals(m.get("shop_name"))
												&& "体验权益".equals(m.get("rights_name"))
												&& "示例地址".equals(m.get("shop_address"))
												&& "021-12345678".equals(m.get("telephone"))
												&& "张三".equals(m.get("user_name"))));
	}

	@Test
	void handler_logsSmsFailureWithoutRethrowing_soConsumeAcks() {
		SmsSendTestService sms = mock(SmsSendTestService.class);
		doThrow(new RuntimeException("gateway down"))
				.when(sms)
				.sendTemplatedNoticeSms(anyLong(), anyString(), anyString(), any());
		ReservationSendSmsNoticeJobHandler handler = new ReservationSendSmsNoticeJobHandler(sms);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(ReservationDispatchJobNames.RESERVATION_SEND_SMS_NOTICE, handler);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						ReservationDispatchJobNames.RESERVATION_SEND_SMS_NOTICE,
						Map.of(
								"company_id",
								9L,
								"mobile",
								"13900000000",
								"to_shop_time",
								1_714_521_600,
								"shop_name",
								"x",
								"rights_name",
								"y"),
						"sms",
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

		verify(sms).sendTemplatedNoticeSms(anyLong(), anyString(), anyString(), any());
	}

	@Test
	void dispatchJob_withAdminStylePassthroughKeys_forwardsShopAddressAndTelephoneToTemplateVars() {
		SmsSendTestService sms = mock(SmsSendTestService.class);
		ReservationSendSmsNoticeJobHandler handler = new ReservationSendSmsNoticeJobHandler(sms);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(ReservationDispatchJobNames.RESERVATION_SEND_SMS_NOTICE, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long epochSec = 1_716_691_800L;
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 77L);
		payload.put("shop_id", 42L);
		payload.put("to_shop_time", (int) epochSec);
		payload.put("shop_name", "商家后台门店");
		payload.put("rights_name", "预约权益");
		payload.put("mobile", "13800138000");
		payload.put("record_id", 9001L);
		payload.put("status", "success");
		payload.put("shop_address", "上海市浦东新区后台路 1 号");
		payload.put("telephone", "021-50888888");
		payload.put("setting_data", Map.of("reservationMode", 0));

		facade.dispatchJob(
				ReservationDispatchJobNames.RESERVATION_SEND_SMS_NOTICE,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"sms",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("sms", msg.queue());

		String expectedDate = DATE_TIME.format(Instant.ofEpochSecond(epochSec));

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(sms)
				.sendTemplatedNoticeSms(
						eq(77L),
						eq("13800138000"),
						eq("reservation_notice"),
						argThat(
								m ->
										m != null
												&& expectedDate.equals(m.get("date"))
												&& "商家后台门店".equals(m.get("shop_name"))
												&& "预约权益".equals(m.get("rights_name"))
												&& "上海市浦东新区后台路 1 号".equals(m.get("shop_address"))
												&& "021-50888888".equals(m.get("telephone"))));
	}
}
