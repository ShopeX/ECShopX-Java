package cn.shopex.ecshopx.goods.service.operatorcart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OrderCheckoutCartPortImplTest {

	@Test
	void mergeCheckoutRecommendRequestItems_appendsLinesNotInCart() {
		List<Map<String, Object>> cartItems = new ArrayList<>();
		Map<String, Object> main = new LinkedHashMap<>();
		main.put("item_id", 7666L);
		main.put("num", 1);
		main.put("activity_type", "normal");
		cartItems.add(main);

		List<Map<String, Object>> requestRows = new ArrayList<>();
		requestRows.add(main);
		Map<String, Object> recommend = new LinkedHashMap<>();
		recommend.put("item_id", 6753L);
		recommend.put("num", 1);
		recommend.put("activity_type", "normal");
		requestRows.add(recommend);

		OrderCheckoutCartPortImpl.mergeCheckoutRecommendRequestItems(cartItems, requestRows);

		assertEquals(2, cartItems.size());
		assertEquals(6753L, cartItems.get(1).get("item_id"));
	}
}
