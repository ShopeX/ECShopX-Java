package cn.shopex.ecshopx.promotions.dispatch;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import cn.shopex.ecshopx.promotions.service.CreateMemberSuccessSendMembercardExecutionService;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateMemberSuccessSendMembercardDispatchListenerTest {

	@Mock
	private CreateMemberSuccessSendMembercardExecutionService executionService;

	@InjectMocks
	private CreateMemberSuccessSendMembercardDispatchListener listener;

	@Test
	void onEvent_whenIfRegisterPromotionFalse_doesNotCallExecution() {
		Map<String, Object> payload = new HashMap<>();
		payload.put("if_register_promotion", false);
		payload.put("company_id", 1L);
		payload.put("user_id", 2L);
		payload.put("mobile", "13800138000");

		listener.onEvent(payload);

		verifyNoInteractions(executionService);
	}

	@Test
	void onEvent_whenIfRegisterPromotionTrue_delegatesToExecutionService() {
		Map<String, Object> payload = new HashMap<>();
		payload.put("if_register_promotion", true);
		payload.put("company_id", 7L);
		payload.put("user_id", 99L);
		payload.put("mobile", "13800138000");

		listener.onEvent(payload);

		verify(executionService).afterMemberCreateSendMembercard(eq(7L), eq(99L), eq("13800138000"));
	}
}
