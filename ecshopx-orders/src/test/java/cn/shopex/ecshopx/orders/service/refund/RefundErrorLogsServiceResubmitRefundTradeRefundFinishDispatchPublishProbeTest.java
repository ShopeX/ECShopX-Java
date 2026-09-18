package cn.shopex.ecshopx.orders.service.refund;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.dispatch.AftersalesRefundTradeRefundFinishPayloadMapper;
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyDispatchEventNames;
import cn.shopex.ecshopx.common.dispatch.TradeRefundFinishEventDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeRefundStatisticsJobDispatchPublisher;
import cn.shopex.ecshopx.dispatch.DispatchCore;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchFanOutPlanner;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchRegistry;
import cn.shopex.ecshopx.dispatch.ListenerDispatchOptions;
import cn.shopex.ecshopx.dispatch.SyncDispatchDriver;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayOrderApplyIdGenerator;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.RefundErrorLogs;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.RefundErrorLogsMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
import cn.shopex.ecshopx.thirdparty.dispatch.ThirdPartyTradeRefundFinishDmCrmDispatchListener;
import cn.shopex.ecshopx.thirdparty.dispatch.ThirdPartyTradeRefundPushMarketingCenterDispatchListener;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.TradeRefundFinishDmCrmProcessor;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.TradeRefundPushMarketingCenterProcessor;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Pure-publisher probes plus one SYNC Bus slice; listener order and payload baselines for
 * {@code EVENT_TRADE_REFUND_FINISH} are anchored in
 * {@link cn.shopex.ecshopx.dispatch.TradeRefundFinishThirdPartySyncBusDispatchFlowTest}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EVENT_TRADE_REFUND_FINISH: resubmitRefund → doRefund → TradeRefundFinishEventDispatchPublisher probe")
class RefundErrorLogsServiceResubmitRefundTradeRefundFinishDispatchPublishProbeTest {

