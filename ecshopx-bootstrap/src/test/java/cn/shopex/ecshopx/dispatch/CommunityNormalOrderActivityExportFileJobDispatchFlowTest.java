package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.community.dispatch.CommunityNormalOrderActivityExportFileJobTypeHandler;
import cn.shopex.ecshopx.community.service.export.CommunityNormalOrderActivityExportFileJobHandler;
import cn.shopex.ecshopx.community.service.export.CommunityNormalOrderActivityXlsxExportService;
import cn.shopex.ecshopx.config.EspierExportFileJobDispatchHandler;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CommunityNormalOrderActivityExportFileJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesCommunityNormalOrderActivityExportFileJobHandler() {
		CommunityNormalOrderActivityExportFileJobHandler jobHandler = mock(CommunityNormalOrderActivityExportFileJobHandler.class);
		CommunityNormalOrderActivityExportFileJobTypeHandler typeHandler =
				new CommunityNormalOrderActivityExportFileJobTypeHandler(jobHandler);
		EspierExportFileJobDispatchHandler router = new EspierExportFileJobDispatchHandler(List.of(typeHandler));

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB, router);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 101L;
		long operatorId = 202L;
		boolean datapassAllowed = true;
		Long optionalActivityId = 55L;
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("distributor_operator", true);
		filter.put("distributor_id_filter", 7);
		filter.put("created_at_gte", 1_700_000_000_000L);
		filter.put("created_at_lte", 1_700_086_400_000L);
		filter.put("filter_created_at_lte_with_null", false);
		filter.put("activity_status", "open");
		filter.put("success", true);
		filter.put("activity_name_contains", "summer");

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", CommunityNormalOrderActivityXlsxExportService.EXPORT_TYPE);
		payload.put("company_id", companyId);
		payload.put("operator_id", operatorId);
		payload.put("datapass_allowed", datapassAllowed);
		payload.put("optional_activity_id", optionalActivityId);
		payload.put("filter", new LinkedHashMap<>(filter));

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
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals("slow", msg.queue());
		assertNull(msg.delay());
		assertEquals(EspierDispatchJobNames.EXPORT_FILE_JOB, msg.messageName());
		assertNull(msg.listenerName());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(jobHandler)
				.run(
						argThat(
								ctx ->
										ctx.companyId() == companyId
												&& ctx.operatorId() == operatorId
												&& ctx.datapassAllowed() == datapassAllowed
												&& optionalActivityId.equals(ctx.optionalActivityId())
												&& ctx.query().isDistributorOperator()
												&& Integer.valueOf(7).equals(ctx.query().getDistributorIdFilter())
												&& Long.valueOf(1_700_000_000_000L).equals(ctx.query().getCreatedAtGte())
												&& Long.valueOf(1_700_086_400_000L).equals(ctx.query().getCreatedAtLte())
												&& !ctx.query().isFilterCreatedAtLteWithNull()
												&& "open".equals(ctx.query().getActivityStatus())
												&& ctx.query().isSuccess()
												&& "summer".equals(ctx.query().getActivityNameContains())));
	}

	@Test
	void dispatchJob_async_payloadMatchesCommunityNormalOrderExportEnvelope() {
		CommunityNormalOrderActivityExportFileJobHandler jobHandler = mock(CommunityNormalOrderActivityExportFileJobHandler.class);
		CommunityNormalOrderActivityExportFileJobTypeHandler typeHandler =
				new CommunityNormalOrderActivityExportFileJobTypeHandler(jobHandler);
		EspierExportFileJobDispatchHandler router = new EspierExportFileJobDispatchHandler(List.of(typeHandler));

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB, router);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 303L;
		long operatorId = 404L;
		boolean datapassAllowed = false;
		Long optionalActivityId = null;
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("distributor_operator", false);
		filter.put("distributor_id_filter", null);
		filter.put("created_at_gte", null);
		filter.put("created_at_lte", null);
		filter.put("filter_created_at_lte_with_null", true);
		filter.put("activity_status", null);
		filter.put("success", false);
		filter.put("activity_name_contains", null);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", CommunityNormalOrderActivityXlsxExportService.EXPORT_TYPE);
		payload.put("company_id", companyId);
		payload.put("operator_id", operatorId);
		payload.put("datapass_allowed", datapassAllowed);
		payload.put("optional_activity_id", optionalActivityId);
		payload.put("filter", new LinkedHashMap<>(filter));

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
		Map<String, Object> got = msg.payload();
		assertEquals(CommunityNormalOrderActivityXlsxExportService.EXPORT_TYPE, got.get("type"));
		assertEquals(companyId, ((Number) got.get("company_id")).longValue());
		assertEquals(operatorId, ((Number) got.get("operator_id")).longValue());
		assertEquals(datapassAllowed, got.get("datapass_allowed"));
		assertNull(got.get("optional_activity_id"));
		@SuppressWarnings("unchecked")
		Map<String, Object> nested = (Map<String, Object>) got.get("filter");
		assertEquals(false, nested.get("distributor_operator"));
		assertNull(nested.get("distributor_id_filter"));
		assertNull(nested.get("created_at_gte"));
		assertNull(nested.get("created_at_lte"));
		assertEquals(true, nested.get("filter_created_at_lte_with_null"));
		assertNull(nested.get("activity_status"));
		assertEquals(false, nested.get("success"));
		assertNull(nested.get("activity_name_contains"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(jobHandler)
				.run(
						argThat(
								ctx ->
										ctx.companyId() == companyId
												&& ctx.operatorId() == operatorId
												&& !ctx.datapassAllowed()
												&& ctx.optionalActivityId() == null
												&& !ctx.query().isDistributorOperator()
												&& ctx.query().getDistributorIdFilter() == null
												&& ctx.query().getCreatedAtGte() == null
												&& ctx.query().getCreatedAtLte() == null
												&& ctx.query().isFilterCreatedAtLteWithNull()
												&& ctx.query().getActivityStatus() == null
												&& !ctx.query().isSuccess()
												&& ctx.query().getActivityNameContains() == null));
	}
}
