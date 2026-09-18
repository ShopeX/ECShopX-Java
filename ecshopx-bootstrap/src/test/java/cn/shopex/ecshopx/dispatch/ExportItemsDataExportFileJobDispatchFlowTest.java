package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.config.ExportItemsDataExportFileJobDispatchPublisherImpl;
import cn.shopex.ecshopx.config.EspierExportFileJobDispatchHandler;
import cn.shopex.ecshopx.goods.dispatch.ItemsDataExportFileJobTypes;
import cn.shopex.ecshopx.goods.dispatch.ItemsExportFileJobTypeHandler;
import cn.shopex.ecshopx.goods.service.export.ItemsDataExportFileJobHandler;
import cn.shopex.ecshopx.goods.service.export.ItemsDataExportContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

class ExportItemsDataExportFileJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesItemsDataExportFileJobHandler() {
		ItemsDataExportFileJobHandler jobHandler = mock(ItemsDataExportFileJobHandler.class);
		ItemsExportFileJobTypeHandler typeHandler = new ItemsExportFileJobTypeHandler(jobHandler);
		EspierExportFileJobDispatchHandler router = new EspierExportFileJobDispatchHandler(List.of(typeHandler));

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB_EXPORT_ITEMS_DATA, router);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 101L;
		long operatorId = 202L;
		Long merchantId = 303L;
		String operatorType = "merchant";
		String itemSource = "item";
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("item_name|contains", "foo");
		filter.put("merchant_id", merchantId);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", ItemsDataExportFileJobTypes.TYPE_ITEMS);
		payload.put("company_id", companyId);
		payload.put("operator_id", operatorId);
		payload.put("operator_type", operatorType);
		payload.put("merchant_id", merchantId);
		payload.put("item_source", itemSource);
		payload.put("filter", new LinkedHashMap<>(filter));

		facade.dispatchJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_EXPORT_ITEMS_DATA,
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
		assertEquals(EspierDispatchJobNames.EXPORT_FILE_JOB_EXPORT_ITEMS_DATA, msg.messageName());
		assertNull(msg.listenerName());
		Map<String, Object> got = msg.payload();
		assertEquals(ItemsDataExportFileJobTypes.TYPE_ITEMS, got.get("type"));
		assertEquals(companyId, asLong(got.get("company_id")));
		assertEquals(operatorId, asLong(got.get("operator_id")));
		assertEquals(operatorType, got.get("operator_type"));
		assertEquals(merchantId, asLong(got.get("merchant_id")));
		assertEquals(itemSource, got.get("item_source"));
		@SuppressWarnings("unchecked")
		Map<String, Object> nested = (Map<String, Object>) got.get("filter");
		assertEquals("foo", nested.get("item_name|contains"));

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
								(ItemsDataExportContext ctx) ->
										ctx.companyId() == companyId
												&& ctx.operatorId() == operatorId
												&& ItemsDataExportFileJobTypes.TYPE_ITEMS.equals(ctx.exportType())
												&& operatorType.equals(ctx.operatorType())
												&& merchantId.equals(ctx.merchantId())
												&& itemSource.equals(ctx.itemSource())
												&& "foo".equals(ctx.filterParams().get("item_name|contains"))));
	}

	@Test
	void dispatchJob_publishPayloadMatchesExportItemsDataExportFileJobEnvelope() {
		DispatchFacade facade = mock(DispatchFacade.class);
		Environment environment = mock(Environment.class);
		when(environment.acceptsProfiles(Profiles.of("local"))).thenReturn(false);
		ExportItemsDataExportFileJobDispatchPublisherImpl publisher =
				new ExportItemsDataExportFileJobDispatchPublisherImpl(facade, environment);

		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", 501L);
		filter.put("item_bn|in", List.of("a", "b"));

		publisher.publish(
				501L, 503L, ItemsDataExportFileJobTypes.TYPE_ITEMS, "merchant", 605L, "services", filter);

		verify(facade)
				.dispatchJob(
						eq(EspierDispatchJobNames.EXPORT_FILE_JOB_EXPORT_ITEMS_DATA),
						argThat(
								p -> {
									if (!(p instanceof Map)) {
										return false;
									}
									Map<?, ?> m = (Map<?, ?>) p;
									if (!ItemsDataExportFileJobTypes.TYPE_ITEMS.equals(String.valueOf(m.get("type")))) {
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
									if (!"services".equals(String.valueOf(m.get("item_source")))) {
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
