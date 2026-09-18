package cn.shopex.ecshopx.orders.service.refund;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class AftersalesRefundDoRefundServiceRefundErrorLogsRecorderProbeTest {

	private static final long COMPANY_ID = 1L;
	private static final long REFUND_BN = 9001L;
	private static final long ORDER_ID = 8001L;

	private OrdersRefundPaymentDispatchService ordersRefundPaymentDispatchService;
	private AftersalesRefundMapper aftersalesRefundMapper;
	private TradeMapper tradeMapper;
	private RefundErrorLogsRecorder refundErrorLogsRecorder;
	private AftersalesRefundDoRefundService service;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), AftersalesRefund.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Trade.class);
	}

	@BeforeEach
	void setUp() {
		ordersRefundPaymentDispatchService = mock(OrdersRefundPaymentDispatchService.class);
		aftersalesRefundMapper = mock(AftersalesRefundMapper.class);
		tradeMapper = mock(TradeMapper.class);
		refundErrorLogsRecorder = mock(RefundErrorLogsRecorder.class);
		service =
				new AftersalesRefundDoRefundService(ordersRefundPaymentDispatchService,
						aftersalesRefundMapper,
						tradeMapper,
						mock(NormalOrdersItemsMapper.class),
						mock(AftersalesDetailMapper.class),
						mock(ApplicationEventPublisher.class),
						mock(PointMemberAddPointService.class),
						mock(TradeRefundStatisticsJobDispatchPublisher.class),
						mock(OrderProcessLogPublishPort.class),
						mock(HfPayOrderApplyIdGenerator.class),
						mock(TradeRefundFinishEventDispatchPublisher.class),
						refundErrorLogsRecorder,
					org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class),
					org.mockito.Mockito.mock(cn.shopex.ecshopx.aftersales.support.EmployeePurchasePrepaidRefundLinesResolver.class));

		AftersalesRefund refund = new AftersalesRefund();
		refund.setCompanyId(COMPANY_ID);
		refund.setRefundBn(REFUND_BN);
		refund.setOrderId(ORDER_ID);
		refund.setTradeId("tr-1");
		refund.setPayType("alipay");
		refund.setRefundFee(100);
		refund.setRefundPoint(0);
		refund.setFreightType("cash");
		refund.setFreight(0);
		refund.setUserId(10L);
		refund.setRefundStatus("AUDIT_SUCCESS");

		AftersalesRefund afterUpdate = new AftersalesRefund();
		afterUpdate.setCompanyId(COMPANY_ID);
		afterUpdate.setRefundBn(REFUND_BN);
		afterUpdate.setOrderId(ORDER_ID);
		afterUpdate.setTradeId("tr-1");
		afterUpdate.setPayType("alipay");
		afterUpdate.setRefundStatus("CHANGE");
		afterUpdate.setRefundFee(100);
		afterUpdate.setAftersalesBn(0L);

		when(aftersalesRefundMapper.selectOne(any(LambdaQueryWrapper.class)))
				.thenReturn(refund, afterUpdate);

		Trade trade = new Trade();
		trade.setCompanyId(String.valueOf(COMPANY_ID));
		trade.setTradeId("tr-1");
		trade.setTradeState("SUCCESS");
		trade.setPayFee(100);
		trade.setWxaAppid("wxa-test");
		when(tradeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(trade);
		when(aftersalesRefundMapper.update(any(), any())).thenReturn(1);
	}

	@Test
	void doRefund_gatewayFail_invokesRefundErrorLogsRecorderOnce() {
		Map<String, Object> fail = new LinkedHashMap<>();
		fail.put("status", "FAIL");
		fail.put("error_code", "E001");
		fail.put("error_desc", "gateway rejected");
		when(ordersRefundPaymentDispatchService.dispatch(
						eq(COMPANY_ID),
						eq("wxa-test"),
						any(AftersalesRefund.class),
						any(Trade.class),
						eq(100),
						eq(0),
						eq(false)))
				.thenReturn(fail);

		service.doRefund(COMPANY_ID, REFUND_BN, false);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
		verify(refundErrorLogsRecorder, times(1))
				.saveRefundError(
						eq(COMPANY_ID),
						eq("wxa-test"),
						payloadCaptor.capture(),
						resultCaptor.capture());
		Map<String, Object> payload = payloadCaptor.getValue();
		assert payload.get("refund_bn").equals(REFUND_BN);
		assert payload.get("company_id").equals(COMPANY_ID);
		assert resultCaptor.getValue().get("status").equals("FAIL");
	}
}
