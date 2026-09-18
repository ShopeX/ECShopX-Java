package cn.shopex.ecshopx.promotions.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MemberTierDiscountPriceTest {

	@Test
	void subFenDiscountTruncatesDiscountAmountBeforeSubtract() {
		assertEquals(0, MemberTierDiscountPrice.discountPerUnitFen(1, 90));
		assertEquals(1, MemberTierDiscountPrice.memberPriceFromTierDiscount(1, 90));
	}

	@Test
	void normalPricesMatchLegacyPhpFormula() {
		assertEquals(9, MemberTierDiscountPrice.discountPerUnitFen(10, 90));
		assertEquals(1, MemberTierDiscountPrice.memberPriceFromTierDiscount(10, 90));
		assertEquals(90, MemberTierDiscountPrice.discountPerUnitFen(100, 90));
		assertEquals(10, MemberTierDiscountPrice.memberPriceFromTierDiscount(100, 90));
	}
}
