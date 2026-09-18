package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.exception.ResourceException;
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

@DisplayName("EVENT_TRADE_FINISH: TradeFinishSmsNotify listener sync dispatch")
class TradeFinishSmsNotifyEventSyncDispatchFlowTest {

	private static final String LISTENER_NAME = "listener:orders.listeners.TradeFinishSmsNotify";
	private static final DateTimeFormatter PAY_TIME_EXPECTED =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	@Test
	void publishEvent_sync_skipsSmsWhenPayTypeIsPoint() {
		CompanyPassportUidByCompanyIdPort passport = mock(CompanyPassportUidByCompanyIdPort.class);
		TradePaySuccessTemplatedSmsPort sms = mock(TradePaySuccessTemplatedSmsPort.class);
		TradeFinishSmsNotifyBusService bus = new TradeFinishSmsNotifyBusService(passport, sms);
		TradeFinishSmsNotifyDispatchListener listener = new TradeFinishSmsNotifyDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.async("sms", null),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("pay_type", "point");
		payload.put("pay_fee", 100);
		payload.put("company_id", 9L);
		payload.put("mobile", "13800138000");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verifyNoInteractions(passport, sms);
	}

	@Test
	void publishEvent_sync_skipsSmsWhenPayTypeIsDeposit() {
		CompanyPassportUidByCompanyIdPort passport = mock(CompanyPassportUidByCompanyIdPort.class);
		TradePaySuccessTemplatedSmsPort sms = mock(TradePaySuccessTemplatedSmsPort.class);
		TradeFinishSmsNotifyBusService bus = new TradeFinishSmsNotifyBusService(passport, sms);
		TradeFinishSmsNotifyDispatchListener listener = new TradeFinishSmsNotifyDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.async("sms", null),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("pay_type", "deposit");
		payload.put("pay_fee", 100);
		payload.put("company_id", 9);
		payload.put("mobile", "13800138000");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verifyNoInteractions(passport, sms);
	}

	@Test
	void publishEvent_sync_skipsSmsWhenPassportUidMissing() {
		CompanyPassportUidByCompanyIdPort passport = mock(CompanyPassportUidByCompanyIdPort.class);
		TradePaySuccessTemplatedSmsPort sms = mock(TradePaySuccessTemplatedSmsPort.class);
		when(passport.findPassportUid(9L)).thenReturn(Optional.empty());

		TradeFinishSmsNotifyBusService bus = new TradeFinishSmsNotifyBusService(passport, sms);
		TradeFinishSmsNotifyDispatchListener listener = new TradeFinishSmsNotifyDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.async("sms", null),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("pay_type", "alipay");
		payload.put("pay_fee", 100);
		payload.put("company_id", "9");
		payload.put("mobile", "13800138000");
		payload.put("time_start", "1704067200");

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		verify(passport, times(1)).findPassportUid(9L);
		verify(sms, never()).sendTradePaySuccess(anyLong(), anyString(), any());
	}

	@Test
	void publishEvent_sync_sendsTradePaySuccessWhenEligible() {
		CompanyPassportUidByCompanyIdPort passport = mock(CompanyPassportUidByCompanyIdPort.class);
		TradePaySuccessTemplatedSmsPort sms = mock(TradePaySuccessTemplatedSmsPort.class);
		when(passport.findPassportUid(9L)).thenReturn(Optional.of("shop-uid-1"));

		TradeFinishSmsNotifyBusService bus = new TradeFinishSmsNotifyBusService(passport, sms);
		TradeFinishSmsNotifyDispatchListener listener = new TradeFinishSmsNotifyDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.async("sms", null),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long epochSec = 1704067200L;
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("pay_type", "alipay");
		payload.put("pay_fee", 100);
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
												&& "1.00".equals(m.get("pay_money"))
												&& Objects.equals(expectedPayTime, m.get("pay_time"))));
	}

	@Test
	void publishEvent_sync_sendsTradePaySuccessWhenPayTypeOfflinePayAndPayFeePositive() {
		CompanyPassportUidByCompanyIdPort passport = mock(CompanyPassportUidByCompanyIdPort.class);
		TradePaySuccessTemplatedSmsPort sms = mock(TradePaySuccessTemplatedSmsPort.class);
		when(passport.findPassportUid(9L)).thenReturn(Optional.of("shop-uid-offline"));

		TradeFinishSmsNotifyBusService bus = new TradeFinishSmsNotifyBusService(passport, sms);
		TradeFinishSmsNotifyDispatchListener listener = new TradeFinishSmsNotifyDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.async("sms", null),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long epochSec = 1704220800L;
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("pay_type", "offline_pay");
		payload.put("pay_fee", 888);
		payload.put("company_id", 9L);
		payload.put("mobile", "13800138999");
		payload.put("time_start", String.valueOf(epochSec));

		facade.publishEvent(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH, payload, DispatchOptions.eventDefaults());

		String expectedPayTime = PAY_TIME_EXPECTED.format(Instant.ofEpochSecond(epochSec));
		verify(passport, times(1)).findPassportUid(9L);
		verify(sms, times(1))
				.sendTradePaySuccess(
						eq(9L),
						eq("13800138999"),
						argThat(
								m ->
										m != null
												&& "8.88".equals(m.get("pay_money"))
												&& Objects.equals(expectedPayTime, m.get("pay_time"))));
	}

	@Test
	void publishEvent_sync_swallowsSmsPortFailures() {
		CompanyPassportUidByCompanyIdPort passport = mock(CompanyPassportUidByCompanyIdPort.class);
		TradePaySuccessTemplatedSmsPort sms = mock(TradePaySuccessTemplatedSmsPort.class);
		when(passport.findPassportUid(9L)).thenReturn(Optional.of("shop-uid-1"));
		doThrow(new ResourceException("sms down")).when(sms).sendTradePaySuccess(anyLong(), anyString(), any());

		TradeFinishSmsNotifyBusService bus = new TradeFinishSmsNotifyBusService(passport, sms);
		TradeFinishSmsNotifyDispatchListener listener = new TradeFinishSmsNotifyDispatchListener(bus);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.async("sms", null),
				listener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("pay_type", "alipay");
		payload.put("pay_fee", 100);
		payload.put("company_id", 9L);
		payload.put("mobile", "13800138000");
		payload.put("time_start", "1704067200");

		assertDoesNotThrow(
				() ->
						facade.publishEvent(
								OrdersDispatchEventNames.EVENT_TRADE_FINISH,
								payload,
								DispatchOptions.eventDefaults()));

		verify(sms, times(1)).sendTradePaySuccess(anyLong(), anyString(), any());
	}
}
