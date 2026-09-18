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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds {@code app_info} for admin order detail, aligned with shop-side attach semantics.
 */
public final class OrderAppAttachAdminBuilder {

	private static final String TYPE_TEXT = "text";
	private static final String TYPE_NOT_PAY = "not_pay";
	private static final String TYPE_CANCEL = "cancel";
	private static final String TYPE_DADA_CANCEL = "dada_cancel";

	private static final String ICON_SUCCESS_1 = "success_1";
	private static final String ICON_SUCCESS_2 = "success_2";
	private static final String ICON_DELIVERY_1 = "delivery_1";
	private static final String ICON_DELIVERY_2 = "delivery_2";
	private static final String ICON_PAYED_1 = "payed_1";
	private static final String ICON_PAYED_2 = "payed_2";
	private static final String ICON_PAYED_3 = "payed_3";
	private static final String ICON_PAYED_4 = "payed_4";
	private static final String ICON_PAYED_5 = "payed_5";
	private static final String ICON_PAYED_6 = "payed_6";
	private static final String ICON_NOT_PAY = "not_pay";
	private static final String ICON_CANCEL = "cancel";

	private OrderAppAttachAdminBuilder() {
	}

	public static Map<String, Object> buildAppInfo(
			String dbOrderStatus,
			String receiptType,
			Map<String, Object> attachParams,
			String cancelFrom) {
		Map<String, Object> statusInfo = new LinkedHashMap<>();
		statusInfo.put("main_status", "");
		statusInfo.put("child_status", "");

		String listStatusMag = "";
		String detailStatusMsg = "";
		Map<String, Object> detailStatus = new LinkedHashMap<>();
		detailStatus.put("icon", ICON_SUCCESS_1);
		detailStatus.put("main_msg", "");
		detailStatus.put("description", "");
		detailStatus.put("type", TYPE_TEXT);

		Map<String, Object> terminalInfo = null;
		List<String> buttonTypes = new ArrayList<>();

		String dadaStatus = str(attachParams.get("dada_status"));

		String orderStatus = dbOrderStatus;
		String orderStatusDes = str(attachParams.get("order_status_des"));
		if ("PAYED_WAIT_PROCESS".equals(orderStatusDes)) {
			orderStatus = "CANCEL";
		}
		if ("PAYED_PARTAIL".equals(orderStatusDes)) {
			orderStatus = "WAIT_BUYER_CONFIRM";
		}

		switch (orderStatus) {
			case "WAIT_GROUPS_SUCCESS" -> {
				statusInfo.put("main_status", "notship");
				listStatusMag = "等待成团";
				detailStatusMsg = "等待成团";
			}
			case "REFUND_PROCESS", "REFUND_SUCCESS", "CANCEL" -> {
				statusInfo.put("main_status", "cancel");
				String cf = cancelFrom != null ? cancelFrom : "";
				if ("buyer".equals(cf)) {
					statusInfo.put("child_status", "cancel_buyer");
					detailStatus.put("description", "用户自主取消订单");
				} else {
					statusInfo.put("child_status", "cancel_shop");
					buttonTypes.add("contact");
					detailStatus.put("description", "商家取消订单");
				}
				if ("PAYED_WAIT_PROCESS".equals(orderStatusDes)) {
					buttonTypes.add("confirmcancel");
					if ("buyer".equals(cf)) {
						detailStatus.put("description", "用户取消已支付订单，待退款");
					} else {
						detailStatus.put("description", "商家取消已支付订单，待退款");
					}
				}
				detailStatusMsg = "订单已取消";
				listStatusMag = "订单已取消";
				detailStatus.put("icon", ICON_CANCEL);
				terminalInfo = new LinkedHashMap<>();
				terminalInfo.put("msg", "取消时间");
				terminalInfo.put("time", attachParams.get("update_time"));
			}
			case "NOTPAY", "WAIT_PAID_CONFIRM", "PART_PAYMENT" -> {
				statusInfo.put("main_status", "notpay");
				listStatusMag = "待付款";
				detailStatusMsg = "订单待付款";
				addAll(
						buttonTypes,
						"contact",
						"payment",
						"cancel",
						"markdown");
				detailStatus.put("type", TYPE_NOT_PAY);
				detailStatus.put("icon", ICON_NOT_PAY);
			}
			case "REVIEW_PASS", "PAYED" -> {
				statusInfo.put("main_status", "notship");
				detailStatusMsg = "订单已付款，待发货";
				if ("dada".equals(receiptType)) {
					statusInfo.put("child_status", "dada_" + dadaStatus);
					switch (dadaStatus) {
						case "0" -> {
							listStatusMag = "已付款待接单";
							detailStatus.put("description", "买家已支付，请尽快接单");
							detailStatus.put("icon", ICON_PAYED_1);
						}
						case "1" -> {
							listStatusMag = "骑士待接单";
							detailStatus.put("description", "当前尚未有骑士接单…");
							detailStatus.put("icon", ICON_PAYED_3);
						}
						case "5" -> {
							listStatusMag = "骑士已取消";
							detailStatus.put("type", TYPE_DADA_CANCEL);
							detailStatus.put("icon", ICON_PAYED_2);
						}
						case "2" -> {
							listStatusMag = "骑士待取货";
							detailStatus.put("description", "骑士已接单，请尽快拣货");
							detailStatus.put("icon", ICON_PAYED_4);
						}
						case "100" -> {
							detailStatus.put("description", "骑士已到店，正在取货");
							listStatusMag = "骑士已到店";
							detailStatus.put("icon", ICON_PAYED_5);
						}
						default -> {
						}
					}
					if ("0".equals(dadaStatus)) {
						addAll(buttonTypes, "contact", "accept", "cancel");
					} else {
						buttonTypes.add("contact");
					}
					if ("5".equals(dadaStatus)) {
						detailStatusMsg = "";
					}
				} else if ("ziti".equals(receiptType)) {
					listStatusMag = "待自提";
					addAll(buttonTypes, "contact", "consume");
					detailStatus.put("icon", ICON_PAYED_6);
				} else if ("merchant".equals(receiptType)) {
					addAll(buttonTypes, "delivery", "cancel");
					String sds = str(attachParams.get("self_delivery_status"));
					if ("CONFIRMING".equals(sds)) {
						listStatusMag = "待确认";
						addAll(buttonTypes, "confirmdeliverystaff", "confirmpackag");
					} else if ("RECEIVEORDER".equals(sds)) {
						listStatusMag = "已接单";
						addAll(buttonTypes, "canceldeliverystaff", "confirmpackag");
					} else if ("PACKAGED".equals(sds)) {
						listStatusMag = "已打包";
						buttonTypes.add("canceldeliverystaff");
					}
					Object opId = attachParams.get("self_delivery_operator_id");
					if (opId == null || "0".equals(String.valueOf(opId))) {
						buttonTypes.add("confirmdeliverystaff");
					}
					detailStatus.put("icon", ICON_PAYED_6);
				} else {
					listStatusMag = "已付款待发货";
					addAll(buttonTypes, "contact", "delivery", "cancel");
					detailStatus.put("description", "买家已支付，请尽快发货");
					detailStatus.put("icon", ICON_PAYED_1);
				}
			}
			case "WAIT_BUYER_CONFIRM" -> {
				statusInfo.put("main_status", "shipping");
				if ("dada".equals(receiptType)) {
					listStatusMag = "配送中";
					detailStatus.put("description", "骑士正在送货…");
					detailStatus.put("icon", ICON_DELIVERY_1);
				} else if ("merchant".equals(receiptType)) {
					String sds = str(attachParams.get("self_delivery_status"));
					if ("CONFIRMING".equals(sds)) {
						listStatusMag = "配送取消";
						addAll(buttonTypes, "confirmdeliverystaff", "updatedelivery");
						detailStatus.put("description", "商家正在重新分配配送员…");
					} else if ("DONE".equals(sds)) {
						listStatusMag = "已送达";
					} else {
						listStatusMag = "配送中";
						addAll(buttonTypes, "updatedelivery", "canceldeliverystaff");
						detailStatus.put("description", "配送员正在送货…");
					}
					Object opId = attachParams.get("self_delivery_operator_id");
					if (opId == null || "0".equals(String.valueOf(opId))) {
						buttonTypes.add("confirmdeliverystaff");
					}
					detailStatus.put("icon", ICON_DELIVERY_2);
				} else {
					listStatusMag = "已发货待收货";
					detailStatus.put("description", "商品配送中");
					detailStatus.put("icon", ICON_DELIVERY_2);
				}
				if ("PAYED_PARTAIL".equals(orderStatusDes)) {
					addAll(buttonTypes, "contact", "delivery2");
				} else {
					buttonTypes.add("contact");
				}
				if (intVal(attachParams.get("left_aftersales_num")) > 0) {
					buttonTypes.add("aftersales");
				}
				detailStatusMsg = "订单已发货，待收货";
			}
			case "DONE" -> {
				statusInfo.put("main_status", "finish");
				listStatusMag = "已完成";
				detailStatusMsg = "订单已完成";
				detailStatus.put("description", "收货人已签收");
				detailStatus.put("icon", ICON_SUCCESS_1);
				Object endTime = attachParams.get("end_time");
				if (endTime != null && intVal(endTime) > 0) {
					terminalInfo = new LinkedHashMap<>();
					terminalInfo.put("msg", "完成时间");
					terminalInfo.put("time", endTime);
				}
				if ("dada".equals(receiptType) && "10".equals(dadaStatus)) {
					statusInfo.put("child_status", "dada_10");
					buttonTypes.add("contact");
					listStatusMag = "未妥投";
					detailStatusMsg = "订单已完成，未妥投";
					detailStatus.put("description", "请联系收货人确认订单地址");
					detailStatus.put("icon", ICON_SUCCESS_2);
					terminalInfo = new LinkedHashMap<>();
					terminalInfo.put("msg", "未妥投时间");
					terminalInfo.put("time", endTime);
				} else if ("merchant".equals(receiptType)) {
					listStatusMag = "已送达";
					detailStatus.put("icon", ICON_SUCCESS_2);
				}
				int closeAt = intVal(attachParams.get("order_auto_close_aftersales_time"));
				if (closeAt > (int) (System.currentTimeMillis() / 1000L)
						&& intVal(attachParams.get("left_aftersales_num")) > 0) {
					buttonTypes.add("aftersales");
				}
			}
			default -> {
			}
		}

		String orderClassName = orderClassLabel(str(attachParams.get("order_class")));
		String deliveryTypeMsg;
		String deliveryTypeName;
		switch (receiptType) {
			case "dada" -> {
				deliveryTypeMsg = "商家同城配送";
				deliveryTypeName = "同城快递";
			}
			case "ziti" -> {
				deliveryTypeMsg = "门店自提";
				deliveryTypeName = "自提";
			}
			case "merchant" -> {
				deliveryTypeMsg = "商家自配";
				deliveryTypeName = "自配";
			}
			default -> {
				deliveryTypeMsg = "商家快递配送";
				deliveryTypeName = "普通快递";
			}
		}

		detailStatus.put("main_msg", detailStatusMsg);
		List<Map<String, Object>> buttons = materializeButtons(buttonTypes);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("status_info", statusInfo);
		out.put("list_status_mag", listStatusMag);
		out.put("detail_status", detailStatus);
		out.put("buttons", buttons);
		out.put("delivery_type_name", deliveryTypeName);
		out.put("delivery_type_msg", deliveryTypeMsg);
		out.put("order_class_name", orderClassName);
		out.put("terminal_info", terminalInfo);
		return out;
	}

