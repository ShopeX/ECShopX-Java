package cn.shopex.ecshopx.mcp.tool;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Aligns MCP tool args with admin UI / backend conventions before loopback.
 * Only fills missing keys or renames aliases; never overrides explicit values.
 */
public final class AdminToolArgNormalizer {

	public static final String PLATFORM_ORDER_CLASS_EXCLUDE = "drug,pointsmall,community,employee_purchase";

	private static final Set<String> COUPON_PAGE_NO_TOOLS = Set.of(
			"coupons_search",
			"coupons_effective_search");

	private AdminToolArgNormalizer() {
	}

	public static Map<String, Object> normalize(String toolName, Map<String, Object> raw) {
		Map<String, Object> args = new LinkedHashMap<>(raw != null ? raw : Map.of());
		if (!StringUtils.hasText(toolName)) {
			return args;
		}
		switch (toolName) {
			case "orders_search" -> normalizeOrdersSearch(args);
			case "goods_items_search" -> normalizeGoodsItemsSearch(args);
			case "shops_list" -> normalizeShopsList(args);
			case "order_process_list" -> copyIfAbsent(args, "orderId", "order_id");
			case "order_remark_update" -> copyIfAbsent(args, "orderId", "order_id");
			case "coupons_search" -> normalizeCouponsSearch(args, true);
			case "coupons_effective_search" -> normalizeCouponsSearch(args, false);
			case "goods_item_status_update" -> sugarGoodsStatus(args);
			case "goods_item_stock_update" -> sugarGoodsStock(args);
			case "refunds_search", "members_search", "supplier_orders_search",
					"marketing_activities_search", "marketing_activity_items_search",
					"seckill_activities_search", "community_activities_search",
					"coupon_holders_search", "coupon_grant_logs_search",
					"coupon_grant_error_logs_search" -> applyCamelPageDefaults(args);
			default -> {
				if (!COUPON_PAGE_NO_TOOLS.contains(toolName)) {
					mapPageSizeAlias(args);
				}
			}
		}
		return args;
	}

	private static void normalizeOrdersSearch(Map<String, Object> args) {
		mapPageSizeAlias(args);
		if (args.get("order_status") == null && args.get("status") != null) {
			args.put("order_status", args.remove("status"));
		}
		putIfAbsent(args, "order_type", "normal");
		Object orderType = args.get("order_type");
		if ("normal".equals(String.valueOf(orderType)) && args.get("order_class_exclude") == null) {
			args.put("order_class_exclude", PLATFORM_ORDER_CLASS_EXCLUDE);
		}
		putIfAbsent(args, "page", 1);
		putIfAbsent(args, "pageSize", 20);
	}

	private static void normalizeGoodsItemsSearch(Map<String, Object> args) {
		mapPageSizeAlias(args);
		putIfAbsent(args, "item_type", "normal");
		putIfAbsent(args, "operate_source", "platform");
		putIfAbsent(args, "distributor_id", 0);
		putIfAbsent(args, "page", 1);
		putIfAbsent(args, "pageSize", 20);
	}

	private static void normalizeShopsList(Map<String, Object> args) {
		mapPageSizeAlias(args);
		putIfAbsent(args, "page", 1);
		putIfAbsent(args, "pageSize", 100);
		putIfAbsent(args, "is_all", true);
	}

	private static void normalizeCouponsSearch(Map<String, Object> args, boolean injectDateStatus) {
		// Prefer snake_case page keys used by discountcard APIs.
		if (args.get("page_no") == null && args.get("page") != null) {
			args.put("page_no", args.remove("page"));
		}
		else {
			args.remove("page");
		}
		if (args.get("page_size") == null) {
			Object pageSize = args.remove("pageSize");
			if (pageSize != null) {
				args.put("page_size", pageSize);
			}
		}
		else {
			args.remove("pageSize");
		}
		putIfAbsent(args, "page_no", 1);
		putIfAbsent(args, "page_size", 20);
		if (injectDateStatus) {
			putIfAbsent(args, "date_status", "2");
		}
	}

	private static void applyCamelPageDefaults(Map<String, Object> args) {
		mapPageSizeAlias(args);
		putIfAbsent(args, "page", 1);
		putIfAbsent(args, "pageSize", 20);
	}

	private static void mapPageSizeAlias(Map<String, Object> args) {
		if (args.get("pageSize") == null && args.get("page_size") != null) {
			args.put("pageSize", args.remove("page_size"));
		}
		else {
			args.remove("page_size");
		}
	}

	private static void sugarGoodsStatus(Map<String, Object> args) {
		if (args.get("items") != null) {
			return;
		}
		Object itemId = firstNonNull(args.get("item_id"), args.get("goods_id"));
		if (itemId == null) {
			return;
		}
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("goods_id", itemId);
		List<Map<String, Object>> items = new ArrayList<>();
		items.add(row);
		args.put("items", items);
	}

	private static void sugarGoodsStock(Map<String, Object> args) {
		if (args.get("items") != null) {
			return;
		}
		Object itemId = args.get("item_id");
		if (itemId == null) {
			return;
		}
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("item_id", itemId);
		if (args.get("store") != null) {
			row.put("store", args.get("store"));
		}
		row.put("is_default", args.get("is_default") != null ? args.get("is_default") : true);
		List<Map<String, Object>> items = new ArrayList<>();
		items.add(row);
		args.put("items", items);
	}

	private static void copyIfAbsent(Map<String, Object> args, String target, String alias) {
		if (args.get(target) == null && args.get(alias) != null) {
			args.put(target, args.get(alias));
		}
	}

	private static void putIfAbsent(Map<String, Object> args, String key, Object value) {
		if (args.get(key) == null) {
			args.put(key, value);
		}
	}

	private static Object firstNonNull(Object a, Object b) {
		return a != null ? a : b;
	}
}
