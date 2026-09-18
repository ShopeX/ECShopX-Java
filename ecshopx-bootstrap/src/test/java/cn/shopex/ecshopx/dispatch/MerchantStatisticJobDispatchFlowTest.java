package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.DatacubeDispatchJobNames;
import cn.shopex.ecshopx.config.MerchantStatisticJobDispatchPublisherImpl;
import cn.shopex.ecshopx.datacube.dispatch.MerchantStatisticJobHandler;
import cn.shopex.ecshopx.datacube.service.MerchantDataService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class MerchantStatisticJobDispatchFlowTest {

	@Test
	@DisplayName("dispatch enqueues slow queue without delay and consumer invokes MerchantDataService#runStatistics")
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesMerchantDataService() {
		MerchantDataService merchantDataService = Mockito.mock(MerchantDataService.class);
		MerchantStatisticJobHandler handler = new MerchantStatisticJobHandler(merchantDataService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(DatacubeDispatchJobNames.MERCHANT_STATISTIC_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 100L);
		payload.put("merchant_id", 200L);
		payload.put("count_date", "2025-03-01");

		facade.dispatchJob(
				DatacubeDispatchJobNames.MERCHANT_STATISTIC_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals("slow", msg.queue());
		assertNull(msg.delay());
		assertEquals(DatacubeDispatchJobNames.MERCHANT_STATISTIC_JOB, msg.messageName());
		assertNull(msg.listenerName());

		Map<String, Object> got = msg.payload();
		assertEquals(100L, asLong(got.get("company_id")));
		assertEquals(200L, asLong(got.get("merchant_id")));
		assertEquals("2025-03-01", got.get("count_date"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		ArgumentCaptor<LocalDate> dateCap = ArgumentCaptor.forClass(LocalDate.class);
		verify(merchantDataService).runStatistics(eq(100L), eq(200L), dateCap.capture());
		assertEquals(LocalDate.of(2025, 3, 1), dateCap.getValue());
	}

	@Test
	void dispatchJob_publishPayloadMatchesMerchantStatisticJobEnvelope() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		MerchantStatisticJobDispatchPublisherImpl publisher = new MerchantStatisticJobDispatchPublisherImpl(dispatchFacade);

		LocalDate d = LocalDate.of(2025, 1, 15);
		publisher.enqueue(42L, 99L, d);

		verify(dispatchFacade)
				.dispatchJob(
						eq(DatacubeDispatchJobNames.MERCHANT_STATISTIC_JOB),
						argThat(
								m ->
										42L == asLong(m.get("company_id"))
												&& 99L == asLong(m.get("merchant_id"))
												&& "2025-01-15".equals(m.get("count_date"))),
						argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.ASYNC
												&& opts.driverOverride() == DispatchDriverType.REDIS
												&& "slow".equals(opts.queue())
												&& opts.delay() == null));
	}

	private static long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}
}
