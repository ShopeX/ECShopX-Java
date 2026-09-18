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

package cn.shopex.ecshopx.promotions.service.wxatemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

public final class WxaTemplateSceneDefinitions {

	private static final Map<String, WxaTemplateSceneDefinition> BY_NAME;
	private static final List<Map.Entry<String, WxaTemplateSceneDefinition>> ORDERED_ENTRIES;

	private WxaTemplateSceneDefinitions() {}

	static {
		LinkedHashMap<String, WxaTemplateSceneDefinition> tmp = new LinkedHashMap<>();
		tmp.put("memberCreateSucc", WxaTemplateSceneDefinition.MEMBER_CREATE_SUCC);
		tmp.put("paymentSucc", WxaTemplateSceneDefinition.PAYMENT_SUCC);
		tmp.put("reservationRemind", WxaTemplateSceneDefinition.RESERVATION_REMIND);
		tmp.put("reservationSucc", WxaTemplateSceneDefinition.RESERVATION_SUCC);
		tmp.put("payOrdersRemind", WxaTemplateSceneDefinition.PAY_ORDERS_REMIND);
		tmp.put("orderDeliverySucc", WxaTemplateSceneDefinition.ORDER_DELIVERY_SUCC);
		tmp.put("aftersalesRefuse", WxaTemplateSceneDefinition.AFTERSALES_REFUSE);
		tmp.put("aftersalesSuccess", WxaTemplateSceneDefinition.AFTERSALES_SUCCESS);
		tmp.put("userGetCardSucc", WxaTemplateSceneDefinition.USER_GET_CARD_SUCC);
		tmp.put("registrationResultNotice", WxaTemplateSceneDefinition.REGISTRATION_RESULT_NOTICE);
		tmp.put("registrationActivityNotice", WxaTemplateSceneDefinition.REGISTRATION_ACTIVITY_NOTICE);
		tmp.put("goodsArrivalNotice", WxaTemplateSceneDefinition.GOODS_ARRIVAL_NOTICE);
		BY_NAME = Map.copyOf(tmp);
		List<Map.Entry<String, WxaTemplateSceneDefinition>> order = new ArrayList<>();
		for (Map.Entry<String, WxaTemplateSceneDefinition> e : tmp.entrySet()) {
			order.add(Map.entry(e.getKey(), e.getValue()));
		}
		ORDERED_ENTRIES = List.copyOf(order);
	}

	public static Optional<WxaTemplateSceneDefinition> findByScenesName(String scenesName) {
		return Optional.ofNullable(BY_NAME.get(scenesName));
	}

	/**
	 * Scenes whose {@code template_name} whitelist contains {@code templateName}, in static registration order.
	 */
	public static List<Map.Entry<String, WxaTemplateSceneDefinition>> orderedScenesMatchingTemplateName(
			@Nullable String templateName) {
		if (!StringUtils.hasText(templateName)) {
			return List.of();
		}
		List<Map.Entry<String, WxaTemplateSceneDefinition>> out = new ArrayList<>();
		for (Map.Entry<String, WxaTemplateSceneDefinition> e : ORDERED_ENTRIES) {
			if (e.getValue().getAllowedTemplateNames().contains(templateName)) {
				out.add(e);
			}
		}
		return List.copyOf(out);
	}

	public static final class WxaTemplateSceneDefinition {

		public static final WxaTemplateSceneDefinition MEMBER_CREATE_SUCC =
				new WxaTemplateSceneDefinition(
						"5117",
						List.of(2, 3),
						List.of("yykcutdown", "yykweishop", "appleweishop", "yykcommunity"),
						"注册成功提醒",
						"注册成功提醒",
						List.of(
								row("date", "注册时间", "date2"),
								row("notice", "温馨提示", "thing3")),
						"会员提醒",
						desc("会员注册成功后触发"));

		public static final WxaTemplateSceneDefinition PAYMENT_SUCC =
				new WxaTemplateSceneDefinition(
						"1648",
						List.of(2, 3, 6, 8, 7),
						List.of(
								"yykcutdown",
								"yykweishop",
								"appleweishop",
								"yykcommunity",
								"yykmendian",
								"yykmembership"),
						"订单支付成功通知",
						"订单支付成功通知",
						List.of(
								row("order_id", "订单号", "character_string2"),
								row("pay_money", "订单金额", "amount3"),
								row("item_name", "订单商品", "thing6"),
								row("pay_date", "支付时间", "date8"),
								row("receipt_type", "取货方式", "thing7")),
						"交易提醒",
						desc("订单支付后触发"));