	private static String orderClassLabel(String oc) {
		if (oc == null) {
			return "";
		}
		return switch (oc) {
			case "normal" -> "普通订单";
			case "groups" -> "拼团订单";
			case "community" -> "社区活动订单";
			case "bargain" -> "助力订单";
			case "seckill" -> "秒杀订单";
			case "shopguide" -> "导购订单";
			case "pointsmall" -> "积分商城";
			case "excard" -> "兑换券订单";
			default -> "";
		};
	}

	private static List<Map<String, Object>> materializeButtons(List<String> types) {
		List<Map<String, Object>> out = new ArrayList<>();
		for (String t : types) {
			Map<String, Object> def = BUTTON_DEFS.get(t);
			if (def != null) {
				out.add(new LinkedHashMap<>(def));
			}
		}
		return out;
	}

	private static void addAll(List<String> list, String... xs) {
		for (String x : xs) {
			list.add(x);
		}
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

	private static final Map<String, Map<String, Object>> BUTTON_DEFS = new LinkedHashMap<>();

	static {
		putBtn("cancel", "cancel", "取消订单");
		putBtn("contact", "contact", "联系客户");
		putBtn("mark", "mark", "备注");
		putBtn("confirmdeliverystaff", "confirmdeliverystaff", "分配配送员");
		putBtn("canceldeliverystaff", "canceldeliverystaff", "取消配送");
		putBtn("confirmpackag", "confirmpackag", "已打包");
		putBtn("delivery", "delivery", "发货");
		putBtn("delivery2", "delivery", "部分发货：发货");
		putBtn("updatedelivery", "updatedelivery", "更新发货");
		putBtn("consume", "consume", "核销");
		putBtn("accept", "accept", "接单");
		putBtn("confirmcancel", "confirmcancel", "退款");
		putBtn("markdown", "markdown", "改价");
		putBtn("aftersales", "aftersales", "申请售后");
		putBtn("payment", "payment", "收款");
	}

	private static void putBtn(String key, String type, String name) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("type", type);
		m.put("name", name);
		BUTTON_DEFS.put(key, m);
	}

