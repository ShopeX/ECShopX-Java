package cn.shopex.ecshopx.goods.service.cart.wxapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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

/**
 * POS 无会员（userId=0）场景（ECX-10719）：满减活动匹配、算价与分摊不依赖会员身份。
 * SkuValidMarketingActivityService 返回的活动直接参与 attachPromotions / computeTotals。
 */
@ExtendWith(MockitoExtension.class)
class WxappH5DistributorCartPromotionServicePosNoMemberTest {

	private static final long COMPANY_ID = 38L;
	private static final long USER_ID = 0L;
	private static final long SHOP_ID = 129L;
	private static final long ITEM_ID = 7666L;
	private static final long MARKETING_ID = 455L;

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
	}

	@Test
	void applyMarketingAndTotals_fullMinusAppliesForNoMemberUser() {
		when(shopMenuService.resolveProductModelKeyForCompany(COMPANY_ID)).thenReturn("bbc");
		when(skuValidMarketingActivityService.getValidMarketingActivityForCartItems(
						eq(COMPANY_ID), anyList(), eq(USER_ID), eq(SHOP_ID)))
				.thenReturn(List.of(fullMinusActivity()));
		when(marketingActivityApplyRulesService.applyActivityRules(
						eq(COMPANY_ID), eq(MARKETING_ID), eq(USER_ID), any()))
				.thenReturn(Map.of("discount_fee", 1000L, "discount_desc", Map.of("rule", "满1件减10元")));

		Map<String, Object> cartLine = posCartLine();
		Map<String, Object> totals =
				service.applyMarketingAndTotals(COMPANY_ID, USER_ID, List.of(cartLine), "pos");

		assertEquals(MARKETING_ID, ((Number) cartLine.get("activity_id")).longValue());
		assertEquals("1000", String.valueOf(cartLine.get("discount_fee")));
		assertEquals(1000L, ((Number) totals.get("discount_fee")).longValue());
		assertEquals(18900L, ((Number) totals.get("total_fee")).longValue());
		assertEquals(1, ((List<?>) totals.get("used_activity_ids")).size());
		assertEquals(1, ((List<?>) totals.get("activity_grouping")).size());
	}

	@Test
	void applyMarketingAndTotals_limitedTimeSaleRepricesForNoMemberUser() {
		when(wxappGoodsItemsDetailPromotionActivityService.getCurrentActivityByItemId(
						eq(COMPANY_ID), eq(ITEM_ID), eq(SHOP_ID), eq(USER_ID)))
				.thenReturn(limitedTimeSaleActivity());

		Map<String, Object> cartLine = posCartLine();
		Map<String, Object> totals = service.apply(COMPANY_ID, USER_ID, List.of(cartLine), "pos");

		assertEquals(Boolean.TRUE, cartLine.get("is_last_price"));
		assertEquals(19000, ((Number) cartLine.get("activity_price")).intValue());
		assertEquals(1000L, ((Number) totals.get("discount_fee")).longValue());
	}

	private static Map<String, Object> fullMinusActivity() {
		Map<String, Object> activity = new LinkedHashMap<>();
		activity.put("marketing_id", String.valueOf(MARKETING_ID));
		activity.put("marketing_type", "full_minus");
		activity.put("marketing_name", "全场任意商品，买1件下单立减10元，上不封顶");
		activity.put("promotion_tag", "满减");
		activity.put("condition_rules", "购买满1.0件，减10.00元;");
		activity.put("use_bound", 0);
		activity.put("join_limit", 0);
		activity.put("usedCount", 0);
		activity.put("shop_ids", List.of(String.valueOf(SHOP_ID)));
		return activity;
	}

	private static Map<String, Object> limitedTimeSaleActivity() {
		Map<String, Object> itemData = new LinkedHashMap<>();
		itemData.put("price", 20000);
		itemData.put("activity_price", 19000);
		itemData.put("limit_num", 0);

		Map<String, Object> info = new LinkedHashMap<>();
		info.put("seckill_id", 88L);
		info.put("activity_name", "周末秒杀");
		info.put("limit_money", 0L);
		info.put("limit_total_money", 0L);

		Map<String, Object> activityData = new LinkedHashMap<>();
		activityData.put("activity_type", "limited_time_sale");
		activityData.put("list", Map.of(String.valueOf(ITEM_ID), itemData));
		activityData.put("info", info);
		return activityData;
	}

	private static Map<String, Object> posCartLine() {
		Map<String, Object> line = new LinkedHashMap<>();
		line.put("cart_id", 631L);
		line.put("company_id", COMPANY_ID);
		line.put("user_id", USER_ID);
		line.put("shop_id", SHOP_ID);
		line.put("shop_type", "shop_offline");
		line.put("item_id", ITEM_ID);
		line.put("num", 1);
		line.put("price", 19900);
		line.put("total_fee", "19900");
		line.put("is_checked", true);
		line.put("item_type", "normal");
		return line;
	}
}
