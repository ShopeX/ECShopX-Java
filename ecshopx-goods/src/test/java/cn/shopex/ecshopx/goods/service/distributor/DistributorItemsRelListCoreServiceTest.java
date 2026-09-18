package cn.shopex.ecshopx.goods.service.distributor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelCatsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import cn.shopex.ecshopx.goods.service.ItemsListMultiLangApplier;
import cn.shopex.ecshopx.goods.service.items.ItemsMedicineService;
import cn.shopex.ecshopx.goods.service.items.ItemsSkuListAssembler;
import cn.shopex.ecshopx.crossborder.mapper.OriginCountryMapper;
import cn.shopex.ecshopx.distribution.repository.DistributorItemsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsAttributesRepository;
import cn.shopex.ecshopx.common.config.LangueProperties;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributorItemsRelListCoreServiceTest {

	@Mock private ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver;
	@Mock private ItemsListQueryRepository itemsListQueryRepository;
	@Mock private DistributorItemsRepository distributorItemsRepository;
	@Mock private ItemsListMultiLangApplier itemsListMultiLangApplier;
	@Mock private ItemsRepository itemsRepository;
	@Mock private ItemsRelCatsRepository itemsRelCatsRepository;
	@Mock private OriginCountryMapper originCountryMapper;
	@Mock private DistributorItemsListSkuSpecApplier distributorItemsListSkuSpecApplier;
	@Mock private ItemsSkuListAssembler itemsSkuListAssembler;
	@Mock private ItemsMedicineService itemsMedicineService;
	@Mock private ItemsAttributesRepository itemsAttributesRepository;
	@Mock private LangueProperties langueProperties;

	private DistributorItemsRelListCoreService service;

	@BeforeEach
	void setUp() {
		service = new DistributorItemsRelListCoreService(
				itemsCategoryDistributorIdResolver,
				itemsListQueryRepository,
				distributorItemsRepository,
				itemsListMultiLangApplier,
				itemsRepository,
				itemsRelCatsRepository,
				originCountryMapper,
				distributorItemsListSkuSpecApplier,
				itemsSkuListAssembler,
				itemsMedicineService,
				itemsAttributesRepository,
				langueProperties);
	}

	@Test
	@DisplayName("standard 模式直营店列表也按 distribution_distributor_items 过滤")
	void query_standardMode_alwaysAppliesDistributorItemsJoin() {
		when(itemsCategoryDistributorIdResolver.resolveProductModel(38L)).thenReturn("standard");
		when(itemsListQueryRepository.countByParams(anyMap())).thenReturn(0L);

		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("distributor_id", 269L);
		filter.put("is_default_true", Boolean.TRUE);
		service.query(38L, 269L, filter, 10, 1, new LinkedHashMap<>(), "zh-CN");

		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(itemsListQueryRepository).countByParams(captor.capture());
		Map<String, Object> qp = captor.getValue();
		assertTrue(Boolean.TRUE.equals(qp.get(ItemsListQueryRepository.KEY_DIST_REL_VIRT_JOIN)));
		assertEquals(269L, qp.get(ItemsListQueryRepository.KEY_DIST_REL_VIRT_DISTRIBUTOR_ID));
		assertFalse(qp.containsKey(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_EQ));
	}

	@Test
	@DisplayName("is_can_sale 筛选映射到 goods_can_sale（与 PHP 一致）")
	void query_standardMode_mapsCanSaleFilterToGoodsCanSale() {
		when(itemsCategoryDistributorIdResolver.resolveProductModel(38L)).thenReturn("standard");
		when(itemsListQueryRepository.countByParams(anyMap())).thenReturn(0L);

		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("distributor_id", 269L);
		filter.put("__dist_is_can_sale_filter", Boolean.TRUE);
		service.query(38L, 269L, filter, 10, 1, new LinkedHashMap<>(), "zh-CN");

		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(itemsListQueryRepository).countByParams(captor.capture());
		assertEquals(Boolean.TRUE, captor.getValue().get(ItemsListQueryRepository.KEY_DIST_REL_VIRT_GOODS_CAN_SALE));
	}
}
