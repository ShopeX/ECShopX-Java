package cn.shopex.ecshopx.goods.service.recommend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.distribution.mapper.DistributorItemsMapper;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import java.util.Map;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoodsRecommendSalabilityResolverTest {

	@Mock
	private DistributorItemsMapper distributorItemsMapper;

	@Mock
	private ItemsMapper itemsMapper;

	@Mock
	private ItemsCategoryDistributorIdResolver productModelResolver;

	private GoodsRecommendSalabilityResolver resolver;

	@BeforeEach
	void setUp() {
		resolver = new GoodsRecommendSalabilityResolver(distributorItemsMapper, itemsMapper, productModelResolver);
	}

	@Test
	void isSellable_standardShopOwnedItemFromOtherShop_returnsFalse() {
		when(productModelResolver.resolveProductModel(1L)).thenReturn("standard");
		Items item = item(100L, 200, "onsale", 10, "[\"http://img/a.jpg\"]");
		assertFalse(resolver.isSellable(item, 1L, 100L, Map.of(), Map.of()));
	}

	@Test
	void isSellable_standardShopOwnedItemSameShop_returnsTrueWhenOnsale() {
		when(productModelResolver.resolveProductModel(1L)).thenReturn("standard");
		Items item = item(100L, 100, "onsale", 10, "[\"http://img/a.jpg\"]");
		assertTrue(resolver.isSellable(item, 1L, 100L, Map.of(), Map.of()));
	}

	@Test
	void isRecommendSellableForMatch_mainOutOfStock_recommendInStock_returnsTrue() {
		when(productModelResolver.resolveProductModel(1L)).thenReturn("standard");
		Items main = item(7666L, 0, "onsale", 100, "[\"http://img/main.jpg\"]");
		Items recommend = item(6749L, 0, "onsale", 100, "[\"http://img/rec.jpg\"]");
		Map<Long, DistributorItems> distributorItems =
				Map.of(
						7666L, distributorRow(7666L, true, 0),
						6749L, distributorRow(6749L, true, 10));
		assertTrue(
				resolver.isRecommendSellableForMatch(main, recommend, 1L, 269L, distributorItems, Map.of()));
		assertFalse(resolver.isPairSellable(main, recommend, 1L, 269L, distributorItems, Map.of()));
	}

	@Test
	void isRecommendSellableForMatch_recommendOutOfStock_returnsFalse() {
		when(productModelResolver.resolveProductModel(1L)).thenReturn("standard");
		Items main = item(7666L, 0, "onsale", 100, "[\"http://img/main.jpg\"]");
		Items recommend = item(6749L, 0, "onsale", 100, "[\"http://img/rec.jpg\"]");
		Map<Long, DistributorItems> distributorItems =
				Map.of(
						7666L, distributorRow(7666L, true, 0),
						6749L, distributorRow(6749L, true, 0));
		assertFalse(
				resolver.isRecommendSellableForMatch(main, recommend, 1L, 269L, distributorItems, Map.of()));
	}

	@Test
	void isSellable_multiSpecSkuDistributorStore_whenLocalStoreOnly_returnsTrue() {
		when(productModelResolver.resolveProductModel(1L)).thenReturn("standard");
		Items spu = item(6749L, 0, "onsale", 0, "[\"http://img/rec.jpg\"]");
		spu.setDefaultItemId(6749L);
		DistributorItems spuRow = distributorRow(6749L, true, 0);
		spuRow.setIsTotalStore(false);
		DistributorItems skuRow = distributorRow(6750L, true, 1);
		skuRow.setDefaultItemId(6749L);
		skuRow.setIsTotalStore(false);
		Map<Long, DistributorItems> distributorItems = Map.of(6749L, spuRow, 6750L, skuRow);
		assertTrue(
				resolver.isSellable(
						spu,
						1L,
						269L,
						distributorItems,
						Map.of(),
						Map.of(),
						Map.of(6749L, 1)));
	}

	@Test
	void isSellable_multiSpecNonDefaultSkuHasStock_whenTotalStore_returnsTrue() {
		when(productModelResolver.resolveProductModel(1L)).thenReturn("standard");
		Items spu = item(6749L, 0, "onsale", 0, "[\"http://img/rec.jpg\"]");
		spu.setDefaultItemId(6749L);
		Map<Long, Items> defaultSkuBySpuId = Map.of(6749L, item(6750L, 0, "onsale", 0, "[\"http://img/rec.jpg\"]"));
		Map<Long, Integer> maxSkuStoreBySpuId = Map.of(6749L, 1);
		DistributorItems di = distributorRow(6749L, true, 0);
		di.setIsTotalStore(true);
		assertTrue(
				resolver.isSellable(
						spu, 1L, 269L, Map.of(6749L, di), defaultSkuBySpuId, maxSkuStoreBySpuId));
	}

	@Test
	void isSellable_multiSpecNonDefaultSkuHasStock_whenLocalStoreOnly_returnsFalseWithoutSkuRows() {
		when(productModelResolver.resolveProductModel(1L)).thenReturn("standard");
		Items spu = item(6749L, 0, "onsale", 0, "[\"http://img/rec.jpg\"]");
		spu.setDefaultItemId(6749L);
		Map<Long, Integer> maxSkuStoreBySpuId = Map.of(6749L, 1);
		DistributorItems di = distributorRow(6749L, true, 0);
		di.setIsTotalStore(false);
		assertFalse(
				resolver.isSellable(
						spu, 1L, 269L, Map.of(6749L, di), Map.of(), maxSkuStoreBySpuId, Map.of()));
	}

	@Test
	void resolveCheckoutSkuItemId_multiSpecPicksSkuWithLocalStore() {
		Items spu = item(6749L, 0, "onsale", 0, null);
		spu.setDefaultItemId(6749L);
		spu.setNospec("false");
		Items sku6750 = item(6750L, 0, "onsale", 0, null);
		sku6750.setDefaultItemId(6749L);
		Items sku6753 = item(6753L, 0, "onsale", 0, null);
		sku6753.setDefaultItemId(6749L);
		when(itemsMapper.selectList(any())).thenReturn(List.of(spu, sku6750, sku6753));
		Map<Long, DistributorItems> distributorItems =
				Map.of(
						6749L, skuDistributorRow(6749L, 6749L, 0),
						6750L, skuDistributorRow(6750L, 6749L, 0),
						6753L, skuDistributorRow(6753L, 6749L, 1));
		GoodsRecommendSalabilityResolver.DistributorContext context =
				new GoodsRecommendSalabilityResolver.DistributorContext(
						distributorItems, Map.of(6749L, 1));
		long checkoutSkuId =
				resolver.resolveCheckoutSkuItemId(
						spu, 38L, context, Map.of(), context.storeBySpuId());
		assertEquals(6753L, checkoutSkuId);
	}

	@Test
	void isSellable_spu6749_oneSkuHasLocalStore_returnsTrue() {
		when(productModelResolver.resolveProductModel(38L)).thenReturn("standard");
		Items spu = item(6749L, 0, "onsale", 0, null);
		spu.setDefaultItemId(6749L);
		Map<Long, DistributorItems> distributorItems =
				Map.of(
						6749L, distributorRow(6749L, true, 0),
						6750L, skuDistributorRow(6750L, 6749L, 0),
						6751L, skuDistributorRow(6751L, 6749L, 0),
						6752L, skuDistributorRow(6752L, 6749L, 0),
						6753L, skuDistributorRow(6753L, 6749L, 1));
		assertTrue(
				resolver.isSellable(
						spu,
						38L,
						269L,
						distributorItems,
						Map.of(),
						Map.of(),
						Map.of(6749L, 1)));
	}

	@Test
	void isSellable_emptyPics_stillSellableWhenInStock() {
		when(productModelResolver.resolveProductModel(1L)).thenReturn("standard");
		Items item = item(6749L, 0, "onsale", 10, null);
		DistributorItems di = distributorRow(6749L, true, 10);
		di.setIsTotalStore(true);
		assertTrue(
				resolver.isSellable(
						item, 1L, 269L, Map.of(6749L, di), Map.of(), Map.of(), Map.of()));
	}

	private static DistributorItems skuDistributorRow(long itemId, long defaultItemId, int store) {
		DistributorItems row = distributorRow(itemId, true, store);
		row.setDefaultItemId(defaultItemId);
		row.setIsTotalStore(false);
		return row;
	}

	private static DistributorItems distributorRow(long itemId, boolean canSale, int store) {
		DistributorItems row = new DistributorItems();
		row.setItemId(itemId);
		row.setIsCanSale(canSale);
		row.setStore((long) store);
		row.setIsTotalStore(false);
		return row;
	}

	private static Items item(long itemId, int distributorId, String approveStatus, int store, String pics) {
		Items item = new Items();
		item.setItemId(itemId);
		item.setDistributorId(distributorId);
		item.setApproveStatus(approveStatus);
		item.setStore(store);
		item.setPics(pics);
		return item;
	}
}
