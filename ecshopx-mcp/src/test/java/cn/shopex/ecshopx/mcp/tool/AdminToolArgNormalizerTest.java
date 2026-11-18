package cn.shopex.ecshopx.mcp.tool;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminToolArgNormalizerTest {

	@Test
	void mapsPageSizeAliasAndLeavesExplicitPageSize() {
		Map<String, Object> args = new LinkedHashMap<>();
		args.put("page_size", 50);
		Map<String, Object> out = AdminToolArgNormalizer.normalize("refunds_search", args);
		assertEquals(50, out.get("pageSize"));
		assertFalse(out.containsKey("page_size"));

		Map<String, Object> keep = new LinkedHashMap<>();
		keep.put("pageSize", 10);
		keep.put("page_size", 99);
		Map<String, Object> kept = AdminToolArgNormalizer.normalize("refunds_search", keep);
		assertEquals(10, kept.get("pageSize"));
		assertFalse(kept.containsKey("page_size"));
	}

	@Test
	void ordersSearchInjectsPlatformEntityDefaultsAndNotshipAlias() {
		Map<String, Object> empty = AdminToolArgNormalizer.normalize("orders_search", Map.of());
		assertEquals("normal", empty.get("order_type"));
		assertEquals("drug,pointsmall,community,employee_purchase", empty.get("order_class_exclude"));
		assertEquals(1, empty.get("page"));
		assertEquals(20, empty.get("pageSize"));

		Map<String, Object> statusAlias = new LinkedHashMap<>();
		statusAlias.put("status", "notship");
		Map<String, Object> aliased = AdminToolArgNormalizer.normalize("orders_search", statusAlias);
		assertEquals("notship", aliased.get("order_status"));
		assertFalse(aliased.containsKey("status"));
	}

	@Test
	void ordersSearchDoesNotOverrideExplicitFilters() {
		Map<String, Object> args = new LinkedHashMap<>();
		args.put("order_type", "community");
		args.put("order_class_exclude", "drug");
		args.put("order_status", "PAYED");
		args.put("page", 3);
		Map<String, Object> out = AdminToolArgNormalizer.normalize("orders_search", args);
		assertEquals("community", out.get("order_type"));
		assertEquals("drug", out.get("order_class_exclude"));
		assertEquals("PAYED", out.get("order_status"));
		assertEquals(3, out.get("page"));
		assertEquals(20, out.get("pageSize"));
	}

	@Test
	void goodsItemsSearchDefaultsMatchPhysicalList() {
		Map<String, Object> out = AdminToolArgNormalizer.normalize("goods_items_search", Map.of());
		assertEquals("normal", out.get("item_type"));
		assertEquals("platform", out.get("operate_source"));
		assertEquals(0, out.get("distributor_id"));
		assertEquals(1, out.get("page"));
		assertEquals(20, out.get("pageSize"));

		Map<String, Object> services = AdminToolArgNormalizer.normalize(
				"goods_items_search", Map.of("item_type", "services"));
		assertEquals("services", services.get("item_type"));
	}

	@Test
	void shopsListDefaultsMatchAuthorizeSelection() {
		Map<String, Object> out = AdminToolArgNormalizer.normalize("shops_list", Map.of());
		assertEquals(1, out.get("page"));
		assertEquals(100, out.get("pageSize"));
		assertEquals(true, out.get("is_all"));
	}

	@Test
	void orderProcessListAcceptsOrderIdSnakeCase() {
		Map<String, Object> out = AdminToolArgNormalizer.normalize(
				"order_process_list", Map.of("order_id", "123"));
		assertEquals("123", out.get("orderId"));
	}

	@Test
	void couponsSearchUsesPageNoPageSize() {
		Map<String, Object> args = new LinkedHashMap<>();
		args.put("page", 2);
		args.put("pageSize", 30);
		Map<String, Object> out = AdminToolArgNormalizer.normalize("coupons_search", args);
		assertEquals(2, out.get("page_no"));
		assertEquals(30, out.get("page_size"));
		assertFalse(out.containsKey("page"));
		assertFalse(out.containsKey("pageSize"));
		assertEquals("2", String.valueOf(out.get("date_status")));
	}

	@Test
	void goodsStatusUpdateSugarsItemIdToItemsBatch() {
		Map<String, Object> args = new LinkedHashMap<>();
		args.put("item_id", "55");
		args.put("status", "onsale");
		args.put("confirm", true);
		Map<String, Object> out = AdminToolArgNormalizer.normalize("goods_item_status_update", args);
		assertTrue(out.get("items") instanceof List<?>);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items = (List<Map<String, Object>>) out.get("items");
		assertEquals(1, items.size());
		assertEquals("55", String.valueOf(items.get(0).get("goods_id")));
		assertEquals("onsale", out.get("status"));
	}

	@Test
	void goodsStockUpdateSugarsItemIdAndStore() {
		Map<String, Object> args = new LinkedHashMap<>();
		args.put("item_id", "9");
		args.put("store", 100);
		args.put("confirm", true);
		Map<String, Object> out = AdminToolArgNormalizer.normalize("goods_item_stock_update", args);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items = (List<Map<String, Object>>) out.get("items");
		assertEquals("9", String.valueOf(items.get(0).get("item_id")));
		assertEquals(100, items.get(0).get("store"));
		assertEquals(true, items.get(0).get("is_default"));
	}
}
