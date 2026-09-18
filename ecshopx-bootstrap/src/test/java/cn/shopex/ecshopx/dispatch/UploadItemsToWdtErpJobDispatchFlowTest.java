package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.GoodsBundleDispatchJobNames;
import cn.shopex.ecshopx.config.UploadItemsToWdtErpJobDispatchPublisherImpl;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.goods.dispatch.UploadItemsToWdtErpJobDispatchPublisher;
import cn.shopex.ecshopx.goods.dispatch.UploadItemsToWdtErpJobHandler;
import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import cn.shopex.ecshopx.goods.service.wdterp.WdtErpUploadItemsOrchestratorService;
import cn.shopex.ecshopx.goods.service.wdterp.WdtErpUploadItemsPageQueryService;
import cn.shopex.ecshopx.goods.integration.wdterp.WdtErpItemStructAssembler;
import cn.shopex.ecshopx.goods.integration.wdterp.WdtErpItemStructAssembler.WdtErpItemStruct;
import cn.shopex.ecshopx.goods.integration.wdterp.WdtErpUploadItemsBatchHandler;
import cn.shopex.ecshopx.systemlink.wdterp.WdtErpOpenApiClient;
import cn.shopex.ecshopx.systemlink.wdterp.WdtErpUploadItemsBatchHandlerImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class UploadItemsToWdtErpJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesWdtOpenApi() {
		long companyId = 42L;
		long itemId = 9001L;
		List<Long> itemIds = List.of(itemId);

		String settingJson =
				"{\"is_open\":true,\"sid\":\"s1\",\"app_key\":\"k1\",\"app_secret\":\"secret:salt\",\"shop_no\":\"shop-default\"}";
		StringRedisTemplate redis = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		ValueOperations<String, String> vo = mock(ValueOperations.class);
		when(redis.opsForValue()).thenReturn(vo);
		when(vo.get(anyString()))
				.thenAnswer(
						inv -> {
							String k = inv.getArgument(0);
							return k != null && k.startsWith("WdtErpSetting:") ? settingJson : null;
						});

		Map<String, Object> goods = new LinkedHashMap<>();
		goods.put("goods_no", "bn9001");
		goods.put("goods_name", "Item 9001");
		Map<String, Object> spec = new LinkedHashMap<>();
		spec.put("spec_no", "bn9001");
		spec.put("spec_name", "Item 9001");
		spec.put("retail_price", 9.99);
		Map<String, Object> goodsPush = new LinkedHashMap<>();
		goodsPush.put("goods", goods);
		goodsPush.put("specList", List.of(spec));
		Map<String, Object> apiRow = new LinkedHashMap<>();
		apiRow.put("goods_id", 1);
		apiRow.put("spec_id", "bn9001");
		apiRow.put("goods_no", "bn9001");
		apiRow.put("spec_no", "bn9001");
		apiRow.put("goods_name", "Item 9001");
		apiRow.put("spec_name", "Item 9001");
		apiRow.put("status", 1);
		apiRow.put("price", 9.99);
		apiRow.put("stock_num", 3);
		WdtErpItemStruct struct = new WdtErpItemStruct(goodsPush, List.of(apiRow));

		WdtErpItemStructAssembler assembler = mock(WdtErpItemStructAssembler.class);
		when(assembler.getItemStruct(companyId, itemId, 0L)).thenReturn(struct);

		WdtErpOpenApiClient openApi = mock(WdtErpOpenApiClient.class);
		when(openApi.call(anyLong(), anyString(), any(), anyString(), anyString(), anyString()))
				.thenReturn(Map.of("ok", true));

		DistributorListQueryService distributors = mock(DistributorListQueryService.class);
		ObjectMapper om = new ObjectMapper();
		WdtErpUploadItemsBatchHandlerImpl batchImpl =
				new WdtErpUploadItemsBatchHandlerImpl(redis, om, assembler, openApi, distributors);

		UploadItemsToWdtErpJobHandler handler = new UploadItemsToWdtErpJobHandler(batchImpl);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(GoodsBundleDispatchJobNames.UPLOAD_ITEMS_TO_WDT_ERP, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("item_ids", itemIds);
		payload.put("distributor_id", 0L);

		facade.dispatchJob(
				GoodsBundleDispatchJobNames.UPLOAD_ITEMS_TO_WDT_ERP,
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
		assertEquals(GoodsBundleDispatchJobNames.UPLOAD_ITEMS_TO_WDT_ERP, msg.messageName());
		assertNull(msg.listenerName());
		Map<String, Object> got = msg.payload();
		assertEquals(companyId, asLong(got.get("company_id")));
		assertEquals(0L, asLong(got.get("distributor_id")));
		@SuppressWarnings("unchecked")
		List<Object> ids = (List<Object>) got.get("item_ids");
		assertEquals(1, ids.size());
		assertEquals(itemId, asLong(ids.get(0)));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(openApi)
				.call(eq(companyId), eq("goods.Goods.push"), any(), eq("s1"), eq("k1"), eq("secret:salt"));
		verify(openApi)
				.call(eq(companyId), eq("goods.ApiGoods.upload"), any(), eq("s1"), eq("k1"), eq("secret:salt"));
	}

	@Test
	void dispatchJob_publishPayloadMatchesUploadItemsEnvelope() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		UploadItemsToWdtErpJobDispatchPublisherImpl publisher =
				new UploadItemsToWdtErpJobDispatchPublisherImpl(dispatchFacade);

		long companyId = 7L;
		List<Long> itemIds = List.of(100L, 101L);
		long distributorId = 3L;
		publisher.enqueueUploadItemsToWdtErp(companyId, itemIds, distributorId);

		verify(dispatchFacade)
				.dispatchJob(
						eq(GoodsBundleDispatchJobNames.UPLOAD_ITEMS_TO_WDT_ERP),
						argThat(
								map -> {
									if (companyId != asLong(map.get("company_id"))) {
										return false;
									}
									if (distributorId != asLong(map.get("distributor_id"))) {
										return false;
									}
									@SuppressWarnings("unchecked")
									List<Object> ids = (List<Object>) map.get("item_ids");
									if (ids == null || ids.size() != 2) {
										return false;
									}
									if (100L != asLong(ids.get(0)) || 101L != asLong(ids.get(1))) {
										return false;
									}
									return map.size() == 3;
								}),
						argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.ASYNC
												&& opts.driverOverride() == DispatchDriverType.REDIS
												&& "slow".equals(opts.queue())
												&& opts.delay() == null));
	}

	@Test
	void jobHandler_forwardsParsedPayloadToBatchHandler() {
		WdtErpUploadItemsBatchHandler batch = mock(WdtErpUploadItemsBatchHandler.class);
		UploadItemsToWdtErpJobHandler handler = new UploadItemsToWdtErpJobHandler(batch);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 11L);
		payload.put("distributor_id", 2L);
		payload.put("item_ids", List.of(201, 202L));

		handler.handle(payload);

		verify(batch).handleBatch(eq(11L), argThat(list -> list.equals(List.of(201L, 202L))), eq(2L));
	}

	@Test
	void orchestrator_standardItemsListBranch_enqueuesJobWithDistributorZeroAndItemIds() {
		long companyId = 33L;
		String operatorType = "shop";
		long distributorId = 0L;
		List<Long> itemIds = List.of(501L, 502L);

		DistributorListQueryService distributors = mock(DistributorListQueryService.class);
		ItemsCategoryDistributorIdResolver productModelResolver = mock(ItemsCategoryDistributorIdResolver.class);
		when(productModelResolver.resolveProductModel(companyId)).thenReturn("standard");

		Map<String, Object> row1 = new LinkedHashMap<>();
		row1.put("item_id", itemIds.get(0));
		Map<String, Object> row2 = new LinkedHashMap<>();
		row2.put("item_id", itemIds.get(1));
		Map<String, Object> pageResult = new LinkedHashMap<>();
		pageResult.put("total_count", 2L);
		pageResult.put("list", List.of(row1, row2));

		WdtErpUploadItemsPageQueryService pageQuery = mock(WdtErpUploadItemsPageQueryService.class);
		when(pageQuery.fetchPage(
						eq(companyId),
						eq(operatorType),
						eq(distributorId),
						eq("standard"),
						any(),
						eq(1),
						eq(100),
						any()))
				.thenReturn(pageResult);

		UploadItemsToWdtErpJobDispatchPublisher publisher = mock(UploadItemsToWdtErpJobDispatchPublisher.class);

		StringRedisTemplate redis = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		ValueOperations<String, String> vo = mock(ValueOperations.class);
		when(redis.opsForValue()).thenReturn(vo);

		WdtErpUploadItemsOrchestratorService orchestrator =
				new WdtErpUploadItemsOrchestratorService(
						distributors, productModelResolver, pageQuery, publisher, redis);

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("item_id", 701L);

		orchestrator.run(null, companyId, operatorType, distributorId, merged);

		verify(publisher, times(1)).enqueueUploadItemsToWdtErp(companyId, itemIds, 0L);
		verify(pageQuery, times(1))
				.fetchPage(
						eq(companyId),
						eq(operatorType),
						eq(distributorId),
						eq("standard"),
						any(),
						eq(1),
						eq(100),
						any());
	}

	private static long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}
}
