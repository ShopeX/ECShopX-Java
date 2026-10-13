package cn.shopex.ecshopx.promotions.service.membertask;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MemberTaskAttributedMetricsDisplayTest {

	@Test
	void spendYuanText_formatsFen() {
		assertThat(MemberTaskAttributedMetricsDisplay.spendYuanText(0L)).isEqualTo("0");
		assertThat(MemberTaskAttributedMetricsDisplay.spendYuanText(100L)).isEqualTo("1");
		assertThat(MemberTaskAttributedMetricsDisplay.spendYuanText(150L)).isEqualTo("1.5");
	}
}
