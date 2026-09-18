package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.config.ExportItemsCodeDataExportFileJobDispatchPublisherImpl;
import cn.shopex.ecshopx.config.EspierExportFileJobDispatchHandler;
import cn.shopex.ecshopx.goods.dispatch.NormalItemsCodeExportFileJobTypeHandler;
import cn.shopex.ecshopx.goods.dispatch.NormalItemsCodeExportFileJobTypes;
import cn.shopex.ecshopx.goods.service.export.NormalItemsCodeExportFileJobHandler;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

class ExportItemsCodeDataExportFileJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesNormalItemsCodeExportFileJobHandler() {
		NormalItemsCodeExportFileJobHandler jobHandler = mock(NormalItemsCodeExportFileJobHandler.class);
		NormalItemsCodeExportFileJobTypeHandler typeHandler = new NormalItemsCodeExportFileJobTypeHandler(jobHandler);
		EspierExportFileJobDispatchHandler router = new EspierExportFileJobDispatchHandler(List.of(typeHandler));

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB_EXPORT_ITEMS_CODE_DATA, router);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 101L;
		long operatorId = 202L;
		Long merchantId = 303L;
		String operatorType = "merchant";

		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("export_type", "wxa");
		filter.put("merchant_id", merchantId);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", NormalItemsCodeExportFileJobTypes.TYPE_ITEMCODE);
		payload.put("company_id", companyId);
		payload.put("operator_id", operatorId);
		payload.put("operator_type", operatorType);
		payload.put("merchant_id", merchantId);
		payload.put("filter", new LinkedHashMap<>(filter));

		facade.dispatchJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_EXPORT_ITEMS_CODE_DATA,
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
		assertEquals(EspierDispatchJobNames.EXPORT_FILE_JOB_EXPORT_ITEMS_CODE_DATA, msg.messageName());
		assertNull(msg.listenerName());
		Map<String, Object> got = msg.payload();
		assertEquals(NormalItemsCodeExportFileJobTypes.TYPE_ITEMCODE, got.get("type"));
		assertEquals(companyId, asLong(got.get("company_id")));
		assertEquals(operatorId, asLong(got.get("operator_id")));
		assertEquals(operatorType, got.get("operator_type"));
		assertEquals(merchantId, asLong(got.get("merchant_id")));
		@SuppressWarnings("unchecked")
		Map<String, Object> nested = (Map<String, Object>) got.get("filter");
		assertEquals("wxa", nested.get("export_type"));

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
										ctx.getCompanyId() == companyId
												&& ctx.getOperatorId() == operatorId
												&& operatorType.equals(ctx.getOperatorType())
												&& merchantId.equals(ctx.getMerchantId())
												&& "wxa".equals(ctx.getFilterParams().get("export_type"))));
	}

	@Test
	void dispatchJob_publishPayloadMatchesExportItemsCodeDataExportFileEnvelope() {
		DispatchFacade facade = mock(DispatchFacade.class);
		Environment environment = mock(Environment.class);
		when(environment.acceptsProfiles(Profiles.of("local"))).thenReturn(false);
		ExportItemsCodeDataExportFileJobDispatchPublisherImpl publisher =
				new ExportItemsCodeDataExportFileJobDispatchPublisherImpl(facade, environment);

		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", 501L);
		filter.put("item_bn|in", List.of("a", "b"));

		publisher.publish(501L, 503L, "merchant", 605L, filter);

		verify(facade)
				.dispatchJob(
						eq(EspierDispatchJobNames.EXPORT_FILE_JOB_EXPORT_ITEMS_CODE_DATA),
						argThat(
								p -> {
									if (!(p instanceof Map)) {
										return false;
									}
									Map<?, ?> m = (Map<?, ?>) p;
									if (!NormalItemsCodeExportFileJobTypes.TYPE_ITEMCODE.equals(String.valueOf(m.get("type")))) {
										return false;
									}
									if (501L != asLong(m.get("company_id")) || 503L != asLong(m.get("operator_id"))) {
										return false;
									}
									if (!"merchant".equals(String.valueOf(m.get("operator_type")))) {
										return false;
									}
									if (605L != asLong(m.get("merchant_id"))) {
										return false;
									}
									Object rawFilter = m.get("filter");
									if (!(rawFilter instanceof Map)) {
										return false;
									}
									Map<?, ?> fm = (Map<?, ?>) rawFilter;
									Object ids = fm.get("item_bn|in");
									if (!(ids instanceof List<?> list) || list.size() != 2) {
										return false;
									}
									return 501L == asLong(fm.get("company_id"));
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
