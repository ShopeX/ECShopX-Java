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

import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.companys.service.deliverystaff.AdminDeliveryStaffDataExportFilter;
import cn.shopex.ecshopx.config.DeliveryStaffDataExportFileJobDispatchPublisherImpl;
import cn.shopex.ecshopx.config.EspierExportFileJobDispatchHandler;
import cn.shopex.ecshopx.datacube.dispatch.DeliveryStaffDataExportFileJobTypeHandler;
import cn.shopex.ecshopx.datacube.dispatch.DeliveryStaffDataExportFileJobTypes;
import cn.shopex.ecshopx.datacube.service.deliverystaff.DeliveryStaffDataCsvExportService;
import cn.shopex.ecshopx.datacube.service.deliverystaff.DeliveryStaffDataExportFileJobHandler;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DeliveryStaffDataExportFileJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesDeliveryStaffDataCsvExportService() {
		DeliveryStaffDataCsvExportService csv = mock(DeliveryStaffDataCsvExportService.class);
		doNothing().when(csv).runExport(any(AdminDeliveryStaffDataExportFilter.class));

		DeliveryStaffDataExportFileJobHandler jobHandler = new DeliveryStaffDataExportFileJobHandler(csv);
		DeliveryStaffDataExportFileJobTypeHandler typeHandler = new DeliveryStaffDataExportFileJobTypeHandler(jobHandler);
		EspierExportFileJobDispatchHandler router = new EspierExportFileJobDispatchHandler(List.of(typeHandler));

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB_DELIVERY_STAFF_DATA, router);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 901L;
		long operatorId = 902L;
		long startEpoch = 1_700_000_000L;
		long endEpoch = 1_700_086_400L;
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("operator_type", "shop");
		filter.put("username", "alice");
		filter.put("start_date", startEpoch);
		filter.put("end_date", endEpoch);

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", DeliveryStaffDataExportFileJobTypes.TYPE_DELIVERY_STAFF_DATA);
		payload.put("company_id", companyId);
		payload.put("operator_id", operatorId);
		payload.put("filter", filter);

		facade.dispatchJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_DELIVERY_STAFF_DATA,
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
		assertEquals(EspierDispatchJobNames.EXPORT_FILE_JOB_DELIVERY_STAFF_DATA, msg.messageName());
		assertNull(msg.listenerName());
		assertTrue(msg.payload() instanceof Map);
		Map<?, ?> pl = (Map<?, ?>) msg.payload();
		assertEquals(DeliveryStaffDataExportFileJobTypes.TYPE_DELIVERY_STAFF_DATA, String.valueOf(pl.get("type")));
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

		verify(csv, times(1))
				.runExport(
						argThat(
								f -> f.getCompanyId() == companyId
										&& f.getOperatorId() == operatorId
										&& f.getStartEpoch() == startEpoch
										&& f.getEndEpoch() == endEpoch
										&& "shop".equals(f.getOperatorType())
										&& "alice".equals(f.getUsername())));
	}

	@Test
	void dispatchJob_publishPayloadMatchesDeliveryStaffEnvelope() {
		DispatchFacade facade = mock(DispatchFacade.class);
		DeliveryStaffDataExportFileJobDispatchPublisherImpl publisher =
				new DeliveryStaffDataExportFileJobDispatchPublisherImpl(facade);

		LinkedHashMap<String, Object> exportFilter = new LinkedHashMap<>();
		exportFilter.put("company_id", 801L);
		exportFilter.put("start_date", 100L);
		exportFilter.put("end_date", 200L);

		publisher.enqueueDeliveryStaffDataExport(801L, 802L, exportFilter);

		verify(facade)
				.dispatchJob(
						eq(EspierDispatchJobNames.EXPORT_FILE_JOB_DELIVERY_STAFF_DATA),
						argThat(
								p -> {
									if (!(p instanceof Map)) {
										return false;
									}
									Map<?, ?> m = (Map<?, ?>) p;
									if (!DeliveryStaffDataExportFileJobTypes.TYPE_DELIVERY_STAFF_DATA.equals(
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
									if (100L != asLong(fm.get("start_date")) || 200L != asLong(fm.get("end_date"))) {
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
