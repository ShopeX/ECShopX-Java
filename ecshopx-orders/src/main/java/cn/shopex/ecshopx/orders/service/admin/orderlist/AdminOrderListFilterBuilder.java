/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.orders.service.admin.orderlist;

import cn.shopex.ecshopx.common.util.DateExpressionParser;
import jakarta.servlet.http.HttpServletRequest;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdminOrderListFilterBuilder {

	private static final ZoneId ZONE = ZoneId.systemDefault();

	private final AdminOrderListDistributorSalesmanLookup distributorSalesmanLookup;
	private final AdminOrderListSupplierOperatorIdsLookup supplierOperatorIdsLookup;

	public AdminOrderListFilterBuilder(
			AdminOrderListDistributorSalesmanLookup distributorSalesmanLookup,
			AdminOrderListSupplierOperatorIdsLookup supplierOperatorIdsLookup) {
		this.distributorSalesmanLookup = distributorSalesmanLookup;
		this.supplierOperatorIdsLookup = supplierOperatorIdsLookup;
	}

	public BuiltFilter build(long companyId, Map<String, Object> jwt, HttpServletRequest request) {
		Map<String, Object> f = new LinkedHashMap<>();
		f.put("company_id", companyId);

		String orderClassParam = request.getParameter("order_class");

		String operatorType = str(jwt.get("operator_type"));
		if ("merchant".equalsIgnoreCase(operatorType)) {
			Object mid = jwt.get("merchant_id");
			if (mid != null && StringUtils.hasText(String.valueOf(mid))) {
				f.put("merchant_id", mid);
			}
		}
		if ("supplier".equalsIgnoreCase(operatorType)) {
			Object op = jwt.get("operator_id");
			if (op != null && StringUtils.hasText(String.valueOf(op))) {
				f.put("supplier_id", op);
			}
		}

		applyJwtShopAndDistributorScope(f, jwt, request, operatorType, companyId);

		applyCreateTimeRange(f, request);
		applyDeliveryTimeRange(f, request);

		putStringParam(f, request, "pay_type");
		if ("point".equals(orderClassParam) || "deposit".equals(orderClassParam)) {
			f.put("pay_type", orderClassParam);
		}
		putStringParam(f, request, "purchase_mode");
		putStringParam(f, request, "receiver_name");
		putStringParam(f, request, "receiver_mobile");
		putStringParam(f, request, "invoice_status");
		putStringParam(f, request, "mobile");
		putStringParam(f, request, "user_id");
		putStringParam(f, request, "source_id");
		putStringParam(f, request, "monitor_id");
		putStringParam(f, request, "act_id");
		putStringParam(f, request, "enterprise_id");
		putStringParam(f, request, "source_from");
		putStringParam(f, request, "promoter_identity");
		putStringParam(f, request, "promoter_mobile");
		putStringParam(f, request, "receipt_type");
		putStringParam(f, request, "self_delivery_status");
		putStringParam(f, request, "distributor_type");
		String orderTypeForClassExclude = request.getParameter("order_type");
		if (orderTypeForClassExclude != null && "normal".equalsIgnoreCase(orderTypeForClassExclude.trim())) {
			String ocEx = request.getParameter("order_class_exclude");
			if (StringUtils.hasText(ocEx)) {
				List<String> parts = new ArrayList<>();
				for (String p : ocEx.split(",")) {
					String t = p.trim();
					if (StringUtils.hasText(t)) {
						parts.add(t);
					}
				}
				if (!parts.isEmpty()) {
					f.put("order_class|notin", parts);
				}
			}
		}
		putStringParam(f, request, "is_invoiced");

		String shopIdReq = request.getParameter("shop_id");
		if (StringUtils.hasText(shopIdReq)) {
			f.put("shop_id", shopIdReq.trim());
		}
		String distReq = request.getParameter("distributor_id");
		if (StringUtils.hasText(distReq)) {
			f.put("distributor_id", distReq.trim());
		}
		putStringParam(f, request, "subdistrict_parent_id");
		putStringParam(f, request, "subdistrict_id");

		String deliveryStaffId = request.getParameter("delivery_staff_id");
		if (StringUtils.hasText(deliveryStaffId)) {
			f.put("self_delivery_operator_id", deliveryStaffId.trim());
		}

		String itemName = request.getParameter("item_name");
		if (StringUtils.hasText(itemName)) {
			f.put("item_name", itemName.trim());
		}
		applyTitleLike(f, request);
		applyOrderHolder(f, request);
		applyOrderIdCondition(f, request.getParameter("order_id"));

		Map<String, Object> filterBak = new LinkedHashMap<>(f);
		String orderStatus = request.getParameter("order_status");
		if (queryParamTruthy(orderStatus)) {
			applyOrderStatusAlias(f, orderStatus.trim());
		}

		String mainStatus = request.getParameter("main_status");
		if (queryParamTruthy(mainStatus)) {
			f.clear();
			f.putAll(filterBak);
			applyMainStatus(f, mainStatus.trim());
		}

		applySalesmanMobile(f, companyId, request);
		applySupplierName(f, companyId, request);
		applyPrescriptionFilters(f, request);

		return new BuiltFilter(f);
	}

	public record BuiltFilter(Map<String, Object> filter) {}

	private void applyJwtShopAndDistributorScope(
			Map<String, Object> f,
			Map<String, Object> jwt,
			HttpServletRequest request,
			String operatorType,
			long companyId) {
		String orderTypeParam = request.getParameter("order_type");
		if (orderTypeParam == null) {
			orderTypeParam = "";
		}
		boolean serviceOrNormal =
				"service".equalsIgnoreCase(orderTypeParam) || "normal".equalsIgnoreCase(orderTypeParam);
		if (!serviceOrNormal && !orderTypeParam.isEmpty()) {
			return;
		}
		if (!StringUtils.hasText(operatorType)) {
			return;
		}
		String ot = operatorType.toLowerCase(Locale.ROOT);
		if ("admin".equals(ot)) {
			return;
		}
		List<Long> jwtShopIds = extractNestedIds(jwt.get("shop_ids"), "shop_id");
		if (!jwtShopIds.isEmpty() && !f.containsKey("shop_id")) {
			f.put("shop_id|in", jwtShopIds);
		}
		if ("distributor".equals(ot) && !f.containsKey("distributor_id")) {
			long distributorId = longVal(jwt.get("distributor_id"));
			if (distributorId > 0L) {
				f.put("distributor_id", distributorId);
			}
		}
	}

	private static List<Long> extractNestedIds(Object raw, String key) {
		List<Long> out = new ArrayList<>();
		if (!(raw instanceof List<?> list)) {
			return out;
		}
		for (Object row : list) {
			if (row instanceof Map<?, ?> m) {
				Object id = m.get(key);
				if (id == null) {
					continue;
				}
				try {
					out.add(Long.parseLong(String.valueOf(id).trim()));
				} catch (NumberFormatException ignored) {
				}
			} else if (row instanceof Number n) {
				out.add(n.longValue());
			}
		}
		return out;
	}

	private void applySalesmanMobile(Map<String, Object> f, long companyId, HttpServletRequest request) {
		String raw = request.getParameter("salesman_mobile");
		if (!StringUtils.hasText(raw)) {
			return;
		}
		long sid = distributorSalesmanLookup.resolveSalesmanIdByMobileOrMinusOne(companyId, raw.trim());
		f.put("salesman_id", sid);
	}

	private void applySupplierName(Map<String, Object> f, long companyId, HttpServletRequest request) {
		String raw = request.getParameter("supplier_name");
		if (!StringUtils.hasText(raw)) {
			return;
		}
		List<Long> opIds = supplierOperatorIdsLookup.listOperatorIdsBySupplierNameContains(companyId, raw.trim());
		if (opIds.isEmpty()) {
			f.put("order_id", "0");
		} else {
			f.put("supplier_operator_id|in", opIds);
		}
	}

	private static void applyPrescriptionFilters(Map<String, Object> f, HttpServletRequest request) {
		String flag = request.getParameter("is_prescription_order");
		if (!"0".equals(flag) && !"1".equals(flag)) {
			return;
		}
		f.put("is_prescription_order", flag);
		putStringParam(f, request, "user_family_name");
		putStringParam(f, request, "user_family_id_card");
		putStringParam(f, request, "user_family_phone");
		putStringParam(f, request, "user_family_gender");
		putStringParam(f, request, "user_family_age");
	}

	private static void applyTitleLike(Map<String, Object> f, HttpServletRequest request) {
		String t = request.getParameter("title");
		if (StringUtils.hasText(t)) {
			f.put("title|like", t.trim());
		}
	}

	private static void applyOrderHolder(Map<String, Object> f, HttpServletRequest request) {
		String raw = request.getParameter("order_holder");
		if (!StringUtils.hasText(raw)) {
			return;
		}
		String[] parts = raw.split(",");
		List<String> vals = new ArrayList<>();
		for (String p : parts) {
			if (StringUtils.hasText(p.trim())) {
				vals.add(p.trim());
			}
		}
		if (!vals.isEmpty()) {
			f.put("order_holder|in", vals);
		}
	}

	private static void applyOrderIdCondition(Map<String, Object> f, String orderId) {
		if (!StringUtils.hasText(orderId)) {
			return;
		}
		String t = orderId.trim();
		if (t.length() < 16) {
			f.put("order_id|like", t);
		} else {
			f.put("order_id", t);
		}
	}

	private static void applyCreateTimeRange(Map<String, Object> f, HttpServletRequest request) {
		putEpochBoundary(f, request.getParameter("time_start_begin"), "create_time|gte", false);
		putEpochBoundary(f, request.getParameter("time_start_end"), "create_time|lte", true);
	}

	private static void applyDeliveryTimeRange(Map<String, Object> f, HttpServletRequest request) {
		putEpochBoundary(f, request.getParameter("delivery_time_begin"), "delivery_time|gte", false);
		putEpochBoundary(f, request.getParameter("delivery_time_end"), "delivery_time|lte", true);
	}

	private static void putEpochBoundary(
			Map<String, Object> f, String raw, String key, boolean endOfDayIfDateOnly) {
		if (!StringUtils.hasText(raw)) {
			return;
		}
		String t = raw.trim();
		try {
			if (t.contains("-") && t.length() >= 10) {
				String datePart = t.substring(0, 10);
				String expr = endOfDayIfDateOnly ? datePart + " 23:59:59" : datePart + " 00:00:00";
				f.put(key, DateExpressionParser.parseToEpochSecond(expr, ZONE));
				return;
			}
			f.put(key, DateExpressionParser.parseToEpochSecond(t, ZONE));
		} catch (Exception ignored) {
			try {
				f.put(key, Long.parseLong(t));
			} catch (NumberFormatException ignored2) {
			}
		}
	}

	private static void putStringParam(Map<String, Object> f, HttpServletRequest request, String param) {
		String v = request.getParameter(param);
		if (StringUtils.hasText(v)) {
			f.put(param, v.trim());
		}
	}

	private static void applyOrderStatusAlias(Map<String, Object> f, String in) {
		String s = in.toLowerCase(Locale.ROOT);
		switch (s) {
			case "ordercancel" -> {
				f.put("order_status", "CANCEL_WAIT_PROCESS");
				f.put("cancel_status", "WAIT_PROCESS");
			}
			case "refundprocess" -> {
				f.put("order_status", "CANCEL");
				f.put("cancel_status", "NO_APPLY_CANCEL");
			}
			case "refundsuccess" -> {
				f.put("order_status", "CANCEL");
				f.put("cancel_status", "SUCCESS");
			}
			case "notship" -> {
				f.put("order_status", "PAYED");
				f.put("cancel_status|in", List.of("NO_APPLY_CANCEL", "FAILS"));
				f.put("receipt_type", "logistics");
			}
			case "cancelapply" -> {
				f.put("order_status", "PAYED");
				f.put("cancel_status", "WAIT_PROCESS");
			}
			case "ziti" -> {
				f.put("receipt_type", "ziti");
				f.put("order_status", "PAYED");
				f.put("ziti_status", "PENDING");
			}
			case "shipping" -> {
				f.put("order_status", "WAIT_BUYER_CONFIRM");
				f.put("delivery_status|in", List.of("DONE", "PARTAIL"));
				f.put("receipt_type", "logistics");
			}
			case "finish" -> f.put("order_status", "DONE");
			case "reviewpass" -> f.put("order_status", "REVIEW_PASS");
			case "done_noinvoice" -> {
				f.put("order_status", "DONE");
				f.put("invoice_not_empty", true);
				f.put("is_invoiced", "0");
			}
			case "done_invoice" -> {
				f.put("order_status", "DONE");
				f.put("invoice_not_empty", true);
				f.put("is_invoiced", "1");
			}
			default -> f.put("order_status", in.toUpperCase(Locale.ROOT));
		}
	}

	private static void applyMainStatus(Map<String, Object> f, String mainStatus) {
		applyOrderStatusAlias(f, mainStatus);
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static boolean queryParamTruthy(String raw) {
		if (raw == null) {
			return false;
		}
		String t = raw.trim();
		return !t.isEmpty() && !"0".equals(t);
	}
}
