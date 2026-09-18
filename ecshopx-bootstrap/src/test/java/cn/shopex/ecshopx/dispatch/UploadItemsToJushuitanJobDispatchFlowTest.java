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
import cn.shopex.ecshopx.config.UploadItemsToJushuitanJobDispatchPublisherImpl;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.goods.dispatch.UploadItemsToJushuitanJobDispatchPublisher;
import cn.shopex.ecshopx.goods.dispatch.UploadItemsToJushuitanJobHandler;
import cn.shopex.ecshopx.goods.integration.jushuitan.JushuitanUploadItemsBatchHandler;
import cn.shopex.ecshopx.goods.service.jushuitan.JushuitanItemStructAssembler;
import cn.shopex.ecshopx.goods.service.jushuitan.JushuitanItemStructAssembler.JushuitanItemStructBundle;
import cn.shopex.ecshopx.goods.service.jushuitan.JushuitanUploadItemsOrchestratorService;
import cn.shopex.ecshopx.goods.service.jushuitan.JushuitanUploadItemsPageQueryService;
import cn.shopex.ecshopx.systemlink.jushuitan.JushuitanOpenApiClient;
import cn.shopex.ecshopx.systemlink.jushuitan.JushuitanUploadItemsBatchHandlerImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class UploadItemsToJushuitanJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesJushuitanOpenApi() {
		long companyId = 42L;
		long itemId = 9001L;
		List<Long> itemIds = List.of(itemId);

		String settingJson =
				"{\"is_open\":true,\"access_token\":\"tok\",\"shop_id\":\"99\"}";
		StringRedisTemplate redis = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		ValueOperations<String, String> vo = mock(ValueOperations.class);
		when(redis.opsForValue()).thenReturn(vo);
		when(vo.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
		when(vo.get(anyString()))
				.thenAnswer(
						inv -> {
							String k = inv.getArgument(0);
							return k != null && k.startsWith("JushuitanSetting:") ? settingJson : null;
						});

		List<Map<String, Object>> sku = List.of(Map.of("i_id", itemId));
		List<List<Map<String, Object>>> shopChunks = List.of(List.of(Map.of("sku_id", itemId)));
		JushuitanItemStructBundle bundle = new JushuitanItemStructBundle(sku, shopChunks);
		JushuitanItemStructAssembler assembler = mock(JushuitanItemStructAssembler.class);
		when(assembler.build(eq(companyId), eq(itemId), eq(0L), eq("99"), eq("normal")))
				.thenReturn(bundle);

		JushuitanOpenApiClient openApi = mock(JushuitanOpenApiClient.class);
		when(openApi.call(anyLong(), anyString(), any(), anyString())).thenReturn(Map.of("ok", true));

		DistributorListQueryService distributors = mock(DistributorListQueryService.class);
		ObjectMapper om = new ObjectMapper();
		JushuitanUploadItemsBatchHandlerImpl batchImpl =
				new JushuitanUploadItemsBatchHandlerImpl(redis, om, assembler, openApi, distributors);

		UploadItemsToJushuitanJobHandler handler = new UploadItemsToJushuitanJobHandler(batchImpl);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(GoodsBundleDispatchJobNames.UPLOAD_ITEMS_TO_JUSHUITAN, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("item_ids", itemIds);
		payload.put("distributor_id", 0L);
		payload.put("item_type", "normal");

		facade.dispatchJob(
				GoodsBundleDispatchJobNames.UPLOAD_ITEMS_TO_JUSHUITAN,
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
		assertEquals(GoodsBundleDispatchJobNames.UPLOAD_ITEMS_TO_JUSHUITAN, msg.messageName());
		assertNull(msg.listenerName());
		Map<String, Object> got = msg.payload();
		assertEquals(companyId, asLong(got.get("company_id")));
		assertEquals(0L, asLong(got.get("distributor_id")));
		assertEquals("normal", String.valueOf(got.get("item_type")));
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

		verify(openApi).call(eq(companyId), eq("item_add"), any(), eq("tok"));
		verify(openApi).call(eq(companyId), eq("shop_item_add"), any(), eq("tok"));
	}

	@Test
	void dispatchJob_publishPayloadMatchesUploadItemsToJushuitanEnvelope() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		UploadItemsToJushuitanJobDispatchPublisherImpl publisher =
				new UploadItemsToJushuitanJobDispatchPublisherImpl(dispatchFacade);

		long companyId = 7L;
		List<Long> itemIds = List.of(100L, 101L);
		long distributorId = 3L;
		publisher.enqueueUploadItemsToJushuitan(companyId, itemIds, distributorId, "special");

		verify(dispatchFacade)
				.dispatchJob(
						eq(GoodsBundleDispatchJobNames.UPLOAD_ITEMS_TO_JUSHUITAN),
						argThat(
								map -> {
									if (companyId != asLong(map.get("company_id"))) {
										return false;
									}
									if (distributorId != asLong(map.get("distributor_id"))) {
										return false;
									}
									if (!"special".equals(String.valueOf(map.get("item_type")))) {
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
									return map.size() == 4;
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
	void jobHandler_forwardsParsedPayloadToJushuitanBatchHandler() {
		JushuitanUploadItemsBatchHandler batch = mock(JushuitanUploadItemsBatchHandler.class);
		UploadItemsToJushuitanJobHandler handler = new UploadItemsToJushuitanJobHandler(batch);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 11L);
		payload.put("distributor_id", 2L);
		payload.put("item_ids", List.of(201, 202L));
		payload.put("item_type", "pointsmall");

		handler.handle(payload);

		verify(batch)
				.handleBatch(
						eq(11L), argThat(list -> list.equals(List.of(201L, 202L))), eq(2L), eq("pointsmall"));
	}

	@Test
	void orchestrator_pageBranch_enqueuesJobWithItemIdsAndItemType() {
		long companyId = 33L;
		String operatorType = "shop";
		long distributorId = 0L;

		DistributorListQueryService distributors = mock(DistributorListQueryService.class);

		Map<String, Object> row1 = new LinkedHashMap<>();
		row1.put("item_id", 701L);
		Map<String, Object> pageResult = new LinkedHashMap<>();
		pageResult.put("total_count", 1L);
		pageResult.put("list", List.of(row1));

		JushuitanUploadItemsPageQueryService pageQuery = mock(JushuitanUploadItemsPageQueryService.class);
		when(pageQuery.resolveProductModel(companyId)).thenReturn("standard");
		when(pageQuery.fetchPage(
						eq(companyId),
						eq(operatorType),
						eq(distributorId),
						eq("standard"),
						eq("normal"),
						any(),
						eq(1),
						eq(100),
						any()))
				.thenReturn(pageResult);

		UploadItemsToJushuitanJobDispatchPublisher publisher = mock(UploadItemsToJushuitanJobDispatchPublisher.class);

		StringRedisTemplate redis = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		ValueOperations<String, String> vo = mock(ValueOperations.class);
		when(redis.opsForValue()).thenReturn(vo);

		JushuitanUploadItemsOrchestratorService orchestrator =
				new JushuitanUploadItemsOrchestratorService(distributors, pageQuery, publisher, redis);

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("item_id", 701L);

		orchestrator.run(null, companyId, operatorType, distributorId, merged, null);

		verify(publisher, times(1)).enqueueUploadItemsToJushuitan(companyId, List.of(701L), 0L, "normal");
		verify(pageQuery, times(1))
				.fetchPage(
						eq(companyId),
						eq(operatorType),
						eq(distributorId),
						eq("standard"),
						eq("normal"),
						any(),
						eq(1),
						eq(100),
						any());
	}

	@Test
	void orchestrator_standardListBranch_enqueuesOnSlow_andConsumeInvokesJushuitanOpenApi() {
		long companyId = 56L;
		long itemId = 9003L;
		String operatorType = "shop";
		long distributorId = 0L;

		String settingJson =
				"{\"is_open\":true,\"access_token\":\"tok\",\"shop_id\":\"99\"}";
		StringRedisTemplate redis = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		ValueOperations<String, String> vo = mock(ValueOperations.class);
		when(redis.opsForValue()).thenReturn(vo);
		when(vo.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
		when(vo.get(anyString()))
				.thenAnswer(
						inv -> {
							String k = inv.getArgument(0);
							if (k != null && k.startsWith("JushuitanSetting:")) {
								return settingJson;
							}
							return null;
						});

		List<Map<String, Object>> sku = List.of(Map.of("i_id", itemId));
		List<List<Map<String, Object>>> shopChunks = List.of(List.of(Map.of("sku_id", itemId)));
		JushuitanItemStructBundle bundle = new JushuitanItemStructBundle(sku, shopChunks);
		JushuitanItemStructAssembler assembler = mock(JushuitanItemStructAssembler.class);
		when(assembler.build(eq(companyId), eq(itemId), eq(0L), eq("99"), eq("normal")))
				.thenReturn(bundle);

		JushuitanOpenApiClient openApi = mock(JushuitanOpenApiClient.class);
		when(openApi.call(anyLong(), anyString(), any(), anyString())).thenReturn(Map.of("ok", true));

		DistributorListQueryService distributors = mock(DistributorListQueryService.class);
		ObjectMapper om = new ObjectMapper();
		JushuitanUploadItemsBatchHandlerImpl batchImpl =
				new JushuitanUploadItemsBatchHandlerImpl(redis, om, assembler, openApi, distributors);

		UploadItemsToJushuitanJobHandler handler = new UploadItemsToJushuitanJobHandler(batchImpl);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(GoodsBundleDispatchJobNames.UPLOAD_ITEMS_TO_JUSHUITAN, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));
		UploadItemsToJushuitanJobDispatchPublisherImpl publisherImpl =
				new UploadItemsToJushuitanJobDispatchPublisherImpl(facade);

		Map<String, Object> row1 = new LinkedHashMap<>();
		row1.put("item_id", itemId);
		Map<String, Object> pageResult = new LinkedHashMap<>();
		pageResult.put("total_count", 1L);
		pageResult.put("list", List.of(row1));

		JushuitanUploadItemsPageQueryService pageQuery = mock(JushuitanUploadItemsPageQueryService.class);
		when(pageQuery.resolveProductModel(companyId)).thenReturn("standard");
		when(pageQuery.fetchPage(
						eq(companyId),
						eq(operatorType),
						eq(distributorId),
						eq("standard"),
						eq("normal"),
						any(),
						eq(1),
						eq(100),
						any()))
				.thenReturn(pageResult);

		JushuitanUploadItemsOrchestratorService orchestrator =
				new JushuitanUploadItemsOrchestratorService(distributors, pageQuery, publisherImpl, redis);

		orchestrator.run(null, companyId, operatorType, distributorId, new LinkedHashMap<>(), null);

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(GoodsBundleDispatchJobNames.UPLOAD_ITEMS_TO_JUSHUITAN, msg.messageName());
		Map<String, Object> got = msg.payload();
		assertEquals(companyId, asLong(got.get("company_id")));
		assertEquals(0L, asLong(got.get("distributor_id")));
		assertEquals("normal", String.valueOf(got.get("item_type")));
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

		verify(openApi).call(eq(companyId), eq("item_add"), any(), eq("tok"));
		verify(openApi).call(eq(companyId), eq("shop_item_add"), any(), eq("tok"));
	}

	@Test
	void orchestrator_pointsmallBranch_enqueuesOnSlow_andConsumeInvokesJushuitanOpenApi() {
		long companyId = 55L;
		long itemId = 9002L;
		String operatorType = "shop";
		long distributorId = 0L;

		String settingJson =
				"{\"is_open\":true,\"access_token\":\"tok\",\"shop_id\":\"99\"}";
		StringRedisTemplate redis = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		ValueOperations<String, String> vo = mock(ValueOperations.class);
		when(redis.opsForValue()).thenReturn(vo);
		when(vo.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
		when(vo.get(anyString()))
				.thenAnswer(
						inv -> {
							String k = inv.getArgument(0);
							if (k != null && k.startsWith("JushuitanSetting:")) {
								return settingJson;
							}
							return null;
						});

		List<Map<String, Object>> sku = List.of(Map.of("i_id", itemId));
		List<List<Map<String, Object>>> shopChunks = List.of(List.of(Map.of("sku_id", itemId)));
		JushuitanItemStructBundle bundle = new JushuitanItemStructBundle(sku, shopChunks);
		JushuitanItemStructAssembler assembler = mock(JushuitanItemStructAssembler.class);
		when(assembler.build(eq(companyId), eq(itemId), eq(0L), eq("99"), eq("pointsmall")))
				.thenReturn(bundle);

		JushuitanOpenApiClient openApi = mock(JushuitanOpenApiClient.class);
		when(openApi.call(anyLong(), anyString(), any(), anyString())).thenReturn(Map.of("ok", true));

		DistributorListQueryService distributors = mock(DistributorListQueryService.class);
		ObjectMapper om = new ObjectMapper();
		JushuitanUploadItemsBatchHandlerImpl batchImpl =
				new JushuitanUploadItemsBatchHandlerImpl(redis, om, assembler, openApi, distributors);

		UploadItemsToJushuitanJobHandler handler = new UploadItemsToJushuitanJobHandler(batchImpl);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(GoodsBundleDispatchJobNames.UPLOAD_ITEMS_TO_JUSHUITAN, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));
		UploadItemsToJushuitanJobDispatchPublisherImpl publisherImpl =
				new UploadItemsToJushuitanJobDispatchPublisherImpl(facade);

		Map<String, Object> row1 = new LinkedHashMap<>();
		row1.put("item_id", itemId);
		Map<String, Object> pageResult = new LinkedHashMap<>();
		pageResult.put("total_count", 1L);
		pageResult.put("list", List.of(row1));

		JushuitanUploadItemsPageQueryService pageQuery = mock(JushuitanUploadItemsPageQueryService.class);
		when(pageQuery.resolveProductModel(companyId)).thenReturn("standard");
		when(pageQuery.fetchPage(
						eq(companyId),
						eq(operatorType),
						eq(distributorId),
						eq("standard"),
						eq("pointsmall"),
						any(),
						eq(1),
						eq(100),
						any()))
				.thenReturn(pageResult);

		JushuitanUploadItemsOrchestratorService orchestrator =
				new JushuitanUploadItemsOrchestratorService(distributors, pageQuery, publisherImpl, redis);

		orchestrator.run(null, companyId, operatorType, distributorId, new LinkedHashMap<>(), "pointsmall");

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(GoodsBundleDispatchJobNames.UPLOAD_ITEMS_TO_JUSHUITAN, msg.messageName());
		Map<String, Object> got = msg.payload();
		assertEquals(companyId, asLong(got.get("company_id")));
		assertEquals(0L, asLong(got.get("distributor_id")));
		assertEquals("pointsmall", String.valueOf(got.get("item_type")));
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

		verify(openApi).call(eq(companyId), eq("item_add"), any(), eq("tok"));
		verify(openApi).call(eq(companyId), eq("shop_item_add"), any(), eq("tok"));
	}

	private static long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}
}
