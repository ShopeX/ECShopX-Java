package cn.shopex.ecshopx.promotions.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateMemberSuccessRegisterPointExecutionServiceTest {

	@Mock
	private RegisterPointConfigService registerPointConfigService;

	@Mock
	private PointMemberAddPointService pointMemberAddPointService;

	@InjectMocks
	private CreateMemberSuccessRegisterPointExecutionService executionService;

	@Test
	void apply_readsRegisterPointConfigWithPointTypeKey() {
		Map<String, Object> config = new LinkedHashMap<>();
		config.put("is_open", "true");
		config.put("point", 100);
		when(registerPointConfigService.getRegisterPointConfig(38L, "point")).thenReturn(config);

		Map<String, Object> payload = Map.of("company_id", 38L, "user_id", 1172L);
		executionService.apply(payload);

		verify(registerPointConfigService).getRegisterPointConfig(38L, "point");
		verify(pointMemberAddPointService).addPointForMemberRegisterGift(1172L, 38L, 100);
	}

	@Test
	void apply_whenConfigClosed_doesNotAddPoints() {
		Map<String, Object> config = new LinkedHashMap<>();
		config.put("is_open", false);
		config.put("point", 100);
		when(registerPointConfigService.getRegisterPointConfig(38L, "point")).thenReturn(config);

		executionService.apply(Map.of("company_id", 38L, "user_id", 1172L));

		verify(registerPointConfigService).getRegisterPointConfig(38L, "point");
		verifyNoInteractions(pointMemberAddPointService);
	}
}
