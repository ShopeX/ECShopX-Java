package cn.shopex.ecshopx.members.dispatch;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import cn.shopex.ecshopx.members.service.systemtag.MemberSystemTagCodes;
import cn.shopex.ecshopx.members.service.systemtag.NewCustomerTagReconcileService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateMemberSuccessNewCustomerDispatchListenerTest {

	@Mock
	private NewCustomerTagReconcileService reconcileService;

	@InjectMocks
	private CreateMemberSuccessNewCustomerDispatchListener listener;

	@Test
	void onEvent_reconcilesRegister() {
		listener.onEvent(Map.of("company_id", 7L, "user_id", 99L));
		verify(reconcileService).reconcile(7L, 99L, MemberSystemTagCodes.SOURCE_REGISTER);
	}

	@Test
	void onEvent_skipsInvalidIds() {
		listener.onEvent(Map.of("company_id", 0L, "user_id", 99L));
		verifyNoInteractions(reconcileService);
	}
}
