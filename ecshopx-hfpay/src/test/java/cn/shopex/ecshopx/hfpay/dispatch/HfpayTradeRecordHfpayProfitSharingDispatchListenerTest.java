package cn.shopex.ecshopx.hfpay.dispatch;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import cn.shopex.ecshopx.hfpay.service.HfpayTradeRecordService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HfpayTradeRecordHfpayProfitSharingDispatchListenerTest {

	@Test
	void onEvent_withOrderId_delegatesToTradeRecordServiceProfit() {
		HfpayTradeRecordService tradeRecordService = mock(HfpayTradeRecordService.class);
		HfpayTradeRecordHfpayProfitSharingDispatchListener listener =
				new HfpayTradeRecordHfpayProfitSharingDispatchListener(tradeRecordService);
		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("order_id", 123L);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		listener.onEvent(payload);

		verify(tradeRecordService).profit(eq("123"));
	}

	@Test
	void onEvent_missingEntities_noInteraction() {
		HfpayTradeRecordService tradeRecordService = mock(HfpayTradeRecordService.class);
		HfpayTradeRecordHfpayProfitSharingDispatchListener listener =
				new HfpayTradeRecordHfpayProfitSharingDispatchListener(tradeRecordService);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("other", "x");

		listener.onEvent(payload);

		verifyNoInteractions(tradeRecordService);
	}
}
