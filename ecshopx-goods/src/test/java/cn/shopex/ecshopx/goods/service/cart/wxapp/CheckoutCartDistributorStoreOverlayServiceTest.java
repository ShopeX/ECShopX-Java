package cn.shopex.ecshopx.goods.service.cart.wxapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.goods.service.distributor.DistributorItemSkuInfoForStoreService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckoutCartDistributorStoreOverlayServiceTest {

	@Mock
	private DistributorItemSkuInfoForStoreService distributorItemSkuInfoForStoreService;

	private CheckoutCartDistributorStoreOverlayService service;

	@BeforeEach
	void setUp() {
		service = new CheckoutCartDistributorStoreOverlayService(distributorItemSkuInfoForStoreService);
	}

	@Test
	void applyForSkuMaps_independentStore_overlaysShopStockAndFlags() {
		DistributorItems di = new DistributorItems();
		di.setIsTotalStore(false);
		di.setStore(901L);
		di.setPrice(100L);
		when(distributorItemSkuInfoForStoreService.findRow(38L, 8116L, 129L)).thenReturn(di);

		Map<String, Object> gift = new LinkedHashMap<>();
		gift.put("item_id", 8116L);
		gift.put("store", 0);
		gift.put("approve_status", "onsale");
		List<Map<String, Object>> maps = new ArrayList<>();
		maps.add(gift);

		service.applyForSkuMaps(38L, 129L, maps);

		assertFalse(Boolean.TRUE.equals(gift.get("is_total_store")));
		assertEquals(false, gift.get("is_total_store"));
		assertEquals(0, gift.get("logistics_store"));
		assertEquals(901, gift.get("store"));
	}

	@Test
	void applyForSkuMaps_totalStore_onlySetsFlag() {
		DistributorItems di = new DistributorItems();
		di.setIsTotalStore(true);
		di.setStore(5L);
		when(distributorItemSkuInfoForStoreService.findRow(38L, 8116L, 129L)).thenReturn(di);

		Map<String, Object> gift = new LinkedHashMap<>();
		gift.put("item_id", 8116L);
		gift.put("store", 12);
		List<Map<String, Object>> maps = List.of(gift);

		service.applyForSkuMaps(38L, 129L, maps);

		assertTrue(Boolean.TRUE.equals(gift.get("is_total_store")));
		assertEquals(12, gift.get("store"));
		assertNull(gift.get("logistics_store"));
	}

	@Test
	void applyForSkuMaps_noRel_noop() {
		when(distributorItemSkuInfoForStoreService.findRow(38L, 8116L, 129L)).thenReturn(null);

		Map<String, Object> gift = new LinkedHashMap<>();
		gift.put("item_id", 8116L);
		gift.put("store", 0);
		service.applyForSkuMaps(38L, 129L, List.of(gift));

		assertNull(gift.get("is_total_store"));
		assertEquals(0, gift.get("store"));
	}
}
