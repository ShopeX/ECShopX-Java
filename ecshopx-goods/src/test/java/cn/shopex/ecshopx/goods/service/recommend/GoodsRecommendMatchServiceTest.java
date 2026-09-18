package cn.shopex.ecshopx.goods.service.recommend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.domain.Items;
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
