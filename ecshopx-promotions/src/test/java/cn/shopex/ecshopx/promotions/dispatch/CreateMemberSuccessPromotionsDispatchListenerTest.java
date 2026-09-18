package cn.shopex.ecshopx.promotions.dispatch;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.promotions.service.CreateMemberSuccessRegisterPromotionExecutionService;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateMemberSuccessPromotionsDispatchListenerTest {

	@Mock
	private CreateMemberSuccessRegisterPromotionExecutionService executionService;

	@InjectMocks
	private CreateMemberSuccessPromotionsDispatchListener listener;

	@Test
	void onEvent_whenIfRegisterPromotionFalse_doesNotCallExecution() {
		Map<String, Object> payload = basePayload();
		payload.put("if_register_promotion", false);
		listener.onEvent(payload);
		verifyNoInteractions(executionService);
	}

	@Test
	void onEvent_whenIfRegisterPromotionNull_doesNotCallExecution() {
		Map<String, Object> payload = basePayload();
		payload.put("if_register_promotion", null);
		listener.onEvent(payload);
		verifyNoInteractions(executionService);
	}

	@Test
	void onEvent_whenIfRegisterPromotionMissing_doesNotCallExecution() {
		Map<String, Object> payload = basePayload();
		payload.remove("if_register_promotion");
		listener.onEvent(payload);
		verifyNoInteractions(executionService);
	}

	@Test
	void onEvent_whenIfRegisterPromotionTrue_delegatesToExecutionService() {
		Map<String, Object> payload = basePayload();
		payload.put("if_register_promotion", true);
		listener.onEvent(payload);
		verify(executionService).executionMarketingAfterMemberCreate(7L, 3L, 99L, "13800138000");
	}

	private static Map<String, Object> basePayload() {
		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 7L);
		payload.put("distributor_id", 3L);
		payload.put("user_id", 99L);
		payload.put("mobile", "13800138000");
		payload.put("if_register_promotion", true);
		return payload;
	}
}
