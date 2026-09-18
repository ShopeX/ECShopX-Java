package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.KaquanDispatchJobNames;
import cn.shopex.ecshopx.kaquan.dispatch.UploadWechatCardJobHandler;
import cn.shopex.ecshopx.kaquan.service.discount.UploadWechatCardWorker;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UploadWechatCardJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnDefaultQueue_andConsumerInvokesWorker() {
		UploadWechatCardWorker worker = mock(UploadWechatCardWorker.class);
		UploadWechatCardJobHandler handler = new UploadWechatCardJobHandler(worker);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(KaquanDispatchJobNames.UPLOAD_WECHAT_CARD, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		List<Long> cardIds = List.of(10L, 20L);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("authorizer_appid", "wx-app");
		payload.put("company_id", 1L);
		payload.put("card_ids", cardIds);

		facade.dispatchJob(
				KaquanDispatchJobNames.UPLOAD_WECHAT_CARD,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"default",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("default", msg.queue());
		assertEquals(KaquanDispatchJobNames.UPLOAD_WECHAT_CARD, msg.messageName());

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(worker).execute(eq("wx-app"), eq(1L), eq(cardIds));
	}

	@Test
	void handler_logsFailureWithoutRethrowing_soConsumeAcks() {
		UploadWechatCardWorker worker = mock(UploadWechatCardWorker.class);
		doThrow(new RuntimeException("downstream"))
				.when(worker)
				.execute(anyString(), anyLong(), anyList());
		UploadWechatCardJobHandler handler = new UploadWechatCardJobHandler(worker);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(KaquanDispatchJobNames.UPLOAD_WECHAT_CARD, handler);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						KaquanDispatchJobNames.UPLOAD_WECHAT_CARD,
						Map.of(
								"authorizer_appid",
								"x",
								"company_id",
								9L,
								"card_ids",
								List.of(3L)),
						"default",
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

		verify(worker).execute(eq("x"), eq(9L), eq(List.of(3L)));
	}

	@Test
	void dispatchJob_withNullAuthorizerAppId_passesNullToWorker() {
		UploadWechatCardWorker worker = mock(UploadWechatCardWorker.class);
		UploadWechatCardJobHandler handler = new UploadWechatCardJobHandler(worker);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(KaquanDispatchJobNames.UPLOAD_WECHAT_CARD, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("authorizer_appid", null);
		payload.put("company_id", 2L);
		payload.put("card_ids", List.of(5L));

		facade.dispatchJob(
				KaquanDispatchJobNames.UPLOAD_WECHAT_CARD,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"default",
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

		verify(worker).execute(isNull(), eq(2L), eq(List.of(5L)));
	}
}
