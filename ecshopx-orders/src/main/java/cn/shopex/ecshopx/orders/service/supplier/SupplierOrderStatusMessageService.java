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

package cn.shopex.ecshopx.orders.service.supplier;

import cn.shopex.ecshopx.orders.service.admin.OrderAppAttachAdminBuilder;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SupplierOrderStatusMessageService {

	private final OrdersShopexErpSettingRedisReader erpSettingRedisReader;

	public SupplierOrderStatusMessageService(OrdersShopexErpSettingRedisReader erpSettingRedisReader) {
		this.erpSettingRedisReader = erpSettingRedisReader;
	}

	public String getOrderStatusMsgForSupplier(Map<String, Object> orderRow) {
		return resolveStatusMessage(orderRow, null);
	}

	private String resolveStatusMessage(Map<String, Object> order, Map<String, Object> dadaData) {
		String orderStatus = str(order.get("order_status"));
		String receiptType = str(order.get("receipt_type"));
		long companyId = longLoose(order.get("company_id"));
		switch (orderStatus) {
			case "WAIT_GROUPS_SUCCESS" -> {
				return "等待成团";
			}
			case "NOTPAY" -> {
				int ps = intVal(order.get("prescription_status"));
				if (ps == 1) {
					String msg = "待补充处方信息";
					Object diag = order.get("diagnosis_data");
					if (diag != null && !(diag instanceof Map<?, ?> m && m.isEmpty())) {
						return "待医生开方";
					}
					return msg;
				}
				return "待支付";
			}
			case "WAIT_PAID_CONFIRM" -> {
				return "支付待确认";
			}
			case "PAYED" -> {
				if ("WAIT_PROCESS".equals(trimCancel(str(order.get("cancel_status"))))) {
					return maybeDadaOverride(order, dadaData, receiptType, "退款处理中");
				}
				if ("PENDING".equals(str(order.get("ziti_status")))) {
					return "待自提";
				}
				if ("PARTAIL".equals(str(order.get("delivery_status")))) {
					return "部分发货";
				}
				if ("DONE".equals(str(order.get("delivery_status")))) {
					return maybeDadaOverride(order, dadaData, receiptType, "待收货");
				}
				String base = erpSettingRedisReader.isErpOpen(companyId) ? "审核中" : "待发货";
				return maybeDadaOverride(order, dadaData, receiptType, base);
			}
			case "REVIEW_PASS" -> {
				if ("PARTAIL".equals(str(order.get("delivery_status")))) {
					return "部分出库";
				}
				return "审核完成,待出库";
			}
			case "CANCEL" -> {
				if ("DONE".equals(str(order.get("delivery_status")))
						|| "DONE".equals(str(order.get("ziti_status")))) {
					return "已关闭";
				}
				if ("NO_APPLY_CANCEL".equals(trimCancel(str(order.get("cancel_status"))))) {
					return "已取消";
				}
				if ("WAIT_PROCESS".equals(trimCancel(str(order.get("cancel_status"))))) {
					return "退款处理中";
				}
				if ("REFUND_PROCESS".equals(trimCancel(str(order.get("cancel_status"))))) {
					return "退款处理中";
				}
				if ("SUCCESS".equals(trimCancel(str(order.get("cancel_status"))))) {
					return maybeDadaOverride(order, dadaData, receiptType, "已取消");
				}
				return "等待退款";
			}
			case "WAIT_BUYER_CONFIRM" -> {
				return maybeDadaOverride(order, dadaData, receiptType, "待收货");
			}
			case "DONE" -> {
				return maybeDadaOverride(order, dadaData, receiptType, "已完成");
			}
			case "REFUND_PROCESS" -> {
				return "退款处理中";
			}
			case "REFUND_SUCCESS" -> {
				return "已退款";
			}
			case "PART_PAYMENT" -> {
				return "部分付款";
			}
			default -> {
				return "订单异常";
			}
		}
	}

	private static String trimCancel(String s) {
		return s == null ? "" : s.trim();
	}

	private static String maybeDadaOverride(
			Map<String, Object> order, Map<String, Object> dadaData, String receiptType, String fallback) {
		if (!"dada".equals(receiptType) || dadaData == null || dadaData.isEmpty()) {
			return fallback;
		}
		Object ds = dadaData.get("dada_status");
		if (ds == null) {
			return fallback;
		}
		String t = OrderAppAttachAdminBuilder.dadaDefaultStatusText(ds);
		return t.isEmpty() ? fallback : t;
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
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

	private static long longLoose(Object o) {
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
