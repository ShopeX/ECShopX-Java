package cn.shopex.ecshopx.companys.service.operatorcart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OperatorShopFastBuyRedisServiceTest {

	@Test
	void buildKeyMatchesPhpShape() {
		OperatorShopFastBuyRedisService svc = new OperatorShopFastBuyRedisService(null, null);
		String key = svc.buildKey(1L, 2L, 3L);
		assertTrue(key.startsWith("shop_fastbuy:"));
		assertEquals(40, key.substring("shop_fastbuy:".length()).length());
	}
}
