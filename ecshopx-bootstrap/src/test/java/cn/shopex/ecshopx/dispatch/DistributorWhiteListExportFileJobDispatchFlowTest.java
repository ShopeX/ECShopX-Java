package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.config.DistributorWhiteListExportFileJobDispatchPublisherImpl;
import cn.shopex.ecshopx.config.EspierExportFileJobDispatchHandler;
import cn.shopex.ecshopx.distribution.dispatch.DistributorWhiteListExportFileJobTypeHandler;
import cn.shopex.ecshopx.distribution.dispatch.DistributorWhiteListExportFileJobTypes;
import cn.shopex.ecshopx.distribution.service.export.DistributorWhiteListExportFileJobHandler;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

class DistributorWhiteListExportFileJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesDistributorWhiteListExportFileJobHandler() {
		DistributorWhiteListExportFileJobHandler jobHandler = mock(DistributorWhiteListExportFileJobHandler.class);
		DistributorWhiteListExportFileJobTypeHandler typeHandler =
				new DistributorWhiteListExportFileJobTypeHandler(jobHandler);
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
		long merchantId = 404L;
		long supplierId = 505L;

		LinkedHashMap<String, Object> filterMap = new LinkedHashMap<>();
		filterMap.put("company_id", companyId);
		filterMap.put("distributor_ids", List.of(303L));
		filterMap.put("mobile_filter_active", true);
		filterMap.put("mobile", "138");
		filterMap.put("username_prefix", "u");
		filterMap.put("shop_not_found", false);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", DistributorWhiteListExportFileJobTypes.TYPE_DISTRIBUTOR_WHITE_LIST);
		payload.put("company_id", companyId);
		payload.put("operator_id", operatorId);
		payload.put("merchant_id", merchantId);
		payload.put("supplier_id", supplierId);
		payload.put("datapass_block", "hdr");
		payload.put("filter", new LinkedHashMap<>(filterMap));

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
		assertEquals(DistributorWhiteListExportFileJobTypes.TYPE_DISTRIBUTOR_WHITE_LIST, got.get("type"));
		assertEquals(companyId, asLong(got.get("company_id")));
		assertEquals(operatorId, asLong(got.get("operator_id")));
		assertEquals(merchantId, asLong(got.get("merchant_id")));
		assertEquals(supplierId, asLong(got.get("supplier_id")));
		assertEquals("hdr", got.get("datapass_block"));
		@SuppressWarnings("unchecked")
		Map<String, Object> nested = (Map<String, Object>) got.get("filter");
		assertEquals(companyId, asLong(nested.get("company_id")));
		assertEquals(true, nested.get("mobile_filter_active"));
		assertEquals("138", nested.get("mobile"));
		assertEquals("u", nested.get("username_prefix"));

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
												&& ctx.merchantId() == merchantId
												&& ctx.supplierId() == supplierId
												&& "hdr".equals(ctx.datapassBlockRaw())
												&& ctx.exportFilter().companyId() == companyId
												&& Objects.equals(
														ctx.exportFilter().distributorIds(), List.of(303L))
												&& ctx.exportFilter().mobileFilterActive()
												&& "138".equals(ctx.exportFilter().mobile())
												&& "u".equals(ctx.exportFilter().usernamePrefix())
												&& !ctx.exportFilter().shopNotFound()));
	}

	@Test
	void dispatchJob_publishPayloadMatchesDistributorWhiteListExportFileJobEnvelope() {
		DispatchFacade facade = mock(DispatchFacade.class);
		Environment environment = mock(Environment.class);
		when(environment.acceptsProfiles(Profiles.of("local"))).thenReturn(false);
		DistributorWhiteListExportFileJobDispatchPublisherImpl publisher =
				new DistributorWhiteListExportFileJobDispatchPublisherImpl(facade, environment);

		LinkedHashMap<String, Object> filterMap = new LinkedHashMap<>();
		filterMap.put("company_id", 501L);
		filterMap.put("distributor_ids", List.of(502L));
		filterMap.put("mobile_filter_active", false);
		filterMap.put("mobile", "");
		filterMap.put("username_prefix", null);
		filterMap.put("shop_not_found", false);

		publisher.publish(501L, 503L, 504L, 506L, filterMap, "block");

		verify(facade)
				.dispatchJob(
						eq(EspierDispatchJobNames.EXPORT_FILE_JOB),
						argThat(
								p -> {
									if (!(p instanceof Map)) {
										return false;
									}
									Map<?, ?> m = (Map<?, ?>) p;
									if (!DistributorWhiteListExportFileJobTypes.TYPE_DISTRIBUTOR_WHITE_LIST.equals(
											String.valueOf(m.get("type")))) {
										return false;
									}
									if (501L != asLong(m.get("company_id"))
											|| 503L != asLong(m.get("operator_id"))
											|| 504L != asLong(m.get("merchant_id"))
											|| 506L != asLong(m.get("supplier_id"))) {
										return false;
									}
									if (!"block".equals(m.get("datapass_block"))) {
										return false;
									}
									Object rawFilter = m.get("filter");
									if (!(rawFilter instanceof Map)) {
										return false;
									}
									Map<?, ?> fm = (Map<?, ?>) rawFilter;
									return 501L == asLong(fm.get("company_id"))
											&& Boolean.FALSE.equals(fm.get("mobile_filter_active"));
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