	/** Default (admin API) dada status texts, aligned with zh-CN lang. */
	public static String dadaDefaultStatusText(Object dadaStatusRaw) {
		String k = dadaStatusRaw == null ? "" : String.valueOf(dadaStatusRaw);
		return switch (k) {
			case "0" -> "店铺待接单";
			case "1" -> "骑士待接单";
			case "2" -> "待取货";
			case "100" -> "骑士到店";
			case "3" -> "配送中";
			case "9" -> "未妥投";
			case "10" -> "妥投异常";
			default -> "";
		};
	}

	/**
	 * Consumer-facing Dada rider status label by request {@code from}.
	 */
	public static String dadaStatusTextForConsumerContext(Object dadaStatusRaw, String from) {
		String k = dadaStatusRaw == null ? "" : String.valueOf(dadaStatusRaw);
		String f = from == null || from.isBlank() ? "api" : from.trim();
		if ("front_detail".equals(f)) {
			return switch (k) {
				case "0" -> "等待商家接单";
				case "1" -> "门店正在拣货，等待骑手接单";
				case "2" -> "骑手正赶往商家";
				case "100" -> "骑士到店";
				case "3" -> "骑手正在快马加鞭向您赶去";
				case "4" -> "此次订单已完成";
				case "5" -> "您的订单已取消";
				case "9" -> "收货地址异常，请联系客服";
				case "10" -> "此次订单已完成";
				default -> "";
			};
		}
		if ("front_list".equals(f)) {
			return switch (k) {
				case "0" -> "商家待接单";
				case "1" -> "商家已接单";
				case "2" -> "待取货";
				case "100" -> "骑士到店";
				case "3" -> "配送中";
				case "9" -> "未妥投";
				case "10" -> "已完成";
				default -> "";
			};
		}
		return dadaDefaultStatusText(dadaStatusRaw);
	}

	public static boolean dadaStatusKeyKnown(Object dadaStatusRaw) {
		String k = dadaStatusRaw == null ? "" : String.valueOf(dadaStatusRaw);
		return !k.isEmpty()
				&& (Objects.equals(k, "0")
						|| Objects.equals(k, "1")
						|| Objects.equals(k, "2")
						|| Objects.equals(k, "100")
						|| Objects.equals(k, "3")
						|| Objects.equals(k, "9")
						|| Objects.equals(k, "10")
						|| Objects.equals(k, "5"));
	}
}
