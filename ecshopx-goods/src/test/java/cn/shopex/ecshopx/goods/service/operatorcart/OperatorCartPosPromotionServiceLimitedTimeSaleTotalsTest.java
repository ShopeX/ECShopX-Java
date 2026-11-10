package cn.shopex.ecshopx.goods.service.operatorcart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import cn.shopex.ecshopx.goods.service.cart.salesperson.SalespersonDistributorCartFormatAndTotalService;
import cn.shopex.ecshopx.goods.service.cart.wxapp.WxappH5DistributorCartPromotionService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * POS 限时特惠聚合补条目（ECX-10719）：行级 limitedTimeSaleAct 按 seckill_id 聚合后
 * 同步追加进 totals 的 used_activity / used_activity_ids / activity_grouping。
 * 仅 POS 链路调用，不影响小程序、导购端的现有产出。
 */
class OperatorCartPosPromotionServiceLimitedTimeSaleTotalsTest {

	private static final long SECKILL_ID = 88L;

	private OperatorCartPosPromotionService service;

	@BeforeEach
	void setUp() {
		service =
				new OperatorCartPosPromotionService(
						mock(WxappH5DistributorCartPromotionService.class),
						mock(SalespersonDistributorCartFormatAndTotalService.class));
	}

	@Test
	void appendLimitedTimeSaleTotals_singleLineAppendsOneGroupingEntry() {
		Map<String, Object> totals = baseTotals();
		List<Map<String, Object>> lines =
				List.of(limitedTimeSaleLine(7001L, SECKILL_ID, "周末秒杀", "1000"));

		service.appendLimitedTimeSaleTotals(totals, lines);

		assertEquals(1, size(totals.get("used_activity")));
		assertEquals(1, size(totals.get("used_activity_ids")));
		assertEquals(1, size(totals.get("activity_grouping")));
		assertEquals(88L, longVal(firstId(totals.get("used_activity_ids"))));

		Map<?, ?> usedActivity = firstEntry(totals.get("used_activity"));
		assertEquals(88L, longVal(usedActivity.get("activity_id")));
		assertEquals("周末秒杀", usedActivity.get("activity_name"));

		Map<?, ?> grouping = firstEntry(totals.get("activity_grouping"));
		assertEquals(88L, longVal(grouping.get("activity_id")));
		assertEquals("周末秒杀", grouping.get("activity_name"));
		assertEquals("限时优惠", grouping.get("activity_tag"));
		assertEquals("limited_time_sale", grouping.get("activity_type"));
		assertEquals("1000", grouping.get("discount_fee"));
		assertEquals(List.of(7001L), grouping.get("cart_ids"));
	}

	@Test
	void appendLimitedTimeSaleTotals_sameActivityMergesDiscountAndCartIds() {
		Map<String, Object> totals = baseTotals();
		List<Map<String, Object>> lines =
				List.of(
						limitedTimeSaleLine(7001L, SECKILL_ID, "周末秒杀", "1000"),
						limitedTimeSaleLine(7002L, SECKILL_ID, "周末秒杀", "500"));

		service.appendLimitedTimeSaleTotals(totals, lines);

		assertEquals(1, size(totals.get("activity_grouping")));
		Map<?, ?> grouping = firstEntry(totals.get("activity_grouping"));
		assertEquals("1500", grouping.get("discount_fee"));
		assertEquals(List.of(7001L, 7002L), grouping.get("cart_ids"));
		assertEquals(1, size(totals.get("used_activity")));
		assertEquals(1, size(totals.get("used_activity_ids")));
	}

	@Test
	void appendLimitedTimeSaleTotals_noLimitedTimeSaleLinesKeepsTotalsUnchanged() {
		Map<String, Object> totals = baseTotals();
		Map<String, Object> plainLine = new LinkedHashMap<>();
		plainLine.put("cart_id", 7003L);
		plainLine.put("item_id", 8116L);
		plainLine.put("num", 1);

		service.appendLimitedTimeSaleTotals(totals, List.of(plainLine));

		assertEquals(0, size(totals.get("used_activity")));
		assertEquals(0, size(totals.get("used_activity_ids")));
		assertEquals(0, size(totals.get("activity_grouping")));
	}

	@Test
	void appendLimitedTimeSaleTotals_toleratesImmutableTotalsLists() {
		Map<String, Object> totals = new LinkedHashMap<>();
		totals.put("used_activity", List.of());
		totals.put("used_activity_ids", List.of());
		totals.put("activity_grouping", List.of());
		List<Map<String, Object>> lines =
				List.of(limitedTimeSaleLine(7001L, SECKILL_ID, "周末秒杀", "1000"));

		service.appendLimitedTimeSaleTotals(totals, lines);

		assertEquals(1, size(totals.get("activity_grouping")));
		assertTrue(totals.get("activity_grouping") instanceof ArrayList);
	}

	private static Map<String, Object> baseTotals() {
		Map<String, Object> totals = new LinkedHashMap<>();
		totals.put("item_fee", 19900L);
		totals.put("discount_fee", 1000L);
		totals.put("total_fee", 18900L);
		totals.put("cart_total_price", 19900L);
		totals.put("cart_total_num", 1);
		totals.put("cart_total_count", 1);
		totals.put("used_activity", new ArrayList<Map<String, Object>>());
		totals.put("used_activity_ids", new ArrayList<Long>());
		totals.put("activity_grouping", new ArrayList<Map<String, Object>>());
		totals.put("gift_activity", List.of());
		totals.put("plus_buy_activity", List.of());
		return totals;
	}

	private static Map<String, Object> limitedTimeSaleLine(
			long cartId, long seckillId, String activityName, String lineDiscountFee) {
		Map<String, Object> act = new LinkedHashMap<>();
		act.put("activity_id", seckillId);
		act.put("marketing_type", "limited_time_sale");
		act.put("marketing_name", activityName);
		act.put("promotion_tag", "限时优惠");

		Map<String, Object> line = new LinkedHashMap<>();
		line.put("cart_id", cartId);
		line.put("item_id", 8116L);
		line.put("num", 1);
		line.put("is_checked", true);
		line.put("price", 20000);
		line.put("activity_price", 19000);
		line.put("total_fee", "19000");
		line.put("limitedTimeSaleAct", act);
		line.put(
				"activity_info",
				List.of(Map.of(
						"type", "limited_time_sale",
						"id", seckillId,
						"rule", "限时优惠",
						"info", activityName,
						"discount_fee", lineDiscountFee)));
		return line;
	}

	private static int size(Object raw) {
		return raw instanceof List<?> l ? l.size() : 0;
	}

	private static Object firstId(Object raw) {
		return raw instanceof List<?> l && !l.isEmpty() ? l.get(0) : null;
	}

	@SuppressWarnings("unchecked")
	private static <T> T firstEntry(Object raw) {
		return (T) ((List<?>) raw).get(0);
	}

	private static long longVal(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v));
	}
}
