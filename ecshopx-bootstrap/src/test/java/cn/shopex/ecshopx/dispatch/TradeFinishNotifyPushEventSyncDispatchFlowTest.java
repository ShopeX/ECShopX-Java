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

@DisplayName("EVENT_TRADE_FINISH: TradeFinishNotifyPush paymentmsg listener sync dispatch")
class TradeFinishNotifyPushEventSyncDispatchFlowTest {

	private static final String LISTENER_NAME = "listener:orders.listeners.TradeFinishNotifyPush";
	private static final DateTimeFormatter PAY_DATE_EXPECTED =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	@Test
	void publishEvent_sync_skipsWsWhenPayTypeIsPoint() {
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
		payload.put("shop_id", "s1");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verifyNoInteractions(port);
	}

	@Test
	void publishEvent_sync_skipsWsWhenPayTypeIsDeposit() {
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
		payload.put("shop_id", "s1");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verifyNoInteractions(port);
	}

	@Test
	void publishEvent_sync_skipsWsWhenPayFeeIsZeroOrMissing() {
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
		missingFee.put("pay_type", "alipay");
		missingFee.put("shop_id", "s1");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, missingFee, DispatchOptions.eventDefaults());

		Map<String, Object> zeroFee = new LinkedHashMap<>();
		zeroFee.put("pay_type", "alipay");
		zeroFee.put("pay_fee", 0);
		zeroFee.put("shop_id", "s1");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, zeroFee, DispatchOptions.eventDefaults());

		verifyNoInteractions(port);
	}

	@Test
	void publishEvent_sync_invokesPaymentMsgPortOnceWithCamelCaseEnvelopeWhenPayFeePositiveAndPayTypeEligible() {
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

		long epochSec = 1704067200L;
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("pay_type", "alipay");
		payload.put("pay_fee", 100);
		payload.put("shop_id", "SHOP-1");
		payload.put("time_start", String.valueOf(epochSec));

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		String expectedPayDate = PAY_DATE_EXPECTED.format(Instant.ofEpochSecond(epochSec));
		verify(port, times(1))
				.sendPaymentNotify(
						argThat(
								m ->
										m != null
												&& payFeeEqualsYuan(m.get("payFee"), "1.00")
												&& "alipay".equals(String.valueOf(m.get("payType")))
												&& "SHOP-1".equals(String.valueOf(m.get("shopId")))
												&& Objects.equals(expectedPayDate, String.valueOf(m.get("payDate")))));
	}

	@Test
	void publishEvent_sync_invokesPaymentMsgPortOnceWhenPayTypeOfflinePayAndPayFeePositive() {
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

		long epochSec = 1704220800L;
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("pay_type", "offline_pay");
		payload.put("pay_fee", 888);
		payload.put("shop_id", "OFF-SHOP-2");
		payload.put("time_start", String.valueOf(epochSec));

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		String expectedPayDate = PAY_DATE_EXPECTED.format(Instant.ofEpochSecond(epochSec));
		verify(port, times(1))
				.sendPaymentNotify(
						argThat(
								m ->
										m != null
												&& payFeeEqualsYuan(m.get("payFee"), "8.88")
												&& "offline_pay".equals(String.valueOf(m.get("payType")))
												&& "OFF-SHOP-2".equals(String.valueOf(m.get("shopId")))
												&& Objects.equals(expectedPayDate, String.valueOf(m.get("payDate")))));
	}

	private static boolean payFeeEqualsYuan(Object raw, String expectedYuan) {
		if (raw instanceof BigDecimal b) {
			return b.compareTo(new BigDecimal(expectedYuan)) == 0;
		}
		return false;
	}
}
