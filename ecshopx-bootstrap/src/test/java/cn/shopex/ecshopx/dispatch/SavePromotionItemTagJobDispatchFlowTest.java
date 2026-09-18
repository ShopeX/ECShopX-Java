package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import cn.shopex.ecshopx.common.dispatch.PromotionsDispatchJobNames;
import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.promotions.dispatch.SavePromotionItemTagJobHandler;
import cn.shopex.ecshopx.promotions.mapper.PromotionsItemsTagMapper;
import cn.shopex.ecshopx.promotions.service.promotionitemtag.PromotionItemTagSyncPort;
import cn.shopex.ecshopx.promotions.service.promotionitemtag.PromotionItemTagSyncService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class SavePromotionItemTagJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andHandlerDelegatesToPromotionItemTagSync() {
		PromotionItemTagSyncPort port = mock(PromotionItemTagSyncPort.class);
		SavePromotionItemTagJobHandler handler = new SavePromotionItemTagJobHandler(port);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(PromotionsDispatchJobNames.SAVE_PROMOTION_ITEM_TAG, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);
		payload.put("promotion_id", 88L);
		payload.put("tag_type", "full_minus");
		payload.put("start_time", 100);
		payload.put("end_time", 200);
		payload.put("item_type", "normal");
		payload.put("item_ids", List.of(10L, 20L));
		payload.put("activity_price_by_item_id", Map.of("10", new BigDecimal("1.5")));

		facade.dispatchJob(
				PromotionsDispatchJobNames.SAVE_PROMOTION_ITEM_TAG,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC, DispatchDriverType.REDIS, "slow", null, RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("slow", msg.queue());
		assertNull(msg.delay());
		assertNull(msg.listenerName());
		assertEquals(PromotionsDispatchJobNames.SAVE_PROMOTION_ITEM_TAG, msg.messageName());
		assertEquals(1L, msg.payload().get("company_id"));
		assertEquals(88L, msg.payload().get("promotion_id"));
		assertEquals("full_minus", msg.payload().get("tag_type"));
		assertEquals(100, ((Number) msg.payload().get("start_time")).intValue());
		assertEquals(200, ((Number) msg.payload().get("end_time")).intValue());
		assertEquals("normal", msg.payload().get("item_type"));
		assertInstanceOf(List.class, msg.payload().get("item_ids"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(port)
				.applySavePromotionItemTagJob(
						argThat(
								m ->
										m != null
												&& Long.valueOf(1L).equals(toLong(m.get("company_id")))
												&& Long.valueOf(88L).equals(toLong(m.get("promotion_id")))
												&& "full_minus".equals(String.valueOf(m.get("tag_type")))
												&& m.get("item_ids") instanceof List<?> list
												&& list.size() == 2));
		verifyNoMoreInteractions(port);
	}

	@Test
	void dispatchJob_singleGroupPayload_consumesAndDelegatesToPromotionItemTagSync() {
		PromotionItemTagSyncPort port = mock(PromotionItemTagSyncPort.class);
		SavePromotionItemTagJobHandler handler = new SavePromotionItemTagJobHandler(port);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(PromotionsDispatchJobNames.SAVE_PROMOTION_ITEM_TAG, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 7L);
		payload.put("promotion_id", 303L);
		payload.put("tag_type", "single_group");
		payload.put("start_time", 1700);
		payload.put("end_time", 3600);
		payload.put("item_type", "services");
		payload.put("item_ids", List.of(42L));
		payload.put("activity_price_by_item_id", Map.of("42", new BigDecimal("15.50")));

		facade.dispatchJob(
				PromotionsDispatchJobNames.SAVE_PROMOTION_ITEM_TAG,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC, DispatchDriverType.REDIS, "slow", null, RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("slow", msg.queue());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(port)
				.applySavePromotionItemTagJob(
						argThat(
								m ->
										m != null
												&& Long.valueOf(7L).equals(toLong(m.get("company_id")))
												&& Long.valueOf(303L).equals(toLong(m.get("promotion_id")))
												&& "single_group".equals(String.valueOf(m.get("tag_type")))
												&& Integer.valueOf(1700).equals(toInt(m.get("start_time")))
												&& Integer.valueOf(3600).equals(toInt(m.get("end_time")))
												&& "services".equals(String.valueOf(m.get("item_type")))
												&& m.get("item_ids") instanceof List<?> list
												&& list.equals(List.of(42L))));
		verifyNoMoreInteractions(port);
	}

	@Test
	void dispatchJob_limitedTimeSaleSeckillPayload_consumesAndDelegatesToPromotionItemTagSync() {
		PromotionItemTagSyncPort port = mock(PromotionItemTagSyncPort.class);
		SavePromotionItemTagJobHandler handler = new SavePromotionItemTagJobHandler(port);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(PromotionsDispatchJobNames.SAVE_PROMOTION_ITEM_TAG, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 3L);
		payload.put("promotion_id", 404L);
		payload.put("tag_type", "limited_time_sale");
		payload.put("start_time", 300);
		payload.put("end_time", 2400);
		payload.put("item_type", "normal");
		payload.put("item_ids", List.of(31L, 47L));
		payload.put(
				"activity_price_by_item_id",
				Map.of("31", new BigDecimal("4.25"), "47", new BigDecimal("6.00")));

		facade.dispatchJob(
				PromotionsDispatchJobNames.SAVE_PROMOTION_ITEM_TAG,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC, DispatchDriverType.REDIS, "slow", null, RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("slow", msg.queue());
		assertEquals("limited_time_sale", msg.payload().get("tag_type"));
		assertInstanceOf(List.class, msg.payload().get("item_ids"));
		List<?> ids = (List<?>) msg.payload().get("item_ids");
		assertEquals(2, ids.size());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(port)
				.applySavePromotionItemTagJob(
						argThat(
								m ->
										m != null
												&& Long.valueOf(3L).equals(toLong(m.get("company_id")))
												&& Long.valueOf(404L).equals(toLong(m.get("promotion_id")))
												&& "limited_time_sale".equals(String.valueOf(m.get("tag_type")))
												&& Integer.valueOf(300).equals(toInt(m.get("start_time")))
												&& Integer.valueOf(2400).equals(toInt(m.get("end_time")))
												&& "normal".equals(String.valueOf(m.get("item_type")))
												&& m.get("item_ids") instanceof List<?> list
												&& list.size() == 2
												&& list.equals(List.of(31L, 47L))
												&& m.get("activity_price_by_item_id") instanceof Map<?, ?> priceMap
												&& priceMap.size() == 2
												&& new BigDecimal("4.25")
														.equals(new BigDecimal(String.valueOf(priceMap.get("31"))))
												&& new BigDecimal("6.00")
														.equals(new BigDecimal(String.valueOf(priceMap.get("47"))))));
		verifyNoMoreInteractions(port);
	}

	@Test
	void handler_swallowsInnerException_likePhp_soConsumeAcks() {
		PromotionsItemsTagMapper mapper = mock(PromotionsItemsTagMapper.class);
		Mockito.when(mapper.delete(ArgumentMatchers.any())).thenThrow(new RuntimeException("db down"));
		MarketingActivityCatalogAccess catalog = mock(MarketingActivityCatalogAccess.class);
		PromotionItemTagSyncService service = new PromotionItemTagSyncService(mapper, catalog);
		SavePromotionItemTagJobHandler handler = new SavePromotionItemTagJobHandler(service);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(PromotionsDispatchJobNames.SAVE_PROMOTION_ITEM_TAG, handler);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 9L);
		payload.put("promotion_id", 1L);
		payload.put("tag_type", "full_minus");
		payload.put("start_time", 1);
		payload.put("end_time", 2);
		payload.put("item_type", "normal");
		payload.put("item_ids", List.of());
		payload.put("activity_price_by_item_id", Map.of());

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						PromotionsDispatchJobNames.SAVE_PROMOTION_ITEM_TAG,
						payload,
						"slow",
						null,
						RetryPolicy.platformDefault(),
						Instant.now(),
						"trace-save-promo-tag",
						null);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		Mockito.verify(mapper).delete(ArgumentMatchers.any());
	}

	private static Long toLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return null;
	}

	private static Integer toInt(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		return null;
	}
}
