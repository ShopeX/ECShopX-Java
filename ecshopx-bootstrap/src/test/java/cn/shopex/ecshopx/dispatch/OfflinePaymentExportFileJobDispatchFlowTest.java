package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.config.EspierExportFileJobDispatchHandler;
import cn.shopex.ecshopx.config.OfflinePaymentExportFileJobDispatchPublisherImpl;
import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
import cn.shopex.ecshopx.orders.dispatch.OfflinePaymentExportFileJobTypeHandler;
import cn.shopex.ecshopx.orders.dispatch.OfflinePaymentExportFileJobTypes;
import cn.shopex.ecshopx.orders.service.offline.export.OfflinePaymentCsvExportService;
import cn.shopex.ecshopx.orders.service.offline.export.OfflinePaymentExportFileJobHandler;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OfflinePaymentExportFileJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesOfflinePaymentCsvExportService() {
		OfflinePaymentCsvExportService csvExportService = mock(OfflinePaymentCsvExportService.class);
		ExportLogCreateService exportLogCreateService = mock(ExportLogCreateService.class);
		LinkedHashMap<String, String> upload = new LinkedHashMap<>();
		upload.put("url", "https://example.com/export.csv");
		upload.put("filename", "offline.csv");
		when(csvExportService.runExport(any(), anyLong())).thenReturn(Optional.of(upload));

		OfflinePaymentExportFileJobHandler jobHandler =
				new OfflinePaymentExportFileJobHandler(csvExportService, exportLogCreateService);
		OfflinePaymentExportFileJobTypeHandler typeHandler = new OfflinePaymentExportFileJobTypeHandler(jobHandler);
		EspierExportFileJobDispatchHandler router = new EspierExportFileJobDispatchHandler(List.of(typeHandler));

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB_OFFLINE_PAYMENT, router);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 901L;
		long operatorId = 902L;
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("check_status", 1);

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", OfflinePaymentExportFileJobTypes.TYPE_OFFLINE_PAYMENT);
		payload.put("company_id", companyId);
		payload.put("operator_id", operatorId);
		payload.put("filter", filter);

		facade.dispatchJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_OFFLINE_PAYMENT,
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
		assertEquals(EspierDispatchJobNames.EXPORT_FILE_JOB_OFFLINE_PAYMENT, msg.messageName());
		assertNull(msg.listenerName());
		assertTrue(msg.payload() instanceof Map);
		Map<?, ?> pl = (Map<?, ?>) msg.payload();
		assertEquals(OfflinePaymentExportFileJobTypes.TYPE_OFFLINE_PAYMENT, String.valueOf(pl.get("type")));
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

		verify(csvExportService, times(1))
				.runExport(
						argThat(
								f -> {
									if (!(f instanceof LinkedHashMap)) {
										return false;
									}
									LinkedHashMap<?, ?> fm = (LinkedHashMap<?, ?>) f;
									return companyId == asLong(fm.get("company_id"))
											&& Integer.valueOf(1).equals(asIntObject(fm.get("check_status")));
								}),
						eq(operatorId));
	}

	@Test
	void dispatchJob_publishPayloadMatchesOfflinePaymentEnvelope() {
		DispatchFacade facade = mock(DispatchFacade.class);
		OfflinePaymentExportFileJobDispatchPublisherImpl publisher =
				new OfflinePaymentExportFileJobDispatchPublisherImpl(facade);

		LinkedHashMap<String, Object> exportFilter = new LinkedHashMap<>();
		exportFilter.put("company_id", 801L);
		exportFilter.put("pay_sn", "SN1");

		publisher.enqueueOfflinePaymentExport(801L, 802L, exportFilter);

		verify(facade)
				.dispatchJob(
						eq(EspierDispatchJobNames.EXPORT_FILE_JOB_OFFLINE_PAYMENT),
						argThat(
								p -> {
									if (!(p instanceof Map)) {
										return false;
									}
									Map<?, ?> m = (Map<?, ?>) p;
									if (!OfflinePaymentExportFileJobTypes.TYPE_OFFLINE_PAYMENT.equals(
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
									return "SN1".equals(String.valueOf(fm.get("pay_sn")));
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
