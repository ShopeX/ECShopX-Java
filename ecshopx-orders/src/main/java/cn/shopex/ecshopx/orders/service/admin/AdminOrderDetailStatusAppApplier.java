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

package cn.shopex.ecshopx.orders.service.admin;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Fills {@code order_status_msg}, {@code order_status_des} and {@code app_info} on admin order detail payloads.
 */
@Component
public class AdminOrderDetailStatusAppApplier {

	public void apply(Map<String, Object> orderInfo, Map<String, Object> dadaMap, String cancelFrom) {
		apply(orderInfo, dadaMap, cancelFrom, "api");
	}

	public void apply(
			Map<String, Object> orderInfo, Map<String, Object> dadaMap, String cancelFrom, String detailFrom) {
		String statusMsg = resolveStatusMessage(orderInfo, dadaMap, detailFrom);
		orderInfo.put("order_status_msg", statusMsg);

		if (!"api".equals(detailFrom == null ? "" : detailFrom.trim())) {
			return;
		}

		Map<String, Object> attach = new LinkedHashMap<>();
		attach.put("order_id", orderInfo.get("order_id"));
		attach.put("company_id", orderInfo.get("company_id"));
		attach.put("order_type", orderInfo.get("order_type"));
		attach.put("order_class", orderInfo.get("order_class"));
		attach.put("order_status_des", orderInfo.get("order_status_des"));
		attach.put("update_time", orderInfo.get("update_time"));
		attach.put("end_time", orderInfo.get("end_time"));
		attach.put("order_auto_close_aftersales_time", orderInfo.get("order_auto_close_aftersales_time"));
		attach.put("left_aftersales_num", orderInfo.get("left_aftersales_num"));
		attach.put("self_delivery_status", orderInfo.get("self_delivery_status"));
		attach.put("self_delivery_operator_id", orderInfo.get("self_delivery_operator_id"));
		if (dadaMap != null && !dadaMap.isEmpty()) {
			Object ds = dadaMap.get("dada_status");
			if (ds != null) {
				attach.put("dada_status", String.valueOf(ds));
			}
		}

		String dbOrderStatus = str(orderInfo.get("order_status"));
		String receiptType = str(orderInfo.get("receipt_type"));
		Map<String, Object> appInfo =
				OrderAppAttachAdminBuilder.buildAppInfo(dbOrderStatus, receiptType, attach, cancelFrom);
		orderInfo.put("app_info", appInfo);
	}

	private static String resolveStatusMessage(
			Map<String, Object> order, Map<String, Object> dadaData, String detailFrom) {
		String orderStatus = str(order.get("order_status"));
		String receiptType = str(order.get("receipt_type"));
		switch (orderStatus) {
			case "WAIT_GROUPS_SUCCESS" -> {
				order.put("order_status_des", "WAIT_GROUPS_SUCCESS");
				return "等待成团";
			}
			case "NOTPAY" -> {
				order.put("order_status_des", "NOTPAY");
				int ps = intVal(order.get("prescription_status"));
				if (ps == 1) {
					order.put("order_status_des", "SUPPLY_PRESCRIPTION_INFO");
					String msg = "待补充处方信息";
					Object diag = order.get("diagnosis_data");
					if (diag != null && !(diag instanceof Map<?, ?> m && m.isEmpty())) {
						order.put("order_status_des", "WAIT_PRESCRIPTION");
						return "待医生开方";
					}
					return msg;
				}
				return "待支付";
			}
			case "WAIT_PAID_CONFIRM" -> {
				order.put("order_status_des", "WAIT_PAID_CONFIRM");
				return "支付待确认";
			}
			case "PAYED" -> {
				if ("WAIT_PROCESS".equals(str(order.get("cancel_status")))) {
					order.put("order_status_des", "PAYED_WAIT_PROCESS");
					return maybeDadaOverride(order, dadaData, receiptType, detailFrom, "退款处理中");
				}
				if ("PENDING".equals(str(order.get("ziti_status")))) {
					order.put("order_status_des", "PAYED_PENDING");
					return "待自提";
				}
				if ("PARTAIL".equals(str(order.get("delivery_status")))) {
					order.put("order_status_des", "PAYED_PARTAIL");
					return "部分发货";
				}
				if ("DONE".equals(str(order.get("delivery_status")))) {
					order.put("order_status_des", "WAIT_BUYER_CONFIRM");
					return maybeDadaOverride(order, dadaData, receiptType, detailFrom, "待收货");
				}
				order.put("order_status_des", "PAYED");
				String base = "待发货";
				return maybeDadaOverride(order, dadaData, receiptType, detailFrom, base);
			}
			case "REVIEW_PASS" -> {
				if ("PARTAIL".equals(str(order.get("delivery_status")))) {
					order.put("order_status_des", "REVIEW_PASS_PARTAIL");
					return "部分出库";
				}
				order.put("order_status_des", "REVIEW_PASS");
				return "审核完成,待出库";
			}
			case "CANCEL" -> {
				if ("DONE".equals(str(order.get("delivery_status")))
						|| "DONE".equals(str(order.get("ziti_status")))) {
					order.put("order_status_des", "CLOSED");
					return "已关闭";
				}
				if ("NO_APPLY_CANCEL".equals(str(order.get("cancel_status")))) {
					order.put("order_status_des", "CANCEL");
					return "已取消";
				}
				if ("WAIT_PROCESS ".equals(str(order.get("cancel_status")))
						|| "WAIT_PROCESS".equals(str(order.get("cancel_status")))) {
					order.put("order_status_des", "CANCEL_WAIT_PROCESS");
					return "退款处理中";
				}
				if ("REFUND_PROCESS".equals(str(order.get("cancel_status")))) {
					order.put("order_status_des", "CANCEL_REFUND_PROCESS");
					return "退款处理中";
				}
				if ("SUCCESS".equals(str(order.get("cancel_status")))) {
					order.put("order_status_des", "CANCEL");
					return maybeDadaOverride(order, dadaData, receiptType, detailFrom, "已取消");
				}
				order.put("order_status_des", "CANCEL_REFUND_FAIL");
				return "等待退款";
			}
			case "WAIT_BUYER_CONFIRM" -> {
				order.put("order_status_des", "WAIT_BUYER_CONFIRM");
				return maybeDadaOverride(order, dadaData, receiptType, detailFrom, "待收货");
			}
			case "DONE" -> {
				order.put("order_status_des", "DONE");
				return maybeDadaOverride(order, dadaData, receiptType, detailFrom, "已完成");
			}
			case "REFUND_PROCESS" -> {
				order.put("order_status_des", "REFUND_PROCESS");
				return "退款处理中";
			}
			case "REFUND_SUCCESS" -> {
				order.put("order_status_des", "REFUND_SUCCESS");
				return "已退款";
			}
			case "PART_PAYMENT" -> {
				order.put("order_status_des", "PART_PAYMENT");
				return "部分付款";
			}
			default -> {
				order.put("order_status_des", "ORDER_ABERRANT");
				return "订单异常";
			}
		}
	}

	private static String maybeDadaOverride(
			Map<String, Object> order,
			Map<String, Object> dadaData,
			String receiptType,
			String detailFrom,
			String fallback) {
		if (!"dada".equals(receiptType) || dadaData == null || dadaData.isEmpty()) {
			return fallback;
		}
		Object ds = dadaData.get("dada_status");
		if (ds == null) {
			return fallback;
		}
		String t = OrderAppAttachAdminBuilder.dadaStatusTextForConsumerContext(ds, detailFrom);
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
}
