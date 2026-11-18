package cn.shopex.ecshopx.mcp.tool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

	@Test
	void p1WriteToolsRequireConfirmAndPaths() {
		Map<String, HttpToolDeclaration> byName = AdminFixedTools.allDeclarations().stream()
				.collect(Collectors.toMap(HttpToolDeclaration::name, d -> d, (a, b) -> a));
		assertTrue(byName.get("order_remark_update").requireConfirm());
		assertEquals("/api/v1/remarks/{orderId}", byName.get("order_remark_update").pathTemplate());
		assertEquals("PUT", byName.get("order_remark_update").method());
		assertEquals("/api/v1/order/{order_id}/cancel", byName.get("order_cancel").pathTemplate());
		assertEquals("/api/v1/delivery", byName.get("order_ship").pathTemplate());
		assertEquals("POST", byName.get("order_ship").method());
		assertEquals("/api/v1/aftersales/refundCheck", byName.get("refund_audit").pathTemplate());
		assertEquals("/api/v1/goods/itemstatusupdate", byName.get("goods_item_status_update").pathTemplate());
		assertEquals("/api/v1/goods/itemstoreupdate", byName.get("goods_item_stock_update").pathTemplate());
	}

	static Stream<Arguments> p2MarketingToolNameAndPath() {
		return Stream.of(
				Arguments.of("marketing_activities_search", "/api/v1/marketing/getlist"),
				Arguments.of("marketing_activity_items_search", "/api/v1/marketing/getItemList"),
				Arguments.of("seckill_activities_search", "/api/v1/promotions/seckillactivity/getlist"),
				Arguments.of("community_activities_search", "/api/v1/community/list"),
				Arguments.of("coupons_search", "/api/v1/discountcard/list"),
				Arguments.of("coupons_effective_search", "/api/v1/effectiveDiscountcard/list"),
				Arguments.of("coupon_get", "/api/v1/discountcard/get"),
				Arguments.of("coupon_holders_search", "/api/v1/discountcard/detail/list"),
				Arguments.of("coupon_grant_logs_search", "/api/v1/promotions/activity/give"),
				Arguments.of("coupon_grant_error_logs_search", "/api/v1/promotions/activity/give/{id}"),
				Arguments.of("coupon_grant_setting_get", "/api/v1/discountcard/couponGrantSetting"));
	}

	@ParameterizedTest
	@MethodSource("p2MarketingToolNameAndPath")
	void p2MarketingReadToolsUseExpectedPaths(String toolName, String expectedPath) {
		Map<String, HttpToolDeclaration> byName = AdminFixedTools.allDeclarations().stream()
				.collect(Collectors.toMap(HttpToolDeclaration::name, d -> d, (a, b) -> a));
		HttpToolDeclaration decl = byName.get(toolName);
		assertEquals(expectedPath, decl.pathTemplate());
		assertEquals("GET", decl.method());
		assertFalse(decl.requireConfirm());
	}

	@Test
	void memberCouponGrantIsNotInFixedDeclarations() {
		List<String> names = AdminFixedTools.allDeclarations().stream()
				.map(HttpToolDeclaration::name)
				.toList();
		assertFalse(names.contains("member_coupon_grant"));
	}
}