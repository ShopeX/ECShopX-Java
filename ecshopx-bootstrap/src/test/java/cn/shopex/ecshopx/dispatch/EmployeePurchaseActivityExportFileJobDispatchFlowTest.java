package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.config.EspierExportFileJobDispatchHandler;
import cn.shopex.ecshopx.employeepurchase.dispatch.EmployeePurchaseActivityItemsExportFileJobTypeHandler;
import cn.shopex.ecshopx.employeepurchase.dispatch.EmployeePurchaseActivityQrcodeExportFileJobTypeHandler;
import cn.shopex.ecshopx.employeepurchase.dispatch.EmployeePurchaseActivityScanStatsExportFileJobTypeHandler;
import cn.shopex.ecshopx.employeepurchase.service.dto.ActivityAdminExportQuery;
import cn.shopex.ecshopx.employeepurchase.service.dto.ActivityItemsExportQuery;
import cn.shopex.ecshopx.employeepurchase.service.export.EmployeePurchaseActivityItemsCsvExportService;
import cn.shopex.ecshopx.employeepurchase.service.export.EmployeePurchaseActivityQrcodeCsvExportService;
import cn.shopex.ecshopx.employeepurchase.service.export.EmployeePurchaseActivityScanStatsCsvExportService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EmployeePurchaseActivityExportFileJobDispatchFlowTest {

	@Test
	void scanStatsExport_dispatchesToSlowQueue() {
		EmployeePurchaseActivityScanStatsCsvExportService csv = mock(EmployeePurchaseActivityScanStatsCsvExportService.class);
		EmployeePurchaseActivityScanStatsExportFileJobTypeHandler handler =
				new EmployeePurchaseActivityScanStatsExportFileJobTypeHandler(csv);
		EspierExportFileJobDispatchHandler router = new EspierExportFileJobDispatchHandler(List.of(handler));

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB, router);
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", "employee_purchase_activity_scan_stats");
		payload.put("company_id", 1L);
		payload.put("activity_id", 900001L);
		payload.put("operator_id", 1L);

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
		assertEquals("slow", captured.get(0).queue());
		assertEquals("employee_purchase_activity_scan_stats", captured.get(0).payload().get("type"));

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(captured.get(0), 1);
		verify(csv).runExport(new ActivityAdminExportQuery(1L, 900001L, null, 1L));
	}

	@Test
	void qrcodeExport_dispatchesToSlowQueue() {
		EmployeePurchaseActivityQrcodeCsvExportService csv = mock(EmployeePurchaseActivityQrcodeCsvExportService.class);
		EmployeePurchaseActivityQrcodeExportFileJobTypeHandler handler =
				new EmployeePurchaseActivityQrcodeExportFileJobTypeHandler(csv);
		EspierExportFileJobDispatchHandler router = new EspierExportFileJobDispatchHandler(List.of(handler));

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB, router);
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", "employee_purchase_activity_qrcode");
		payload.put("company_id", 1L);
		payload.put("activity_id", 900001L);
		payload.put("operator_id", 1L);

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
		assertEquals("slow", captured.get(0).queue());
		assertTrue(captured.get(0).payload().get("type").toString().contains("qrcode"));
	}

	@Test
	void activityItemsExport_dispatchesToSlowQueueAndRunsCsv() {
		EmployeePurchaseActivityItemsCsvExportService csv = mock(EmployeePurchaseActivityItemsCsvExportService.class);
		EmployeePurchaseActivityItemsExportFileJobTypeHandler handler =
				new EmployeePurchaseActivityItemsExportFileJobTypeHandler(csv);
		EspierExportFileJobDispatchHandler router = new EspierExportFileJobDispatchHandler(List.of(handler));

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB, router);
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", "employee_purchase_activity_items");
		payload.put("company_id", 1L);
		payload.put("activity_id", 166L);
		payload.put("operator_id", 9L);
		payload.put("distributor_id", 0L);
		payload.put("item_bn", "SKU-1");

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
		assertEquals("slow", captured.get(0).queue());
		assertEquals("employee_purchase_activity_items", captured.get(0).payload().get("type"));

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(captured.get(0), 1);
		verify(csv)
				.runExport(
						new ActivityItemsExportQuery(
								1L, 166L, 0L, 9L, null, null, null, null, "SKU-1", null, null));
	}
}
