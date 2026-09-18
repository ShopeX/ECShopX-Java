package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.DepositDispatchJobNames;
import cn.shopex.ecshopx.promotions.dispatch.RechargeSendSmsNoticeJobHandler;
import cn.shopex.ecshopx.promotions.service.SmsSendTestService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class RechargeSendSmsNoticeJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSmsQueue_andConsumerInvokesDepositRechargeTemplate() {
		SmsSendTestService sms = mock(SmsSendTestService.class);
		StringRedisTemplate redis = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
		when(redis.opsForHash()).thenReturn(hashOps);
		when(hashOps.get("shopDepositTotal", "10")).thenReturn("15000");

		RechargeSendSmsNoticeJobHandler handler = new RechargeSendSmsNoticeJobHandler(sms, redis);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(DepositDispatchJobNames.RECHARGE_SEND_SMS_NOTICE, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("companyId", 10L);
		payload.put("userId", 20L);
		payload.put("mobile", "13900001001");
		payload.put("totalFee", 5000L);

		facade.dispatchJob(
				DepositDispatchJobNames.RECHARGE_SEND_SMS_NOTICE,
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
		assertEquals(DepositDispatchJobNames.RECHARGE_SEND_SMS_NOTICE, msg.messageName());

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(sms)
				.sendTemplatedNoticeSms(
						eq(10L),
						eq("13900001001"),
						eq("deposit_recharge"),
						argThat(
								m ->
										m != null
												&& "50.00".equals(m.get("recharge_money"))
												&& "150.00".equals(m.get("deposit_money"))
												&& m.get("recharge_date") != null
												&& !m.get("recharge_date").isEmpty()));
	}

	@Test
	void dispatchJob_payloadContainsCompanyUserMobileTotalFee() {
		SmsSendTestService sms = mock(SmsSendTestService.class);
		StringRedisTemplate redis = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
		when(redis.opsForHash()).thenReturn(hashOps);
		when(hashOps.get("shopDepositTotal", "7")).thenReturn("0");

		RechargeSendSmsNoticeJobHandler handler = new RechargeSendSmsNoticeJobHandler(sms, redis);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(DepositDispatchJobNames.RECHARGE_SEND_SMS_NOTICE, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("companyId", 7L);
		payload.put("userId", 8L);
		payload.put("mobile", "13800138000");
		payload.put("totalFee", 12345L);

		facade.dispatchJob(
				DepositDispatchJobNames.RECHARGE_SEND_SMS_NOTICE,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"sms",
						null,
						RetryPolicy.platformDefault()));

		DispatchMessage msg = captured.get(0);
		assertEquals(7L, ((Number) msg.payload().get("companyId")).longValue());
		assertEquals(8L, ((Number) msg.payload().get("userId")).longValue());
		assertEquals("13800138000", msg.payload().get("mobile"));
		assertEquals(12345L, ((Number) msg.payload().get("totalFee")).longValue());
	}
}
