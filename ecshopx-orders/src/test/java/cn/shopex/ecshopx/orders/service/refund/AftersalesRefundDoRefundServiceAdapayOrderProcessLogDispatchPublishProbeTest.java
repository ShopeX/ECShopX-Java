package cn.shopex.ecshopx.orders.service.refund;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.dispatch.TradeRefundFinishEventDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeRefundStatisticsJobDispatchPublisher;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.integration.OrderProcessLogPublishPortImpl;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayOrderApplyIdGenerator;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("EVENT_ORDER_PROCESS_LOG: RefundJob/adapay doRefund publishEvent probe")
class AftersalesRefundDoRefundServiceAdapayOrderProcessLogDispatchPublishProbeTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), AftersalesRefund.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Trade.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrdersItems.class);
	}

	@Test
	void doRefund_adapay_success_invokesPublishEventOnce_withRefundSuccessDetail() {
		OrdersRefundPaymentDispatchService ordersRefundPaymentDispatchService =
				mock(OrdersRefundPaymentDispatchService.class);
		AftersalesRefundMapper aftersalesRefundMapper = mock(AftersalesRefundMapper.class);
		TradeMapper tradeMapper = mock(TradeMapper.class);
		NormalOrdersItemsMapper normalOrdersItemsMapper = mock(NormalOrdersItemsMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		PointMemberAddPointService pointMemberAddPointService = mock(PointMemberAddPointService.class);
		TradeRefundStatisticsJobDispatchPublisher tradeRefundStatisticsJobDispatchPublisher =
				mock(TradeRefundStatisticsJobDispatchPublisher.class);
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = new OrderProcessLogPublishPortImpl(dispatchFacade);

		long companyId = 701L;
		long refundBn = 42001L;
		long orderId = 90001L;

		AftersalesRefund refundBefore = new AftersalesRefund();
		refundBefore.setCompanyId(companyId);
		refundBefore.setRefundBn(refundBn);
		refundBefore.setOrderId(orderId);
		refundBefore.setTradeId("tr-adapay-1");
		refundBefore.setPayType("adapay");
		refundBefore.setRefundFee(100);
		refundBefore.setFreightType("cash");
		refundBefore.setFreight(0);
		refundBefore.setRefundPoint(0);
		refundBefore.setUserId(1L);
		refundBefore.setAftersalesBn(500L);

		AftersalesRefund refundAfter = new AftersalesRefund();
		refundAfter.setCompanyId(companyId);
		refundAfter.setRefundBn(refundBn);
		refundAfter.setOrderId(orderId);
		refundAfter.setTradeId("tr-adapay-1");
		refundAfter.setPayType("adapay");
		refundAfter.setRefundFee(100);
		refundAfter.setRefundStatus("SUCCESS");
		refundAfter.setRefundId("rid-adapay-1");
		refundAfter.setAftersalesBn(500L);
		refundAfter.setRefundedFee(100);

		AtomicInteger selectCalls = new AtomicInteger(0);
		when(aftersalesRefundMapper.selectOne(any()))
				.thenAnswer(inv -> selectCalls.incrementAndGet() == 1 ? refundBefore : refundAfter);

		Trade trade = new Trade();
		trade.setCompanyId(String.valueOf(companyId));
		trade.setTradeId("tr-adapay-1");
		trade.setTradeState("SUCCESS");
		trade.setPayFee(200);
		when(tradeMapper.selectOne(any())).thenReturn(trade);

		Map<String, Object> payRes = new LinkedHashMap<>();
		payRes.put("status", "SUCCESS");
		payRes.put("refund_id", "rid-adapay-1");
		when(ordersRefundPaymentDispatchService.dispatch(
						eq(companyId),
						any(),
						any(AftersalesRefund.class),
						any(Trade.class),
						eq(100),
						eq(0),
						eq(false)))
				.thenReturn(payRes);

		when(aftersalesDetailMapper.selectList(any())).thenReturn(Collections.emptyList());

		AftersalesRefundDoRefundService service =
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
					mock(TradeRefundFinishEventDispatchPublisher.class),
					mock(RefundErrorLogsRecorder.class),
					org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class),
					org.mockito.Mockito.mock(cn.shopex.ecshopx.aftersales.support.EmployeePurchasePrepaidRefundLinesResolver.class));

		service.doRefund(companyId, refundBn, false);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));
		Map<String, Object> payload = payloadCaptor.getValue();
		assertEquals(orderId, payload.get("order_id"));
		assertEquals(companyId, payload.get("company_id"));
		assertEquals("system", payload.get("operator_type"));
		assertEquals("订单退款", payload.get("remarks"));
		assertEquals(
				"订单号：" + orderId + "，订单退款成功（adapay支付渠道）",
				String.valueOf(payload.get("detail")));
	}

	@Test
	void doRefund_adapay_fail_invokesPublishEventOnce_withErrorDetail() {
		OrdersRefundPaymentDispatchService ordersRefundPaymentDispatchService =
				mock(OrdersRefundPaymentDispatchService.class);
		AftersalesRefundMapper aftersalesRefundMapper = mock(AftersalesRefundMapper.class);
		TradeMapper tradeMapper = mock(TradeMapper.class);
		NormalOrdersItemsMapper normalOrdersItemsMapper = mock(NormalOrdersItemsMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		PointMemberAddPointService pointMemberAddPointService = mock(PointMemberAddPointService.class);
		TradeRefundStatisticsJobDispatchPublisher tradeRefundStatisticsJobDispatchPublisher =
				mock(TradeRefundStatisticsJobDispatchPublisher.class);
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = new OrderProcessLogPublishPortImpl(dispatchFacade);

		long companyId = 702L;
		long refundBn = 42002L;
		long orderId = 90002L;
		String err = "AdaPay gateway rejected";

		AftersalesRefund refundBefore = new AftersalesRefund();
		refundBefore.setCompanyId(companyId);
		refundBefore.setRefundBn(refundBn);
		refundBefore.setOrderId(orderId);
		refundBefore.setTradeId("tr-adapay-2");
		refundBefore.setPayType("AdaPay");
		refundBefore.setRefundFee(50);
		refundBefore.setFreightType("cash");
		refundBefore.setFreight(0);
		refundBefore.setRefundPoint(0);
		refundBefore.setUserId(2L);
		refundBefore.setAftersalesBn(501L);

		AftersalesRefund refundAfter = new AftersalesRefund();
		refundAfter.setCompanyId(companyId);
		refundAfter.setRefundBn(refundBn);
		refundAfter.setOrderId(orderId);
		refundAfter.setTradeId("tr-adapay-2");
		refundAfter.setPayType("adapay");
		refundAfter.setRefundFee(50);
		refundAfter.setRefundStatus("CHANGE");

		AtomicInteger selectCalls = new AtomicInteger(0);
		when(aftersalesRefundMapper.selectOne(any()))
				.thenAnswer(inv -> selectCalls.incrementAndGet() == 1 ? refundBefore : refundAfter);

		Trade trade = new Trade();
		trade.setCompanyId(String.valueOf(companyId));
		trade.setTradeId("tr-adapay-2");
		trade.setTradeState("SUCCESS");
		trade.setPayFee(100);
		when(tradeMapper.selectOne(any())).thenReturn(trade);

		Map<String, Object> payRes = new LinkedHashMap<>();
		payRes.put("status", "FAIL");
		payRes.put("error_desc", err);
		when(ordersRefundPaymentDispatchService.dispatch(
						eq(companyId),
						any(),
						any(AftersalesRefund.class),
						any(Trade.class),
						eq(50),
						eq(0),
						eq(false)))
				.thenReturn(payRes);

		AftersalesRefundDoRefundService service =
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
					mock(TradeRefundFinishEventDispatchPublisher.class),
					mock(RefundErrorLogsRecorder.class),
					org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class),
					org.mockito.Mockito.mock(cn.shopex.ecshopx.aftersales.support.EmployeePurchasePrepaidRefundLinesResolver.class));

		service.doRefund(companyId, refundBn, false);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));
		Map<String, Object> payload = payloadCaptor.getValue();
		assertEquals(
				"订单号：" + orderId + "，订单退款失败（adapay支付渠道），失败原因：" + err,
				String.valueOf(payload.get("detail")));
	}
}
