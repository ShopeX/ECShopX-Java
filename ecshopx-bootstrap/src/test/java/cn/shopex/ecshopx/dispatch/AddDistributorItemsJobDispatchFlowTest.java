package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.AddDistributorItemsJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.DistributionDispatchJobNames;
import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.common.port.shuyun.ShuyunOpenPlatformProductSyncPort;
import cn.shopex.ecshopx.config.AddDistributorItemsJobDispatchPublisherImpl;
import cn.shopex.ecshopx.distribution.repository.DistributorItemsRepository;
import cn.shopex.ecshopx.goods.dispatch.AddDistributorItemsJobHandler;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.service.distributor.DistributorItemsPagedAddRunner;
import cn.shopex.ecshopx.goods.service.distributor.DistributorItemsSkuPageQueryService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AddDistributorItemsJobDispatchFlowTest {

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesHandlerWithMocks() {
		DispatchHandler handler = mock(DispatchHandler.class);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(DistributionDispatchJobNames.ADD_DISTRIBUTOR_ITEMS_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 11L);
		payload.put("distributor_id", 22L);
		payload.put("item_ids", List.of(101L, 102L));
		payload.put("is_can_sale", true);
		payload.put("page", 1);
		payload.put("pageSize", 100);

		facade.dispatchJob(
				DistributionDispatchJobNames.ADD_DISTRIBUTOR_ITEMS_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("slow", msg.queue());
		assertEquals(DistributionDispatchJobNames.ADD_DISTRIBUTOR_ITEMS_JOB, msg.messageName());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
		verify(handler, times(1)).handle(cap.capture());
		Map<String, Object> passed = cap.getValue();
		assertEquals(11L, ((Number) passed.get("company_id")).longValue());
		assertEquals(22L, ((Number) passed.get("distributor_id")).longValue());
		assertEquals(List.of(101L, 102L), passed.get("item_ids"));
		assertEquals(true, passed.get("is_can_sale"));
		assertEquals(1, ((Number) passed.get("page")).intValue());
		assertEquals(100, ((Number) passed.get("pageSize")).intValue());
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void dispatchJob_async_payloadMatchesAddDistributorItemsEnvelope() {
		DispatchFacade facade = mock(DispatchFacade.class);

		AddDistributorItemsJobDispatchPublisher publisher = new AddDistributorItemsJobDispatchPublisherImpl(facade);

		publisher.enqueueFirstPage(9L, 8L, List.of(201L, 202L), false, 50);

		ArgumentCaptor<Map<String, Object>> payloadCap = ArgumentCaptor.forClass(Map.class);
		ArgumentCaptor<DispatchOptions> optsCap = ArgumentCaptor.forClass(DispatchOptions.class);
		verify(facade)
				.dispatchJob(eq(DistributionDispatchJobNames.ADD_DISTRIBUTOR_ITEMS_JOB), payloadCap.capture(), optsCap.capture());

		DispatchOptions opts = optsCap.getValue();
		assertEquals(DispatchMode.ASYNC, opts.mode());
		assertEquals(DispatchDriverType.REDIS, opts.driverOverride());
		assertEquals("slow", opts.queue());
		assertEquals(RetryPolicy.platformDefault(), opts.retryPolicy());

		Map<String, Object> p = payloadCap.getValue();
		assertEquals(9L, ((Number) p.get("company_id")).longValue());
		assertEquals(8L, ((Number) p.get("distributor_id")).longValue());
		assertEquals(List.of(201L, 202L), p.get("item_ids"));
		assertEquals(false, p.get("is_can_sale"));
		assertEquals(1, ((Number) p.get("page")).intValue());
		assertEquals(50, ((Number) p.get("pageSize")).intValue());

		reset(facade);
		publisher.enqueuePage(1L, 2L, null, true, 3, 80);
		payloadCap = ArgumentCaptor.forClass(Map.class);
		optsCap = ArgumentCaptor.forClass(DispatchOptions.class);
		verify(facade)
				.dispatchJob(eq(DistributionDispatchJobNames.ADD_DISTRIBUTOR_ITEMS_JOB), payloadCap.capture(), optsCap.capture());
		Map<String, Object> p2 = payloadCap.getValue();
		assertEquals("_all", p2.get("item_ids"));
		assertEquals(3, ((Number) p2.get("page")).intValue());
		assertEquals(80, ((Number) p2.get("pageSize")).intValue());
		assertEquals(true, p2.get("is_can_sale"));
	}

	@Test
	void dispatchJob_chain_enqueuesSecondJobWhenMorePages() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		AddDistributorItemsJobDispatchPublisher publisher = new AddDistributorItemsJobDispatchPublisherImpl(facade);
		DistributorItemsSkuPageQueryService sku = mock(DistributorItemsSkuPageQueryService.class);
		org.mockito.Mockito.when(sku.countSkus(org.mockito.Mockito.anyLong(), org.mockito.Mockito.any()))
				.thenReturn(150L);
		Items row = new Items();
		row.setItemId(1L);
		row.setDefaultItemId(1L);
		row.setGoodsId(1L);
		row.setPrice(100);
		org.mockito.Mockito.when(
						sku.selectPage(
								org.mockito.Mockito.eq(1L),
								org.mockito.Mockito.isNull(),
								org.mockito.Mockito.anyInt(),
								org.mockito.Mockito.eq(100)))
				.thenReturn(List.of(row));

		DistributorItemsRepository repo = mock(DistributorItemsRepository.class);
		org.mockito.Mockito.when(repo.listByDistributorAndItemIds(org.mockito.Mockito.anyLong(), org.mockito.Mockito.anyLong(), org.mockito.Mockito.anyList()))
				.thenReturn(List.of());

		DistributorItemsPagedAddRunner runner =
				new DistributorItemsPagedAddRunner(
						publisher, sku, repo, mock(ShuyunOpenPlatformProductSyncPort.class));
		AddDistributorItemsJobHandler handler = new AddDistributorItemsJobHandler(runner);
		registry.registerJob(DistributionDispatchJobNames.ADD_DISTRIBUTOR_ITEMS_JOB, handler);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);
		payload.put("distributor_id", 2L);
		payload.put("item_ids", "_all");
		payload.put("is_can_sale", false);
		payload.put("page", 1);
		payload.put("pageSize", 100);

		facade.dispatchJob(
				DistributionDispatchJobNames.ADD_DISTRIBUTOR_ITEMS_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage first = captured.get(0);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(first, 1);

		assertEquals(2, captured.size());
		DispatchMessage second = captured.get(1);
		Map<String, Object> firstPayload = first.payload();
		Map<String, Object> secondPayload = second.payload();
		assertEquals(2, ((Number) secondPayload.get("page")).intValue());
		assertEquals(DistributionDispatchJobNames.ADD_DISTRIBUTOR_ITEMS_JOB, second.messageName());
		assertEquals("slow", second.queue());
		assertEquals(((Number) firstPayload.get("company_id")).longValue(), ((Number) secondPayload.get("company_id")).longValue());
		assertEquals(((Number) firstPayload.get("distributor_id")).longValue(), ((Number) secondPayload.get("distributor_id")).longValue());
		assertEquals(firstPayload.get("item_ids"), secondPayload.get("item_ids"));
		assertEquals(firstPayload.get("is_can_sale"), secondPayload.get("is_can_sale"));
		assertEquals(((Number) firstPayload.get("pageSize")).intValue(), ((Number) secondPayload.get("pageSize")).intValue());
	}
}
