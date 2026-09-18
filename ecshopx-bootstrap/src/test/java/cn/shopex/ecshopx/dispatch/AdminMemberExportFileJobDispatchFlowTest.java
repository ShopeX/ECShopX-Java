package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.config.AdminMemberExportFileJobDispatchPublisherImpl;
import cn.shopex.ecshopx.config.EspierExportFileJobDispatchHandler;
import cn.shopex.ecshopx.members.dispatch.AdminMemberExportFileJobHandler;
import cn.shopex.ecshopx.members.dispatch.AdminMemberExportFileJobPayloadSupport;
import cn.shopex.ecshopx.members.dispatch.AdminMemberExportFileJobTypeHandler;
import cn.shopex.ecshopx.members.dispatch.AdminMemberExportFileJobTypes;
import cn.shopex.ecshopx.members.service.admin.dto.AdminMemberBatchOperatingMemberQueryFilter;
import cn.shopex.ecshopx.members.service.export.AdminMemberCsvExportRunService;
import cn.shopex.ecshopx.members.service.export.AdminMemberExportJobContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AdminMemberExportFileJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesAdminMemberCsvExportRunService() {
		AdminMemberCsvExportRunService runService = mock(AdminMemberCsvExportRunService.class);
		AdminMemberExportFileJobHandler jobHandler = new AdminMemberExportFileJobHandler(runService);
		AdminMemberExportFileJobTypeHandler typeHandler = new AdminMemberExportFileJobTypeHandler(jobHandler);
		EspierExportFileJobDispatchHandler router = new EspierExportFileJobDispatchHandler(List.of(typeHandler));

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB_ADMIN_MEMBER_EXPORT, router);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 701L;
		long operatorId = 702L;
		AdminMemberBatchOperatingMemberQueryFilter filter = new AdminMemberBatchOperatingMemberQueryFilter();
		filter.setCompanyId(companyId);
		filter.setMembersGradeId(42L);
		AdminMemberExportJobContext ctx =
				new AdminMemberExportJobContext(companyId, operatorId, 0L, 0L, true, filter);
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>(AdminMemberExportFileJobPayloadSupport.toPayload(ctx));

		facade.dispatchJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_ADMIN_MEMBER_EXPORT,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						AdminMemberExportFileJobDispatchPublisherImpl.ADMIN_MEMBER_EXPORT_JOB_QUEUE,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals(AdminMemberExportFileJobDispatchPublisherImpl.ADMIN_MEMBER_EXPORT_JOB_QUEUE, msg.queue());
		assertNull(msg.delay());
		assertEquals(EspierDispatchJobNames.EXPORT_FILE_JOB_ADMIN_MEMBER_EXPORT, msg.messageName());
		assertNull(msg.listenerName());
		assertEquals(AdminMemberExportFileJobTypes.TYPE_ADMIN_MEMBER_EXPORT, String.valueOf(msg.payload().get("type")));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(runService, times(1))
				.runExport(
						argThat(
								c ->
										c.companyId() == companyId
												&& c.operatorId() == operatorId
												&& c.datapassBlock()
												&& c.queryFilter().getCompanyId() == companyId
												&& Long.valueOf(42L).equals(c.queryFilter().getMembersGradeId())));
	}

	@Test
	void dispatchJob_publishPayloadMatchesAdminMemberExportEnvelope() {
		DispatchFacade facade = mock(DispatchFacade.class);
		AdminMemberExportFileJobDispatchPublisherImpl publisher = new AdminMemberExportFileJobDispatchPublisherImpl(facade);

		AdminMemberBatchOperatingMemberQueryFilter filter = new AdminMemberBatchOperatingMemberQueryFilter();
		filter.setCompanyId(501L);
		filter.setMembersGradeId(9L);
		filter.setRemarksLike("vip");
		AdminMemberExportJobContext ctx =
				new AdminMemberExportJobContext(501L, 503L, 0L, 0L, false, filter);

		publisher.enqueueAdminMemberExport(ctx);

		verify(facade)
				.dispatchJob(
						eq(EspierDispatchJobNames.EXPORT_FILE_JOB_ADMIN_MEMBER_EXPORT),
						argThat(
								p -> {
									if (!(p instanceof Map)) {
										return false;
									}
									Map<?, ?> m = (Map<?, ?>) p;
									if (!AdminMemberExportFileJobTypes.TYPE_ADMIN_MEMBER_EXPORT.equals(
											String.valueOf(m.get("type")))) {
										return false;
									}
									if (501L != asLong(m.get("company_id")) || 503L != asLong(m.get("operator_id"))) {
										return false;
									}
									if (0L != asLong(m.get("supplier_id")) || 0L != asLong(m.get("merchant_id"))) {
										return false;
									}
									if (0 != asLong(m.get("datapass_block"))) {
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
									if (9L != asLong(fm.get("members_grade_id"))) {
										return false;
									}
									if (!"vip".equals(String.valueOf(fm.get("remarks_like")))) {
										return false;
									}
									return true;
								}),
						argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.ASYNC
												&& opts.driverOverride() == DispatchDriverType.REDIS
												&& AdminMemberExportFileJobDispatchPublisherImpl.ADMIN_MEMBER_EXPORT_JOB_QUEUE
														.equals(opts.queue())
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
