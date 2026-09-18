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

package cn.shopex.ecshopx.community.service.admin;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CommunityAdminOrderListFilterBuilder {

	private static final ZoneId ZONE = ZoneId.systemDefault();

	public Map<String, Object> build(long companyId, Map<String, Object> jwtUser, HttpServletRequest request) {
		Map<String, Object> f = new LinkedHashMap<>();
		f.put("company_id", companyId);

		String operatorType = str(jwtUser.get("operator_type"));
		if ("merchant".equalsIgnoreCase(operatorType)) {
			Object mid = jwtUser.get("merchant_id");
			if (mid != null && StringUtils.hasText(String.valueOf(mid))) {
				f.put("merchant_id", mid);
			}
		}

		int page = parsePage(request.getParameter("page"));
		int pageSize = parsePageSize(request.getParameter("pageSize"));
		f.put("page", page);
		f.put("pageSize", pageSize);
		String orderBy = request.getParameter("order_by");
		f.put("order_by", orderBy == null ? "" : orderBy);

		applyCreateTimeRange(f, request);
		applyDeliveryTimeRange(f, request);

		String orderStatus = request.getParameter("order_status");
		if (StringUtils.hasText(orderStatus)) {
			applyOrderStatusAlias(f, orderStatus.trim());
		}

		if ("NOTPAY".equals(String.valueOf(f.get("order_status")))) {
			f.put("auto_cancel_time|gt", System.currentTimeMillis() / 1000L);
		}

		putStringEq(f, request, "receiver_name");
		putStringEq(f, request, "item_name");
		applyOrderIdCondition(f, request.getParameter("order_id"));
		putLikeIfPresent(f, request, "title", "title|like");
		putStringEq(f, request, "mobile");
		putStringEq(f, request, "user_id");
		putStringEq(f, request, "source_id");

		f.put("distributor_id", 0L);
		if ("distributor".equalsIgnoreCase(operatorType)) {
			String did = request.getParameter("distributor_id");
			if (StringUtils.hasText(did)) {
				f.put("distributor_id", parseLongSafe(did, 0L));
			}
		}

		if (!f.containsKey("shop_id")) {
			String shopId = request.getParameter("shop_id");
			if (StringUtils.hasText(shopId)) {
				f.put("shop_id", shopId.trim());
			}
		}

		String receiptType = request.getParameter("receipt_type");
		if (StringUtils.hasText(receiptType)) {
			f.put("receipt_type", receiptType.trim());
		}
		String isInvoiced = request.getParameter("is_invoiced");
		if (StringUtils.hasText(isInvoiced)) {
			f.put("is_invoiced", isInvoiced.trim());
		}

		String activityName = request.getParameter("activity_name");
		if (StringUtils.hasText(activityName)) {
			f.put("activity_name", activityName.trim());
		}
		String activityStatus = request.getParameter("activity_status");
		if (StringUtils.hasText(activityStatus)) {
			f.put("activity_status", activityStatus.trim());
		}

		f.put("order_class", "community");
		return f;
	}

	private static void applyCreateTimeRange(Map<String, Object> f, HttpServletRequest request) {
		String begin = request.getParameter("time_start_begin");
		String end = request.getParameter("time_start_end");
		if (StringUtils.hasText(begin)) {
			f.put("create_time|gte", parseTimeBoundary(begin.trim(), true));
		}
		if (StringUtils.hasText(end)) {
			f.put("create_time|lte", parseTimeBoundary(end.trim(), false));
		}
	}

	private static void applyDeliveryTimeRange(Map<String, Object> f, HttpServletRequest request) {
		String begin = request.getParameter("delivery_time_begin");
		String end = request.getParameter("delivery_time_end");
		if (StringUtils.hasText(begin)) {
			f.put("delivery_time|gte", parseTimeBoundary(begin.trim(), true));
		}
		if (StringUtils.hasText(end)) {
			f.put("delivery_time|lte", parseTimeBoundary(end.trim(), false));
		}
	}

	/**
	 * 解析筛选时间边界：若含日期分隔符则按本地时区取当日 00:00:00 或当日结束对应的 Unix 秒；否则按整型时间戳解析。
	 */
	private static long parseTimeBoundary(String raw, boolean startOfDay) {
		if (raw.contains("-")) {
			String datePart = raw.trim();
			if (datePart.length() >= 10) {
				datePart = datePart.substring(0, 10);
			}
			LocalDate d = LocalDate.parse(datePart);
			if (startOfDay) {
				return d.atStartOfDay(ZONE).toEpochSecond();
			}
			return d.plusDays(1).atStartOfDay(ZONE).toEpochSecond() - 1;
		}
		return parseLongSafe(raw, 0L);
	}

	private static void applyOrderStatusAlias(Map<String, Object> f, String in) {
		String s = in.toLowerCase();
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
			default -> f.put("order_status", in.toUpperCase());
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

	private static void putStringEq(Map<String, Object> f, HttpServletRequest request, String param) {
		String v = request.getParameter(param);
		if (StringUtils.hasText(v)) {
			f.put(param, v.trim());
		}
	}

	private static void putLikeIfPresent(Map<String, Object> f, HttpServletRequest request, String param, String key) {
		String v = request.getParameter(param);
		if (StringUtils.hasText(v)) {
			f.put(key, v.trim());
		}
	}

	private static int parsePage(String raw) {
		if (!StringUtils.hasText(raw)) {
			return 1;
		}
		int p = (int) parseLongSafe(raw.trim(), 1L);
		return p < 1 ? 1 : p;
	}

	private static int parsePageSize(String raw) {
		if (!StringUtils.hasText(raw)) {
			return 20;
		}
		int n = (int) parseLongSafe(raw.trim(), 20L);
		if (n == 0) {
			return 20;
		}
		return n;
	}

	private static long parseLongSafe(String s, long dflt) {
		try {
			return Long.parseLong(s.trim());
		} catch (NumberFormatException e) {
			return dflt;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
	}
}
