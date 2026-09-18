package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.config.DistributorItemsExportFileJobDispatchPublisherImpl;
import cn.shopex.ecshopx.config.EspierExportFileJobDispatchHandler;
import cn.shopex.ecshopx.goods.dispatch.DistributorItemsExportFileJobTypeHandler;
import cn.shopex.ecshopx.goods.dispatch.DistributorItemsExportFileJobTypes;
import cn.shopex.ecshopx.goods.service.export.DistributorItemsExportFileJobHandler;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

class DistributorItemsExportFileJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesDistributorItemsExportFileJobHandler() {
		DistributorItemsExportFileJobHandler jobHandler = mock(DistributorItemsExportFileJobHandler.class);
		DistributorItemsExportFileJobTypeHandler typeHandler = new DistributorItemsExportFileJobTypeHandler(jobHandler);
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
		long distributorId = 303L;
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("distributor_id", distributorId);
		filter.put("item_type", "normal");
		filter.put("__dist_is_can_sale_filter", Boolean.TRUE);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", DistributorItemsExportFileJobTypes.TYPE_DISTRIBUTOR_ITEMS);
		payload.put("company_id", companyId);
		payload.put("operator_id", operatorId);
		payload.put("filter", new LinkedHashMap<>(filter));
		payload.put("accept_language", "en");
		payload.put("page_size", 500);

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
		assertEquals(DistributorItemsExportFileJobTypes.TYPE_DISTRIBUTOR_ITEMS, got.get("type"));
		assertEquals(companyId, asLong(got.get("company_id")));
		assertEquals(operatorId, asLong(got.get("operator_id")));
		assertEquals("en", got.get("accept_language"));
		assertEquals(500, asInt(got.get("page_size")));
		@SuppressWarnings("unchecked")
		Map<String, Object> nested = (Map<String, Object>) got.get("filter");
		assertEquals(distributorId, asLong(nested.get("distributor_id")));
		assertEquals("normal", nested.get("item_type"));
		assertEquals(Boolean.TRUE, nested.get("__dist_is_can_sale_filter"));

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
												&& ctx.getDistributorId() == distributorId
												&& ctx.getOperatorId() == operatorId
												&& "en".equals(ctx.getAcceptLanguage())
												&& ctx.getPageSize() == 500
												&& "normal".equals(ctx.getFilterBase().get("item_type"))
												&& Boolean.TRUE.equals(
														ctx.getFilterBase().get("__dist_is_can_sale_filter"))));
	}

	@Test
	void dispatchJob_publishPayloadMatchesDistributorItemsExportFileJobEnvelope() {
		DispatchFacade facade = mock(DispatchFacade.class);
		Environment environment = mock(Environment.class);
		when(environment.acceptsProfiles(Profiles.of("local"))).thenReturn(false);
		DistributorItemsExportFileJobDispatchPublisherImpl publisher =
				new DistributorItemsExportFileJobDispatchPublisherImpl(facade, environment);

		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", 501L);
		filter.put("distributor_id", 502L);
		filter.put("item_type", "normal");

		publisher.publish(501L, 503L, "zh-Hans", filter, 500);

		verify(facade)
				.dispatchJob(
						eq(EspierDispatchJobNames.EXPORT_FILE_JOB),
						argThat(
								p -> {
									if (!(p instanceof Map)) {
										return false;
									}
									Map<?, ?> m = (Map<?, ?>) p;
									if (!DistributorItemsExportFileJobTypes.TYPE_DISTRIBUTOR_ITEMS.equals(
											String.valueOf(m.get("type")))) {
										return false;
									}
									if (501L != asLong(m.get("company_id")) || 503L != asLong(m.get("operator_id"))) {
										return false;
									}
									if (!"zh-Hans".equals(m.get("accept_language")) || 500 != asInt(m.get("page_size"))) {
										return false;
									}
									Object rawFilter = m.get("filter");
									if (!(rawFilter instanceof Map)) {
										return false;
									}
									Map<?, ?> fm = (Map<?, ?>) rawFilter;
									return 502L == asLong(fm.get("distributor_id"));
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

	private static int asInt(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(String.valueOf(v).trim());
	}
}