		public static final WxaTemplateSceneDefinition RESERVATION_REMIND =
				new WxaTemplateSceneDefinition(
						"5378",
						List.of(1, 2, 6, 8, 4),
						List.of("yykmendian"),
						"预约到店提醒",
						"预约到店提醒",
						List.of(
								row("name", "预约内容", "thing1"),
								row("date", "预约时间", "date2"),
								row("shop_name", "预约门店", "thing6"),
								row("shop_address", "门店地址", "thing8"),
								row("remarks", "温馨提示", "thing4")),
						"交易提醒",
						descReservation());

		public static final WxaTemplateSceneDefinition RESERVATION_SUCC =
				new WxaTemplateSceneDefinition(
						"5380",
						List.of(1, 2, 6, 8, 4),
						List.of("yykmendian"),
						"预约成功通知",
						"预约成功通知",
						List.of(
								row("name", "预约内容", "thing1"),
								row("date", "预约时间", "date2"),
								row("shop_name", "预约门店", "thing6"),
								row("shop_address", "门店地址", "thing8"),
								row("remarks", "温馨提示", "thing4")),
						"交易提醒",
						desc("预约创建成功后触发"));

		public static final WxaTemplateSceneDefinition PAY_ORDERS_REMIND =
				new WxaTemplateSceneDefinition(
						"2723",
						List.of(1, 7, 6, 8, 4),
						List.of("yykweishop", "appleweishop"),
						"订单待支付提醒",
						"订单待支付提醒",
						List.of(
								row("order_id", "订单号", "character_string1"),
								row("pay_money", "待支付金额", "amount7"),
								row("item_name", "商品名称", "thing6"),
								row("created", "下单时间", "date8"),
								row("remarks", "温馨提示", "thing4")),
						"交易提醒",
						descPayOrders());

		public static final WxaTemplateSceneDefinition ORDER_DELIVERY_SUCC =
				new WxaTemplateSceneDefinition(
						"1856",
						List.of(7, 14, 3, 5),
						List.of("yykweishop", "appleweishop"),
						"订单发货提醒",
						"订单发货提醒",
						List.of(
								row("order_id", "订单号", "character_string7"),
								row("delivery_corp", "物流公司", "thing14"),
								row("delivery_code", "快递单号", "character_string3"),
								row("item_name", "商品信息", "thing5")),
						"交易提醒",
						desc("商家发货后立即触发"));

		public static final WxaTemplateSceneDefinition AFTERSALES_REFUSE =
				new WxaTemplateSceneDefinition(
						"4330",
						List.of(1, 5, 6),
						List.of("yykweishop", "appleweishop"),
						"售后通知",
						"售后通知",
						List.of(
								row("order_id", "订单编号", "character_string1"),
								row("refund_fee", "退款金额", "amount5"),
								row("remarks", "备注", "thing6")),
						"交易提醒",
						desc("商家售后操作后触发"));

		public static final WxaTemplateSceneDefinition AFTERSALES_SUCCESS =
				new WxaTemplateSceneDefinition(
						"4499",
						List.of(1, 5, 6),
						List.of("yykweishop", "appleweishop"),
						"售后进度通知",
						"退货回寄提醒",
						List.of(
								row("order_id", "订单编号", "character_string1"),
								row("refund_fee", "退款金额", "amount5"),
								row("remarks", "备注", "thing6")),
						"交易提醒",
						desc("商家同意退货等待买家寄回时触发"));

		public static final WxaTemplateSceneDefinition USER_GET_CARD_SUCC =
				new WxaTemplateSceneDefinition(
						"3995",
						List.of(1, 2, 3, 4),
						List.of("yykweishop", "appleweishop"),
						"优惠券领取通知",
						"优惠券状态通知",
						List.of(
								row("title", "优惠券名称", "thing1"),
								row("active_date", "有效期", "character_string2"),
								row("used_action", "使用方式", "thing3"),
								row("remarks", "温馨提醒", "thing4")),
						"交易提醒",
						desc("优惠券领取触发"));

		public static final WxaTemplateSceneDefinition REGISTRATION_RESULT_NOTICE =
				new WxaTemplateSceneDefinition(
						"6618",
						List.of(1, 2),
						List.of("yykweishop", "appleweishop"),
						"报名结果通知",
						"报名结果通知",
						List.of(
								row("activity_name", "报名活动", "thing1"),
								row("review_result", "报名结果", "thing2")),
						"交易提醒",
						desc("报名审核结果通知"));

