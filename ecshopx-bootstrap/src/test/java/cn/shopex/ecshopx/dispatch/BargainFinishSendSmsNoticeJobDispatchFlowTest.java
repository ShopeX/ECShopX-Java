package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.PromotionsDispatchJobNames;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.promotions.dispatch.BargainFinishSendSmsNoticeJobHandler;
import cn.shopex.ecshopx.promotions.service.SmsSendTestService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BargainFinishSendSmsNoticeJobDispatchFlowTest {

	private static final DateTimeFormatter END_TIME =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

	@Test
	void dispatchJob_async_enqueuesOnSmsQueue_andHandlerSendsBargainFinishTemplate() {
		SmsSendTestService sms = mock(SmsSendTestService.class);
		MemberAccountService members = mock(MemberAccountService.class);
		when(members.resolveMemberMobileForH5Context(anyLong(), anyLong())).thenReturn("13900001001");
		BargainFinishSendSmsNoticeJobHandler handler = new BargainFinishSendSmsNoticeJobHandler(members, sms);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(PromotionsDispatchJobNames.BARGAIN_FINISH_SEND_SMS_NOTICE, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long promotionEndSec = 1_714_521_600L;
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);
		payload.put("bargain_owner_user_id", 20L);
		payload.put("promotion_end_time_sec", promotionEndSec);
		payload.put("locale_language_tag", "zh-CN");
		payload.put("item_name", "演示商品");
		payload.put("price", 9_900);

		facade.dispatchJob(
				PromotionsDispatchJobNames.BARGAIN_FINISH_SEND_SMS_NOTICE,
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
		assertNull(msg.delay());
		assertNull(msg.listenerName());
		assertEquals(PromotionsDispatchJobNames.BARGAIN_FINISH_SEND_SMS_NOTICE, msg.messageName());
		assertEquals(1L, msg.payload().get("company_id"));
		assertEquals(20L, msg.payload().get("bargain_owner_user_id"));
		assertEquals("演示商品", msg.payload().get("item_name"));
		assertEquals(9_900, ((Number) msg.payload().get("price")).intValue());

		String expectedEnd = END_TIME.format(Instant.ofEpochSecond(promotionEndSec));

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
						eq("bargainFinish_notice"),
						argThat(
								m ->
										m != null
												&& "演示商品".equals(m.get("item_name"))
												&& "99.00".equals(m.get("pay_money"))
												&& expectedEnd.equals(m.get("end_time"))));
	}

	@Test
	void handler_logsSmsFailureWithoutRethrowing_soConsumeAcks() {
		SmsSendTestService sms = mock(SmsSendTestService.class);
		MemberAccountService members = mock(MemberAccountService.class);
		when(members.resolveMemberMobileForH5Context(anyLong(), anyLong())).thenReturn("13900000000");
		doThrow(new RuntimeException("gateway down"))
				.when(sms)
				.sendTemplatedNoticeSms(anyLong(), anyString(), anyString(), any());
		BargainFinishSendSmsNoticeJobHandler handler = new BargainFinishSendSmsNoticeJobHandler(members, sms);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(PromotionsDispatchJobNames.BARGAIN_FINISH_SEND_SMS_NOTICE, handler);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						PromotionsDispatchJobNames.BARGAIN_FINISH_SEND_SMS_NOTICE,
						Map.of(
								"company_id",
								9L,
								"bargain_owner_user_id",
								20L,
								"promotion_end_time_sec",
								1_714_521_600L,
								"locale_language_tag",
								"zh-CN",
								"item_name",
								"x",
								"price",
								100),
						"sms",
						null,
						RetryPolicy.platformDefault(),
						Instant.now(),
						"trace-bargain-sms",
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
}
