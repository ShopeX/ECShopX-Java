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

package cn.shopex.ecshopx.orders.service.orderexport;

import cn.shopex.ecshopx.common.orders.port.OrderExportActivityIdsLookupPort;
import cn.shopex.ecshopx.common.util.DateExpressionParser;
import cn.shopex.ecshopx.orders.service.invoice.export.InvoiceExportSalesmanResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OrderExportFilterAssembler {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

	private final InvoiceExportSalesmanResolver invoiceExportSalesmanResolver;
	private final OrderExportActivityIdsLookupPort orderExportActivityIdsLookupPort;

	public OrderExportFilterAssembler(
			InvoiceExportSalesmanResolver invoiceExportSalesmanResolver,
			OrderExportActivityIdsLookupPort orderExportActivityIdsLookupPort) {
		this.invoiceExportSalesmanResolver = invoiceExportSalesmanResolver;
		this.orderExportActivityIdsLookupPort = orderExportActivityIdsLookupPort;
	}

	public OrderExportAssemblyResult assemble(
			long companyId,
			long operatorId,
			String operatorType,
			Long merchantIdOrNull,
			List<Long> distributorIdsFromJwt,
			List<Long> shopIdsFromJwt,
			HttpServletRequest request) {
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();

		String orderTypeRaw = request.getParameter("order_type");
		String orderTypeTrim = orderTypeRaw == null ? "" : orderTypeRaw.trim();
		String typeParam = request.getParameter("type");
		String typeEffective = typeParam != null ? typeParam : "normal_order";
		if (Objects.equals("supplier_order", typeEffective)) {
			orderTypeTrim = "supplier_order";
		}

		filter.put("order_type", orderTypeTrim);

		String exportTypeForJob = typeParam != null ? typeParam : "normal_order";

		switch (orderTypeTrim) {
			case "supplier_order" -> {
				filter.put("supplier_id", Long.valueOf(operatorId));
				exportTypeForJob = "supplier_order";
			}
			case "service" -> {
				exportTypeForJob = "service_order";
				if (shopIdsFromJwt != null && !shopIdsFromJwt.isEmpty()) {
					filter.put("shop_id", new ArrayList<>(shopIdsFromJwt));
				}
				String shopReq = request.getParameter("shop_id");
				if (shopReq != null) {
					filter.put("shop_id", shopReq.trim());
				}
			}
			case "normal" -> {
				String exclude = trim(request.getParameter("order_class_exclude"));
				if (StringUtils.hasText(exclude)) {
					List<String> parts =
							Arrays.stream(exclude.split(","))
									.map(String::trim)
									.filter(StringUtils::hasText)
									.toList();
					if (!parts.isEmpty()) {
						filter.put("order_class|notin", new ArrayList<>(parts));
					}
				}
				String orderClass = trim(request.getParameter("order_class"));
				if (StringUtils.hasText(orderClass)) {
					filter.put("order_class", orderClass);
				}
				String op = operatorType == null ? "" : operatorType.trim();
				if ("staff".equalsIgnoreCase(op)) {
					if (request.getParameter("distributor_id") != null) {
						filter.put("distributor_id", String.valueOf(request.getParameter("distributor_id")));
					} else if (distributorIdsFromJwt != null && !distributorIdsFromJwt.isEmpty()) {
						filter.put("distributor_id|in", new ArrayList<>(distributorIdsFromJwt));
					}
				} else if ("supplier".equalsIgnoreCase(op)) {
					filter.put("supplier_id", Long.valueOf(operatorId));
				} else {
					if (request.getParameter("distributor_id") != null) {
						filter.put("distributor_id", String.valueOf(request.getParameter("distributor_id")));
					}
				}
			}
			default -> {
			}
		}

		putTimeBoundary(filter, request.getParameter("time_start_begin"), "create_time|gte", false);
		putTimeBoundary(filter, request.getParameter("time_start_end"), "create_time|lte", true);
		putTimeBoundary(filter, request.getParameter("delivery_time_begin"), "delivery_time|gte", false);
		putTimeBoundary(filter, request.getParameter("delivery_time_end"), "delivery_time|lte", true);
		applyOrderDateRange(request, filter);

		String orderStatus = trim(request.getParameter("order_status"));
		if (StringUtils.hasText(orderStatus) && !"0".equals(orderStatus)) {
			applyOrderStatusAlias(filter, orderStatus);
		}

		putIfText(filter, "act_id", request.getParameter("act_id"));
		putIfText(filter, "pay_type", request.getParameter("pay_type"));
		applyOrderId(filter, request.getParameter("order_id"));
		putIfText(filter, "mobile", request.getParameter("mobile"));
		putIfText(filter, "receipt_type", request.getParameter("receipt_type"));
		putIfText(filter, "user_id", request.getParameter("user_id"));
		putIfText(filter, "source_id", request.getParameter("source_id"));
		String orderClassAgain = trim(request.getParameter("order_class"));
		if (StringUtils.hasText(orderClassAgain)) {
			filter.put("order_class", orderClassAgain);
		}

		filter.put("company_id", Long.valueOf(companyId));
		if ("merchant".equalsIgnoreCase(operatorType) && merchantIdOrNull != null) {
			filter.put("merchant_id", merchantIdOrNull);
		}

		String salesmanMobile = trim(request.getParameter("salesman_mobile"));
		if (StringUtils.hasText(salesmanMobile)) {
			long sid = invoiceExportSalesmanResolver.findSalesmanIdByMobile(companyId, salesmanMobile);
			filter.put("salesman_id", Long.valueOf(sid));
		}

		String activityName = trim(request.getParameter("activity_name"));
		String activityStatus = trim(request.getParameter("activity_status"));
		if (StringUtils.hasText(activityName) || StringUtils.hasText(activityStatus)) {
			List<Long> actIds =
					orderExportActivityIdsLookupPort.lookupActivityIdsForOrderExport(
							companyId,
							StringUtils.hasText(activityName) ? activityName : null,
							StringUtils.hasText(activityStatus) ? activityStatus : null);
			List<Long> merged = new ArrayList<>();
			merged.add(Long.valueOf(0L));
			merged.addAll(actIds);
			filter.put("act_id", merged);
		}

		filter.put("datapass_block", request.getHeader("x-datapass-block"));

		if ("supplier_order".equals(orderTypeTrim)) {
			exportTypeForJob = "supplier_order";
		}

		return new OrderExportAssemblyResult(filter, exportTypeForJob, orderTypeTrim);
	}

	private static void applyOrderId(LinkedHashMap<String, Object> filter, String raw) {
		if (!StringUtils.hasText(raw)) {
			return;
		}
		String t = raw.trim();
		if (t.length() < 16) {
			filter.put("order_id|like", t);
		} else {
			filter.put("order_id", t);
		}
	}

	private static void applyOrderStatusAlias(LinkedHashMap<String, Object> f, String in) {
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
				f.put("item_delivery_status", "PENDING");
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
				f.put("invoice_not_empty", Boolean.TRUE);
				f.put("is_invoiced", Integer.valueOf(0));
			}
			case "done_invoice" -> {
				f.put("order_status", "DONE");
				f.put("invoice_not_empty", Boolean.TRUE);
				f.put("is_invoiced", Integer.valueOf(1));
			}
			default -> f.put("order_status", in.toUpperCase(Locale.ROOT));
		}
	}

	private static void applyOrderDateRange(HttpServletRequest request, LinkedHashMap<String, Object> filter) {
		String[] raw = request.getParameterValues("order_date");
		if (raw == null || raw.length < 2) {
			raw = request.getParameterValues("order_date[]");
		}
		String startRaw = null;
		String endRaw = null;
		if (raw != null && raw.length >= 2) {
			startRaw = raw[0];
			endRaw = raw[1];
		} else {
			startRaw = request.getParameter("order_date[0]");
			endRaw = request.getParameter("order_date[1]");
		}
		if (!StringUtils.hasText(startRaw) || !StringUtils.hasText(endRaw)) {
			return;
		}
		Long start = parseOrderDateEpochSeconds(startRaw.trim());
		Long end = parseOrderDateEpochSeconds(endRaw.trim());
		if (start == null || end == null) {
			return;
		}
		filter.put("create_time|gte", Integer.valueOf(start.intValue()));
		filter.put("create_time|lte", Integer.valueOf(end.intValue()));
	}

	private static Long parseOrderDateEpochSeconds(String raw) {
		try {
			return DateExpressionParser.parseToEpochSecond(raw, SHANGHAI);
		} catch (Exception e) {
			return null;
		}
	}

	private static void putTimeBoundary(
			LinkedHashMap<String, Object> f, String raw, String key, boolean endOfDayIfDateOnly) {
		if (!StringUtils.hasText(raw)) {
			return;
		}
		String t = raw.trim();
		try {
			if (t.contains("-") && t.length() >= 10) {
				String datePart = t.substring(0, 10);
				String expr = endOfDayIfDateOnly ? datePart + " 23:59:59" : datePart + " 00:00:00";
				f.put(key, DateExpressionParser.parseToEpochSecond(expr, SHANGHAI));
				return;
			}
			f.put(key, DateExpressionParser.parseToEpochSecond(t, SHANGHAI));
		} catch (Exception ignored) {
			try {
				f.put(key, Long.parseLong(t));
			} catch (NumberFormatException ignored2) {
			}
		}
	}

	private static void putIfText(LinkedHashMap<String, Object> filter, String key, String raw) {
		String t = trim(raw);
		if (StringUtils.hasText(t)) {
			filter.put(key, t);
		}
	}

	private static String trim(String s) {
		return s == null ? "" : s.trim();
	}
}
