package cn.shopex.ecshopx.orders.service.refund;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.common.dispatch.TradeRefundFinishEventDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeRefundStatisticsJobDispatchPublisher;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AftersalesRefundDoRefundServiceResubmitTradeStatisticsPublishTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), AftersalesRefund.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Trade.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrdersItems.class);
	}

	@Test
	void doRefund_whenResubmitTrue_andPaymentSuccess_publishesTradeRefundStatisticsOnce() {
		OrdersRefundPaymentDispatchService ordersRefundPaymentDispatchService = mock(OrdersRefundPaymentDispatchService.class);
		AftersalesRefundMapper aftersalesRefundMapper = mock(AftersalesRefundMapper.class);
		TradeMapper tradeMapper = mock(TradeMapper.class);
		NormalOrdersItemsMapper normalOrdersItemsMapper = mock(NormalOrdersItemsMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		PointMemberAddPointService pointMemberAddPointService = mock(PointMemberAddPointService.class);
		TradeRefundStatisticsJobDispatchPublisher tradeRefundStatisticsJobPublishMock =
				mock(TradeRefundStatisticsJobDispatchPublisher.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);

		long companyId = 7L;
		long refundBn = 42L;

		AftersalesRefund refundBefore = new AftersalesRefund();
		refundBefore.setCompanyId(companyId);
		refundBefore.setRefundBn(refundBn);
		refundBefore.setOrderId(9001L);
		refundBefore.setTradeId("tr-1");
		refundBefore.setPayType("wxpay");
		refundBefore.setRefundFee(100);
		refundBefore.setFreightType("cash");
		refundBefore.setFreight(0);
		refundBefore.setRefundPoint(50);
		refundBefore.setUserId(1L);
		refundBefore.setAftersalesBn(null);

		AftersalesRefund refundAfter = new AftersalesRefund();
		refundAfter.setCompanyId(companyId);
		refundAfter.setRefundBn(refundBn);
		refundAfter.setOrderId(9001L);
		refundAfter.setTradeId("tr-1");
		refundAfter.setPayType("wxpay");
		refundAfter.setRefundFee(100);
		refundAfter.setRefundStatus("SUCCESS");
		refundAfter.setRefundId("rid-1");

		AtomicInteger selectCalls = new AtomicInteger(0);
		when(aftersalesRefundMapper.selectOne(any())).thenAnswer(inv -> selectCalls.incrementAndGet() == 1 ? refundBefore : refundAfter);

		Trade trade = new Trade();
		trade.setCompanyId(String.valueOf(companyId));
		trade.setTradeId("tr-1");
		trade.setTradeState("SUCCESS");
		trade.setPayFee(200);
		trade.setDistributorId("5");
		trade.setMerchantId(8L);
		when(tradeMapper.selectOne(any())).thenReturn(trade);

		Map<String, Object> payRes = new LinkedHashMap<>();
		payRes.put("status", "SUCCESS");
		payRes.put("refund_id", "rid-1");
		when(ordersRefundPaymentDispatchService.dispatch(
						eq(companyId),
						any(),
						any(AftersalesRefund.class),
						any(Trade.class),
						eq(100),
						eq(50),
						eq(true)))
				.thenReturn(payRes);

		when(normalOrdersItemsMapper.selectList(any())).thenReturn(Collections.emptyList());

		AftersalesRefundDoRefundService service =
				new AftersalesRefundDoRefundService(ordersRefundPaymentDispatchService,
						aftersalesRefundMapper,
						tradeMapper,
						normalOrdersItemsMapper,
						aftersalesDetailMapper,
						applicationEventPublisher,
						pointMemberAddPointService,
						tradeRefundStatisticsJobPublishMock,
						orderProcessLogPublishPort,
						mock(HfPayOrderApplyIdGenerator.class),
						mock(TradeRefundFinishEventDispatchPublisher.class),
					mock(RefundErrorLogsRecorder.class),
					org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class),
					org.mockito.Mockito.mock(cn.shopex.ecshopx.aftersales.support.EmployeePurchasePrepaidRefundLinesResolver.class));

		service.doRefund(companyId, refundBn, true);

		verify(orderProcessLogPublishPort, never()).publish(any());
		verify(tradeRefundStatisticsJobPublishMock, times(1)).publish(any());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(tradeRefundStatisticsJobPublishMock).publish(payloadCaptor.capture());
		Map<String, Object> stats = payloadCaptor.getValue();
		assertEquals(companyId, stats.get("company_id"));
		assertEquals(9001L, stats.get("order_id"));
		assertEquals(refundBn, stats.get("refund_bn"));
		assertEquals("tr-1", stats.get("trade_id"));
		assertEquals("wxpay", stats.get("pay_type"));
		assertEquals(100, stats.get("refund_fee"));
		assertEquals(200, stats.get("pay_fee"));
		assertEquals("5", stats.get("distributor_id"));
		assertEquals(8L, stats.get("merchant_id"));

		verify(pointMemberAddPointService, never())
				.addPointForAftersalesRefund(anyLong(), anyLong(), anyInt(), anyLong(), anyLong(), any());
	}
}
