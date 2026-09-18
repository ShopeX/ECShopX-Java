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
@DisplayName("EVENT_ORDER_PROCESS_LOG: RefundJob/bspay executor publishEvent probe")
class StandardOnlinePayAftersalesRefundExecutorBspayOrderProcessLogDispatchPublishProbeTest {

	@Test
	void execute_bspay_success_invokesPublishEventOnce_withBspayRefundSuccessDetail() {
		WxpaySecapiAftersalesRefundRunner wx = mock(WxpaySecapiAftersalesRefundRunner.class);
		AlipayTradeAftersalesRefundRunner alipay = mock(AlipayTradeAftersalesRefundRunner.class);
		BsPayAftersalesOnlineRefundRunner bs = mock(BsPayAftersalesOnlineRefundRunner.class);
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = new OrderProcessLogPublishPortImpl(dispatchFacade);

		Map<String, Object> ok = new LinkedHashMap<>();
		ok.put("status", "SUCCESS");
		ok.put("refund_id", "bs-req-seq-1");
		when(bs.refund(anyLong(), anyString(), anyString(), anyLong(), anyInt())).thenReturn(ok);

		PaymentSubjectDistributorIdPort paymentSubjectDistributorIdPort =
				mock(PaymentSubjectDistributorIdPort.class);
		StandardOnlinePayAftersalesRefundExecutor executor =
				new StandardOnlinePayAftersalesRefundExecutor(wx, alipay, bs, orderProcessLogPublishPort, paymentSubjectDistributorIdPort);


		AftersalesRefundPaymentContext ctx = bspayRefundContext(88031L, 77031L);
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
		assertEquals(77031L, ((Number) payload.get("order_id")).longValue());
		assertEquals(88031L, ((Number) payload.get("company_id")).longValue());
		assertEquals("system", payload.get("operator_type"));
		assertEquals("订单退款", payload.get("remarks"));
		assertEquals("订单号：77031，订单退款成功（斗拱支付渠道）", payload.get("detail"));
	}

	@Test
	void execute_bspay_fail_invokesPublishEventOnce_withBspayRefundFailDetail() {
		WxpaySecapiAftersalesRefundRunner wx = mock(WxpaySecapiAftersalesRefundRunner.class);
		AlipayTradeAftersalesRefundRunner alipay = mock(AlipayTradeAftersalesRefundRunner.class);
		BsPayAftersalesOnlineRefundRunner bs = mock(BsPayAftersalesOnlineRefundRunner.class);
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = new OrderProcessLogPublishPortImpl(dispatchFacade);

		Map<String, Object> fail = new LinkedHashMap<>();
		fail.put("status", "FAIL");
		fail.put("error_desc", "网关超时");
		when(bs.refund(anyLong(), anyString(), anyString(), anyLong(), anyInt())).thenReturn(fail);

		PaymentSubjectDistributorIdPort paymentSubjectDistributorIdPort =
				mock(PaymentSubjectDistributorIdPort.class);
		StandardOnlinePayAftersalesRefundExecutor executor =
				new StandardOnlinePayAftersalesRefundExecutor(wx, alipay, bs, orderProcessLogPublishPort, paymentSubjectDistributorIdPort);


		AftersalesRefundPaymentContext ctx = bspayRefundContext(88032L, 77032L);
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
		assertEquals("订单号：77032，订单退款失败（斗拱支付渠道），失败原因：网关超时", payload.get("detail"));
	}

	private static AftersalesRefundPaymentContext bspayRefundContext(long companyId, long orderId) {
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
				"bspay",
				"bspay",
				"trade-bs-1",
				200,
				100,
				"20240101",
				"pay_txn_bs_1",
				false,
				"",
				"",
				"",
				0);
	}
}
