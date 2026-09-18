package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.HfpayDispatchEventNames;
import cn.shopex.ecshopx.hfpay.dispatch.HfpayTradeRecordHfpayProfitSharingDispatchListener;
import cn.shopex.ecshopx.hfpay.service.HfpayTradeRecordService;
import cn.shopex.ecshopx.hfpay.service.profit.HfpayProfitSplitConfirmService;
import cn.shopex.ecshopx.orders.dispatch.ProfitSharingDispatchListener;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderProfitSharing;
import cn.shopex.ecshopx.orders.domain.OrderProfitSharingDetails;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.hfpay.dispatch.HfpayProfitSharingDispatchExecutionService;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderProfitSharingDetailsMapper;
import cn.shopex.ecshopx.orders.mapper.OrderProfitSharingMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class HfpayProfitSharingEventSyncBusDispatchFlowTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), OrderProfitSharing.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), OrderProfitSharingDetails.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrders.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Trade.class);
	}

	@Test
	void publishHfpayProfitSharing_sync_realListener_invokesPay006AndUpdatesSharingRow() {
		OrderProfitSharingMapper sharingMapper = mock(OrderProfitSharingMapper.class);
		OrderProfitSharingDetailsMapper detailsMapper = mock(OrderProfitSharingDetailsMapper.class);
		NormalOrdersMapper normalOrdersMapper = mock(NormalOrdersMapper.class);
		TradeMapper tradeMapper = mock(TradeMapper.class);
		HfpayProfitSplitConfirmService pay006 = mock(HfpayProfitSplitConfirmService.class);

		OrderProfitSharing data = new OrderProfitSharing();
		data.setOrderProfitSharingId(71L);
		data.setCompanyId(10L);
		data.setOrderId(900L);
		data.setStatus(0);
		data.setTotalFee(300);
		when(sharingMapper.selectById(71L)).thenReturn(data);

		OrderProfitSharingDetails det = new OrderProfitSharingDetails();
		det.setSharingId(71L);
		det.setTotalFee(300);
		det.setChannelId("uc");
		det.setChannelAcctId("ac");
		when(detailsMapper.selectList(any())).thenReturn(List.of(det));

		NormalOrders ord = new NormalOrders();
		ord.setOrderId(900L);
		ord.setCreateTime(1700000000);
		when(normalOrdersMapper.selectById(900L)).thenReturn(ord);

		Trade tr = new Trade();
		tr.setTradeId("TR-1");
		tr.setTradeState("SUCCESS");
		when(tradeMapper.selectOne(any())).thenReturn(tr);

		Map<String, Object> payRes = new LinkedHashMap<>();
		payRes.put("resp_code", "C00000");
		payRes.put("order_id", "OID");
		payRes.put("order_date", "20240102");
		payRes.put("resp_desc", "ok");
		when(pay006.pay006(anyLong(), anyString(), anyString(), eq("27"), anyString(), anyString()))
				.thenReturn(payRes);
		when(pay006.extractRespCode(any())).thenReturn("C00000");

		ObjectMapper om = new ObjectMapper();
		HfpayProfitSharingDispatchExecutionService execution =
				new HfpayProfitSharingDispatchExecutionService(
						sharingMapper, detailsMapper, normalOrdersMapper, tradeMapper, pay006, om);
		ProfitSharingDispatchListener listener = new ProfitSharingDispatchListener(execution);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		registry.registerEventListener(
				HfpayDispatchEventNames.EVENT_HFPAY_PROFIT_SHARING,
				HfpayDispatchEventNames.LISTENER_HFPAY_PROFIT_SHARING,
				ListenerDispatchOptions.syncDefaults(),
				listener);

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("order_id", 900L);
		entities.put("order_profit_sharing_id", List.of(71L));
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		assertDoesNotThrow(
				() ->
						facade.publishEvent(
								HfpayDispatchEventNames.EVENT_HFPAY_PROFIT_SHARING,
								payload,
								DispatchOptions.eventDefaults()));

		verify(pay006).pay006(anyLong(), anyString(), anyString(), eq("27"), anyString(), anyString());
		verify(sharingMapper).update(isNull(), any());
		assertTrue(captured.isEmpty());
	}

	@Test
	void publishHfpayProfitSharing_sync_emptyOrderProfitSharingIds_noPay006() {
		OrderProfitSharingMapper sharingMapper = mock(OrderProfitSharingMapper.class);
		OrderProfitSharingDetailsMapper detailsMapper = mock(OrderProfitSharingDetailsMapper.class);
		NormalOrdersMapper normalOrdersMapper = mock(NormalOrdersMapper.class);
		TradeMapper tradeMapper = mock(TradeMapper.class);
		HfpayProfitSplitConfirmService pay006 = mock(HfpayProfitSplitConfirmService.class);
		ObjectMapper om = new ObjectMapper();
		HfpayProfitSharingDispatchExecutionService execution =
				new HfpayProfitSharingDispatchExecutionService(
						sharingMapper, detailsMapper, normalOrdersMapper, tradeMapper, pay006, om);
		ProfitSharingDispatchListener listener = new ProfitSharingDispatchListener(execution);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		registry.registerEventListener(
				HfpayDispatchEventNames.EVENT_HFPAY_PROFIT_SHARING,
				HfpayDispatchEventNames.LISTENER_HFPAY_PROFIT_SHARING,
				ListenerDispatchOptions.syncDefaults(),
				listener);

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("order_id", 1L);
		entities.put("order_profit_sharing_id", List.of());
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		facade.publishEvent(
				HfpayDispatchEventNames.EVENT_HFPAY_PROFIT_SHARING, payload, DispatchOptions.eventDefaults());

		verify(pay006, never()).pay006(anyLong(), anyString(), anyString(), anyString(), anyString(), anyString());
		assertTrue(captured.isEmpty());
	}

	@Test
	void publishHfpayProfitSharing_sync_statusAlreadyOne_skipsPay006() {
		OrderProfitSharingMapper sharingMapper = mock(OrderProfitSharingMapper.class);
		OrderProfitSharingDetailsMapper detailsMapper = mock(OrderProfitSharingDetailsMapper.class);
		NormalOrdersMapper normalOrdersMapper = mock(NormalOrdersMapper.class);
		TradeMapper tradeMapper = mock(TradeMapper.class);
		HfpayProfitSplitConfirmService pay006 = mock(HfpayProfitSplitConfirmService.class);

		OrderProfitSharing row = new OrderProfitSharing();
		row.setOrderProfitSharingId(81L);
		row.setStatus(1);
		row.setCompanyId(1L);
		row.setOrderId(500L);
		when(sharingMapper.selectById(81L)).thenReturn(row);

		ObjectMapper om = new ObjectMapper();
		HfpayProfitSharingDispatchExecutionService execution =
				new HfpayProfitSharingDispatchExecutionService(
						sharingMapper, detailsMapper, normalOrdersMapper, tradeMapper, pay006, om);
		ProfitSharingDispatchListener listener = new ProfitSharingDispatchListener(execution);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		registry.registerEventListener(
				HfpayDispatchEventNames.EVENT_HFPAY_PROFIT_SHARING,
				HfpayDispatchEventNames.LISTENER_HFPAY_PROFIT_SHARING,
				ListenerDispatchOptions.syncDefaults(),
				listener);

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("order_id", 500L);
		entities.put("order_profit_sharing_id", List.of(81L));
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		facade.publishEvent(
				HfpayDispatchEventNames.EVENT_HFPAY_PROFIT_SHARING, payload, DispatchOptions.eventDefaults());

		verify(pay006, never()).pay006(anyLong(), anyString(), anyString(), anyString(), anyString(), anyString());
		assertTrue(captured.isEmpty());
	}

	@Test
	void publishHfpayProfitSharingTradeRecord295_sync_listener_invokesTradeRecordProfit() {
		HfpayTradeRecordService tradeRecordService = mock(HfpayTradeRecordService.class);
		HfpayTradeRecordHfpayProfitSharingDispatchListener tradeListener =
				new HfpayTradeRecordHfpayProfitSharingDispatchListener(tradeRecordService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		registry.registerEventListener(
				HfpayDispatchEventNames.EVENT_HFPAY_PROFIT_SHARING_TRADE_RECORD,
				HfpayDispatchEventNames.LISTENER_HFPAY_TRADE_RECORD_PROFIT_SHARING,
				ListenerDispatchOptions.syncDefaults(),
				tradeListener);

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("order_id", 900L);
		entities.put("order_profit_sharing_id", List.of(71L));
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		assertDoesNotThrow(
				() ->
						facade.publishEvent(
								HfpayDispatchEventNames.EVENT_HFPAY_PROFIT_SHARING_TRADE_RECORD,
								payload,
								DispatchOptions.eventDefaults()));

		verify(tradeRecordService).profit(eq("900"));
		assertTrue(captured.isEmpty());
	}
}
