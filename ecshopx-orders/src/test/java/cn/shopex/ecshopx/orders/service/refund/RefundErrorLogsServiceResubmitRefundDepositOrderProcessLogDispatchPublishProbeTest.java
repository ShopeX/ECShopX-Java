package cn.shopex.ecshopx.orders.service.refund;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("EVENT_ORDER_PROCESS_LOG: resubmitRefund/deposit publishEvent probe")
class RefundErrorLogsServiceResubmitRefundDepositOrderProcessLogDispatchPublishProbeTest {

	private static final String LOG_ID_SUCCESS = "9012";

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), AftersalesRefund.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Trade.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrdersItems.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), RefundErrorLogs.class);
	}

	@Test
	void resubmitRefund_deposit_success_invokesPublishEventOnce_withDispatchServiceAlignedDetail() {
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
		OrderProcessLogPublishPort orderProcessLogPublishPort =
				mock(OrderProcessLogPublishPort.class);

		OrdersRefundPaymentDispatchService ordersRefundPaymentDispatchService =
				new OrdersRefundPaymentDispatchService(
						Collections.emptyList(),
						pointMemberAddPointService,
						mock(OrdersDepositOrderRefundService.class),
						dispatchStackOrderProcessLogPublishPort, mock(NormalOrdersMapper.class));

		long companyId = 91531L;
		long refundBn = 51531L;
		long orderId = 61531L;
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
		refundBefore.setTradeId("tr-deposit-resubmit-1");
		refundBefore.setPayType("deposit");
		refundBefore.setRefundFee(100);
		refundBefore.setFreightType("cash");
		refundBefore.setFreight(0);
		refundBefore.setRefundPoint(0);
		refundBefore.setUserId(1L);
		refundBefore.setAftersalesBn(0L);

		AftersalesRefund refundAfter = new AftersalesRefund();
		refundAfter.setCompanyId(companyId);
		refundAfter.setRefundBn(refundBn);
		refundAfter.setOrderId(orderId);
		refundAfter.setTradeId("tr-deposit-resubmit-1");
		refundAfter.setPayType("deposit");
		refundAfter.setRefundFee(100);
		refundAfter.setRefundStatus("SUCCESS");
		refundAfter.setRefundId(String.valueOf(refundBn));
		refundAfter.setAftersalesBn(0L);
		refundAfter.setRefundedFee(100);
		refundAfter.setRefundedPoint(0);

		AtomicInteger refundSelectCalls = new AtomicInteger(0);
		when(aftersalesRefundMapper.selectOne(any()))
				.thenAnswer(inv -> refundSelectCalls.incrementAndGet() == 1 ? refundBefore : refundAfter);

		Trade trade = new Trade();
		trade.setCompanyId(String.valueOf(companyId));
		trade.setTradeId("tr-deposit-resubmit-1");
		trade.setTradeState("SUCCESS");
		trade.setPayFee(100);
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

		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						argThat(
								(Map<String, Object> m) ->
										m != null
												&& Objects.equals(m.get("order_id"), orderId)
												&& Objects.equals(((Number) m.get("company_id")).longValue(), companyId)
												&& "system".equals(m.get("operator_type"))
												&& "订单退款".equals(m.get("remarks"))
												&& ("订单号：" + orderId + "，订单退款成功（储值金额渠道)")
														.equals(String.valueOf(m.get("detail")))),
						eq(DispatchOptions.oplQueuedAfterCommit()));
		verify(orderProcessLogPublishPort, never()).publish(any());
	}
}
