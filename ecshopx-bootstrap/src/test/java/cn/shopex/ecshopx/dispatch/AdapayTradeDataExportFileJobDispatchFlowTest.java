package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.adapay.dispatch.AdapayTradeDataExportFileJobHandler;
import cn.shopex.ecshopx.adapay.dispatch.AdapayTradeDataExportFileJobTypeHandler;
import cn.shopex.ecshopx.adapay.dispatch.AdapayTradeDataExportFileJobTypes;
import cn.shopex.ecshopx.adapay.service.export.AdapayTradeDataCsvExportService;
import cn.shopex.ecshopx.adapay.service.export.AdapayTradeDataExportContext;
import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.config.AdapayTradeDataExportFileJobDispatchPublisherImpl;
import cn.shopex.ecshopx.config.EspierExportFileJobDispatchHandler;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AdapayTradeDataExportFileJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesAdapayTradeDataCsvExportService() {
		AdapayTradeDataCsvExportService csvExportService = mock(AdapayTradeDataCsvExportService.class);
		AdapayTradeDataExportFileJobHandler jobHandler = new AdapayTradeDataExportFileJobHandler(csvExportService);
		AdapayTradeDataExportFileJobTypeHandler typeHandler = new AdapayTradeDataExportFileJobTypeHandler(jobHandler);
		EspierExportFileJobDispatchHandler router = new EspierExportFileJobDispatchHandler(List.of(typeHandler));

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB_ADAPAY_TRADE_DATA, router);

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
		filter.put("status", "PAID");

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", AdapayTradeDataExportFileJobTypes.TYPE_ADAPAY_TRADE_DATA);
		payload.put("company_id", companyId);
		payload.put("operator_id", jwtOperatorId);
		payload.put("filter", filter);

		facade.dispatchJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_ADAPAY_TRADE_DATA,
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
		assertEquals(EspierDispatchJobNames.EXPORT_FILE_JOB_ADAPAY_TRADE_DATA, msg.messageName());
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
								(AdapayTradeDataExportContext ctx) ->
										ctx.companyId() == companyId
												&& ctx.jwtOperatorId() == jwtOperatorId
												&& "admin".equals(ctx.outputOperatorType())
												&& "PAID".equals(String.valueOf(ctx.preparedFilter().get("status")))));
	}

	@Test
	void dispatchJob_publishPayloadMatchesAdapayTradeDataEnvelope() {
		DispatchFacade facade = mock(DispatchFacade.class);
		AdapayTradeDataExportFileJobDispatchPublisherImpl publisher =
				new AdapayTradeDataExportFileJobDispatchPublisherImpl(facade);

		LinkedHashMap<String, Object> prepared = new LinkedHashMap<>();
		prepared.put("company_id", 501L);
		prepared.put("operator_type", "distributor");
		prepared.put("status", "SUCCESS");
		AdapayTradeDataExportContext ctx =
				new AdapayTradeDataExportContext(501L, 503L, "distributor", prepared);

		publisher.enqueueAdapayTradeDataExport(ctx);

		verify(facade)
				.dispatchJob(
						eq(EspierDispatchJobNames.EXPORT_FILE_JOB_ADAPAY_TRADE_DATA),
						argThat(
								p -> {
									if (!(p instanceof Map)) {
										return false;
									}
									Map<?, ?> m = (Map<?, ?>) p;
									if (!AdapayTradeDataExportFileJobTypes.TYPE_ADAPAY_TRADE_DATA.equals(
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
									if (!"distributor".equals(String.valueOf(fm.get("operator_type")))) {
										return false;
									}
									return "SUCCESS".equals(String.valueOf(fm.get("status")));
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
