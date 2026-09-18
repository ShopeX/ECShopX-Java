package cn.shopex.ecshopx.shuyun.service.openplatform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProductSyncServiceValidationTest {

	@Test
	void validateProductRow_ok() {
		Map<String, Object> row = minimalOk("9");
		assertNull(ProductSyncService.validateProductRow(row));
	}

	@Test
	void validateProductRow_missingCategory() {
		Map<String, Object> row = minimalOk("9");
		row.remove("category_id");
		assertEquals("missing_category_id", ProductSyncService.validateProductRow(row));
	}

	@Test
	void mergeKeys_phpCompatiblePrefix() {
		String cat = OrderSyncDispatchCacheKeys.categorySyncMergeKey(1L, 2L);
		assertTrue(cat.startsWith("shuyun_open_platform:merge:"));
		assertEquals(64, cat.substring("shuyun_open_platform:merge:".length()).length());
		String prod = OrderSyncDispatchCacheKeys.productSyncMergeKey(1L, 2L, 3L);
		assertTrue(prod.startsWith("shuyun_open_platform:merge:"));
		String shop = OrderSyncDispatchCacheKeys.shopSyncMergeKey(1L, 2L);
		assertTrue(shop.startsWith("shuyun_open_platform:merge:"));
	}

	private static Map<String, Object> minimalOk(String productId) {
		Map<String, Object> sku = new LinkedHashMap<>();
		sku.put("sku_id", "1");
		sku.put("sku_detail", ProductSyncService.SKU_DETAIL_SINGLE_SPEC);
		sku.put("price", 1.0);
		sku.put("status", 1);
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("shop_id", "10-off");
		row.put("product_id", productId);
		row.put("product_name", "n");
		row.put("category_id", "10");
		row.put("type", "SY_NORMAL");
		row.put("modified", "2026-01-01 00:00:00");
		row.put("status", "SY_ONLINE");
		row.put("price", 1.0);
		row.put("skus", List.of(sku));
		return row;
	}
}
