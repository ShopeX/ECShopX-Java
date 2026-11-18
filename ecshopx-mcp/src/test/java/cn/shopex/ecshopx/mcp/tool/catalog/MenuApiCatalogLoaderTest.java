package cn.shopex.ecshopx.mcp.tool.catalog;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuApiCatalogLoaderTest {

	@Test
	void loadsOrdersAndPermissionEntries() {
		MenuApiCatalog catalog = MenuApiCatalogLoader.loadClasspath();
		assertFalse(catalog.findByRouteOrPermission("tradenormalorders").isEmpty());
		assertFalse(catalog.findByRouteOrPermission("/api/v1/orders").isEmpty());
		assertFalse(catalog.findByRouteOrPermission("permission").isEmpty());
		List<MenuApiEntry> orders = catalog.findByMethodAndPath("GET", "/api/v1/orders");
		assertFalse(orders.isEmpty());
	}

	@Test
	void resolveIntersectsCatalogWithMenuKeys() {
		MenuApiCatalog catalog = MenuApiCatalogLoader.loadClasspath();
		Set<String> menus = Set.of("tradenormalorders");
		List<MenuApiEntry> resolved = catalog.resolve("orders", menus);
		assertTrue(resolved.stream().anyMatch(e -> "/api/v1/orders".equals(e.path())));
	}
}