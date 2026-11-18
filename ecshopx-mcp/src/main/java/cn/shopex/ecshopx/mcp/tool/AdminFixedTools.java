package cn.shopex.ecshopx.mcp.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class AdminFixedTools {

	private AdminFixedTools() {
	}

	public static List<HttpToolDeclaration> allDeclarations() {
		List<HttpToolDeclaration> list = new ArrayList<>();
		list.addAll(p0Reads());
		list.addAll(p1Writes());
		return list;
	}

	static List<HttpToolDeclaration> p0Reads() {
		String pageSchema = """
				{"type":"object","properties":{"page":{"type":"integer"},"pageSize":{"type":"integer"},"page_size":{"type":"integer"}},"additionalProperties":true}""";
		return List.of(
				HttpToolDeclaration.read("menus_list", "List menus visible to the current operator",
						"{\"type\":\"object\",\"properties\":{},\"additionalProperties\":false}",
						"GET", "/api/v1/permission"),
				HttpToolDeclaration.read("orders_search", "Search admin orders", pageSchema, "GET", "/api/v1/orders"),
				HttpToolDeclaration.read("order_get", "Get order detail",
						"{\"type\":\"object\",\"properties\":{\"order_id\":{\"type\":\"string\"}},\"required\":[\"order_id\"],\"additionalProperties\":true}",
						"GET", "/api/v1/order/{order_id}"),
				HttpToolDeclaration.read("order_process_list", "List order process logs",
						"{\"type\":\"object\",\"properties\":{\"orderId\":{\"type\":\"string\"}},\"required\":[\"orderId\"],\"additionalProperties\":true}",
						"GET", "/api/v1/order/process/{orderId}"),
				HttpToolDeclaration.read("refunds_search", "Search refunds", pageSchema, "GET", "/api/v1/refund"),
				HttpToolDeclaration.read("members_search", "Search members", pageSchema, "GET", "/api/v1/members"),
				HttpToolDeclaration.read("member_get", "Get member detail",
						"{\"type\":\"object\",\"properties\":{\"user_id\":{\"type\":\"string\"},\"mobile\":{\"type\":\"string\"}},\"additionalProperties\":true}",
						"GET", "/api/v1/member"),
				HttpToolDeclaration.read("goods_items_search", "Search goods items", pageSchema, "GET", "/api/v1/goods/items"),
				HttpToolDeclaration.read("distributor_info_get", "Get distributor/shop info",
						"{\"type\":\"object\",\"properties\":{},\"additionalProperties\":true}",
						"GET", "/api/v1/distributors/info"),
				HttpToolDeclaration.read("supplier_orders_search", "Search supplier orders", pageSchema,
						"GET", "/api/v1/supplier/get_order_list"),
				HttpToolDeclaration.read("datacube_goods_get", "Get goods datacube", pageSchema,
						"GET", "/api/v1/datacube/goodsdata"));
	}

	static List<HttpToolDeclaration> p1Writes() {
		return List.of(
				new HttpToolDeclaration(
						"order_remark_update",
						"Update order seller/internal remarks; requires confirm=true",
						"""
						{"type":"object","properties":{"orderId":{"type":"string"},"remark":{"type":"string"},"confirm":{"type":"boolean"}},"required":["orderId","confirm"],"additionalProperties":true}""",
						"PUT", "/api/v1/remarks/{orderId}", true, Set.of("orderId")),
				new HttpToolDeclaration(
						"order_cancel",
						"Cancel an unshipped order; requires confirm=true",
						"""
						{"type":"object","properties":{"order_id":{"type":"string"},"cancel_reason":{"type":"string"},"confirm":{"type":"boolean"}},"required":["order_id","confirm"],"additionalProperties":true}""",
						"POST", "/api/v1/order/{order_id}/cancel", true, Set.of("order_id")),
				new HttpToolDeclaration(
						"order_ship",
						"Ship order via POST /delivery; requires order_id (or API-equivalent) and confirm=true",
						"""
						{"type":"object","properties":{"order_id":{"type":"string"},"confirm":{"type":"boolean"}},"required":["order_id","confirm"],"additionalProperties":true}""",
						"POST", "/api/v1/delivery", true, Set.of("order_id")),
				new HttpToolDeclaration(
						"refund_audit",
						"Audit aftersales refund via refundCheck; requires confirm=true",
						"""
						{"type":"object","properties":{"aftersales_bn":{"type":"string"},"confirm":{"type":"boolean"}},"required":["confirm"],"additionalProperties":true}""",
						"POST", "/api/v1/aftersales/refundCheck", true, Set.of()),
				new HttpToolDeclaration(
						"goods_item_status_update",
						"Update goods item shelf status; requires confirm=true",
						"""
						{"type":"object","properties":{"item_id":{"type":"string"},"confirm":{"type":"boolean"}},"required":["confirm"],"additionalProperties":true}""",
						"PUT", "/api/v1/goods/itemstatusupdate", true, Set.of()),
				new HttpToolDeclaration(
						"goods_item_stock_update",
						"Update goods item stock; requires confirm=true",
						"""
						{"type":"object","properties":{"item_id":{"type":"string"},"confirm":{"type":"boolean"}},"required":["confirm"],"additionalProperties":true}""",
						"PUT", "/api/v1/goods/itemstoreupdate", true, Set.of()));
	}
}