package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.config.EspierExportFileJobDispatchHandler;
import cn.shopex.ecshopx.employeepurchase.dispatch.EmployeePurchaseEmployeesExportFileJobTypeHandler;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseEmployeeCsvExportService;
import cn.shopex.ecshopx.employeepurchase.service.dto.EmployeeAdminExportQuery;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EmployeePurchaseEmployeesExportFileJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesEmployeeCsvExport() {
		EmployeePurchaseEmployeeCsvExportService csvExportService = mock(EmployeePurchaseEmployeeCsvExportService.class);
		EmployeePurchaseEmployeesExportFileJobTypeHandler typeHandler =
				new EmployeePurchaseEmployeesExportFileJobTypeHandler(csvExportService);
		EspierExportFileJobDispatchHandler router = new EspierExportFileJobDispatchHandler(List.of(typeHandler));

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB, router);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		EmployeeAdminExportQuery query = new EmployeeAdminExportQuery(1L, 2, "13800138000", null, null, null, 99L);
		long operatorId = 7L;
		boolean datapass = true;

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", "employee_purchase_employees");
		payload.put("company_id", query.companyId());
		payload.put("operator_id", operatorId);
		payload.put("datapass_block", datapass ? 1 : 0);
		payload.put("distributor_id", query.distributorId());
		payload.put("mobile", query.mobile());
		payload.put("enterprise_id", query.enterpriseId());

		facade.dispatchJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB,
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
		assertEquals(EspierDispatchJobNames.EXPORT_FILE_JOB, msg.messageName());

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(csvExportService).runExport(eq(query), eq(operatorId), eq(datapass));
	}

	@Test
	void handler_whenRunExportThrows_logsAndConsumerStillCompletes() {
		EmployeePurchaseEmployeeCsvExportService csvExportService = mock(EmployeePurchaseEmployeeCsvExportService.class);
		doThrow(new RuntimeException("downstream"))
				.when(csvExportService)
				.runExport(any(EmployeeAdminExportQuery.class), anyLong(), anyBoolean());
		EmployeePurchaseEmployeesExportFileJobTypeHandler typeHandler =
				new EmployeePurchaseEmployeesExportFileJobTypeHandler(csvExportService);
		EspierExportFileJobDispatchHandler router = new EspierExportFileJobDispatchHandler(List.of(typeHandler));

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB, router);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						EspierDispatchJobNames.EXPORT_FILE_JOB,
						Map.of(
								"type",
								"employee_purchase_employees",
								"company_id",
								9L,
								"operator_id",
								3L,
								"datapass_block",
								0),
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
						eq(new EmployeeAdminExportQuery(9L, null, null, null, null, null, null)),
						eq(3L),
						eq(false));
	}
}
