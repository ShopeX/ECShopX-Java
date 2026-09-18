package cn.shopex.ecshopx.companys.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.companys.service.setting.RechargeSettingRedisService;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FrontDepositPayEnabledPortImplTest {

	@Mock
	private RechargeSettingRedisService rechargeSettingRedisService;

	@InjectMocks
	private FrontDepositPayEnabledPortImpl port;

	@Test
	void enabledWhenRechargeStatusTrue() {
		when(rechargeSettingRedisService.handle(141L, Collections.emptyMap()))
				.thenReturn(Map.of("recharge_status", Boolean.TRUE));

		assertTrue(port.isDepositPayEnabled(141L));
		verify(rechargeSettingRedisService).handle(141L, Collections.emptyMap());
	}

	@Test
	void disabledWhenRechargeStatusFalse() {
		when(rechargeSettingRedisService.handle(141L, Collections.emptyMap()))
				.thenReturn(Map.of("recharge_status", Boolean.FALSE));

		assertFalse(port.isDepositPayEnabled(141L));
	}

	@Test
	void disabledWhenRechargeStatusMissing() {
		when(rechargeSettingRedisService.handle(141L, Collections.emptyMap())).thenReturn(Map.of());

		assertFalse(port.isDepositPayEnabled(141L));
	}
}
