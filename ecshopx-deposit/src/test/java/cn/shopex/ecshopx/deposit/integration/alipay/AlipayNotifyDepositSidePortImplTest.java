package cn.shopex.ecshopx.deposit.integration.alipay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.deposit.domain.DepositTrade;
import cn.shopex.ecshopx.deposit.mapper.DepositTradeMapper;
import cn.shopex.ecshopx.deposit.service.AdapayDepositTradeRechargeCallbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlipayNotifyDepositSidePortImplTest {

	@Mock
	private DepositTradeMapper depositTradeMapper;

	@Mock
	private AdapayDepositTradeRechargeCallbackService adapayDepositTradeRechargeCallbackService;

	private AlipayNotifyDepositSidePortImpl port;

	@BeforeEach
	void setUp() {
		port = new AlipayNotifyDepositSidePortImpl(
				depositTradeMapper, adapayDepositTradeRechargeCallbackService);
	}

	@Test
	void requireCompanyIdByDepositTradeId_returnsOwningCompany() {
		DepositTrade row = new DepositTrade();
		row.setDepositTradeId("dep-1");
		row.setCompanyId("42");
		when(depositTradeMapper.selectById("dep-1")).thenReturn(row);

		assertThat(port.requireCompanyIdByDepositTradeId("dep-1")).isEqualTo(42L);
		verifyNoInteractions(adapayDepositTradeRechargeCallbackService);
	}

	@Test
	void requireCompanyIdByDepositTradeId_missingTrade_throwsBadRequest() {
		when(depositTradeMapper.selectById("missing")).thenReturn(null);

		assertThatThrownBy(() -> port.requireCompanyIdByDepositTradeId("missing"))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("储值单不存在");
	}
}
