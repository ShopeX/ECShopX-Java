package cn.shopex.ecshopx.orders.service.alipay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.DistributionDistributorPeekMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.payment.OrdersTradePaymentCallbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlipayNotifyOrderSidePortImplTest {

	@Mock
	private TradeMapper tradeMapper;

	@Mock
	private DistributionDistributorPeekMapper distributionDistributorPeekMapper;

	@Mock
	private OrdersTradePaymentCallbackService ordersTradePaymentCallbackService;

	private AlipayNotifyOrderSidePortImpl port;

	@BeforeEach
	void setUp() {
		port =
				new AlipayNotifyOrderSidePortImpl(
						tradeMapper,
						distributionDistributorPeekMapper,
						ordersTradePaymentCallbackService);
	}

	@Test
	void requireCompanyIdByTradeId_returnsOwningCompany() {
		Trade row = new Trade();
		row.setTradeId("trade-1");
		row.setCompanyId("42");
		when(tradeMapper.selectById("trade-1")).thenReturn(row);

		assertThat(port.requireCompanyIdByTradeId("trade-1")).isEqualTo(42L);
		verifyNoInteractions(ordersTradePaymentCallbackService);
	}

	@Test
	void requireCompanyIdByTradeId_missingTrade_throwsBadRequest() {
		when(tradeMapper.selectById("missing")).thenReturn(null);

		assertThatThrownBy(() -> port.requireCompanyIdByTradeId("missing"))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("订单不存在");
	}

	@Test
	void requireCompanyIdByTradeId_invalidCompany_throwsBadRequest() {
		Trade row = new Trade();
		row.setTradeId("trade-bad");
		row.setCompanyId("0");
		when(tradeMapper.selectById("trade-bad")).thenReturn(row);

		assertThatThrownBy(() -> port.requireCompanyIdByTradeId("trade-bad"))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("订单企业编号无效");
	}
}
