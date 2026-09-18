package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.HfpayDispatchEventNames;
import cn.shopex.ecshopx.hfpay.dispatch.HfpayTradeRecordHfpayDistributorWithdrawSuccessDispatchListener;
import cn.shopex.ecshopx.hfpay.service.HfpayTradeRecordService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HfpayDistributorWithdrawSuccessEventSyncBusDispatchFlowTest {

	@Test
	void publishDistributorWithdrawSuccess_sync_listener_invokesTradeRecordWithdraw() {
		HfpayTradeRecordService mockTradeRecordService = mock(HfpayTradeRecordService.class);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		registry.registerEventListener(
				HfpayDispatchEventNames.EVENT_HFPAY_DISTRIBUTOR_WITHDRAW_SUCCESS,
				HfpayDispatchEventNames.LISTENER_HFPAY_TRADE_RECORD_DISTRIBUTOR_WITHDRAW_SUCCESS,
				ListenerDispatchOptions.syncDefaults(),
				new HfpayTradeRecordHfpayDistributorWithdrawSuccessDispatchListener(mockTradeRecordService));

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("hfpay_cash_record_id", 10L);
		entities.put("company_id", 7L);
		entities.put("distributor_id", 3L);
		entities.put("trans_amt", 1000);
		entities.put("order_id", "ORD-X");
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		assertDoesNotThrow(
				() ->
						facade.publishEvent(
								HfpayDispatchEventNames.EVENT_HFPAY_DISTRIBUTOR_WITHDRAW_SUCCESS,
								payload,
								DispatchOptions.eventDefaults()));

		verify(mockTradeRecordService).withdraw(eq(7L), eq(3L), eq(1000), eq("ORD-X"));
		assertTrue(captured.isEmpty());
	}
}
