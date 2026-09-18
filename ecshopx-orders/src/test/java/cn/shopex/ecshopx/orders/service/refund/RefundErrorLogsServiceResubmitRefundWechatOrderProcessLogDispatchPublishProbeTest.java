package cn.shopex.ecshopx.orders.service.refund;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.payment.PaymentSubjectDistributorIdPort;
import cn.shopex.ecshopx.common.dispatch.TradeRefundFinishEventDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeRefundStatisticsJobDispatchPublisher;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.integration.OrderProcessLogPublishPortImpl;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayOrderApplyIdGenerator;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.RefundErrorLogs;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.RefundErrorLogsMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.payment.service.orderrefund.AlipayTradeAftersalesRefundRunner;
import cn.shopex.ecshopx.payment.service.orderrefund.BsPayAftersalesOnlineRefundRunner;
import cn.shopex.ecshopx.payment.service.orderrefund.StandardOnlinePayAftersalesRefundExecutor;
import cn.shopex.ecshopx.payment.service.orderrefund.WxpaySecapiAftersalesRefundRunner;
import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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
@DisplayName("EVENT_ORDER_PROCESS_LOG: resubmitRefund/wxpay publishEvent probe")
class RefundErrorLogsServiceResubmitRefundWechatOrderProcessLogDispatchPublishProbeTest {

