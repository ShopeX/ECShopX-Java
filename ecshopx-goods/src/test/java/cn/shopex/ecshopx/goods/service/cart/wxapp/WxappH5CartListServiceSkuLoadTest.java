package cn.shopex.ecshopx.goods.service.cart.wxapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.crossborder.mapper.CrossBorderSetMapper;
import cn.shopex.ecshopx.crossborder.mapper.OriginCountryMapper;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.distribution.service.HandleValidCartValidShopIdsService;
import cn.shopex.ecshopx.goods.service.distributor.DistributorItemSkuInfoForStoreService;
import cn.shopex.ecshopx.goods.service.distributor.DistributorItemsRelListCoreService;
import cn.shopex.ecshopx.goods.service.items.ItemAvailableStoreResolver;
import cn.shopex.ecshopx.goods.service.items.ItemLogisticsStoreEnricher;
import cn.shopex.ecshopx.goods.service.order.normal.GiftActivityStoreAdjustService;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsListQueryOrchestrator;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeUserVipGradeGetService;
import cn.shopex.ecshopx.orders.mapper.CartMapper;
import cn.shopex.ecshopx.promotions.service.CartMemberpreferenceValidationService;
import cn.shopex.ecshopx.promotions.service.ItemsTagActivityCheckService;
import cn.shopex.ecshopx.promotions.service.PackagePromotionFrontPackageInfoService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class WxappH5CartListServiceSkuLoadTest {

	@Mock private CartMapper cartMapper;
	@Mock private StringRedisTemplate stringRedisTemplate;
	@Mock private WxappGoodsItemsListQueryOrchestrator wxappGoodsItemsListQueryOrchestrator;
	@Mock private HandleValidCartValidShopIdsService handleValidCartValidShopIdsService;
	@Mock private DistributorListQueryService distributorListQueryService;
	@Mock private DistributorItemSkuInfoForStoreService distributorItemSkuInfoForStoreService;
	@Mock private DistributorItemsRelListCoreService distributorItemsRelListCoreService;
	@Mock private OriginCountryMapper originCountryMapper;
	@Mock private CrossBorderSetMapper crossBorderSetMapper;
	@Mock private PackagePromotionFrontPackageInfoService packagePromotionFrontPackageInfoService;
	@Mock private ItemsTagActivityCheckService itemsTagActivityCheckService;
	@Mock private WxappH5CartMemberPriceEnrichmentService wxappH5CartMemberPriceEnrichmentService;
	@Mock private WxappH5CartListTotalAndPromotionAggregator wxappH5CartListTotalAndPromotionAggregator;
	@Mock private WxappH5DistributorCartPromotionService wxappH5DistributorCartPromotionService;
	@Mock private WxappH5PackageCartSupport wxappH5PackageCartSupport;
	@Mock private PointsmallCartSkuLoadService pointsmallCartSkuLoadService;
	@Mock private VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService;
	@Mock private CartMemberpreferenceValidationService cartMemberpreferenceValidationService;
	@Mock private CheckoutCartLogisticsSupplierStoreClampService checkoutCartLogisticsSupplierStoreClampService;
	@Mock private CheckoutCartDistributorStoreOverlayService checkoutCartDistributorStoreOverlayService;
	@Mock private ItemLogisticsStoreEnricher itemLogisticsStoreEnricher;
	@Mock private GiftActivityStoreAdjustService giftActivityStoreAdjustService;
	@Mock private ItemAvailableStoreResolver itemAvailableStoreResolver;

	private WxappH5CartListService service;

	@BeforeEach
	void setUp() {
		service =
				new WxappH5CartListService(
						cartMapper,
						stringRedisTemplate,
						wxappGoodsItemsListQueryOrchestrator,
						handleValidCartValidShopIdsService,
						distributorListQueryService,
						distributorItemSkuInfoForStoreService,
						distributorItemsRelListCoreService,
						originCountryMapper,
						crossBorderSetMapper,
						packagePromotionFrontPackageInfoService,
						itemsTagActivityCheckService,
						wxappH5CartMemberPriceEnrichmentService,
						wxappH5CartListTotalAndPromotionAggregator,
						wxappH5DistributorCartPromotionService,
						wxappH5PackageCartSupport,
						pointsmallCartSkuLoadService,
						vipGradeUserVipGradeGetService,
						cartMemberpreferenceValidationService,
						checkoutCartLogisticsSupplierStoreClampService,
						checkoutCartDistributorStoreOverlayService,
						itemLogisticsStoreEnricher,
						giftActivityStoreAdjustService,
						itemAvailableStoreResolver);
		when(wxappGoodsItemsListQueryOrchestrator.querySkuItemsList(anyLong(), any(), anyList()))
				.thenReturn(Map.of("list", List.of(), "total_count", 0L));
	}

	@Test
	void loadSkuItemsForCount_distributorShop_passesIsCanSaleForStandardStoreRelJoin() {
		service.loadSkuItemsForCount(38L, 1170L, 311L, "distributor", List.of(7666L), 0, null, null);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<LinkedHashMap<String, Object>> queryCaptor = ArgumentCaptor.forClass(LinkedHashMap.class);
		verify(wxappGoodsItemsListQueryOrchestrator).querySkuItemsList(eq(38L), queryCaptor.capture(), eq(List.of()));
		LinkedHashMap<String, Object> query = queryCaptor.getValue();
		assertEquals(311L, ((Number) query.get("distributor_id")).longValue());
		assertTrue(Boolean.TRUE.equals(query.get("is_can_sale")));
	}

	@Test
	void loadSkuItemsForCount_communityShop_doesNotForceIsCanSale() {
		service.loadSkuItemsForCount(38L, 1170L, 311L, "community", List.of(7666L), 0, null, null);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<LinkedHashMap<String, Object>> queryCaptor = ArgumentCaptor.forClass(LinkedHashMap.class);
		verify(wxappGoodsItemsListQueryOrchestrator).querySkuItemsList(eq(38L), queryCaptor.capture(), eq(List.of()));
		LinkedHashMap<String, Object> query = queryCaptor.getValue();
		assertEquals(311L, ((Number) query.get("distributor_id")).longValue());
		assertTrue(!query.containsKey("is_can_sale"));
	}
}
