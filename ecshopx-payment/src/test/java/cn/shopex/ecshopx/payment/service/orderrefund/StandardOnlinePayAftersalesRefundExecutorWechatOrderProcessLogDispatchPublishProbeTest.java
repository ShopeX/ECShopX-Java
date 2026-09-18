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
@DisplayName("EVENT_ORDER_PROCESS_LOG: RefundJob/wxpay executor publishEvent probe")
class StandardOnlinePayAftersalesRefundExecutorWechatOrderProcessLogDispatchPublishProbeTest {

	@Test
	void execute_wxpay_success_invokesPublishEventOnce_withWechatRefundSuccessDetail() {
		WxpaySecapiAftersalesRefundRunner wx = mock(WxpaySecapiAftersalesRefundRunner.class);
		AlipayTradeAftersalesRefundRunner alipay = mock(AlipayTradeAftersalesRefundRunner.class);
		BsPayAftersalesOnlineRefundRunner bs = mock(BsPayAftersalesOnlineRefundRunner.class);
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = new OrderProcessLogPublishPortImpl(dispatchFacade);

		Map<String, Object> ok = new LinkedHashMap<>();
		ok.put("status", "SUCCESS");
		ok.put("refund_id", "wx-refund-1");
		when(wx.refund(anyLong(), anyLong(), anyString(), anyString(), anyLong(), anyInt(), anyInt()))
				.thenReturn(ok);

		PaymentSubjectDistributorIdPort paymentSubjectDistributorIdPort =
				mock(PaymentSubjectDistributorIdPort.class);
		when(paymentSubjectDistributorIdPort.resolveActualDistributorId(anyLong(), anyLong()))
				.thenAnswer(inv -> (Long) inv.getArgument(1));
		StandardOnlinePayAftersalesRefundExecutor executor =
				new StandardOnlinePayAftersalesRefundExecutor(wx, alipay, bs, orderProcessLogPublishPort, paymentSubjectDistributorIdPort);


		AftersalesRefundPaymentContext ctx = wechatRefundContext(88041L, 77041L);
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
		assertEquals(77041L, ((Number) payload.get("order_id")).longValue());
		assertEquals(88041L, ((Number) payload.get("company_id")).longValue());
		assertEquals("system", payload.get("operator_type"));
		assertEquals("订单退款", payload.get("remarks"));
		assertEquals("订单号：77041，订单退款成功（微信支付渠道）", payload.get("detail"));
	}

	@Test
	void execute_wxpay_fail_invokesPublishEventOnce_withWechatRefundFailDetail() {
		WxpaySecapiAftersalesRefundRunner wx = mock(WxpaySecapiAftersalesRefundRunner.class);
		AlipayTradeAftersalesRefundRunner alipay = mock(AlipayTradeAftersalesRefundRunner.class);
		BsPayAftersalesOnlineRefundRunner bs = mock(BsPayAftersalesOnlineRefundRunner.class);
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = new OrderProcessLogPublishPortImpl(dispatchFacade);

		Map<String, Object> fail = new LinkedHashMap<>();
		fail.put("status", "FAIL");
		fail.put("error_desc", "余额不足");
		when(wx.refund(anyLong(), anyLong(), anyString(), anyString(), anyLong(), anyInt(), anyInt()))
				.thenReturn(fail);

		PaymentSubjectDistributorIdPort paymentSubjectDistributorIdPort =
				mock(PaymentSubjectDistributorIdPort.class);
		when(paymentSubjectDistributorIdPort.resolveActualDistributorId(anyLong(), anyLong()))
				.thenAnswer(inv -> (Long) inv.getArgument(1));
		StandardOnlinePayAftersalesRefundExecutor executor =
				new StandardOnlinePayAftersalesRefundExecutor(wx, alipay, bs, orderProcessLogPublishPort, paymentSubjectDistributorIdPort);


		AftersalesRefundPaymentContext ctx = wechatRefundContext(88042L, 77042L);
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
		assertEquals("订单号：77042，订单退款失败（微信支付渠道），失败原因：余额不足", payload.get("detail"));
	}

	private static AftersalesRefundPaymentContext wechatRefundContext(long companyId, long orderId) {
		return new AftersalesRefundPaymentContext(
				companyId,
				"app-wx-hint",
				44001L,
				orderId,
				1L,
				0L,
				201L,
				0L,
				0L,
				"wxpayjs",
				"wx_lite",
				"trade-wx-1",
				200,
				100,
				"",
				"pay_txn_wx_1",
				false,
				"",
				"",
				"",
				0);
	}
}