	private static final String LOG_ID_SUCCESS = "9025";
	private static final String LOG_ID_FAIL = "9026";

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), AftersalesRefund.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Trade.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrdersItems.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), RefundErrorLogs.class);
	}

	@Test
	void resubmitRefund_wxpay_success_invokesPublishEventOnce_withExecutorAlignedDetail() {
		RefundErrorLogsMapper refundErrorLogsMapper = mock(RefundErrorLogsMapper.class);
		AftersalesRefundMapper aftersalesRefundMapper = mock(AftersalesRefundMapper.class);
		TradeMapper tradeMapper = mock(TradeMapper.class);
		NormalOrdersItemsMapper normalOrdersItemsMapper = mock(NormalOrdersItemsMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		PointMemberAddPointService pointMemberAddPointService = mock(PointMemberAddPointService.class);
		TradeRefundStatisticsJobDispatchPublisher tradeRefundStatisticsJobDispatchPublisher =
				mock(TradeRefundStatisticsJobDispatchPublisher.class);
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		OrderProcessLogPublishPort dispatchStackOrderProcessLogPublishPort =
				new OrderProcessLogPublishPortImpl(dispatchFacade);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);

		WxpaySecapiAftersalesRefundRunner wx = mock(WxpaySecapiAftersalesRefundRunner.class);
		AlipayTradeAftersalesRefundRunner alipay = mock(AlipayTradeAftersalesRefundRunner.class);
		BsPayAftersalesOnlineRefundRunner bs = mock(BsPayAftersalesOnlineRefundRunner.class);
		PaymentSubjectDistributorIdPort paymentSubjectDistributorIdPort =
				mock(PaymentSubjectDistributorIdPort.class);
		when(paymentSubjectDistributorIdPort.resolveActualDistributorId(anyLong(), anyLong()))
				.thenAnswer(inv -> (Long) inv.getArgument(1));
		StandardOnlinePayAftersalesRefundExecutor channelExecutor =
				new StandardOnlinePayAftersalesRefundExecutor(wx, alipay, bs, dispatchStackOrderProcessLogPublishPort, paymentSubjectDistributorIdPort);

		OrdersRefundPaymentDispatchService ordersRefundPaymentDispatchService =
				new OrdersRefundPaymentDispatchService(
						List.of(channelExecutor),
						pointMemberAddPointService,
						mock(OrdersDepositOrderRefundService.class),
						dispatchStackOrderProcessLogPublishPort,
						mock(NormalOrdersMapper.class));

		Map<String, Object> ok = new LinkedHashMap<>();
		ok.put("status", "SUCCESS");
		ok.put("refund_id", "wx-resubmit-rid-1");
		when(wx.refund(anyLong(), anyLong(), anyString(), anyString(), anyLong(), anyInt(), anyInt()))
				.thenReturn(ok);

		long companyId = 91561L;
		long refundBn = 51561L;
		long orderId = 61561L;
		long id = Long.parseLong(LOG_ID_SUCCESS);

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
		refundBefore.setTradeId("tr-wxpay-resubmit-1");
		refundBefore.setPayType("wxpayjs");
		refundBefore.setRefundFee(100);
		refundBefore.setFreightType("cash");
		refundBefore.setFreight(0);
		refundBefore.setRefundPoint(0);
		refundBefore.setUserId(1L);
		refundBefore.setAftersalesBn(0L);
		refundBefore.setDistributorId(701L);

		AftersalesRefund refundAfter = new AftersalesRefund();
		refundAfter.setCompanyId(companyId);
		refundAfter.setRefundBn(refundBn);
		refundAfter.setOrderId(orderId);
		refundAfter.setTradeId("tr-wxpay-resubmit-1");
		refundAfter.setPayType("wxpayjs");
		refundAfter.setRefundFee(100);
		refundAfter.setRefundStatus("SUCCESS");
		refundAfter.setRefundId("wx-resubmit-rid-1");
		refundAfter.setAftersalesBn(0L);
		refundAfter.setRefundedFee(100);
		refundAfter.setDistributorId(701L);

		AtomicInteger refundSelectCalls = new AtomicInteger(0);
		when(aftersalesRefundMapper.selectOne(any()))
				.thenAnswer(inv -> refundSelectCalls.incrementAndGet() == 1 ? refundBefore : refundAfter);

		Trade trade = new Trade();
		trade.setCompanyId(String.valueOf(companyId));
		trade.setTradeId("tr-wxpay-resubmit-1");
		trade.setTradeState("SUCCESS");
		trade.setPayFee(200);
		trade.setPayChannel("wxpay");
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
						mock(TradeRefundFinishEventDispatchPublisher.class),
					mock(RefundErrorLogsRecorder.class),
					org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class),
					org.mockito.Mockito.mock(cn.shopex.ecshopx.aftersales.support.EmployeePurchasePrepaidRefundLinesResolver.class));

		RefundErrorLogsService refundErrorLogsService =
				new RefundErrorLogsService(
						refundErrorLogsMapper, aftersalesRefundDoRefundService, new ObjectMapper());

		refundErrorLogsService.resubmitRefund(LOG_ID_SUCCESS);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));
		Map<String, Object> payload = payloadCaptor.getValue();
		assertEquals(orderId, ((Number) payload.get("order_id")).longValue());
		assertEquals(companyId, ((Number) payload.get("company_id")).longValue());
		assertEquals("system", payload.get("operator_type"));
		assertEquals("订单退款", payload.get("remarks"));
		assertEquals("订单号：" + orderId + "，订单退款成功（微信支付渠道）", String.valueOf(payload.get("detail")));
		verify(orderProcessLogPublishPort, never()).publish(any());
	}

	@Test
	void resubmitRefund_wxpay_fail_invokesPublishEventOnce_withExecutorAlignedDetail() {
		RefundErrorLogsMapper refundErrorLogsMapper = mock(RefundErrorLogsMapper.class);
		AftersalesRefundMapper aftersalesRefundMapper = mock(AftersalesRefundMapper.class);
		TradeMapper tradeMapper = mock(TradeMapper.class);
		NormalOrdersItemsMapper normalOrdersItemsMapper = mock(NormalOrdersItemsMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		PointMemberAddPointService pointMemberAddPointService = mock(PointMemberAddPointService.class);
		TradeRefundStatisticsJobDispatchPublisher tradeRefundStatisticsJobDispatchPublisher =
				mock(TradeRefundStatisticsJobDispatchPublisher.class);
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		OrderProcessLogPublishPort dispatchStackOrderProcessLogPublishPort =
				new OrderProcessLogPublishPortImpl(dispatchFacade);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);

		WxpaySecapiAftersalesRefundRunner wx = mock(WxpaySecapiAftersalesRefundRunner.class);
		AlipayTradeAftersalesRefundRunner alipay = mock(AlipayTradeAftersalesRefundRunner.class);
		BsPayAftersalesOnlineRefundRunner bs = mock(BsPayAftersalesOnlineRefundRunner.class);
		PaymentSubjectDistributorIdPort paymentSubjectDistributorIdPort =
				mock(PaymentSubjectDistributorIdPort.class);
		when(paymentSubjectDistributorIdPort.resolveActualDistributorId(anyLong(), anyLong()))
				.thenAnswer(inv -> (Long) inv.getArgument(1));
		StandardOnlinePayAftersalesRefundExecutor channelExecutor =
				new StandardOnlinePayAftersalesRefundExecutor(wx, alipay, bs, dispatchStackOrderProcessLogPublishPort, paymentSubjectDistributorIdPort);

		OrdersRefundPaymentDispatchService ordersRefundPaymentDispatchService =
				new OrdersRefundPaymentDispatchService(
						List.of(channelExecutor),
						pointMemberAddPointService,
						mock(OrdersDepositOrderRefundService.class),
						dispatchStackOrderProcessLogPublishPort,
						mock(NormalOrdersMapper.class));

		String err = "余额不足";
		Map<String, Object> fail = new LinkedHashMap<>();
		fail.put("status", "FAIL");
		fail.put("error_desc", err);
		when(wx.refund(anyLong(), anyLong(), anyString(), anyString(), anyLong(), anyInt(), anyInt()))
				.thenReturn(fail);

		long companyId = 91562L;
		long refundBn = 51562L;
		long orderId = 61562L;
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
		refundBefore.setTradeId("tr-wxpay-resubmit-2");
		refundBefore.setPayType("wxpayjs");
		refundBefore.setRefundFee(50);
		refundBefore.setFreightType("cash");
		refundBefore.setFreight(0);
		refundBefore.setRefundPoint(0);
		refundBefore.setUserId(2L);
		refundBefore.setAftersalesBn(0L);
		refundBefore.setDistributorId(702L);

		AftersalesRefund refundAfter = new AftersalesRefund();
		refundAfter.setCompanyId(companyId);
		refundAfter.setRefundBn(refundBn);
		refundAfter.setOrderId(orderId);
		refundAfter.setTradeId("tr-wxpay-resubmit-2");
		refundAfter.setPayType("wxpayjs");
		refundAfter.setRefundFee(50);
		refundAfter.setRefundStatus("CHANGE");
		refundAfter.setDistributorId(702L);

		AtomicInteger refundSelectCalls = new AtomicInteger(0);
		when(aftersalesRefundMapper.selectOne(any()))
				.thenAnswer(inv -> refundSelectCalls.incrementAndGet() == 1 ? refundBefore : refundAfter);

		Trade trade = new Trade();
		trade.setCompanyId(String.valueOf(companyId));
		trade.setTradeId("tr-wxpay-resubmit-2");
		trade.setTradeState("SUCCESS");
		trade.setPayFee(100);
		trade.setPayChannel("wxpay");
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
						mock(TradeRefundFinishEventDispatchPublisher.class),
					mock(RefundErrorLogsRecorder.class),
					org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class),
					org.mockito.Mockito.mock(cn.shopex.ecshopx.aftersales.support.EmployeePurchasePrepaidRefundLinesResolver.class));

		RefundErrorLogsService refundErrorLogsService =
				new RefundErrorLogsService(
						refundErrorLogsMapper, aftersalesRefundDoRefundService, new ObjectMapper());

		refundErrorLogsService.resubmitRefund(LOG_ID_FAIL);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));
		Map<String, Object> payload = payloadCaptor.getValue();
		assertEquals(orderId, ((Number) payload.get("order_id")).longValue());
		assertEquals(companyId, ((Number) payload.get("company_id")).longValue());
		assertEquals("system", payload.get("operator_type"));
		assertEquals("订单退款", payload.get("remarks"));
		assertEquals(
				"订单号：" + orderId + "，订单退款失败（微信支付渠道），失败原因：" + err,
				String.valueOf(payload.get("detail")));
		verify(orderProcessLogPublishPort, never()).publish(any());
	}
}
