package cn.shopex.ecshopx.companys.service.operatorcart;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.shopex.ecshopx.common.exception.ResourceException;
import org.junit.jupiter.api.Test;

class OperatorFastBuyStockValidatorTest {

	@Test
	void rejectsWhenShopHasStock() {
		ResourceException ex =
				assertThrows(ResourceException.class, () -> OperatorFastBuyStockValidator.validate(2, 10, 1));
		assertTrue(ex.getMessage().contains("请从收银加购"));
	}

	@Test
	void rejectsWhenPlatformStockInsufficient() {
		ResourceException ex =
				assertThrows(ResourceException.class, () -> OperatorFastBuyStockValidator.validate(0, 1, 2));
		assertTrue(ex.getMessage().contains("库存不足"));
	}

	@Test
	void acceptsWhenShopEmptyAndPlatformEnough() {
		assertDoesNotThrow(() -> OperatorFastBuyStockValidator.validate(0, 5, 2));
	}

	@Test
	void skipGateAllowsShopStock() {
		assertDoesNotThrow(() -> OperatorFastBuyStockValidator.validate(3, 5, 1, true));
	}
}
