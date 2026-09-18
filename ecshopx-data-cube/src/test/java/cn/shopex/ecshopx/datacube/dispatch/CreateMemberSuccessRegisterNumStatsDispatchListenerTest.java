package cn.shopex.ecshopx.datacube.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.datacube.service.CreateMemberSuccessRegisterNumStatsExecutionService;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateMemberSuccessRegisterNumStatsDispatchListenerTest {

	@Mock
	private CreateMemberSuccessRegisterNumStatsExecutionService executionService;

	@InjectMocks
	private CreateMemberSuccessRegisterNumStatsDispatchListener listener;

	@Test
	void onEvent_delegatesToExecutionServiceWithSamePayload() {
		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 11L);
		payload.put("monitor_id", 22L);
		payload.put("source_id", 33L);
		payload.put("user_id", 44L);

		listener.onEvent(payload);

		verify(executionService).apply(payload);
		assertEquals(11L, payload.get("company_id"));
		assertEquals(22L, payload.get("monitor_id"));
		assertEquals(33L, payload.get("source_id"));
		assertEquals(44L, payload.get("user_id"));
	}
}
