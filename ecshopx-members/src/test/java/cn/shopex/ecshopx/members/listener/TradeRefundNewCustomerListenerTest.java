package cn.shopex.ecshopx.members.listener;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import cn.shopex.ecshopx.common.event.TradeRefundSpringEvent;
import cn.shopex.ecshopx.members.service.systemtag.MemberSystemTagCodes;
import cn.shopex.ecshopx.members.service.systemtag.NewCustomerTagReconcileService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TradeRefundNewCustomerListenerTest {

	@Mock
	private NewCustomerTagReconcileService reconcileService;

	@InjectMocks
	private TradeRefundNewCustomerListener listener;

	@Test
	void onRefund_success_reconciles() {
		listener.onTradeRefund(
				new TradeRefundSpringEvent(
						this, Map.of("company_id", 7L, "user_id", 99L, "refund_status", "SUCCESS")));
		verify(reconcileService).reconcile(7L, 99L, MemberSystemTagCodes.SOURCE_REFUND);
	}

	@Test
	void onRefund_notSuccess_skips() {
		listener.onTradeRefund(
				new TradeRefundSpringEvent(
						this, Map.of("company_id", 7L, "user_id", 99L, "refund_status", "READY")));
		verifyNoInteractions(reconcileService);
	}
}
