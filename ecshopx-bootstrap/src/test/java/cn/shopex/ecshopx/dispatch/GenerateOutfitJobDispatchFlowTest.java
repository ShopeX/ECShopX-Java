package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.ShopexAiBundleDispatchJobNames;
import cn.shopex.ecshopx.config.GenerateOutfitJobDispatchPublisherImpl;
import cn.shopex.ecshopx.shopexai.api.admin.v1.dto.GenerateOutfitRequest;
import cn.shopex.ecshopx.shopexai.api.admin.v1.dto.OutfitGenerateAcceptedResponse;
import cn.shopex.ecshopx.shopexai.dispatch.GenerateOutfitJobDispatchPublisher;
import cn.shopex.ecshopx.shopexai.dispatch.GenerateOutfitJobHandler;
import cn.shopex.ecshopx.shopexai.domain.MemberOutfit;
import cn.shopex.ecshopx.shopexai.domain.MemberOutfitLog;
import cn.shopex.ecshopx.shopexai.mapper.MemberOutfitLogMapper;
import cn.shopex.ecshopx.shopexai.mapper.MemberOutfitMapper;
import cn.shopex.ecshopx.shopexai.service.OutfitAnyoneGenerationService;
import cn.shopex.ecshopx.shopexai.service.OutfitAnyoneGenerateOrchestratorService;
import cn.shopex.ecshopx.shopexai.service.OutfitGenerationPendingCacheValue;
import cn.shopex.ecshopx.shopexai.service.OutfitGenerationResultCacheService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class GenerateOutfitJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesOutfitGenerationService() {
		String cacheKey = "req-uuid-1";
		long companyId = 9L;
		long operatorId = 8L;
		long distributorId = 7L;

		OutfitAnyoneGenerationService generationService = mock(OutfitAnyoneGenerationService.class);
		when(generationService.generateOutfit(anyString(), anyString(), anyString()))
				.thenReturn(new LinkedHashMap<>(Map.of("result_url", "http://img/out.png")));

		OutfitGenerationResultCacheService cacheService = mock(OutfitGenerationResultCacheService.class);

		GenerateOutfitJobHandler handler = new GenerateOutfitJobHandler(generationService, cacheService);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(ShopexAiBundleDispatchJobNames.GENERATE_OUTFIT_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("person_image_url", "http://person");
		payload.put("top_garment_url", "http://top");
		payload.put("bottom_garment_url", "http://bottom");
		payload.put("cache_key", cacheKey);
		payload.put("cache_ttl", 3600);
		payload.put("company_id", companyId);
		payload.put("operator_id", operatorId);
		payload.put("distributor_id", distributorId);

		facade.dispatchJob(
				ShopexAiBundleDispatchJobNames.GENERATE_OUTFIT_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						new RetryPolicy(2, Duration.ofSeconds(3))));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals("slow", msg.queue());
		assertNull(msg.delay());
		assertEquals(2, msg.retryPolicy().maxAttempts());
		assertEquals(Duration.ofSeconds(3), msg.retryPolicy().nextDelay());
		Map<String, Object> got = msg.payload();
		assertEquals("http://person", got.get("person_image_url"));
		assertEquals("http://top", got.get("top_garment_url"));
		assertEquals("http://bottom", got.get("bottom_garment_url"));
		assertEquals(cacheKey, got.get("cache_key"));
		assertEquals(3600, asInt(got.get("cache_ttl")));
		assertEquals(companyId, asLong(got.get("company_id")));
		assertEquals(operatorId, asLong(got.get("operator_id")));
		assertEquals(distributorId, asLong(got.get("distributor_id")));
		assertEquals(8, got.size());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(generationService)
				.generateOutfit(eq("http://person"), eq("http://top"), eq("http://bottom"));
		verify(cacheService)
				.saveResult(
						eq(cacheKey),
						argThat(
								m ->
										Boolean.TRUE.equals(m.get("job_completed"))
												&& m.get("completed_at") != null
												&& "http://img/out.png".equals(m.get("result_url"))),
						eq(3600));
	}

	@Test
	void dispatchJob_publishPayloadMatchesGenerateOutfitEnvelope() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		GenerateOutfitJobDispatchPublisherImpl publisher =
				new GenerateOutfitJobDispatchPublisherImpl(dispatchFacade);

		publisher.enqueueGenerateOutfit(
				"http://p",
				"http://t",
				"http://b",
				"ck",
				3600,
				11L,
				12L,
				13L);

		verify(dispatchFacade)
				.dispatchJob(
						eq(ShopexAiBundleDispatchJobNames.GENERATE_OUTFIT_JOB),
						argThat(
								map ->
										"http://p".equals(map.get("person_image_url"))
												&& "http://t".equals(map.get("top_garment_url"))
												&& "http://b".equals(map.get("bottom_garment_url"))
												&& "ck".equals(map.get("cache_key"))
												&& asInt(map.get("cache_ttl")) == 3600
												&& asLong(map.get("company_id")) == 11L
												&& asLong(map.get("operator_id")) == 12L
												&& asLong(map.get("distributor_id")) == 13L
												&& map.size() == 8),
						argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.ASYNC
												&& opts.driverOverride() == DispatchDriverType.REDIS
												&& "slow".equals(opts.queue())
												&& opts.delay() == null
												&& opts.retryPolicy().maxAttempts() == 2
												&& Duration.ofSeconds(3).equals(opts.retryPolicy().nextDelay())));
	}

	@Test
	void jobHandler_parsesPayloadAndDelegatesToGenerationService() {
		OutfitAnyoneGenerationService generationService = mock(OutfitAnyoneGenerationService.class);
		OutfitGenerationResultCacheService cacheService = mock(OutfitGenerationResultCacheService.class);
		GenerateOutfitJobHandler handler = new GenerateOutfitJobHandler(generationService, cacheService);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("person_image_url", "http://a");
		payload.put("top_garment_url", "http://b");
		payload.put("bottom_garment_url", "");
		payload.put("cache_key", "k");
		payload.put("cache_ttl", 3600);
		payload.put("company_id", 5L);
		payload.put("operator_id", 6L);
		payload.put("distributor_id", 7L);

		when(generationService.generateOutfit(anyString(), anyString(), anyString()))
				.thenReturn(new LinkedHashMap<>());

		handler.handle(payload);

		verify(generationService).generateOutfit("http://a", "http://b", "");
	}

	@Test
	void orchestrator_writesPendingRedis_before_enqueueGenerateOutfit() {
		MemberOutfitLogMapper logMapper = mock(MemberOutfitLogMapper.class);
		MemberOutfitMapper outfitMapper = mock(MemberOutfitMapper.class);
		OutfitGenerationResultCacheService cache = mock(OutfitGenerationResultCacheService.class);
		GenerateOutfitJobDispatchPublisher publisher = mock(GenerateOutfitJobDispatchPublisher.class);
		OutfitAnyoneGenerationService generationService = mock(OutfitAnyoneGenerationService.class);

		when(logMapper.insert(any(MemberOutfitLog.class)))
				.thenAnswer(
						inv -> {
							MemberOutfitLog row = inv.getArgument(0);
							row.setId(500L);
							return 1;
						});

		MemberOutfitLog loaded = new MemberOutfitLog();
		loaded.setId(500L);
		loaded.setRequestId("rid-orchestrator-test");
		loaded.setModelId(88L);
		loaded.setTopGarmentUrl("http://top-x");
		loaded.setBottomGarmentUrl(null);

		when(logMapper.selectById(500L)).thenReturn(loaded);

		MemberOutfit model = new MemberOutfit();
		model.setId(88L);
		model.setModelImage("http://model-face");
		when(outfitMapper.selectById(88L)).thenReturn(model);

		OutfitAnyoneGenerateOrchestratorService orchestrator =
				new OutfitAnyoneGenerateOrchestratorService(
						logMapper, outfitMapper, cache, publisher, generationService);

		GenerateOutfitRequest req = new GenerateOutfitRequest();
		req.setMemberId(1);
		req.setItemId(2);
		req.setModelId(88L);
		req.setTopGarmentUrl("http://top-x");
		req.setBottomGarmentUrl(null);

		OutfitGenerateAcceptedResponse resp =
				orchestrator.generateOutfitQueued(req, 3L, 4L, 5L);
		assertEquals("rid-orchestrator-test", resp.getRequestId());

		InOrder order = inOrder(cache, publisher);
		order.verify(cache)
				.writePendingStatus(
						eq("rid-orchestrator-test"),
						eq(3600),
						argThat(
								p -> {
									if (p == null) {
										return false;
									}
									if (!"pending".equals(p.getStatus())
											|| p.getCreatedAt() == null
											|| p.isJobCompleted()) {
										return false;
									}
									try {
										com.fasterxml.jackson.databind.ObjectMapper om =
												new com.fasterxml.jackson.databind.ObjectMapper();
										String json = om.writeValueAsString(p);
										@SuppressWarnings("unchecked")
										Map<String, Object> keys = om.readValue(json, Map.class);
										return keys.keySet()
														.equals(Set.of("status", "created_at", "job_completed"))
												&& !Boolean.TRUE.equals(keys.get("job_completed"));
									} catch (Exception e) {
										return false;
									}
								}));
		order.verify(publisher)
				.enqueueGenerateOutfit(
						eq("http://model-face"),
						eq("http://top-x"),
						eq(""),
						eq("rid-orchestrator-test"),
						eq(3600),
						eq(3L),
						eq(4L),
						eq(5L));

		verifyNoMoreInteractions(cache, publisher);
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
