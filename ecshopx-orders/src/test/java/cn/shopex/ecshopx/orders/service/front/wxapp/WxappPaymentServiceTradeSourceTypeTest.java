package cn.shopex.ecshopx.orders.service.front.wxapp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WxappPaymentServiceTradeSourceTypeTest {

	@Test
	void resolveTradeSourceType_groupsOrder_matchesPhpConcatenation() {
		assertThat(WxappPaymentService.resolveTradeSourceType("normal", "groups"))
				.isEqualTo("normal_groups");
	}

	@Test
	void resolveTradeSourceType_onlyOrderType_fallsBackToOrderType() {
		assertThat(WxappPaymentService.resolveTradeSourceType("normal", "")).isEqualTo("normal");
	}

	@Test
	void resolveTradeSourceType_empty_returnsOrderPay() {
		assertThat(WxappPaymentService.resolveTradeSourceType("", "")).isEqualTo("order_pay");
	}
}
