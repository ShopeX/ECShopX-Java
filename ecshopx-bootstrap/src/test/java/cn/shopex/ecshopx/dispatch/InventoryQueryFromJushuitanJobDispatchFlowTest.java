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
import cn.shopex.ecshopx.config.InventoryQueryFromJushuitanJobDispatchPublisherImpl;
import cn.shopex.ecshopx.goods.dispatch.InventoryQueryFromJushuitanJobHandler;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.integration.jushuitan.JushuitanInventoryQueryBatchHandler;
import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import cn.shopex.ecshopx.goods.service.jushuitan.JushuitanInventoryPersistService;
import cn.shopex.ecshopx.goods.service.jushuitan.JushuitanItemStoreQueryStructService;
import cn.shopex.ecshopx.goods.service.jushuitan.JushuitanQueryInventoryOrchestratorService;
import cn.shopex.ecshopx.systemlink.jushuitan.JushuitanInventoryQueryBatchHandlerImpl;
import cn.shopex.ecshopx.systemlink.jushuitan.JushuitanOpenApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class InventoryQueryFromJushuitanJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesItemStoreQuery() {
		long companyId = 42L;
		long itemId = 9001L;
		List<Long> itemIds = List.of(itemId);

		String settingJson =
				"{\"is_open\":true,\"access_token\":\"tok\",\"shop_id\":\"99\"}";
		StringRedisTemplate redis = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		ValueOperations<String, String> vo = mock(ValueOperations.class);
		when(redis.opsForValue()).thenReturn(vo);
		when(vo.get(anyString()))
				.thenAnswer(
						inv -> {
							String k = inv.getArgument(0);
							return k != null && k.startsWith("JushuitanSetting:") ? settingJson : null;
						});

		JushuitanItemStoreQueryStructService structService = mock(JushuitanItemStoreQueryStructService.class);
		when(structService.buildItemStoreQueryPayload(eq(companyId), any()))
				.thenReturn(
						Map.of(
								"wms_co_id",
								0,
								"page_index",
								1,
								"page_size",
								20,
								"sku_ids",
								"SKU-1"));

		JushuitanOpenApiClient openApi = mock(JushuitanOpenApiClient.class);
		when(openApi.call(anyLong(), anyString(), any(), anyString()))
				.thenReturn(Map.of("code", "0", "inventorys", List.of(Map.of("qty", 1))));

		JushuitanInventoryPersistService persist = mock(JushuitanInventoryPersistService.class);
		ObjectMapper om = new ObjectMapper();
		JushuitanInventoryQueryBatchHandlerImpl batchImpl =
				new JushuitanInventoryQueryBatchHandlerImpl(redis, om, openApi, structService, persist);

		InventoryQueryFromJushuitanJobHandler handler = new InventoryQueryFromJushuitanJobHandler(batchImpl);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(GoodsBundleDispatchJobNames.INVENTORY_QUERY_FROM_JUSHUITAN, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("item_ids", itemIds);

		facade.dispatchJob(
				GoodsBundleDispatchJobNames.INVENTORY_QUERY_FROM_JUSHUITAN,
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
		assertEquals(GoodsBundleDispatchJobNames.INVENTORY_QUERY_FROM_JUSHUITAN, msg.messageName());
		assertNull(msg.listenerName());
		Map<String, Object> got = msg.payload();
		assertEquals(companyId, asLong(got.get("company_id")));
		@SuppressWarnings("unchecked")
		List<Object> ids = (List<Object>) got.get("item_ids");
		assertEquals(1, ids.size());
		assertEquals(itemId, asLong(ids.get(0)));
		assertEquals(2, got.size());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(openApi).call(eq(companyId), eq("item_store_query"), any(), eq("tok"));
		verify(persist, times(1)).persistInventoriesFromJushuitan(eq(companyId), any());
	}

	@Test
	void dispatchJob_publishPayloadMatchesInventoryQueryFromJushuitanEnvelope() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		InventoryQueryFromJushuitanJobDispatchPublisherImpl publisher =
				new InventoryQueryFromJushuitanJobDispatchPublisherImpl(dispatchFacade);

		long companyId = 7L;
		List<Long> itemIds = List.of(100L, 101L);
		publisher.enqueueInventoryQueryFromJushuitan(companyId, itemIds);

		verify(dispatchFacade)
				.dispatchJob(
						eq(GoodsBundleDispatchJobNames.INVENTORY_QUERY_FROM_JUSHUITAN),
						argThat(
								map -> {
									if (companyId != asLong(map.get("company_id"))) {
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
									return map.size() == 2;
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
	void jobHandler_forwardsParsedPayloadToInventoryQueryBatchHandler() {
		JushuitanInventoryQueryBatchHandler batch = mock(JushuitanInventoryQueryBatchHandler.class);
		InventoryQueryFromJushuitanJobHandler handler = new InventoryQueryFromJushuitanJobHandler(batch);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 11L);
		payload.put("item_ids", List.of(201, 202L));

		handler.handle(payload);

		verify(batch).handleBatches(eq(11L), argThat(list -> list.equals(List.of(201L, 202L))));
	}

	@Test
	void orchestrator_pageBranch_enqueuesJobWithItemIdsAndConsumeRunsOpenApiPath() {
		long companyId = 33L;
		long itemId = 701L;

		Items row = new Items();
		row.setItemId(itemId);
		row.setItemBn("BN-701");

		ItemsListQueryRepository repo = mock(ItemsListQueryRepository.class);
		when(repo.countByParams(any())).thenReturn(1L);
		when(repo.selectPageByParamsItemIdDesc(any(), eq(0), eq(100))).thenReturn(List.of(row));

		String settingJson =
				"{\"is_open\":true,\"access_token\":\"tok\",\"shop_id\":\"99\"}";
		StringRedisTemplate redis = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		ValueOperations<String, String> vo = mock(ValueOperations.class);
		when(redis.opsForValue()).thenReturn(vo);
		when(vo.get(anyString()))
				.thenAnswer(
						inv -> {
							String k = inv.getArgument(0);
							return k != null && k.startsWith("JushuitanSetting:") ? settingJson : null;
						});

		JushuitanItemStoreQueryStructService structService = mock(JushuitanItemStoreQueryStructService.class);
		when(structService.buildItemStoreQueryPayload(eq(companyId), any()))
				.thenReturn(
						Map.of(
								"wms_co_id",
								0,
								"page_index",
								1,
								"page_size",
								20,
								"sku_ids",
								"BN-701"));

		JushuitanOpenApiClient openApi = mock(JushuitanOpenApiClient.class);
		when(openApi.call(anyLong(), anyString(), any(), anyString()))
				.thenReturn(Map.of("code", "0", "inventorys", List.of(Map.of("qty", 2))));

		JushuitanInventoryPersistService persist = mock(JushuitanInventoryPersistService.class);
		ObjectMapper om = new ObjectMapper();
		JushuitanInventoryQueryBatchHandlerImpl batchImpl =
				new JushuitanInventoryQueryBatchHandlerImpl(redis, om, openApi, structService, persist);

		InventoryQueryFromJushuitanJobHandler handler = new InventoryQueryFromJushuitanJobHandler(batchImpl);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(GoodsBundleDispatchJobNames.INVENTORY_QUERY_FROM_JUSHUITAN, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		InventoryQueryFromJushuitanJobDispatchPublisherImpl publisherImpl =
				new InventoryQueryFromJushuitanJobDispatchPublisherImpl(facade);

		JushuitanQueryInventoryOrchestratorService orchestrator =
				new JushuitanQueryInventoryOrchestratorService(repo, publisherImpl);

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("item_id", itemId);

		orchestrator.run(companyId, merged, null);

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(GoodsBundleDispatchJobNames.INVENTORY_QUERY_FROM_JUSHUITAN, msg.messageName());
		Map<String, Object> got = msg.payload();
		assertEquals(companyId, asLong(got.get("company_id")));
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

		verify(openApi).call(eq(companyId), eq("item_store_query"), any(), eq("tok"));
		verify(persist, times(1)).persistInventoriesFromJushuitan(eq(companyId), any());
	}

	private static long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}
}
