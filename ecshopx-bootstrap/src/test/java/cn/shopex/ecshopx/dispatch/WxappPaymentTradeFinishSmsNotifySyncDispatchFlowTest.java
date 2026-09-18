package cn.shopex.ecshopx.dispatch;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.port.companys.CompanyPassportUidByCompanyIdPort;
import cn.shopex.ecshopx.common.port.orders.TradePaySuccessTemplatedSmsPort;
import cn.shopex.ecshopx.orders.dispatch.TradeFinishSmsNotifyDispatchListener;
import cn.shopex.ecshopx.orders.service.notify.TradeFinishSmsNotifyBusService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Wxapp-oriented {@code EVENT_TRADE_FINISH} payloads hitting {@link TradeFinishSmsNotifyBusService}: zero-fee wxpay SMS
 * eligibility vs {@code point} early exit (production listener/stack reused from entry-02).
 */
@DisplayName("EVENT_TRADE_FINISH: Wxapp-style SMS notify sync dispatch slice")
class WxappPaymentTradeFinishSmsNotifySyncDispatchFlowTest {

	private static final String LISTENER_NAME = "listener:orders.listeners.TradeFinishSmsNotify";
	private static final DateTimeFormatter PAY_TIME_EXPECTED =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private static DispatchFacade buildFacadeWithSmsListener(
			TradeFinishSmsNotifyDispatchListener listener) {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.async("sms", null),
				listener);
		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		return new DispatchFacade(core, new DispatchFanOutPlanner(registry));
	}

	@Test
	void publishEvent_sync_wxpayZeroFee_sendsTradePaySuccessWhenEligible() {
		CompanyPassportUidByCompanyIdPort passport = mock(CompanyPassportUidByCompanyIdPort.class);
		TradePaySuccessTemplatedSmsPort sms = mock(TradePaySuccessTemplatedSmsPort.class);
		when(passport.findPassportUid(9L)).thenReturn(Optional.of("shop-uid-1"));

		TradeFinishSmsNotifyBusService bus = new TradeFinishSmsNotifyBusService(passport, sms);
		DispatchFacade facade = buildFacadeWithSmsListener(new TradeFinishSmsNotifyDispatchListener(bus));

		long epochSec = 1704067200L;
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("pay_type", "wxpay");
		payload.put("pay_fee", 0);
		payload.put("company_id", 9L);
		payload.put("mobile", "13800138000");
		payload.put("time_start", String.valueOf(epochSec));

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		String expectedPayTime = PAY_TIME_EXPECTED.format(Instant.ofEpochSecond(epochSec));
		verify(sms, times(1))
				.sendTradePaySuccess(
						eq(9L),
						eq("13800138000"),
						argThat(
								m ->
										m != null
												&& "0.00".equals(m.get("pay_money"))
												&& Objects.equals(expectedPayTime, m.get("pay_time"))));
	}

	@Test
	void publishEvent_sync_pointPay_skipsSms() {
		CompanyPassportUidByCompanyIdPort passport = mock(CompanyPassportUidByCompanyIdPort.class);
		TradePaySuccessTemplatedSmsPort sms = mock(TradePaySuccessTemplatedSmsPort.class);

		TradeFinishSmsNotifyBusService bus = new TradeFinishSmsNotifyBusService(passport, sms);
		DispatchFacade facade = buildFacadeWithSmsListener(new TradeFinishSmsNotifyDispatchListener(bus));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("pay_type", "point");
		payload.put("pay_fee", 50);
		payload.put("company_id", 9L);
		payload.put("mobile", "13800138000");
		payload.put("time_start", "1704067200");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verifyNoInteractions(passport, sms);
	}
}
