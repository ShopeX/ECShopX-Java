package cn.shopex.ecshopx.goods.service.wxapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRelTagsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.promotions.domain.SeckillActivity;
import cn.shopex.ecshopx.promotions.domain.SeckillRelGoods;
import cn.shopex.ecshopx.promotions.mapper.LimitItemPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.LimitPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsRelGoodsMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillRelGoodsMapper;
import cn.shopex.ecshopx.promotions.service.PromotionGroupsActivityAdminRowAssembler;
import cn.shopex.ecshopx.promotions.service.PromotionGroupsTeamItemsListService;
import cn.shopex.ecshopx.promotions.service.SeckillActivityCreateService;
import cn.shopex.ecshopx.promotions.service.systemtag.MemberCrowdMatcher;
import cn.shopex.ecshopx.promotions.service.wxapp.WxappSeckillActivityItemStoreReadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * PHP Align: CheckPromotionsValid::resolveActivitySourceDistributorId — standard 模式下详情店铺上下文
 * 替换后的 distributor_id 应与活动 source_id 比较，而不是商品归属 distributor_id。
 */
@ExtendWith(MockitoExtension.class)
class WxappGoodsItemsDetailPromotionActivityServiceSeckillShopContextTest {

	private static final long COMPANY_ID = 38L;
	private static final long ITEM_ID = 6745L;
	private static final long SHOP_ID = 129L;
	private static final long SECKILL_ID = 999L;

	@Mock private ItemsRepository itemsRepository;
	@Mock private SeckillRelGoodsMapper seckillRelGoodsMapper;
	@Mock private SeckillActivityMapper seckillActivityMapper;
	@Mock private WxappSeckillActivityItemStoreReadService wxappSeckillActivityItemStoreReadService;
	@Mock private PromotionGroupsActivityMapper promotionGroupsActivityMapper;
	@Mock private PromotionGroupsRelGoodsMapper promotionGroupsRelGoodsMapper;
	@Mock private PromotionGroupsTeamItemsListService promotionGroupsTeamItemsListService;
	@Mock private LimitItemPromotionsMapper limitItemPromotionsMapper;
	@Mock private LimitPromotionsMapper limitPromotionsMapper;
	@Mock private ItemsRelTagsRepository itemsRelTagsRepository;
	@Mock private SeckillActivityCreateService seckillActivityCreateService;
	@Mock private PromotionGroupsActivityAdminRowAssembler promotionGroupsActivityAdminRowAssembler;
	@Mock private MemberCrowdMatcher memberCrowdMatcher;

	private WxappGoodsItemsDetailPromotionActivityService service;

	@BeforeEach
	void setUp() {
		service =
				new WxappGoodsItemsDetailPromotionActivityService(
						itemsRepository,
						seckillRelGoodsMapper,
						seckillActivityMapper,
						wxappSeckillActivityItemStoreReadService,
						promotionGroupsActivityMapper,
						promotionGroupsRelGoodsMapper,
						promotionGroupsTeamItemsListService,
						limitItemPromotionsMapper,
						limitPromotionsMapper,
						itemsRelTagsRepository,
						seckillActivityCreateService,
						promotionGroupsActivityAdminRowAssembler,
						new ObjectMapper(),
						memberCrowdMatcher);
	}

	@Test
	void getCurrentActivityByItemId_limitedTimeSale_usesRequestShopIdAgainstSourceId_forMallItem() {
		Items mallItem = mallItem(ITEM_ID);
		when(itemsRepository.getByItemIdAndCompany(ITEM_ID, COMPANY_ID)).thenReturn(mallItem);
		when(itemsRepository.listByDefaultItemIdAndCompany(ITEM_ID, COMPANY_ID)).thenReturn(List.of(mallItem));

		SeckillRelGoods rel = relGoods(ITEM_ID, SECKILL_ID);
		when(seckillRelGoodsMapper.selectList(any())).thenReturn(List.of(rel));

		SeckillActivity act = shopLimitedTimeSale(SECKILL_ID, SHOP_ID, ",129,");
		when(seckillActivityMapper.selectById(SECKILL_ID)).thenReturn(act);
		when(memberCrowdMatcher.matchesLimitedTimeSale(anyLong(), anyLong(), anyString(), any(), any()))
				.thenReturn(true);
		when(seckillActivityCreateService.buildGoodsDetailSeckillActivityInfo(act))
				.thenReturn(new LinkedHashMap<>(Map.of("seckill_id", SECKILL_ID, "source_id", SHOP_ID)));

		Map<String, Object> out = service.getCurrentActivityByItemId(COMPANY_ID, ITEM_ID, SHOP_ID);

		assertNotNull(out);
		assertEquals("limited_time_sale", out.get("activity_type"));
	}

	@Test
	void getCurrentActivityByItemId_limitedTimeSale_rejectsWhenRequestShopIdDoesNotMatchSourceId() {
		Items mallItem = mallItem(ITEM_ID);
		when(itemsRepository.getByItemIdAndCompany(ITEM_ID, COMPANY_ID)).thenReturn(mallItem);
		when(itemsRepository.listByDefaultItemIdAndCompany(ITEM_ID, COMPANY_ID)).thenReturn(List.of(mallItem));

		SeckillRelGoods rel = relGoods(ITEM_ID, SECKILL_ID);
		when(seckillRelGoodsMapper.selectList(any())).thenReturn(List.of(rel));

		SeckillActivity act = shopLimitedTimeSale(SECKILL_ID, SHOP_ID, ",129,");
		when(seckillActivityMapper.selectById(SECKILL_ID)).thenReturn(act);
		when(memberCrowdMatcher.matchesLimitedTimeSale(anyLong(), anyLong(), anyString(), any(), any()))
				.thenReturn(true);
		when(seckillActivityCreateService.buildGoodsDetailSeckillActivityInfo(eq(act)))
				.thenReturn(new LinkedHashMap<>(Map.of("seckill_id", SECKILL_ID, "source_id", SHOP_ID)));

		assertNull(service.getCurrentActivityByItemId(COMPANY_ID, ITEM_ID, 999L));
	}

	private static Items mallItem(long itemId) {
		Items it = new Items();
		it.setItemId(itemId);
		it.setPrice(1200000);
		it.setNospec("false");
		it.setDefaultItemId(itemId);
		it.setDistributorId(0);
		it.setItemSource("mall");
		return it;
	}

	private static SeckillRelGoods relGoods(long itemId, long seckillId) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		SeckillRelGoods rel = new SeckillRelGoods();
		rel.setId(1L);
		rel.setSeckillId(seckillId);
		rel.setItemId(itemId);
		rel.setCompanyId(COMPANY_ID);
		rel.setSeckillType("limited_time_sale");
		rel.setDisabled(false);
		rel.setActivityReleaseTime(now - 100);
		rel.setActivityEndTime(now + 3600);
		rel.setActivityPrice(100);
		rel.setLimitNum(1);
		rel.setSalesStore(0);
		return rel;
	}

	private static SeckillActivity shopLimitedTimeSale(long seckillId, long sourceId, String distributorCsv) {
		SeckillActivity act = new SeckillActivity();
		act.setSeckillId(seckillId);
		act.setCompanyId(COMPANY_ID);
		act.setSeckillType("limited_time_sale");
		act.setSourceId(sourceId);
		act.setDistributorId(distributorCsv);
		return act;
	}
}
