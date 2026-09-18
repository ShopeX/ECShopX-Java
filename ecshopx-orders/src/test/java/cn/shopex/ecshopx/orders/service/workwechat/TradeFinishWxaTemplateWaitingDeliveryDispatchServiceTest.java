package cn.shopex.ecshopx.orders.service.workwechat;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.SendWaitingDeliveryNoticeJobDispatchPublisher;
import cn.shopex.ecshopx.common.port.orders.TradeFinishPaymentSuccShopNamePort;
import cn.shopex.ecshopx.common.port.orders.TradeFinishPaymentSuccWxaSubscribeSendPort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TradeFinishWxaTemplateWaitingDeliveryDispatchServiceTest {

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private SendWaitingDeliveryNoticeJobDispatchPublisher sendWaitingDeliveryNoticeJobDispatchPublisher;

	@Mock
	private TradeFinishPaymentSuccWxaSubscribeSendPort tradeFinishPaymentSuccWxaSubscribeSendPort;

	@Mock
	private TradeFinishPaymentSuccShopNamePort tradeFinishPaymentSuccShopNamePort;

	@InjectMocks
	private TradeFinishWxaTemplateWaitingDeliveryDispatchService tradeFinishWxaTemplateWaitingDeliveryDispatchService;

	@Test
	@DisplayName("Trade-finish after paths such as H5 Alipay sync return: wxpay publishes waiting-delivery job when distributor and non-self-pickup receipt allow")
	void whenPayTypeWxpay_andDistributorAndNotZiti_invokesSendWaitingDeliveryNoticePublisher() {
		String companyId = "9";
		String orderId = "501";
		NormalOrders row = new NormalOrders();
		row.setCompanyId(9L);
		row.setOrderId(501L);
		row.setDistributorId(77L);
		row.setReceiptType("logistics");
		when(normalOrdersMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(row);

		Map<String, Object> payload = basePayload(companyId, orderId, "wxpay");
		tradeFinishWxaTemplateWaitingDeliveryDispatchService.dispatchIfApplicable(payload);

		verify(sendWaitingDeliveryNoticeJobDispatchPublisher)
				.publish(eq(companyId), eq(orderId), eq("77"));
		verifyNoInteractions(tradeFinishPaymentSuccWxaSubscribeSendPort);
	}

	@Test
	@DisplayName("Trade-finish after paths such as H5 Alipay sync return: deposit pay type publishes waiting-delivery job when distributor and receipt type allow")
	void whenPayTypeDeposit_andDistributorAndNotZiti_invokesSendWaitingDeliveryNoticePublisher() {
		String companyId = "9";
		String orderId = "501";
		NormalOrders row = new NormalOrders();
		row.setCompanyId(9L);
		row.setOrderId(501L);
		row.setDistributorId(12L);
		row.setReceiptType("merchant");
		when(normalOrdersMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(row);

		Map<String, Object> payload = basePayload(companyId, orderId, "deposit");
		tradeFinishWxaTemplateWaitingDeliveryDispatchService.dispatchIfApplicable(payload);

		verify(sendWaitingDeliveryNoticeJobDispatchPublisher)
				.publish(eq(companyId), eq(orderId), eq("12"));
		verifyNoInteractions(tradeFinishPaymentSuccWxaSubscribeSendPort);
	}

	@Test
	@DisplayName("Member-card trade source does not enqueue waiting-delivery notice")
	void whenTradeSourceMembercard_doesNotInvokePublisher() {
		Map<String, Object> payload = basePayload("9", "501", "wxpay");
		payload.put("trade_source_type", "membercard");
		tradeFinishWxaTemplateWaitingDeliveryDispatchService.dispatchIfApplicable(payload);

		verifyNoInteractions(sendWaitingDeliveryNoticeJobDispatchPublisher);
		verifyNoInteractions(normalOrdersMapper);
		verifyNoInteractions(tradeFinishPaymentSuccWxaSubscribeSendPort);
	}

	@Test
	@DisplayName("Alipay pay_type on trade-finish skips WXA waiting-delivery path (only wxpay/deposit gate this listener)")
	void whenPayTypeAlipay_doesNotInvokePublisher() {
		Map<String, Object> payload = basePayload("9", "501", "alipay");
		tradeFinishWxaTemplateWaitingDeliveryDispatchService.dispatchIfApplicable(payload);

		verifyNoInteractions(sendWaitingDeliveryNoticeJobDispatchPublisher);
		verifyNoInteractions(normalOrdersMapper);
		verifyNoInteractions(tradeFinishPaymentSuccWxaSubscribeSendPort);
	}

	@Test
	@DisplayName("Self-pickup receipt type skips waiting-delivery dispatch even for wxpay")
	void whenReceiptTypeZiti_doesNotInvokePublisher() {
		NormalOrders row = new NormalOrders();
		row.setCompanyId(9L);
		row.setOrderId(501L);
		row.setDistributorId(3L);
		row.setReceiptType("ziti");
		when(normalOrdersMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(row);

		Map<String, Object> payload = basePayload("9", "501", "wxpay");
		tradeFinishWxaTemplateWaitingDeliveryDispatchService.dispatchIfApplicable(payload);

		verifyNoInteractions(sendWaitingDeliveryNoticeJobDispatchPublisher);
		verifyNoInteractions(tradeFinishPaymentSuccWxaSubscribeSendPort);
	}

	@Test
	@DisplayName("Zero distributor id skips waiting-delivery dispatch")
	void whenDistributorIdZero_doesNotInvokePublisher() {
		NormalOrders row = new NormalOrders();
		row.setCompanyId(9L);
		row.setOrderId(501L);
		row.setDistributorId(0L);
		row.setReceiptType("logistics");
		when(normalOrdersMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(row);

		Map<String, Object> payload = basePayload("9", "501", "wxpay");
		tradeFinishWxaTemplateWaitingDeliveryDispatchService.dispatchIfApplicable(payload);

		verifyNoInteractions(sendWaitingDeliveryNoticeJobDispatchPublisher);
		verifyNoInteractions(tradeFinishPaymentSuccWxaSubscribeSendPort);
	}

	@Test
	@DisplayName("wxpay after waiting-delivery gate invokes paymentSucc wxa port with forceFire=false and trade-derived data")
	void whenWxpay_withTradeFields_invokesPaymentSuccPortWithForceFireFalse() {
		String companyId = "9";
		String orderId = "501";
		NormalOrders row = new NormalOrders();
		row.setCompanyId(9L);
		row.setOrderId(501L);
		row.setDistributorId(77L);
		row.setReceiptType("logistics");
		row.setOrderClass("normal");
		when(normalOrdersMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(row);
		when(tradeFinishPaymentSuccShopNamePort.resolveForSubscribeTemplate(9L, "normal", "12", "wx-app"))
				.thenReturn("Corner Shop");

		Map<String, Object> payload = paymentSuccPayload(companyId, orderId);
		tradeFinishWxaTemplateWaitingDeliveryDispatchService.dispatchIfApplicable(payload);

		verify(sendWaitingDeliveryNoticeJobDispatchPublisher)
				.publish(eq(companyId), eq(orderId), eq("77"));
		verify(tradeFinishPaymentSuccWxaSubscribeSendPort)
				.send(
						argThat(
								m -> {
									if (!"paymentSucc".equals(m.get("scenes_name"))) {
										return false;
									}
									if (!Long.valueOf(9L).equals(toLong(m.get("company_id")))) {
										return false;
									}
									if (!"wx-app".equals(m.get("appid"))) {
										return false;
									}
									if (!"open-1".equals(m.get("openid"))) {
										return false;
									}
									Object dataObj = m.get("data");
									if (!(dataObj instanceof Map<?, ?> d)) {
										return false;
									}
									return "99.99".equals(String.valueOf(d.get("pay_money")))
											&& "2026-05-08 10:00:00".equals(String.valueOf(d.get("pay_date")))
											&& "item A".equals(String.valueOf(d.get("item_name")))
											&& "Corner Shop".equals(String.valueOf(d.get("shop_name")))
											&& orderId.equals(String.valueOf(d.get("order_id")))
											&& "trade-99".equals(String.valueOf(d.get("trade_id")))
											&& "物流配送".equals(String.valueOf(d.get("receipt_type")))
											&& "微信支付".equals(String.valueOf(d.get("pay_type")));
								}),
						eq(false));
	}

	@Test
	@DisplayName("deposit publishes waiting-delivery when applicable then skips paymentSucc wxa port")
	void whenDeposit_afterWaitingDelivery_skipsPaymentSuccPort() {
		String companyId = "9";
		String orderId = "501";
		NormalOrders row = new NormalOrders();
		row.setCompanyId(9L);
		row.setOrderId(501L);
		row.setDistributorId(12L);
		row.setReceiptType("merchant");
		when(normalOrdersMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(row);

		Map<String, Object> payload = paymentSuccPayload(companyId, orderId);
		payload.put("pay_type", "deposit");
		tradeFinishWxaTemplateWaitingDeliveryDispatchService.dispatchIfApplicable(payload);

		verify(sendWaitingDeliveryNoticeJobDispatchPublisher)
				.publish(eq(companyId), eq(orderId), eq("12"));
		verifyNoInteractions(tradeFinishPaymentSuccWxaSubscribeSendPort);
	}

	@Test
	@DisplayName("wxpay when order or trade fields missing skips paymentSucc port (tolerant)")
	void whenWxpay_missingPayFee_skipsPaymentSuccPort() {
		String companyId = "9";
		String orderId = "501";
		NormalOrders row = new NormalOrders();
		row.setCompanyId(9L);
		row.setOrderId(501L);
		row.setDistributorId(77L);
		row.setReceiptType("logistics");
		when(normalOrdersMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(row);

		Map<String, Object> payload = paymentSuccPayload(companyId, orderId);
		payload.remove("pay_fee");
		tradeFinishWxaTemplateWaitingDeliveryDispatchService.dispatchIfApplicable(payload);

		verify(sendWaitingDeliveryNoticeJobDispatchPublisher)
				.publish(eq(companyId), eq(orderId), eq("77"));
		verifyNoInteractions(tradeFinishPaymentSuccWxaSubscribeSendPort);
	}

	private static Map<String, Object> basePayload(String companyId, String orderId, String payType) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("order_id", orderId);
		payload.put("pay_type", payType);
		return payload;
	}

	private static Map<String, Object> paymentSuccPayload(String companyId, String orderId) {
		Map<String, Object> payload = basePayload(companyId, orderId, "wxpay");
		payload.put("trade_id", "trade-99");
		payload.put("pay_fee", 9999);
		payload.put("detail", "item A");
		payload.put("time_start", "2026-05-08 10:00:00");
		payload.put("wxa_appid", "wx-app");
		payload.put("open_id", "open-1");
		payload.put("shop_id", "12");
		return payload;
	}

	private static Long toLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw));
	}
}
