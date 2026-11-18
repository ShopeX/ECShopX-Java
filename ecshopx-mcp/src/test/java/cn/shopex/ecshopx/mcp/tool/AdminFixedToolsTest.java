package cn.shopex.ecshopx.mcp.tool;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AdminFixedToolsTest {

	@Test
	void p0ReadToolsAreRegisteredWithExpectedPaths() {
		List<HttpToolDeclaration> all = AdminFixedTools.allDeclarations();
		Map<String, HttpToolDeclaration> byName = all.stream()
				.collect(Collectors.toMap(HttpToolDeclaration::name, d -> d, (a, b) -> a));
		assertEquals("/api/v1/permission", byName.get("menus_list").pathTemplate());
		assertEquals("GET", byName.get("orders_search").method());
		assertEquals("/api/v1/order/{order_id}", byName.get("order_get").pathTemplate());
		assertEquals("/api/v1/order/process/{orderId}", byName.get("order_process_list").pathTemplate());
		assertFalse(byName.get("menus_list").requireConfirm());
	}
}