package cn.shopex.ecshopx.orders.service.refund;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.dispatch.TradeRefundFinishEventDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeRefundStatisticsJobDispatchPublisher;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.integration.OrderProcessLogPublishPortImpl;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayOrderApplyIdGenerator;
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

/**
 * Aligns {@code detail} and five keys with {@link
 * cn.shopex.ecshopx.chinaumspay.service.refund.ChinaumsAftersalesRefundPayExecutor} templates (same
 * literals as that executor's private helpers).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EVENT_ORDER_PROCESS_LOG: resubmitRefund/chinaums publishEvent probe")
class RefundErrorLogsServiceResubmitRefundChinaumsOrderProcessLogDispatchPublishProbeTest {

	private static final String LOG_ID = "8901";

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), AftersalesRefund.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Trade.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrdersItems.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), RefundErrorLogs.class);
	}

	@Test
	void resubmitRefund_chinaums_success_invokesPublishEventOnce_withUnionPayAlignedDetail() {
		RefundErrorLogsMapper refundErrorLogsMapper = mock(RefundErrorLogsMapper.class);
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

		long companyId = 813L;
		long refundBn = 43013L;
		long orderId = 91013L;
		long id = Long.parseLong(LOG_ID);

		RefundErrorLogs initialLog = new RefundErrorLogs();
		initialLog.setId(id);
		initialLog.setDataJson(
				"{\"refund_bn\":" + refundBn + ",\"company_id\":" + companyId + "}");

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
		refundBefore.setTradeId("tr-chinaums-resubmit-1");
		refundBefore.setPayType("chinaums");
		refundBefore.setRefundFee(100);
		refundBefore.setFreightType("cash");
		refundBefore.setFreight(0);
		refundBefore.setRefundPoint(0);
		refundBefore.setUserId(1L);
		refundBefore.setAftersalesBn(700L);

		AftersalesRefund refundAfter = new AftersalesRefund();
		refundAfter.setCompanyId(companyId);
		refundAfter.setRefundBn(refundBn);
		refundAfter.setOrderId(orderId);
		refundAfter.setTradeId("tr-chinaums-resubmit-1");
		refundAfter.setPayType("chinaums");
		refundAfter.setRefundFee(100);
		refundAfter.setRefundStatus("SUCCESS");
		refundAfter.setRefundId("rid-chinaums-resubmit-1");
		refundAfter.setAftersalesBn(700L);
		refundAfter.setRefundedFee(100);

		AtomicInteger refundSelectCalls = new AtomicInteger(0);
		when(aftersalesRefundMapper.selectOne(any()))
				.thenAnswer(inv -> refundSelectCalls.incrementAndGet() == 1 ? refundBefore : refundAfter);

		Trade trade = new Trade();
		trade.setCompanyId(String.valueOf(companyId));
		trade.setTradeId("tr-chinaums-resubmit-1");
		trade.setTradeState("SUCCESS");
		trade.setPayFee(200);
		when(tradeMapper.selectOne(any())).thenReturn(trade);

		when(ordersRefundPaymentDispatchService.dispatch(
						eq(companyId),
						any(),
						any(AftersalesRefund.class),
						any(Trade.class),
						eq(100),
						eq(0),
						eq(true)))
				.thenAnswer(
						inv -> {
							Map<String, Object> entities = new LinkedHashMap<>();
							entities.put("order_id", orderId);
							entities.put("company_id", companyId);
							entities.put("operator_type", "system");
							entities.put("remarks", "订单退款");
							entities.put(
									"detail",
									"订单号：" + orderId + "，订单退款成功（银联支付渠道）");
							orderProcessLogPublishPort.publish(entities);
							Map<String, Object> payRes = new LinkedHashMap<>();
							payRes.put("status", "SUCCESS");
							payRes.put("refund_id", "rid-chinaums-resubmit-1");
							return payRes;
						});

		when(aftersalesDetailMapper.selectList(any())).thenReturn(Collections.emptyList());

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

		ObjectMapper objectMapper = new ObjectMapper();
		RefundErrorLogsService refundErrorLogsService =
				new RefundErrorLogsService(
						refundErrorLogsMapper, aftersalesRefundDoRefundService, objectMapper);

		refundErrorLogsService.resubmitRefund(LOG_ID);

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
				"订单号：" + orderId + "，订单退款成功（银联支付渠道）",
				String.valueOf(payload.get("detail")));
	}

	@Test
	void resubmitRefund_chinaums_fail_invokesPublishEventOnce_withUnionPayAlignedDetail() {
		RefundErrorLogsMapper refundErrorLogsMapper = mock(RefundErrorLogsMapper.class);
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

		long companyId = 814L;
		long refundBn = 43014L;
		long orderId = 91014L;
		long id = 8902L;
		String errMsg = "网关拒绝（resubmit errCode）";

		RefundErrorLogs initialLog = new RefundErrorLogs();
		initialLog.setId(id);
		initialLog.setDataJson(
				"{\"refund_bn\":" + refundBn + ",\"company_id\":" + companyId + "}");

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
		refundBefore.setTradeId("tr-chinaums-resubmit-2");
		refundBefore.setPayType("chinaums");
		refundBefore.setRefundFee(50);
		refundBefore.setFreightType("cash");
		refundBefore.setFreight(0);
		refundBefore.setRefundPoint(0);
		refundBefore.setUserId(2L);
		refundBefore.setAftersalesBn(701L);

		AftersalesRefund refundAfter = new AftersalesRefund();
		refundAfter.setCompanyId(companyId);
		refundAfter.setRefundBn(refundBn);
		refundAfter.setOrderId(orderId);
		refundAfter.setTradeId("tr-chinaums-resubmit-2");
		refundAfter.setPayType("chinaums");
		refundAfter.setRefundFee(50);
		refundAfter.setRefundStatus("FAIL");

		AtomicInteger refundSelectCalls = new AtomicInteger(0);
		when(aftersalesRefundMapper.selectOne(any()))
				.thenAnswer(inv -> refundSelectCalls.incrementAndGet() == 1 ? refundBefore : refundAfter);

		Trade trade = new Trade();
		trade.setCompanyId(String.valueOf(companyId));
		trade.setTradeId("tr-chinaums-resubmit-2");
		trade.setTradeState("SUCCESS");
		trade.setPayFee(100);
		when(tradeMapper.selectOne(any())).thenReturn(trade);

		when(ordersRefundPaymentDispatchService.dispatch(
						eq(companyId),
						any(),
						any(AftersalesRefund.class),
						any(Trade.class),
						eq(50),
						eq(0),
						eq(true)))
				.thenAnswer(
						inv -> {
							Map<String, Object> entities = new LinkedHashMap<>();
							entities.put("order_id", orderId);
							entities.put("company_id", companyId);
							entities.put("operator_type", "system");
							entities.put("remarks", "订单退款");
							entities.put(
									"detail",
									"订单号：" + orderId + "，订单退款失败（银联支付渠道），失败原因：" + errMsg);
							orderProcessLogPublishPort.publish(entities);
							Map<String, Object> payRes = new LinkedHashMap<>();
							payRes.put("status", "FAIL");
							payRes.put("error_code", "TIMEOUT");
							payRes.put("error_desc", errMsg);
							return payRes;
						});

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

		ObjectMapper objectMapper = new ObjectMapper();
		RefundErrorLogsService refundErrorLogsService =
				new RefundErrorLogsService(
						refundErrorLogsMapper, aftersalesRefundDoRefundService, objectMapper);

		refundErrorLogsService.resubmitRefund("8902");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));
		Map<String, Object> payload = payloadCaptor.getValue();
		assertEquals(
				"订单号：" + orderId + "，订单退款失败（银联支付渠道），失败原因：" + errMsg,
				String.valueOf(payload.get("detail")));
	}
}
