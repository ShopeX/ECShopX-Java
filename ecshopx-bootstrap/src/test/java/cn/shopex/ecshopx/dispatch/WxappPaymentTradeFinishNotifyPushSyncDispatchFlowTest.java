package cn.shopex.ecshopx.dispatch;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.port.orders.PaymentMsgNotifyWebSocketSendPort;
import cn.shopex.ecshopx.orders.dispatch.TradeFinishNotifyPushDispatchListener;
import cn.shopex.ecshopx.orders.service.notify.TradeFinishNotifyPushBusService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Wxapp-aligned {@code pay_type} (e.g. {@code wxpay}) on EVENT_TRADE_FINISH → TradeFinishNotifyPush sync path,
 * complementing {@link TradeFinishNotifyPushEventSyncDispatchFlowTest} (Alipay-centric positive case).
 */
@DisplayName("EVENT_TRADE_FINISH: Wxapp-oriented TradeFinishNotifyPush sync dispatch")
class WxappPaymentTradeFinishNotifyPushSyncDispatchFlowTest {

	private static final String LISTENER_NAME = "listener:orders.listeners.TradeFinishNotifyPush";
	private static final DateTimeFormatter PAY_DATE_EXPECTED =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	@Test
	@DisplayName("wxpay-ish payload + SYNC parent: skips websocket when pay_type is point")
	void wxappPublishEvent_sync_skipsWsWhenPayTypeIsPoint() {
		PaymentMsgNotifyWebSocketSendPort port = mock(PaymentMsgNotifyWebSocketSendPort.class);
		TradeFinishNotifyPushBusService busService = new TradeFinishNotifyPushBusService(port);
		TradeFinishNotifyPushDispatchListener listener = new TradeFinishNotifyPushDispatchListener(busService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.asyncDefaults(),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("pay_type", "point");
		payload.put("pay_fee", 100);
		payload.put("shop_id", "mini-s1");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verifyNoInteractions(port);
	}

	@Test
	@DisplayName("wxpay-ish payload + SYNC parent: skips websocket when pay_type is deposit")
	void wxappPublishEvent_sync_skipsWsWhenPayTypeIsDeposit() {
		PaymentMsgNotifyWebSocketSendPort port = mock(PaymentMsgNotifyWebSocketSendPort.class);
		TradeFinishNotifyPushBusService busService = new TradeFinishNotifyPushBusService(port);
		TradeFinishNotifyPushDispatchListener listener = new TradeFinishNotifyPushDispatchListener(busService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.asyncDefaults(),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("pay_type", "Deposit");
		payload.put("pay_fee", 100);
		payload.put("shop_id", "mini-s1");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verifyNoInteractions(port);
	}

	@Test
	@DisplayName("wxpay-ish eligibility + SYNC parent: skips websocket when pay_fee absent or zero")
	void wxappPublishEvent_sync_skipsWsWhenPayFeeIsZeroOrMissing() {
		PaymentMsgNotifyWebSocketSendPort port = mock(PaymentMsgNotifyWebSocketSendPort.class);
		TradeFinishNotifyPushBusService busService = new TradeFinishNotifyPushBusService(port);
		TradeFinishNotifyPushDispatchListener listener = new TradeFinishNotifyPushDispatchListener(busService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.asyncDefaults(),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> missingFee = new LinkedHashMap<>();
		missingFee.put("pay_type", "wxpay");
		missingFee.put("shop_id", "mini-s1");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, missingFee, DispatchOptions.eventDefaults());

		Map<String, Object> zeroFee = new LinkedHashMap<>();
		zeroFee.put("pay_type", "wxpayh5");
		zeroFee.put("pay_fee", 0);
		zeroFee.put("shop_id", "mini-s1");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, zeroFee, DispatchOptions.eventDefaults());

		verifyNoInteractions(port);
	}

	@Test
	@DisplayName("wxpay family + positive pay_fee: invokes payment websocket port once with camelCase envelope")
	void wxappPublishEvent_sync_invokesPaymentMsgPortOnceWhenPayTypeWxpayAndPayFeePositive() {
		PaymentMsgNotifyWebSocketSendPort port = mock(PaymentMsgNotifyWebSocketSendPort.class);
		TradeFinishNotifyPushBusService busService = new TradeFinishNotifyPushBusService(port);
		TradeFinishNotifyPushDispatchListener listener = new TradeFinishNotifyPushDispatchListener(busService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.asyncDefaults(),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long epochSec = 1704153600L;
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("pay_type", "wxpay");
		payload.put("pay_fee", 150);
		payload.put("shop_id", "MINI-SHOP-9");
		payload.put("time_start", String.valueOf(epochSec));

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		String expectedPayDate = PAY_DATE_EXPECTED.format(Instant.ofEpochSecond(epochSec));
		verify(port, times(1))
				.sendPaymentNotify(
						argThat(
								m ->
										m != null
												&& payFeeEqualsYuan(m.get("payFee"), "1.50")
												&& "wxpay".equals(String.valueOf(m.get("payType")))
												&& "MINI-SHOP-9".equals(String.valueOf(m.get("shopId")))
												&& Objects.equals(expectedPayDate, String.valueOf(m.get("payDate")))));
	}

	private static boolean payFeeEqualsYuan(Object raw, String expectedYuan) {
		if (raw instanceof BigDecimal b) {
			return b.compareTo(new BigDecimal(expectedYuan)) == 0;
		}
		return false;
	}
}
