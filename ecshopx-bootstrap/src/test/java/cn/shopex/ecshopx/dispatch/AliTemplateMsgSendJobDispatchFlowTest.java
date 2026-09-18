package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.ali.service.alitemplate.AliOpenTemplateLibraryRedisAccessor;
import cn.shopex.ecshopx.ali.service.alitemplate.AliTemplateMsgService;
import cn.shopex.ecshopx.common.dispatch.PromotionsDispatchJobNames;
import cn.shopex.ecshopx.config.AliTemplateMsgSendDispatchPublisherImpl;
import cn.shopex.ecshopx.promotions.dispatch.AliTemplateMsgSendJobHandler;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AliTemplateMsgSendJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnDefaultQueue_andConsumerInvokesAliTemplateSend() {
		AliTemplateMsgService sendService = mock(AliTemplateMsgService.class);
		AliTemplateMsgSendJobHandler handler = new AliTemplateMsgSendJobHandler(sendService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(PromotionsDispatchJobNames.ALI_TEMPLATE_MSG_SEND, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> innerData = new LinkedHashMap<>();
		innerData.put("item_name", "SKU-1");
		innerData.put("notice", "到货");

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("scenes_name", "goodsArrivalNotice");
		payload.put("company_id", 42L);
		payload.put("to_user_id", "2088-open-test");
		payload.put("page_query_str", "id=99");
		payload.put("data", innerData);
		payload.put("is_force_fire", true);

		facade.dispatchJob(
				PromotionsDispatchJobNames.ALI_TEMPLATE_MSG_SEND,
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
		assertEquals(PromotionsDispatchJobNames.ALI_TEMPLATE_MSG_SEND, msg.messageName());

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(sendService).send(any(), eq(true));
	}

	@Test
	void dispatchJob_delayed_whenListenerForceFireFalse_andTemplateSendTimeDescPresent() {
		DispatchFacade facade = mock(DispatchFacade.class);
		AliOpenTemplateLibraryRedisAccessor accessor = mock(AliOpenTemplateLibraryRedisAccessor.class);
		when(accessor.resolveDispatchDelayMinutesFromTemplate(7, "goodsArrivalNotice"))
				.thenReturn(Optional.of(Duration.ofMinutes(2)));

		AliTemplateMsgSendDispatchPublisherImpl publisher = new AliTemplateMsgSendDispatchPublisherImpl(facade, accessor);
		Map<String, Object> sendBody = new LinkedHashMap<>();
		sendBody.put("scenes_name", "goodsArrivalNotice");
		sendBody.put("company_id", 7L);
		sendBody.put("to_user_id", "2088x");
		sendBody.put("data", Map.of("item_name", "n", "notice", "x"));
		publisher.publish(sendBody, false);

		ArgumentCaptor<DispatchOptions> optCap = ArgumentCaptor.forClass(DispatchOptions.class);
		verify(facade).dispatchJob(eq(PromotionsDispatchJobNames.ALI_TEMPLATE_MSG_SEND), any(), optCap.capture());
		assertEquals(Duration.ofSeconds(120L), optCap.getValue().delay());
		assertEquals("default", optCap.getValue().queue());
		assertEquals(DispatchMode.ASYNC, optCap.getValue().mode());
		assertEquals(DispatchDriverType.REDIS, optCap.getValue().driverOverride());
	}

	@Test
	void handler_whenSendServiceThrows_logsSendFailureWithoutRethrowing_soConsumeAcks() {
		AliTemplateMsgService sendService = mock(AliTemplateMsgService.class);
		doThrow(new RuntimeException("downstream"))
				.when(sendService)
				.send(any(), any(Boolean.class));
		AliTemplateMsgSendJobHandler handler = new AliTemplateMsgSendJobHandler(sendService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(PromotionsDispatchJobNames.ALI_TEMPLATE_MSG_SEND, handler);

		Map<String, Object> innerData =
				Map.of("item_name", "x", "notice", "y");
		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						PromotionsDispatchJobNames.ALI_TEMPLATE_MSG_SEND,
						Map.of(
								"company_id",
								1L,
								"scenes_name",
								"goodsArrivalNotice",
								"to_user_id",
								"2088",
								"page_query_str",
								"id=1",
								"data",
								innerData,
								"is_force_fire",
								true),
						"default",
						null,
						RetryPolicy.platformDefault(),
						Instant.now(),
						"trace-ali-tpl",
						null);

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(sendService).send(any(), eq(true));
	}
}
