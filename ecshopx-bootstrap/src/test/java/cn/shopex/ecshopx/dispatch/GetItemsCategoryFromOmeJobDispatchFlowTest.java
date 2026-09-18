package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.GoodsBundleDispatchJobNames;
import cn.shopex.ecshopx.config.GetItemsCategoryFromOmeJobDispatchPublisherImpl;
import cn.shopex.ecshopx.goods.dispatch.GetItemsCategoryFromOmeJobHandler;
import cn.shopex.ecshopx.goods.service.ItemsCategorySaveService;
import cn.shopex.ecshopx.goods.service.ome.OmeItemCategoryFromOmeSyncRunner;
import cn.shopex.ecshopx.goods.service.ome.ShopexErpOpenApiClient;
import cn.shopex.ecshopx.goods.service.ome.ShopexErpSettingRedisAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GetItemsCategoryFromOmeJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerRunsCategoryGetList() {
		long companyId = 10L;

		ShopexErpSettingRedisAccessor settings = mock(ShopexErpSettingRedisAccessor.class);
		when(settings.getParsedSetting(companyId)).thenReturn(Map.of("is_openapi_open", true));

		ShopexErpOpenApiClient openApi = mock(ShopexErpOpenApiClient.class);
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("parent_code", "");
		row.put("cat_name", "RootCat");
		row.put("cat_code", "C1");
		row.put("cat_code_path", "1,2,3");
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("count", 1);
		data.put("lists", List.of(row));
		when(openApi.call(eq(companyId), eq("category.getList"), any())).thenReturn(
				Map.of("rsp", "succ", "data", data));

		ItemsCategorySaveService saveService = mock(ItemsCategorySaveService.class);
		ObjectMapper om = new ObjectMapper();

		OmeItemCategoryFromOmeSyncRunner runner =
				new OmeItemCategoryFromOmeSyncRunner(settings, openApi, saveService, om);
		GetItemsCategoryFromOmeJobHandler handler = new GetItemsCategoryFromOmeJobHandler(runner);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(GoodsBundleDispatchJobNames.GET_ITEMS_CATEGORY_FROM_OME, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("distributor_id", 0);

		facade.dispatchJob(
				GoodsBundleDispatchJobNames.GET_ITEMS_CATEGORY_FROM_OME,
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
		assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals("slow", msg.queue());
		assertNull(msg.delay());
		assertEquals(GoodsBundleDispatchJobNames.GET_ITEMS_CATEGORY_FROM_OME, msg.messageName());
		assertNull(msg.listenerName());

		Map<String, Object> got = msg.payload();
		assertEquals(companyId, asLong(got.get("company_id")));
		assertEquals(0, asInt(got.get("distributor_id")));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(openApi, times(1)).call(eq(companyId), eq("category.getList"), any());
		verify(saveService, times(1)).saveItemsCategory(eq(companyId), eq(0L), anyList());
	}

	@Test
	void dispatchJob_publishPayloadMatchesGetItemsCategoryFromOmeEnvelope() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		GetItemsCategoryFromOmeJobDispatchPublisherImpl publisher =
				new GetItemsCategoryFromOmeJobDispatchPublisherImpl(dispatchFacade);

		long companyId = 55L;
		publisher.enqueueGetItemsCategoryFromOme(companyId);

		verify(dispatchFacade)
				.dispatchJob(
						eq(GoodsBundleDispatchJobNames.GET_ITEMS_CATEGORY_FROM_OME),
						argThat(
								map -> companyId == asLong(map.get("company_id")) && 0 == asInt(map.get("distributor_id"))),
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
