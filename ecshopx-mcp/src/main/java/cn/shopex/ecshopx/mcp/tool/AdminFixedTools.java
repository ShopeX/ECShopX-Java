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
		list.addAll(p2MarketingReads());
		return list;
	}

	static List<HttpToolDeclaration> p0Reads() {
		String pageSchema = """
				{"type":"object","properties":{"page":{"type":"integer","description":"1-based page"},"pageSize":{"type":"integer","description":"page size; page_size is accepted as alias"}},"additionalProperties":true}""";
		return List.of(
				HttpToolDeclaration.read("menus_list",
						"List menus/permissions visible to the current operator (GET /permission). Use before menu_apis_resolve.",
						"{\"type\":\"object\",\"properties\":{},\"additionalProperties\":false}",
						"GET", "/api/v1/permission"),
				HttpToolDeclaration.read("orders_search",
						"Search admin entity orders (GET /orders). Defaults align with platform physical-order list: order_type=normal, order_class_exclude=drug,pointsmall,community,employee_purchase, page=1, pageSize=20. Pending-ship tab: order_status=notship (alias status=notship) — expands to PAYED + cancel_status filter + receipt_type=logistics. Prefer order_status over bare PAYED. Pagination keys: page + pageSize.",
						"""
						{"type":"object","properties":{"page":{"type":"integer"},"pageSize":{"type":"integer"},"order_type":{"type":"string","description":"default normal"},"order_class_exclude":{"type":"string"},"order_status":{"type":"string","description":"UI tab alias e.g. notship, shipping, finish; or raw PAYED"},"status":{"type":"string","description":"alias of order_status"},"mobile":{"type":"string"},"order_id":{"type":"string"}},"additionalProperties":true}""",
						"GET", "/api/v1/orders"),
				HttpToolDeclaration.read("order_get",
						"Get order detail including receiver address (list APIs may mask via DataPass). Path arg: order_id.",
						"{\"type\":\"object\",\"properties\":{\"order_id\":{\"type\":\"string\"}},\"required\":[\"order_id\"],\"additionalProperties\":true}",
						"GET", "/api/v1/order/{order_id}"),
				HttpToolDeclaration.read("order_process_list",
						"List order process/operation logs (array, not paged). Path arg orderId; order_id accepted as alias.",
						"{\"type\":\"object\",\"properties\":{\"orderId\":{\"type\":\"string\"},\"order_id\":{\"type\":\"string\"}},\"additionalProperties\":true}",
						"GET", "/api/v1/order/process/{orderId}"),
				HttpToolDeclaration.read("refunds_search",
						"Search refunds (GET /refund). Response uses total_count (not pager). Filters: refund_bn, aftersales_bn, order_id, mobile, refund_status, refund_type, time_start_begin/end. Pagination: page + pageSize.",
						pageSchema, "GET", "/api/v1/refund"),
				HttpToolDeclaration.read("members_search",
						"Search members (GET /members). Pagination page+pageSize (default 1/20). Filters: mobile, username, tag_id, grade_id, distributor_id, time_start_begin/end.",
						pageSchema, "GET", "/api/v1/members"),
				HttpToolDeclaration.read("member_get",
						"Get member detail. Provide user_id and/or mobile (at least one).",
						"{\"type\":\"object\",\"properties\":{\"user_id\":{\"type\":\"string\"},\"mobile\":{\"type\":\"string\"}},\"additionalProperties\":true}",
						"GET", "/api/v1/member"),
				HttpToolDeclaration.read("goods_items_search",
						"Search goods items (GET /goods/items). Defaults align with physical goods list: item_type=normal, operate_source=platform, distributor_id=0, page=1, pageSize=20. Backend default without item_type is services — omit only when you want services. page+pageSize are required by API (injected if missing).",
						"""
						{"type":"object","properties":{"page":{"type":"integer"},"pageSize":{"type":"integer"},"item_type":{"type":"string","description":"normal|services|...; default normal"},"operate_source":{"type":"string"},"distributor_id":{"type":"integer"},"keywords":{"type":"string"},"approve_status":{"type":"string"}},"additionalProperties":true}""",
						"GET", "/api/v1/goods/items"),
				HttpToolDeclaration.read("distributor_info_get",
						"Get distributor/shop info. Optional distributor_id (default 0 = JWT/current context). HTTP 200 may still embed status_code 422 if shop not selected.",
						"{\"type\":\"object\",\"properties\":{\"distributor_id\":{\"type\":\"integer\"}},\"additionalProperties\":true}",
						"GET", "/api/v1/distributors/info"),
				HttpToolDeclaration.read("shops_list",
						"List shops manageable by the current operator (GET /distributors). For distributor accounts use this before shop_select. Defaults: page=1, pageSize=100, is_all=true.",
						"""
						{"type":"object","properties":{"page":{"type":"integer"},"pageSize":{"type":"integer"},"is_all":{"type":"boolean"}},"additionalProperties":true}""",
						"GET", "/api/v1/distributors"),
				HttpToolDeclaration.read("supplier_orders_search",
						"Search supplier orders for the JWT supplier operator. Pagination page+pageSize (default 1/20). Date filter key is order_date (epoch seconds), not create_time aliases from other order UIs.",
						pageSchema, "GET", "/api/v1/supplier/get_order_list"),
				HttpToolDeclaration.read("datacube_goods_get",
						"Goods datacube report (not a paged list). Pass start/end as ISO dates; defaults to yesterday when omitted. Optional order_class, act_id; export=1 triggers async export.",
						"""
						{"type":"object","properties":{"start":{"type":"string"},"end":{"type":"string"},"order_class":{"type":"string"},"act_id":{"type":"string"},"export":{"type":"integer"}},"additionalProperties":true}""",
						"GET", "/api/v1/datacube/goodsdata"));
	}

	static List<HttpToolDeclaration> p1Writes() {
		return List.of(
				new HttpToolDeclaration(
						"shop_select",
						"Select current shop for a multi-shop distributor operator (POST /operator/select/distributor). Persists Redis selection used by JWT filter. Requires confirm=true and set_distributor_id.",
						"""
						{"type":"object","properties":{"set_distributor_id":{"type":"integer"},"confirm":{"type":"boolean"}},"required":["set_distributor_id","confirm"],"additionalProperties":true}""",
						"POST", "/api/v1/operator/select/distributor", true, Set.of("set_distributor_id")),
				new HttpToolDeclaration(
						"order_remark_update",
						"Update order seller/internal remarks (PUT /remarks/{orderId}). Requires confirm=true. Body: remark (required by API, max 150), is_distribution true for distributor_remark. order_id alias accepted for orderId.",
						"""
						{"type":"object","properties":{"orderId":{"type":"string"},"order_id":{"type":"string"},"remark":{"type":"string"},"is_distribution":{"type":"boolean"},"confirm":{"type":"boolean"}},"required":["confirm"],"additionalProperties":true}""",
						"PUT", "/api/v1/remarks/{orderId}", true, Set.of("orderId")),
				new HttpToolDeclaration(
						"order_cancel",
						"Cancel an unshipped normal order. Requires confirm=true and cancel_reason as integer key 1-12 (same as admin cancel dialog).",
						"""
						{"type":"object","properties":{"order_id":{"type":"string"},"cancel_reason":{"type":"integer","description":"1-12"},"other_reason":{"type":"string"},"confirm":{"type":"boolean"}},"required":["order_id","cancel_reason","confirm"],"additionalProperties":true}""",
						"POST", "/api/v1/order/{order_id}/cancel", true, Set.of("order_id")),
				new HttpToolDeclaration(
						"order_ship",
						"Ship order (POST /delivery). Requires confirm=true. Besides order_id pass admin delivery fields: delivery_type (batch|sep), delivery_corp, delivery_code for express; or self_delivery_* for merchant delivery. sep needs sepInfo JSON.",
						"""
						{"type":"object","properties":{"order_id":{"type":"string"},"delivery_type":{"type":"string"},"delivery_corp":{"type":"string"},"delivery_code":{"type":"string"},"sepInfo":{"type":"string"},"self_delivery_operator_id":{"type":"string"},"self_delivery_status":{"type":"string"},"delivery_pics":{},"delivery_remark":{"type":"string"},"confirm":{"type":"boolean"}},"required":["order_id","confirm"],"additionalProperties":true}""",
						"POST", "/api/v1/delivery", true, Set.of("order_id")),
				new HttpToolDeclaration(
						"refund_audit",
						"Audit aftersales refund (POST /aftersales/refundCheck). Requires confirm=true, aftersales_bn, check_refund (agree/reject). Reject needs refunds_memo; agree usually refund_fee/refund_point/freight in fen (cents).",
						"""
						{"type":"object","properties":{"aftersales_bn":{"type":"string"},"check_refund":{"type":"string"},"refunds_memo":{"type":"string"},"refund_fee":{"type":"integer"},"refund_point":{"type":"integer"},"freight":{"type":"integer"},"confirm":{"type":"boolean"}},"required":["aftersales_bn","check_refund","confirm"],"additionalProperties":true}""",
						"POST", "/api/v1/aftersales/refundCheck", true, Set.of("aftersales_bn")),
				new HttpToolDeclaration(
						"goods_item_status_update",
						"Batch update goods shelf status (PUT /goods/itemstatusupdate). Requires confirm=true and status (onsale|instock). Prefer items:[{goods_id}]. Single item_id is expanded to items batch.",
						"""
						{"type":"object","properties":{"items":{"type":"array"},"item_id":{"type":"string"},"status":{"type":"string"},"operate_source":{"type":"string"},"confirm":{"type":"boolean"}},"required":["status","confirm"],"additionalProperties":true}""",
						"PUT", "/api/v1/goods/itemstatusupdate", true, Set.of()),
				new HttpToolDeclaration(
						"goods_item_stock_update",
						"Batch update goods stock (PUT /goods/itemstoreupdate). Requires confirm=true. Prefer items:[{item_id,store,is_default}]. Single item_id+store is expanded to items batch.",
						"""
						{"type":"object","properties":{"items":{"type":"array"},"item_id":{"type":"string"},"store":{"type":"integer"},"is_default":{"type":"boolean"},"operate_source":{"type":"string"},"confirm":{"type":"boolean"}},"required":["confirm"],"additionalProperties":true}""",
						"PUT", "/api/v1/goods/itemstoreupdate", true, Set.of()));
	}

	static List<HttpToolDeclaration> p2MarketingReads() {
		String pageSchema = """
				{"type":"object","properties":{"page":{"type":"integer"},"pageSize":{"type":"integer"}},"additionalProperties":true}""";
		return List.of(
				HttpToolDeclaration.read("marketing_activities_search",
						"Search marketing activities (GET /marketing/getlist). marketing_type is required by API (e.g. full_discount, full_minus, full_gift). Pagination: page+pageSize.",
						"""
						{"type":"object","properties":{"marketing_type":{"type":"string"},"marketing_id":{"type":"string"},"marketing_name":{"type":"string"},"status":{"type":"string"},"item_type":{"type":"string"},"start_time":{"type":"integer"},"end_time":{"type":"integer"},"page":{"type":"integer"},"pageSize":{"type":"integer"}},"required":["marketing_type"],"additionalProperties":true}""",
						"GET", "/api/v1/marketing/getlist"),
				HttpToolDeclaration.read("marketing_activity_items_search",
						"Search marketing activity items. Pass marketing_id (required for intended list). Pagination: page+pageSize.",
						"""
						{"type":"object","properties":{"marketing_id":{"type":"string"},"page":{"type":"integer"},"pageSize":{"type":"integer"}},"required":["marketing_id"],"additionalProperties":true}""",
						"GET", "/api/v1/marketing/getItemList"),
				HttpToolDeclaration.read("seckill_activities_search",
						"Search seckill / limited-time-sale activities. Optional seckill_type=limited_time_sale for 限时特惠 UI. Pagination: page+pageSize.",
						"""
						{"type":"object","properties":{"seckill_type":{"type":"string"},"status":{"type":"string"},"keywords":{"type":"string"},"name":{"type":"string"},"page":{"type":"integer"},"pageSize":{"type":"integer"}},"additionalProperties":true}""",
						"GET", "/api/v1/promotions/seckillactivity/getlist"),
				HttpToolDeclaration.read("community_activities_search",
						"Search community group-buy activities (GET /community/list), not community leader sites. Filters: activity_name, activity_status, time_start_begin/end. Pagination: page+pageSize.",
						pageSchema, "GET", "/api/v1/community/list"),
				HttpToolDeclaration.read("coupons_search",
						"Search discount cards (GET /discountcard/list). Pagination uses page_no+page_size (page/pageSize are remapped). Default date_status=2 (effective), matching admin coupon list tab.",
						"""
						{"type":"object","properties":{"page_no":{"type":"integer"},"page_size":{"type":"integer"},"page":{"type":"integer"},"pageSize":{"type":"integer"},"date_status":{"type":"string"},"card_type":{"type":"string"},"title":{"type":"string"},"card_id":{"type":"string"},"store_self":{"type":"boolean"}},"additionalProperties":true}""",
						"GET", "/api/v1/discountcard/list"),
				HttpToolDeclaration.read("coupons_effective_search",
						"Search effective discount cards for grant pickers. Pagination page_no+page_size (page/pageSize remapped).",
						"""
						{"type":"object","properties":{"page_no":{"type":"integer"},"page_size":{"type":"integer"},"page":{"type":"integer"},"pageSize":{"type":"integer"},"card_type":{"type":"string"}},"additionalProperties":true}""",
						"GET", "/api/v1/effectiveDiscountcard/list"),
				HttpToolDeclaration.read("coupon_get",
						"Get discount card detail. card_id is required.",
						"{\"type\":\"object\",\"properties\":{\"card_id\":{\"type\":\"string\"}},\"required\":[\"card_id\"],\"additionalProperties\":true}",
						"GET", "/api/v1/discountcard/get"),
				HttpToolDeclaration.read("coupon_holders_search",
						"Search coupon holders / redeem logs. card_id required. is_use=true for redeem branch (total_count); receive list may use count. Pagination: page+pageSize.",
						"""
						{"type":"object","properties":{"card_id":{"type":"string"},"is_use":{"type":"boolean"},"mobile":{"type":"string"},"page":{"type":"integer"},"pageSize":{"type":"integer"}},"required":["card_id"],"additionalProperties":true}""",
						"GET", "/api/v1/discountcard/detail/list"),
				HttpToolDeclaration.read("coupon_grant_logs_search",
						"Search coupon grant batch logs (GET /promotions/activity/give). Pagination: page+pageSize.",
						pageSchema, "GET", "/api/v1/promotions/activity/give"),
				HttpToolDeclaration.read("coupon_grant_error_logs_search",
						"Search coupon grant error rows for a batch. Path id is give_id from grant logs.",
						"""
						{"type":"object","properties":{"id":{"type":"string"},"page":{"type":"integer"},"pageSize":{"type":"integer"}},"required":["id"],"additionalProperties":true}""",
						"GET", "/api/v1/promotions/activity/give/{id}"),
				HttpToolDeclaration.read("coupon_grant_setting_get",
						"Get company coupon grant settings (no query params).",
						"{\"type\":\"object\",\"properties\":{},\"additionalProperties\":false}",
						"GET", "/api/v1/discountcard/couponGrantSetting"));
	}
}
