package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.CreateDistributorJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThemeDispatchJobNames;
import cn.shopex.ecshopx.config.CreateDistributorJobDispatchPublisherImpl;
import cn.shopex.ecshopx.distribution.service.PagesTemplateNewDistributorFacade;
import cn.shopex.ecshopx.theme.dispatch.CreateDistributorJobHandler;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CreateDistributorJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesFacadeWithRowPayload() throws java.lang.Exception {
		PagesTemplateNewDistributorFacade pagesFacade = mock(PagesTemplateNewDistributorFacade.class);
		CreateDistributorJobHandler handler = new CreateDistributorJobHandler(pagesFacade);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(ThemeDispatchJobNames.CREATE_DISTRIBUTOR_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade dispatchFacade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 11L);
		payload.put("distributor_id", 22L);
		payload.put("name", "Test Shop");

		dispatchFacade.dispatchJob(
				ThemeDispatchJobNames.CREATE_DISTRIBUTOR_JOB,
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
		assertEquals(ThemeDispatchJobNames.CREATE_DISTRIBUTOR_JOB, msg.messageName());
		assertEquals(11L, ((Number) msg.payload().get("company_id")).longValue());
		assertEquals(22L, ((Number) msg.payload().get("distributor_id")).longValue());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(pagesFacade, times(1))
				.applyDefaultTemplatesForNewDistributor(
						argThat(
								row ->
										row != null
												&& Long.valueOf(11L).equals(toLong(row.get("company_id")))
												&& Long.valueOf(22L).equals(toLong(row.get("distributor_id")))
												&& "Test Shop".equals(row.get("name"))));
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void dispatchJob_async_payloadMatchesCreateDistributorPublisherEnvelope() {
		DispatchFacade mockDispatchFacade = mock(DispatchFacade.class);

		CreateDistributorJobDispatchPublisher publisher = new CreateDistributorJobDispatchPublisherImpl(mockDispatchFacade);

		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("company_id", 9L);
		snapshot.put("distributor_id", 8L);
		snapshot.put("shop_code", "S001");
		publisher.enqueueAfterDistributorCreate(snapshot);

		ArgumentCaptor<Map<String, Object>> payloadCap = ArgumentCaptor.forClass(Map.class);
		ArgumentCaptor<DispatchOptions> optsCap = ArgumentCaptor.forClass(DispatchOptions.class);
		verify(mockDispatchFacade)
				.dispatchJob(eq(ThemeDispatchJobNames.CREATE_DISTRIBUTOR_JOB), payloadCap.capture(), optsCap.capture());

		DispatchOptions opts = optsCap.getValue();
		assertEquals(DispatchMode.ASYNC, opts.mode());
		assertEquals(DispatchDriverType.REDIS, opts.driverOverride());
		assertEquals("slow", opts.queue());
		assertEquals(RetryPolicy.platformDefault(), opts.retryPolicy());

		Map<String, Object> p = payloadCap.getValue();
		assertEquals(9L, ((Number) p.get("company_id")).longValue());
		assertEquals(8L, ((Number) p.get("distributor_id")).longValue());
		assertEquals("S001", p.get("shop_code"));
	}

	private static Long toLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw == null) {
			return null;
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}
}
