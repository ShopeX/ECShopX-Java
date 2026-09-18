package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.chinaumspay.dispatch.ChinaumsDivisionListExportFileJobTypeHandler;
import cn.shopex.ecshopx.chinaumspay.dispatch.ChinaumsDivisionListExportFileJobTypes;
import cn.shopex.ecshopx.chinaumspay.service.divisionlist.ChinaumsDivisionListExportFileJobHandler;
import cn.shopex.ecshopx.chinaumspay.service.divisionlist.DivisionListCsvExportService;
import cn.shopex.ecshopx.chinaumspay.service.divisionlist.DivisionListExportContext;
import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.config.ChinaumsDivisionListExportFileJobDispatchPublisherImpl;
import cn.shopex.ecshopx.config.EspierExportFileJobDispatchHandler;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ChinaumsDivisionListExportFileJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesDivisionListCsvExportService() {
		DivisionListCsvExportService csv = mock(DivisionListCsvExportService.class);
		doNothing().when(csv).runExport(any(DivisionListExportContext.class));

		ChinaumsDivisionListExportFileJobHandler jobHandler = new ChinaumsDivisionListExportFileJobHandler(csv);
		ChinaumsDivisionListExportFileJobTypeHandler typeHandler = new ChinaumsDivisionListExportFileJobTypeHandler(jobHandler);
		EspierExportFileJobDispatchHandler router = new EspierExportFileJobDispatchHandler(List.of(typeHandler));

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB_CHINAUMS_DIVISION, router);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 901L;
		long operatorId = 902L;
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("back_status", "2");
		filter.put("create_time|gte", "2024-01-01 00:00:00");
		filter.put("create_time|lte", "2024-01-31 23:59:59");
		filter.put("distributor_id", 77L);

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", ChinaumsDivisionListExportFileJobTypes.TYPE_CHINAUMS_DIVISION);
		payload.put("company_id", companyId);
		payload.put("operator_id", operatorId);
		payload.put("filter", filter);

		facade.dispatchJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_CHINAUMS_DIVISION,
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
		assertEquals(EspierDispatchJobNames.EXPORT_FILE_JOB_CHINAUMS_DIVISION, msg.messageName());
		assertNull(msg.listenerName());
		assertTrue(msg.payload() instanceof Map);
		Map<?, ?> pl = (Map<?, ?>) msg.payload();
		assertEquals(ChinaumsDivisionListExportFileJobTypes.TYPE_CHINAUMS_DIVISION, String.valueOf(pl.get("type")));
		assertEquals(companyId, asLong(pl.get("company_id")));
		assertEquals(operatorId, asLong(pl.get("operator_id")));
		assertTrue(pl.get("filter") instanceof Map);
		Map<?, ?> nested = (Map<?, ?>) pl.get("filter");
		assertTrue(nested.containsKey("back_status"));
		assertTrue(nested.containsKey("create_time|gte"));
		assertTrue(nested.containsKey("create_time|lte"));
		assertTrue(nested.containsKey("distributor_id"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(csv, times(1))
				.runExport(
						argThat(
								ctx -> ctx.getCompanyId() == companyId
										&& ctx.getOperatorId() == operatorId
										&& "2".equals(ctx.getFilter().getBackStatus())
										&& "2024-01-01 00:00:00".equals(ctx.getFilter().getCreateTimeBegin())
										&& "2024-01-31 23:59:59".equals(ctx.getFilter().getCreateTimeEnd())
										&& ctx.getFilter().getDistributorId() != null
										&& ctx.getFilter().getDistributorId() == 77L));
	}

	@Test
	void dispatchJob_publishPayloadMatchesChinaumsDivisionEnvelope() {
		DispatchFacade facade = mock(DispatchFacade.class);
		ChinaumsDivisionListExportFileJobDispatchPublisherImpl publisher =
				new ChinaumsDivisionListExportFileJobDispatchPublisherImpl(facade);

		LinkedHashMap<String, Object> exportFilter = new LinkedHashMap<>();
		exportFilter.put("company_id", 801L);
		exportFilter.put("back_status", "1");

		publisher.enqueueChinaumsDivisionListExport(801L, 802L, exportFilter);

		verify(facade)
				.dispatchJob(
						eq(EspierDispatchJobNames.EXPORT_FILE_JOB_CHINAUMS_DIVISION),
						argThat(
								p -> {
									if (!(p instanceof Map)) {
										return false;
									}
									Map<?, ?> m = (Map<?, ?>) p;
									if (!ChinaumsDivisionListExportFileJobTypes.TYPE_CHINAUMS_DIVISION.equals(
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
									if (!"1".equals(String.valueOf(fm.get("back_status")))) {
										return false;
									}
									return true;
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

	private static long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}
}
