package cn.shopex.ecshopx.promotions.dispatch;

import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.promotions.service.membertask.MemberTaskProfileCompletionService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberTaskUpdateMemberSuccessDispatchListenerTest {

	@Mock
	private MemberTaskProfileCompletionService memberTaskProfileCompletionService;

	@Test
	void onEvent_delegatesToService() {
		MemberTaskUpdateMemberSuccessDispatchListener listener =
				new MemberTaskUpdateMemberSuccessDispatchListener(memberTaskProfileCompletionService);
		Map<String, Object> payload = Map.of("company_id", 1L, "user_id", 9L);
		listener.onEvent(payload);
		verify(memberTaskProfileCompletionService).apply(payload);
	}
}
