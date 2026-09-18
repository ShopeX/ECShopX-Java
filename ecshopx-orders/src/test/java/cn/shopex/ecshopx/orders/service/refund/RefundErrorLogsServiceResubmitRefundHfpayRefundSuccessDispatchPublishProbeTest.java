package cn.shopex.ecshopx.orders.service.refund;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.common.dispatch.TradeRefundFinishEventDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeRefundStatisticsJobDispatchPublisher;
import cn.shopex.ecshopx.common.port.hfpay.HfpayRefundSuccessEventPublishPort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayAcouJsonPostClient;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayOrderApplyIdGenerator;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayPaymentSettingService;
import cn.shopex.ecshopx.hfpay.service.refund.HfpayAftersalesRefundPayExecutor;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.RefundErrorLogs;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.RefundErrorLogsMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("event:294: resubmitRefund/hfpay HfpayRefundSuccessEventPublishPort probe")
class RefundErrorLogsServiceResubmitRefundHfpayRefundSuccessDispatchPublishProbeTest {

	private static final String LOG_ID_HFPAY = "9027";

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), AftersalesRefund.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Trade.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrdersItems.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), RefundErrorLogs.class);
	}

	@Test
	void resubmitRefund_hfpay_gatewaySuccess_invokesPublishPortOnce_withOrderIdAndRefundBn() {
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
		NormalOrdersMapper normalOrdersMapper = mock(NormalOrdersMapper.class);

		HfpayRefundSuccessEventPublishPort publishPort = mock(HfpayRefundSuccessEventPublishPort.class);
		HfPayAcouJsonPostClient acouJsonPostClient = mock(HfPayAcouJsonPostClient.class);
		HfPayPaymentSettingService paymentSettingService = mock(HfPayPaymentSettingService.class);

		Map<String, Object> setting = new LinkedHashMap<>();
		setting.put("mer_cust_id", "M1");
		Map<String, Object> gateway = new LinkedHashMap<>();
		gateway.put("resp_code", "C00002");
		gateway.put("order_id", "gw-hfpay-resubmit-1");

		long companyId = 91571L;
		long refundBn = 51571L;
		long orderId = 61571L;
		long id = Long.parseLong(LOG_ID_HFPAY);

		when(paymentSettingService.loadForCompany(companyId)).thenReturn(setting);
		when(acouJsonPostClient.reb001(eq(setting), any())).thenReturn(gateway);

		HfpayAftersalesRefundPayExecutor hfpayExecutor =
				new HfpayAftersalesRefundPayExecutor(publishPort, acouJsonPostClient, paymentSettingService);
		OrdersRefundPaymentDispatchService ordersRefundPaymentDispatchService =
				new OrdersRefundPaymentDispatchService(
						List.of(hfpayExecutor),
						pointMemberAddPointService,
						mock(OrdersDepositOrderRefundService.class),
						orderProcessLogPublishPort,
						normalOrdersMapper);

		NormalOrders orderRow = new NormalOrders();
		orderRow.setOrderId(orderId);
		orderRow.setCompanyId(companyId);
		orderRow.setCreateTime(1_704_067_200);
		orderRow.setIsProfitsharing(0);
		when(normalOrdersMapper.selectOne(any())).thenReturn(orderRow);

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
		refundBefore.setTradeId("tr-hfpay-resubmit-1");
		refundBefore.setPayType("hfpay");
		refundBefore.setRefundFee(100);
		refundBefore.setFreightType("cash");
		refundBefore.setFreight(0);
		refundBefore.setRefundPoint(0);
		refundBefore.setUserId(1L);
		refundBefore.setAftersalesBn(0L);
		refundBefore.setDistributorId(701L);
		refundBefore.setHfOrderId("hfoid-resubmit-1");
		refundBefore.setCreateTime(1_704_153_600);

		AftersalesRefund refundAfter = new AftersalesRefund();
		refundAfter.setCompanyId(companyId);
		refundAfter.setRefundBn(refundBn);
		refundAfter.setOrderId(orderId);
		refundAfter.setTradeId("tr-hfpay-resubmit-1");
		refundAfter.setPayType("hfpay");
		refundAfter.setRefundFee(100);
		refundAfter.setRefundStatus("SUCCESS");
		refundAfter.setRefundId("gw-hfpay-resubmit-1");
		refundAfter.setAftersalesBn(0L);
		refundAfter.setRefundedFee(100);
		refundAfter.setDistributorId(701L);
		refundAfter.setHfOrderId("hfoid-resubmit-1");
		refundAfter.setCreateTime(1_704_153_600);

		AtomicInteger refundSelectCalls = new AtomicInteger(0);
		when(aftersalesRefundMapper.selectOne(any()))
				.thenAnswer(inv -> refundSelectCalls.incrementAndGet() == 1 ? refundBefore : refundAfter);

		Trade trade = new Trade();
		trade.setCompanyId(String.valueOf(companyId));
		trade.setTradeId("tr-hfpay-resubmit-1");
		trade.setTradeState("SUCCESS");
		trade.setPayFee(200);
		trade.setPayChannel("hfpay");
		when(tradeMapper.selectOne(any())).thenReturn(trade);

		when(normalOrdersItemsMapper.selectList(any())).thenReturn(Collections.emptyList());
		when(aftersalesRefundMapper.update(isNull(), any())).thenReturn(1);

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

		refundErrorLogsService.resubmitRefund(LOG_ID_HFPAY);

		verify(publishPort, times(1)).publishSyncOnGatewayRefundSuccess(eq(String.valueOf(orderId)), eq(refundBn));
	}
}
