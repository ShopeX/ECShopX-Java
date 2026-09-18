package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.ali.dispatch.ItemStoreUpdateSendAliTemplateDispatchListener;
import cn.shopex.ecshopx.ali.service.alitemplate.AliOpenTemplateLibraryRedisAccessor;
import cn.shopex.ecshopx.ali.service.alitemplate.AliTemplateMsgService;
import cn.shopex.ecshopx.ali.service.alitemplate.ItemStoreGoodsArrivalAliTemplateOrchestrator;
import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.common.dispatch.GoodsDispatchEventNames;
import cn.shopex.ecshopx.common.dispatch.PromotionsDispatchJobNames;
import cn.shopex.ecshopx.common.goods.ItemCompanyIdResolver;
import cn.shopex.ecshopx.common.members.port.MembersSubscribeNoticeItemNamePort;
import cn.shopex.ecshopx.config.AliTemplateMsgSendDispatchPublisherImpl;
import cn.shopex.ecshopx.members.domain.SubscribeNotice;
import cn.shopex.ecshopx.members.mapper.SubscribeNoticeMapper;
import cn.shopex.ecshopx.promotions.dispatch.AliTemplateMsgSendJobHandler;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class ItemStoreUpdatedAliTemplateBusDispatchFlowTest {

	@Test
	void publishItemStoreUpdated_syncRunsSubscribers_thenAliTemplateJobEnqueued() {
		DispatchListener arrival = mock(DispatchListener.class);
		AliTemplateMsgService aliSend = mock(AliTemplateMsgService.class);
		AliTemplateMsgSendJobHandler jobHandler = new AliTemplateMsgSendJobHandler(aliSend);

		ItemCompanyIdResolver companyIdResolver = mock(ItemCompanyIdResolver.class);
		when(companyIdResolver.findCompanyIdByItemId(55L)).thenReturn(Optional.of(9L));

		SubscribeNotice sub = new SubscribeNotice();
		sub.setOpenId("2088buyer");
		sub.setCompanyId(9L);
		sub.setRelId(55L);
		sub.setUserId(3L);

		SubscribeNoticeMapper subscribeNoticeMapper = mock(SubscribeNoticeMapper.class);
		when(subscribeNoticeMapper.selectList(any())).thenReturn(List.of(sub));

		MembersSubscribeNoticeItemNamePort itemNamePort = mock(MembersSubscribeNoticeItemNamePort.class);
		when(itemNamePort.resolveItemName(9L, 55L, "zh-CN")).thenReturn("Widget");

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		AliOpenTemplateLibraryRedisAccessor accessor = mock(AliOpenTemplateLibraryRedisAccessor.class);
		AliTemplateMsgSendDispatchPublisherImpl publisher = new AliTemplateMsgSendDispatchPublisherImpl(facade, accessor);

		ItemStoreGoodsArrivalAliTemplateOrchestrator orchestrator =
				new ItemStoreGoodsArrivalAliTemplateOrchestrator(
						companyIdResolver, subscribeNoticeMapper, itemNamePort, publisher);
		ItemStoreUpdateSendAliTemplateDispatchListener aliListener =
				new ItemStoreUpdateSendAliTemplateDispatchListener(orchestrator);

		registry.registerEventListener(
				GoodsDispatchEventNames.EVENT_ITEM_STORE_UPDATE,
				"listener:goods.listeners.SendTemplateMsgListener",
				ListenerDispatchOptions.syncDefaults(),
				aliListener);
		registry.registerEventListener(
				GoodsDispatchEventNames.EVENT_ITEM_STORE_UPDATE,
				"listener:members.listeners.ItemStoreUpdatedGoodsArrival",
				ListenerDispatchOptions.syncDefaults(),
				arrival);
		registry.registerJob(PromotionsDispatchJobNames.ALI_TEMPLATE_MSG_SEND, jobHandler);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("item_id", 55L);
		payload.put("store", 5);
		payload.put("distributor_id", 0L);

		facade.publishEvent(GoodsDispatchEventNames.EVENT_ITEM_STORE_UPDATE, payload, DispatchOptions.eventDefaults());

		InOrder order = inOrder(subscribeNoticeMapper, arrival);
		order.verify(subscribeNoticeMapper).selectList(any());
		order.verify(arrival).onEvent(any());

		assertEquals(1, captured.size());
		assertEquals(PromotionsDispatchJobNames.ALI_TEMPLATE_MSG_SEND, captured.get(0).messageName());

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(captured.get(0), 1);

		verify(aliSend).send(any(), eq(true));
	}
}
