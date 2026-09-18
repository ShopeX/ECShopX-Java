package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.config.EspierExportFileJobDispatchHandler;
import cn.shopex.ecshopx.salesperson.dispatch.ProfitDistributorExportFileJobTypeHandler;
import cn.shopex.ecshopx.salesperson.dispatch.ProfitSalespersonExportFileJobTypeHandler;
import cn.shopex.ecshopx.salesperson.service.export.ProfitExportFileJobHandler;
import cn.shopex.ecshopx.salesperson.service.export.ProfitExportFileJobTypes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProfitExportFileJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesProfitExportFileJobHandler_forDistributor() {
		ProfitExportFileJobHandler jobHandler = mock(ProfitExportFileJobHandler.class);
		ProfitDistributorExportFileJobTypeHandler distHandler =
				new ProfitDistributorExportFileJobTypeHandler(jobHandler);
		ProfitSalespersonExportFileJobTypeHandler spHandler =
				new ProfitSalespersonExportFileJobTypeHandler(jobHandler);
		EspierExportFileJobDispatchHandler router =
				new EspierExportFileJobDispatchHandler(List.of(distHandler, spHandler));

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB, router);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 101L;
		long operatorId = 202L;
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("date", "202501");
		filter.put("profit_user_type", "x");

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", ProfitExportFileJobTypes.TYPE_PROFIT_DISTRIBUTOR);
		payload.put("company_id", companyId);
		payload.put("operator_id", operatorId);
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
		Map<String, Object> got = msg.payload();
		assertEquals(ProfitExportFileJobTypes.TYPE_PROFIT_DISTRIBUTOR, got.get("type"));
		assertEquals(companyId, got.get("company_id"));
		assertEquals(operatorId, got.get("operator_id"));
		@SuppressWarnings("unchecked")
		Map<String, Object> nested = (Map<String, Object>) got.get("filter");
		assertEquals("202501", nested.get("date"));
		assertEquals("x", nested.get("profit_user_type"));

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
												&& ctx.rawExportType()
														.equals(ProfitExportFileJobTypes.TYPE_PROFIT_DISTRIBUTOR)
												&& ctx.dateYm().equals("202501")
												&& ctx.distributorId() == operatorId
												&& "x".equals(ctx.profitUserTypeRaw())));
	}

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesProfitExportFileJobHandler_forSalesperson() {
		ProfitExportFileJobHandler jobHandler = mock(ProfitExportFileJobHandler.class);
		ProfitDistributorExportFileJobTypeHandler distHandler =
				new ProfitDistributorExportFileJobTypeHandler(jobHandler);
		ProfitSalespersonExportFileJobTypeHandler spHandler =
				new ProfitSalespersonExportFileJobTypeHandler(jobHandler);
		EspierExportFileJobDispatchHandler router =
				new EspierExportFileJobDispatchHandler(List.of(distHandler, spHandler));

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB, router);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 303L;
		long operatorId = 404L;
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("date", "202501");
		filter.put("profit_user_type", null);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", ProfitExportFileJobTypes.TYPE_PROFIT_SALESPERSON);
		payload.put("company_id", companyId);
		payload.put("operator_id", operatorId);
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
		Map<String, Object> got = msg.payload();
		assertEquals(ProfitExportFileJobTypes.TYPE_PROFIT_SALESPERSON, got.get("type"));
		assertEquals(companyId, got.get("company_id"));
		assertEquals(operatorId, got.get("operator_id"));
		@SuppressWarnings("unchecked")
		Map<String, Object> nested = (Map<String, Object>) got.get("filter");
		assertEquals("202501", nested.get("date"));
		assertNull(nested.get("profit_user_type"));

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
												&& ctx.rawExportType()
														.equals(ProfitExportFileJobTypes.TYPE_PROFIT_SALESPERSON)
												&& ctx.dateYm().equals("202501")
												&& ctx.distributorId() == operatorId
												&& ctx.profitUserTypeRaw() == null));
	}
}