	private static final String LOG_ID_ZERO_FEE = "99001";
	private static final String LOG_ID_FAIL = "99002";

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), AftersalesRefund.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Trade.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrdersItems.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), RefundErrorLogs.class);
	}

	@Test
	void resubmitRefund_afterDoRefund_invokesTradeRefundFinishPublisherOnce_withMapperAlignedPayload() {
		RefundErrorLogsMapper refundErrorLogsMapper = mock(RefundErrorLogsMapper.class);
		AftersalesRefundMapper aftersalesRefundMapper = mock(AftersalesRefundMapper.class);
		TradeMapper tradeMapper = mock(TradeMapper.class);
		NormalOrdersItemsMapper normalOrdersItemsMapper = mock(NormalOrdersItemsMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		PointMemberAddPointService pointMemberAddPointService = mock(PointMemberAddPointService.class);
		TradeRefundStatisticsJobDispatchPublisher tradeRefundStatisticsJobDispatchPublisher =
				mock(TradeRefundStatisticsJobDispatchPublisher.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
		TradeRefundFinishEventDispatchPublisher tradeRefundFinishEventDispatchPublisher =
				mock(TradeRefundFinishEventDispatchPublisher.class);
		OrdersRefundPaymentDispatchService ordersRefundPaymentDispatchService =
				mock(OrdersRefundPaymentDispatchService.class);

		long companyId = 881_001L;
		long refundBn = 881_002L;
		long orderId = 881_003L;
		long id = Long.parseLong(LOG_ID_ZERO_FEE);

		RefundErrorLogs initialLog = new RefundErrorLogs();
		initialLog.setId(id);
		initialLog.setDataJson("{\"refund_bn\":" + refundBn + ",\"company_id\":" + companyId + "}");
		RefundErrorLogs freshLog = new RefundErrorLogs();
		freshLog.setId(id);
		freshLog.setDataJson(initialLog.getDataJson());
		freshLog.setIsResubmit(Boolean.TRUE);
		when(refundErrorLogsMapper.selectById(id)).thenReturn(initialLog, freshLog);
		when(refundErrorLogsMapper.update(isNull(), any())).thenReturn(1);

		AftersalesRefund refundBefore = new AftersalesRefund();
		refundBefore.setCompanyId(companyId);
		refundBefore.setRefundBn(refundBn);
		refundBefore.setOrderId(orderId);
		refundBefore.setTradeId("tr-zero-fee-1");
		refundBefore.setPayType("alipay");
		refundBefore.setRefundFee(0);
		refundBefore.setFreightType("cash");
		refundBefore.setFreight(0);
		refundBefore.setRefundPoint(0);
		refundBefore.setUserId(11L);
		refundBefore.setAftersalesBn(0L);
		refundBefore.setShopId(501L);
		refundBefore.setDistributorId(601L);

		AftersalesRefund refundAfter = new AftersalesRefund();
		refundAfter.setCompanyId(companyId);
		refundAfter.setRefundBn(refundBn);
		refundAfter.setOrderId(orderId);
		refundAfter.setTradeId("tr-zero-fee-1");
		refundAfter.setPayType("alipay");
		refundAfter.setRefundFee(0);
		refundAfter.setRefundStatus("SUCCESS");
		refundAfter.setRefundId("");
		refundAfter.setAftersalesBn(0L);
		refundAfter.setRefundedFee(0);
		refundAfter.setRefundSuccessTime((long) (System.currentTimeMillis() / 1000L));
		refundAfter.setShopId(501L);
		refundAfter.setDistributorId(601L);

		AtomicInteger refundSelectCalls = new AtomicInteger(0);
		when(aftersalesRefundMapper.selectOne(any()))
				.thenAnswer(inv -> refundSelectCalls.incrementAndGet() == 1 ? refundBefore : refundAfter);

		Trade trade = new Trade();
		trade.setCompanyId(String.valueOf(companyId));
		trade.setTradeId("tr-zero-fee-1");
		trade.setTradeState("SUCCESS");
		trade.setPayFee(0);
		when(tradeMapper.selectOne(any())).thenReturn(trade);
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(Collections.emptyList());

		AftersalesRefundDoRefundService aftersalesRefundDoRefundService =
				new AftersalesRefundDoRefundService(ordersRefundPaymentDispatchService,
						aftersalesRefundMapper,
						tradeMapper,
						normalOrdersItemsMapper,
						aftersalesDetailMapper,
						applicationEventPublisher,
						pointMemberAddPointService,
						tradeRefundStatisticsJobDispatchPublisher,
						orderProcessLogPublishPort,
						mock(HfPayOrderApplyIdGenerator.class),
						tradeRefundFinishEventDispatchPublisher,
					mock(RefundErrorLogsRecorder.class),
					org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class),
					org.mockito.Mockito.mock(cn.shopex.ecshopx.aftersales.support.EmployeePurchasePrepaidRefundLinesResolver.class));

		RefundErrorLogsService refundErrorLogsService =
				new RefundErrorLogsService(
						refundErrorLogsMapper, aftersalesRefundDoRefundService, new ObjectMapper());

		refundErrorLogsService.resubmitRefund(LOG_ID_ZERO_FEE);

		verify(ordersRefundPaymentDispatchService, never())
				.dispatch(anyLong(), any(), any(), any(), anyInt(), anyInt(), anyBoolean());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(tradeRefundFinishEventDispatchPublisher, times(1)).publish(payloadCaptor.capture());
		Map<String, Object> p = payloadCaptor.getValue();
		assertEquals(companyId, ((Number) p.get("company_id")).longValue());
		assertEquals(orderId, ((Number) p.get("order_id")).longValue());
		assertEquals(refundBn, ((Number) p.get("refund_bn")).longValue());
		assertEquals("tr-zero-fee-1", p.get("trade_id"));
		assertEquals(0L, ((Number) p.get("aftersales_bn")).longValue());
		assertEquals(501L, ((Number) p.get("shop_id")).longValue());
		assertEquals(601L, ((Number) p.get("distributor_id")).longValue());
		assertEquals(0, ((Number) p.get("refund_fee")).intValue());
		assertEquals(0, ((Number) p.get("refunded_fee")).intValue());
		String refundSuccessTime = String.valueOf(p.get("refund_success_time"));
		assertTrue(
				refundSuccessTime.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"),
				"refund_success_time should be formatted: " + refundSuccessTime);
	}

	@Test
	@DisplayName(
			"resubmit success: SYNC fan-out invokes marketing then DmCrm processor (order baseline: TradeRefundFinishThirdPartySyncBusDispatchFlowTest)")
	void resubmitRefund_syncBus_fanOut_invokesMarketingThenDmCrmProcessorWithSamePayloadAsMapper() {
		RefundErrorLogsMapper refundErrorLogsMapper = mock(RefundErrorLogsMapper.class);
		AftersalesRefundMapper aftersalesRefundMapper = mock(AftersalesRefundMapper.class);
		TradeMapper tradeMapper = mock(TradeMapper.class);
		NormalOrdersItemsMapper normalOrdersItemsMapper = mock(NormalOrdersItemsMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		PointMemberAddPointService pointMemberAddPointService = mock(PointMemberAddPointService.class);
		TradeRefundStatisticsJobDispatchPublisher tradeRefundStatisticsJobDispatchPublisher =
				mock(TradeRefundStatisticsJobDispatchPublisher.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
		OrdersRefundPaymentDispatchService ordersRefundPaymentDispatchService =
				mock(OrdersRefundPaymentDispatchService.class);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		TradeRefundPushMarketingCenterProcessor marketingProcessor =
				mock(TradeRefundPushMarketingCenterProcessor.class);
		ThirdPartyTradeRefundPushMarketingCenterDispatchListener marketingListener =
				new ThirdPartyTradeRefundPushMarketingCenterDispatchListener(marketingProcessor);
		TradeRefundFinishDmCrmProcessor dmCrmProcessor = mock(TradeRefundFinishDmCrmProcessor.class);
		ThirdPartyTradeRefundFinishDmCrmDispatchListener dmCrmListener =
				new ThirdPartyTradeRefundFinishDmCrmDispatchListener(dmCrmProcessor);

		registry.registerEventListener(
				ThirdPartyDispatchEventNames.EVENT_TRADE_REFUND_FINISH,
				"listener:thirdparty.trade_refund_finish_push_marketing_center",
				ListenerDispatchOptions.syncDefaults(),
				marketingListener);
		registry.registerEventListener(
				ThirdPartyDispatchEventNames.EVENT_TRADE_REFUND_FINISH,
				"listener:thirdparty.trade_refund_finish_dm_crm",
				ListenerDispatchOptions.syncDefaults(),
				dmCrmListener);

		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), java.util.Map.of());
		DispatchFacade dispatchFacade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));
		TradeRefundFinishEventDispatchPublisher tradeRefundFinishEventDispatchPublisher =
				map ->
						dispatchFacade.publishEvent(
								ThirdPartyDispatchEventNames.EVENT_TRADE_REFUND_FINISH,
								map,
								DispatchOptions.eventDefaults());

		long companyId = 881_001L;
		long refundBn = 881_002L;
		long orderId = 881_003L;
		long id = Long.parseLong(LOG_ID_ZERO_FEE);

		RefundErrorLogs initialLog = new RefundErrorLogs();
		initialLog.setId(id);
		initialLog.setDataJson("{\"refund_bn\":" + refundBn + ",\"company_id\":" + companyId + "}");
		RefundErrorLogs freshLog = new RefundErrorLogs();
		freshLog.setId(id);
		freshLog.setDataJson(initialLog.getDataJson());
		freshLog.setIsResubmit(Boolean.TRUE);
		when(refundErrorLogsMapper.selectById(id)).thenReturn(initialLog, freshLog);
		when(refundErrorLogsMapper.update(isNull(), any())).thenReturn(1);

		AftersalesRefund refundBefore = new AftersalesRefund();
		refundBefore.setCompanyId(companyId);
		refundBefore.setRefundBn(refundBn);
		refundBefore.setOrderId(orderId);
		refundBefore.setTradeId("tr-zero-fee-1");
		refundBefore.setPayType("alipay");
		refundBefore.setRefundFee(0);
		refundBefore.setFreightType("cash");
		refundBefore.setFreight(0);
		refundBefore.setRefundPoint(0);
		refundBefore.setUserId(11L);
		refundBefore.setAftersalesBn(0L);
		refundBefore.setShopId(501L);
		refundBefore.setDistributorId(601L);

		AftersalesRefund refundAfter = new AftersalesRefund();
		refundAfter.setCompanyId(companyId);
		refundAfter.setRefundBn(refundBn);
		refundAfter.setOrderId(orderId);
		refundAfter.setTradeId("tr-zero-fee-1");
		refundAfter.setPayType("alipay");
		refundAfter.setRefundFee(0);
		refundAfter.setRefundStatus("SUCCESS");
		refundAfter.setRefundId("");
		refundAfter.setAftersalesBn(0L);
		refundAfter.setRefundedFee(0);
		refundAfter.setRefundSuccessTime((long) (System.currentTimeMillis() / 1000L));
		refundAfter.setShopId(501L);
		refundAfter.setDistributorId(601L);

		AtomicInteger refundSelectCalls = new AtomicInteger(0);
		when(aftersalesRefundMapper.selectOne(any()))
				.thenAnswer(inv -> refundSelectCalls.incrementAndGet() == 1 ? refundBefore : refundAfter);

		Trade trade = new Trade();
		trade.setCompanyId(String.valueOf(companyId));
		trade.setTradeId("tr-zero-fee-1");
		trade.setTradeState("SUCCESS");
		trade.setPayFee(0);
		when(tradeMapper.selectOne(any())).thenReturn(trade);
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(Collections.emptyList());

		AftersalesRefundDoRefundService aftersalesRefundDoRefundService =
				new AftersalesRefundDoRefundService(ordersRefundPaymentDispatchService,
						aftersalesRefundMapper,
						tradeMapper,
						normalOrdersItemsMapper,
						aftersalesDetailMapper,
						applicationEventPublisher,
						pointMemberAddPointService,
						tradeRefundStatisticsJobDispatchPublisher,
						orderProcessLogPublishPort,
						mock(HfPayOrderApplyIdGenerator.class),
						tradeRefundFinishEventDispatchPublisher,
					mock(RefundErrorLogsRecorder.class),
					org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class),
					org.mockito.Mockito.mock(cn.shopex.ecshopx.aftersales.support.EmployeePurchasePrepaidRefundLinesResolver.class));

		RefundErrorLogsService refundErrorLogsService =
				new RefundErrorLogsService(
						refundErrorLogsMapper, aftersalesRefundDoRefundService, new ObjectMapper());

		refundErrorLogsService.resubmitRefund(LOG_ID_ZERO_FEE);

		verify(ordersRefundPaymentDispatchService, never())
				.dispatch(anyLong(), any(), any(), any(), anyInt(), anyInt(), anyBoolean());

		InOrder inOrder = inOrder(marketingProcessor, dmCrmProcessor);
		inOrder.verify(marketingProcessor).handle(any());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> dmCrmCaptor = ArgumentCaptor.forClass(Map.class);
		inOrder.verify(dmCrmProcessor).handle(dmCrmCaptor.capture());
		assertEquals(
				AftersalesRefundTradeRefundFinishPayloadMapper.toPayload(refundAfter),
				dmCrmCaptor.getValue());
	}

	@Test
	void resubmitRefund_gatewayFailPath_stillInvokesTradeRefundFinishPublisherOnce() {
		RefundErrorLogsMapper refundErrorLogsMapper = mock(RefundErrorLogsMapper.class);
		AftersalesRefundMapper aftersalesRefundMapper = mock(AftersalesRefundMapper.class);
		TradeMapper tradeMapper = mock(TradeMapper.class);
		NormalOrdersItemsMapper normalOrdersItemsMapper = mock(NormalOrdersItemsMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		PointMemberAddPointService pointMemberAddPointService = mock(PointMemberAddPointService.class);
		TradeRefundStatisticsJobDispatchPublisher tradeRefundStatisticsJobDispatchPublisher =
				mock(TradeRefundStatisticsJobDispatchPublisher.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
		TradeRefundFinishEventDispatchPublisher tradeRefundFinishEventDispatchPublisher =
				mock(TradeRefundFinishEventDispatchPublisher.class);
		OrdersRefundPaymentDispatchService ordersRefundPaymentDispatchService =
				mock(OrdersRefundPaymentDispatchService.class);

		Map<String, Object> fail = new LinkedHashMap<>();
		fail.put("status", "FAIL");
		fail.put("error_desc", "gateway declined");
		when(ordersRefundPaymentDispatchService.dispatch(
						anyLong(), any(), any(AftersalesRefund.class), any(Trade.class), anyInt(), anyInt(), eq(true)))
				.thenReturn(fail);

		long companyId = 882_001L;
		long refundBn = 882_002L;
		long orderId = 882_003L;
		long id = Long.parseLong(LOG_ID_FAIL);

		RefundErrorLogs initialLog = new RefundErrorLogs();
		initialLog.setId(id);
		initialLog.setDataJson("{\"refund_bn\":" + refundBn + ",\"company_id\":" + companyId + "}");
		RefundErrorLogs freshLog = new RefundErrorLogs();
		freshLog.setId(id);
		freshLog.setDataJson(initialLog.getDataJson());
		freshLog.setIsResubmit(Boolean.TRUE);
		when(refundErrorLogsMapper.selectById(id)).thenReturn(initialLog, freshLog);
		when(refundErrorLogsMapper.update(isNull(), any())).thenReturn(1);

		AftersalesRefund refundBefore = new AftersalesRefund();
		refundBefore.setCompanyId(companyId);
		refundBefore.setRefundBn(refundBn);
		refundBefore.setOrderId(orderId);
		refundBefore.setTradeId("tr-fail-1");
		refundBefore.setPayType("wxpayjs");
		refundBefore.setRefundFee(80);
		refundBefore.setFreightType("cash");
		refundBefore.setFreight(0);
		refundBefore.setRefundPoint(0);
		refundBefore.setUserId(12L);
		refundBefore.setAftersalesBn(0L);
		refundBefore.setDistributorId(602L);

		AftersalesRefund refundAfter = new AftersalesRefund();
		refundAfter.setCompanyId(companyId);
		refundAfter.setRefundBn(refundBn);
		refundAfter.setOrderId(orderId);
		refundAfter.setTradeId("tr-fail-1");
		refundAfter.setPayType("wxpayjs");
		refundAfter.setRefundFee(80);
		refundAfter.setRefundStatus("CHANGE");
		refundAfter.setDistributorId(602L);

		AtomicInteger refundSelectCalls = new AtomicInteger(0);
		when(aftersalesRefundMapper.selectOne(any()))
				.thenAnswer(inv -> refundSelectCalls.incrementAndGet() == 1 ? refundBefore : refundAfter);

		Trade trade = new Trade();
		trade.setCompanyId(String.valueOf(companyId));
		trade.setTradeId("tr-fail-1");
		trade.setTradeState("SUCCESS");
		trade.setPayFee(200);
		when(tradeMapper.selectOne(any())).thenReturn(trade);

		AftersalesRefundDoRefundService aftersalesRefundDoRefundService =
				new AftersalesRefundDoRefundService(ordersRefundPaymentDispatchService,
						aftersalesRefundMapper,
						tradeMapper,
						normalOrdersItemsMapper,
						aftersalesDetailMapper,
						applicationEventPublisher,
						pointMemberAddPointService,
						tradeRefundStatisticsJobDispatchPublisher,
						orderProcessLogPublishPort,
						mock(HfPayOrderApplyIdGenerator.class),
						tradeRefundFinishEventDispatchPublisher,
					mock(RefundErrorLogsRecorder.class),
					org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class),
					org.mockito.Mockito.mock(cn.shopex.ecshopx.aftersales.support.EmployeePurchasePrepaidRefundLinesResolver.class));

		RefundErrorLogsService refundErrorLogsService =
				new RefundErrorLogsService(
						refundErrorLogsMapper, aftersalesRefundDoRefundService, new ObjectMapper());

		refundErrorLogsService.resubmitRefund(LOG_ID_FAIL);

		verify(ordersRefundPaymentDispatchService, times(1))
				.dispatch(anyLong(), any(), any(), any(), anyInt(), anyInt(), eq(true));
		verify(tradeRefundStatisticsJobDispatchPublisher, never()).publish(any());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(tradeRefundFinishEventDispatchPublisher, times(1)).publish(payloadCaptor.capture());
		assertEquals(
				AftersalesRefundTradeRefundFinishPayloadMapper.toPayload(refundAfter),
				payloadCaptor.getValue());
	}
}
