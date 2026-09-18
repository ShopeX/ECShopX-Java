package cn.shopex.ecshopx.goods.service.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.distribution.service.DistributorDeliveryCapability;
import cn.shopex.ecshopx.distribution.service.DistributorDeliveryCapabilityService;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemLogisticsStoreEnricherDeliveryDisplayTest {

	@Mock
	private ItemsRepository itemsRepository;
	@Mock
	private SupplierItemsRepository supplierItemsRepository;
	@Mock
	private DistributorDeliveryCapabilityService distributorDeliveryCapabilityService;

	private ItemLogisticsStoreEnricher enricher;

	@BeforeEach
	void setUp() {
		enricher = new ItemLogisticsStoreEnricher(
				itemsRepository, supplierItemsRepository, distributorDeliveryCapabilityService);
	}

	@Test
	void applyFrontDisplay_supplierExpressOnly_replacesStoreWithLogistics() {
		when(distributorDeliveryCapabilityService.resolveForFront(141L, 0L))
				.thenReturn(DistributorDeliveryCapability.of(true, false, false));
		Map<String, Object> detail = supplierRow();
		detail.put("distributor_id", 0);
		detail.put("store", 10);
		detail.put("logistics_store", 30);
		enricher.applyFrontDisplayStoreTotal(141L, 0L, detail);
		assertEquals(30, detail.get("store"));
		assertEquals(30, detail.get("logistics_store"));
	}

	@Test
	void applyFrontDisplay_supplierLocalOnly_keepsLocalStore() {
		when(distributorDeliveryCapabilityService.resolveForFront(141L, 9L))
				.thenReturn(DistributorDeliveryCapability.of(false, true, false));
		Map<String, Object> detail = supplierRow();
		detail.put("distributor_id", 9L);
		detail.put("store", 10);
		detail.put("logistics_store", 30);
		enricher.applyFrontDisplayStoreTotal(141L, 9L, detail);
		assertEquals(10, detail.get("store"));
	}

	@Test
	void applyFrontDisplay_selfOwned_skipsDeliveryCombine_keepsLocalStore() {
		Map<String, Object> detail = new HashMap<>();
		detail.put("distributor_id", 0);
		detail.put("supplier_id", 0);
		detail.put("supplier_item_id", 0);
		detail.put("store", 10);
		detail.put("item_total_store", 10);
		detail.put("logistics_store", 0);
		enricher.applyFrontDisplayStoreTotal(141L, 0L, detail);
		assertEquals(10, detail.get("store"));
		assertEquals(10, detail.get("item_total_store"));
		verify(distributorDeliveryCapabilityService, never()).resolveForFront(anyLong(), anyLong());
	}

	/**
	 * 列表 dealListStore 汇总本地库存为 0 后，须与详情一样用供应商共享库存合成，否则会误显示无货。
	 */
	@Test
	void enrichListRowsAndApplyDisplayTotal_supplierWithNullLocalStore_usesLogisticsLikeDetail() {
		Items sku = new Items();
		sku.setItemId(7723L);
		sku.setSupplierItemId(330);
		sku.setCompanyId(141L);
		when(itemsRepository.listByCompanyAndItemIdsPreservingOrder(eq(141L), org.mockito.ArgumentMatchers.anyList()))
				.thenReturn(List.of(sku));
		cn.shopex.ecshopx.supplier.domain.SupplierItems si = new cn.shopex.ecshopx.supplier.domain.SupplierItems();
		si.setItemId(330L);
		si.setStore(95);
		when(supplierItemsRepository.listByCompanyAndItemIds(eq(141L), org.mockito.ArgumentMatchers.anyCollection()))
				.thenReturn(List.of(si));
		when(distributorDeliveryCapabilityService.resolveForFront(141L, 285L))
				.thenReturn(DistributorDeliveryCapability.of(true, false, false));

		Map<String, Object> row = supplierRow();
		row.put("item_id", 7723L);
		row.put("goods_id", 7723L);
		row.put("distributor_id", 285L);
		row.put("supplier_item_id", 330);
		row.put("store", 0);
		List<Map<String, Object>> rows = new ArrayList<>();
		rows.add(row);

		enricher.enrichListRowsAndApplyDisplayTotal(141L, 0L, rows);

		assertEquals(95, row.get("logistics_store"));
		assertEquals(95, row.get("store"));
	}

	@Test
	void applyFrontDisplay_selfOwnedExpressOnlyHq_doesNotWipeLocalStore() {
		Map<String, Object> detail = new HashMap<>();
		detail.put("distributor_id", 0);
		detail.put("supplier_id", 0);
		detail.put("supplier_item_id", 0);
		detail.put("store", 83);
		detail.put("item_total_store", 83);
		detail.put("logistics_store", 0);
		enricher.applyFrontDisplayStoreTotal(141L, 0L, detail);
		assertEquals(83, detail.get("store"));
		assertEquals(83, detail.get("item_total_store"));
		verify(distributorDeliveryCapabilityService, never()).resolveForFront(anyLong(), anyLong());
	}

	@Test
	void applyFrontDisplay_supplierShopOwned_ignoresRequestZero_usesOwnerShopCapability() {
		when(distributorDeliveryCapabilityService.resolveForFront(141L, 314L))
				.thenReturn(DistributorDeliveryCapability.of(true, true, true));
		Map<String, Object> detail = supplierRow();
		detail.put("distributor_id", 314);
		detail.put("store", 10);
		detail.put("item_total_store", 10);
		detail.put("logistics_store", 0);
		enricher.applyFrontDisplayStoreTotal(141L, 0L, detail);
		assertEquals(10, detail.get("store"));
		assertEquals(10, detail.get("item_total_store"));
		verify(distributorDeliveryCapabilityService).resolveForFront(141L, 314L);
		verify(distributorDeliveryCapabilityService, never()).resolveForFront(141L, 0L);
	}

	@Test
	void applyFrontDisplay_supplierBoth_sums() {
		when(distributorDeliveryCapabilityService.resolveForFront(eq(141L), anyLong()))
				.thenReturn(DistributorDeliveryCapability.of(true, true, false));
		Map<String, Object> detail = supplierRow();
		detail.put("store", 10);
		detail.put("logistics_store", 30);
		enricher.applyFrontDisplayStoreTotal(141L, 0L, detail);
		assertEquals(40, detail.get("store"));
	}

	@Test
	void applyFrontDisplay_skipsSeckillActivity() {
		Map<String, Object> detail = supplierRow();
		detail.put("store", 5);
		detail.put("logistics_store", 30);
		detail.put("activity_type", "seckill");
		enricher.applyFrontDisplayStoreTotal(141L, 0L, detail);
		assertEquals(5, detail.get("store"));
	}

	@Test
	void applyFrontDisplay_limitedTimeSale_supplierExpressOnly_usesLogisticsStore() {
		when(distributorDeliveryCapabilityService.resolveForFront(141L, 0L))
				.thenReturn(DistributorDeliveryCapability.logisticsOnlyFallback());
		Map<String, Object> detail = supplierRow();
		detail.put("distributor_id", 0);
		detail.put("store", 111);
		detail.put("logistics_store", 97);
		detail.put("activity_type", "limited_time_sale");
		enricher.applyFrontDisplayStoreTotal(141L, 0L, detail);
		assertEquals(97, detail.get("store"));
	}

	@Test
	void applyFrontDisplayList_onlySupplierRowsCombined() {
		when(distributorDeliveryCapabilityService.resolveForFront(141L, 0L))
				.thenReturn(DistributorDeliveryCapability.of(true, false, false));
		Map<String, Object> supplier = supplierRow();
		supplier.put("item_id", 1L);
		supplier.put("distributor_id", 0);
		supplier.put("store", 10);
		supplier.put("logistics_store", 30);
		Map<String, Object> selfOwned = new HashMap<>();
		selfOwned.put("item_id", 3L);
		selfOwned.put("distributor_id", 0);
		selfOwned.put("supplier_id", 0);
		selfOwned.put("supplier_item_id", 0);
		selfOwned.put("store", 83);
		selfOwned.put("logistics_store", 0);
		Map<String, Object> limitedSale = supplierRow();
		limitedSale.put("item_id", 2L);
		limitedSale.put("distributor_id", 0);
		limitedSale.put("store", 111);
		limitedSale.put("logistics_store", 30);
		Map<String, Object> tag = new HashMap<>();
		tag.put("tag_type", "limited_time_sale");
		limitedSale.put("promotion_activity", List.of(tag));
		Map<String, Object> groupAct = supplierRow();
		groupAct.put("item_id", 4L);
		groupAct.put("distributor_id", 0);
		groupAct.put("store", 5);
		groupAct.put("logistics_store", 30);
		groupAct.put("activity_type", "group");
		List<Map<String, Object>> rows = new ArrayList<>();
		rows.add(supplier);
		rows.add(selfOwned);
		rows.add(limitedSale);
		rows.add(groupAct);
		enricher.applyFrontDisplayStoreTotalList(141L, 0L, rows);
		assertEquals(30, supplier.get("store"));
		assertEquals(83, selfOwned.get("store"));
		assertEquals(30, limitedSale.get("store"));
		assertEquals(5, groupAct.get("store"));
	}

	@Test
	void combineEffectiveStore_selfItem_returnsLocalWithoutDeliveryResolve() {
		Items item = new Items();
		item.setDistributorId(0);
		item.setSupplierItemId(0);
		int store = enricher.combineEffectiveStore(141L, 0L, 83, item);
		assertEquals(83, store);
		verify(distributorDeliveryCapabilityService, never()).resolveForFront(anyLong(), anyLong());
	}

	@Test
	void combineEffectiveStore_supplierItem_usesItemOwnerNotRequest() {
		when(distributorDeliveryCapabilityService.resolveForFront(141L, 314L))
				.thenReturn(DistributorDeliveryCapability.of(true, true, true));
		Items item = new Items();
		item.setDistributorId(314);
		item.setSupplierItemId(88);
		int store = enricher.combineEffectiveStore(141L, 0L, 10, item);
		assertEquals(10, store);
		verify(distributorDeliveryCapabilityService).resolveForFront(141L, 314L);
	}

	@Test
	void applyCartLines_onlyIndependentShopStore() {
		when(distributorDeliveryCapabilityService.resolveForFront(141L, 9L))
				.thenReturn(DistributorDeliveryCapability.of(true, false, false));
		Map<String, Object> sku = new HashMap<>();
		sku.put("logistics_store", 30);
		Map<Long, Map<String, Object>> skuByItem = Map.of(100L, sku);

		Map<String, Object> independent = new HashMap<>();
		independent.put("item_id", 100L);
		independent.put("shop_id", 9L);
		independent.put("store", 10);
		independent.put("is_total_store", false);

		Map<String, Object> totalStore = new HashMap<>();
		totalStore.put("item_id", 100L);
		totalStore.put("shop_id", 9L);
		totalStore.put("store", 40);
		totalStore.put("is_total_store", true);

		enricher.applyCartLinesDisplayStoreAfterOverlay(
				141L, List.of(independent, totalStore), skuByItem);
		assertEquals(30, independent.get("store"));
		assertEquals(40, totalStore.get("store"));
	}

	private static Map<String, Object> supplierRow() {
		Map<String, Object> m = new HashMap<>();
		m.put("supplier_id", 729);
		m.put("supplier_item_id", 88);
		return m;
	}
}
