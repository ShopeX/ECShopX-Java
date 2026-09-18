package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.DatacubeDispatchJobNames;
import cn.shopex.ecshopx.config.StatisticJobDispatchPublisherImpl;
import cn.shopex.ecshopx.datacube.dispatch.StatisticJobHandler;
import cn.shopex.ecshopx.datacube.service.CompanyDataService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class StatisticJobDispatchFlowTest {

	@Test
	@DisplayName("dispatch enqueues slow queue without delay and consumer invokes CompanyDataService#runStatistics (default dimension)")
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesRunStatistics_defaultDimension() {
		CompanyDataService companyDataService = Mockito.mock(CompanyDataService.class);
		StatisticJobHandler handler = new StatisticJobHandler(companyDataService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(DatacubeDispatchJobNames.COMPANY_STATISTIC_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 100L);
		payload.put("count_date", "2025-03-01");
		payload.put("act_id", 0L);

		facade.dispatchJob(
				DatacubeDispatchJobNames.COMPANY_STATISTIC_JOB,
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
		assertEquals(DatacubeDispatchJobNames.COMPANY_STATISTIC_JOB, msg.messageName());
		assertNull(msg.listenerName());

		Map<String, Object> got = msg.payload();
		assertEquals(100L, asLong(got.get("company_id")));
		assertFalse(got.containsKey("order_class"));
		assertEquals("2025-03-01", got.get("count_date"));
		assertEquals(0L, asLong(got.get("act_id")));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		ArgumentCaptor<LocalDate> dateCap = ArgumentCaptor.forClass(LocalDate.class);
		verify(companyDataService).runStatistics(eq(100L), dateCap.capture(), isNull(), eq(0L));
		assertEquals(LocalDate.of(2025, 3, 1), dateCap.getValue());
	}

	@Test
	@DisplayName("consumer maps employee_purchase dimension and act_id")
	void dispatchJob_consumer_invokesRunStatistics_employeePurchase() {
		CompanyDataService companyDataService = Mockito.mock(CompanyDataService.class);
		StatisticJobHandler handler = new StatisticJobHandler(companyDataService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(DatacubeDispatchJobNames.COMPANY_STATISTIC_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 42L);
		payload.put("count_date", "2025-01-15");
		payload.put("order_class", "employee_purchase");
		payload.put("act_id", 77L);

		facade.dispatchJob(
				DatacubeDispatchJobNames.COMPANY_STATISTIC_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		DispatchMessage msg = captured.get(0);
		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(companyDataService)
				.runStatistics(eq(42L), eq(LocalDate.of(2025, 1, 15)), eq("employee_purchase"), eq(77L));
	}

	@Test
	void dispatchJob_publishPayloadMatchesStatisticJobEnvelope() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		StatisticJobDispatchPublisherImpl publisher = new StatisticJobDispatchPublisherImpl(dispatchFacade);

		LocalDate d = LocalDate.of(2025, 1, 15);
		publisher.enqueue(42L, d, null, 0L);

		verify(dispatchFacade)
				.dispatchJob(
						eq(DatacubeDispatchJobNames.COMPANY_STATISTIC_JOB),
						argThat(
								m ->
										42L == asLong(m.get("company_id"))
												&& "2025-01-15".equals(m.get("count_date"))
												&& !m.containsKey("order_class")
												&& 0L == asLong(m.get("act_id"))),
						argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.ASYNC
												&& opts.driverOverride() == DispatchDriverType.REDIS
												&& "slow".equals(opts.queue())
												&& opts.delay() == null));

		publisher.enqueue(9L, d, "employee_purchase", 5L);
		verify(dispatchFacade)
				.dispatchJob(
						eq(DatacubeDispatchJobNames.COMPANY_STATISTIC_JOB),
						argThat(
								m ->
										9L == asLong(m.get("company_id"))
												&& "2025-01-15".equals(m.get("count_date"))
												&& "employee_purchase".equals(m.get("order_class"))
												&& 5L == asLong(m.get("act_id"))),
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
