package cn.shopex.ecshopx.payment.service.orderrefund;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.payment.PaymentSubjectDistributorIdPort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.refund.AftersalesRefundPaymentContext;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.integration.OrderProcessLogPublishPortImpl;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("EVENT_ORDER_PROCESS_LOG: RefundJob/alipay executor publishEvent probe")
class StandardOnlinePayAftersalesRefundExecutorAlipayOrderProcessLogDispatchPublishProbeTest {

	@Test
	void execute_alipay_success_invokesPublishEventOnce_withRefundSuccessDetail() {
		WxpaySecapiAftersalesRefundRunner wx = mock(WxpaySecapiAftersalesRefundRunner.class);
		AlipayTradeAftersalesRefundRunner alipay = mock(AlipayTradeAftersalesRefundRunner.class);
		BsPayAftersalesOnlineRefundRunner bs = mock(BsPayAftersalesOnlineRefundRunner.class);
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = new OrderProcessLogPublishPortImpl(dispatchFacade);

		Map<String, Object> ok = new LinkedHashMap<>();
		ok.put("status", "SUCCESS");
		ok.put("refund_id", "ali-trade-1");
		when(alipay.refund(anyLong(), anyLong(), anyString(), anyLong(), anyInt(), anyInt())).thenReturn(ok);

		PaymentSubjectDistributorIdPort paymentSubjectDistributorIdPort =
				mock(PaymentSubjectDistributorIdPort.class);
		when(paymentSubjectDistributorIdPort.resolveActualDistributorId(anyLong(), anyLong()))
				.thenAnswer(inv -> (Long) inv.getArgument(1));
		StandardOnlinePayAftersalesRefundExecutor executor =
				new StandardOnlinePayAftersalesRefundExecutor(wx, alipay, bs, orderProcessLogPublishPort, paymentSubjectDistributorIdPort);


		AftersalesRefundPaymentContext ctx = alipayRefundContext(88011L, 77011L);
		Map<String, Object> payRes = executor.execute(ctx);
		assertEquals("SUCCESS", String.valueOf(payRes.get("status")));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));
		Map<String, Object> payload = payloadCaptor.getValue();
		assertEquals(77011L, ((Number) payload.get("order_id")).longValue());
		assertEquals(88011L, ((Number) payload.get("company_id")).longValue());
		assertEquals("system", payload.get("operator_type"));
		assertEquals("订单退款", payload.get("remarks"));
		assertEquals("订单号：77011，订单退款成功（支付宝渠道）", payload.get("detail"));
	}

	@Test
	void execute_alipay_fail_invokesPublishEventOnce_withErrorDetail() {
		WxpaySecapiAftersalesRefundRunner wx = mock(WxpaySecapiAftersalesRefundRunner.class);
		AlipayTradeAftersalesRefundRunner alipay = mock(AlipayTradeAftersalesRefundRunner.class);
		BsPayAftersalesOnlineRefundRunner bs = mock(BsPayAftersalesOnlineRefundRunner.class);
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = new OrderProcessLogPublishPortImpl(dispatchFacade);

		Map<String, Object> fail = new LinkedHashMap<>();
		fail.put("status", "FAIL");
		fail.put("error_code", "ACQ.TRADE_HAS_CLOSE");
		fail.put("error_desc", "交易已关闭");
		when(alipay.refund(anyLong(), anyLong(), anyString(), anyLong(), anyInt(), anyInt())).thenReturn(fail);

		PaymentSubjectDistributorIdPort paymentSubjectDistributorIdPort =
				mock(PaymentSubjectDistributorIdPort.class);
		when(paymentSubjectDistributorIdPort.resolveActualDistributorId(anyLong(), anyLong()))
				.thenAnswer(inv -> (Long) inv.getArgument(1));
		StandardOnlinePayAftersalesRefundExecutor executor =
				new StandardOnlinePayAftersalesRefundExecutor(wx, alipay, bs, orderProcessLogPublishPort, paymentSubjectDistributorIdPort);


		AftersalesRefundPaymentContext ctx = alipayRefundContext(88012L, 77012L);
		Map<String, Object> payRes = executor.execute(ctx);
		assertEquals("FAIL", String.valueOf(payRes.get("status")));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));
		Map<String, Object> payload = payloadCaptor.getValue();
		assertEquals("订单号：77012，订单退款失败（支付宝渠道），失败原因：交易已关闭", payload.get("detail"));
	}

	private static AftersalesRefundPaymentContext alipayRefundContext(long companyId, long orderId) {
		return new AftersalesRefundPaymentContext(
				companyId,
				"",
				44001L,
				orderId,
				1L,
				0L,
				0L,
				0L,
				0L,
				"alipay",
				"wx_lite",
				"trade-ali-1",
				200,
				100,
				"",
				"pay_txn_ali_1",
				false,
				"",
				"",
				"",
				0);
	}
}
