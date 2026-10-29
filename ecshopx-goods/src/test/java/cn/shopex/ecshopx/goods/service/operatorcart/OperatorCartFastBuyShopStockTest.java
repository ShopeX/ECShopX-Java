package cn.shopex.ecshopx.goods.service.operatorcart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OperatorCartFastBuyShopStockTest {

	@Test
	void platformModelAlwaysZeroShopStock() {
		Map<String, Object> item = Map.of("store", 5, "is_total_store", false);
		assertEquals(0, OperatorCartFastBuyFacadeImpl.resolveShopStockForFastBuy(10L, "platform", item));
	}

	@Test
	void headquartersShipmentZeroShopStock() {
		Map<String, Object> item = Map.of("store", 5, "is_total_store", true);
		assertEquals(0, OperatorCartFastBuyFacadeImpl.resolveShopStockForFastBuy(10L, "standard", item));
	}

	@Test
	void storeShipmentUsesItemStore() {
		Map<String, Object> item = new HashMap<>();
		item.put("store", 4);
		item.put("is_total_store", false);
		assertEquals(4, OperatorCartFastBuyFacadeImpl.resolveShopStockForFastBuy(10L, "standard", item));
	}
}
