package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.bspay.dispatch.BspayWithdrawDataExportFileJobHandler;
import cn.shopex.ecshopx.bspay.dispatch.BspayWithdrawDataExportFileJobTypeHandler;
import cn.shopex.ecshopx.bspay.dispatch.BspayWithdrawDataExportFileJobTypes;
import cn.shopex.ecshopx.bspay.service.export.BspayWithdrawDataCsvExportService;
import cn.shopex.ecshopx.bspay.service.export.BspayWithdrawDataExportContext;
import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.config.BspayWithdrawDataExportFileJobDispatchPublisherImpl;
import cn.shopex.ecshopx.config.EspierExportFileJobDispatchHandler;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BspayWithdrawDataExportFileJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesBspayWithdrawDataCsvExportService() {
		BspayWithdrawDataCsvExportService csvExportService = mock(BspayWithdrawDataCsvExportService.class);
		BspayWithdrawDataExportFileJobHandler jobHandler = new BspayWithdrawDataExportFileJobHandler(csvExportService);
		BspayWithdrawDataExportFileJobTypeHandler typeHandler =
				new BspayWithdrawDataExportFileJobTypeHandler(jobHandler);
		EspierExportFileJobDispatchHandler router = new EspierExportFileJobDispatchHandler(List.of(typeHandler));

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB_BSPAY_WITHDRAW_DATA, router);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 701L;
		long jwtOperatorId = 702L;
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("operator_type", "staff");
		filter.put("status", "SUBMITTED");

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", BspayWithdrawDataExportFileJobTypes.TYPE_BSPAY_WITHDRAW);
		payload.put("company_id", companyId);
		payload.put("operator_id", jwtOperatorId);
		payload.put("filter", filter);

		facade.dispatchJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_BSPAY_WITHDRAW_DATA,
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
		assertEquals(EspierDispatchJobNames.EXPORT_FILE_JOB_BSPAY_WITHDRAW_DATA, msg.messageName());
		assertNull(msg.listenerName());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(csvExportService, times(1))
				.runExport(
						argThat(
								(BspayWithdrawDataExportContext ctx) ->
										ctx.companyId() == companyId
												&& ctx.jwtOperatorId() == jwtOperatorId
												&& "SUBMITTED".equals(String.valueOf(ctx.exportFilter().get("status")))));
	}

	@Test
	void dispatchJob_publishPayloadMatchesBspayWithdrawDataEnvelope() {
		DispatchFacade facade = mock(DispatchFacade.class);
		BspayWithdrawDataExportFileJobDispatchPublisherImpl publisher =
				new BspayWithdrawDataExportFileJobDispatchPublisherImpl(facade);

		LinkedHashMap<String, Object> exportFilter = new LinkedHashMap<>();
		exportFilter.put("company_id", 501L);
		exportFilter.put("operator_type", "staff");
		exportFilter.put("status", "DONE");
		BspayWithdrawDataExportContext ctx =
				new BspayWithdrawDataExportContext(501L, 503L, exportFilter);

		publisher.enqueueBspayWithdrawDataExport(ctx);

		verify(facade)
				.dispatchJob(
						eq(EspierDispatchJobNames.EXPORT_FILE_JOB_BSPAY_WITHDRAW_DATA),
						argThat(
								p -> {
									if (!(p instanceof Map)) {
										return false;
									}
									Map<?, ?> m = (Map<?, ?>) p;
									if (!BspayWithdrawDataExportFileJobTypes.TYPE_BSPAY_WITHDRAW.equals(
											String.valueOf(m.get("type")))) {
										return false;
									}
									if (501L != asLong(m.get("company_id")) || 503L != asLong(m.get("operator_id"))) {
										return false;
									}
									Object rawFilter = m.get("filter");
									if (!(rawFilter instanceof Map)) {
										return false;
									}
									Map<?, ?> fm = (Map<?, ?>) rawFilter;
									if (501L != asLong(fm.get("company_id"))) {
										return false;
									}
									if (!"staff".equals(String.valueOf(fm.get("operator_type")))) {
										return false;
									}
									return "DONE".equals(String.valueOf(fm.get("status")));
								}),
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
