package cn.shopex.ecshopx.dispatch;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.orders.dispatch.OrdersTradeFinishWxaTemplateDispatchListener;
import cn.shopex.ecshopx.orders.service.workwechat.TradeFinishWxaTemplateWaitingDeliveryDispatchService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Wxapp-oriented {@code EVENT_TRADE_FINISH} payloads for {@link TradeFinishWxaTemplateWaitingDeliveryDispatchService}: zero-fee
 * wxpay shape vs {@code point} early return inside dispatch (production listener reused).
 */
@DisplayName("EVENT_TRADE_FINISH: Wxapp-style Wxa templateMsg sync dispatch slice")
class WxappPaymentTradeFinishWxaTemplateMsgSyncDispatchFlowTest {

	private static final String LISTENER_NAME = "listener:orders.listeners.TradeFinishWxaTemplateMsg";

	private static DispatchFacade buildFacadeWithWxaListener(
			OrdersTradeFinishWxaTemplateDispatchListener listener) {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.async("default", null),
				listener);
		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		return new DispatchFacade(core, new DispatchFanOutPlanner(registry));
	}

	@Test
	void publishEvent_sync_wxpayZeroFee_dispatchesIfApplicableWithWxappOrderKeys() {
		TradeFinishWxaTemplateWaitingDeliveryDispatchService svc =
				mock(TradeFinishWxaTemplateWaitingDeliveryDispatchService.class);
		DispatchFacade facade =
				buildFacadeWithWxaListener(new OrdersTradeFinishWxaTemplateDispatchListener(svc));

		long companyId = 9L;
		String orderId = "O1";
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("pay_type", "wxpay");
		payload.put("pay_fee", 0);
		payload.put("company_id", companyId);
		payload.put("order_id", orderId);
		payload.put("trade_source_type", "normal");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(svc, times(1))
				.dispatchIfApplicable(
						argThat(
								m ->
										m != null
												&& "wxpay"
														.equalsIgnoreCase(
																String.valueOf(m.get("pay_type")))
												&& String.valueOf(companyId)
														.equals(String.valueOf(m.get("company_id")))
												&& orderId.equals(String.valueOf(m.get("order_id")))
												&& "normal"
														.equalsIgnoreCase(
																String.valueOf(
																		m.get("trade_source_type")))));
	}

	@Test
	void publishEvent_sync_pointPay_invokesDispatchIfApplicable_listenerDelegatesEarlyExitInProduction() {
		TradeFinishWxaTemplateWaitingDeliveryDispatchService svc =
				mock(TradeFinishWxaTemplateWaitingDeliveryDispatchService.class);
		DispatchFacade facade =
				buildFacadeWithWxaListener(new OrdersTradeFinishWxaTemplateDispatchListener(svc));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("pay_type", "point");
		payload.put("pay_fee", 50);
		payload.put("company_id", 9L);
		payload.put("order_id", "O2");
		payload.put("trade_source_type", "normal");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(svc, times(1))
				.dispatchIfApplicable(
						argThat(
								m ->
										m != null
												&& "point"
														.equalsIgnoreCase(
																String.valueOf(m.get("pay_type")))
												&& "9".equals(String.valueOf(m.get("company_id")))
												&& "O2".equals(String.valueOf(m.get("order_id")))));
	}
}
