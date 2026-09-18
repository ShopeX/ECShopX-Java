package cn.shopex.ecshopx.hfpay.dispatch;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.hfpay.service.HfpayTradeRecordService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HfpayTradeRecordHfpayRefundSuccessDispatchListenerTest {

	@Test
	void onEvent_delegatesToRefundSuccess_withParsedIds() {
		HfpayTradeRecordService tradeRecordService = mock(HfpayTradeRecordService.class);
		HfpayTradeRecordHfpayRefundSuccessDispatchListener listener =
				new HfpayTradeRecordHfpayRefundSuccessDispatchListener(tradeRecordService);
		listener.onEvent(Map.of("order_id", "1", "refund_bn", 2L));
		verify(tradeRecordService).refundSuccess(eq("1"), eq(2L));
	}
}
