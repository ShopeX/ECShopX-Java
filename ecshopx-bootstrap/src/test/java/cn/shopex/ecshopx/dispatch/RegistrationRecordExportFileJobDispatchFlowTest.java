package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.config.EspierExportFileJobDispatchHandler;
import cn.shopex.ecshopx.selfservice.dispatch.RegistrationRecordExportFileJobTypeHandler;
import cn.shopex.ecshopx.selfservice.service.export.RegistrationRecordCsvExportService;
import cn.shopex.ecshopx.selfservice.service.export.RegistrationRecordExportJobContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class RegistrationRecordExportFileJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesCsvExportService() {
		RegistrationRecordCsvExportService csvMock = mock(RegistrationRecordCsvExportService.class);
		RegistrationRecordExportFileJobTypeHandler typeHandler = new RegistrationRecordExportFileJobTypeHandler(csvMock);
		EspierExportFileJobDispatchHandler router = new EspierExportFileJobDispatchHandler(List.of(typeHandler));

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB_REGISTRATION_RECORD, router);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", "selform_registration_record");
		payload.put("company_id", 1L);
		payload.put("operator_id", 7L);
		payload.put("supplier_id", 3L);
		payload.put("activity_id", 99L);
		payload.put("mobile", "13800138000");
		payload.put("start_time", 100);
		payload.put("end_time", 200);
		payload.put("datapass_block", "");
		payload.put("locale_language_tag", Locale.SIMPLIFIED_CHINESE.toLanguageTag());

		facade.dispatchJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_REGISTRATION_RECORD,
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
		assertEquals(EspierDispatchJobNames.EXPORT_FILE_JOB_REGISTRATION_RECORD, msg.messageName());

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(csvMock)
				.runExport(
						argThat(
								ctx ->
										ctx.companyId() == 1L
												&& ctx.operatorId() == 7L
												&& ctx.supplierId() == 3L
												&& ctx.activityId() == 99L
												&& "13800138000".equals(ctx.mobilePlain())
												&& Objects.equals(ctx.startCreatedInclusive(), 100)
												&& Objects.equals(ctx.endCreatedInclusive(), 200)
												&& "".equals(ctx.datapassBlock())
												&& Locale.SIMPLIFIED_CHINESE.equals(ctx.locale())));
	}

	@Test
	void handler_whenRunExportThrows_logsAndConsumerStillCompletes() {
		RegistrationRecordCsvExportService csvExportService = mock(RegistrationRecordCsvExportService.class);
		doThrow(new RuntimeException("downstream"))
				.when(csvExportService)
				.runExport(org.mockito.ArgumentMatchers.any(RegistrationRecordExportJobContext.class));
		RegistrationRecordExportFileJobTypeHandler typeHandler = new RegistrationRecordExportFileJobTypeHandler(csvExportService);
		EspierExportFileJobDispatchHandler router = new EspierExportFileJobDispatchHandler(List.of(typeHandler));

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB_REGISTRATION_RECORD, router);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						EspierDispatchJobNames.EXPORT_FILE_JOB_REGISTRATION_RECORD,
						Map.of(
								"type",
								"selform_registration_record",
								"company_id",
								9L,
								"operator_id",
								3L,
								"supplier_id",
								0L,
								"activity_id",
								12L,
								"datapass_block",
								"",
								"locale_language_tag",
								Locale.ENGLISH.toLanguageTag()),
						"slow",
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

		verify(csvExportService)
				.runExport(
						argThat(
								(RegistrationRecordExportJobContext ctx) ->
										ctx.companyId() == 9L
												&& ctx.operatorId() == 3L
												&& ctx.supplierId() == 0L
												&& ctx.activityId() == 12L
												&& ctx.mobilePlain() == null
												&& ctx.startCreatedInclusive() == null
												&& ctx.endCreatedInclusive() == null
												&& "".equals(ctx.datapassBlock())
												&& Locale.ENGLISH.equals(ctx.locale())));
	}
}
