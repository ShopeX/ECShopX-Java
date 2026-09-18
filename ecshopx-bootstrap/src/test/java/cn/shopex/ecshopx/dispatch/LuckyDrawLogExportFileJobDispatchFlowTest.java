package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.config.EspierExportFileJobDispatchHandler;
import cn.shopex.ecshopx.config.LuckyDrawLogExportFileJobDispatchPublisherImpl;
import cn.shopex.ecshopx.promotions.dispatch.LuckyDrawLogExportFileJobHandler;
import cn.shopex.ecshopx.promotions.dispatch.LuckyDrawLogExportFileJobTypeHandler;
import cn.shopex.ecshopx.promotions.dispatch.LuckyDrawLogExportFileJobTypes;
import cn.shopex.ecshopx.promotions.service.TurntableConfigService;
import cn.shopex.ecshopx.promotions.service.export.LuckyDrawLogExportJobContext;
import cn.shopex.ecshopx.promotions.service.export.TurntableLuckyDrawLogExportAsyncService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

class LuckyDrawLogExportFileJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesTurntableLuckyDrawLogExport() {
		TurntableLuckyDrawLogExportAsyncService asyncService = mock(TurntableLuckyDrawLogExportAsyncService.class);
		TurntableConfigService turntableConfigService = mock(TurntableConfigService.class);
		when(turntableConfigService.parseRequiredActivityId("55")).thenReturn(55L);
		LuckyDrawLogExportFileJobHandler jobHandler = new LuckyDrawLogExportFileJobHandler(asyncService);
		LuckyDrawLogExportFileJobTypeHandler typeHandler =
				new LuckyDrawLogExportFileJobTypeHandler(jobHandler, turntableConfigService);
		EspierExportFileJobDispatchHandler router = new EspierExportFileJobDispatchHandler(List.of(typeHandler));

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB_LUCKDRAW_LOG, router);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 701L;
		long operatorId = 702L;
		long merchantId = 703L;
		long supplierId = 704L;
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("activity_id", "55");
		filter.put("datapass_block", "dp");

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", LuckyDrawLogExportFileJobTypes.TYPE_EXPORT_LUCKDRAW_LOG);
		payload.put("company_id", companyId);
		payload.put("operator_id", operatorId);
		payload.put("merchant_id", merchantId);
		payload.put("supplier_id", supplierId);
		payload.put("filter", filter);

		facade.dispatchJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_LUCKDRAW_LOG,
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
		assertEquals(EspierDispatchJobNames.EXPORT_FILE_JOB_LUCKDRAW_LOG, msg.messageName());
		assertNull(msg.listenerName());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(asyncService, times(1))
				.runExport(
						argThat(
								(LuckyDrawLogExportJobContext ctx) ->
										ctx.companyId() == companyId
												&& ctx.operatorId() == operatorId
												&& ctx.merchantId() == merchantId
												&& ctx.supplierId() == supplierId
												&& ctx.actId() == 55L
												&& "dp".equals(ctx.datapassBlockRaw())));
	}

	@Test
	void dispatchJob_localProfile_syncDriver_queueNull() {
		Environment env = mock(Environment.class);
		when(env.acceptsProfiles(Profiles.of("local"))).thenReturn(true);
		DispatchFacade facade = mock(DispatchFacade.class);
		LuckyDrawLogExportFileJobDispatchPublisherImpl publisher = new LuckyDrawLogExportFileJobDispatchPublisherImpl(facade, env);

		publisher.enqueue(11L, 12L, 13L, 14L, "88", null);

		verify(facade)
				.dispatchJob(
						eq(EspierDispatchJobNames.EXPORT_FILE_JOB_LUCKDRAW_LOG),
						argThat(
								p -> {
									if (!(p instanceof Map)) {
										return false;
									}
									Map<?, ?> m = (Map<?, ?>) p;
									if (!LuckyDrawLogExportFileJobTypes.TYPE_EXPORT_LUCKDRAW_LOG.equals(String.valueOf(m.get("type")))) {
										return false;
									}
									if (11L != asLong(m.get("company_id"))
											|| 12L != asLong(m.get("operator_id"))
											|| 13L != asLong(m.get("merchant_id"))
											|| 14L != asLong(m.get("supplier_id"))) {
										return false;
									}
									Object rawFilter = m.get("filter");
									if (!(rawFilter instanceof Map)) {
										return false;
									}
									Map<?, ?> fm = (Map<?, ?>) rawFilter;
									if (!"88".equals(String.valueOf(fm.get("activity_id")))) {
										return false;
									}
									return fm.get("datapass_block") == null;
								}),
						argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.SYNC
												&& opts.driverOverride() == DispatchDriverType.SYNC
												&& opts.queue() == null
												&& opts.delay() == null));
	}

	private static long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}
}
