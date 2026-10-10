package cn.shopex.ecshopx.promotions.dispatch;

import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskGrantTrigger;
import cn.shopex.ecshopx.promotions.service.membertask.MemberTaskRewardGrantService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberTaskNormalOrderConfirmReceiptDispatchListenerTest {

	@Mock
	private MemberTaskRewardGrantService memberTaskRewardGrantService;

	@InjectMocks
	private MemberTaskNormalOrderConfirmReceiptDispatchListener listener;

	@Test
	void onEvent_delegatesToGrantPendingIfOrderDone() {
		listener.onEvent(Map.of("company_id", 1L, "order_id", 9L));
		verify(memberTaskRewardGrantService)
				.grantPendingIfOrderDone(1L, 9L, MemberTaskGrantTrigger.CONFIRM_RECEIPT);
	}
}
