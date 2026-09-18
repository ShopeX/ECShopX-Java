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

package cn.shopex.ecshopx.orders.service.invoice.export;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.util.DateExpressionParser;
import jakarta.servlet.http.HttpServletRequest;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class InvoiceExportFilterAssembler {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

	private static final List<String> INVOICE_STATUS_EXPORT =
			List.of("pending", "success", "inProgress", "waste", "failed");

	private final InvoiceExportSalesmanResolver invoiceExportSalesmanResolver;

	public InvoiceExportFilterAssembler(InvoiceExportSalesmanResolver invoiceExportSalesmanResolver) {
		this.invoiceExportSalesmanResolver = invoiceExportSalesmanResolver;
	}

	public LinkedHashMap<String, Object> assemble(
			long companyId,
			long operatorId,
			String operatorType,
			Long merchantIdOrNull,
			List<Long> distributorIdsFromJwt,
			HttpServletRequest request) {
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		String orderTypeRaw = request.getParameter("order_type");
		String typeNorm = orderTypeRaw == null ? "" : orderTypeRaw.trim().toLowerCase(Locale.ROOT);

		if ("normal".equals(typeNorm)) {
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
			filter.put("order_type", "normal");
			boolean staff = "staff".equalsIgnoreCase(operatorType == null ? "" : operatorType.trim());
			if (staff) {
				String distReq = trim(request.getParameter("distributor_id"));
				if (StringUtils.hasText(distReq)) {
					filter.put("distributor_id", longVal(distReq));
				} else if (distributorIdsFromJwt != null && !distributorIdsFromJwt.isEmpty()) {
					filter.put("distributor_id|in", new ArrayList<>(distributorIdsFromJwt));
				}
			} else {
				String distReq = trim(request.getParameter("distributor_id"));
				if (StringUtils.hasText(distReq)) {
					filter.put("distributor_id", longVal(distReq));
				}
			}
		} else if ("supplier_order".equals(typeNorm)) {
			if (operatorId > Integer.MAX_VALUE) {
				throw new BadRequestException("operator_id 无效");
			}
			filter.put("supplier_id", operatorId);
		}

		String timeStartBegin = request.getParameter("time_start_begin");
		if (StringUtils.hasText(timeStartBegin)) {
			filter.put("create_time|gte", timeStartBegin.trim());
			String timeStartEnd = request.getParameter("time_start_end");
			if (StringUtils.hasText(timeStartEnd)) {
				filter.put("create_time|lte", timeStartEnd.trim());
			}
		}

		applyOrderDateRange(request, filter);

		String orderStatus = trim(request.getParameter("order_status"));
		if (StringUtils.hasText(orderStatus) && !"0".equals(orderStatus)) {
			applyOrderStatusFilter(filter, orderStatus);
		}

		putIfText(filter, "pay_type", request.getParameter("pay_type"));
		putIfText(filter, "order_id", request.getParameter("order_id"));
		putIfText(filter, "mobile", request.getParameter("mobile"));
		putIfText(filter, "user_id", request.getParameter("user_id"));
		putIfText(filter, "source_id", request.getParameter("source_id"));
		String orderClassAgain = trim(request.getParameter("order_class"));
		if (StringUtils.hasText(orderClassAgain)) {
			filter.put("order_class", orderClassAgain);
		}

		filter.put("company_id", companyId);
		if ("merchant".equals(operatorType) && merchantIdOrNull != null) {
			filter.put("merchant_id", merchantIdOrNull);
		}

		String salesmanMobile = trim(request.getParameter("salesman_mobile"));
		if (StringUtils.hasText(salesmanMobile)) {
			long sid = invoiceExportSalesmanResolver.findSalesmanIdByMobile(companyId, salesmanMobile);
			filter.put("salesman_id", Long.valueOf(sid));
		}

		filter.put("invoice_status|in", new ArrayList<>(INVOICE_STATUS_EXPORT));
		return filter;
	}

	private static void applyOrderDateRange(HttpServletRequest request, LinkedHashMap<String, Object> filter) {
		String[] raw = request.getParameterValues("order_date");
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
		Long start = parseEpochSeconds(startRaw.trim());
		Long end = parseEpochSeconds(endRaw.trim());
		if (start == null || end == null) {
			return;
		}
		filter.put("create_time|gte", Integer.valueOf(start.intValue()));
		filter.put("create_time|lte", Integer.valueOf(end.intValue()));
	}

	private static Long parseEpochSeconds(String raw) {
		try {
			return DateExpressionParser.parseToEpochSecond(raw, SHANGHAI);
		} catch (Exception e) {
			return null;
		}
	}

	private static void applyOrderStatusFilter(LinkedHashMap<String, Object> filter, String status) {
		switch (status) {
			case "refundprocess" -> {
				filter.put("order_status", "CANCEL_WAIT_PROCESS");
				filter.put("cancel_status", "WAIT_PROCESS");
			}
			case "ordercancel" -> {
				filter.put("order_status", "CANCEL");
				filter.put("cancel_status", "NO_APPLY_CANCEL");
			}
			case "refundsuccess" -> {
				filter.put("order_status", "CANCEL");
				filter.put("cancel_status", "SUCCESS");
			}
			case "notship" -> {
				filter.put("order_status", "PAYED");
				filter.put("ziti_status", "NOTZITI");
			}
			case "finish" -> filter.put("order_status", "DONE");
			case "done_noinvoice" -> {
				filter.put("order_status", "DONE");
				filter.put("invoice_not_empty", Boolean.TRUE);
				filter.put("is_invoiced", Integer.valueOf(0));
			}
			case "done_invoice" -> {
				filter.put("order_status", "DONE");
				filter.put("invoice_not_empty", Boolean.TRUE);
				filter.put("is_invoiced", Integer.valueOf(1));
			}
			default -> filter.put("order_status", status.toUpperCase(Locale.ROOT));
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

	private static long longVal(String s) {
		try {
			return Long.parseLong(s.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