		public static final WxaTemplateSceneDefinition REGISTRATION_ACTIVITY_NOTICE =
				new WxaTemplateSceneDefinition(
						"342",
						List.of(10, 12, 13, 6),
						List.of("yykweishop", "appleweishop"),
						"活动开始提醒",
						"活动开始提醒",
						List.of(
								row("activity_name", "报名活动", "thing10"),
								row("activity_start_time", "开始时间", "time12"),
								row("activity_end_time", "结束时间", "time13"),
								row("activity_address", "活动地址", "thing6")),
						"交易提醒",
						descRegistrationActivity());

		public static final WxaTemplateSceneDefinition GOODS_ARRIVAL_NOTICE =
				new WxaTemplateSceneDefinition(
						"5019",
						List.of(1, 2),
						List.of("yykweishop", "appleweishop"),
						"商品到货通知",
						"商品到货通知",
						List.of(
								row("item_name", "商品名称", "thing1"),
								row("notice", "温馨提示", "thing2")),
						"会员提醒",
						desc("缺货商品到货通知到会员"));

		private final String id;
		private final List<Integer> keywordIdList;
		private final List<String> allowedTemplateNames;
		private final String listTitle;
		private final String listSceneDesc;
		private final List<Map<String, String>> valueMaps;
		private final String tmplType;
		private final Map<String, Object> sendTimeDesc;

		private WxaTemplateSceneDefinition(
				String id,
				List<Integer> keywordIdList,
				List<String> allowedTemplateNames,
				String listTitle,
				String listSceneDesc,
				List<Map<String, String>> valueMaps,
				String tmplType,
				Map<String, Object> sendTimeDesc) {
			this.id = id;
			this.keywordIdList = List.copyOf(keywordIdList);
			this.allowedTemplateNames = List.copyOf(allowedTemplateNames);
			this.listTitle = listTitle;
			this.listSceneDesc = listSceneDesc;
			List<Map<String, String>> copied = new ArrayList<>();
			for (Map<String, String> m : valueMaps) {
				copied.add(Map.copyOf(m));
			}
			this.valueMaps = List.copyOf(copied);
			this.tmplType = tmplType;
			this.sendTimeDesc = Map.copyOf(sendTimeDesc);
		}

		private WxaTemplateSceneDefinition() {
			this("", List.of(), List.of(), "", "", List.<Map<String, String>>of(), "", Map.of());
		}

		public List<String> getAllowedTemplateNames() {
			return allowedTemplateNames;
		}

		public String getListTitle() {
			return listTitle;
		}

		public String getListSceneDesc() {
			return listSceneDesc;
		}

		public List<Map<String, String>> getValueAsMaps() {
			List<Map<String, String>> out = new ArrayList<>();
			for (Map<String, String> m : valueMaps) {
				out.add(Map.copyOf(m));
			}
			return List.copyOf(out);
		}

		public List<Integer> getKeywordIdList() {
			return keywordIdList;
		}

		public String getSceneDesc() {
			return listSceneDesc;
		}

		public String getId() {
			return id;
		}

		public String getTmplType() {
			return tmplType;
		}

		public Map<String, Object> getSendTimeDesc() {
			return sendTimeDesc;
		}

		private static Map<String, String> row(String column, String title, String keyword) {
			LinkedHashMap<String, String> m = new LinkedHashMap<>();
			m.put("column", column);
			m.put("title", title);
			m.put("keyword", keyword);
			return m;
		}

		private static Map<String, Object> desc(String title) {
			LinkedHashMap<String, Object> m = new LinkedHashMap<>();
			m.put("title", title);
			return m;
		}

		private static Map<String, Object> descReservation() {
			LinkedHashMap<String, Object> m = new LinkedHashMap<>();
			m.put("title", "预约到期前");
			m.put("time_list", List.of(90, 60, 30, 10));
			m.put("value", 60);
			return m;
		}

		private static Map<String, Object> descPayOrders() {
			LinkedHashMap<String, Object> m = new LinkedHashMap<>();
			m.put("title", "买家下单");
			m.put("time_list", List.of(5, 10, 15, 20, 30, 60, 120, 180));
			m.put("value", 10);
			m.put("end_title", "未付款触发");
			return m;
		}

		private static Map<String, Object> descRegistrationActivity() {
			LinkedHashMap<String, Object> m = new LinkedHashMap<>();
			m.put("title", "活动开始前");
			m.put("time_list", List.of(2, 4, 8, 12, 24, 48, 72));
			m.put("value", 24);
			m.put("time_unit", "小时");
			m.put("end_title", "，触发通知");
			return m;
		}
	}
}
