package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.aftersales.dispatch.AftersaleFinancialExportFileJobTypeHandler;
import cn.shopex.ecshopx.aftersales.service.export.AftersalesFinancialExportFileJobHandler;
import cn.shopex.ecshopx.aftersales.service.export.AftersalesFinancialExportJobContext;
import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.config.EspierExportFileJobDispatchHandler;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AftersaleFinancialExportFileJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesFinancialExportHandler() {
		AftersalesFinancialExportFileJobHandler jobHandler = mock(AftersalesFinancialExportFileJobHandler.class);
		AftersaleFinancialExportFileJobTypeHandler typeHandler =
				new AftersaleFinancialExportFileJobTypeHandler(jobHandler);
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
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("aftersales_status", 2);
		filter.put("order_id_eq", 999L);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", AftersalesFinancialExportFileJobHandler.EXPORT_FILE_JOB_TYPE_AFTERSALE_FINANCIAL);
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
		assertEquals(
				AftersalesFinancialExportFileJobHandler.EXPORT_FILE_JOB_TYPE_AFTERSALE_FINANCIAL,
				got.get("type"));
		assertEquals(companyId, got.get("company_id"));
		assertEquals(operatorId, got.get("operator_id"));
		@SuppressWarnings("unchecked")
		Map<String, Object> nested = (Map<String, Object>) got.get("filter");
		assertEquals(2, nested.get("aftersales_status"));
		assertEquals(999L, nested.get("order_id_eq"));

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
								(AftersalesFinancialExportJobContext ctx) ->
										ctx.companyId() == companyId
												&& ctx.operatorId() == operatorId
												&& Integer.valueOf(2).equals(ctx.filter().get("aftersales_status"))
												&& Long.valueOf(999L).equals(ctx.filter().get("order_id_eq"))));
	}
}
