package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.config.EspierExportFileJobDispatchHandler;
import cn.shopex.ecshopx.hfpay.dispatch.HfpayWithdrawRecordExportFileJobTypeHandler;
import cn.shopex.ecshopx.hfpay.service.export.HfpayWithdrawRecordExportFileJobHandler;
import cn.shopex.ecshopx.hfpay.service.export.HfpayWithdrawRecordExportFileJobTypes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HfpayWithdrawRecordExportFileJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesHfpayWithdrawRecordExportFileJobHandler() {
		HfpayWithdrawRecordExportFileJobHandler jobHandler = mock(HfpayWithdrawRecordExportFileJobHandler.class);
		HfpayWithdrawRecordExportFileJobTypeHandler typeHandler = new HfpayWithdrawRecordExportFileJobTypeHandler(jobHandler);
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
		filter.put("start_date", "2025-01-01 00:00:00");
		filter.put("end_date", "2025-01-31 23:59:59");
		filter.put("distributor_id", 7L);
		filter.put("order_id", "9001");
		filter.put("cash_status", "2");

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", HfpayWithdrawRecordExportFileJobTypes.TYPE_HFPAY_WITHDRAW_RECORD);
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
		assertEquals(HfpayWithdrawRecordExportFileJobTypes.TYPE_HFPAY_WITHDRAW_RECORD, got.get("type"));
		assertEquals(companyId, got.get("company_id"));
		assertEquals(operatorId, got.get("operator_id"));
		@SuppressWarnings("unchecked")
		Map<String, Object> nested = (Map<String, Object>) got.get("filter");
		assertEquals(companyId, ((Number) nested.get("company_id")).longValue());
		assertEquals("2025-01-01 00:00:00", nested.get("start_date"));
		assertEquals("2025-01-31 23:59:59", nested.get("end_date"));
		assertEquals(7L, ((Number) nested.get("distributor_id")).longValue());
		assertEquals("9001", nested.get("order_id"));
		assertEquals("2", nested.get("cash_status"));

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
						eq(companyId),
						eq(operatorId),
						argThat(
								f ->
										f.get("company_id").equals(companyId)
												&& "2025-01-01 00:00:00".equals(f.get("start_date"))
												&& "2025-01-31 23:59:59".equals(f.get("end_date"))
												&& Long.valueOf(7L).equals(((Number) f.get("distributor_id")).longValue())
												&& "9001".equals(f.get("order_id"))
												&& "2".equals(f.get("cash_status"))));
	}
}
