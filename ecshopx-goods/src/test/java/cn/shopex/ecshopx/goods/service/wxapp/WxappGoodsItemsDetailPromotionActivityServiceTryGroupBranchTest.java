package cn.shopex.ecshopx.goods.service.wxapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRelTagsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsActivity;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsRelGoods;
import cn.shopex.ecshopx.promotions.mapper.LimitItemPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.LimitPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsRelGoodsMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillRelGoodsMapper;
import cn.shopex.ecshopx.promotions.service.PromotionGroupsActivityAdminRowAssembler;
import cn.shopex.ecshopx.promotions.service.PromotionGroupsTeamItemsListService;
import cn.shopex.ecshopx.promotions.service.SeckillActivityCreateService;
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

@ExtendWith(MockitoExtension.class)
class WxappGoodsItemsDetailPromotionActivityServiceTryGroupBranchTest {

	private static final long COMPANY_ID = 1L;
	private static final long ACT_ID = 42L;
	private static final int NOW = 1_700_000_000;

	@Mock
	private ItemsRepository itemsRepository;
	@Mock
	private SeckillRelGoodsMapper seckillRelGoodsMapper;
	@Mock
	private SeckillActivityMapper seckillActivityMapper;
	@Mock
	private WxappSeckillActivityItemStoreReadService wxappSeckillActivityItemStoreReadService;
	@Mock
	private PromotionGroupsActivityMapper promotionGroupsActivityMapper;
	@Mock
	private PromotionGroupsRelGoodsMapper promotionGroupsRelGoodsMapper;
	@Mock
	private PromotionGroupsTeamItemsListService promotionGroupsTeamItemsListService;
	@Mock
	private LimitItemPromotionsMapper limitItemPromotionsMapper;
	@Mock
	private LimitPromotionsMapper limitPromotionsMapper;
	@Mock
	private ItemsRelTagsRepository itemsRelTagsRepository;
	@Mock
	private SeckillActivityCreateService seckillActivityCreateService;
	@Mock
	private PromotionGroupsActivityAdminRowAssembler promotionGroupsActivityAdminRowAssembler;

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
						new ObjectMapper());
	}

	@Test
	void getCurrentActivityByItemId_groupBranch_buildsListFromRelGoodsForCandidateSkus() {
		long itemId = 100L;
		Items p1 = item(100L, 5000, "false", 900L);
		Items sku101 = item(101L, 6000, "false", 900L);
		Items sku102 = item(102L, 7000, "false", 900L);

		when(itemsRepository.getByItemIdAndCompany(itemId, COMPANY_ID)).thenReturn(p1);
		when(itemsRepository.listByDefaultItemIdAndCompany(900L, COMPANY_ID)).thenReturn(List.of(p1, sku101, sku102));
		when(seckillRelGoodsMapper.selectList(any())).thenReturn(List.of());

		PromotionGroupsRelGoods rel100 = rel(100L, 8800L, 10L);
		PromotionGroupsRelGoods rel101 = rel(101L, 9900L, 20L);
		when(promotionGroupsRelGoodsMapper.selectList(any()))
				.thenReturn(List.of(rel100, rel101))
				.thenReturn(List.of(rel100, rel101, rel(103L, 1111L, 5L)));

		PromotionGroupsActivity activity = activity(ACT_ID, 100L);
		when(promotionGroupsActivityMapper.selectList(any())).thenReturn(List.of(activity));
		when(itemsRepository.listByCompanyAndItemIdsPreservingOrder(eq(COMPANY_ID), any()))
				.thenReturn(List.of(p1, sku101));
		when(promotionGroupsActivityAdminRowAssembler.toRow(eq(activity), any(Integer.class)))
				.thenReturn(new LinkedHashMap<>(Map.of("groups_activity_id", ACT_ID)));

		Map<String, Object> out = service.getCurrentActivityByItemId(COMPANY_ID, itemId, 0L);

		assertNotNull(out);
		assertEquals("group", out.get("activity_type"));
		@SuppressWarnings("unchecked")
		Map<String, Map<String, Object>> list = (Map<String, Map<String, Object>>) out.get("list");
		assertEquals(2, list.size());
		assertNotNull(list.get("100"));
		assertNotNull(list.get("101"));
		assertNull(list.get("102"));
		assertEquals(8800L, list.get("100").get("activity_price"));
		assertEquals(9900L, list.get("101").get("activity_price"));
		assertEquals(10L, list.get("100").get("store"));
		assertEquals(20L, list.get("101").get("store"));
		assertEquals(5000, list.get("100").get("price"));
		assertEquals(6000, list.get("101").get("price"));
	}

	@Test
	void getCurrentActivityByItemId_groupBranch_fallsBackToActivityGoodsIdWhenNoRelRows() {
		long itemId = 200L;
		Items p1 = item(200L, 3000, "true", 0L);

		when(itemsRepository.getByItemIdAndCompany(itemId, COMPANY_ID)).thenReturn(p1);
		when(seckillRelGoodsMapper.selectList(any())).thenReturn(List.of());
		when(promotionGroupsRelGoodsMapper.selectList(any())).thenReturn(List.of());

		PromotionGroupsActivity activity = activity(99L, 200L);
		activity.setActPrice(2500L);
		activity.setStore(30L);
		when(promotionGroupsActivityMapper.selectList(any())).thenReturn(List.of(activity));
		when(promotionGroupsActivityAdminRowAssembler.toRow(eq(activity), any(Integer.class)))
				.thenReturn(new LinkedHashMap<>());

		Map<String, Object> out = service.getCurrentActivityByItemId(COMPANY_ID, itemId, 0L);

		assertNotNull(out);
		assertEquals("group", out.get("activity_type"));
		@SuppressWarnings("unchecked")
		Map<String, Map<String, Object>> list = (Map<String, Map<String, Object>>) out.get("list");
		assertEquals(1, list.size());
		assertEquals(2500L, list.get("200").get("activity_price"));
		assertEquals(30L, list.get("200").get("store"));
	}

	private static Items item(long itemId, int price, String nospec, long defaultItemId) {
		Items it = new Items();
		it.setItemId(itemId);
		it.setPrice(price);
		it.setNospec(nospec);
		it.setDefaultItemId(defaultItemId);
		it.setDistributorId(0);
		return it;
	}

	private static PromotionGroupsRelGoods rel(long itemId, long activityPrice, long activityStore) {
		PromotionGroupsRelGoods rel = new PromotionGroupsRelGoods();
		rel.setGroupsActivityId(ACT_ID);
		rel.setCompanyId(COMPANY_ID);
		rel.setItemId(itemId);
		rel.setActivityPrice(activityPrice);
		rel.setActivityStore(activityStore);
		return rel;
	}

	private static PromotionGroupsActivity activity(long actId, long goodsId) {
		PromotionGroupsActivity act = new PromotionGroupsActivity();
		act.setGroupsActivityId(actId);
		act.setCompanyId(COMPANY_ID);
		act.setGoodsId(goodsId);
		act.setDisabled(false);
		act.setBeginTime(1L);
		act.setEndTime(Long.MAX_VALUE);
		act.setCreated(NOW);
		act.setLimitBuyNum(2L);
		act.setRigUp(false);
		return act;
	}
}
