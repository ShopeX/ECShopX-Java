package cn.shopex.ecshopx.orders.dispatch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.orders.service.refund.OfflinePayTradeFinishCanceledNormalOrderRefundService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrdersTradeFinishOfflinePayCanceledNormalOrderRefundDispatchListenerTest {

	@Mock
	private OfflinePayTradeFinishCanceledNormalOrderRefundService delegate;

	@Test
	void onEvent_delegatesToServiceWithTradeRow() {
		OrdersTradeFinishOfflinePayCanceledNormalOrderRefundDispatchListener listener =
				new OrdersTradeFinishOfflinePayCanceledNormalOrderRefundDispatchListener(delegate);
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("trade_state", "SUCCESS");
		row.put("pay_type", "offline_pay");
		row.put("company_id", 1L);
		row.put("order_id", 2L);
		listener.onEvent(row);
		verify(delegate).executeIfApplicable(eq(row));
	}

	@Test
	void onEvent_ignoresNonSuccessTradeState() {
		OrdersTradeFinishOfflinePayCanceledNormalOrderRefundDispatchListener listener =
				new OrdersTradeFinishOfflinePayCanceledNormalOrderRefundDispatchListener(delegate);
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("trade_state", "NOTPAY");
		row.put("pay_type", "offline_pay");
		listener.onEvent(row);
		verify(delegate, never()).executeIfApplicable(any());
	}
}
