package cn.shopex.ecshopx.goods.service.items;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.shopex.ecshopx.common.inventory.InventoryDeductTarget;
import cn.shopex.ecshopx.common.inventory.ItemInventoryLineContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InventoryDeductTargetResolverTest {

	private final InventoryDeductTargetResolver resolver = new InventoryDeductTargetResolver();

	@Test
	@DisplayName("is_total_store=true → PLATFORM_ITEMS（不因 items.distributor_id 走店前缀 key）")
	void totalStoreUsesPlatformPool() {
		ItemInventoryLineContext ctx = ItemInventoryLineContext.of(1L, 16L, 0L, 9999L, true, "logistics", 0L);
		assertEquals(InventoryDeductTarget.PLATFORM_ITEMS, resolver.resolve(ctx));
	}

	@Test
	@DisplayName("is_total_store=false + 请求 dist>0 → SHOP_DISTRIBUTOR_ITEMS")
	void independentShopStock() {
		ItemInventoryLineContext ctx = ItemInventoryLineContext.of(1L, 16L, 0L, 3008L, false, "ziti", 0L);
		assertEquals(InventoryDeductTarget.SHOP_DISTRIBUTOR_ITEMS, resolver.resolve(ctx));
	}

	@Test
	@DisplayName("is_total_store=true + 请求 dist=0 → PLATFORM_ITEMS")
	void hqItemUsesPlatformPool() {
		ItemInventoryLineContext ctx = ItemInventoryLineContext.of(1L, 16L, 0L, 0L, true, "ziti", 0L);
		assertEquals(InventoryDeductTarget.PLATFORM_ITEMS, resolver.resolve(ctx));
	}

	@Test
	@DisplayName("logistics + supplier_id>0 → SUPPLIER_ITEMS")
	void supplierLogisticsTakesPriority() {
		ItemInventoryLineContext ctx = ItemInventoryLineContext.of(1L, 16L, 5L, 3008L, true, "logistics", 0L);
		assertEquals(InventoryDeductTarget.SUPPLIER_ITEMS, resolver.resolve(ctx));
	}

	@Test
	@DisplayName("非 logistics 的供应商商品仍按 is_total_store 判定 item_store key")
	void supplierNonLogisticsUsesTotalStoreFlag() {
		ItemInventoryLineContext ctx = ItemInventoryLineContext.of(1L, 16L, 5L, 0L, true, "ziti", 0L);
		assertEquals(InventoryDeductTarget.PLATFORM_ITEMS, resolver.resolve(ctx));
	}
}
