package cn.shopex.ecshopx.goods.service.cart.salesperson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsDetailPromotionActivityService;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsListMemberPriceApplyService;
import cn.shopex.ecshopx.promotions.service.GoodsItemsListPromotionEnrichmentService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * POS 购物车促销汇总合并（ECX-10719）：totals 覆盖 shop 桶硬编码字段，
 * 满赠/加价购字段不被覆盖（POS 只做满减、满折、限时特惠）。
 */
class SalespersonDistributorCartFormatAndTotalServiceMergePromotionTotalsTest {

	private static final long COMPANY_ID = 38L;
	private static final long SHOP_ID = 129L;

	private SalespersonDistributorCartFormatAndTotalService service;

	@BeforeEach
	void setUp() {
		service =
				new SalespersonDistributorCartFormatAndTotalService(
						mock(WxappGoodsItemsListMemberPriceApplyService.class),
						mock(GoodsItemsListPromotionEnrichmentService.class),
						mock(WxappGoodsItemsDetailPromotionActivityService.class),
						mock(DistributorListQueryService.class));
	}

	@Test
	void mergePromotionTotals_overwritesShopTotalsAndPromotionFields() {
		List<Map<String, Object>> shopBlocks = new ArrayList<>();
		shopBlocks.add(shopBlock());

		Map<String, Object> totals = baseTotals();
		service.mergePromotionTotals(shopBlocks, totals);

		Map<String, Object> shop = shopBlocks.get(0);
		assertEquals(19900L, longVal(shop.get("item_fee")));
		assertEquals(1000L, longVal(shop.get("discount_fee")));
		assertEquals(18900L, longVal(shop.get("total_fee")));
		assertEquals(19900L, longVal(shop.get("cart_total_price")));
		assertEquals(1, intVal(shop.get("cart_total_num")));
		assertEquals(1, intVal(shop.get("cart_total_count")));
		assertSame(totals.get("used_activity"), shop.get("used_activity"));
		assertSame(totals.get("used_activity_ids"), shop.get("used_activity_ids"));
		assertSame(totals.get("activity_grouping"), shop.get("activity_grouping"));
	}

	@Test
	void mergePromotionTotals_skipsGiftAndPlusBuyFields() {
		List<Map<String, Object>> shopBlocks = new ArrayList<>();
		shopBlocks.add(shopBlock());

		Map<String, Object> totals = baseTotals();
		totals.put("gift_activity", List.of(Map.of("activity_id", 501L)));
		totals.put("plus_buy_activity", List.of(Map.of("activity_id", 601L)));

		service.mergePromotionTotals(shopBlocks, totals);

		Map<String, Object> shop = shopBlocks.get(0);
		assertEquals(List.of(), shop.get("gift_activity"));
		assertEquals(List.of(), shop.get("plus_buy_activity"));
	}

	@Test
	void mergePromotionTotals_noopWhenTotalsNullOrBlocksEmpty() {
		List<Map<String, Object>> shopBlocks = new ArrayList<>();
		shopBlocks.add(shopBlock());

		service.mergePromotionTotals(shopBlocks, null);
		service.mergePromotionTotals(new ArrayList<>(), baseTotals());

		assertEquals(0L, longVal(shopBlocks.get(0).get("discount_fee")));
	}

	private static Map<String, Object> shopBlock() {
		Map<String, Object> shop = new LinkedHashMap<>();
		shop.put("shop_id", SHOP_ID);
		shop.put("shop_type", "distributor");
		shop.put("item_fee", 0L);
		shop.put("cart_total_price", 0L);
		shop.put("cart_total_num", 0);
		shop.put("cart_total_count", 0);
		shop.put("discount_fee", 0L);
		shop.put("total_fee", 0L);
		shop.put("used_activity", List.of());
		shop.put("used_activity_ids", List.of());
		shop.put("activity_grouping", List.of());
		shop.put("gift_activity", List.of());
		shop.put("plus_buy_activity", List.of());
		return shop;
	}

	private static Map<String, Object> baseTotals() {
		Map<String, Object> totals = new LinkedHashMap<>();
		totals.put("item_fee", 19900L);
		totals.put("discount_fee", 1000L);
		totals.put("total_fee", 18900L);
		totals.put("cart_total_price", 19900L);
		totals.put("cart_total_num", 1);
		totals.put("cart_total_count", 1);
		totals.put(
				"used_activity",
				List.of(Map.of("activity_id", 455L, "activity_name", "全场任意商品，买1件下单立减10元，上不封顶")));
		totals.put("used_activity_ids", List.of(455L));
		totals.put(
				"activity_grouping",
				List.of(Map.of(
						"activity_id", 455L,
						"activity_name", "全场任意商品，买1件下单立减10元，上不封顶",
						"activity_tag", "满减",
						"activity_type", "full_minus",
						"discount_fee", "1000")));
		return totals;
	}

	private static long longVal(Object v) {
		return ((Number) v).longValue();
	}

	private static int intVal(Object v) {
		return ((Number) v).intValue();
	}
}
