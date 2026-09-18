package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.config.EspierExportFileJobDispatchHandler;
import cn.shopex.ecshopx.config.StatementsSummarizedExportFileJobDispatchPublisherImpl;
import cn.shopex.ecshopx.orders.dispatch.StatementsSummarizedExportFileJobTypeHandler;
import cn.shopex.ecshopx.orders.dispatch.StatementsSummarizedExportFileJobTypes;
import cn.shopex.ecshopx.orders.service.statement.export.StatementsSummarizedExportFileJobHandler;
import cn.shopex.ecshopx.orders.service.statement.export.StatementsSummarizedExportJobContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

class StatementsSummarizedExportFileJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesSummarizedHandler() {
		StatementsSummarizedExportFileJobHandler mockHandler = mock(StatementsSummarizedExportFileJobHandler.class);
		StatementsSummarizedExportFileJobTypeHandler typeHandler =
				new StatementsSummarizedExportFileJobTypeHandler(mockHandler);
		EspierExportFileJobDispatchHandler router = new EspierExportFileJobDispatchHandler(List.of(typeHandler));

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB_STATEMENTS_SUMMARIZED, router);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 901L;
		long operatorId = 902L;
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("status", 1);

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", StatementsSummarizedExportFileJobTypes.TYPE_STATEMENTS_SUMMARIZED);
		payload.put("company_id", companyId);
		payload.put("operator_id", operatorId);
		payload.put("filter", filter);

		facade.dispatchJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_STATEMENTS_SUMMARIZED,
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
		assertEquals("slow", msg.queue());
		assertNull(msg.delay());
		assertEquals(EspierDispatchJobNames.EXPORT_FILE_JOB_STATEMENTS_SUMMARIZED, msg.messageName());
		assertNull(msg.listenerName());
		assertTrue(msg.payload() instanceof Map);
		Map<?, ?> pl = (Map<?, ?>) msg.payload();
		assertEquals(StatementsSummarizedExportFileJobTypes.TYPE_STATEMENTS_SUMMARIZED, String.valueOf(pl.get("type")));
		assertEquals(companyId, asLong(pl.get("company_id")));
		assertEquals(operatorId, asLong(pl.get("operator_id")));
		assertTrue(pl.get("filter") instanceof Map);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(mockHandler, times(1))
				.run(
						argThat(
								ctx -> {
									if (!(ctx instanceof StatementsSummarizedExportJobContext)) {
										return false;
									}
									StatementsSummarizedExportJobContext c =
											(StatementsSummarizedExportJobContext) ctx;
									if (c.companyId() != companyId || c.operatorId() != operatorId) {
										return false;
									}
									LinkedHashMap<String, Object> f = c.filter();
									return companyId == asLong(f.get("company_id"))
											&& Integer.valueOf(1).equals(asIntObject(f.get("status")));
								}));
	}

	@Test
	void dispatchJob_localProfile_syncPath_executesWithoutRedisCapture() {
		StatementsSummarizedExportFileJobHandler mockHandler = mock(StatementsSummarizedExportFileJobHandler.class);
		StatementsSummarizedExportFileJobTypeHandler typeHandler =
				new StatementsSummarizedExportFileJobTypeHandler(mockHandler);
		EspierExportFileJobDispatchHandler router = new EspierExportFileJobDispatchHandler(List.of(typeHandler));

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB_STATEMENTS_SUMMARIZED, router);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 301L;
		long operatorId = 302L;
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", StatementsSummarizedExportFileJobTypes.TYPE_STATEMENTS_SUMMARIZED);
		payload.put("company_id", companyId);
		payload.put("operator_id", operatorId);
		payload.put("filter", filter);

		facade.dispatchJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_STATEMENTS_SUMMARIZED,
				payload,
				new DispatchOptions(
						DispatchMode.SYNC,
						DispatchDriverType.SYNC,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertTrue(captured.isEmpty());

		verify(mockHandler, times(1))
				.run(
						argThat(
								ctx -> {
									if (!(ctx instanceof StatementsSummarizedExportJobContext)) {
										return false;
									}
									StatementsSummarizedExportJobContext c =
											(StatementsSummarizedExportJobContext) ctx;
									return c.companyId() == companyId
											&& c.operatorId() == operatorId
											&& companyId == asLong(c.filter().get("company_id"));
								}));
	}

	@Test
	void publisherImpl_dispatchesWithExpectedMessageName() {
		DispatchFacade facade = mock(DispatchFacade.class);
		Environment env = mock(Environment.class);
		when(env.acceptsProfiles(Profiles.of("local"))).thenReturn(false);
		StatementsSummarizedExportFileJobDispatchPublisherImpl publisher =
				new StatementsSummarizedExportFileJobDispatchPublisherImpl(facade, env);

		LinkedHashMap<String, Object> exportFilter = new LinkedHashMap<>();
		exportFilter.put("company_id", 801L);
		exportFilter.put("range", "week");

		publisher.enqueueSummarizedExport(801L, 802L, exportFilter);

		verify(facade)
				.dispatchJob(
						eq(EspierDispatchJobNames.EXPORT_FILE_JOB_STATEMENTS_SUMMARIZED),
						argThat(
								p -> {
									if (!(p instanceof Map)) {
										return false;
									}
									Map<?, ?> m = (Map<?, ?>) p;
									if (!StatementsSummarizedExportFileJobTypes.TYPE_STATEMENTS_SUMMARIZED.equals(
											String.valueOf(m.get("type")))) {
										return false;
									}
									if (801L != asLong(m.get("company_id")) || 802L != asLong(m.get("operator_id"))) {
										return false;
									}
									Object rawFilter = m.get("filter");
									if (!(rawFilter instanceof Map)) {
										return false;
									}
									Map<?, ?> fm = (Map<?, ?>) rawFilter;
									if (801L != asLong(fm.get("company_id"))) {
										return false;
									}
									return "week".equals(String.valueOf(fm.get("range")));
								}),
						argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.ASYNC
												&& opts.driverOverride() == DispatchDriverType.REDIS
												&& "slow".equals(opts.queue())
												&& opts.delay() == null
												&& RetryPolicy.platformDefault().equals(opts.retryPolicy())));
	}

	@Test
	void publisherImpl_localProfile_usesSyncOptions() {
		DispatchFacade facade = mock(DispatchFacade.class);
		Environment env = mock(Environment.class);
		when(env.acceptsProfiles(Profiles.of("local"))).thenReturn(true);
		StatementsSummarizedExportFileJobDispatchPublisherImpl publisher =
				new StatementsSummarizedExportFileJobDispatchPublisherImpl(facade, env);

		LinkedHashMap<String, Object> exportFilter = new LinkedHashMap<>();
		exportFilter.put("company_id", 640L);

		publisher.enqueueSummarizedExport(640L, 641L, exportFilter);

		verify(facade)
				.dispatchJob(
						eq(EspierDispatchJobNames.EXPORT_FILE_JOB_STATEMENTS_SUMMARIZED),
						argThat(
								p -> {
									if (!(p instanceof Map)) {
										return false;
									}
									Map<?, ?> m = (Map<?, ?>) p;
									return StatementsSummarizedExportFileJobTypes.TYPE_STATEMENTS_SUMMARIZED.equals(
													String.valueOf(m.get("type")))
											&& 640L == asLong(m.get("company_id"))
											&& 641L == asLong(m.get("operator_id"));
								}),
						argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.SYNC
												&& opts.driverOverride() == DispatchDriverType.SYNC
												&& opts.queue() == null
												&& opts.delay() == null
												&& RetryPolicy.platformDefault().equals(opts.retryPolicy())));
	}

	private static long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}

	private static Integer asIntObject(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(String.valueOf(v).trim());
	}
}
