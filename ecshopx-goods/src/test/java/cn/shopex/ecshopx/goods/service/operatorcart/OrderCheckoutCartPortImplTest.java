package cn.shopex.ecshopx.goods.service.operatorcart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.shopex.ecshopx.goods.service.recommend.GoodsRecommendCheckoutMergeService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OrderCheckoutCartPortImplTest {

	@Test
	void mergeCheckoutRecommendRequestItems_appendsRecommendAtBottom() {
		List<Map<String, Object>> cartItems = new ArrayList<>();
		cartItems.add(line(7666L, 1));

		List<Map<String, Object>> requestRows = new ArrayList<>();
		requestRows.add(line(6753L, 1));

		OrderCheckoutCartPortImpl.mergeCheckoutRecommendRequestItems(cartItems, requestRows);

		assertEquals(2, cartItems.size());
		assertEquals(7666L, cartItems.get(0).get("item_id"));
		assertFalse(GoodsRecommendCheckoutMergeService.isRecommendLine(cartItems.get(0)));
		assertEquals(6753L, cartItems.get(1).get("item_id"));
		assertTrue(GoodsRecommendCheckoutMergeService.isRecommendLine(cartItems.get(1)));
	}

	@Test
	void markTrailingRecommendLines_flagsLastN() {
		List<Map<String, Object>> items = new ArrayList<>();
		items.add(line(2L, 1));
		items.add(line(5L, 1));
		items.add(line(7L, 1));

		OrderCheckoutCartPortImpl.markTrailingRecommendLines(items, 2);

		assertFalse(GoodsRecommendCheckoutMergeService.isRecommendLine(items.get(0)));
		assertTrue(GoodsRecommendCheckoutMergeService.isRecommendLine(items.get(1)));
		assertTrue(GoodsRecommendCheckoutMergeService.isRecommendLine(items.get(2)));
	}

	@Test
	void markTrailingRecommendLines_skipsWhenCountExceedsSize() {
		List<Map<String, Object>> items = new ArrayList<>();
		items.add(line(2L, 1));

		OrderCheckoutCartPortImpl.markTrailingRecommendLines(items, 2);

		assertFalse(GoodsRecommendCheckoutMergeService.isRecommendLine(items.get(0)));
	}

	@Test
	void mergeCheckoutRecommendRequestItems_sameItemId_keepsSeparateRecommendLine() {
		List<Map<String, Object>> cartItems = new ArrayList<>();
		cartItems.add(line(5L, 2));

		List<Map<String, Object>> requestRows = new ArrayList<>();
		requestRows.add(line(5L, 1));

		OrderCheckoutCartPortImpl.mergeCheckoutRecommendRequestItems(cartItems, requestRows);

		assertEquals(2, cartItems.size());
		assertEquals(5L, cartItems.get(0).get("item_id"));
		assertEquals(2, cartItems.get(0).get("num"));
		assertFalse(GoodsRecommendCheckoutMergeService.isRecommendLine(cartItems.get(0)));
		assertEquals(5L, cartItems.get(1).get("item_id"));
		assertEquals(1, cartItems.get(1).get("num"));
		assertTrue(GoodsRecommendCheckoutMergeService.isRecommendLine(cartItems.get(1)));
	}

	private static Map<String, Object> line(long itemId, int num) {
		Map<String, Object> line = new LinkedHashMap<>();
		line.put("item_id", itemId);
		line.put("num", num);
		line.put("activity_type", "normal");
		return line;
	}
}
