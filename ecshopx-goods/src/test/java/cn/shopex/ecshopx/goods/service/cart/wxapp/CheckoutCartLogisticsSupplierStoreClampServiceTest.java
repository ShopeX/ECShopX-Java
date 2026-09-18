package cn.shopex.ecshopx.goods.service.cart.wxapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.service.items.ItemAvailableStoreResolver;
import cn.shopex.ecshopx.goods.service.items.SupplierItemStoreService;
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
class CheckoutCartLogisticsSupplierStoreClampServiceTest {

	@Mock
	private SupplierItemStoreService supplierItemStoreService;

	private CheckoutCartLogisticsSupplierStoreClampService service;

	@BeforeEach
	void setUp() {
		service = new CheckoutCartLogisticsSupplierStoreClampService(
				new ItemAvailableStoreResolver(supplierItemStoreService));
	}

	@Test
	void logisticsClamp_returnsAdjustmentAndKeepsCartNum() {
		Map<String, Object> line = new HashMap<>();
		line.put("item_id", 100L);
		line.put("item_name", "测试商品");
		line.put("num", 5);
		line.put("supplier_id", 9L);
		line.put("supplier_item_id", 88L);
		List<Map<String, Object>> valid = new ArrayList<>();
		valid.add(line);
		Map<String, Object> sku = new HashMap<>(line);
		Map<Long, Map<String, Object>> skuByItem = Map.of(100L, sku);
		when(supplierItemStoreService.resolveSupplierItemStore(141L, 88L)).thenReturn(2);

		List<Map<String, Object>> tips = service.clampIfNeeded(
				141L, true, Map.of("receipt_type", "logistics"), valid, new ArrayList<>(), skuByItem);

		assertEquals(1, tips.size());
		assertEquals(5, tips.get(0).get("original_num"));
		assertEquals(2, tips.get(0).get("available_num"));
		assertEquals(2, line.get("num"));
		assertEquals(5, line.get("cart_num"));
	}

	@Test
	void noClamp_returnsEmpty() {
		Map<String, Object> line = new HashMap<>();
		line.put("item_id", 100L);
		line.put("num", 1);
		line.put("store", 10);
		line.put("logistics_store", 0);
		List<Map<String, Object>> valid = new ArrayList<>();
		valid.add(line);

		List<Map<String, Object>> tips = service.clampIfNeeded(
				141L, true, Map.of("receipt_type", "ziti"), valid, new ArrayList<>(), Map.of());

		assertTrue(tips.isEmpty());
		assertEquals(1, line.get("num"));
	}

	@Test
	void logisticsClampToZero_removesLineAndRecordsAdjustment() {
		Map<String, Object> line = new HashMap<>();
		line.put("item_id", 100L);
		line.put("item_name", "测试");
		line.put("num", 1);
		line.put("is_checked", true);
		line.put("supplier_id", 9L);
		line.put("supplier_item_id", 88L);
		List<Map<String, Object>> valid = new ArrayList<>();
		valid.add(line);
		List<Map<String, Object>> invalid = new ArrayList<>();
		Map<String, Object> sku = new HashMap<>(line);
		when(supplierItemStoreService.resolveSupplierItemStore(141L, 88L)).thenReturn(0);

		List<Map<String, Object>> tips = service.clampIfNeeded(
				141L, true, Map.of("receipt_type", "logistics"), valid, invalid, Map.of(100L, sku));

		assertTrue(valid.isEmpty());
		assertEquals(1, invalid.size());
		assertEquals(1, tips.size());
		assertEquals(0, tips.get(0).get("available_num"));
		String tip = CheckoutCartLogisticsSupplierStoreClampService.buildStoreQuantityAdjustTip(
				tips, true, "ziti");
		assertTrue(tip.contains("切换为自提"));
	}

	@Test
	void logisticsClampUnchecked_silentlyRemovesWithoutAdjustmentTip() {
		Map<String, Object> line = new HashMap<>();
		line.put("item_id", 7894L);
		line.put("item_name", "测试");
		line.put("num", 1);
		line.put("is_checked", false);
		line.put("supplier_id", 657L);
		line.put("supplier_item_id", 344L);
		List<Map<String, Object>> valid = new ArrayList<>();
		valid.add(line);
		List<Map<String, Object>> invalid = new ArrayList<>();
		Map<String, Object> sku = new HashMap<>(line);
		when(supplierItemStoreService.resolveSupplierItemStore(141L, 344L)).thenReturn(0);

		List<Map<String, Object>> tips = service.clampIfNeeded(
				141L, true, Map.of("receipt_type", "logistics"), valid, invalid, Map.of(7894L, sku));

		assertTrue(valid.isEmpty());
		assertEquals(1, invalid.size());
		assertTrue(tips.isEmpty());
	}
}
