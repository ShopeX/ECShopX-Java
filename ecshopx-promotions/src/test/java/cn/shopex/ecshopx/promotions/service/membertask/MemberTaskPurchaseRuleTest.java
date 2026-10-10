package cn.shopex.ecshopx.promotions.service.membertask;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MemberTaskPurchaseRuleTest {

	@Test
	void matchesRemainingAfterRefund_partialStillMeetsThreshold() {
		MemberTaskPurchaseRule rule = new MemberTaskPurchaseRule(true, 100L);
		assertThat(rule.matchesRemainingAfterRefund(200L, 50L)).isTrue();
	}

	@Test
	void matchesRemainingAfterRefund_partialBelowThreshold() {
		MemberTaskPurchaseRule rule = new MemberTaskPurchaseRule(true, 100L);
		assertThat(rule.matchesRemainingAfterRefund(200L, 150L)).isFalse();
	}
}
