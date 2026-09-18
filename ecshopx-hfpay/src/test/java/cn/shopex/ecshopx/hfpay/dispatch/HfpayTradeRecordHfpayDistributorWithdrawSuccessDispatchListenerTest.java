package cn.shopex.ecshopx.hfpay.dispatch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.hfpay.service.HfpayTradeRecordService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HfpayTradeRecordHfpayDistributorWithdrawSuccessDispatchListenerTest {

	@Mock
	private HfpayTradeRecordService hfpayTradeRecordService;

	@InjectMocks
	private HfpayTradeRecordHfpayDistributorWithdrawSuccessDispatchListener listener;

	@Test
	void onEvent_mapsEntitiesToWithdraw_usingTransAmtFromPayload() {
		LinkedHashMap<String, Object> entities = new LinkedHashMap<>();
		entities.put("hfpay_cash_record_id", 1L);
		entities.put("company_id", 11L);
		entities.put("distributor_id", 22L);
		entities.put("trans_amt", 1000);
		entities.put("order_id", "ORD-1");
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		listener.onEvent(payload);

		verify(hfpayTradeRecordService).withdraw(eq(11L), eq(22L), eq(1000), eq("ORD-1"));
	}

	@Test
	void onEvent_skipsWhenTransAmtMissingOrInvalid() {
		LinkedHashMap<String, Object> entities = new LinkedHashMap<>();
		entities.put("company_id", 1L);
		entities.put("distributor_id", 2L);
		entities.put("order_id", "O");
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		listener.onEvent(payload);

		verify(hfpayTradeRecordService, never()).withdraw(anyLong(), anyLong(), anyInt(), any());
	}
}
