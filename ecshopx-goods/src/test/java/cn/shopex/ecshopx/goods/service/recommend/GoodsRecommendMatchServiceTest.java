package cn.shopex.ecshopx.goods.service.recommend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.recommend.GoodsRecommendRuleMainItem;
import cn.shopex.ecshopx.goods.domain.recommend.GoodsRecommendRuleRecommendItem;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.mapper.recommend.GoodsRecommendRuleMainItemMapper;
import cn.shopex.ecshopx.goods.mapper.recommend.GoodsRecommendRuleRecommendItemMapper;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsListMemberPriceApplyService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoodsRecommendMatchServiceTest {

	@Mock
	private GoodsRecommendDisplaySettingService displaySettingService;

	@Mock
	private GoodsRecommendRuleMainItemMapper mainItemMapper;

	@Mock
	private GoodsRecommendRuleRecommendItemMapper recommendItemMapper;

	@Mock
	private ItemsMapper itemsMapper;

	@Mock
	private GoodsRecommendSalabilityResolver salabilityResolver;

	@Mock
	private WxappGoodsItemsListMemberPriceApplyService wxappGoodsItemsListMemberPriceApplyService;

	private GoodsRecommendGoodsIdResolver goodsIdResolver;

	private GoodsRecommendMatchService matchService;

	@BeforeEach
	void setUp() {
		goodsIdResolver = new GoodsRecommendGoodsIdResolver(itemsMapper);
		matchService =
				new GoodsRecommendMatchService(
						displaySettingService,
						mainItemMapper,
						recommendItemMapper,
						itemsMapper,
						salabilityResolver,
						goodsIdResolver,
						wxappGoodsItemsListMemberPriceApplyService);
	}

	@Test
	void match_emptyMainItemIds_returnsEmptyList() {
		Map<String, Object> result =
				matchService.match(1L, GoodsRecommendScene.CHECKOUT, List.of(), 0L, List.of(), 0L, null);
		assertEquals(List.of(), result.get("items"));
		assertEquals(0, result.get("total"));
	}

	@Test
	void match_sceneDisabled_returnsEmptyList() {
		when(displaySettingService.getDisplaySetting(1L)).thenReturn(disabledCheckoutSetting());
		Map<String, Object> result =
				matchService.match(1L, GoodsRecommendScene.CHECKOUT, List.of(100L), 0L, List.of(), 0L, null);
		assertEquals(List.of(), result.get("items"));
		assertEquals(0, result.get("total"));
	}

	@Test
	void match_noRuleForMain_returnsEmptyList() {
		when(displaySettingService.getDisplaySetting(1L)).thenReturn(enabledCheckoutSetting());
		Items main = new Items();
		main.setItemId(100L);
		main.setGoodsId(100L);
		main.setCompanyId(1L);
		when(itemsMapper.selectList(any())).thenReturn(List.of(main));
		when(mainItemMapper.selectList(any())).thenReturn(List.of());
		Map<String, Object> result =
				matchService.match(1L, GoodsRecommendScene.CHECKOUT, List.of(100L), 0L, List.of(), 0L, null);
		assertTrue(((List<?>) result.get("items")).isEmpty());
	}

	@Test
	void match_recommendAlreadyInCart_stillReturnsWhenSellable() {
		stubCartRuleWithRecommend200(true);
		Map<String, Object> result =
				matchService.match(
						1L, GoodsRecommendScene.CART, List.of(100L, 200L), 0L, List.of(), 0L, null);
		List<?> items = (List<?>) result.get("items");
		assertEquals(1, items.size());
		assertEquals(200L, ((Map<?, ?>) items.get(0)).get("item_id"));
		assertEquals(199, ((Map<?, ?>) items.get(0)).get("market_price"));
	}

	@Test
	void match_recommendAlreadyInCart_omitsWhenNotSellable() {
		stubCartRuleWithRecommend200(false);
		Map<String, Object> result =
				matchService.match(
						1L, GoodsRecommendScene.CART, List.of(100L, 200L), 0L, List.of(), 0L, null);
		assertTrue(((List<?>) result.get("items")).isEmpty());
	}

	private void stubCartRuleWithRecommend200(boolean recommendSellable) {
		when(displaySettingService.getDisplaySetting(1L)).thenReturn(enabledCartSetting());
		Items main = item(100L, 100L);
		Items recommend = item(200L, 200L);
		recommend.setMarketPrice(199);
		when(itemsMapper.selectList(any())).thenReturn(List.of(main, recommend));

		GoodsRecommendRuleMainItem mainRow = new GoodsRecommendRuleMainItem();
		mainRow.setCompanyId(1L);
		mainRow.setRuleId(1L);
		mainRow.setGoodsId(100L);
		when(mainItemMapper.selectList(any())).thenReturn(List.of(mainRow));

		GoodsRecommendRuleRecommendItem recommendRow = new GoodsRecommendRuleRecommendItem();
		recommendRow.setCompanyId(1L);
		recommendRow.setRuleId(1L);
		recommendRow.setGoodsId(200L);
		recommendRow.setSort(0);
		recommendRow.setId(1L);
		when(recommendItemMapper.selectList(any())).thenReturn(List.of(recommendRow));

		when(salabilityResolver.loadDefaultSkus(eq(1L), any())).thenReturn(Map.of());
		when(salabilityResolver.loadMaxSkuStoreBySpuId(eq(1L), any())).thenReturn(Map.of());
		when(salabilityResolver.loadDistributorContext(eq(1L), eq(0L), any()))
				.thenReturn(GoodsRecommendSalabilityResolver.DistributorContext.empty());
		when(salabilityResolver.isRecommendSellableForMatch(
						any(), any(), eq(1L), eq(0L), any(), any(), any(), any()))
				.thenReturn(recommendSellable);
		if (recommendSellable) {
			when(salabilityResolver.resolvePrice(any(), any())).thenReturn(100);
			when(salabilityResolver.resolveSales(any(), any())).thenReturn(1L);
			when(salabilityResolver.resolveStore(any(), any(), any(), any(), any())).thenReturn(10);
		}
	}

	private static Items item(long itemId, long goodsId) {
		Items item = new Items();
		item.setItemId(itemId);
		item.setGoodsId(goodsId);
		item.setCompanyId(1L);
		item.setItemName("item-" + itemId);
		item.setCreated(1);
		return item;
	}

	private static Map<String, Object> enabledCartSetting() {
		Map<String, Object> setting = new LinkedHashMap<>();
		setting.put("cart_enabled", 1);
		setting.put("cart_limit", 6);
		setting.put("cart_sort", GoodsRecommendDisplaySort.SALES_DESC);
		return setting;
	}

	private static Map<String, Object> disabledCheckoutSetting() {
		Map<String, Object> setting = new LinkedHashMap<>();
		setting.put("checkout_enabled", 0);
		setting.put("checkout_limit", 6);
		setting.put("checkout_sort", GoodsRecommendDisplaySort.SALES_DESC);
		return setting;
	}

	private static Map<String, Object> enabledCheckoutSetting() {
		Map<String, Object> setting = new LinkedHashMap<>();
		setting.put("checkout_enabled", 1);
		setting.put("checkout_limit", 6);
		setting.put("checkout_sort", GoodsRecommendDisplaySort.SALES_DESC);
		return setting;
	}
}
