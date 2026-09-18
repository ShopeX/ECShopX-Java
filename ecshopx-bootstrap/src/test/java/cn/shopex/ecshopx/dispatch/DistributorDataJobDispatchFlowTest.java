package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.DatacubeDispatchJobNames;
import cn.shopex.ecshopx.config.DistributorDataJobDispatchPublisherImpl;
import cn.shopex.ecshopx.datacube.dispatch.DistributorDataJobHandler;
import cn.shopex.ecshopx.datacube.service.DistributorDataService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class DistributorDataJobDispatchFlowTest {

	@Test
	@DisplayName("dispatch enqueues slow queue without delay and consumer invokes DistributorDataService#runStatistics")
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesDistributorDataService() {
		DistributorDataService distributorDataService = Mockito.mock(DistributorDataService.class);
		DistributorDataJobHandler handler = new DistributorDataJobHandler(distributorDataService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(DatacubeDispatchJobNames.DISTRIBUTOR_DATA_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 100L);
		payload.put("distributor_id", 5L);
		payload.put("merchant_id", 200L);
		payload.put("count_date", "2025-03-01");

		facade.dispatchJob(
				DatacubeDispatchJobNames.DISTRIBUTOR_DATA_JOB,
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
		assertEquals(DatacubeDispatchJobNames.DISTRIBUTOR_DATA_JOB, msg.messageName());
		assertNull(msg.listenerName());

		Map<String, Object> got = msg.payload();
		assertEquals(100L, asLong(got.get("company_id")));
		assertEquals(5L, asLong(got.get("distributor_id")));
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
		verify(distributorDataService).runStatistics(eq(100L), eq(5L), eq(200L), dateCap.capture());
		assertEquals(LocalDate.of(2025, 3, 1), dateCap.getValue());
	}

	@Test
	void dispatchJob_publishPayloadMatchesDistributorDataJobEnvelope() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		DistributorDataJobDispatchPublisherImpl publisher = new DistributorDataJobDispatchPublisherImpl(dispatchFacade);

		LocalDate d = LocalDate.of(2025, 1, 15);
		publisher.enqueue(42L, 7L, 99L, d);

		verify(dispatchFacade)
				.dispatchJob(
						eq(DatacubeDispatchJobNames.DISTRIBUTOR_DATA_JOB),
						argThat(
								m ->
										42L == asLong(m.get("company_id"))
												&& 7L == asLong(m.get("distributor_id"))
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
