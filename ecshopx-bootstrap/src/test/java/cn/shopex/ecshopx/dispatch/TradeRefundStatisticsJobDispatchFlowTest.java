package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.common.dispatch.OrdersDispatchJobNames;
import cn.shopex.ecshopx.common.dispatch.TradeRefundFinishEventDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeRefundStatisticsJobDispatchPublisher;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayOrderApplyIdGenerator;
import cn.shopex.ecshopx.orders.dispatch.TradeRefundStatisticsJobHandler;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.refund.AftersalesRefundDoRefundService;
import cn.shopex.ecshopx.orders.service.refund.OrdersRefundPaymentDispatchService;
import cn.shopex.ecshopx.orders.service.refund.RefundErrorLogsRecorder;
import cn.shopex.ecshopx.orders.service.statistics.TradeRefundOrderPayStatisticsService;
import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

class TradeRefundStatisticsJobDispatchFlowTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), AftersalesRefund.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Trade.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrdersItems.class);
	}

	@Test
	@DisplayName("dispatchJob enqueues slow queue with 5s delay and consumer invokes recordRefundStatistics")
	void dispatchJob_withDelay_enqueuesSlow_andConsumerInvokesRecordRefundStatistics() {
		TradeRefundOrderPayStatisticsService statisticsService = Mockito.mock(TradeRefundOrderPayStatisticsService.class);
		TradeRefundStatisticsJobHandler handler = new TradeRefundStatisticsJobHandler(statisticsService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(OrdersDispatchJobNames.TRADE_REFUND_STATISTICS_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new SyncDispatchDriver(registry),
						java.util.Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 7L);
		payload.put("order_id", 9001L);
		payload.put("refund_bn", 42L);
		payload.put("trade_id", "tr-1");
		payload.put("refund_fee", 100);
		payload.put("pay_fee", 200);
		facade.dispatchJob(
				OrdersDispatchJobNames.TRADE_REFUND_STATISTICS_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						Duration.ofSeconds(5),
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("slow", msg.queue());
		assertEquals(Duration.ofSeconds(5), msg.delay());
		assertEquals(OrdersDispatchJobNames.TRADE_REFUND_STATISTICS_JOB, msg.messageName());
		assertEquals(7L, msg.payload().get("company_id"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						Mockito.mock(FailedJobRecorder.class),
						Mockito.mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		Mockito.verify(statisticsService).recordRefundStatistics(captor.capture());
		assertEquals(7L, captor.getValue().get("company_id"));
		assertEquals(9001L, captor.getValue().get("order_id"));
		assertEquals(100, captor.getValue().get("refund_fee"));
	}

	@Test
	void dispatchJob_payloadRetainsSnakeCaseKeys() {
		TradeRefundOrderPayStatisticsService statisticsService = Mockito.mock(TradeRefundOrderPayStatisticsService.class);
		TradeRefundStatisticsJobHandler handler = new TradeRefundStatisticsJobHandler(statisticsService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(OrdersDispatchJobNames.TRADE_REFUND_STATISTICS_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new SyncDispatchDriver(registry),
						java.util.Map.of(DispatchDriverType.REDIS, captured::add));
		new DispatchFacade(core, new DispatchFanOutPlanner(registry))
				.dispatchJob(
						OrdersDispatchJobNames.TRADE_REFUND_STATISTICS_JOB,
						Map.of(
								"company_id",
								1L,
								"order_id",
								2L,
								"refund_bn",
								3L,
								"trade_id",
								"tid",
								"pay_type",
								"wxpay",
								"refund_fee",
								10,
								"pay_fee",
								99,
								"distributor_id",
								"5",
								"merchant_id",
								8L),
						new DispatchOptions(
								DispatchMode.ASYNC,
								DispatchDriverType.REDIS,
								"slow",
								Duration.ofSeconds(5),
								RetryPolicy.platformDefault()));

		DispatchMessage msg = captured.get(0);
		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						Mockito.mock(FailedJobRecorder.class),
						Mockito.mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		Mockito.verify(statisticsService).recordRefundStatistics(captor.capture());
		Map<String, Object> p = captor.getValue();
		assertTrue(p.containsKey("company_id"));
		assertTrue(p.containsKey("order_id"));
		assertTrue(p.containsKey("refund_bn"));
		assertTrue(p.containsKey("trade_id"));
		assertTrue(p.containsKey("pay_type"));
		assertTrue(p.containsKey("refund_fee"));
		assertTrue(p.containsKey("pay_fee"));
		assertTrue(p.containsKey("distributor_id"));
		assertTrue(p.containsKey("merchant_id"));
	}

	@Test
	@DisplayName(
			"resubmit doRefund success: captured publish payload dispatches, consumer runs handler and recordRefundStatistics")
	void resubmitDoRefund_success_capturedPublishPayload_dispatchJob_enqueue_andConsumerInvokesRecordRefundStatistics() {
		long companyId = 7L;
		long refundBn = 42L;
		Map<String, Object> payload = capturePublishedPayloadFromResubmitSuccessDoRefund(companyId, refundBn);

		TradeRefundOrderPayStatisticsService statisticsService = Mockito.mock(TradeRefundOrderPayStatisticsService.class);
		TradeRefundStatisticsJobHandler handler = new TradeRefundStatisticsJobHandler(statisticsService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(OrdersDispatchJobNames.TRADE_REFUND_STATISTICS_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new SyncDispatchDriver(registry),
						java.util.Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		facade.dispatchJob(
				OrdersDispatchJobNames.TRADE_REFUND_STATISTICS_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						Duration.ofSeconds(5),
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						Mockito.mock(FailedJobRecorder.class),
						Mockito.mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		Mockito.verify(statisticsService).recordRefundStatistics(captor.capture());
		Map<String, Object> recorded = captor.getValue();
		assertEquals(payload.get("company_id"), recorded.get("company_id"));
		assertEquals(payload.get("order_id"), recorded.get("order_id"));
		assertEquals(payload.get("refund_bn"), recorded.get("refund_bn"));
		assertEquals(payload.get("trade_id"), recorded.get("trade_id"));
		assertEquals(payload.get("refund_fee"), recorded.get("refund_fee"));
	}

	private Map<String, Object> capturePublishedPayloadFromResubmitSuccessDoRefund(long companyId, long refundBn) {
		OrdersRefundPaymentDispatchService ordersRefundPaymentDispatchService = Mockito.mock(OrdersRefundPaymentDispatchService.class);
		AftersalesRefundMapper aftersalesRefundMapper = Mockito.mock(AftersalesRefundMapper.class);
		TradeMapper tradeMapper = Mockito.mock(TradeMapper.class);
		NormalOrdersItemsMapper normalOrdersItemsMapper = Mockito.mock(NormalOrdersItemsMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = Mockito.mock(AftersalesDetailMapper.class);
		ApplicationEventPublisher applicationEventPublisher = Mockito.mock(ApplicationEventPublisher.class);
		PointMemberAddPointService pointMemberAddPointService = Mockito.mock(PointMemberAddPointService.class);
		TradeRefundStatisticsJobDispatchPublisher tradeRefundStatisticsJobDispatchPublisher =
				Mockito.mock(TradeRefundStatisticsJobDispatchPublisher.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = Mockito.mock(OrderProcessLogPublishPort.class);

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
						tradeRefundStatisticsJobDispatchPublisher,
						orderProcessLogPublishPort,
					Mockito.mock(HfPayOrderApplyIdGenerator.class),
					Mockito.mock(TradeRefundFinishEventDispatchPublisher.class),
					Mockito.mock(RefundErrorLogsRecorder.class),
					org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class),
					org.mockito.Mockito.mock(cn.shopex.ecshopx.aftersales.support.EmployeePurchasePrepaidRefundLinesResolver.class));

		service.doRefund(companyId, refundBn, true);

		verify(orderProcessLogPublishPort, never()).publish(any());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> publishCaptor = ArgumentCaptor.forClass(Map.class);
		Mockito.verify(tradeRefundStatisticsJobDispatchPublisher).publish(publishCaptor.capture());
		return publishCaptor.getValue();
	}
}
