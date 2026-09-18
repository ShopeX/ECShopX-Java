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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.common.orders.port.AdminOrderDetailDistributionSupportPort;
import cn.shopex.ecshopx.common.util.DateExpressionParser;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WxappOrderListFilterAssembler {

	private static final ZoneId ZONE = ZoneId.systemDefault();

	private final AdminOrderDetailDistributionSupportPort adminOrderDetailDistributionSupportPort;

	public WxappOrderListFilterAssembler(AdminOrderDetailDistributionSupportPort adminOrderDetailDistributionSupportPort) {
		this.adminOrderDetailDistributionSupportPort = adminOrderDetailDistributionSupportPort;
	}

	public Built built(long companyId, Map<String, Object> auth, Map<String, Object> paramMap) {
		Map<String, Object> f = new LinkedHashMap<>();
		f.put("company_id", companyId);

		if (paramMap.containsKey("time_start_begin")) {
			Object beginRaw = paramMap.get("time_start_begin");
			String beginStr = beginRaw == null ? "" : String.valueOf(beginRaw);
			putEpochBoundaryForWxappList(f, beginStr, "create_time|gte", false);
			Object endRaw = paramMap.get("time_start_end");
			String endStr = endRaw == null ? "" : String.valueOf(endRaw);
			putEpochBoundaryForWxappList(f, endStr, "create_time|lte", true);
		}

		if (paramMap.containsKey("order_id")) {
			Object v = paramMap.get("order_id");
			if (v != null && StringUtils.hasText(String.valueOf(v).trim())) {
				f.put("order_id", String.valueOf(v).trim());
			}
		}
		if (paramMap.containsKey("mobile")) {
			Object v = paramMap.get("mobile");
			if (v != null && StringUtils.hasText(String.valueOf(v).trim())) {
				f.put("mobile", String.valueOf(v).trim());
			}
		}
		if (paramMap.containsKey("activity_id")) {
			Object v = paramMap.get("activity_id");
			if (v != null && StringUtils.hasText(String.valueOf(v).trim())) {
				f.put("act_id", v);
			}
		}

		String orderType = stringParam(paramMap, "order_type", "service");
		f.put("order_type", orderType);

		Object invoiceListRaw = paramMap.get("invoice_list");
		if (invoiceListRaw == null) {
			f.put("invoice_list", 0);
		} else {
			f.put("invoice_list", invoiceListRaw);
		}

		if ("bargain".equalsIgnoreCase(orderType)) {
			paramMap.put("order_class", "bargain");
			f.put("order_class", "bargain");
		}

		String orderClassParam = stringParam(paramMap, "order_class", "");
		if ("employee_purchase".equals(orderClassParam)) {
			f.put("order_class", "employee_purchase");
		} else if (!"bargain".equalsIgnoreCase(orderType)) {
			f.put(
					"order_class|in",
					List.of(
							"normal",
							"groups",
							"seckill",
							"shopguide",
							"shopadmin",
							"bargain",
							"pointsmall",
							"excard",
							"drug",
							"employee_purchase"));
		}

		if (paramMap.containsKey("order_status") && StringUtils.hasText(stringParam(paramMap, "order_status", ""))) {
			if (!"service".equalsIgnoreCase(orderType)) {
				f.put("order_status|neq", "NOTPAY");
			} else {
				f.put("order_status", "DONE");
			}
		}

		if (paramMap.containsKey("delivery_status") && truthyParam(paramMap.get("delivery_status"))) {
			f.put("delivery_status", String.valueOf(paramMap.get("delivery_status")).trim());
		}
		if (paramMap.containsKey("self_delivery_status") && truthyParam(paramMap.get("self_delivery_status"))) {
			f.put("self_delivery_status", String.valueOf(paramMap.get("self_delivery_status")).trim());
		}
		if (paramMap.containsKey("self_delivery_operator_id") && truthyParam(paramMap.get("self_delivery_operator_id"))) {
			f.put("self_delivery_operator_id", String.valueOf(paramMap.get("self_delivery_operator_id")).trim());
		}

		if (paramMap.containsKey("status")) {
			f.remove("delivery_status");
			f.remove("order_status|neq");
			applyStatusSwitch(f, paramMap);
		}

		String isDistRaw = paramMap.get("is_distribution") == null ? "" : String.valueOf(paramMap.get("is_distribution"));
		boolean isDistribution = !isDistRaw.equals("");
		if (isDistribution) {
			f.put("is_distribution", Boolean.TRUE);
		}

		if (isDistribution) {
			String mobileFromAuth = auth.get("mobile") == null ? "" : String.valueOf(auth.get("mobile"));
			Map<String, Object> infoRow =
					adminOrderDetailDistributionSupportPort.resolveWxappListDistributorFilter(companyId, mobileFromAuth);
			long distributorIdForFilter = 0L;
			if (infoRow != null && !infoRow.isEmpty()) {
				Object raw = infoRow.get("distributor_id");
				if (raw instanceof Number && ((Number) raw).longValue() > 0L) {
					distributorIdForFilter = ((Number) raw).longValue();
				} else if (raw != null) {
					String s = String.valueOf(raw).trim();
					if (!s.isEmpty()) {
						try {
							long v = Long.parseLong(s);
							if (v > 0L) {
								distributorIdForFilter = v;
							}
						} catch (NumberFormatException ignored) {
						}
					}
				}
			}
			f.put("distributor_id", distributorIdForFilter);
		}

		if (paramMap.containsKey("distributor_id") && truthyParam(paramMap.get("distributor_id"))) {
			f.put("distributor_id", longVal(paramMap.get("distributor_id")));
		}

		boolean selfDeliveryOpTruthy =
				paramMap.containsKey("self_delivery_operator_id") && truthyParam(paramMap.get("self_delivery_operator_id"));

		boolean salesManMode = false;
		if (truthyParam(paramMap.get("promoter_user_id")) && truthyParam(paramMap.get("isSalesmanPage"))) {
			salesManMode = true;
			f.remove("user_id");
			f.put("salesman_id", longVal(paramMap.get("promoter_user_id")));
			f.put("order_source", "salesperson");
		}
		if (!salesManMode && !f.containsKey("distributor_id") && !selfDeliveryOpTruthy) {
			f.put("user_id", auth.get("user_id"));
		}

		boolean step13EmptyWhenFalsyUser = !f.containsKey("distributor_id") && !selfDeliveryOpTruthy;

		f.remove("is_distribution");
		return new Built(f, step13EmptyWhenFalsyUser, salesManMode);
	}

	private static void applyStatusSwitch(Map<String, Object> f, Map<String, Object> paramMap) {
		int status = intVal(paramMap.get("status"));
		switch (status) {
			case 1 -> {
				// 待收货（前端 tab / 会员中心入口均传 status=1）
				f.put("order_status", "WAIT_BUYER_CONFIRM");
				f.put("delivery_status", "DONE");
				f.put("ziti_status", "NOTZITI");
			}
			case 2 -> {
				// 与 case 1 同义（历史「待收货」入口，仍保留兼容）
				f.put("order_status", "WAIT_BUYER_CONFIRM");
				f.put("delivery_status", "DONE");
			}
			case 3 -> {
				f.put("order_status", "DONE");
				f.put("delivery_status", "DONE");
				f.put("ziti_status", "DONE");
			}
			case 4 -> {
				f.put("order_status", "PAYED");
				f.put("ziti_status", "PENDING");
			}
			case 5 -> f.put("order_status", "NOTPAY");
			case 6 -> {
				f.put("order_status", "PAYED");
				f.put("ziti_status", "NOTZITI");
			}
			case 7 -> {
				f.put("order_status", "DONE");
				Object isRate = paramMap.get("is_rate");
				f.put("is_rate", isRate == null ? 0 : isRate);
			}
			default -> {
			}
		}
	}

	public record Built(Map<String, Object> filter, boolean step13EmptyWhenFalsyUser, boolean salesManMode) {}

	public static boolean looseAuthUserIdFalsy(Object userId) {
		if (userId == null) {
			return true;
		}
		if (Boolean.FALSE.equals(userId)) {
			return true;
		}
		if (userId instanceof Number n && n.longValue() == 0L) {
			return true;
		}
		if (userId instanceof CharSequence cs) {
			String t = cs.toString().trim();
			return t.isEmpty() || "0".equals(t);
		}
		if (userId instanceof Map<?, ?> m && m.isEmpty()) {
			return true;
		}
		if (userId instanceof Iterable<?> it && !it.iterator().hasNext()) {
			return true;
		}
		if (userId instanceof Object[] arr && arr.length == 0) {
			return true;
		}
		return false;
	}

	private static void putEpochBoundaryForWxappList(
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

	private static String stringParam(Map<String, Object> paramMap, String key, String dflt) {
		Object v = paramMap.get(key);
		if (v == null) {
			return dflt;
		}
		String s = String.valueOf(v).trim();
		return s.isEmpty() ? dflt : s;
	}

	private static boolean truthyParam(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.longValue() != 0L;
		}
		String s = String.valueOf(v).trim();
		return !s.isEmpty() && !"0".equals(s) && !"false".equalsIgnoreCase(s);
	}

	private static int intVal(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
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
}
