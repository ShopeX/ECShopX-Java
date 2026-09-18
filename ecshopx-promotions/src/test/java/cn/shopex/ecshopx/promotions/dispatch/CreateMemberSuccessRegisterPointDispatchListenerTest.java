package cn.shopex.ecshopx.promotions.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.promotions.service.CreateMemberSuccessRegisterPointExecutionService;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateMemberSuccessRegisterPointDispatchListenerTest {

	@Mock
	private CreateMemberSuccessRegisterPointExecutionService executionService;

	@InjectMocks
	private CreateMemberSuccessRegisterPointDispatchListener listener;

	@Test
	void onEvent_delegatesToExecutionServiceWithSamePayload() {
		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 7L);
		payload.put("user_id", 99L);

		listener.onEvent(payload);

		verify(executionService).apply(payload);
		assertEquals(7L, payload.get("company_id"));
		assertEquals(99L, payload.get("user_id"));
	}
}
