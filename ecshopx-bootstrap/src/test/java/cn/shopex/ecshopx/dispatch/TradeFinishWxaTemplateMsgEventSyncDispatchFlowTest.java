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

@DisplayName("EVENT_TRADE_FINISH: TradeFinishWxaTemplateMsg listener sync dispatch")
class TradeFinishWxaTemplateMsgEventSyncDispatchFlowTest {

	private static final String LISTENER_NAME = "listener:orders.listeners.TradeFinishWxaTemplateMsg";

	@Test
	void publishEvent_sync_invokesDispatchIfApplicableOnce_forAlipayPayTypePayload() {
		TradeFinishWxaTemplateWaitingDeliveryDispatchService svc =
				mock(TradeFinishWxaTemplateWaitingDeliveryDispatchService.class);
		OrdersTradeFinishWxaTemplateDispatchListener listener =
				new OrdersTradeFinishWxaTemplateDispatchListener(svc);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.async("default", null),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("pay_type", "alipay");
		payload.put("company_id", "9");
		payload.put("order_id", "501");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(svc, times(1))
				.dispatchIfApplicable(
						argThat(
								m -> {
									return m != null
											&& "alipay".equals(String.valueOf(m.get("pay_type")))
											&& "9".equals(String.valueOf(m.get("company_id")))
											&& "501".equals(String.valueOf(m.get("order_id")));
								}));
	}

	@Test
	void publishEvent_sync_invokesDispatchIfApplicableOnce_forOfflinePayWithPositivePayFee() {
		TradeFinishWxaTemplateWaitingDeliveryDispatchService svc =
				mock(TradeFinishWxaTemplateWaitingDeliveryDispatchService.class);
		OrdersTradeFinishWxaTemplateDispatchListener listener =
				new OrdersTradeFinishWxaTemplateDispatchListener(svc);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.async("default", null),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("pay_type", "offline_pay");
		payload.put("pay_fee", 10_000);
		payload.put("company_id", 9L);
		payload.put("order_id", 501L);

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(svc, times(1))
				.dispatchIfApplicable(
						argThat(
								m -> {
									return m != null
											&& "offline_pay".equals(String.valueOf(m.get("pay_type")))
											&& m.get("pay_fee") instanceof Number
											&& ((Number) m.get("pay_fee")).intValue() == 10_000
											&& "9".equals(String.valueOf(m.get("company_id")))
											&& "501".equals(String.valueOf(m.get("order_id")));
								}));
	}

	@Test
	void publishEvent_sync_invokesDispatchIfApplicableOnce_forWxpayPositivePayFee() {
		TradeFinishWxaTemplateWaitingDeliveryDispatchService svc =
				mock(TradeFinishWxaTemplateWaitingDeliveryDispatchService.class);
		OrdersTradeFinishWxaTemplateDispatchListener listener =
				new OrdersTradeFinishWxaTemplateDispatchListener(svc);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.async("default", null),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("pay_type", "wxpay");
		payload.put("pay_fee", 188);
		payload.put("company_id", "9");
		payload.put("order_id", "501");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(svc, times(1))
				.dispatchIfApplicable(
						argThat(
								m -> {
									return m != null
											&& "wxpay".equals(String.valueOf(m.get("pay_type")))
											&& m.get("pay_fee") instanceof Number
											&& ((Number) m.get("pay_fee")).intValue() == 188
											&& "9".equals(String.valueOf(m.get("company_id")))
											&& "501".equals(String.valueOf(m.get("order_id")));
								}));
	}

	@Test
	void publishEvent_sync_invokesDispatchIfApplicableOnce_thenEarlyReturn_forMembercardTradeSourceType() {
		TradeFinishWxaTemplateWaitingDeliveryDispatchService svc =
				mock(TradeFinishWxaTemplateWaitingDeliveryDispatchService.class);
		OrdersTradeFinishWxaTemplateDispatchListener listener =
				new OrdersTradeFinishWxaTemplateDispatchListener(svc);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.async("default", null),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("trade_source_type", "membercard");
		payload.put("pay_type", "wxpay");
		payload.put("company_id", 9L);
		payload.put("order_id", 501L);

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(svc, times(1))
				.dispatchIfApplicable(
						argThat(
								m -> {
									return m != null
											&& "membercard"
													.equalsIgnoreCase(
															String.valueOf(m.get("trade_source_type")));
								}));
	}
}
