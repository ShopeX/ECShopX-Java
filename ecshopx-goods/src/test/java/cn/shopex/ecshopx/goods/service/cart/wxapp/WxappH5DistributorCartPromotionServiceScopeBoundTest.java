package cn.shopex.ecshopx.goods.service.cart.wxapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsDetailPromotionActivityService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeUserVipGradeGetService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.promotions.service.LimitPersonBuyService;
import cn.shopex.ecshopx.promotions.service.MarketingActivityApplyRulesService;
import cn.shopex.ecshopx.promotions.service.SeckillUserBuysStoreService;
import cn.shopex.ecshopx.promotions.service.SkuValidMarketingActivityService;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class WxappH5DistributorCartPromotionServiceScopeBoundTest {

	private static final long COMPANY_ID = 38L;
	private static final long USER_ID = 261L;
	private static final long SHOP_ID = 310L;
	private static final long SKU_ITEM_ID = 8116L;
	private static final long MARKETING_ID = 449L;
	private static final long TAG_ID = 117L;

	@Mock private SkuValidMarketingActivityService skuValidMarketingActivityService;
	@Mock private MarketingActivityApplyRulesService marketingActivityApplyRulesService;
	@Mock private ShopMenuService shopMenuService;
	@Mock private WxappGoodsItemsDetailPromotionActivityService wxappGoodsItemsDetailPromotionActivityService;
	@Mock private MemberAccountService memberAccountService;
	@Mock private VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService;
	@Mock private LimitPersonBuyService limitPersonBuyService;
	@Mock private SeckillUserBuysStoreService seckillUserBuysStoreService;
	@Mock private MarketingActivityCatalogAccess marketingActivityCatalogAccess;
	@Mock private StringRedisTemplate stringRedisTemplate;

	private WxappH5DistributorCartPromotionService service;

	@BeforeEach
	void setUp() {
		service =
				new WxappH5DistributorCartPromotionService(
						skuValidMarketingActivityService,
						marketingActivityApplyRulesService,
						shopMenuService,
						wxappGoodsItemsDetailPromotionActivityService,
						memberAccountService,
						vipGradeUserVipGradeGetService,
						limitPersonBuyService,
						seckillUserBuysStoreService,
						marketingActivityCatalogAccess,
						stringRedisTemplate);
		when(shopMenuService.resolveProductModelKeyForCompany(COMPANY_ID)).thenReturn("bbc");
	}

	@Test
	void applyMarketingAndTotals_attachesFullDiscountWhenTagScopeHitsCatalog() {
		Map<String, Object> activity = tagScopedActivity("full_discount");
		when(skuValidMarketingActivityService.getValidMarketingActivityForCartItems(
						eq(COMPANY_ID), anyList(), eq(USER_ID), eq(SHOP_ID)))
				.thenReturn(List.of(activity));
		when(marketingActivityCatalogAccess.listMarketingIdsHitBySkuItem(
						eq(COMPANY_ID), eq(SKU_ITEM_ID), eq(List.of(MARKETING_ID)), anyInt()))
				.thenReturn(List.of(MARKETING_ID));
		when(marketingActivityApplyRulesService.applyActivityRules(
						eq(COMPANY_ID), eq(MARKETING_ID), eq(USER_ID), any()))
				.thenReturn(Map.of("discount_fee", 50L, "discount_desc", Map.of("rule", "满折")));

		Map<String, Object> cartLine = cartLine(SKU_ITEM_ID);
		Map<String, Object> totals = service.applyMarketingAndTotals(COMPANY_ID, USER_ID, List.of(cartLine), "h5");

		assertEquals(MARKETING_ID, longVal(cartLine.get("activity_id")));
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> promotions = (List<Map<String, Object>>) cartLine.get("promotions");
		assertNotNull(promotions);
		assertEquals(1, promotions.size());
		assertEquals(50L, longVal(totals.get("discount_fee")));
		verify(marketingActivityCatalogAccess)
				.listMarketingIdsHitBySkuItem(
						eq(COMPANY_ID), eq(SKU_ITEM_ID), eq(List.of(MARKETING_ID)), anyInt());
	}

	@Test
	void applyMarketingAndTotals_attachesFullGiftWhenTagScopeHitsCatalog() {
		Map<String, Object> activity = tagScopedActivity("full_gift");
		when(skuValidMarketingActivityService.getValidMarketingActivityForCartItems(
						eq(COMPANY_ID), anyList(), eq(USER_ID), eq(SHOP_ID)))
				.thenReturn(List.of(activity));
		when(marketingActivityCatalogAccess.listMarketingIdsHitBySkuItem(
						eq(COMPANY_ID), eq(SKU_ITEM_ID), eq(List.of(MARKETING_ID)), anyInt()))
				.thenReturn(List.of(MARKETING_ID));

		Map<String, Object> cartLine = cartLine(SKU_ITEM_ID);
		service.applyMarketingAndTotals(COMPANY_ID, USER_ID, List.of(cartLine), "h5");

		@SuppressWarnings("unchecked")
		List<Long> giftIds = (List<Long>) cartLine.get("full_gift_id");
		assertNotNull(giftIds);
		assertTrue(giftIds.contains(MARKETING_ID));
	}

	@Test
	void applyMarketingAndTotals_fallsBackToRelItemsMapForSpecifiedSku() {
		Map<String, Object> activity = tagScopedActivity("full_minus");
		activity.put("use_bound", 1);
		@SuppressWarnings("unchecked")
		Map<String, Object> items = (Map<String, Object>) activity.get("items");
		items.clear();
		items.put(String.valueOf(SKU_ITEM_ID), Map.of("item_id", SKU_ITEM_ID));
		when(skuValidMarketingActivityService.getValidMarketingActivityForCartItems(
						eq(COMPANY_ID), anyList(), eq(USER_ID), eq(SHOP_ID)))
				.thenReturn(List.of(activity));
		when(marketingActivityCatalogAccess.listMarketingIdsHitBySkuItem(
						eq(COMPANY_ID), eq(SKU_ITEM_ID), eq(List.of(MARKETING_ID)), anyInt()))
				.thenReturn(List.of());
		when(marketingActivityApplyRulesService.applyActivityRules(
						eq(COMPANY_ID), eq(MARKETING_ID), eq(USER_ID), any()))
				.thenReturn(Map.of("discount_fee", 20L, "discount_desc", Map.of()));

		Map<String, Object> cartLine = cartLine(SKU_ITEM_ID);
		service.applyMarketingAndTotals(COMPANY_ID, USER_ID, List.of(cartLine), "h5");

		assertEquals(MARKETING_ID, longVal(cartLine.get("activity_id")));
	}

	private static Map<String, Object> tagScopedActivity(String marketingType) {
		Map<String, Object> activity = new LinkedHashMap<>();
		activity.put("marketing_id", String.valueOf(MARKETING_ID));
		activity.put("marketing_type", marketingType);
		activity.put("marketing_name", "scope-test");
		activity.put("use_bound", 3);
		activity.put("join_limit", 0);
		activity.put("usedCount", 0);
		activity.put("shop_ids", List.of("all"));
		activity.put("source_id", "0");
		activity.put("items", new LinkedHashMap<>(Map.of(String.valueOf(TAG_ID), Map.of("item_id", TAG_ID))));
		return activity;
	}

	private static Map<String, Object> cartLine(long itemId) {
		Map<String, Object> line = new LinkedHashMap<>();
		line.put("cart_id", 9001L);
		line.put("item_id", itemId);
		line.put("shop_id", SHOP_ID);
		line.put("goods_id", itemId);
		line.put("num", 2);
		line.put("price", 100L);
		line.put("total_fee", "200");
		line.put("is_checked", true);
		line.put("discount_fee", 0L);
		return line;
	}

	private static long longVal(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v == null) {
			return 0L;
		}
		return Long.parseLong(v.toString().trim());
	}
}
