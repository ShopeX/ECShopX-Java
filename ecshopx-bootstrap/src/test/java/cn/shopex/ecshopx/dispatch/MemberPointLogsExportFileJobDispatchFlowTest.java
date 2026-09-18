package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.config.EspierExportFileJobDispatchHandler;
import cn.shopex.ecshopx.config.MemberPointLogsExportFileJobDispatchPublisherImpl;
import cn.shopex.ecshopx.point.dispatch.MemberPointLogsExportFileJobHandler;
import cn.shopex.ecshopx.point.dispatch.MemberPointLogsExportFileJobTypeHandler;
import cn.shopex.ecshopx.point.dispatch.MemberPointLogsExportFileJobTypes;
import cn.shopex.ecshopx.point.service.export.PointMemberLogCsvExportService;
import cn.shopex.ecshopx.point.service.export.PointMemberLogExportContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MemberPointLogsExportFileJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesPointMemberLogCsvExportService() {
		PointMemberLogCsvExportService csvExportService = mock(PointMemberLogCsvExportService.class);
		MemberPointLogsExportFileJobHandler jobHandler = new MemberPointLogsExportFileJobHandler(csvExportService);
		MemberPointLogsExportFileJobTypeHandler typeHandler = new MemberPointLogsExportFileJobTypeHandler(jobHandler);
		EspierExportFileJobDispatchHandler router = new EspierExportFileJobDispatchHandler(List.of(typeHandler));

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB_MEMBER_POINT_LOGS, router);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 701L;
		long operatorId = 702L;
		long supplierId = 0L;
		long userId = 5L;
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("datapass_block", 1);
		filter.put("user_id", userId);
		filter.put("mobile", "138");
		filter.put("username", "u");
		filter.put("name", "n");
		filter.put("date_begin", 100L);
		filter.put("date_end", 200L);

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", MemberPointLogsExportFileJobTypes.TYPE_MEMBER_POINT_LOGS);
		payload.put("company_id", companyId);
		payload.put("operator_id", operatorId);
		payload.put("supplier_id", supplierId);
		payload.put("filter", filter);

		facade.dispatchJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_MEMBER_POINT_LOGS,
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
		assertEquals(EspierDispatchJobNames.EXPORT_FILE_JOB_MEMBER_POINT_LOGS, msg.messageName());
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
								(PointMemberLogExportContext ctx) ->
										ctx.companyId() == companyId
												&& ctx.operatorId() == operatorId
												&& ctx.supplierId() == supplierId
												&& ctx.userIdParam() == userId
												&& "138".equals(ctx.mobile())
												&& ctx.datapassBlock()
												&& Long.valueOf(100L).equals(ctx.dateBegin())
												&& Long.valueOf(200L).equals(ctx.dateEnd())));
	}

	@Test
	void dispatchJob_publishPayloadMatchesMemberPointLogsEnvelope() {
		DispatchFacade facade = mock(DispatchFacade.class);
		MemberPointLogsExportFileJobDispatchPublisherImpl publisher =
				new MemberPointLogsExportFileJobDispatchPublisherImpl(facade);

		PointMemberLogExportContext ctx =
				new PointMemberLogExportContext(501L, 503L, 0L, 7L, null, "wu", "nm", 10L, 20L, false);

		publisher.enqueueMemberPointLogsExport(ctx);

		verify(facade)
				.dispatchJob(
						eq(EspierDispatchJobNames.EXPORT_FILE_JOB_MEMBER_POINT_LOGS),
						argThat(
								p -> {
									if (!(p instanceof Map)) {
										return false;
									}
									Map<?, ?> m = (Map<?, ?>) p;
									if (!MemberPointLogsExportFileJobTypes.TYPE_MEMBER_POINT_LOGS.equals(
											String.valueOf(m.get("type")))) {
										return false;
									}
									if (501L != asLong(m.get("company_id")) || 503L != asLong(m.get("operator_id"))) {
										return false;
									}
									if (0L != asLong(m.get("supplier_id"))) {
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
									if (7L != asLong(fm.get("user_id"))) {
										return false;
									}
									if (0 != asLong(fm.get("datapass_block"))) {
										return false;
									}
									Object mob = fm.get("mobile");
									if (mob != null) {
										return false;
									}
									if (!"wu".equals(String.valueOf(fm.get("username")))) {
										return false;
									}
									if (!"nm".equals(String.valueOf(fm.get("name")))) {
										return false;
									}
									if (10L != asLong(fm.get("date_begin")) || 20L != asLong(fm.get("date_end"))) {
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
												&& opts.delay() == null));
	}

	private static long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}
}
