package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.common.dispatch.GoodsBundleDispatchJobNames;
import cn.shopex.ecshopx.common.dispatch.GoodsDispatchEventNames;
import cn.shopex.ecshopx.common.port.goods.ItemBatchEditStatusMarketingSkuRowsPort;
import cn.shopex.ecshopx.config.ItemBatchEditStatusEventJobDispatchPublisherImpl;
import cn.shopex.ecshopx.config.ItemBatchEditStatusEventJobHandler;
import cn.shopex.ecshopx.goods.dispatch.ItemsApproveStatusSyncDispatchListener;
import cn.shopex.ecshopx.goods.service.items.ItemsApproveStatusSyncService;
import cn.shopex.ecshopx.thirdparty.dispatch.ItemBatchEditStatusPushMarketingCenterDispatchListener;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.ItemBatchEditStatusPushMarketingCenterProcessor;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.MarketingCenterOpenApiSignedFormClient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ItemBatchEditStatusEventJobDispatchFlowTest {

	private static final String LISTENER_MARKETING = "listener:thirdparty.item_batch_edit_status_push_marketing_center";
	private static final String LISTENER_GOODS_SYNC = "listener:goods.items_approve_status_sync";

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerRunsHandler_andFanOutEmitsTwoEventMessages() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new cn.shopex.ecshopx.dispatch.SyncDispatchDriver(registry),
						Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		DispatchListener stubMarketing = payload -> {};
		DispatchListener stubGoods = payload -> {};
		registry.registerJob(
				GoodsBundleDispatchJobNames.ITEM_BATCH_EDIT_STATUS_EVENT_JOB,
				new ItemBatchEditStatusEventJobHandler(facade));
		registry.registerEventListener(
				GoodsDispatchEventNames.EVENT_ITEM_BATCH_EDIT_STATUS,
				LISTENER_MARKETING,
				ListenerDispatchOptions.asyncDefaults(),
				stubMarketing);
		registry.registerEventListener(
				GoodsDispatchEventNames.EVENT_ITEM_BATCH_EDIT_STATUS,
				LISTENER_GOODS_SYNC,
				ListenerDispatchOptions.asyncDefaults(),
				stubGoods);

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 42L);
		payload.put("goods_id", 9001L);
		payload.put("approve_status", "onsale");

		facade.dispatchJob(
				GoodsBundleDispatchJobNames.ITEM_BATCH_EDIT_STATUS_EVENT_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage jobMsg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, jobMsg.messageType());
		assertEquals(DispatchMode.ASYNC, jobMsg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, jobMsg.driverType());
		assertEquals("slow", jobMsg.queue());
		assertNull(jobMsg.delay());
		assertEquals(GoodsBundleDispatchJobNames.ITEM_BATCH_EDIT_STATUS_EVENT_JOB, jobMsg.messageName());
		assertNull(jobMsg.listenerName());
		Map<String, Object> jobPayload = jobMsg.payload();
		assertEquals(42L, asLong(jobPayload.get("company_id")));
		assertEquals(9001L, asLong(jobPayload.get("goods_id")));
		assertEquals("onsale", jobPayload.get("approve_status"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(jobMsg, 1);

		assertEquals(3, captured.size());
		DispatchMessage ev1 = captured.get(1);
		DispatchMessage ev2 = captured.get(2);
		assertEquals(DispatchMessageType.EVENT, ev1.messageType());
		assertEquals(DispatchMessageType.EVENT, ev2.messageType());
		assertEquals(GoodsDispatchEventNames.EVENT_ITEM_BATCH_EDIT_STATUS, ev1.messageName());
		assertEquals(GoodsDispatchEventNames.EVENT_ITEM_BATCH_EDIT_STATUS, ev2.messageName());
		assertEquals(LISTENER_MARKETING, ev1.listenerName());
		assertEquals(LISTENER_GOODS_SYNC, ev2.listenerName());
		assertEquals("default", ev1.queue());
		assertEquals("default", ev2.queue());
		assertEquals(42L, asLong(ev1.payload().get("company_id")));
		assertEquals(9001L, asLong(ev1.payload().get("goods_id")));
		assertEquals("onsale", ev1.payload().get("approve_status"));
		assertEquals(42L, asLong(ev2.payload().get("company_id")));
		assertEquals(9001L, asLong(ev2.payload().get("goods_id")));
		assertEquals("onsale", ev2.payload().get("approve_status"));
	}

	@Test
	void dispatchJob_consumeMarketingEventListener_invokesBasicsItemProccessViaBusPath() {
		MarketingCenterOpenApiSignedFormClient marketingClient = mock(MarketingCenterOpenApiSignedFormClient.class);
		ItemBatchEditStatusMarketingSkuRowsPort skuRowsPort = mock(ItemBatchEditStatusMarketingSkuRowsPort.class);
		ItemBatchEditStatusPushMarketingCenterProcessor processor =
				new ItemBatchEditStatusPushMarketingCenterProcessor(marketingClient, skuRowsPort);
		DispatchListener marketingListener = new ItemBatchEditStatusPushMarketingCenterDispatchListener(processor);
		DispatchListener stubGoods = payload -> {};

		List<Map<String, Object>> portRows = new ArrayList<>();
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("item_bn", "SKU-1");
		row.put("approve_status", "onsale");
		portRows.add(row);
		when(skuRowsPort.listRows(42L, 9001L)).thenReturn(portRows);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new cn.shopex.ecshopx.dispatch.SyncDispatchDriver(registry),
						Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		registry.registerJob(
				GoodsBundleDispatchJobNames.ITEM_BATCH_EDIT_STATUS_EVENT_JOB,
				new ItemBatchEditStatusEventJobHandler(facade));
		registry.registerEventListener(
				GoodsDispatchEventNames.EVENT_ITEM_BATCH_EDIT_STATUS,
				LISTENER_MARKETING,
				ListenerDispatchOptions.asyncDefaults(),
				marketingListener);
		registry.registerEventListener(
				GoodsDispatchEventNames.EVENT_ITEM_BATCH_EDIT_STATUS,
				LISTENER_GOODS_SYNC,
				ListenerDispatchOptions.asyncDefaults(),
				stubGoods);

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 42L);
		payload.put("goods_id", 9001L);
		payload.put("approve_status", "onsale");

		facade.dispatchJob(
				GoodsBundleDispatchJobNames.ITEM_BATCH_EDIT_STATUS_EVENT_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());

		assertEquals(1, captured.size());
		runtime.consume(captured.get(0), 1);
		assertEquals(3, captured.size());

		DispatchMessage marketingChild =
				captured.stream()
						.filter(m -> m.messageType() == DispatchMessageType.EVENT && LISTENER_MARKETING.equals(m.listenerName()))
						.findFirst()
						.orElseThrow();
		runtime.consume(marketingChild, 1);

		verify(marketingClient)
				.basicsItemProccess(
						eq(42L),
						argThat(
								params ->
										params != null
												&& params.size() == 1
												&& params.get("0") instanceof Map<?, ?> m
												&& "SKU-1".equals(m.get("item_bn"))
												&& "onsale".equals(m.get("approve_status"))));

		DispatchMessage goodsChild =
				captured.stream()
						.filter(m -> m.messageType() == DispatchMessageType.EVENT && LISTENER_GOODS_SYNC.equals(m.listenerName()))
						.findFirst()
						.orElseThrow();
		runtime.consume(goodsChild, 1);
	}

	@Test
	void dispatchJob_consumeGoodsApproveStatusListener_invokesSyncServiceAndPreservesFanOutOrder() {
		ItemsApproveStatusSyncService itemsApproveStatusSyncService = mock(ItemsApproveStatusSyncService.class);
		DispatchListener goodsListener = new ItemsApproveStatusSyncDispatchListener(itemsApproveStatusSyncService);
		DispatchListener stubMarketing = payload -> {};

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new cn.shopex.ecshopx.dispatch.SyncDispatchDriver(registry),
						Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		registry.registerJob(
				GoodsBundleDispatchJobNames.ITEM_BATCH_EDIT_STATUS_EVENT_JOB,
				new ItemBatchEditStatusEventJobHandler(facade));
		registry.registerEventListener(
				GoodsDispatchEventNames.EVENT_ITEM_BATCH_EDIT_STATUS,
				LISTENER_MARKETING,
				ListenerDispatchOptions.asyncDefaults(),
				stubMarketing);
		registry.registerEventListener(
				GoodsDispatchEventNames.EVENT_ITEM_BATCH_EDIT_STATUS,
				LISTENER_GOODS_SYNC,
				ListenerDispatchOptions.asyncDefaults(),
				goodsListener);

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		long companyId = 42L;
		long goodsId = 9001L;
		String approveStatus = "onsale";
		payload.put("company_id", companyId);
		payload.put("goods_id", goodsId);
		payload.put("approve_status", approveStatus);

		facade.dispatchJob(
				GoodsBundleDispatchJobNames.ITEM_BATCH_EDIT_STATUS_EVENT_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());

		assertEquals(1, captured.size());
		runtime.consume(captured.get(0), 1);
		assertEquals(3, captured.size());

		DispatchMessage marketingChild =
				captured.stream()
						.filter(m -> m.messageType() == DispatchMessageType.EVENT && LISTENER_MARKETING.equals(m.listenerName()))
						.findFirst()
						.orElseThrow();
		runtime.consume(marketingChild, 1);

		DispatchMessage goodsChild =
				captured.stream()
						.filter(m -> m.messageType() == DispatchMessageType.EVENT && LISTENER_GOODS_SYNC.equals(m.listenerName()))
						.findFirst()
						.orElseThrow();
		runtime.consume(goodsChild, 1);

		verify(itemsApproveStatusSyncService)
				.syncDistributorItemsForApproveStatus(eq(companyId), eq(goodsId), eq(approveStatus));
	}

	@Test
	void dispatchJob_publishPayloadMatchesItemBatchEditStatusJobEnvelope() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		ItemBatchEditStatusEventJobDispatchPublisherImpl publisher =
				new ItemBatchEditStatusEventJobDispatchPublisherImpl(dispatchFacade);

		publisher.enqueueAfterApproveStatusUpdate(7L, 55L, "instock");

		verify(dispatchFacade)
				.dispatchJob(
						eq(GoodsBundleDispatchJobNames.ITEM_BATCH_EDIT_STATUS_EVENT_JOB),
						argThat(
								map ->
										map != null
												&& 7L == asLong(map.get("company_id"))
												&& 55L == asLong(map.get("goods_id"))
												&& "instock".equals(map.get("approve_status"))),
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
